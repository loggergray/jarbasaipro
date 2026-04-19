import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const _channel = MethodChannel('com.jarbas.app/control');

  bool _jarbasAtivo = false;
  bool _accessibilityOk = false;
  String _status = 'Jarbas inativo';

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    final accessOk = await _channel.invokeMethod<bool>('isAccessibilityEnabled') ?? false;
    setState(() => _accessibilityOk = accessOk);
  }

  Future<void> _requestAllPermissions() async {
    await [
      Permission.microphone,
      Permission.phone,
      Permission.contacts,
    ].request();

    final accessOk = await _channel.invokeMethod<bool>('isAccessibilityEnabled') ?? false;
    if (!accessOk) {
      _showAccessibilityDialog();
    } else {
      setState(() => _accessibilityOk = true);
    }
  }

  void _showAccessibilityDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A2E),
        title: const Text('Permissao necessaria',
            style: TextStyle(color: Colors.white)),
        content: const Text(
          'Para o Jarbas controlar o celular por voz, ative o servico de Acessibilidade do Jarbas nas configuracoes.',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _channel.invokeMethod('openAccessibilitySettings');
            },
            child: const Text('Abrir Configuracoes',
                style: TextStyle(color: Color(0xFF00D4FF))),
          ),
        ],
      ),
    );
  }

  Future<void> _iniciarJarbas() async {
    if (!_accessibilityOk) {
      await _requestAllPermissions();
      return;
    }
    await _channel.invokeMethod('startJarbas');
    setState(() {
      _jarbasAtivo = true;
      _status = 'Jarbas ouvindo... Diga "Jarbas" para comecar';
    });
  }

  Future<void> _encerrarJarbas() async {
    await _channel.invokeMethod('stopJarbas');
    setState(() {
      _jarbasAtivo = false;
      _status = 'Jarbas inativo';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0D0D1A),
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'JARBAS',
                style: TextStyle(
                  fontSize: 48,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF00D4FF),
                  letterSpacing: 8,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'Assistente de Voz',
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.white54,
                  letterSpacing: 2,
                ),
              ),
              const SizedBox(height: 60),
              AnimatedContainer(
                duration: const Duration(milliseconds: 500),
                width: 180,
                height: 180,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: _jarbasAtivo
                      ? const Color(0xFF00D4FF).withOpacity(0.15)
                      : Colors.white.withOpacity(0.05),
                  border: Border.all(
                    color: _jarbasAtivo
                        ? const Color(0xFF00D4FF)
                        : Colors.white24,
                    width: 2,
                  ),
                  boxShadow: _jarbasAtivo
                      ? [
                          BoxShadow(
                            color: const Color(0xFF00D4FF).withOpacity(0.3),
                            blurRadius: 30,
                            spreadRadius: 5,
                          )
                        ]
                      : [],
                ),
                child: Icon(
                  Icons.mic,
                  size: 80,
                  color: _jarbasAtivo
                      ? const Color(0xFF00D4FF)
                      : Colors.white30,
                ),
              ),
              const SizedBox(height: 40),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 40),
                child: Text(
                  _status,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    color: _jarbasAtivo
                        ? const Color(0xFF00D4FF)
                        : Colors.white38,
                  ),
                ),
              ),
              const SizedBox(height: 60),
              if (!_jarbasAtivo)
                ElevatedButton(
                  onPressed: _iniciarJarbas,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF00D4FF),
                    foregroundColor: const Color(0xFF0D0D1A),
                    padding: const EdgeInsets.symmetric(
                        horizontal: 60, vertical: 18),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(50)),
                  ),
                  child: const Text(
                    'INICIAR JARBAS',
                    style: TextStyle(
                        fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                )
              else
                ElevatedButton(
                  onPressed: _encerrarJarbas,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red.shade700,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(
                        horizontal: 60, vertical: 18),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(50)),
                  ),
                  child: const Text(
                    'ENCERRAR JARBAS',
                    style: TextStyle(
                        fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                ),
              const SizedBox(height: 20),
              if (!_accessibilityOk)
                TextButton(
                  onPressed: () =>
                      _channel.invokeMethod('openAccessibilitySettings'),
                  child: const Text(
                    'Ativar Acessibilidade',
                    style: TextStyle(color: Colors.orange),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
