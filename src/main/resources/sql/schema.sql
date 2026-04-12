-- ============================================================
-- Schema DRS ERP - PostgreSQL
-- ============================================================

-- Extensões
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- ============================================================
-- TABELA: usuarios
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    login       VARCHAR(50)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL DEFAULT 'OPERADOR', -- ADMIN, GERENTE, OPERADOR
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP  NOT NULL DEFAULT NOW()
);

-- Admin padrão (senha: admin123)
INSERT INTO usuarios (nome, login, senha_hash, perfil) VALUES
('Administrador', 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCYi6jIYmUWCOpsTRBRtlLm', 'ADMIN')
ON CONFLICT DO NOTHING;

-- ============================================================
-- TABELA: clientes
-- ============================================================
CREATE TABLE IF NOT EXISTS clientes (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    cpf_cnpj    VARCHAR(20)  UNIQUE,
    tipo_pessoa CHAR(1)      NOT NULL DEFAULT 'F', -- F=Física, J=Jurídica
    rg_ie       VARCHAR(20),
    email       VARCHAR(100),
    telefone    VARCHAR(20),
    celular     VARCHAR(20),
    cep         VARCHAR(10),
    logradouro  VARCHAR(150),
    numero      VARCHAR(10),
    complemento VARCHAR(80),
    bairro      VARCHAR(80),
    cidade      VARCHAR(80),
    estado      CHAR(2),
    limite_credito  NUMERIC(15,2) DEFAULT 0,
    observacoes TEXT,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: fornecedores
-- ============================================================
CREATE TABLE IF NOT EXISTS fornecedores (
    id          SERIAL PRIMARY KEY,
    razao_social VARCHAR(150) NOT NULL,
    nome_fantasia VARCHAR(100),
    cnpj        VARCHAR(20) UNIQUE,
    ie          VARCHAR(20),
    email       VARCHAR(100),
    telefone    VARCHAR(20),
    celular     VARCHAR(20),
    cep         VARCHAR(10),
    logradouro  VARCHAR(150),
    numero      VARCHAR(10),
    complemento VARCHAR(80),
    bairro      VARCHAR(80),
    cidade      VARCHAR(80),
    estado      CHAR(2),
    contato     VARCHAR(100),
    observacoes TEXT,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: categorias
-- ============================================================
CREATE TABLE IF NOT EXISTS categorias (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(80) NOT NULL UNIQUE,
    descricao   VARCHAR(200),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO categorias (nome) VALUES
('Geral'), ('Alimentos'), ('Bebidas'), ('Limpeza'),
('Higiene'), ('Eletrônicos'), ('Vestuário'), ('Papelaria')
ON CONFLICT DO NOTHING;

-- ============================================================
-- TABELA: produtos
-- ============================================================
CREATE TABLE IF NOT EXISTS produtos (
    id              SERIAL PRIMARY KEY,
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    codigo_barras   VARCHAR(50) UNIQUE,
    nome            VARCHAR(150) NOT NULL,
    descricao       TEXT,
    categoria_id    INT REFERENCES categorias(id),
    fornecedor_id   INT REFERENCES fornecedores(id),
    unidade         VARCHAR(10) NOT NULL DEFAULT 'UN',
    preco_custo     NUMERIC(15,2) NOT NULL DEFAULT 0,
    preco_venda     NUMERIC(15,2) NOT NULL DEFAULT 0,
    margem_lucro    NUMERIC(5,2) DEFAULT 0,
    estoque_atual   NUMERIC(15,3) NOT NULL DEFAULT 0,
    estoque_minimo  NUMERIC(15,3) NOT NULL DEFAULT 0,
    estoque_maximo  NUMERIC(15,3) DEFAULT 0,
    ncm             VARCHAR(10),
    cfop            VARCHAR(10),
    icms_aliquota   NUMERIC(5,2) DEFAULT 0,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: vendas
-- ============================================================
CREATE TABLE IF NOT EXISTS vendas (
    id              SERIAL PRIMARY KEY,
    numero          VARCHAR(20) NOT NULL UNIQUE,
    cliente_id      INT REFERENCES clientes(id),
    usuario_id      INT NOT NULL REFERENCES usuarios(id),
    data_venda      TIMESTAMP NOT NULL DEFAULT NOW(),
    subtotal        NUMERIC(15,2) NOT NULL DEFAULT 0,
    desconto        NUMERIC(15,2) NOT NULL DEFAULT 0,
    acrescimo       NUMERIC(15,2) NOT NULL DEFAULT 0,
    total           NUMERIC(15,2) NOT NULL DEFAULT 0,
    forma_pagamento VARCHAR(30) NOT NULL DEFAULT 'DINHEIRO',
    valor_pago      NUMERIC(15,2) DEFAULT 0,
    troco           NUMERIC(15,2) DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'FINALIZADA', -- RASCUNHO, FINALIZADA, CANCELADA
    observacoes     TEXT,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: itens_venda
-- ============================================================
CREATE TABLE IF NOT EXISTS itens_venda (
    id          SERIAL PRIMARY KEY,
    venda_id    INT NOT NULL REFERENCES vendas(id) ON DELETE CASCADE,
    produto_id  INT NOT NULL REFERENCES produtos(id),
    quantidade  NUMERIC(15,3) NOT NULL,
    preco_unit  NUMERIC(15,2) NOT NULL,
    desconto    NUMERIC(15,2) NOT NULL DEFAULT 0,
    subtotal    NUMERIC(15,2) NOT NULL
);

-- ============================================================
-- TABELA: movimentacoes_estoque
-- ============================================================
CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
    id              SERIAL PRIMARY KEY,
    produto_id      INT NOT NULL REFERENCES produtos(id),
    tipo            VARCHAR(10) NOT NULL, -- ENTRADA, SAIDA, AJUSTE
    quantidade      NUMERIC(15,3) NOT NULL,
    estoque_anterior NUMERIC(15,3) NOT NULL,
    estoque_novo    NUMERIC(15,3) NOT NULL,
    motivo          VARCHAR(100),
    venda_id        INT REFERENCES vendas(id),
    usuario_id      INT REFERENCES usuarios(id),
    data_mov        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: contas_pagar
-- ============================================================
CREATE TABLE IF NOT EXISTS contas_pagar (
    id              SERIAL PRIMARY KEY,
    descricao       VARCHAR(200) NOT NULL,
    fornecedor_id   INT REFERENCES fornecedores(id),
    valor           NUMERIC(15,2) NOT NULL,
    data_emissao    DATE NOT NULL DEFAULT CURRENT_DATE,
    data_vencimento DATE NOT NULL,
    data_pagamento  DATE,
    valor_pago      NUMERIC(15,2),
    forma_pagamento VARCHAR(30),
    status          VARCHAR(20) NOT NULL DEFAULT 'ABERTA', -- ABERTA, PAGA, VENCIDA, CANCELADA
    categoria       VARCHAR(80),
    observacoes     TEXT,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: contas_receber
-- ============================================================
CREATE TABLE IF NOT EXISTS contas_receber (
    id              SERIAL PRIMARY KEY,
    descricao       VARCHAR(200) NOT NULL,
    cliente_id      INT REFERENCES clientes(id),
    venda_id        INT REFERENCES vendas(id),
    valor           NUMERIC(15,2) NOT NULL,
    data_emissao    DATE NOT NULL DEFAULT CURRENT_DATE,
    data_vencimento DATE NOT NULL,
    data_recebimento DATE,
    valor_recebido  NUMERIC(15,2),
    forma_recebimento VARCHAR(30),
    status          VARCHAR(20) NOT NULL DEFAULT 'ABERTA', -- ABERTA, RECEBIDA, VENCIDA, CANCELADA
    observacoes     TEXT,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABELA: cargos
-- ============================================================
CREATE TABLE IF NOT EXISTS cargos (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(80) NOT NULL UNIQUE,
    salario_base NUMERIC(15,2) DEFAULT 0,
    descricao   VARCHAR(200)
);

INSERT INTO cargos (nome, salario_base) VALUES
('Gerente', 5000.00), ('Vendedor', 2000.00),
('Caixa', 1800.00), ('Estoquista', 1800.00)
ON CONFLICT DO NOTHING;

-- ============================================================
-- TABELA: funcionarios
-- ============================================================
CREATE TABLE IF NOT EXISTS funcionarios (
    id              SERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    cpf             VARCHAR(14)  UNIQUE,
    rg              VARCHAR(20),
    data_nascimento DATE,
    data_admissao   DATE NOT NULL DEFAULT CURRENT_DATE,
    data_demissao   DATE,
    cargo_id        INT REFERENCES cargos(id),
    salario         NUMERIC(15,2) NOT NULL DEFAULT 0,
    email           VARCHAR(100),
    telefone        VARCHAR(20),
    celular         VARCHAR(20),
    cep             VARCHAR(10),
    logradouro      VARCHAR(150),
    numero          VARCHAR(10),
    complemento     VARCHAR(80),
    bairro          VARCHAR(80),
    cidade          VARCHAR(80),
    estado          CHAR(2),
    observacoes     TEXT,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ÍNDICES para performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_produtos_codigo_barras ON produtos(codigo_barras);
CREATE INDEX IF NOT EXISTS idx_produtos_nome ON produtos(nome);
CREATE INDEX IF NOT EXISTS idx_clientes_nome ON clientes(nome);
CREATE INDEX IF NOT EXISTS idx_clientes_cpf_cnpj ON clientes(cpf_cnpj);
CREATE INDEX IF NOT EXISTS idx_vendas_data ON vendas(data_venda);
CREATE INDEX IF NOT EXISTS idx_vendas_cliente ON vendas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_contas_pagar_vencimento ON contas_pagar(data_vencimento);
CREATE INDEX IF NOT EXISTS idx_contas_receber_vencimento ON contas_receber(data_vencimento);

-- ============================================================
-- VIEW: resumo_dashboard
-- ============================================================
CREATE OR REPLACE VIEW vw_vendas_hoje AS
SELECT
    COUNT(*)          AS total_vendas,
    COALESCE(SUM(total), 0) AS valor_total,
    COALESCE(AVG(total), 0) AS ticket_medio
FROM vendas
WHERE DATE(data_venda) = CURRENT_DATE
  AND status = 'FINALIZADA';

CREATE OR REPLACE VIEW vw_estoque_baixo AS
SELECT p.id, p.codigo, p.nome, p.estoque_atual, p.estoque_minimo,
       c.nome AS categoria
FROM produtos p
LEFT JOIN categorias c ON c.id = p.categoria_id
WHERE p.estoque_atual <= p.estoque_minimo
  AND p.ativo = TRUE
ORDER BY p.estoque_atual ASC;

CREATE OR REPLACE VIEW vw_contas_vencer AS
SELECT 'PAGAR' AS tipo, descricao, valor, data_vencimento,
       (data_vencimento - CURRENT_DATE) AS dias_para_vencer
FROM contas_pagar
WHERE status = 'ABERTA' AND data_vencimento <= CURRENT_DATE + 7
UNION ALL
SELECT 'RECEBER' AS tipo, descricao, valor, data_vencimento,
       (data_vencimento - CURRENT_DATE) AS dias_para_vencer
FROM contas_receber
WHERE status = 'ABERTA' AND data_vencimento <= CURRENT_DATE + 7
ORDER BY data_vencimento;
