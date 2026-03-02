package com.exemplo.sicob.startup;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class DatabaseInit {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInit.class.getName());

    @PostConstruct
    public void init() {
        System.out.println("DatabaseInit: starting DB initialization");
        try {
            InitialContext ic = new InitialContext();
            DataSource ds = (DataSource) ic.lookup("jdbc/sicob");
            try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
                // Sintaxe H2 compatível: ID BIGINT AUTO_INCREMENT (ou IDENTITY), VARCHAR com tamanho, TIMESTAMP
                String ddl = "CREATE TABLE IF NOT EXISTS pedido (" +
                        "ID BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "CLIENTE VARCHAR(255) NOT NULL, " +
                        "criado_em TIMESTAMP, " +
                        "PRODUTO VARCHAR(255) NOT NULL, " +
                        "QUANTIDADE INTEGER NOT NULL, " +
                        "STATUS VARCHAR(50) NOT NULL" +
                        ")";
                LOGGER.info("DatabaseInit: executing DDL -> " + ddl);
                System.out.println("DatabaseInit: executing DDL -> " + ddl);
                s.executeUpdate(ddl);
                
                // Inserir dados iniciais se a tabela estiver vazia
                String checkTable = "SELECT COUNT(*) FROM pedido";
                java.sql.ResultSet rs = s.executeQuery(checkTable);
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("DatabaseInit: table is empty, inserting initial data...");
                    String insert = "INSERT INTO pedido (CLIENTE, PRODUTO, QUANTIDADE, STATUS, criado_em) " +
                                   "VALUES ('Cliente Exemplo', 'Produto Demo', 5, 'PENDENTE', CURRENT_TIMESTAMP())";
                    s.executeUpdate(insert);
                    System.out.println("DatabaseInit: initial data inserted successfully");
                }
                
                LOGGER.info("DatabaseInit: DDL executed successfully (table created or already existed)");
                System.out.println("DatabaseInit: DDL executed successfully (table created or already existed)");
            }
        } catch (NamingException | SQLException e) {
            LOGGER.log(Level.SEVERE, "DatabaseInit: failed to initialize database", e);
            System.err.println("DatabaseInit: failed to initialize database: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
