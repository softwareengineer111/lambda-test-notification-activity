import 'package:flutter/material.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:firebase_app_check/firebase_app_check.dart';
import 'package:firebase_auth/firebase_auth.dart';

class SendNotificationScreen extends StatefulWidget {
  const SendNotificationScreen({Key? key}) : super(key: key);

  @override
  State<SendNotificationScreen> createState() => _SendNotificationScreenState();
}

class _SendNotificationScreenState extends State<SendNotificationScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _tokenCtrl = TextEditingController();
  final TextEditingController _driverCtrl = TextEditingController();
  final TextEditingController _textCtrl = TextEditingController();
  final TextEditingController _titleCtrl = TextEditingController(text: 'Ride update');
  String _action = 'start';
  bool _loading = false;
  String? _result;

  @override
  void dispose() {
    _tokenCtrl.dispose();
    _driverCtrl.dispose();
    _textCtrl.dispose();
    _titleCtrl.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _prefillToken();
  }

  Future<void> _prefillToken() async {
    try {
      final token = await FirebaseMessaging.instance.getToken();
      if (token != null && token.isNotEmpty) {
        _tokenCtrl.text = token;
        setState(() {});
      }
    } catch (e) {
      // Ignore errors, user can paste token manually
    }
  }

  Future<void> _ensureSignedIn() async {
    final auth = FirebaseAuth.instance;
    if (auth.currentUser == null) {
      await auth.signInAnonymously();
    }
  }

  Future<void> sendRide({
    required String driver,
    required String text,
    String action = 'start',
    String title = 'Ride update',
  }) async {
    if (FirebaseAuth.instance.currentUser == null) {
      await FirebaseAuth.instance.signInAnonymously();
    }
    await FirebaseAppCheck.instance.activate();

    final token = await FirebaseMessaging.instance.getToken();
    if (token == null) throw Exception('FCM token unavailable');

    final callable = FirebaseFunctions.instanceFor(region: 'us-central1').httpsCallable('sendRideNotification');

    final res = await callable.call({
      'token': token,
      'driver': driver,
      'text': text,
      'action': action,
      'title': title,
    });
    // res.data -> { success: true, action: ... }
  }

  Future<void> _send() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _result = null;
    });
    try {
      await _ensureSignedIn();
      // Cloud Functions region must match deployment (us-central1)
      final functions = FirebaseFunctions.instanceFor(region: 'us-central1');
      final callable = functions.httpsCallable('sendRideNotification');
      final resp = await callable.call(<String, dynamic>{
        'token': _tokenCtrl.text.trim(),
        'driver': _driverCtrl.text.trim(),
        'text': _textCtrl.text.trim(),
        'action': _action,
        'title': _titleCtrl.text.trim(),
      });
      setState(() {
        _result = 'Амжилттай: ${resp.data}';
      });
    } on FirebaseFunctionsException catch (e) {
      setState(() {
        _result = 'Function алдаа: ${e.code} ${e.message}';
      });
    } catch (e) {
      setState(() {
        _result = 'Алдаа: $e';
      });
    } finally {
      setState(() {
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Push илгээх (Callable)')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _tokenCtrl,
                decoration: const InputDecoration(labelText: 'FCM token (хүлээн авагч)'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Token шаардлагатай' : null,
                maxLines: 2,
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: _prefillToken,
                  child: const Text('Миний token-ийг автоматаар авах'),
                ),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _driverCtrl,
                decoration: const InputDecoration(labelText: 'Driver нэр'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Driver шаардлагатай' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _textCtrl,
                decoration: const InputDecoration(labelText: 'Мессеж (text)'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Text шаардлагатай' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _titleCtrl,
                decoration: const InputDecoration(labelText: 'Title (сонголттой)'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                value: _action,
                items: const [
                  DropdownMenuItem(value: 'start', child: Text('start')),
                  DropdownMenuItem(value: 'update', child: Text('update')),
                  DropdownMenuItem(value: 'stop', child: Text('stop')),
                ],
                onChanged: (v) {
                  if (v != null) setState(() => _action = v);
                },
                decoration: const InputDecoration(labelText: 'Action'),
              ),
              const SizedBox(height: 20),
              _loading
                  ? const CircularProgressIndicator()
                  : ElevatedButton(
                      onPressed: _send,
                      child: const Text('Push илгээх'),
                    ),
              const SizedBox(height: 16),
              if (_result != null) SelectableText(_result!),
            ],
          ),
        ),
      ),
    );
  }
}
