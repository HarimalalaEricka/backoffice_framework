package com.app.controllers;

import com.framework.annotation.*;
import com.framework.model.ModelView;
import com.framework.model.SessionModelView;
import com.app.models.Token;
import com.app.repository.TokenRepository;
import com.app.service.TokenService;
import java.time.LocalDateTime;

/**
 * Contrôleur pour la génération de tokens.
 */
@Controller
public class TokenController {

    /**
     * Afficher le formulaire de génération de token.
     * Route: GET /token/generate
     */
    @HandleGet("/token/generate")
    public ModelView generateForm() {
        ModelView mv = new ModelView();
        mv.setView("/token_generate.jsp");
        return mv;
    }

    /**
     * Traiter la génération de token.
     * Route: POST /token/generate
     */
    @HandlePost("/token/generate")
    public SessionModelView handleGenerate(@RequestParam("duree") int duree, @RequestParam("unite") String unite) {
        SessionModelView mv = new SessionModelView("/token_generate.jsp");
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres";

        try {
            // Validation de la durée
            if (duree <= 0) {
                throw new IllegalArgumentException("La durée doit être supérieure à 0");
            }

            // Initialiser les services
            TokenService tokenService = new TokenService();
            TokenRepository tokenRepo = new TokenRepository(url, username, password);

            // Génération UUID
            String tokenValue = tokenService.generateUUID();

            // Calcul date_heure_expiration
            LocalDateTime dateExpiration = tokenService.calculateExpiration(duree, unite);

            // Créer le token
            Token token = new Token(0, tokenValue, dateExpiration);

            // Insertion en base
            tokenRepo.insertToken(token);

            // Stocker le token en session (session personnalisée)
            mv.addSessionAttribute("token", tokenValue);

            // Retourner le token généré
            mv.addAttribute("success", true);
            mv.addAttribute("token", tokenValue);
            mv.addAttribute("expiration", dateExpiration);
            mv.addAttribute("duree", duree);

        } catch (IllegalArgumentException e) {
            mv.addAttribute("error", "Erreur de validation : " + e.getMessage());
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la génération du token : " + e.getMessage());
        }

        mv.setView("/token_generate.jsp");
        return mv;
    }
}
