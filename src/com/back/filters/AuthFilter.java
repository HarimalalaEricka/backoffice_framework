package com.app.filters;

import com.app.models.Token;
import com.app.repository.TokenRepository;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;

@WebFilter("/api/*")   // n’intercepte que les URL commençant par /api
public class AuthFilter implements Filter {

    private TokenRepository tokenRepo;

    @Override
    public void init(FilterConfig filterConfig) {
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        // Essayer plusieurs credentials connus (fallback si nécessaire)
        String[][] creds = {
            {"postgres", "postgres"},
            {"postgres", "postgres"}
        };
        for (String[] c : creds) {
            try {
                tokenRepo = new TokenRepository(url, c[0], c[1]);
                if (tokenRepo.isConnected()) {
                    System.out.println("AuthFilter: connexion DB OK avec utilisateur=" + c[0]);
                    break;
                }
            } catch (Exception e) {
                System.err.println("AuthFilter: échec connexion avec utilisateur=" + c[0] + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                      "Token absent ou mal formé");
            return;
        }

        String tokenValue = auth.substring(7);
        if (tokenRepo == null || !tokenRepo.isConnected()) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      "Vérification du token impossible (DB non disponible)");
            return;
        }

        Token token = tokenRepo.findByToken(tokenValue);
        if (token == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                      "Token inconnu");
            return;
        }
        if (token.getDateHeureExpiration().isBefore(LocalDateTime.now())) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                      "Token expiré");
            return;
        }

        // tout est bon, on laisse passer la requête
        chain.doFilter(req, res);
    }

    private void sendError(HttpServletResponse resp, int status, String msg)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\":\"error\",\"message\":\"" + msg + "\"}");
    }

    @Override
    public void destroy() {}
}