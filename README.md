# ERP Desktop

Sistema ERP Desktop completo desenvolvido em **Java 17 + JavaFX 21 + PostgreSQL**.

## 📋 Módulos Implementados

| Módulo | Descrição |
|--------|-----------|
| 🔐 Login | Autenticação com BCrypt, configuração do banco na tela |
| 📊 Dashboard | Resumo do dia: vendas, estoque baixo, indicadores financeiros |
| 🛒 Frente de Caixa (PDV) | Busca por código de barras/nome, múltiplas formas de pagamento |
| 👥 Clientes | CRUD completo com endereço, CPF/CNPJ, crédito |
| 📦 Produtos | CRUD com custo/venda, margem, estoque, NCM/CFOP |
| 🏭 Fornecedores | CRUD completo com dados fiscais |
| 📋 Estoque | Visualização com alertas de estoque baixo/zerado |
| 💰 Financeiro | Contas a pagar e a receber, baixa de títulos |
| 👔 Funcionários (RH) | Cadastro de funcionários com cargo e salário |
| 📈 Relatórios | Relatório de vendas por período, estoque baixo |

## 🚀 Pré-requisitos

- **Java 17+** (JDK) — [Download](https://adoptium.net/)
- **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 14+** — [Download](https://www.postgresql.org/download/)

## 🗄️ Configuração do Banco de Dados

1. Crie o banco de dados no PostgreSQL:
```sql
CREATE DATABASE erp_desktop;
```

2. Execute o schema:
```bash
psql -U postgres -d erp_desktop -f src/main/resources/sql/schema.sql
```

3. Configure a conexão em `src/main/resources/database.properties`:
```properties
db.host=localhost
db.port=5432
db.name=erp_desktop
db.user=postgres
db.password=SUA_SENHA_AQUI
```

> ⚡ Alternativa: configure diretamente na tela de login, na seção "Configuração do Banco de Dados"

## ▶️ Como Executar

```bash
cd ERP-Desktop
mvn javafx:run
```

## 📦 Gerar Executável (JAR)

```bash
mvn clean package
java -jar target/erp-desktop-1.0.0.jar
```

## 🔐 Login Padrão

| Campo | Valor |
|-------|-------|
| Usuário | `admin` |
| Senha | `admin123` |

## 🛠️ Tecnologias Utilizadas

- **JavaFX 21** — Interface gráfica
- **PostgreSQL + HikariCP** — Banco de dados e connection pool
- **BCrypt** — Criptografia de senhas
- **ControlsFX** — Componentes extras
- **Apache POI** — Exportação Excel
- **iText** — Exportação PDF
- **Logback** — Logging

## 📁 Estrutura do Projeto

```
ERP-Desktop/
├── pom.xml
└── src/main/
    ├── java/com/erp/
    │   ├── Main.java              # Ponto de entrada
    │   ├── config/
    │   │   └── DatabaseConfig.java
    │   ├── model/                 # Entidades
    │   │   ├── Usuario.java
    │   │   ├── Cliente.java
    │   │   ├── Produto.java
    │   │   ├── Venda.java
    │   │   ├── ItemVenda.java
    │   │   ├── Fornecedor.java
    │   │   ├── Funcionario.java
    │   │   ├── ContaPagar.java
    │   │   └── ContaReceber.java
    │   ├── dao/                   # Acesso ao banco
    │   ├── view/                  # Telas JavaFX
    │   └── util/                  # Utilitários
    └── resources/
        ├── css/style.css          # Tema escuro
        ├── sql/schema.sql         # DDL PostgreSQL
        └── database.properties    # Config do banco
```
