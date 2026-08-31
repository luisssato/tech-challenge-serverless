package com.fiap.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Consulta dados de autenticação do usuário no banco PostgreSQL via JDBC. */
public class UsuarioRepository {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    /** Construtor de produção */
    public UsuarioRepository() {
        this.dbUrl = System.getenv("DB_URL");
        this.dbUser = System.getenv("DB_USER");
        this.dbPassword = System.getenv("DB_PASSWORD");
    }

    /** Construtor testável */
    public UsuarioRepository(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    /** Retorna a role do usuário pelo login (CPF normalizado) */
    public String buscarRolePorLogin(String login) {
        String query = "SELECT role FROM oficina.usuario WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, login);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("role");
                }
                return null;
            }

        } catch (SQLException e) {
            System.err.println("[UsuarioRepository] Erro ao consultar banco: " + e.getMessage());
            throw new RuntimeException("Falha ao consultar a base de dados.", e);
        }
    }
}
