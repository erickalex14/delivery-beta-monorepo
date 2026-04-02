package com.deliveryapp.identityservice.services.impl;

import com.deliveryapp.identityservice.events.EmailOtpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    // 🛡️ Nos aseguramos de usar StringRedisTemplate
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String OTP_PREFIX = "otp:email:";
    private static final long OTP_VALIDITY_MINUTES = 5;

    public void generateAndSendOtp(String email) {
        // 1. Generar código aleatorio de 6 dígitos
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Guardarlo en Redis con Time-To-Live (TTL)
        redisTemplate.opsForValue().set(
                OTP_PREFIX + email,
                otp,
                OTP_VALIDITY_MINUTES,
                TimeUnit.MINUTES
        );

        // 3. Imprimir en consola (Para el MVP)
        System.out.println("🔐 [MOCK EMAIL] Enviando OTP a " + email + ": " + otp);

        // 4. Enviar evento a RabbitMQ para el Notification Service
        rabbitTemplate.convertAndSend("notification.exchange", "email.send_otp",
                new EmailOtpEvent(email, otp));
    }

    public boolean validateOtp(String email, String inputOtp) {
        // Capturamos como Object genérico primero
        Object rawValue = redisTemplate.opsForValue().get(OTP_PREFIX + email);

        // Lo convertimos a String de forma segura solo si no es nulo
        String storedOtp = rawValue != null ? rawValue.toString() : null;

        // Validamos si existe y coincide
        if (storedOtp != null && storedOtp.equals(inputOtp)) {
            // Eliminarlo inmediatamente (One-Time Password real)
            redisTemplate.delete(OTP_PREFIX + email);
            return true;
        }

        return false;
    }
}