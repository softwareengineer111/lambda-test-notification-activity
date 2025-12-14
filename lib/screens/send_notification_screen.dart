import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class SendNotificationScreen extends StatefulWidget {
  const SendNotificationScreen({Key? key}) : super(key: key);

  @override
  State<SendNotificationScreen> createState() => _SendNotificationScreenState();
}

class _SendNotificationScreenState extends State<SendNotificationScreen> {
  final _companyController = TextEditingController(text: 'Ламбда ХХК');
  final _jobTitleController = TextEditingController(text: 'Мобайл аппликейшн хөгжүүлэгч');
  final _descriptionController = TextEditingController(text: 'iOS болон Android аппликейшн хөгжүүлэх');
  final _imageUrlController = TextEditingController(text: 'https://picsum.photos/200');
  String _selectedAction = 'update';
  bool _sending = false;

  // Replace with your OneSignal REST API Key from Settings > Keys & IDs
  final String _oneSignalApiKey = 'be13a59a-95c4-43c5-b104-43d3b3f1921d';
  final String _oneSignalAppId = 'be13a59a-95c4-43c5-b104-43d3b3f1921d';

  @override
  void dispose() {
    _companyController.dispose();
    _jobTitleController.dispose();
    _descriptionController.dispose();
    _imageUrlController.dispose();
    super.dispose();
  }

  Future<void> _sendNotification() async {
    if (_oneSignalApiKey == 'YOUR_ONESIGNAL_REST_API_KEY') {
      _showError('Please set your OneSignal REST API Key in the code');
      return;
    }

    setState(() => _sending = true);

    try {
      final company = _companyController.text.isEmpty ? 'Компани' : _companyController.text;
      final jobTitle = _jobTitleController.text.isEmpty ? 'Ажлын байр' : _jobTitleController.text;
      final description = _descriptionController.text.isEmpty ? 'Албан тушаал зарлагдлаа' : _descriptionController.text;
      final imageUrl = _imageUrlController.text.isEmpty ? 'https://picsum.photos/200' : _imageUrlController.text;

      // Build OneSignal notification payload matching user's structure
      final payload = {
        'app_id': _oneSignalAppId,
        'included_segments': ['All'],
        'headings': {'en': company},
        'contents': {'en': jobTitle},
        'content_available': true,
        'mutable_content': true,
        'data': {
          'type': 'job',
          'company': company,
          'jobTitle': jobTitle,
          'description': description,
          'companyImageUrl': imageUrl,
          'action': _selectedAction,
          'ride_id': 123,
          'eta': '5 min',
          'driver': 'Ariun',
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
              controller: _companyController,
              decoration: const InputDecoration(
                labelText: 'Company Name',
                hintText: 'Ламбда ХХК',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _jobTitleController,
              decoration: const InputDecoration(
                labelText: 'Job Title',
                hintText: 'Мобайл аппликейшн хөгжүүлэгч',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _descriptionController,
              decoration: const InputDecoration(
                labelText: 'Description',
                hintText: 'iOS болон Android аппликейшн хөгжүүлэх',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _imageUrlController,
              decoration: const InputDecoration(
                labelText: 'Company Logo URL',
                hintText: 'https://example.com/logo.png',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.url,
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
