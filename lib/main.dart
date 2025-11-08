import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  print("🔥 Background message: ${message.data}");
}

Future<void> _requestNotificationPermission() async {
  final status = await Permission.notification.request();
  if (status.isGranted) {
    print("✅ Notification permission granted");
  } else {
    print("❌ Notification permission denied");
  }
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();

  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

  // Request notification permission on Android 13+
  await _requestNotificationPermission();

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  static const platform = MethodChannel('com.example.foreground/service');
  String _status = 'No ride';
  String _eta = '';

  Future<void> _startService(String driver) async {
    // Ensure we have notification permission on Android 13+
    try {
      if (await _ensureNotificationPermission() == false) {
        print('Notification permission denied');
        return;
      }

      final res = await platform.invokeMethod('startService', {'title': 'Ride with $driver', 'text': 'Driver is arriving... ETA 5 min'});
      print('startService: \$res');
      setState(() {
        _status = 'Driver is arriving';
        _eta = '5 min';
      });
    } on PlatformException catch (e) {
      print('Failed to start service: \$e');
    }
  }

  Future<bool> _ensureNotificationPermission() async {
    // On Android pre-13 the permission is granted at install-time.
    if (!await Permission.notification.shouldShowRequestRationale && (await Permission.notification.status).isGranted) {
      return true;
    }

    final status = await Permission.notification.request();
    return status.isGranted;
  }

  Future<void> _updateService(String status, String eta) async {
    try {
      final res = await platform.invokeMethod('updateService', {'title': 'Ride Status: \$status', 'text': 'ETA: \$eta', 'status': status, 'eta': eta});
      print('updateService: \$res');
      setState(() {
        _status = status;
        _eta = eta;
      });
    } on PlatformException catch (e) {
      print('Failed to update service: \$e');
    }
  }

  Future<void> _stopService() async {
    try {
      final res = await platform.invokeMethod('stopService');
      print('stopService: \$res');
      setState(() {
        _status = 'No ride';
        _eta = '';
      });
    } on PlatformException catch (e) {
      print('Failed to stop service: \$e');
    }
  }

  @override
  void initState() {
    super.initState();

    // Handle incoming FCM messages while app is in foreground
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('📩 Message received: ${message.notification?.title}');
      final data = message.data;

      if (data['action'] == 'start') {
        _startService(data['driver'] ?? 'Unknown');
      } else if (data['action'] == 'update') {
        _updateService(data['status'] ?? 'Updating...', data['eta'] ?? '...');
      } else if (data['action'] == 'stop') {
        _stopService();
      }
    });

    // Print FCM device token for debugging
    getDeviceToken();
  }

  Future<void> getDeviceToken() async {
    try {
      FirebaseMessaging messaging = FirebaseMessaging.instance;
      String? token = await messaging.getToken();
      print('🔥 FCM Device Token: $token');
    } catch (e) {
      print('Failed to get FCM token: $e');
    }
  }

  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter + Native Foreground',
      home: Scaffold(
        appBar: AppBar(title: const Text('Uber-like Notification (Android)')),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Status: \$_status', style: const TextStyle(fontSize: 18)),
              const SizedBox(height: 8),
              Text('ETA: \$_eta', style: const TextStyle(fontSize: 16)),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () => _startService('Ariun'),
                child: const Text('Start Ride (native service)'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: () => _updateService('Driver Arrived', '0 min'),
                child: const Text('Update: Arrived'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: () => _updateService('On Trip', '15 min'),
                child: const Text('Update: On Trip'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _stopService,
                child: const Text('Stop Ride (stop service)'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
