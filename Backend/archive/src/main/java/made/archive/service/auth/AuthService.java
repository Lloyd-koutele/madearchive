package made.archive.service.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import made.archive.entite.DeviceSession;
import made.archive.entite.User;
import made.archive.entite.UserActiveToken;
import made.archive.exception.BusinessException;
import made.archive.repository.DeviceSessionRepository;
import made.archive.repository.UserActiveTokenRepository;
import made.archive.repository.UserRepository;
import made.archive.security.JwtService;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService 
{

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserActiveTokenRepository activeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceSessionRepository deviceSessionRepository;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    @Transactional
    public AuthResponse authenticate(LoginRequest request) 
    {

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) 
        {
            return AuthResponse.failed("Email ou mot de passe incorrect");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) 
        {
            return AuthResponse.failed("Email ou mot de passe incorrect");
        }

        if(!user.isActif())
        {
            return AuthResponse.failed("Accès non autorisé pour ce compte, veuillez contacter l'administrateur");
        }

        return resolveSession(user);
    }

    public AuthResponse logout() 
    {
        return AuthResponse.success("Déconnexion réussie");
    }

    @Data
    public static class LoginRequest 
    {
        private String email;
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class AuthResponse 
    {
        private boolean success;
        private String message;
        private String token;
        private String refreshToken;
        private UUID userId;
        

        public static AuthResponse success(String token, String refreshToken, UUID userId) {
            return new AuthResponse(true, "Authentification réussie", token, refreshToken, userId);
        }

        public static AuthResponse success(String message) {
            return new AuthResponse(true, message, null, null, null);
        }

        public static AuthResponse failed(String message) {
            return new AuthResponse(false, message, null, null, null);
        }

        
    }

    private AuthResponse resolveSession (User user)
    {
        Instant now = Instant.now();
        String accessToken;

        // Nettoyage des sessions expirées ou révoquées au moment de la connexion
        deviceSessionRepository.deleteExpiredOrRevokedByUser(user, now);

        Optional<UserActiveToken> existSession = activeTokenRepository.findByUser(user);

        if(existSession.isPresent() && existSession.get().getExpiresAt().isAfter(now))
        {
            accessToken = existSession.get().getAccessToken();
        }
        else
        {
            accessToken = jwtService.generateToken(user);
            UserActiveToken uat = existSession.orElse(new UserActiveToken());
            uat.setUser(user);
            uat.setAccessToken(accessToken);
            uat.setExpiresAt(now.plusMillis(jwtService.getExpirationTime()));
            activeTokenRepository.save(uat);
        }

        String refreshToken = UUID.randomUUID().toString();
        DeviceSession device = new DeviceSession();
        device.setUser(user);
        device.setRefreshToken(refreshToken);
        device.setExpiresAt(now.plusMillis(refreshExpiration));
        deviceSessionRepository.save(device);

        return AuthResponse.success(accessToken, refreshToken, user.getId());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) 
    {
        DeviceSession device = deviceSessionRepository
            .findByRefreshTokenAndRevokedFalse(refreshToken)
            .orElseThrow(() -> new BusinessException("Session invalide"));

        Instant now = Instant.now();

        if (device.getExpiresAt().isBefore(now)) {
            device.setRevoked(true);
            deviceSessionRepository.save(device);
            throw new BusinessException("Session expirée, veuillez vous reconnecter");
        }

        
        User user = device.getUser();

        // Vérifier si un autre appareil a déjà renouvelé l'access token
        Optional<UserActiveToken> existing = activeTokenRepository.findByUser(user);
        if (existing.isPresent() && existing.get().getExpiresAt().isAfter(now)) 
        {
            // Access token toujours valide → le retourner tel quel
            return AuthResponse.success(existing.get().getAccessToken(), refreshToken, user.getId());
        }

        // Générer un nouvel access token pour tous les appareils
        String newAccess = jwtService.generateToken(user);
        UserActiveToken uat = existing.orElse(new UserActiveToken());
        uat.setUser(user);
        uat.setAccessToken(newAccess);
        uat.setExpiresAt(now.plusMillis(jwtService.getExpirationTime()));
        activeTokenRepository.save(uat);

        return AuthResponse.success(newAccess, refreshToken, user.getId());
    }

    @Transactional
    public AuthResponse logout(String refreshToken) 
    {
        // 1. On cherche la session de cet appareil précis
        deviceSessionRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
            .ifPresent(device -> {
            
            // 2. Cet appareil passe à "revoked = true"
            device.setRevoked(true);
            deviceSessionRepository.save(device);

            // 3. On vérifie s'il reste d'AUTRES appareils actifs pour cet utilisateur
            List<DeviceSession> remaining = deviceSessionRepository
                .findAllByUserAndRevokedFalse(device.getUser());

            // 💡 4. Si la liste est vide (c'était le tout dernier appareil connecté)
            if (remaining.isEmpty()) 
            {
                activeTokenRepository.findByUser(device.getUser())
                    .ifPresent(activeToken -> {
                    activeToken.setExpiresAt(Instant.now());
                    activeTokenRepository.save(activeToken);});
            }
        });

        return AuthResponse.success("Déconnexion réussie");
    }
}
