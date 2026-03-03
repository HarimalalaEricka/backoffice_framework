package com.app.filter;

import com.app.models.Token;
import com.app.repository.TokenRepository;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Filtre HTTP pour vérifier les tokens sur les API protégées
 */
public class TokenFilter implements Filter {

    private TokenRepository tokenRepo;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("=== Initialisation du TokenFilter ===");
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres";
        this.tokenRepo = new TokenRepository(url, username, password);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestURI = request.getRequestURI();
        
        // Vérifier si c'est une route API protégée
        if (requestURI.contains("/api/")) {
            
            // Récupérer le header Authorization
            String authHeader = request.getHeader("Authorization");
            
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();

            // Vérifier la présence du header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{");
                out.println("  \"status\": \"error\",");
                out.println("  \"message\": \"Token manquant. Header Authorization: Bearer <token> requis\"");
                out.println("}");
                return;
            }

            // Extraire le token
            String tokenValue = authHeader.substring(7); // Enlever "Bearer "

            // Vérifier l'existence du token en base
            Token token = tokenRepo.findByTokenValue(tokenValue);
            
            if (token == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{");
                out.println("  \"status\": \"error\",");
                out.println("  \"message\": \"Token invalide\"");
                out.println("}");
                return;
            }

            // Vérifier l'expiration
            if (tokenRepo.isTokenExpired(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{");
                out.println("  \"status\": \"error\",");
                out.println("  \"message\": \"Token expiré\"");
                out.println("}");
                return;
            }

            // Token valide, continuer la chaîne
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // Route non protégée, continuer
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
        System.out.println("TokenFilter détruit");
    }
}
