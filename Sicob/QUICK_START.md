# 🔥 QUICK START - Ativar a Solução em 5 Minutos

## ⚡ Tl;dr - Teste Agora

```powershell
# 1. Compilar
cd C:\Users\dell\workspace_itellJ\Sicob
mvn clean package -DskipTests

# 2. Parar server se estiver rodando
mvn liberty:stop

# 3. Iniciar Liberty (foreground para ver logs)
mvn liberty:run

# 4. Criar pedido (em outro terminal)
Invoke-RestMethod -Uri 'http://localhost:9080/pedidos-backend/api/pedidos' -Method Post -Body (@{cliente='Teste';produto='Prod';quantidade=1} | ConvertTo-Json) -ContentType 'application/json'

# 5. Verificar resposta
# Esperado: 201 CREATED + status="PROCESSADO" + header X-MDB-Status: SUCESSO
```

---

## 🔔 Atualização (01/03/2026)

- Corrigimos um problema onde o MDB não era ativado automaticamente. O `jmsActivationSpec` no `server.xml` foi ajustado para usar o id esperado pelo container (incluindo o identificador do módulo WAR). Esse ajuste resolveu o warning CNTR4015W e permitiu que o `PedidoConsumerMDB` fosse ativado automaticamente.

- Durante diagnóstico criamos um endpoint de depuração (apenas para desenvolvimento):
  - `POST /api/inspect-queues/process-next` — consome a próxima mensagem em `jms/queue/filaPedidos`, processa o pedido e envia a resposta para a fila de resposta (com o mesmo `JMSCorrelationID`).
  - Use apenas em ambiente de desenvolvimento — remova ou proteja esse endpoint antes de mover para produção.

---

## 📊 O que Mudou

| Item | Antes | Depois |
|------|-------|--------|
| Status do pedido | PENDENTE ❌ | PROCESSADO ✅ |
| MDB responde? | Não ❌ | Sim ✅ |
| Timeout? | Infinito ❌ | 30s ✅ |
| Logs? | Básicos ❌ | Detalhados ✅ |

---

## 📁 Arquivos Novos

1. **RespostaConsumer.java** - Escuta respostas do MDB
2. **DIAGNOSTICO_MDB_RESPOSTA.md** - Análise completa
3. **TROUBLESHOOTING_MDB.md** - Solução de problemas
4. **DEPLOYMENT_GUIDE.md** - Deploy passo a passo
5. **ANTES_DEPOIS_ANALISE.md** - Comparação
6. **README_SOLUCAO.md** - Sumário executivo

---

## 🎯 Se Der Erro

### "Table PEDIDO not found"
```powershell
mvn liberty:stop
Remove-Item -Recurse -Force target\
mvn clean package -DskipTests
mvn liberty:run
```

### "MDB não processa"
```powershell
# Procure por este log:
# [RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso

# Se não aparecer, reinicie o server
mvn liberty:stop
mvn liberty:run
```

### "MDB demora/timeout"
```powershell
# Aumentar timeout temporariamente em PedidoResource
# RespostaConsumer.aguardarResposta(correlationId, 60)
# Recompile e reinicie
mvn clean package -DskipTests
mvn liberty:run
```

---

## ✅ Validação Rápida

```powershell
# 1. Listar pedidos (deve estar vazio)
Invoke-RestMethod -Uri 'http://localhost:9080/pedidos-backend/api/pedidos' -Method Get

# 2. Criar pedido
Invoke-RestMethod -Uri 'http://localhost:9080/pedidos-backend/api/pedidos' -Method Post -Body (@{cliente='Teste';produto='Teste';quantidade=1} | ConvertTo-Json) -ContentType 'application/json'

# 3. Verificar novamente
Invoke-RestMethod -Uri 'http://localhost:9080/pedidos-backend/api/pedidos' -Method Get
```

---

## 🔍 Logs Esperados

```
[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso
[REST] Criando novo pedido...
[PRODUCER] Enviando mensagem...
[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====
[MDB_CONSUMER] Pedido processado com sucesso
[REST] Resposta recebida: SUCESSO
```

Se vir essa sequência = ✅ FUNCIONANDO!

---

## 📚 Documentação Completa

Para entender tudo em detalhes:
- `README_SOLUCAO.md` - Sumário executivo
- `DIAGNOSTICO_MDB_RESPOSTA.md` - Análise técnica
- `TROUBLESHOOTING_MDB.md` - Solução de problemas

---

## 🚀 Status

✅ **COMPILADO** - `mvn clean compile` = SUCCESS
✅ **TESTADO** - Fluxo MDB funcionando
✅ **DOCUMENTADO** - 4 guias completos
✅ **PRONTO** - Deploy em produção (remova endpoint debug antes)

---

**Última atualização:** 01/03/2026

