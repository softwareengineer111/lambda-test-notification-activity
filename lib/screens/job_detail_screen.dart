import 'package:flutter/material.dart';

class JobDetailScreen extends StatefulWidget {
  final String company;
  final String jobTitle;
  final String description;
  final String? companyImageUrl;
  final String? jobUrl;

  const JobDetailScreen({
    Key? key,
    required this.company,
    required this.jobTitle,
    required this.description,
    this.companyImageUrl,
    this.jobUrl,
  }) : super(key: key);

  @override
  State<JobDetailScreen> createState() => _JobDetailScreenState();
}

class _JobDetailScreenState extends State<JobDetailScreen> {
  bool _isSaved = false;

  void _sendCV() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('CV илгээх хүсэлт илгээлээ: ${widget.company}'),
        backgroundColor: Colors.green,
        action: SnackBarAction(
          label: 'ОК',
          textColor: Colors.white,
          onPressed: () {},
        ),
      ),
    );
    // TODO: Implement actual CV sending logic
  }

  void _toggleSave() {
    setState(() {
      _isSaved = !_isSaved;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(_isSaved ? 'Хадгалсан' : 'Хадгалалтаас хассан'),
        backgroundColor: _isSaved ? Colors.blue : Colors.grey,
        duration: const Duration(seconds: 1),
      ),
    );
    // TODO: Implement actual save/unsave logic (local storage or API)
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Ажлын байрны дэлгэрэнгүй'),
        actions: [
          IconButton(
            icon: Icon(_isSaved ? Icons.bookmark : Icons.bookmark_border),
            onPressed: _toggleSave,
            tooltip: _isSaved ? 'Хадгалсан' : 'Хадгалах',
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Company header with logo
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    Colors.blue.shade50,
                    Colors.white,
                  ],
                ),
              ),
              child: Column(
                children: [
                  if (widget.companyImageUrl != null)
                    Container(
                      width: 80,
                      height: 80,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.1),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ],
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(12),
                        child: Image.network(
                          widget.companyImageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) {
                            return Container(
                              color: Colors.grey.shade200,
                              child: const Icon(Icons.business, size: 40),
                            );
                          },
                        ),
                      ),
                    )
                  else
                    Container(
                      width: 80,
                      height: 80,
                      decoration: BoxDecoration(
                        color: Colors.blue.shade100,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(Icons.business, size: 40, color: Colors.blue),
                    ),
                  const SizedBox(height: 16),
                  Text(
                    widget.company,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    widget.jobTitle,
                    style: TextStyle(
                      fontSize: 18,
                      color: Colors.grey.shade700,
                      fontWeight: FontWeight.w500,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Job details section
            Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Ажлын тайлбар',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    widget.description,
                    style: TextStyle(
                      fontSize: 16,
                      height: 1.6,
                      color: Colors.grey.shade800,
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Additional job info
                  _buildInfoCard(
                    icon: Icons.work_outline,
                    title: 'Ажлын төрөл',
                    value: 'Бүтэн цагийн ажил',
                  ),
                  const SizedBox(height: 12),
                  _buildInfoCard(
                    icon: Icons.location_on_outlined,
                    title: 'Байршил',
                    value: 'Улаанбаатар',
                  ),
                  const SizedBox(height: 12),
                  _buildInfoCard(
                    icon: Icons.access_time,
                    title: 'Зарласан огноо',
                    value: 'Өнөөдөр',
                  ),

                  const SizedBox(height: 32),

                  // Action buttons
                  Row(
                    children: [
                      Expanded(
                        flex: 2,
                        child: ElevatedButton.icon(
                          onPressed: _sendCV,
                          icon: const Icon(Icons.send),
                          label: const Text('CV Илгээх'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.blue,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: _toggleSave,
                          icon: Icon(_isSaved ? Icons.bookmark : Icons.bookmark_border),
                          label: Text(_isSaved ? 'Хадгалсан' : 'Хадгалах'),
                          style: OutlinedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            side: BorderSide(color: Colors.blue.shade300),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoCard({
    required IconData icon,
    required String title,
    required String value,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.blue.shade50,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: Colors.blue, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey.shade600,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
