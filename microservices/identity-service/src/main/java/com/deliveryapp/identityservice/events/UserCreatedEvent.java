package com.deliveryapp.identityservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreatedEvent {
    private UUID userId;
    private String phone;
    private String email;
    private String role;
}