# SICCOB - Frontend (Angular)

Aplicação frontend escrita em Angular 15 para cadastro e listagem de pedidos.
Este repositório contém a interface que consome uma API REST exposta em `environment.apiUrl`.

---

## Pré‑requisitos ⚙️

- Node.js 18 ou superior (versão LTS recomendada).
- `npm` vem junto com o Node.
- Opcional: Angular CLI global (`npm install -g @angular/cli`) para conveniência.

---

## Quick Start 🚀

Siga estes passos a partir da raiz do workspace:

```bash
# 1. entre na pasta do projeto
cd C:\Users\dell\workspace_itellJ\Sicob-Front

# 2. instale todas as dependências do projeto
npm install

# 3. (primeira vez) certifique-se de que o arquivo tsconfig.app.json existe
#    – o CLI cria automaticamente se faltar.

# 4. iniciar o servidor de desenvolvimento
npm start
# (ou, se preferir) ng serve --open
```

O comando acima compila a aplicação e abre o navegador em `http://localhost:4200`.
O servidor permanece ativo até interrompê‑lo (Ctrl+C).

Para gerar uma build de produção:

```bash
npm run build
```

Os artefatos de build vão para `dist/sicob-front`.

### Conexão com backend / CORS

Durante o desenvolvimento, o servidor Angular roda em `localhost:4200` e
o backend em outra porta (por exemplo `localhost:9080`).
Por padrão o navegador bloqueia requisições cruzadas (CORS) quando o servidor
remoto não envia o cabeçalho `Access-Control-Allow-Origin`.

Duas abordagens para resolver:

1. **Ativar CORS no backend** – configurar a API para retornar
   `Access-Control-Allow-Origin: *` (ou o domínio do frontend) em todas as
   respostas. Exemplo em Express:

   ```js
   app.use((req, res, next) => {
     res.header('Access-Control-Allow-Origin', '*');
     res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
     next();
   });
   ```

2. **Usar proxy do Angular** (recomendado durante dev).  O projeto já
   inclui o arquivo `proxy.conf.json` e o comando `npm start` está ajustado
   para utilizá‑lo. Ele redireciona requisições de `/api/*` para o backend
   (`http://localhost:9080/pedidos-backend`) e evita o problema de CORS.

  A configuração atual do frontend em desenvolvimento está em
  `src/environments/environment.ts` com:

  ```ts
  apiUrl: '/api'
  ```

   ```json
   {
     "/api": {
       "target": "http://localhost:9080/pedidos-backend",
       "secure": false,
       "changeOrigin": true,
       "logLevel": "debug"
     }
   }
   ```

   Nesse cenário, a aplicação continua chamando `http://localhost:4200/api/pedidos`;
   o proxy passa o pedido adiante e o navegador não vê recursos de domínios
   diferentes.



---

## Notas importantes 📝

- Em desenvolvimento, a base da API está configurada como `/api` em
  `src/environments/environment.ts` e é resolvida pelo `proxy.conf.json`.
- Componentes existentes:
  - `PedidoFormComponent` – formulário de pedido
  - `PedidoListComponent` – lista de pedidos
- Serviço: `PedidoService` realiza chamadas HTTP para o backend.
- O conteúdo estático de fallback foi removido de `src/index.html`; a interface
  exibida é a da aplicação Angular.

Se os erros `Cannot find module '@angular/core'...` aparecerem,
verifique se as dependências estão instaladas e reinicie o servidor TS
(Ctrl+Shift+P → *TypeScript: Restart TS Server*).

Se o CLI reclamar de `root` ausente em `angular.json`, abra o arquivo e
adicione a propriedade:

```json
"root": "",
```

(isto já foi corrigido no repositório atual.)

---

## Desenvolvimento 🛠️

- `npm test` — executar testes (quando implementados).
- `npm run lint` — análise de código.
- Adicione novas dependências com `npm install <pacote> --save`.


Este README pode ser atualizado com instruções adicionais conforme o
projeto evoluir.
