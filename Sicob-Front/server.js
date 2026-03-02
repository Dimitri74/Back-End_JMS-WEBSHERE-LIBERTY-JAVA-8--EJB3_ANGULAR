const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 4200;
const ROOT_DIR = path.join(__dirname, 'src');

const server = http.createServer((req, res) => {
  // Rota padrão é index.html
  let filePath;
  // rotas estáticas simples para demonstrar dashboard quando Angular não está funcionando
  if (req.url === '/') {
    filePath = '/index.html';
  } else if (req.url === '/pedido-list' || req.url === '/pedido-form') {
    // serve uma pequena página de placeholder
    const pageName = req.url.slice(1);
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`<!doctype html><html><head><meta charset="utf-8"><title>${pageName}</title></head><body><h2>${pageName}</h2><p>Apresente aqui a interface do ${pageName} (não implementado).</p><a href="/">Voltar</a></body></html>`);
    return;
  } else {
    filePath = req.url;
  }
  filePath = path.join(ROOT_DIR, filePath);

  // Previne directory traversal
  if (!filePath.startsWith(ROOT_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  // Tenta servir o arquivo
  fs.readFile(filePath, (err, content) => {
    if (err) {
      // Se não encontra, tenta index.html (para SPA)
      fs.readFile(path.join(ROOT_DIR, 'index.html'), (err, content) => {
        if (err) {
          res.writeHead(404, { 'Content-Type': 'text/plain' });
          res.end('404 - Arquivo não encontrado');
        } else {
          res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
          res.end(content);
        }
      });
    } else {
      // Determina o tipo de conteúdo baseado na extensão
      let contentType = 'text/plain';
      if (filePath.endsWith('.html')) contentType = 'text/html';
      else if (filePath.endsWith('.js')) contentType = 'application/javascript';
      else if (filePath.endsWith('.css')) contentType = 'text/css';
      else if (filePath.endsWith('.json')) contentType = 'application/json';

      res.writeHead(200, { 'Content-Type': contentType + '; charset=utf-8' });
      res.end(content);
    }
  });
});

server.listen(PORT, () => {
  console.log(`✅ Servidor rodando em http://localhost:${PORT}`);
  console.log(`📂 Servindo arquivos de: ${ROOT_DIR}`);
  console.log(`\nPressione Ctrl+C para parar o servidor`);
});
