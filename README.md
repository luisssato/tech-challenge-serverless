# tech-challenge-serverless

Módulo serverless de autenticação e gateway do **Tech Challenge — Fase 4**.

Provisiona a função **AWS Lambda** em Java 21 para autenticação de clientes via CPF e o **Amazon API Gateway** (REST API) atuando como ponto de entrada único (Reverse Proxy / Fachada) para os 3 microsserviços da oficina no EKS.

---

## Propósito

- **Autenticação de Clientes:** validação do CPF de clientes (formato e dígitos verificadores) e consulta à base de dados (`os.usuario`) para emissão de token JWT com claim `CLIENTE`.
- **Ponto de Entrada Centralizado (API Gateway):** provisionamento de um API Gateway REST atuando como gateway único da solução:
  - Roteia `/auth` para a Lambda de autenticação.
  - Encaminha `/os/{proxy+}` para o **`os-service`** no EKS.
  - Encaminha `/execution/{proxy+}` para o **`execution-service`** no EKS.
  - Encaminha `/billing/{proxy+}` para o **`billing-service`** no EKS.

---

## Tecnologias

- **Linguagem & Runtime:** Java 21 (AWS Lambda Managed Runtime)
- **Gerenciamento de Build:** Gradle 8.x
- **Infrastructure as Code (IaC):** AWS SAM CLI / AWS CloudFormation
- **Serviços AWS:** AWS Lambda, Amazon API Gateway, Amazon CloudWatch
- **Autenticação & JWT:** JJWT (`io.jsonwebtoken:jjwt`)
- **Acesso a Banco de Dados:** PostgreSQL JDBC Driver nativo
- **Testes Unitários:** JUnit 5, Mockito
- **CI/CD:** GitHub Actions

---

## Estrutura do Repositório

```
.github/workflows/
├── sam-validate.yml    CI: compilação, testes unitários e validação do template SAM
└── sam-deploy.yml      CD: build e deploy automático na AWS
lambda-auth/
├── template.yaml       Template SAM (Definição da Lambda e API Gateway com rotas /os, /execution, /billing)
└── AuthFunction/       Código-fonte da função de autenticação
    ├── build.gradle
    └── src/
        ├── main/java/com/fiap/auth/
        │   ├── AuthHandler.java          Handler de entrada da requisição API Gateway
        │   ├── DocumentoValidator.java   Validação matemática e normalização de CPF
        │   ├── UsuarioRepository.java    Consulta JDBC à base PostgreSQL (schema os)
        │   └── JwtUtil.java             Geração e assinatura do token JWT
        └── test/java/com/fiap/auth/
            ├── AuthHandlerTest.java
            ├── DocumentoValidatorTest.java
            ├── UsuarioRepositoryTest.java
            └── JwtUtilTest.java
```

---

## Arquitetura deste Repositório

```
                            ┌───────────────────────────┐
                            │   Amazon API Gateway      │
                            └─────────────┬─────────────┘
                                          │
         ┌────────────────────────┬───────┴───────┬─────────────────────────┐
         │ /auth                  │ /os/{proxy+}  │ /execution/{proxy+}     │ /billing/{proxy+}
         ▼                        ▼               ▼                         ▼
┌──────────────────┐    ┌──────────────────┐    ┌───────────────────┐    ┌───────────────────┐
│   AWS Lambda     │    │    os-service    │    │ execution-service │    │  billing-service  │
│  (AuthFunction)  │    │      (EKS)       │    │       (EKS)       │    │       (EKS)       │
└────────┬─────────┘    └──────────────────┘    └───────────────────┘    └───────────────────┘
         │ JDBC (VPC)
         ▼
┌──────────────────┐
│  RDS PostgreSQL  │ (schema: os)
└──────────────────┘
```

### Fluxo de Autenticação

- **Clientes:** enviam o CPF para `POST /auth`. A Lambda valida o documento, consulta a tabela `os.usuario` no PostgreSQL e retorna um JWT com a claim `role: "CLIENTE"`. O cliente utiliza este token no header `Authorization: Bearer <token>` para consumir as rotas protegidas dos microsserviços.
- **Usuários Internos:** realizam login por usuário e senha diretamente no `os-service` (`POST /os/api/auth/login`), que emite o JWT com a role correspondente (`GERENTE`, `ATENDENTE`, `MECANICO`).
- Ambos os emissores utilizam o mesmo `JWT_SECRET`, garantindo validação uniforme em todos os microsserviços.

---

## Pré-requisitos

- Java JDK 21
- AWS SAM CLI (v1.100+)
- Docker (para execução e testes locais com SAM)
- AWS CLI v2 com credenciais configuradas

---

## Execução e Testes Locais

### 1. Testes Unitários

```bash
cd lambda-auth/AuthFunction
./gradlew test
```

### 2. Validação do Template SAM

```bash
cd lambda-auth
sam validate --lint --template template.yaml
```

### 3. Execução da API Localmente

```bash
cd lambda-auth
sam build
sam local start-api
```

### 4. Chamadas de Teste

```bash
# Validação com CPF inválido (Retorno 400):
curl -X POST http://127.0.0.1:3000/auth \
  -H "Content-Type: application/json" \
  -d '{"cpf": "111.111.111-11"}'

# Validação com CPF válido:
curl -X POST http://127.0.0.1:3000/auth \
  -H "Content-Type: application/json" \
  -d '{"cpf": "529.982.247-25"}'
```

---

## CI/CD (GitHub Actions)

- **`sam-validate.yml` (Pull Request para `main`):** executa os testes unitários via Gradle e a validação do template (`sam validate --lint`).
- **`sam-deploy.yml` (Push na `main` ou disparo manual):** compila o código Java, executa os testes e realiza o deploy da stack no CloudFormation via `sam deploy`.

### Secrets do Repositório

| Secret | Descrição |
|---|---|
| `AWS_ACCESS_KEY_ID` | Chave de acesso AWS |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta AWS |
| `AWS_SESSION_TOKEN` | Token de sessão temporário (AWS Academy Learner Lab) |
| `JWT_SECRET` | Chave de assinatura dos tokens (mínimo 32 caracteres) |
| `DB_URL` | String de conexão JDBC do RDS do `os-service` (`jdbc:postgresql://<host>:5432/<db>`) |
| `DB_USER` | Usuário do banco de dados |
| `DB_PASSWORD` | Senha do banco de dados |
| `OS_SERVICE_LB_DNS` | DNS externo do Load Balancer (NLB) do `os-service` |
| `EXECUTION_SERVICE_LB_DNS` | DNS externo do Load Balancer (NLB) do `execution-service` |
| `BILLING_SERVICE_LB_DNS` | DNS externo do Load Balancer (NLB) do `billing-service` |
| `VPC_SUBNET_IDS` | Subnets privadas da VPC para a Lambda |
| `VPC_SECURITY_GROUP_IDS` | Security Group autorizado no banco de dados |

---

## Especificação da API (`POST /auth`)

### Contrato de Requisição
```json
POST /auth
Content-Type: application/json

{
  "cpf": "529.982.247-25"
}
```

### Respostas da API

- **`200 OK` — Autenticação realizada com sucesso:**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "role": "CLIENTE",
    "expiresIn": 3600
  }
  ```

- **`400 Bad Request` — CPF ou formato inválido:**
  ```json
  {
    "message": "CPF com formato ou digito verificador invalido"
  }
  ```

- **`401 Unauthorized` — Cliente não localizado na base:**
  ```json
  {
    "message": "Cliente nao encontrado ou nao cadastrado"
  }
  ```

- **`500 Internal Server Error` — Erro de conexão ou indisponibilidade:**
  ```json
  {
    "message": "Erro interno. Por favor, tente novamente."
  }
  ```

---

## Outputs do CloudFormation

Após o provisionamento na AWS, a stack exporta os seguintes valores:

- **`AuthEndpoint`:** URL HTTPS pública para autenticação de clientes (`POST /auth`).
- **`OsProxyEndpoint`:** URL base do proxy para o `os-service` (`https://<api-id>.execute-api.<region>.amazonaws.com/Prod/os`).
- **`ExecutionProxyEndpoint`:** URL base do proxy para o `execution-service` (`https://<api-id>.execute-api.<region>.amazonaws.com/Prod/execution`).
- **`BillingProxyEndpoint`:** URL base do proxy para o `billing-service` (`https://<api-id>.execute-api.<region>.amazonaws.com/Prod/billing`).
- **`AuthFunctionArn`:** ARN da função Lambda provisionada.

---
