package com.fiap.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentoValidator - Testes Unitários")
class DocumentoValidatorTest {

    @Test
    @DisplayName("Deve validar CPF com pontuação (formato brasileiro)")
    void deveValidarCpfComPontuacao() {
        assertTrue(DocumentoValidator.isCpfValido("529.982.247-25"));
    }

    @Test
    @DisplayName("Deve validar CPF sem pontuação (somente dígitos)")
    void deveValidarCpfSemPontuacao() {
        assertTrue(DocumentoValidator.isCpfValido("52998224725"));
    }

    @Test
    @DisplayName("Deve validar CPF do João Oliveira (seed)")
    void deveValidarCpfJoaoOliveira() {
        assertTrue(DocumentoValidator.isCpfValido("111.444.777-35"));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com todos os dígitos iguais (111.111.111-11)")
    void deveRejeitarCpfComDigitosIguais() {
        assertFalse(DocumentoValidator.isCpfValido("111.111.111-11"));
        assertFalse(DocumentoValidator.isCpfValido("000.000.000-00"));
        assertFalse(DocumentoValidator.isCpfValido("99999999999"));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com dígito verificador incorreto")
    void deveRejeitarCpfComDigitoVerificadorIncorreto() {
        assertFalse(DocumentoValidator.isCpfValido("529.982.247-99"));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com menos de 11 dígitos")
    void deveRejeitarCpfCurto() {
        assertFalse(DocumentoValidator.isCpfValido("1234567890")); // 10 dígitos
    }

    @Test
    @DisplayName("Deve rejeitar CPF com mais de 11 dígitos")
    void deveRejeitarCpfLongo() {
        assertFalse(DocumentoValidator.isCpfValido("529982247250")); // 12 dígitos
    }

    @Test
    @DisplayName("Deve rejeitar CPF nulo")
    void deveRejeitarCpfNulo() {
        assertFalse(DocumentoValidator.isCpfValido(null));
    }

    @Test
    @DisplayName("Deve rejeitar string vazia")
    void deveRejeitarStringVazia() {
        assertFalse(DocumentoValidator.isCpfValido(""));
    }

    @Test
    @DisplayName("Deve rejeitar texto não numérico")
    void deveRejeitarTextoNaoNumerico() {
        assertFalse(DocumentoValidator.isCpfValido("abc.def.ghi-jk"));
    }

    @Test
    @DisplayName("Deve normalizar CPF removendo pontuação")
    void deveNormalizarCpfRemovendoPontuacao() {
        assertEquals("52998224725", DocumentoValidator.normalizarDocumento("529.982.247-25"));
    }

    @Test
    @DisplayName("Deve normalizar CPF removendo espaços")
    void deveNormalizarCpfRemovendoEspacos() {
        assertEquals("52998224725", DocumentoValidator.normalizarDocumento("529 982 247 25"));
    }

    @Test
    @DisplayName("Deve retornar null quando documento for null")
    void deveRetornarNullQuandoDocumentoForNull() {
        assertNull(DocumentoValidator.normalizarDocumento(null));
    }
}
