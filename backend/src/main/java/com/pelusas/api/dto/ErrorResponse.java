package com.pelusas.api.dto;

public record ErrorResponse(int estado, String codigo, String mensaje) {
}
