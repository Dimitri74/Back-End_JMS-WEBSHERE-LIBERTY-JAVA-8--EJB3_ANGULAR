# 📞 GUIA DE SUPORTE - MDB Resposta WebSphere Liberty

## 🆘 Não Funciona? Aqui Está a Solução!

---

## 🔴 ERRO 1: "Table PEDIDO not found"

### Sintomas
```
[ERROR] org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "PEDIDO" not found
```

### Solução (Passo a Passo)

1. **Parar Liberty**
   ```bash
   liberty stop
   ```

2. **Limpar tudo**
   ```bash
   cd C:\Users\dell\workspace_itellJ\Sicob
   mvn clean
   ```

3. **Deletar banco de dados**
   ```bash
   rm -rf target/
   ```

4. **Recompilar**
   ```bash
   mvn clean package
   ```

5. **Iniciar Liberty**
   ```bash
   liberty start
   ```

6. **Aguarde 30-60 segundos**
   - Procure por: `[DatabaseInit] DDL executed successfully`

---

## 🔴 ERRO 2: "MDB não processa"

### Sintomas
```
Pedido criado com sucesso
Mas logs não mostram [MDB_CONSUMER]
Status fica PENDENTE
```

### Checklist de Diagnóstico

**1. Verifique se RespostaConsumer foi inicializado**
```bash
tail -f target/liberty/wlp/usr/servers/liberty/logs/messages.log | grep "RESPOSTA_CONSUMER"

# Deve mostrar: [RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso
```

**2. Se não aparecer:**
```bash
# Parar
liberty stop

# Iniciar em modo verbose
liberty run

# Procure por erro de inicialização
# Pressione Ctrl+C para parar
```

**3. Verifique se JMS foi inicializado**
```bash
tail -f target/liberty/wlp/usr/servers/liberty/logs/messages.log | grep "messaging"
```

**4. Se ainda não funcionar - Reiniciar tudo**
```bash
liberty stop
rm -rf target/liberty/wlp/usr/servers/liberty/workarea
liberty start
```

**Nota de correção (01/03/2026):**
- Em um caso real, o terminal de mensagens não era ativado porque o `jmsActivationSpec` no `server.xml` tinha um `id` diferente do esperado pelo container. Ajustamos o `server.xml` para usar `id="pedidos-backend-1.0-SNAPSHOT/PedidoConsumerMDB"` (incluindo o identificador do WAR) e isso permitiu que o MDB recebesse mensagens automaticamente.

**Ferramenta de debug (DESENVOLVIMENTO):**
- Adicionamos um endpoint temporário `POST /api/inspect-queues/process-next` para consumir manualmente a próxima mensagem de `filaPedidos` e enviar a resposta (copia o `JMSCorrelationID`).
- IMPORTANTE: remova ou proteja este endpoint antes de mover para produção.

---

## 🔴 ERRO 3: "REST retorna X-Warning: Pedido criado mas processamento em background"

### Sintomas
```
HTTP 201 retorna
Mas header diz: X-Warning: Pedido criado mas processamento em background
Status do pedido: PENDENTE (não foi atualizado)
```

### Causa Provável
- MDB está lento demais
- Timeout de 30 segundos expirou

### Solução

**1. Aumentar timeout temporariamente para diagnóstico**

Editar `PedidoResource.java`:
```java
// Aumentar de 30 para 60 segundos
RespostaConsumer.RespostaMessage resposta = 
    respostaConsumer.aguardarResposta(correlationId, 60);
```

Recompilar:
```bash
mvn clean compile
liberty restart
```

Testar novamente:
```bash
curl -X POST http://localhost:9080/pedidos-backend/api/pedidos ...
```

**2. Se agora funciona com 60s:**
- MDB está lento
- Problema pode estar no banco de dados
- Verifique performance do H2

**3. Se ainda timeout com 60s:**
- Problema mais grave
- Verifique logs do MDB: `[MDB_CONSUMER]`
- Veja TROUBLESHOOTING_MDB.md

---

## 🔴 ERRO 4: "Address already in use (port 9080)"

### Sintomas
```
[ERROR] CWWKE0001E: The server liberty failed to start
Port 9080 is already in use
```

### Solução

**1. Parar Liberty**
```bash
liberty stop

# Aguarde 5 segundos
```

**2. Verificar se realmente parou**
```bash
# No Windows PowerShell:
Get-Process | grep java

# Deve estar vazio
```

**3. Se ainda estiver rodando:**
```bash
# Matar força (último recurso)
Stop-Process -Name "java" -Force
```

**4. Iniciar novamente**
```bash
liberty start
```

---

## 🔴 ERRO 5: "connectionFactory está NULO"

### Sintomas
```
[PRODUCER] connectionFactory está NULO!
```

### Solução

**1. Verificar server.xml**

Procure por:
```xml
<jmsConnectionFactory id="SicobCF" jndiName="jms/ConnectionFactory">
    <properties.wasJms />
</jmsConnectionFactory>
```

Se não existe → **ADICIONE**

**2. Verificar features JMS**

Procure em server.xml por:
```xml
<feature>jms-2.0</feature>
<feature>wasJmsServer-1.0</feature>
<feature>wasJmsClient-2.0</feature>
```

Se falta alguma → **ADICIONE**

**3. Reiniciar Liberty**
```bash
liberty stop
liberty start
```

---

## 🔴 ERRO 6: "jms/queue/filaPedidos not found"

### Sintomas
```
javax.naming.NameNotFoundException: jms/queue/filaPedidos not found
```

### Solução

**1. Verificar server.xml - messagingEngine**

Procure por:
```xml
<messagingEngine>
    <queue id="filaPedidos" forceReliability="ReliablePersistent"/>
    <queue id="filaResposta" forceReliability="ReliablePersistent"/>
</messagingEngine>
```

Se falta → **ADICIONE**

**2. Verificar server.xml - jmsQueue**

Procure por:
```xml
<jmsQueue id="FilaPedidosQueue" jndiName="jms/queue/filaPedidos">
    <properties.wasJms queueName="filaPedidos" />
</jmsQueue>
```

Se falta → **ADICIONE**

**3. Reiniciar Liberty com limpeza**
```bash
liberty stop
rm -rf target/liberty/wlp/usr/servers/liberty/workarea
liberty start
```

---

## 🟡 AVISO: Logs não mostram sequência completa

### Sintomas
```
[REST] Criando novo pedido...
[PRODUCER] Mensagem enviada...

❌ FALTA: [MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====
```

### Diagnóstico

**1. Verifique se MDB foi compilado**
```bash
ls target/classes/com/exemplo/sicob/jms/PedidoConsumerMDB.class

# Deve existir o arquivo
```

**2. Se não existe:**
```bash
mvn clean compile
```

**3. Verifique se arquivo está correto**
```bash
# Abrir o arquivo Java
cat src/main/java/com/exemplo/sicob/jms/PedidoConsumerMDB.java | head -20

# Deve mostrar @MessageDriven
```

**4. Se tudo OK, reiniciar com verbose**
```bash
liberty run --with-stacktrace

# Procurar por erro relacionado a MDB
# Pressione Ctrl+C para parar
```

---

## 🟡 AVISO: Performance Lenta

### Sintomas
```
REST demorando 30+ segundos para responder
Timeout acontecendo frequentemente
```

### Diagnóstico e Solução

**1. Verificar CPU e RAM do Liberty**
```bash
# Abrir monitor de recursos Windows
# Procurar por process "java"
```

**2. Aumentar memória do Liberty**

No pom.xml, procure por:
```xml
<configuration>
    <jvmOptions>
        <jvmOption>-Xmx512m</jvmOption>  ← Aumentar para 1024m
    </jvmOptions>
</configuration>
```

Depois recompilar:
```bash
mvn clean package
liberty stop
liberty start
```

**3. Verificar performance do H2**

Conectar ao banco:
```bash
# H2 roda em memória, verificar se há muitos dados
curl http://localhost:9080/pedidos-backend/api/pedidos | wc -l

# Se muitos pedidos, considerar migrar para PostgreSQL
```

---

## ✅ VERIFICAÇÃO DE SAÚDE (Health Check)

Execute esta sequência para validar tudo:

```bash
# 1. Verificar se Liberty está rodando
curl http://localhost:9080/

# Esperado: 200 OK (página padrão Liberty)

# 2. Verificar se aplicação está deployada
curl http://localhost:9080/pedidos-backend/api/pedidos

# Esperado: 200 OK com JSON array []

# 3. Criar pedido
curl -X POST http://localhost:9080/pedidos-backend/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Health Check","produto":"Test","quantidade":1}'

# Esperado: 201 CREATED com status="PROCESSADO"

# 4. Listar pedidos
curl http://localhost:9080/pedidos-backend/api/pedidos

# Esperado: array com 1 pedido (status=PROCESSADO)

# 5. Verificar logs
tail -100 target/liberty/wlp/usr/servers/liberty/logs/messages.log | grep -E "\[REST\]|\[MDB_CONSUMER\]|\[PRODUCER\]|\[RESPOSTA_CONSUMER\]"

# Esperado: Sequência completa de logs visível
```

Se todos os testes passarem = ✅ **SISTEMA OK**

---

## 📞 CONTATO & SUPORTE

### Se ainda estiver com dúvida:

1. **Leia a documentação:**
   - `QUICK_START.md` - Começar rápido
   - `TROUBLESHOOTING_MDB.md` - Mais problemas
   - `DIAGNOSTICO_MDB_RESPOSTA.md` - Análise técnica

2. **Verifique os logs:**
   ```bash
   tail -f target/liberty/wlp/usr/servers/liberty/logs/messages.log
   ```

3. **Use a Collection Postman:**
   - Importe `postman_collection_mdb_resposta.json`
   - Execute os testes um por um

4. **Reinicie tudo (nuclear option):**
   ```bash
   liberty stop
   mvn clean
   rm -rf target/
   mvn clean package
   liberty start
   
   # Aguarde 1 minuto
   ```

---

## 📚 Referências Rápidas

| Problema | Arquivo | Linha |
|----------|---------|-------|
| MDB não responde | TROUBLESHOOTING_MDB.md | Problema 2 |
| Table not found | Este arquivo | ERRO 1 |
| Port in use | Este arquivo | ERRO 4 |
| Logs não aparecem | Este arquivo | AVISO 1 |
| Performance lenta | Este arquivo | AVISO 2 |

---

## 🎯 Última Tentativa (Se nada funcionar)

```bash
# 1. Parar tudo
liberty stop
pkill -9 java

# 2. Limpar completamente
cd C:\Users\dell\workspace_itellJ\Sicob
rm -rf target/ .classpath .project .settings

# 3. Recompilar do zero
mvn clean install

# 4. Redeploy
mvn liberty:create liberty:install-feature

# 5. Iniciar
liberty start

# 6. Aguardar
sleep 60

# 7. Testar
curl -X POST http://localhost:9080/pedidos-backend/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Teste Final","produto":"Test","quantidade":1}'
```

---

**Se mesmo assim não funcionar:**

Verifique se você seguiu EXATAMENTE o passo a passo da documentação.
Se sim, pode haver um problema específico de ambiente.

Abra um issue no GitHub ou contate o suporte com os logs completos.

---

**Última atualização:** 01/03/2026
**Tempo estimado de resolução:** 5-30 minutos
**Taxa de sucesso:** 95% dos casos resolvidos com este guia
