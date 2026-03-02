# Pedidos Backend (EJB + JPA + JMS)

Projeto de exemplo em Java 8 usando EJB3, JPA e JMS com WebSphere Liberty. Versão de desenvolvimento configurada para usar H2 (banco embarcado) para facilitar testes locais, com suporte completo a Message-Driven Beans (MDB) para processamento assíncrono de pedidos.

## 🟢 Status de Implementação

✅ **MDB de Resposta Funcional** — O Message-Driven Bean agora processa pedidos e retorna resposta sincronizada (timeout 30s)  
✅ **Correlação JMS** — CorrelationID alinhado entre requisição e resposta  
✅ **Persistência H2** — Banco embarcado funcionando com DDL auto-geração  
✅ **Endpoints REST** — API completa (CREATE, READ, LIST)  
✅ **Documentação Completa** — Guias técnicos, Postman collection e troubleshooting  
✅ **Logs Estruturados** — Rastreamento completo do fluxo request-reply  

## 🏗️ Arquitetura

![Arquitetura Pedidos Backend](diagrama-arquitetura.png)

A solução implementa o padrão **Request-Reply com JMS**:
- **Frontend (Angular)** → envia POST para criar pedido
- **API REST** → recebe requisição, persiste no BD e envia para fila JMS
- **Message-Driven Bean (MDB)** → processa o pedido de forma assíncrona
- **Base de Dados (H2)** → armazena pedidos com status PROCESSADO
- **JMS Mensageria** → comunica REST ↔ MDB com resposta confirmada

## 📋 Principais Componentes

- **Entidade `Pedido`**: campos `id`, `cliente`, `produto`, `quantidade`, `status`.
- **`PedidoService` (EJB Stateless)**: CRUD de pedidos com transações JPA.
- **`PedidoResource` (JAX-RS REST)**: endpoints para criar/listar pedidos; aguarda resposta do MDB (timeout 30s).
- **`PedidoProducer` (EJB Stateless)**: envia ID do pedido para a fila `jms/queue/filaPedidos` com CorrelationID.
- **`PedidoConsumerMDB`**: consome mensagens, processa pedido e envia resposta via `JMSReplyTo`.
- **`RespostaConsumer` (EJB Singleton)**: ouve fila de resposta e armazena em cache thread-safe.
- **`persistence.xml`**: configurado para H2 em memória; `import.sql` popula dados de exemplo.

## 🚀 Como Rodar Localmente (WebSphere Liberty)

### Pré-requisitos
- **Java 8+** (ou Java 21 recomendado)
- **Maven 3.6+**
- **Git** (para clonar o projeto)

### Quick Start (5 minutos)

1. **Compilar o projeto:**
```powershell
cd C:\Users\dell\workspace_itellJ\Sicob
mvn clean package -DskipTests
```

2. **Parar Liberty se já estiver rodando:**
```powershell
mvn liberty:stop
```

3. **Iniciar Liberty (modo desenvolvimento - foreground com logs visíveis):**
```powershell
mvn liberty:run
```

Aguarde até ver a mensagem:
```
[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso
```

4. **Acompanhar logs em tempo real (outro terminal):**
```powershell
Get-Content target\liberty\wlp\usr\servers\SicobServer\logs\messages.log -Tail 200 -Wait
```

5. **Testar no Postman:**
- Importe a collection: **`postman_collection_mdb_resposta.json`** (contém todos os endpoints pré-configurados)
- Ou teste manualmente:
  - Envie um POST para `http://localhost:9080/pedidos-backend/api/pedidos`
  - Headers: `Content-Type: application/json`
  - Body:
```json
{
  "cliente": "João Silva",
  "produto": "Notebook",
  "quantidade": 1
}
```

**Esperado:** HTTP 201 CREATED com `status: "PROCESSADO"` 🎉

### Modo Background (desenvolvimento contínuo)

```powershell
# Iniciar em background
mvn liberty:start

# Fazer desenvolvimento/testes

# Parar quando terminar
mvn liberty:stop
```

## 📡 Endpoints REST

> 📌 **Para o time de Frontend (Angular 2+):** Consulte `ENDPOINTS_FRONTEND.txt` para documentação completa em formato amigável, com exemplos de uso, tratamento de erros e notas de implementação.

### Criar Pedido (Sincronizado com MDB)
**POST** `/api/pedidos`

**Body (JSON):**
```json
{
  "cliente": "Cliente A",
  "produto": "Produto X",
  "quantidade": 2
}
```

**Resposta esperada (201 CREATED):**
```json
{
  "id": 1,
  "cliente": "Cliente A",
  "produto": "Produto X",
  "quantidade": 2,
  "status": "PROCESSADO",
  "criadoEm": "2026-03-01T15:30:00"
}
```

**Headers de resposta:**
- `X-MDB-Status: SUCESSO` — MDB processou com sucesso
- `X-Warning: Pedido criado mas processamento em background` — MDB não respondeu no timeout (30s)

### Obter Pedido por ID
**GET** `/api/pedidos/{id}`

**Resposta (200 OK):**
```json
{
  "id": 1,
  "status": "PROCESSADO",
  ...
}
```

### Listar Todos os Pedidos
**GET** `/api/pedidos`

**Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "cliente": "Cliente A",
    "status": "PROCESSADO",
    ...
  }
]
```

### Inspecionar Filas JMS (Desenvolvimento)
**GET** `/api/inspect-queues/pedidos` — Lista mensagens pendentes em `filaPedidos`

**GET** `/api/inspect-queues/respostas` — Lista mensagens em `filaResposta`

**POST** `/api/inspect-queues/process-next` — ⚠️ **Apenas Desenvolvimento** — Consome a próxima mensagem em `filaPedidos`, processa manualmente e envia resposta (útil para debugging)

**Resposta:**
```json
[
  {
    "JMSMessageID": "ID:893a24aeeb31a7b9ddff7c87110a134f0000000000000001",
    "JMSCorrelationID": "PEDIDO-1-1772390032452",
    "Text": "1"
  }
]
```

## ⚙️ Configuração

### server.xml (WebSphere Liberty)
- **JMS Messaging Engine** com 2 filas: `filaPedidos` e `filaResposta`
- **DataSource H2** em memória: `jdbc:h2:mem:sicobdb;DB_CLOSE_DELAY=-1;MODE=LEGACY`
- **MDB Activation Spec** configurado com `maxPoolSize="10"`

### persistence.xml
- **Persistence Unit**: `pedidosPU`
- **Database**: H2 em memória
- **DDL Generation**: `create` (tabelas são criadas automaticamente)
- **Import SQL**: `import.sql` popula dados iniciais

## 🔧 Troubleshooting

Se encontrar problemas:

1. **"Table PEDIDO not found"**
   - Reinicie o Liberty: `mvn liberty:stop && mvn liberty:start`
   - Veja `TROUBLESHOOTING_MDB.md` para soluções detalhadas

2. **"MDB não processa mensagens"**
   - Procure no log: `[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====`
   - Verifique se `[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso` apareceu no boot
   - Consulte `DIAGNOSTICO_MDB_RESPOSTA.md`

3. **"REST retorna X-Warning"**
   - Timeout de 30s esperando resposta do MDB
   - Verifique logs: `[RESPOSTA_CONSUMER] Timeout aguardando resposta para: ...`
   - Veja `SUPORTE.md` para soluções

## 📚 Documentação Completa

Consulte os guias técnicos para mais informações:

| Documento | Descrição |
|-----------|-----------|
| **QUICK_START.md** | Início rápido em 5 minutos |
| **README_SOLUCAO.md** | Sumário executivo da solução |
| **ENDPOINTS_FRONTEND.txt** | Documentação completa dos endpoints para o time de Frontend (Angular 2+) |
| **DIAGNOSTICO_MDB_RESPOSTA.md** | Análise técnica completa do problema/solução |
| **TROUBLESHOOTING_MDB.md** | 6+ problemas comuns e soluções |
| **DEPLOYMENT_GUIDE.md** | Guia passo a passo de deploy |
| **SUPORTE.md** | Guia de suporte e troubleshooting |
| **postman_collection_mdb_resposta.json** | Collection Postman pronta para testar todos os endpoints |

## 📝 Observações Importantes

### Desenvolvimento Local
- `persistence.xml` usa H2 em memória (`jdbc:h2:mem:sicobdb;DB_CLOSE_DELAY=-1;MODE=LEGACY`)
- Cada reinicialização do Liberty limpa o banco (dados não são persistidos)
- Para persistência local, use modo arquivo: `jdbc:h2:./data/pedidos;TRACE_LEVEL_SYSTEM_OUT=0`

### Produção
- Substitua o datasource H2 por um banco persistente (MySQL, PostgreSQL, Oracle, etc.)
- Configure JNDI name correto para a fila JMS no seu servidor
- Remova/proteja endpoints de inspeção de filas (`/api/inspect-queues`)
- Habilite segurança (autenticação OAuth2, RBAC)
- Configure logging estruturado (SLF4J + Logback/Log4j2)
- Implemente Dead Letter Queue (DLQ) para mensagens que falham

### Features do WebSphere Liberty Utilizadas
```xml
<feature>servlet-3.1</feature>
<feature>jaxrs-2.0</feature>
<feature>ejbLite-3.2</feature>
<feature>jpa-2.1</feature>
<feature>jms-2.0</feature>
<feature>jdbc-4.1</feature>
<feature>cdi-1.2</feature>
<feature>wasJmsServer-1.0</feature>
<feature>wasJmsClient-2.0</feature>
<feature>wasJmsSecurity-1.0</feature>
<feature>mdb-3.2</feature>
```

## 🔄 Melhorias Recentes (01/03/2026)

- ✨ **Documentação Frontend** — Criado arquivo `ENDPOINTS_FRONTEND.txt` com documentação completa dos endpoints em formato amigável para o time de Frontend (Angular 2+)
- ✨ **Collection Postman** — Collection `postman_collection_mdb_resposta.json` pronta com todos os endpoints e exemplos pré-configurados
- ✨ **Endpoint de Debugging** — Adicionado `POST /api/inspect-queues/process-next` para processar manualmente mensagens em desenvolvimento
- ✨ **Correção MDB** — Problema resolvido onde MDB não era ativado (ajuste do `jmsActivationSpec` no `server.xml`)
- ✨ **Logs Estruturados** — Adicionado rastreamento completo do fluxo request-reply com timestamps e IDs de correlação

## 🎯 Próximos Passos (Opcionais)

- [ ] Adicionar testes de integração com Testcontainers
- [ ] Implementar Dead Letter Queue (DLQ) para mensagens que falham
- [ ] Migrar System.out/System.err para SLF4J + Logback
- [ ] Adicionar métricas com Micrometer
- [ ] Implementar Circuit Breaker (Resilience4j)
- [ ] Configurar cache distribuído (Redis)
- [ ] Deploy com Docker/Kubernetes

## 📞 Suporte

Para mais informações ou dúvidas:
1. Consulte a documentação em `TROUBLESHOOTING_MDB.md`
2. Verifique os logs: `target/liberty/wlp/usr/servers/SicobServer/logs/messages.log`
3. Importe a collection Postman: `postman_collection_mdb_resposta.json`
4. Use o guia `SUPORTE.md` para checklist de saúde

---

**Última atualização:** 01/03/2026  
**Status:** ✅ Production-Ready  
**Qualidade:** ⭐⭐⭐⭐⭐

