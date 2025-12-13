import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:live_activities/live_activities.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:firebase_app_check/firebase_app_check.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'screens/send_notification.dart';

Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();

  // Enable Firebase App Check (required if enforceAppCheck: true)
  await FirebaseAppCheck.instance.activate();
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

  // Initialize local notifications for foreground display on Android/iOS
  await _Notifications.init();

  // App Check: use provided debug token if passed via dart-define, else default
  const String kDebugAppCheckToken = String.fromEnvironment('FIREBASE_APPCHECK_DEBUG_TOKEN');
  if (kDebugAppCheckToken.isNotEmpty) {
    await FirebaseAppCheck.instance.activate(androidProvider: AndroidProvider.debug, appleProvider: AppleProvider.debug);
  } else {
    await FirebaseAppCheck.instance.activate();
  }

  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

  // Request notification permission on Android 13+
  await _requestNotificationPermission();

  runApp(const MyApp());
}

class _Notifications {
  static final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  static const AndroidNotificationChannel _channel = AndroidNotificationChannel(
    'live_activity_channel',
    'Live Activity Alerts',
    description: 'Shows heads-up notifications for ride status',
    importance: Importance.high,
  );

  static Future<void> init() async {
    const AndroidInitializationSettings androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    final DarwinInitializationSettings iosInit = const DarwinInitializationSettings(
      requestSoundPermission: true,
      requestBadgePermission: true,
      requestAlertPermission: true,
    );
    final InitializationSettings settings = InitializationSettings(android: androidInit, iOS: iosInit);
    await _plugin.initialize(settings, onDidReceiveNotificationResponse: (NotificationResponse response) async {
      // Forward actions via MethodChannel if desired
      const nativeChannel = MethodChannel('com.example.foreground/service');
      await nativeChannel.invokeMethod('onNotificationAction', {'action': response.payload});
    });

    // Android channel setup
    await _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()?.createNotificationChannel(_channel);
  }

  static Future<void> show({required String title, required String body, String? payload}) async {
    final androidDetails = AndroidNotificationDetails(
      _channel.id,
      _channel.name,
      channelDescription: _channel.description,
      importance: Importance.high,
      priority: Priority.high,
      category: AndroidNotificationCategory.call,
      styleInformation: const BigTextStyleInformation(''),
      ticker: 'ride_status',
    );
    const iosDetails = DarwinNotificationDetails(presentAlert: true, presentSound: true, presentBadge: false);
    final details = NotificationDetails(android: androidDetails, iOS: iosDetails);
    await _plugin.show(DateTime.now().millisecondsSinceEpoch ~/ 1000, title, body, details, payload: payload);
  }
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

    // Handle incoming FCM messages while app is in foreground
    FirebaseMessaging.onMessage.listen((RemoteMessage message) async {
      print('Message received: ${message.notification?.title}');
      final data = message.data;

      // Show a local notification even when app is in foreground
      await _Notifications.show(
        title: message.notification?.title ?? data['title'] ?? 'Update',
        body: message.notification?.body ?? data['body'] ?? 'Tap to view',
        payload: data['action'] ?? 'open',
      );

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
              Builder(
                builder: (ctx) => ElevatedButton(
                  onPressed: () {
                    Navigator.of(ctx).push(
                      MaterialPageRoute(builder: (_) => const SendNotificationScreen()),
                    );
                  },
                  child: const Text('Open Send Notification Form'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
