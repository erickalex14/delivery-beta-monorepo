package com.deliveryapp.identityservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // 👈 Esto convierte la clase en el "Escudo" de todos los controladores
public class GlobalExceptionHandler {

    // Atrapa cualquier RuntimeException que lances en tus Services (Ej: "Credenciales inválidas")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage()); // Aquí sale tu mensaje personalizado

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // (Opcional) Más adelante puedes agregar aquí @ExceptionHandler(MethodArgumentNotValidException.class)
    // para los errores de validación de los DTOs (ej: email mal escrito).
}