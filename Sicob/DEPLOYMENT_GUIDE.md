# 🚀 Guia de Deployment - MDB Resposta no Liberty

## 📋 Pré-requisitos

- ✅ Java 8 ou superior
- ✅ Maven 3.6+
- ✅ IntelliJ IDEA com Liberty Tools plugin
- ✅ WebSphere Liberty (instalado via Maven)

---

## 🔧 Passos de Configuração

### 1️⃣ Compilar o Projeto

```bash
cd C:\Users\dell\workspace_itellJ\Sicob

# Compilar (sem package)
mvn clean compile

# Ou package completo (cria WAR)
mvn clean package
```

**Esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXXs
```

---

### 2️⃣ Iniciar Liberty (Primeira Vez)

#### Opção A: Via IntelliJ IDEA

1. Clique em **Liberty Tools** na barra lateral esquerda
2. Selecione a configuração Liberty
3. Clique em **Start Liberty Server**
4. Aguarde 30-60 segundos
5. Verifique logs na aba **Terminal** ou **Run**

#### Opção B: Via Linha de Comando

```bash
# Parar se já estiver rodando
liberty stop

# Iniciar
liberty start
```

**Esperado nos logs:**
```
[AUDIT   ] CWWKE0001I: The server liberty has been launched.
[AUDIT   ] CWWKZ0001I: Application pedidos-backend started
[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso
```

---

### 3️⃣ Deploy da Aplicação

#### Opção A: Deploy Automático (IntelliJ)

1. Abra o projeto no IntelliJ
2. Na janela **Project**, clique direito no projeto `Sicob`
3. Selecione **Liberty** → **Publish**
4. Aguarde 10-20 segundos

#### Opção B: Deploy Manual

```bash
# Dentro do projeto Sicob
mvn clean package liberty:create liberty:install-feature

# Depois redeploy
mvn liberty:publish
```

#### Opção C: Deploy Automático (Watch Mode)

```bash
mvn clean package liberty:dev
```

Este modo recompila e redeploy automaticamente quando você salva arquivos!

---

## ✅ Validação Pós-Deploy

### 1. Verifique se a Aplicação está Rodando

```bash
curl http://localhost:9080/pedidos-backend/api/pedidos
```

**Esperado:**
```json
[]
```
(Array vazio é esperado na primeira vez)

### 2. Crie um Pedido de Teste

```bash
curl -X POST http://localhost:9080/pedidos-backend/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": "Teste",
    "produto": "Produto X",
    "quantidade": 1
  }'
```

**Esperado:**
```json
{
  "id": 1,
  "cliente": "Teste",
  "produto": "Produto X",
  "quantidade": 1,
  "status": "PROCESSADO",
  "criadoEm": "2026-03-01T14:30:00"
}
```

Com header `X-MDB-Status: SUCESSO`

### 3. Verifique os Logs

No console Liberty, procure por:
```
[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====
[MDB_CONSUMER] Pedido processado com sucesso
[REST] Resposta recebida: SUCESSO
```

---

## 📂 Estrutura de Arquivos Após Deploy

```
target/
├── pedidos-backend-1.0-SNAPSHOT.war    # Seu aplicativo empacotado
├── liberty/
│   └── wlp/
│       ├── bin/
│       │   ├── server (script para parar/iniciar)
│       │   └── server.bat (Windows)
│       ├── etc/
│       │   └── server.xml (configuração principal)
│       └── usr/
│           ├── servers/
│           │   └── liberty/
│           │       ├── apps/ (aplicações deployadas)
│           │       ├── logs/ (arquivos de log)
│           │       ├── workarea/ (cache)
│           │       └── server.xml (configuração)
│           └── shared/
│               └── resources/ (drivers de banco, etc)
```

---

## 🛑 Parar e Reiniciar Liberty

### Parar

```bash
# Via Terminal no projeto
liberty stop

# Ou Ctrl+C se estiver rodando em foreground
```

### Reiniciar

```bash
# Parar
liberty stop

# Aguardar 5 segundos

# Iniciar
liberty start

# Ou em modo desenvolvedor (logs no console)
liberty run
```

---

## 🔍 Verificar Logs

### Logs em Tempo Real

```bash
# No modo run (foreground)
liberty run

# Pressione Ctrl+C para parar
```

### Logs em Arquivo

```bash
# Abrir logs da aplicação
cd target/liberty/wlp/usr/servers/liberty/logs
cat messages.log

# Ou apenas os últimas 50 linhas
tail -50 messages.log
```

### Filtrar Logs por Componente

```bash
# Apenas logs do MDB
tail -f messages.log | grep MDB_CONSUMER

# Apenas logs do REST
tail -f messages.log | grep REST

# Apenas logs do JMS
tail -f messages.log | grep PRODUCER
```

---

## 📊 Monitoramento em Tempo Real

### Terminal 1: Rodar Liberty

```bash
cd C:\Users\dell\workspace_itellJ\Sicob
liberty run
```

### Terminal 2: Enviar Requisições

```bash
# Teste contínuo a cada 5 segundos
while true; do
  echo "=== Criando pedido em $(date) ==="
  curl -X POST http://localhost:9080/pedidos-backend/api/pedidos \
    -H "Content-Type: application/json" \
    -d '{"cliente":"Teste","produto":"Prod","quantidade":1}'
  echo -e "\n"
  sleep 5
done
```

### Terminal 3: Acompanhar Logs

```bash
tail -f C:\Users\dell\workspace_itellJ\Sicob\target\liberty\wlp\usr\servers\liberty\logs\messages.log
```

---

## 🐛 Troubleshooting de Deploy

### Problema: "Address already in use (port 9080)"

**Solução:**
```bash
# Verifique se Liberty já está rodando
liberty status

# Se estiver, pare-o
liberty stop

# Aguarde 10 segundos e inicie novamente
liberty start
```

### Problema: "Application failed to deploy"

**Solução:**
```bash
# Limpar tudo e recompilar
mvn clean package

# Deletar workarea
rm -rf target/liberty/wlp/usr/servers/liberty/workarea

# Redeploy
liberty run
```

### Problema: "Falha ao carregar driver H2"

**Solução:**
```bash
# Copiar JAR do H2 para shared resources
cp ~/.m2/repository/com/h2database/h2/2.x.x/h2-2.x.x.jar \
   target/liberty/wlp/usr/shared/resources/

# Restart Liberty
liberty stop
liberty start
```

---

## 📈 Checklist de Deployment

- [ ] Maven compilou com sucesso
- [ ] Liberty iniciou sem erros
- [ ] Aplicação foi deployada (`CWWKZ0001I`)
- [ ] Console mostra `Consumer de resposta inicializado com sucesso`
- [ ] GET `/api/pedidos` retorna 200
- [ ] POST `/api/pedidos` retorna 201 com status `PROCESSADO`
- [ ] Logs mostram sequência completa (produção → MDB → resposta)
- [ ] Banco H2 foi criado com dados iniciais

---

## 🚨 Suporte e Referências

### Arquivos Importantes

- **Configuração**: `src/main/liberty/config/server.xml`
- **Logs**: `target/liberty/wlp/usr/servers/liberty/logs/messages.log`
- **POM**: `pom.xml` (dependências)
- **Diagnóstico**: `DIAGNOSTICO_MDB_RESPOSTA.md`
- **Troubleshooting**: `TROUBLESHOOTING_MDB.md`

### Documentação Official

- WebSphere Liberty: https://openliberty.io/docs/
- Apache OpenMQ: https://activemq.apache.org/
- Eclipse EclipseLink: https://www.eclipse.org/eclipselink/

### Contatos e Suporte

Se encontrar problema:
1. Verifique `TROUBLESHOOTING_MDB.md`
2. Veja os logs em `target/liberty/wlp/usr/servers/liberty/logs/messages.log`
3. Procure pela sequência de logs esperada
4. Identifique qual etapa está falhando

---

**Última atualização:** 01/03/2026
**Status:** ✅ Pronto para Produção

