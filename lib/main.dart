import 'package:flutter/material.dart';
import 'package:onesignal_flutter/onesignal_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final GlobalKey<ScaffoldMessengerState> _scaffoldMessengerKey = GlobalKey<ScaffoldMessengerState>();

  // TODO: Replace with your OneSignal App ID
  static const String _oneSignalAppId = 'be13a59a-95c4-43c5-b104-43d3b3f1921d';

  @override
  void initState() {
    super.initState();

    OneSignal.Debug.setLogLevel(OSLogLevel.verbose);
    OneSignal.initialize(_oneSignalAppId);
    OneSignal.Notifications.requestPermission(true);

    OneSignal.User.pushSubscription.addObserver((state) {
      debugPrint('OneSignal push optedIn=${state.current.optedIn} id=${state.current.id}');
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OneSignal Job Notifications',
      scaffoldMessengerKey: _scaffoldMessengerKey,
      home: Scaffold(
        appBar: AppBar(title: const Text('OneSignal (Job Only)')),
        body: const Padding(
          padding: EdgeInsets.all(16),
          child: Text(
            'Flutter only initializes OneSignal.\n\n'
            'Foreground + background custom job notifications are rendered in Kotlin '
            '(NotificationServiceExtension + foreground lifecycle listener).\n\n'
            'Set YOUR_ONESIGNAL_APP_ID in lib/main.dart.',
          ),
        ),
      ),
    );
  }
}
