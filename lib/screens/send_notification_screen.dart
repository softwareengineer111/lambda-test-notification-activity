import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class SendNotificationScreen extends StatefulWidget {
  const SendNotificationScreen({Key? key}) : super(key: key);

  @override
  State<SendNotificationScreen> createState() => _SendNotificationScreenState();
}

class _SendNotificationScreenState extends State<SendNotificationScreen> {
  final _titleController = TextEditingController();
  final _statusController = TextEditingController();
  final _etaController = TextEditingController(text: '5 min');
  final _driverController = TextEditingController(text: 'Ariun');
  String _selectedAction = 'update';
  bool _sending = false;

  // Replace with your OneSignal REST API Key from Settings > Keys & IDs
  final String _oneSignalApiKey = 'be13a59a-95c4-43c5-b104-43d3b3f1921d';
  final String _oneSignalAppId = 'be13a59a-95c4-43c5-b104-43d3b3f1921d';

  @override
  void dispose() {
    _titleController.dispose();
    _statusController.dispose();
    _etaController.dispose();
    _driverController.dispose();
    super.dispose();
  }

  Future<void> _sendNotification() async {
    if (_oneSignalApiKey == 'YOUR_ONESIGNAL_REST_API_KEY') {
      _showError('Please set your OneSignal REST API Key in the code');
      return;
    }

    setState(() => _sending = true);

    try {
      final title = _titleController.text.isEmpty ? 'Ride Update' : _titleController.text;
      final status = _statusController.text.isEmpty ? 'Driver arriving...' : _statusController.text;
      final eta = _etaController.text.isEmpty ? '5 min' : _etaController.text;
      final driver = _driverController.text.isEmpty ? 'Driver' : _driverController.text;

      // Build OneSignal notification payload
      final payload = {
        'app_id': _oneSignalAppId,
        'included_segments': ['All'], // Send to all subscribed users
        'headings': {'en': title},
        'contents': {'en': status},
        'data': {
          'action': _selectedAction,
          'status': status,
          'eta': eta,
          'driver': driver,
          'title': title,
        },
      };

      final response = await http.post(
        Uri.parse('https://onesignal.com/api/v1/notifications'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Basic $_oneSignalApiKey',
        },
        body: jsonEncode(payload),
      );

      if (response.statusCode == 200) {
        final result = jsonDecode(response.body);
        _showSuccess('Notification sent! Recipients: ${result['recipients']}');
      } else {
        _showError('Failed: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      _showError('Error: $e');
    } finally {
      setState(() => _sending = false);
    }
  }

  void _showSuccess(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.green),
    );
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Send Push Notification')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: 'Title',
                hintText: 'Ride Update',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _statusController,
              decoration: const InputDecoration(
                labelText: 'Status',
                hintText: 'Driver arriving...',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _etaController,
              decoration: const InputDecoration(
                labelText: 'ETA',
                hintText: '5 min',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _driverController,
              decoration: const InputDecoration(
                labelText: 'Driver Name',
                hintText: 'Ariun',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              value: _selectedAction,
              decoration: const InputDecoration(
                labelText: 'Action',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'start', child: Text('Start Ride')),
                DropdownMenuItem(value: 'update', child: Text('Update Ride')),
                DropdownMenuItem(value: 'stop', child: Text('Stop Ride')),
              ],
              onChanged: (value) {
                setState(() => _selectedAction = value ?? 'update');
              },
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _sending ? null : _sendNotification,
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(16),
              ),
              child: _sending ? const CircularProgressIndicator() : const Text('Send Notification', style: TextStyle(fontSize: 16)),
            ),
            const SizedBox(height: 16),
            const Card(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Setup Instructions:',
                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                    SizedBox(height: 8),
                    Text('1. Go to OneSignal Dashboard'),
                    Text('2. Settings > Keys & IDs'),
                    Text('3. Copy REST API Key'),
                    Text('4. Paste in send_notification_screen.dart'),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
