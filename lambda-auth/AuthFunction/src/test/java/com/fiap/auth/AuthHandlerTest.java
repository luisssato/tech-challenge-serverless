package com.fiap.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthHandler - Testes Unitários")
class AuthHandlerTest {

    // CPF real do seed: Maria Silva (52998224725)
    private static final String CPF_VALIDO_SEED = "529.982.247-25";
    private static final String CPF_VALIDO_NORMALIZADO = "52998224725";
    private static final String CPF_INVALIDO = "111.111.111-11";
    private static final String JWT_SECRET_TEST = "chave-secreta-para-testes-unitarios-com-32chars";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger lambdaLogger;

    private AuthHandler authHandler;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(JWT_SECRET_TEST, 3600);
        authHandler = new AuthHandler(new ObjectMapper(), usuarioRepository, jwtUtil);

        when(context.getLogger()).thenReturn(lambdaLogger);
        when(context.getAwsRequestId()).thenReturn("test-request-id");
        when(context.getFunctionName()).thenReturn("AuthFunction-test");
        doNothing().when(lambdaLogger).log(anyString());
    }

    @Test
    @DisplayName("Deve retornar 200 com token JWT quando CPF válido e cliente cadastrado")
    void deveRetornar200ComTokenQuandoCpfValidoEClienteCadastrado() {
        // Arrange
        when(usuarioRepository.buscarRolePorLogin(CPF_VALIDO_NORMALIZADO)).thenReturn("CLIENTE");
        APIGatewayProxyRequestEvent request = requestComBody("{\"cpf\": \"" + CPF_VALIDO_SEED + "\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("token"), "Resposta deve conter o campo 'token'");
        assertTrue(response.getBody().contains("CLIENTE"), "Resposta deve conter a role");
        assertTrue(response.getBody().contains("expiresIn"), "Resposta deve conter 'expiresIn'");
        verify(usuarioRepository).buscarRolePorLogin(CPF_VALIDO_NORMALIZADO);
    }

    @Test
    @DisplayName("Deve aceitar CPF sem formatação (somente dígitos)")
    void deveAceitarCpfSemFormatacao() {
        // Arrange
        when(usuarioRepository.buscarRolePorLogin(CPF_VALIDO_NORMALIZADO)).thenReturn("CLIENTE");
        APIGatewayProxyRequestEvent request = requestComBody("{\"cpf\": \"" + CPF_VALIDO_NORMALIZADO + "\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar 400 quando body for nulo")
    void deveRetornar400QuandoBodyNulo() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(null);

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("message"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Deve retornar 400 quando body for JSON inválido")
    void deveRetornar400QuandoBodyForJsonInvalido() {
        // Arrange
        APIGatewayProxyRequestEvent request = requestComBody("nao-e-json");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Deve retornar 400 quando campo 'cpf' estiver ausente no body")
    void deveRetornar400QuandoCpfAusente() {
        // Arrange
        APIGatewayProxyRequestEvent request = requestComBody("{\"outro_campo\": \"valor\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Deve retornar 400 quando CPF tem formato inválido (todos dígitos iguais)")
    void deveRetornar400QuandoCpfComDigitosIguais() {
        // Arrange — CPF com todos dígitos iguais, passa na contagem mas falha no
        // algoritmo
        APIGatewayProxyRequestEvent request = requestComBody("{\"cpf\": \"111.111.111-11\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("invalido"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Deve retornar 400 quando CPF tem dígito verificador incorreto")
    void deveRetornar400QuandoCpfComDigitoVerificadorIncorreto() {
        // Arrange — CPF com 11 dígitos mas DV errado
        APIGatewayProxyRequestEvent request = requestComBody("{\"cpf\": \"529.982.247-99\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Deve retornar 401 quando CPF válido mas cliente não cadastrado no banco")
    void deveRetornar401QuandoClienteNaoCadastrado() {
        // Arrange — CPF válido do João Oliveira, mas não existe na tabela usuario
        when(usuarioRepository.buscarRolePorLogin(anyString())).thenReturn(null);
        APIGatewayProxyRequestEvent request = requestComBody("{\"cpf\": \"111.444.777-35\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("message"));
    }

    @Test
    @DisplayName("Deve retornar 500 quando banco de dados lançar exceção")
    void deveRetornar500QuandoBancoLancarExcecao() {
        // Arrange
        when(usuarioRepository.buscarRolePorLogin(anyString()))
                .thenThrow(new RuntimeException("Conexao recusada pelo banco"));
        APIGatewayProxyRequestEvent request = requestComBody("{\"cpf\": \"" + CPF_VALIDO_SEED + "\"}");

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertEquals(500, response.getStatusCode());
        assertFalse(response.getBody().contains("Conexao recusada"), "Detalhe interno não deve vazar para o cliente");
    }

    @Test
    @DisplayName("Todas as respostas devem ter Content-Type application/json")
    void todasAsRespostasDevemTerContentTypeJson() {
        // Arrange — caso mais simples (body nulo)
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(null);

        // Act
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, context);

        // Assert
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    private APIGatewayProxyRequestEvent requestComBody(String body) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(body);
        return request;
    }
}
