# Simulação de Usuário Cego Usando o Jarbas

## Cenário: Usuário Cego Acordando e Usando o App Kore Organiza

**Nota**: Esta simulação é para fins de teste e desenvolvimento. Assume que o Jarbas está instalado, configurado e funcionando perfeitamente. Como você será o usuário, foque em como um cego interagiria apenas por voz.

### Pré-condições:
- Celular bloqueado na tela de PIN.
- Jarbas com acessibilidade ativada.
- Contato SOS configurado: "119" (emergência).
- URL do Kore: https://contbilidade-inteligente-frcy.onrender.com

### Jornada do Usuário (Passo a Passo):

#### 1. **Acordar o Celular e Desbloquear**
   - **Ação do Usuário**: Toca na tela ou botão power (como faria normalmente).
   - **Jarbas Ativo**: Escuta continuamente.
   - **Comando de Voz**: "Jarbas acorda"
   - **Resposta do Jarbas**: "Diga a senha agora."
   - **Comando de Voz**: "Jarbas um dois tres quatro" (senha numérica).
   - **Ação Interna**: Jarbas acorda tela, digita senha "1234", confirma.
   - **Resultado**: Tela desbloqueada, usuário na home.

#### 2. **Abrir o Kore Organiza**
   - **Comando de Voz**: "Jarbas entrar no kore organiza"
   - **Resposta do Jarbas**: "Abrindo Kore Organiza no Chrome"
   - **Ação Interna**: Abre Chrome, navega para a URL, aguarda carregamento.
   - **Resultado**: Página do Kore carregada.

#### 3. **Fazer Login**
   - **Comando de Voz**: "Jarbas login admin senha senha123"
   - **Resposta do Jarbas**: "Usuário digitado", "Senha digitada", "Entrando"
   - **Ação Interna**: Encontra campos de login, digita "admin" e "senha123", clica em "Entrar".
   - **Resultado**: Logado no dashboard do Kore.

#### 4. **Navegar no Dashboard**
   - **Comando de Voz**: "Jarbas lê a tela"
   - **Resposta do Jarbas**: "Na tela: Botão Dashboard, Botão Relatórios, Campo de busca..."
   - **Comando de Voz**: "Jarbas clica em Relatórios"
   - **Resposta do Jarbas**: "Clicado em Relatórios"
   - **Ação Interna**: Encontra e clica no botão "Relatórios".
   - **Resultado**: Página de relatórios aberta.

#### 5. **Interagir com Elementos**
   - **Comando de Voz**: "Jarbas digita relatório mensal"
   - **Resposta do Jarbas**: "Digitado: relatório mensal", "Enviado" (se houver botão enviar).
   - **Ação Interna**: Digita no campo ativo, tenta enviar.
   - **Resultado**: Texto inserido.

#### 6. **Emergência (Cenário de Teste)**
   - **Comando de Voz**: "Jarbas socorro"
   - **Resposta do Jarbas**: "Emergencia acionada!"
   - **Ação Interna**: Envia SMS para "119" com mensagem de emergência, liga para "119".
   - **Resultado**: Chamada/SMS enviados.

#### 7. **Outras Ações**
   - **Ouvir Música**: "Jarbas toca música de pagode"
   - **Ligar para Contato**: "Jarbas liga pra mae"
   - **Pesquisa**: "Jarbas pesquisa sobre acessibilidade"
   - **Navegação**: "Jarbas volta", "Jarbas home"

### Possíveis Problemas na Simulação (Como Cego):
- **Feedback Auditivo Insuficiente**: Se o Jarbas não falar o que está acontecendo, o usuário fica perdido.
- **Campos Não Detectados**: Se o Kore mudar layout, login pode falhar.
- **Ruído Ambiente**: Voz pode não ser reconhecida em ambientes barulhentos.
- **Dependência de Acessibilidade**: Se desabilitada, nada funciona.

### Como Testar na Prática:
1. Instale o APK da v9.
2. Configure permissões e acessibilidade.
3. Use fones de ouvido para ouvir respostas.
4. Fale comandos claramente.
5. Observe logs no Android Studio para debug.

### Melhorias Baseadas na Simulação:
- Adicionar mais feedback: "Página carregada", "Login bem-sucedido".
- Suporte a gestos por voz: "Jarbas desliza para cima".
- Modo tutorial: "Jarbas me ensine a usar".

Esta simulação mostra como o Jarbas capacita um usuário cego a usar o celular e web apps de forma independente! Se precisar ajustar, me avise.