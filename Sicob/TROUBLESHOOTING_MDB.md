# 🔍 Guia Completo de Troubleshooting - MDB Resposta

## ⚡ Checklist Rápido

- [ ] Liberty iniciado e rodando
- [ ] Aplicação deployada com sucesso
- [ ] Tabela `pedido` criada no banco H2
- [ ] Logs no console mostram `Consumer de resposta inicializado com sucesso`
- [ ] POST em `/api/pedidos` retorna 201 CREATED

---

## 🚨 Problemas Comuns e Soluções

### ❌ Problema 1: "Table PEDIDO not found"

**Descrição:**
```
[ERROR] org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "PEDIDO" not found
```

**Causa:**
- DatabaseInit não foi executado ou falhou
- Tabela ainda não foi criada

**Solução:**
1. **Reinicie o Liberty** (isso força execução de `@Startup`)
   ```bash
   # Parar
   liberty stop
   
   # Iniciar
   liberty start
   ```

2. **Verifique logs do startup:**
   ```
   [DatabaseInit] executing DDL -> CREATE TABLE IF NOT EXISTS pedido (...)
   [DatabaseInit] DDL executed successfully
   ```

3. **Se ainda não funcionar:**
   - Delete o banco H2: `rm -r target/liberty/wlp/usr/shared/resources/*`
   - Rebuild: `mvn clean package`
   - Redeploy e restart Liberty

---

### ❌ Problema 2: "MDB não processa mensagens"

**Descrição:**
- POST em `/api/pedidos` retorna sucesso
- Mas não há logs `[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====`

**Causa Provável 1: RespostaConsumer não iniciou**

**Solução:**
```
Procure por este log no console:
[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso

Se NÃO aparecer:
- Verifique se as queues JMS foram criadas
- Verifique se connectionFactory foi injetada
```

**Causa Provável 2: MDB não foi ativado**

**Solução:**
1. Verifique `server.xml`:
   ```xml
   <jmsActivationSpec id="pedidos-backend/PedidoConsumerMDB">
       <properties.wasJms destinationRef="FilaPedidosQueue" destinationType="javax.jms.Queue" maxPoolSize="10" />
   </jmsActivationSpec>
   ```

2. Verifique se `@MessageDriven` está correto em `PedidoConsumerMDB`:
   ```java
   @MessageDriven(activationConfig = {
       @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/queue/filaPedidos"),
       @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
       @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
       @ActivationConfigProperty(propertyName = "maxPoolSize", propertyValue = "10")
   })
   ```

3. **Enable debug logging:**
   ```xml
   <logging traceSpecification="*=finest"/>
   ```
   Depois reinicie e procure por: `MDB` ou `messagingEngine`

**Nota de correção (01/03/2026):**
- Em um caso real, o container não ativava o terminal de mensagens porque o `jmsActivationSpec` no `server.xml` tinha id diferente do esperado pelo container. Corrigimos o arquivo para usar o id `pedidos-backend-1.0-SNAPSHOT/PedidoConsumerMDB` (incluindo o identificador do WAR) e isso resolveu o problema.

**Ferramenta de debug:**
- Para facilitar o diagnóstico, um endpoint de debug foi adicionado: `POST /api/inspect-queues/process-next`. Ele consome manualmente a próxima mensagem em `filaPedidos`, processa o pedido e envia a resposta com o mesmo `JMSCorrelationID`. Use apenas em desenvolvimento e remova/proteja antes de entrar em produção.

---

### ❌ Problema 3: "REST retorna X-Warning: Pedido criado mas processamento em background"

**Descrição:**
- Resposta HTTP 201, mas header diz processamento em background
- Status do pedido ainda é "PENDENTE"

**Causa:**
- Timeout esperando resposta do MDB (padrão: 30s)
- MDB está lento ou não respondeu

**Solução:**

1. **Aumentar timeout temporariamente para diagnóstico:**
   
   Em `PedidoResource.java`:
   ```java
   RespostaConsumer.RespostaMessage resposta = respostaConsumer.aguardarResposta(correlationId, 60); // 60 segundos
   ```

2. **Verificar logs de resposta:**
   ```
   [RESPOSTA_CONSUMER] Aguardando resposta para: PEDIDO-1-xxx (timeout: 30s)
   [RESPOSTA_CONSUMER] TIMEOUT aguardando resposta para: PEDIDO-1-xxx
   ```

3. **Verificar se MDB enviou resposta:**
   ```
   [MDB_CONSUMER] Preparando envio de resposta...
   [MDB_CONSUMER] Resposta enviada com sucesso!
   ```

4. **Se MDB não responde:**
   - Pode estar em transação pendente
   - Verifique se `PedidoService.atualizarStatus()` está funcionando
   - Teste endpoint `GET /api/pedidos/1` para verificar status

---

### ❌ Problema 4: "Injeção de Bean falha"

**Erro:**
```
java.lang.IllegalStateException: Unable to process the request for bean [PedidoResource]
```

**Causa:**
- CDI não configurado
- Bean não encontrado

**Solução:**

1. **Verifique `beans.xml` em `src/main/webapp/WEB-INF/`:**
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <beans xmlns="http://xmlns.jcp.org/xml/ns/cdi"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/cdi 
          http://xmlns.jcp.org/xml/ns/cdi/beans_1_1.xsd"
          bean-discovery-mode="all">
   </beans>
   ```

2. **Verifique se `cdi-1.2` está no `server.xml`:**
   ```xml
   <feature>cdi-1.2</feature>
   ```

3. **Limpe cache:**
   ```bash
   mvn clean
   ```

4. **Redeploy:**
   - Via IDE: Clique direito em projeto > Liberty > Publish

---

### ❌ Problema 5: "connectionFactory está NULO"

**Erro:**
```
[PRODUCER] connectionFactory está NULO!
```

**Causa:**
- JNDI lookup falhou
- ConnectionFactory não foi criada no Liberty

**Solução:**

1. **Verifique `server.xml`:**
   ```xml
   <jmsConnectionFactory id="SicobCF" jndiName="jms/ConnectionFactory">
       <properties.wasJms />
   </jmsConnectionFactory>
   ```

2. **Verifique se features JMS estão habilitadas:**
   ```xml
   <feature>jms-2.0</feature>
   <feature>wasJmsServer-1.0</feature>
   <feature>wasJmsClient-2.0</feature>
   ```

3. **Se ainda não funcionar:**
   - Delete a pasta `target/liberty`
   - Rebuild: `mvn clean liberty:create liberty:install-feature`

---

### ❌ Problema 6: "Queue não encontrada"

**Erro:**
```
javax.naming.NameNotFoundException: jms/queue/filaPedidos not found
```

**Causa:**
- Queue não foi criada no Liberty
- JNDI name incorreto

**Solução:**

1. **Verifique `server.xml` - messaging engine:**
   ```xml
   <messagingEngine>
       <queue id="filaPedidos" forceReliability="ReliablePersistent"/>
       <queue id="filaResposta" forceReliability="ReliablePersistent"/>
   </messagingEngine>
   ```

2. **Verifique JNDI name:**
   ```xml
   <jmsQueue id="FilaPedidosQueue" jndiName="jms/queue/filaPedidos">
       <properties.wasJms queueName="filaPedidos" />
   </jmsQueue>
   ```

3. **Restart Liberty completo:**
   ```bash
   liberty stop
   rm -rf target/
   mvn clean package
   liberty start
   ```

---

## 📊 Verificação de Saúde (Health Check)

Crie este endpoint para verificar se tudo está ok:

```java
@GET
@Path("/health")
public Response health() {
    StringBuilder status = new StringBuilder();
    
    // Verificar conexão com BD
    try {
        List<Pedido> list = pedidoService.listAll();
        status.append("✅ BD conectado\n");
    } catch (Exception e) {
        status.append("❌ BD falhou: ").append(e.getMessage()).append("\n");
    }
    
    // Verificar JMS
    try {
        ConnectionFactory cf = // lookup
        status.append("✅ JMS conectado\n");
    } catch (Exception e) {
        status.append("❌ JMS falhou: ").append(e.getMessage()).append("\n");
    }
    
    return Response.ok(status.toString()).build();
}
```

Teste com:
```bash
curl http://localhost:9080/pedidos-backend/api/pedidos/health
```

---

## 🔧 Modo Debug Avançado

### Ativar logging fino:

**server.xml:**
```xml
<logging traceSpecification="com.exemplo.sicob.*=finest:org.eclipse.persistence.*=finest:com.ibm.ws.jms*=finest"/>
```

### Logs de JMS detalhados:

**server.xml:**
```xml
<logging 
    traceSpecification="org.apache.activemq.*=finest:
                       com.ibm.ws.jms*=finest:
                       com.ibm.ejs.container.*=finest"
/>
```

### Ativar verbose no Liberty:

```bash
# Inicia com output detalhado
liberty run --with-stacktrace
```

---

## 📈 Performance - Otimizações

### Aumentar pool de MDBs:

```xml
<jmsActivationSpec id="pedidos-backend/PedidoConsumerMDB">
    <properties.wasJms 
        destinationRef="FilaPedidosQueue" 
        destinationType="javax.jms.Queue" 
        maxPoolSize="20"  <!-- Aumentar para 20 -->
    />
</jmsActivationSpec>
```

### Aumentar timeout de resposta:

```java
// Em PedidoResource.java
RespostaConsumer.RespostaMessage resposta = 
    respostaConsumer.aguardarResposta(correlationId, 60); // 60 segundos ao invés de 30
```

---

## 🎯 Verificação Final de Integração

Execute esta sequência para validar tudo:

```bash
# 1. Criar pedido
curl -X POST http://localhost:9080/pedidos-backend/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Teste","produto":"Produto","quantidade":1}'

# Esperado: 201 CREATED com X-MDB-Status: SUCESSO

# 2. Verificar status
curl http://localhost:9080/pedidos-backend/api/pedidos/1

# Esperado: status = "PROCESSADO"

# 3. Listar todos
curl http://localhost:9080/pedidos-backend/api/pedidos

# Esperado: array com o pedido criado
```

---

## 📝 Logs que você DEVE VER (Ordem Esperada)

```
1. [RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso
   ↓
2. [REST] Criando novo pedido...
   ↓
3. [PRODUCER] Preparando para enviar pedido ID: 1
   ↓
4. [PRODUCER] Mensagem enviada com sucesso para a fila.
   ↓
5. [REST] Aguardando resposta do MDB...
   ↓
6. [MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====
   ↓
7. [MDB_CONSUMER] Pedido processado com sucesso
   ↓
8. [MDB_CONSUMER] Resposta enviada com sucesso!
   ↓
9. [RESPOSTA_CONSUMER] Resposta recebida
   ↓
10. [RESPOSTA_CONSUMER] Resposta encontrada!
   ↓
11. [REST] Resposta recebida: SUCESSO
```

Se faltar qualquer log desta sequência, identifique qual está faltando e aplique a solução correspondente acima.

---

**Última atualização:** 01/03/2026
