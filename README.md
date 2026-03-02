# Projeto SICOB - Teste de JMS com Angular e EJB

## 📋 Visão Geral

Este repositório contém uma solução completa de estudo de caso para demonstrar a integração entre **Frontend Angular**, **Backend EJB/JPA** e **Mensageria JMS** usando a plataforma **IBM Liberty**.

A arquitetura foi desenvolvida para validar padrões de comunicação assíncrona com Message-Driven Beans em um ambiente empresarial.

---

## 🏗️ Arquitetura

```
┌─────────────────┐
│    Frontend     │
│    Angular      │
└────────┬────────┘
         │ API REST
         ▼
┌─────────────────────┐
│   Backend API REST  │
│  EJB / JPA          │
└────────┬────────────┘
         │ Mensagens
         ▼
┌─────────────────────────────┐
│   Message-Driven Bean (MDB) │
│   (Processamento Assíncrono)│
└────────┬────────────────────┘
         │
         ▼
┌─────────────────┐
│  Base de Dados  │
│    (JPA)        │
└─────────────────┘
         ▲
         │
┌────────────────────────────┐
│   Mensageria JMS           │
│   (Comunicação Assíncrona) │
└────────────────────────────┘
```

---

## 📁 Estrutura dos Projetos

### 1️⃣ **Sicob** (Backend)
**Finalidade:** Teste e demonstração de JMS com Message-Driven Beans (MDB)

**Stack Tecnológico:**
- Java EE / Jakarta EE
- EJB (Enterprise JavaBeans)
- JPA (Java Persistence API)
- JMS (Java Message Service)
- IBM Liberty (Application Server)
- Maven (Build)

**Componentes Principais:**
- API REST para operações CRUD
- Message-Driven Beans para processamento assíncrono
- Persistência com JPA
- Integração com JMS para mensageria

**Localização:** `/Sicob`

---

### 2️⃣ **Sicob-Front** (Frontend)
**Finalidade:** Interface de usuário para interação com o Backend

**Stack Tecnológico:**
- Angular (Framework Web)
- TypeScript
- Angular CLI
- RxJS (Reactive Programming)
- HTML5 / CSS3

**Componentes Principais:**
- Componentes Angular reutilizáveis
- Serviços para comunicação REST
- Formulários de entrada de dados
- Listagem e visualização de dados

**Localização:** `/Sicob-Front`

---

## 🔄 Fluxo de Comunicação

1. **Frontend (Angular)** → Envia requisições HTTP REST para o Backend
2. **Backend (API REST)** → Recebe requisições e processa dados com EJB/JPA
3. **Backend → JMS** → Publica mensagens na fila de mensagens
4. **Message-Driven Bean (MDB)** → Consome mensagens de forma assíncrona
5. **MDB → Banco de Dados** → Persiste dados via JPA
6. **Frontend ← Backend** → Retorna respostas HTTP ao cliente

---

## 🚀 Como Usar

### Backend (Sicob)

```bash
cd Sicob
mvn clean install
mvn liberty:run
```

O backend estará disponível em: `http://localhost:9080`

### Frontend (Sicob-Front)

```bash
cd Sicob-Front
npm install
ng serve
```

O frontend estará disponível em: `http://localhost:4200`

---

## 📝 Funcionalidades

### Backend
- ✅ Gerenciamento de Pedidos (CRUD)
- ✅ Processamento assíncrono com MDB
- ✅ Persistência em banco de dados
- ✅ APIs REST documentadas

### Frontend
- ✅ Formulário de criação de pedidos
- ✅ Listagem de pedidos
- ✅ Comunicação em tempo real com backend
- ✅ Interface responsiva

---

## 📚 Conceitos Demonstrados

| Conceito | Descrição |
|----------|-----------|
| **JMS (Java Message Service)** | Padrão de mensageria assíncrona |
| **MDB (Message-Driven Bean)** | Componente EJB que processa mensagens |
| **REST API** | Comunicação síncrona entre frontend e backend |
| **JPA** | Mapeamento objeto-relacional |
| **Angular** | Framework moderno para frontend |
| **EJB** | Componentes Java Enterprise |
| **IBM Liberty** | Servidor de aplicações leve |

---

## 🛠️ Requisitos

- **Java 8+** (recomendado Java 11 ou superior)
- **Node.js 14+** e **npm 6+**
- **Maven 3.6+**
- **Git**

---

## 📖 Documentação Adicional

Consulte os READMEs específicos em cada projeto:

- [Sicob - Backend README](./Sicob/README.md)
- [Sicob-Front - Frontend README](./Sicob-Front/README.md)

---

## 👥 Contribuidores

Este projeto é um estudo de caso educacional para demonstração de padrões arquiteturais em Java EE.

---

## 📄 Licença

Este projeto é fornecido como material educacional.

---

**Versão:** 1.0  
**Última atualização:** 2026-03-02

