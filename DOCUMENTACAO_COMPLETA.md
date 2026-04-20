# JARBAS - Assistente de Voz Totalmente Acessível

## Visão Geral

Jarbas é um assistente de voz avançado para dispositivos Android, desenvolvido com foco total em acessibilidade e inclusão. Ele permite que pessoas com deficiências físicas, visuais, motoras ou cognitivas, além de idosos e qualquer usuário que prefira controle por voz, interajam completamente com seus smartphones de forma independente e intuitiva.

### Missão
Tornar a tecnologia móvel acessível a todos, quebrando barreiras físicas e capacitando usuários com limitações a viverem com autonomia digital.

### Público-Alvo
- **Pessoas com Deficiências Visuais** (cegos ou com baixa visão): Navegação por voz sem necessidade de toque na tela.
- **Pessoas com Deficiências Motoras** (paraplégicos, tetraplégicos, distrofia muscular): Controle total sem movimentos finos.
- **Pessoas com Deficiências Cognitivas** (demência, autismo): Interface simples e consistente por voz.
- **Idosos**: Facilita uso de smartphones para comunicação, lembretes e emergências.
- **Qualquer Usuário**: Pessoas ocupadas, com lesões temporárias ou preferência por hands-free.

## Funcionalidades Principais

### 1. Controle de Voz Completo
- **Ativação**: Sempre começa com "Jarbas" para evitar ativações acidentais.
- **Reconhecimento Contínuo**: Processa comandos em português brasileiro com alta precisão.
- **Feedback Auditivo**: Confirma todas as ações com voz clara e calma.

### 2. Gerenciamento de Apps e Navegação
- Abre qualquer app instalado (WhatsApp, YouTube, Chrome, etc.).
- Navega entre telas (voltar, home, recentes).
- Abre conversas específicas no WhatsApp por nome de contato.

### 3. Comunicação e Conectividade
- Faz ligações para contatos da agenda.
- Pesquisa na web usando DuckDuckGo (privacidade).
- Envia SMS de emergência (SOS) para contatos configurados.

### 4. Entretenimento e Mídia
- Toca músicas no YouTube por artista ou gênero.
- Controla reprodução (play/pause, próximo vídeo).
- Pula anúncios automaticamente.

### 5. Segurança e Emergência
- Desbloqueia tela e digita senha (PIN ou padrão).
- Configuração de contato SOS para emergências.
- Liga automaticamente para SAMU se necessário.

### 6. Leitura e Interação com Tela
- **Leitura de Tela**: Descreve elementos visíveis (textos, botões).
- **Digitação Inteligente**: Digita em campos ativos e envia automaticamente.
- **Cliques por Voz**: "Clica em [texto]" para interagir com botões/menus.

### 7. Integração com Web Apps
- Abre web apps no Chrome (ex: Kore Organiza).
- Faz login automático (usuário + senha).
- Navega e interage com elementos da página.

## Instalação e Configuração

### Requisitos do Sistema
- Android 8.0 (API 26) ou superior.
- Pelo menos 100MB de espaço livre.
- Microfone funcional.
- Conexão à internet para reconhecimento de voz.

### Passos de Instalação
1. **Baixe o APK**: Disponível no repositório GitHub (branch atual).
2. **Instale**: Permita instalação de fontes desconhecidas.
3. **Permissões Iniciais**:
   - Microfone: Para reconhecimento de voz.
   - Contatos: Para ligações e WhatsApp.
   - Telefone: Para chamadas.
   - SMS: Para SOS.
4. **Ative Acessibilidade**:
   - Vá em Configurações > Acessibilidade > Jarbas.
   - Ative o serviço (essencial para interação com tela).
5. **Desative Otimização de Bateria**:
   - Configurações > Apps > Jarbas > Bateria > Não otimizar.
   - Impede que o sistema mate o serviço.
6. **Configure Contato SOS**:
   - No app, digite o número no campo "Contato SOS".

### Primeira Execução
- Abra o app.
- Conceda todas as permissões solicitadas.
- Diga "Jarbas" para testar reconhecimento.
- Diga "Jarbas abre WhatsApp" para testar abertura de app.

## Guia de Uso

### Comandos Básicos
- **Iniciar**: "Jarbas" (ativa escuta).
- **Apps**: "Jarbas abre [app]" (ex: "Jarbas abre WhatsApp").
- **Conversas**: "Jarbas abre WhatsApp com [nome]" (abre chat específico).
- **Ligações**: "Jarbas liga pra [nome]".
- **Música**: "Jarbas toca música de [artista]".
- **Controle Música**: "Jarbas pausa", "Jarbas próximo".
- **Pesquisa**: "Jarbas pesquisa sobre [tópico]".
- **Emergência**: "Jarbas socorro".
- **Navegação**: "Jarbas volta", "Jarbas home".
- **Leitura**: "Jarbas lê a tela".
- **Digitação**: "Jarbas digita [texto]".
- **Web App**: "Jarbas entrar no kore organiza".
- **Login Web**: "Jarbas login [user] senha [pass]".
- **Cliques**: "Jarbas clica em [texto]".

### Dicas de Uso
- Fale claramente e próximo ao microfone.
- Use nomes completos para contatos.
- Para web apps, aguarde carregamento antes de comandos.
- Em caso de erro, repita o comando.

## Arquitetura Técnica

### Componentes Principais
1. **Flutter App (Dart)**: Interface UI e comunicação com Android.
2. **Foreground Service (Kotlin)**: Processa voz e comandos.
3. **Accessibility Service (Kotlin)**: Interage com tela e apps.
4. **Boot Receiver**: Reinicia serviço após reboot.

### Tecnologias Utilizadas
- **Reconhecimento de Voz**: Android SpeechRecognizer (Google API).
- **Síntese de Voz**: Android TextToSpeech (TTS).
- **Acessibilidade**: Android Accessibility API.
- **Permissões**: Android Permission System.
- **Persistência**: SharedPreferences para configurações.

### Fluxo de Funcionamento
1. Usuário fala "Jarbas [comando]".
2. Foreground Service reconhece voz.
3. Processa comando e chama Accessibility Service.
4. Accessibility executa ação (toque, digitação, etc.).
5. Feedback auditivo confirma ação.

### Segurança
- Dados de voz processados localmente (não enviados para nuvem).
- Senhas armazenadas criptografadas.
- Acesso limitado a permissões essenciais.

## Impacto Social e Acessibilidade

### Benefícios para Pessoas com Deficiências
- **Independência**: Controle total do dispositivo sem ajuda.
- **Inclusão**: Participação em sociedade digital.
- **Segurança**: SOS rápido em emergências.
- **Produtividade**: Acesso a trabalho, educação, entretenimento.

### Estudos de Caso
- **Usuário Cego**: Navega WhatsApp, ouve mensagens, responde por voz.
- **Usuário Paraplégico**: Controla música, faz ligações, acessa web sem toques.
- **Idoso com Tremor**: Evita erros de toque, usa voz para tudo.

### Estatísticas de Acessibilidade
- 15% da população mundial tem alguma deficiência (OMS).
- 80% dos apps móveis não são acessíveis (relatórios de acessibilidade).
- Jarbas preenche essa lacuna, oferecendo 100% de controle por voz.

## Melhorias Supremas Planejadas

### 1. IA Avançada e NLP
- Integração com GPT/Claude para entender frases naturais.
- Comando: "Jarbas me ajude com [tarefa complexa]".
- Aprendizado de hábitos do usuário.

### 2. Suporte a Mais Dispositivos
- Wearables (relógios) para controle remoto.
- TVs Android para controle de casa inteligente.
- Carros com Android Auto.

### 3. Recursos Avançados de Acessibilidade
- **Leitura de Tela Completa**: Descreve imagens via IA (OCR + visão computacional).
- **Navegação Espacial**: "Jarbas vai para cima", "Jarbas desliza para direita".
- **Modo Emergência**: Detecção de quedas via acelerômetro, SOS automático.

### 4. Integrações Externas
- Calendário: "Jarbas agende reunião amanhã".
- Lembretes: "Jarbas me lembre de tomar remédio".
- Casa Inteligente: Controle de luzes, temperatura via voz.

### 5. Personalização e Gamificação
- Perfis de usuário (criança, idoso, profissional).
- Recompensas por uso diário para motivar.
- Temas de voz (masculino/feminino, velocidades).

### 6. Privacidade e Segurança Máxima
- Modo offline completo.
- Criptografia end-to-end para dados.
- Auditoria de acessos.

### 7. Expansão Global
- Suporte a múltiplos idiomas.
- Colaboração com ONGs de deficiência.
- Certificações de acessibilidade (WCAG).

## Desenvolvimento e Contribuição

### Estrutura do Projeto
- `lib/`: Código Flutter (UI).
- `android/app/src/main/kotlin/com/jarbas/app/`: Serviços Android.
- `README.md`: Documentação básica.
- `DOCUMENTACAO_COMPLETA.md`: Este arquivo.

### Como Contribuir
1. Fork o repositório.
2. Crie branch para feature.
3. Implemente e teste.
4. Pull Request com descrição detalhada.

### Testes
- Teste em dispositivos reais (não emuladores).
- Usuários beta com deficiências para feedback.
- Cobertura de testes unitários >80%.

## Conclusão

Jarbas representa um avanço revolucionário em acessibilidade móvel, capacitando milhões de pessoas a viverem com dignidade e independência na era digital. Com desenvolvimento contínuo, ele se tornará a ponte perfeita entre humanos e tecnologia, especialmente para aqueles que mais precisam.

### Contato
- Desenvolvedor: loggergray
- Repositório: https://github.com/loggergray/jarbasaipro
- Branch Atual: jarbaskorev9e

### Licença
Licença MIT - Uso livre para fins não comerciais e comerciais, com atribuição.

---

*Este documento é vivo e será atualizado com novas features e feedbacks de usuários.*
