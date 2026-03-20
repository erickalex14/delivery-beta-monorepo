package com.deliveryapp.coretransactional.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // Le dice a Spring: "Soy el Abogado Global de toda la App"
public class GlobalExceptionHandler {

    // 1. Atrapa nuestros errores manuales del Service (ej. "Billetera no encontrada")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeExceptions(RuntimeException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage()); // Metemos el mensaje en un JSON bonito
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Atrapa los errores de los DTOs (El Cadenero: @NotNull, @Min)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Extrae todos los errores de validación y los arma en un JSON dinámico
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}