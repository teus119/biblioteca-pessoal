# 📚 Biblioteca Pessoal

Gerenciador de biblioteca pessoal completo com backend Spring Boot, MongoDB e frontend web responsivo.

## 🚀 Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 3.2 + Java 21 |
| Banco de Dados | MongoDB 7.0 |
| Segurança | Spring Security + JWT (JJWT 0.12) |
| Testes | Testcontainers + WireMock (VCR) |
| Cobertura | Jacoco ≥ 80% |
| CI/CD | GitHub Actions |
| Qualidade | SonarCloud |
| Frontend | HTML5 + CSS3 + JavaScript (ES6+) |

---

## 🏁 Como Rodar Localmente

### Pré-requisitos
- Java 21+
- Maven 3.9+
- Docker Desktop (para Testcontainers e MongoDB local)

### 1. Subir o MongoDB
```bash
docker compose up mongodb -d
```

### 2. Rodar o backend
```bash
cd backend
mvn spring-boot:run
```

O servidor inicia em `http://localhost:8080`

### 3. Abrir o frontend
Abra o arquivo `frontend/index.html` diretamente no navegador, ou use um servidor estático:
```bash
# Com Python (se instalado)
cd frontend
python -m http.server 3000
# Acesse: http://localhost:3000
```

---

## 🧪 Executar Testes

```bash
cd backend
mvn verify
```

> ⚠️ **Docker deve estar rodando!** Os testes usam Testcontainers que sobe um container MongoDB automaticamente.

### Relatório de cobertura
Após `mvn verify`, abra:
```
backend/target/site/jacoco/index.html
```

---

## 🔍 SonarCloud (CI)

O projeto está integrado ao **SonarCloud** via GitHub Actions.

Para configurar no seu repositório:
1. Acesse [sonarcloud.io](https://sonarcloud.io) e conecte seu GitHub
2. Importe o repositório `biblioteca-pessoal`
3. Adicione os seguintes **Secrets** no GitHub:
   - `SONAR_TOKEN` — gerado no SonarCloud
   - `SONAR_PROJECT_KEY` — ex: `seu-usuario_biblioteca-pessoal`
   - `SONAR_ORGANIZATION` — ex: `seu-usuario`

---

## 📁 Estrutura do Projeto

```
biblioteca-pessoal/
├── backend/                    # API Spring Boot
│   ├── src/main/java/com/biblioteca/
│   │   ├── config/             # Segurança, MongoDB, CORS
│   │   ├── controller/         # AuthController, BookController
│   │   ├── dto/                # AuthDTO, BookDTO
│   │   ├── exception/          # GlobalExceptionHandler
│   │   ├── model/              # User, Book
│   │   ├── repository/         # UserRepository, BookRepository
│   │   ├── security/           # JwtService, JwtAuthenticationFilter
│   │   └── service/            # AuthService, BookService, IsbnLookupService
│   └── src/test/java/com/biblioteca/
│       ├── unit/               # Caixa Branca: BookService, AuthService, JwtService
│       ├── integration/        # Testcontainers: BookRepository
│       ├── controller/         # E2E: AuthController, BookController
│       └── vcr/                # WireMock VCR: IsbnLookupService
├── frontend/
│   ├── index.html              # Login
│   ├── register.html           # Cadastro
│   ├── dashboard.html          # Dashboard principal
│   ├── css/styles.css          # Design system dark mode
│   └── js/
│       ├── api.js              # Wrapper REST
│       ├── auth.js             # Sessão JWT + Toast
│       └── dashboard.js        # Lógica do dashboard
├── .github/workflows/ci.yml    # GitHub Actions CI
├── docker-compose.yml          # MongoDB + SonarQube
└── RTM.md                      # Matriz de Rastreabilidade
```

---

## 🔐 API Endpoints

### Auth (público)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Cadastrar usuário |
| POST | `/api/auth/login` | Login → JWT |
| GET | `/api/auth/me` | Perfil do usuário logado |

### Books (requer JWT Bearer Token)
| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/books` | Listar livros do usuário |
| POST | `/api/books` | Criar livro |
| GET | `/api/books/{id}` | Buscar livro por ID |
| PUT | `/api/books/{id}` | Atualizar livro |
| DELETE | `/api/books/{id}` | Deletar livro |
| GET | `/api/books/search?q=` | Buscar por título/autor |
| GET | `/api/books/stats` | Estatísticas pessoais |
| GET | `/api/books/isbn/{isbn}` | Buscar metadados por ISBN (VCR) |

---

## 🧪 Estratégia de Testes

| Tipo | Localização | Tecnologia | Descrição |
|---|---|---|---|
| Unitários (Caixa Branca) | `unit/` | Testcontainers + JUnit 5 | Lógica interna de Services |
| Integração | `integration/` | Testcontainers + @ParameterizedTest | Repository + queries MongoDB |
| E2E (Caixa Preta) | `controller/` | MockMvc + Testcontainers | Endpoints HTTP completos |
| VCR | `vcr/` | WireMock | Chamadas HTTP externas gravadas |

> ✅ **Zero mocks!** Todos os testes usam MongoDB real via Testcontainers.
