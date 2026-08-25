package com.fiap.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> RESPONSE_HEADERS = new HashMap<>();

    static {
        RESPONSE_HEADERS.put("Content-Type", "application/json");
    }

    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    /** Construtor de produção */
    public AuthHandler() {
        this.objectMapper = new ObjectMapper();
        this.usuarioRepository = new UsuarioRepository();
        this.jwtUtil = new JwtUtil();
    }

    /** Construtor testável */
    public AuthHandler(ObjectMapper objectMapper, UsuarioRepository usuarioRepository, JwtUtil jwtUtil) {
        this.objectMapper = objectMapper;
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        String requestId = context.getAwsRequestId();
        log(context, "INFO", "Requisicao recebida", "requestId=" + requestId);

        try {
            String body = input.getBody();
            if (body == null || body.isBlank()) {
                log(context, "WARN", "Body ausente", "requestId=" + requestId);
                return response(400, errorBody("Body obrigatorio. Envie: { \"cpf\": \"...\" }"));
            }

            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(body);
            } catch (JsonProcessingException e) {
                log(context, "WARN", "Body invalido (nao e JSON)", "requestId=" + requestId);
                return response(400, errorBody("Body deve ser um JSON valido"));
            }

            if (!jsonNode.has("cpf") || jsonNode.get("cpf").asText().isBlank()) {
                log(context, "WARN", "Campo cpf ausente", "requestId=" + requestId);
                return response(400, errorBody("Campo 'cpf' obrigatorio no corpo da requisicao"));
            }

            String rawCpf = jsonNode.get("cpf").asText();

            if (!DocumentoValidator.isCpfValido(rawCpf)) {
                log(context, "WARN", "CPF com formato invalido", "requestId=" + requestId);
                return response(400, errorBody("CPF com formato ou digito verificador invalido"));
            }

            String cpfNormalizado = DocumentoValidator.normalizarDocumento(rawCpf);

            // Existência na tabela usuario valida o status do cliente.
            String role = usuarioRepository.buscarRolePorLogin(cpfNormalizado);

            if (role == null) {
                log(context, "INFO", "CPF nao encontrado na base", "requestId=" + requestId);
                return response(401, errorBody("Cliente nao encontrado ou nao cadastrado"));
            }

            String token = jwtUtil.generateAccessToken(cpfNormalizado, role);
            log(context, "INFO", "Token gerado com sucesso", "requestId=" + requestId + " role=" + role);

            String responseBody = objectMapper.writeValueAsString(
                    Map.of("token", token, "role", role, "expiresIn", 3600));

            return response(200, responseBody);

        } catch (Exception e) {
            log(context, "ERROR", "Erro interno", "requestId=" + requestId + " error=" + e.getMessage());
            return response(500, errorBody("Erro interno. Por favor, tente novamente."));
        }
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withHeaders(RESPONSE_HEADERS)
                .withStatusCode(statusCode)
                .withBody(body);
    }

    private String errorBody(String message) {
        return "{\"message\": \"" + message + "\"}";
    }

    private void log(Context context, String level, String event, String details) {
        context.getLogger().log(String.format(
                "{\"level\":\"%s\",\"event\":\"%s\",%s,\"function\":\"%s\"}%n",
                level, event, details, context.getFunctionName()));
    }
}
