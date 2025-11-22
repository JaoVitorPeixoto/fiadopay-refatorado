# FiadoPay Simulator (Spring Boot + H2)

### Contexto da Refatoração

Projeto refatorado seguindo os requisitos da atividade AVII, mantendo o contrato original da API (auth fake, criação/consulta de pagamentos, juros de parcelado, webhook, idempotência).


## A refatoração trouxe:

- Anotações customizadas

- Reflexão para plugins de pagamento e antifraude

- ExecutorService para webhooks e tarefas assíncronas

- Middleware de autenticação

- Padrões de arquitetura para maior coesão e extensibilidade

###  Estrutura do Projeto
```bash
A organização foi pensada para separar responsabilidades de Infraestrutura (Aspects, Config) de Regras de Negócio (Domain, Service).
src/main/java/edu/ucsal/fiadopay
├── config              # Classes de configurações da aplicação
├── annotation          # Metadados (Adesivos do sistema)
├── aspect              # Lógica Transversal (Antifraude/Logs)
├── domain              # Entidades e Enums
├── repo                # Camada de Dados
├── controller          # Camada de controle de requisições e respostas
├── dto                 # Camada de transferência de dados
├── mapper              # Métodos de parser
├── middleware          # Interceptors
├── service
│   ├── auth            # Lógica de Tokens e Autenticação
│   ├── merchant        # Gestão de Lojistas
│   ├── payment         # Core de Pagamentos (Strategy Factory)
│   └── webhook         # Motor de Entrega Assíncrona
|   └── signature       # Core para assinaturas (Strategy X-signature)
```

### Anotações Criadas

| Anotação | Finalidade | 
| -------- | ----- | 
| `@PaymentMethod(type="CARD")`      | Marca implementações de métodos de pagamento detectados via reflexão     | 
| `@AntiFraud(name="HighAmount", threshold=...)`        | Define regras antifraude plugáveis  
| `@Public`        | Permite ignorar autenticação no endpoint ou controller     |   
| `@WebhookSink`        | Marca operações relacionadas ao fluxo de entrega de webhooks     |     |  

### Middleware de Autenticação (Interceptor)

- Arquivo principal: AuthenticationInterceptor.java

- Responsável por:

- Validar header Authorization: Bearer FAKE-XXXX

- Resolver merchant autenticado via AuthService

- Liberar rotas anotadas com @Public

- Retornar erros em JSON padronizado

- Anexar o Merchant na request para uso posterior

Isso substitui filtros hardcoded e mantém o padrão clean.

### Threads e Concorrência (ExecutorService)

O sistema usa threads em três pontos:

#### 1) Processamento de pagamento

- Pagamentos são calculados/validados em threads separadas quando necessário.

#### 2) Motor de antifraude

- Cada regra anotada roda de maneira assíncrona, sem bloquear o fluxo principal.

#### 3) Webhooks

- ExecutorService com pool fixo de workers

- Retentativas automáticas com backoff (attempt * delay)

- Registro em banco de cada tentativa

- Thread pool finalizado com @PreDestroy

### Padrões Aplicados
| Padrão | Uso | 
| -------- | ----- | 
| Strategy      | Cálculo de juros, antifraude, envio de webhook e assinatura     | 
| Factory + Reflection        | Registro automático de métodos de pagamento |
| AOP (Aspect) | @AntiFraud Separa a validação de segurança da regra de negócio (Clean Code). |


## Rodar
```bash
mvn spring-boot:run
```

H2 console: http://localhost:8080/h2  
Swagger UI: http://localhost:8080/swagger-ui.html

## Fluxo

1) **Cadastrar merchant**
```bash
curl -X POST http://localhost:8080/fiadopay/admin/merchants   -H "Content-Type: application/json"   -d '{"name":"MinhaLoja ADS","webhookUrl":"http://localhost:8081/webhooks/payments"}'
```

2) **Obter token**
```bash
curl -X POST http://localhost:8080/fiadopay/auth/token   -H "Content-Type: application/json"   -d '{"client_id":"<clientId>","client_secret":"<clientSecret>"}'
```

3) **Criar pagamento**
```bash
curl -X POST http://localhost:8080/fiadopay/gateway/payments   -H "Authorization: Bearer FAKE-<merchantId>"   -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000"   -H "Content-Type: application/json"   -d '{"method":"CARD","currency":"BRL","amount":250.50,"installments":12,"metadataOrderId":"ORD-123"}'
```

4) **Consultar pagamento**
```bash
curl http://localhost:8080/fiadopay/gateway/payments/<paymentId>
```

5) **Reembolsar pagamento**
```bash
curl -X POST http://localhost:8080/fiadopay/gateway/refunds   -H "Authorization: Bearer FAKE-<merchantId>"   -H "Content-Type: application/json"   -d '{"paymentId": "<paymentId>"}'
```