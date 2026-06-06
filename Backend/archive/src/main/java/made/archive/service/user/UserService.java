package made.archive.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import made.archive.entite.User;
import made.archive.exception.BusinessException;
import made.archive.entite.Role;
import made.archive.repository.UserRepository;
import made.archive.repository.RoleRepository;
import made.archive.dto.UserDto;
import made.archive.dto.UserResponseDto;

@Service
public class UserService
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    private UserResponseDto convertUserToDto(User user) 
    {
        return new UserResponseDto(
            user.getId(),
            user.getNom(),
            user.getPrenom(),
            user.getEmail(),
            user.getTelephone(),
            user.isActif(),
            user.getRoles()
        );
    }

    private UserDto convertToDtoWithoutPassword(User entity) 
    {
        UserDto dto = new UserDto();
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setRoles(entity.getRoles()); 
        dto.setPassword(null); 
        return dto;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() 
    {
        List<User> users = userRepository.findAll();
        return users.stream()
            .map(this::convertUserToDto)
            .toList();
    }

    @Transactional
    public List<UserResponseDto> getUsersyRole (Role roles)
    {
        if (roles == null) 
        {
            throw new BusinessException("Le rôle est obligatoire");
        }
        
        List<User> users = userRepository.findByRoles(roles);
        return users.stream()
            .map(this::convertUserToDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(boolean actif) 
    {
        try
        {
            return userRepository.findByActif(actif);
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la récupération des utilisateurs par statut: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> getUserById(UUID id) 
    {
        if (id == null) 
        {
            throw new BusinessException("L'ID est obligatoire");
        }
        return userRepository.findById(id)
                .map(this::convertUserToDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponseDto> getMe(UUID userId) 
    {
        try
        {
            if (userId == null) 
            {
                throw new BusinessException("L'ID est obligatoire");
            }
            return userRepository.findById(userId)
                    .map(this::convertUserToDto);
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponseDto> getUserByEmail(String email) 
    {
        try
        {
            if (!StringUtils.hasText(email)) 
            {
                throw new BusinessException("L'email est obligatoire");
            }
            return userRepository.findByEmail(email)
            .map(this::convertUserToDto);
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
    }

    @Transactional
    public User updateUserStatus(UUID id, UserDto dto) 
    {
        try
        {
            if (dto == null || id == null) 
            {
                throw new BusinessException("Les données de mise à jour sont invalides");
            }
            
            Optional<User> userOpt = userRepository.findById(id);
            User user = userOpt.orElseThrow(() -> 
                new BusinessException("Utilisateur non trouvé avec l'ID: " + id));
                
            user.setActif(dto.isActif());
            return userRepository.save(user);
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la mise à jour du statut de l'utilisateur: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) 
    {
        try
        {
            if (userId == null) 
            {
                throw new BusinessException("L'ID est obligatoire");
            }
            return userRepository.findById(userId)
                .map(User::isActif)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé avec l'ID: " + userId));
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la vérification du statut de l'utilisateur: " + e.getMessage());
        }
    }

    @Transactional
    public UserDto createUser(UserDto dto) 
    {   
        try
        {
            if (userRepository.findByEmail(dto.getEmail()).isPresent()) 
            {
                throw new BusinessException("Cet email est déjà utilisé");
            }

            if (dto.getPassword() == null || dto.getPassword().isBlank()) 
            {
                throw new BusinessException("Le mot de passe est obligatoire");
            }
            
            User user = new User();
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setTelephone(dto.getTelephone());
            Set<Role> rolesAttribues = new HashSet<>();
            for (Role roleDto : dto.getRoles()) 
            {
                Role roleExistant = roleRepository.findByName(roleDto.getName())
                    .orElseThrow(() -> new RuntimeException("Erreur : Le rôle " + roleDto.getName() + " n'existe pas en base de données."));
                rolesAttribues.add(roleExistant);
            }
            user.setRoles(rolesAttribues);

            userRepository.save(user);
            dto.setPassword(null);
            return dto;
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la création de l'utilisateur: " + e.getMessage());
        }
    }

@Transactional
public Optional<UserDto> updateUser(UUID id, UserDto dto)
{
    try
    {
        if (dto == null || id == null)
            throw new BusinessException("Les données de mise à jour sont invalides");

        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Utilisateur non trouvé avec l'ID: " + id));

        // Vérification doublon email uniquement si l'email change
        if (!user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
                throw new BusinessException("Cet email est déjà utilisé par un autre compte");
            });
            user.setEmail(dto.getEmail());
        }

        user.setNom(dto.getNom());
        user.setPrenom(dto.getPrenom());
        user.setTelephone(dto.getTelephone());

        // Mot de passe uniquement si fourni
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        Set<Role> rolesAttribues = new HashSet<>();
        for (Role roleDto : dto.getRoles()) {
            Role roleExistant = roleRepository.findByName(roleDto.getName())
                .orElseThrow(() -> new RuntimeException("Rôle introuvable : " + roleDto.getName()));
            rolesAttribues.add(roleExistant);
        }
        user.setRoles(rolesAttribues);

        userRepository.save(user);
        dto.setPassword(null);
        return Optional.of(dto);
    }
    catch(Exception e)
    {
        throw new BusinessException("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
    }
}

    @Transactional
    public Optional<UserDto> updateMe(UUID userId, UserDto dto) 
    {
        try
        {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("L'utilisateur est introuvable"));
    
        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setTelephone(dto.getTelephone());
    
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) 
        {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        dto.setPassword(null);
        User upateUser = userRepository.save(user);
        return Optional.of(convertToDtoWithoutPassword(upateUser));
        }
        catch(Exception e)
        {
            throw new BusinessException("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
        }
    }
}