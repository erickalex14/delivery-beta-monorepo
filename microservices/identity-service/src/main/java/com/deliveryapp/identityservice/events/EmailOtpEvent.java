package com.deliveryapp.identityservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailOtpEvent {
    private String email;
    private String otp;
}