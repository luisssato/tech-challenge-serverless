package com.fiap.auth;

public class DocumentoValidator {

    public static String normalizarDocumento(String documento) {
        if (documento == null) return null;
        return documento.replaceAll("[.\\-/\\s]", "").toUpperCase();
    }

    public static boolean isCpfValido(String cpf) {
        if (cpf == null) return false;
        
        String digitos = normalizarDocumento(cpf);
        if (digitos.length() != 11) return false;
        if (digitos.chars().distinct().count() == 1) return false; // Verifica se sao todos iguais (ex: 11111111111)

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += (digitos.charAt(i) - '0') * (10 - i);
            }
            int primeiro = 11 - (soma % 11);
            if (primeiro >= 10) primeiro = 0;
            if (primeiro != (digitos.charAt(9) - '0')) return false;

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += (digitos.charAt(i) - '0') * (11 - i);
            }
            int segundo = 11 - (soma % 11);
            if (segundo >= 10) segundo = 0;
            return segundo == (digitos.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }
}
