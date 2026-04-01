package com.deliveryapp.identityservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraemos el header "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // Si no hay token o no empieza con "Bearer ", lo dejamos pasar al siguiente filtro
        // (Spring Security lo bloqueará más adelante si la ruta era protegida).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraemos el JWT limpio
        jwt = authHeader.substring(7);

        try {
            // 3. Verificamos si el token está en la lista negra de Redis (Logout)
            if (jwtBlacklistService.isBlacklisted(jwt)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token revocado (Sesion cerrada)");
                return;
            }

            // 4. Extraemos el ID del usuario desde el Token
            userId = jwtService.extractUserId(jwt);
            String role = jwtService.extractRole(jwt);

            // 5. Si hay ID y el usuario aún no está autenticado en este hilo de Spring...
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Creamos el objeto de autenticación con el Rol
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Inyectamos al usuario en el contexto de seguridad de Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Si el token expiró o la firma es falsa, atrapamos el error y devolvemos 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalido o expirado");
            return;
        }

        // 6. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}