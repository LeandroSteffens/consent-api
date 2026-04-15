package com.sensedia.consent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT adaptado para o consent-service.
 *
 * DIFERENÇA CHAVE em relação ao auth-service:
 * - NÃO usa UserDetailsService (não tem acesso ao banco de usuários)
 * - Extrai username e roles DIRETAMENTE dos claims do JWT
 * - Cria o Authentication token a partir dos dados do JWT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // 1. Verifica blacklist no Redis
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                log.debug("Token presente na blacklist, rejeitando");
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Valida assinatura e expiração do JWT
            if (!jwtService.isTokenValid(jwt)) {
                log.debug("Token JWT inválido ou expirado");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Extrai username e roles DIRETO do token (sem consultar banco de usuários)
            final String username = jwtService.extractUsername(jwt);
            final List<String> roles = jwtService.extractRoles(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 4. Cria authorities a partir das roles presentes no JWT
                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                        : List.of();

                // 5. Cria Authentication SEM consultar o banco de usuários
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Usuário '{}' autenticado via JWT com roles: {}", username, roles);
            }
        } catch (Exception e) {
            log.debug("Falha ao processar token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
