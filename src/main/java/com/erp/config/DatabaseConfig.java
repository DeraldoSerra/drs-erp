package com.erp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    private static String url;
    private static String usuario;
    private static String senha;
    private static String banco;
    private static String host;
    private static int porta;

    static {
        carregarConfiguracao();
    }

    private static void carregarConfiguracao() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (in != null) {
                props.load(in);
                host    = props.getProperty("db.host", "localhost");
                porta   = Integer.parseInt(props.getProperty("db.port", "5432"));
                banco   = props.getProperty("db.name", "erp_desktop");
                usuario = props.getProperty("db.user", "postgres");
                senha   = props.getProperty("db.password", "postgres");
                boolean ssl = Boolean.parseBoolean(props.getProperty("db.ssl", "false"));
                url = "jdbc:postgresql://" + host + ":" + porta + "/" + banco
                        + (ssl ? "?sslmode=require" : "");
            } else {
                host    = "localhost";
                porta   = 5432;
                banco   = "erp_desktop";
                usuario = "postgres";
                senha   = "postgres";
                url = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
            }
        } catch (IOException e) {
            log.error("Erro ao carregar database.properties", e);
        }
    }

    public static void inicializar(String dbHost, int dbPorta, String dbNome,
                                    String dbUsuario, String dbSenha) {
        host    = dbHost;
        porta   = dbPorta;
        banco   = dbNome;
        usuario = dbUsuario;
        senha   = dbSenha;
        url     = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
        criarPool();
    }

    public static void inicializar() {
        criarPool();
    }

    private static void criarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(senha);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("ERP-Pool");
        // Garante que todas as datas/horas usem o horário de Brasília (banco Neon usa UTC por padrão)
        config.setConnectionInitSql("SET timezone='America/Sao_Paulo'");
        dataSource = new HikariDataSource(config);
        log.info("Pool de conexões criado: {}", url);
    }

    public static Connection getConexao() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            criarPool();
        }
        return dataSource.getConnection();
    }

    public static boolean testarConexao(String dbHost, int dbPorta, String dbNome,
                                         String dbUsuario, String dbSenha) {
        String testUrl = "jdbc:postgresql://" + dbHost + ":" + dbPorta + "/" + dbNome;
        try {
            Class.forName("org.postgresql.Driver");
            try (var conn = java.sql.DriverManager.getConnection(testUrl, dbUsuario, dbSenha)) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.warn("Teste de conexão falhou: {}", e.getMessage());
            return false;
        }
    }

    public static void fechar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public static String getHost()    { return host; }
    public static int    getPorta()   { return porta; }
    public static String getBanco()   { return banco; }
    public static String getUsuario() { return usuario; }
    public static String getSenha()   { return senha; }
    public static String getUrl()     { return url; }
}
