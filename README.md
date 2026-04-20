# Jarbas - Assistente de Voz para Android

## Descrição
Jarbas é um assistente de voz completo para dispositivos Android, focado em acessibilidade. Permite controlar o dispositivo por voz, abrir apps, fazer ligações, pesquisar na web e muito mais.

## Funcionalidades Atuais
- **Reconhecimento de Voz**: Escuta comandos começando com "Jarbas"
- **Abrir Apps**: Abre WhatsApp, YouTube e outros apps
- **Tocar Música**: Abre YouTube e toca músicas
- **Fazer Ligações**: Liga para contatos da agenda
- **Pesquisa Web**: Busca informações usando DuckDuckGo
- **Pular Anúncios**: Automaticamente pula anúncios no YouTube
- **Desbloquear Tela**: Acorda a tela e digita senha
- **Navegação**: Volta (back) e vai para tela inicial (home)
- **SOS**: Liga para contato de emergência ou SAMU- **Ler Tela**: Descreve textos e botões visíveis
- **Digitar Texto**: Digita em campos ativos e tenta enviar
- **Abrir Conversas no WhatsApp**: Abre chat específico por nome
## Problemas Identificados e Motivos de Não Funcionar

### 1. **Serviço Para de Funcionar Após Algum Tempo**
   - **Causa**: Otimização de bateria do Android mata serviços em background.
   - **Solução**: Adicionar isenção de otimização de bateria. O código já tem método para abrir configurações, mas não é chamado automaticamente.

### 2. **Reconhecimento de Voz Falha**
   - **Causa**: Dependente da API do Google Speech. Pode falhar com ruído, sotaque ou sem internet.
   - **Solução**: Melhorar tratamento de erros e adicionar fallbacks.

### 3. **Desbloqueio de Tela Inconsistente**
   - **Causa**: Usa `GLOBAL_ACTION_BACK` para acordar tela, que não funciona em todos os dispositivos. Layouts de bloqueio variam.
   - **Solução**: Usar `DevicePolicyManager` ou detectar tipo de bloqueio.

### 4. **Não Abre Conversas ou Interage Profundamente**
   - **Causa**: Código limitado a abrir apps, não navega dentro deles.
   - **Solução**: Adicionar navegação por acessibilidade (ler tela, clicar em elementos específicos).

### 5. **Não Lê ou Entende Conteúdo da Tela**
   - **Causa**: Acessibilidade só usada para anúncios e senha. Não varre textos/botões.
   - **Solução**: Implementar leitura de tela completa.

### 6. **Flutter SDK Não Instalado**
   - **Causa**: Ambiente de desenvolvimento não tem Flutter.
   - **Solução**: Instalar Flutter ou usar Android Studio para build.

### 7. **Dependências Não Utilizadas**
   - **Causa**: pubspec.yaml tem `speech_to_text` e `flutter_tts`, mas código usa APIs nativas do Android.
   - **Solução**: Unificar ou remover desnecessárias.

## Como Usar
1. Instale o APK no dispositivo Android.
2. Abra o app e conceda permissões (microfone, contatos, telefone, SMS).
3. Digite o número do contato de emergência (SOS) no campo.
4. Ative o serviço de acessibilidade em Configurações > Acessibilidade > Jarbas.
5. Desative otimização de bateria para o app.
6. Diga "Jarbas" seguido do comando.

## Comandos de Voz
- "Jarbas abre WhatsApp"
- "Jarbas abre WhatsApp com [nome]" (abre conversa específica)
- "Jarbas toca música de [artista]"
- "Jarbas pausa" (pausa música)
- "Jarbas próximo" (próximo vídeo)
- "Jarbas liga pra [nome]"
- "Jarbas pesquisa sobre [tópico]"
- "Jarbas acorda" (desbloqueia tela)
- "Jarbas socorro" (emergência)
- "Jarbas lê a tela" (descreve conteúdo)
- "Jarbas digita [texto]" (digita e tenta enviar)
- "Jarbas entrar no kore organiza" (abre web app no Chrome)
- "Jarbas login [usuário] senha [senha]" (faz login no web app)
- "Jarbas clica em [texto]" (clica em elemento com texto)

## Melhorias Planejadas para Funcionamento Granular
- **Leitura de Tela**: Comando "Jarbas lê a tela" para descrever elementos visíveis. ✅ Implementado
- **Digitação Avançada**: "Jarbas digita [texto]" em campos ativos. ✅ Implementado
- **Navegação em Apps**: Abrir conversas específicas no WhatsApp. ✅ Implementado
- **Controle de Mídia**: Play/pause, volume.
- **IA Integrada**: Entendimento de frases naturais usando NLP.
- **Persistência**: Reinício automático de serviços se mortos. ✅ Parcial (reinício via acessibilidade)

## Build e Instalação
- **Requisitos**: Android SDK, Flutter (se editar Dart), Gradle.
- **Build**: `flutter build apk` ou use Android Studio.
- **Teste**: Instale em dispositivo real para testar acessibilidade.

## Contribuição
Para melhorar, foque em robustez de serviços e expansão da acessibilidade.

## Licença
[Adicione licença se aplicável]