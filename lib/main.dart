import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:live_activities/live_activities.dart';
import 'package:onesignal_flutter/onesignal_flutter.dart';
import 'screens/send_notification_screen.dart';

Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  print("🔥 Background message: ${message.notification?.title}");
  print("🔥 Background message: ${message.notification?.body}");
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
  final GlobalKey<ScaffoldMessengerState> _scaffoldMessengerKey = GlobalKey<ScaffoldMessengerState>();
  final LiveActivities _live = LiveActivities();
  String? _latestActivityId;
  // iOS-д ActivityKit-д тодорхойлсон ActivityAttributes нэр (extension-д яг энэ нэрээр байх ёстой)
  final String _attributesType = 'LiveActivitiesAppAttributes';

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

    // OneSignal init: replace with your actual OneSignal App ID
    OneSignal.Debug.setLogLevel(OSLogLevel.verbose);
    OneSignal.initialize('be13a59a-95c4-43c5-b104-43d3b3f1921d');
    // Request user permission (Android 13+ / iOS)
    OneSignal.Notifications.requestPermission(true);
    // Log subscription state & playerId for testing
    OneSignal.User.pushSubscription.addObserver((state) {
      debugPrint('OneSignal subscribed: ${state.current.optedIn}, playerId: ${state.current.id}');
    });
    // Log initial state via v5 properties
    debugPrint('OneSignal initial subscribed: ${OneSignal.User.pushSubscription.optedIn}, playerId: ${OneSignal.User.pushSubscription.id}');
    OneSignal.Notifications.addForegroundWillDisplayListener((event) {
      debugPrint('🔔 Foreground notification received: ${event.notification.title}');
      // Prevent default OneSignal notification
      event.preventDefault();

      // Show custom notification instead
      final data = event.notification.additionalData ?? {};
      final title = event.notification.title ?? (data['title'] ?? 'Ride');
      final status = data['status'] ?? event.notification.body ?? 'Driver arriving...';
      final eta = data['eta'] ?? '5 min';

      debugPrint('📱 Showing custom notification: $title - $status');
      platform.invokeMethod('showCustomNotification', {
        'title': title,
        'status': status,
        'eta': eta,
      }).then((result) {
        debugPrint('✅ Custom notification shown: $result');
      }).catchError((error) {
        debugPrint('❌ Failed to show custom notification: $error');
      });

      // Handle action
      final action = data['action'];
      if (action == 'start') {
        _startService(data['driver'] ?? 'Unknown');
      } else if (action == 'update') {
        _updateService(data['status'] ?? status, data['eta'] ?? eta);
      } else if (action == 'stop') {
        _stopService();
      }
    });
    OneSignal.Notifications.addClickListener((event) {
      debugPrint('👆 Notification clicked: ${event.notification.title}');
      final data = event.notification.additionalData ?? {};

      // Show custom notification when clicked from background
      final title = event.notification.title ?? (data['title'] ?? 'Ride');
      final status = data['status'] ?? event.notification.body ?? 'Driver arriving...';
      final eta = data['eta'] ?? '5 min';

      platform.invokeMethod('showCustomNotification', {
        'title': title,
        'status': status,
        'eta': eta,
      });

      // Handle action
      final action = data['action'];
      if (action == 'start') {
        _startService(data['driver'] ?? 'Unknown');
      } else if (action == 'update') {
        _updateService(status, eta);
      } else if (action == 'stop') {
        _stopService();
      }
    });

    // Handle incoming FCM messages while app is in foreground
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('Message received: ${message.notification?.title}');
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

    // Initialize live_activities (iOS requires appGroupId; Android safely ignores)
    _initLiveActivities();

    // Listen for native notification button actions forwarded via MethodChannel
    const nativeChannel = MethodChannel('com.example.foreground/service');
    nativeChannel.setMethodCallHandler((call) async {
      if (call.method == 'onNotificationAction') {
        final args = call.arguments as Map?;
        final action = args != null ? args['action'] as String? : null;
        print('Notification action from native: $action');
        if (action == 'stop') {
          await _stopService();
        } else if (action == 'call') {
          // Example: show a snackbar or handle call action
          _scaffoldMessengerKey.currentState?.showSnackBar(const SnackBar(content: Text('Call action tapped')));
        } else if (action == 'navigate') {
          _scaffoldMessengerKey.currentState?.showSnackBar(const SnackBar(content: Text('Navigate action tapped')));
        }
      }
    });
  }

  Future<void> _initLiveActivities() async {
    try {
      await _live.init(appGroupId: 'group.your.app');
      // Listen activity updates to keep push token / ids (optional)
      _live.activityUpdateStream.listen((event) {
        event.map(
          active: (a) {
            _latestActivityId = a.activityId;
            debugPrint('LiveActivity active: id=${a.activityId}, token=${a.activityToken}');
          },
          ended: (a) {
            debugPrint('LiveActivity ended: id=${a.activityId}');
          },
          unknown: (a) {
            debugPrint('LiveActivity unknown: id=${a.activityId}');
          },
          stale: (a) {
            debugPrint('LiveActivity stale: id=${a.activityId}');
          },
        );
      });
    } catch (e) {
      debugPrint('live_activities init failed: $e');
    }
  }

  Future<void> _createLiveActivity() async {
    try {
      final id = await _live.createActivity(
        _attributesType,
        {
          'matchName': 'Derby',
          'matchStartDate': DateTime.now().millisecondsSinceEpoch,
          'teamAName': 'Team A',
          'teamAScore': 0,
          'teamBName': 'Team B',
          'teamBScore': 0,
          // Optional images
          //'teamAImageUrl': 'https://example.com/a.png',
          //'teamBImageUrl': 'https://example.com/b.png',
        },
      );
      _latestActivityId = id;
      _scaffoldMessengerKey.currentState?.showSnackBar(SnackBar(content: Text('Live Activity created: $id')));
    } catch (e) {
      _scaffoldMessengerKey.currentState?.showSnackBar(SnackBar(content: Text('Create failed: $e')));
    }
  }

  Future<void> _updateLiveActivity() async {
    try {
      final id = _latestActivityId;
      if (id == null) {
        _scaffoldMessengerKey.currentState?.showSnackBar(const SnackBar(content: Text('No activity to update')));
        return;
      }
      await _live.updateActivity(id, {
        'teamAScore': 1,
        'teamBScore': 0,
      });
      _scaffoldMessengerKey.currentState?.showSnackBar(SnackBar(content: Text('Live Activity updated: $id')));
    } catch (e) {
      _scaffoldMessengerKey.currentState?.showSnackBar(SnackBar(content: Text('Update failed: $e')));
    }
  }

  Future<void> _endLiveActivity() async {
    try {
      final id = _latestActivityId;
      if (id == null) {
        _scaffoldMessengerKey.currentState?.showSnackBar(const SnackBar(content: Text('No activity to end')));
        return;
      }
      await _live.endActivity(id);
      _scaffoldMessengerKey.currentState?.showSnackBar(SnackBar(content: Text('Live Activity ended: $id')));
      _latestActivityId = null;
    } catch (e) {
      _scaffoldMessengerKey.currentState?.showSnackBar(SnackBar(content: Text('End failed: $e')));
    }
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
      scaffoldMessengerKey: _scaffoldMessengerKey,
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
              const Divider(height: 32),
              const Text('Live Activities (plugin)', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _createLiveActivity,
                child: const Text('Create Live Activity'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _updateLiveActivity,
                child: const Text('Update Live Activity'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _endLiveActivity,
                child: const Text('End Live Activity'),
              ),
              const Divider(height: 32),
              const Text('Push Notifications', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Builder(
                builder: (context) => ElevatedButton.icon(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const SendNotificationScreen()),
                    );
                  },
                  icon: const Icon(Icons.send),
                  label: const Text('Send Push Notification'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orange,
                    foregroundColor: Colors.white,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
