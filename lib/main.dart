import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:onesignal_flutter/onesignal_flutter.dart';
import 'dart:async';

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

  String? _pushSubscriptionId;

  Timer? _pollTimer;

  // TODO: Replace with your OneSignal App ID
  static const String _oneSignalAppId = 'be13a59a-95c4-43c5-b104-43d3b3f1921d';

  @override
  void initState() {
    super.initState();

    OneSignal.Debug.setLogLevel(OSLogLevel.verbose);
    OneSignal.initialize(_oneSignalAppId);
    OneSignal.Notifications.requestPermission(true);

    OneSignal.User.pushSubscription.addObserver((state) {
      final id = state.current.id;
      debugPrint('OneSignal push optedIn=${state.current.optedIn} id=$id');
      if (mounted) {
        setState(() {
          _pushSubscriptionId = id;
        });
      }
    });

    // In some cases (especially on first install) the observer may not fire
    // immediately. Do a delayed fetch + a short retry loop.
    Future.delayed(const Duration(seconds: 1), _refreshPushSubscriptionId);

    _pollTimer?.cancel();
    var triesLeft = 8; // ~8 seconds max
    _pollTimer = Timer.periodic(const Duration(seconds: 1), (t) {
      triesLeft -= 1;
      _refreshPushSubscriptionId();
      if ((_pushSubscriptionId != null && _pushSubscriptionId!.isNotEmpty) || triesLeft <= 0) {
        t.cancel();
      }
    });
  }

  void _refreshPushSubscriptionId() {
    try {
      // onesignal_flutter v5: subscription state is available under User.pushSubscription
      final id = OneSignal.User.pushSubscription.id;
      final optedIn = OneSignal.User.pushSubscription.optedIn;
      debugPrint('OneSignal poll optedIn=$optedIn id=$id');
      if (mounted && id != null && id.isNotEmpty && id != _pushSubscriptionId) {
        setState(() {
          _pushSubscriptionId = id;
        });
      }
    } catch (e) {
      debugPrint('OneSignal poll error: $e');
    }
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OneSignal Job Notifications',
      scaffoldMessengerKey: _scaffoldMessengerKey,
      home: Scaffold(
        appBar: AppBar(title: const Text('OneSignal (Job Only)')),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Flutter only initializes OneSignal.\n\n'),
              const SizedBox(height: 16),
              const Text('Push Subscription ID ("playerId"):', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              SelectableText(_pushSubscriptionId ?? 'Waiting for OneSignal...'),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: (_pushSubscriptionId == null || _pushSubscriptionId!.isEmpty)
                    ? null
                    : () async {
                        await Clipboard.setData(ClipboardData(text: _pushSubscriptionId!));
                        _scaffoldMessengerKey.currentState?.showSnackBar(
                          const SnackBar(content: Text('Copied Push Subscription ID')),
                        );
                      },
                child: const Text('Copy ID'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
