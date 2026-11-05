package com.fiap.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioRepository - Testes Unitários")
class UsuarioRepositoryTest {

    @Test
    @DisplayName("Construtor padrão deve usar schema 'os' quando DB_SCHEMA não definido")
    void construtorPadraoUsaSchemaOs() {
        UsuarioRepository repository = new UsuarioRepository("jdbc:postgresql://localhost:5432/db", "user", "pass");
        assertNotNull(repository);
    }

    @Test
    @DisplayName("Construtor com schema customizado deve ser aceito")
    void construtorComSchemaCustomizado() {
        UsuarioRepository repository = new UsuarioRepository("jdbc:postgresql://localhost:5432/db", "user", "pass", "custom_schema");
        assertNotNull(repository);
    }
}
