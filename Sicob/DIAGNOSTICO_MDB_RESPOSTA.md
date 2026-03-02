# 🔧 Diagnóstico e Solução: MDB de Resposta no WebSphere Liberty

## 📋 Problemas Identificados e Resolvidos

### **Problema 1: MDB não estava sendo ativado**
**Causa:** A configuração do `@MessageDriven` no código não estava sincronizada com a configuração do `server.xml`.

**Solução aplicada:**
- Melhorei a `jmsActivationSpec` no `server.xml` com `maxPoolSize="10"`
- Adicionei propriedades de configuração explícitas ao `@MessageDriven` (`acknowledgeMode`, `maxPoolSize`)

### **Problema 2: Sem mecanismo de resposta**
**Causa:** O MDB processava a mensagem, mas não tinha forma de notificar o cliente sobre o resultado.

**Solução aplicada:**
- ✅ Implementei padrão **Request-Reply** com JMS
- ✅ Criei classe `RespostaConsumer` singleton que escuta fila de resposta
- ✅ MDB agora envia resposta de volta via `JMSReplyTo`
- ✅ REST aguarda resposta antes de retornar ao cliente

### **Problema 3: Falta de transação no MDB**
**Causa:** O MDB não tinha tratamento explícito de transação.

**Solução aplicada:**
- Adicionei `@TransactionAttribute(TransactionAttributeType.REQUIRED)`
- Melhorei tratamento de erros com try-catch robusto

### **Problema 4: Timeout/Fila não processando**
**Causa:** Sem logs adequados era impossível diagnosticar o problema.

**Solução aplicada:**
- ✅ Adicionar logging detalhado em TODAS as etapas (prefixos `[PRODUCER]`, `[MDB_CONSUMER]`, `[RESPOSTA_CONSUMER]`, `[REST]`)
- ✅ Logs tanto via Logger quanto System.out para visibilidade imediata

---

## 🏗️ Arquitetura da Solução

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENTE REST                               │
│              POST /pedidos (criar pedido)                        │
└──────────────────────┬──────────────────────────────────────────┘
                       │
         ┌─────────────▼──────────────────┐
         │    PedidoResource (REST)       │
         │  - Criar pedido no DB          │
         │  - Enviar ID para fila         │
         │  - AGUARDAR resposta (30s)     │
         └──────────┬──────────────────────┘
                    │
        ┌───────────▼─────────────┐
        │   FILA: filaPedidos     │
        │ (JMS Queue - WAS Local)  │
        └───────────┬─────────────┘
                    │
        ┌───────────▼──────────────────┐
        │  PedidoConsumerMDB           │
        │  - Recebe ID do pedido       │
        │  - Atualiza status: PENDENTE │
        │    para PROCESSADO           │
        │  - Envia resposta via ReplyTo│
        └───────────┬──────────────────┘
                    │
        ┌───────────▼─────────────────┐
        │  FILA: filaResposta         │
        │ (JMS Queue - WAS Local)     │
        └───────────┬─────────────────┘
                    │
        ┌───────────▼──────────────────┐
        │  RespostaConsumer (Singleton)│
        │  - Escuta fila de resposta   │
        │  - Armazena em cache         │
        │  - Notifica via correlationID│
        └───────────┬──────────────────┘
                    │
        ┌───────────▼──────────────────┐
        │  PedidoResource aguarda      │
        │  - Retorna pedido com status │
        │    PROCESSADO ao cliente     │
        └──────────────────────────────┘
```

---

## 🚀 Como Testar

### **Pré-requisitos:**
1. Liberty iniciado e aplicação deployada
2. Tabela de pedidos criada (DatabaseInit.java cria automaticamente)

### **Teste 1: Criar Pedido com Sucesso**

```bash
curl -X POST http://localhost:9080/pedidos-backend/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": "João Silva",
    "produto": "Notebook",
    "quantidade": 1
  }'
```

**Esperado:**
- Status: 201 CREATED
- Body: Pedido com status = "PROCESSADO"
- Header: `X-MDB-Status: SUCESSO`
- Console Liberty: Logs detalhados de produção e consumo

### **Teste 2: Verificar Status do Pedido**

```bash
curl -X GET http://localhost:9080/pedidos-backend/api/pedidos/1 \
  -H "Content-Type: application/json"
```

**Esperado:**
- Status: 200 OK
- Body: Pedido com status = "PROCESSADO"

### **Teste 3: Listar Todos os Pedidos**

```bash
curl -X GET http://localhost:9080/pedidos-backend/api/pedidos \
  -H "Content-Type: application/json"
```

---

## 📊 Logs esperados (console Liberty)

Quando você criar um pedido, você deve ver algo como:

```
[REST] Criando novo pedido...
[REST] Pedido criado com ID: 1
[PRODUCER] Preparando para enviar pedido ID: 1
[PRODUCER] Enviando mensagem com correlationID: PEDIDO-1-1234567890123
[PRODUCER] Mensagem enviada com sucesso para a fila.
[REST] Aguardando resposta do MDB...

===== MENSAGEM RECEBIDA NO MDB! =====
[MDB_CONSUMER] ID recebido: 1
[MDB_CONSUMER] CorrelationID: PEDIDO-1-1234567890123
[MDB_CONSUMER] ReplyTo: jms/queue/filaResposta
[MDB_CONSUMER] Pedido processado com sucesso: 1, status: PROCESSADO
[MDB_CONSUMER] Preparando envio de resposta...
[MDB_CONSUMER] Resposta enviada com sucesso!

[RESPOSTA_CONSUMER] Resposta recebida - CorrelationID: PEDIDO-1-1234567890123, Status: SUCESSO
[RESPOSTA_CONSUMER] Resposta armazenada no cache
[RESPOSTA_CONSUMER] Aguardando resposta para: PEDIDO-1-1234567890123 (timeout: 30s)
[RESPOSTA_CONSUMER] Resposta encontrada!
[REST] Resposta recebida: SUCESSO
```

---

## ⚙️ Configurações Principais

### **server.xml - JMS Configuration**

```xml
<!-- JMS Messaging Engine Local -->
<messagingEngine>
    <queue id="filaPedidos" forceReliability="ReliablePersistent"/>
    <queue id="filaResposta" forceReliability="ReliablePersistent"/>
</messagingEngine>

<!-- JMS Connection Factory -->
<jmsConnectionFactory id="SicobCF" jndiName="jms/ConnectionFactory">
    <properties.wasJms />
</jmsConnectionFactory>

<!-- Filas JMS -->
<jmsQueue id="FilaPedidosQueue" jndiName="jms/queue/filaPedidos">
    <properties.wasJms queueName="filaPedidos" />
</jmsQueue>

<jmsQueue id="FilaRespostaQueue" jndiName="jms/queue/filaResposta">
    <properties.wasJms queueName="filaResposta" />
</jmsQueue>

<!-- MDB Activation Spec -->
<jmsActivationSpec id="pedidos-backend/PedidoConsumerMDB">
    <properties.wasJms destinationRef="FilaPedidosQueue" destinationType="javax.jms.Queue" maxPoolSize="10" />
</jmsActivationSpec>
```

### **Features Requeridas (server.xml)**

```xml
<feature>jms-2.0</feature>
<feature>wasJmsServer-1.0</feature>
<feature>wasJmsClient-2.0</feature>
<feature>mdb-3.2</feature>
<feature>ejbLite-3.2</feature>
<feature>cdi-1.2</feature>
```

---

## 🔍 Troubleshooting

### **Problema: MDB não processa mensagens**

**Solução:**
1. Verifique no console: `[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====`
2. Se não aparecer, verifique se `RespostaConsumer` foi inicializado: `[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso`
3. Valide injeções: `[PRODUCER] connectionFactory está NULO!` ou `[PRODUCER] filaPedidos está NULA!`

**Nota de correção (01/03/2026):**
- Em um caso real, o container não ativava o terminal de mensagens porque o `jmsActivationSpec` no `server.xml` tinha id diferente do esperado. Corrigimos o `server.xml` para `id="pedidos-backend-1.0-SNAPSHOT/PedidoConsumerMDB"` (incluindo o identificador do módulo WAR) — isso resolveu o warning `CNTR4015W` e permitiu ao MDB receber mensagens automaticamente.

**Ferramenta de debug temporária:**
- Criamos `POST /api/inspect-queues/process-next` (apenas para desenvolvimento). Ele consome manualmente a próxima mensagem na `filaPedidos`, processa o pedido e envia a resposta para `filaResposta` com o mesmo `JMSCorrelationID`. Use este endpoint apenas para liberar mensagens em ambiente de desenvolvimento e remova/proteja em produção.

### **Problema: REST retorna timeout (X-Warning header)**

**Solução:**
1. Verifique se MDB é ativado (logs acima)
2. Aumente timeout em `PedidoResource.criarPedido()`: `aguardarResposta(correlationId, 60)` (60 segundos)
3. Verifique transações: MDB pode estar em deadlock

### **Problema: Status do pedido não foi atualizado**

**Solução:**
1. Verifique se tabela existe: `SELECT COUNT(*) FROM pedido;`
2. Verifique logs do MDB: `[MDB_CONSUMER] Pedido não encontrado:`
3. Valide a injeção do `PedidoService` no MDB

---

## 📈 Performance e Escalabilidade

- **maxPoolSize="10"**: Até 10 MDBs processando em paralelo
- **ReliablePersistent**: Mensagens não são perdidas se o servidor parar
- **ConcurrentHashMap**: Cache thread-safe de respostas
- **Timeout de 30s**: Pode ser ajustado conforme necessidade

---

## 🎯 Próximos Passos (Opcional)

1. **Adicionar Dead Letter Queue (DLQ)** para mensagens que falham após retry
2. **Implementar Circuit Breaker** se o banco fica indisponível
3. **Cache em Redis** se precisar escalabilidade horizontal
4. **Webhooks** para notificar sistemas externos do resultado

---

**Última atualização:** 01/03/2026
**Status:** ✅ Solução completa implementada
