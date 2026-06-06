package made.archive.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import made.archive.entite.Role;
import made.archive.entite.User;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import made.archive.dto.UserDto;
import made.archive.dto.UserResponseDto;
import made.archive.service.user.UserService;


@RestController
@RequestMapping("/api/admin")
public class AdminController
{
    private final UserService userService;
    
    public AdminController(UserService userService)
    {
        this.userService = userService;
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/users/create-user")
    public ResponseEntity<?> createUser(@RequestBody UserDto dto)
    {
        try
        {
            return ResponseEntity.ok(userService.createUser(dto));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la création de l'utilisateur: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() 
    {
        try 
        {
            List<UserResponseDto> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des administrateurs: " + e.getMessage());
        }
    }


    @Secured("ROLE_ADMIN")
    @GetMapping("/users/inactifs")
    public ResponseEntity<?> getUsersInactifs() 
    {
        try 
        {
            List<User> users = userService.getUsersByStatus(false);
            return ResponseEntity.ok(users);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des utilisateurs inactifs: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/users/actifs")
    public ResponseEntity<?> getUsersActifs() 
    {
        try 
        {
            List<User> users = userService.getUsersByStatus(true);
            return ResponseEntity.ok(users);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des utilisateurs actifs: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/users/{userId}/status")
    public ResponseEntity<?> checkUserStatus(@PathVariable Long userId) 
    {
        try 
        {
            Boolean isActive = userService.isUserActive(userId);
            return ResponseEntity.ok(isActive);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.badRequest().body("Erreur lors de la vérification du statut: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/users/status/{id}")
    public ResponseEntity<?> updateUserStatus(@PathVariable UUID id, @Valid @RequestBody UserDto dto) 
    {
        try 
        {
            User updatedUser = userService.updateUserStatus(id, dto);
            return ResponseEntity.ok(updatedUser);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour du statut: " + e.getMessage());
        }
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/users/update-user/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto dto)
    {
        try
        {
            Optional<UserDto> updatedUser = userService.updateUser(id, dto);
            return ResponseEntity.ok(updatedUser);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
        }
    }

    @Secured("ROLE_USER")
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable UUID id)
    {
        try
        {
            Optional<UserResponseDto> user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
    }

    @Secured("ROLE_USER")
    @GetMapping("/users/roles")
    public ResponseEntity<?> getUserByRoles(@Valid @RequestBody Role roles)
    {
        try
        {
            List<UserResponseDto> user = userService.getUsersyRole(roles);
            return ResponseEntity.ok(user);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
    }
}