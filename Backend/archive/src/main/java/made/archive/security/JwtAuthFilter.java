package made.archive.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.logging.Logger;


@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter
{

    private static final Logger logger = Logger.getLogger(JwtAuthFilter.class.getName());

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException
    {
        // Vérifier d'abord l'en-tête Authorization
        String jwt = null;
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(JwtService.PREFIX)) 
        {
            jwt = authHeader.substring(JwtService.PREFIX.length()).trim();
        } 
        else 
        {
            // Vérifier si le token est fourni en paramètre de requête
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isEmpty()) 
            {
                // Vérifier si le token contient déjà le préfixe Bearer
                if (tokenParam.startsWith(JwtService.PREFIX)) 
                {
                    jwt = tokenParam.substring(JwtService.PREFIX.length()).trim();
                } 
                else 
                {
                    jwt = tokenParam.trim();
                }
                logger.info("Token JWT trouvé dans les paramètres de requête");
            }
        }
        
        // Si aucun token n'est trouvé, continuer la chaîne de filtres
        if (jwt == null) 
        {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwt.isEmpty())
        {
            logger.warning("Token JWT est vide");
            filterChain.doFilter(request, response);
            return;
        }

        try
        {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null)
            {
                var userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userEmail))
                {
                    var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Authentification réussie pour : " + userEmail);
                }
                else
                {
                    logger.warning("Token JWT invalide pour : " + userEmail);
                }
            }
        }
        catch (Exception e)
        {
            logger.severe("Erreur JWT : " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
