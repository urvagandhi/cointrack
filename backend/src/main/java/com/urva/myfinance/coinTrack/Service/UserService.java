package com.urva.myfinance.coinTrack.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.urva.myfinance.coinTrack.DTO.AuthResponse;
import com.urva.myfinance.coinTrack.Model.User;
import com.urva.myfinance.coinTrack.Repository.UserRepository;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;
import com.urva.myfinance.coinTrack.UnauthorizedException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final AuthService authService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authManager, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.authService = authService;
    }

    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching all users: " + e.getMessage(), e);
        }
    }

    public User getUserById(String id) {
        try {
            Optional<User> user = userRepository.findById(id);
            if (user.isPresent()) {
                return user.get();
            }
            throw new ResourceNotFoundException("User", id);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user by id: " + id + ". " + e.getMessage(), e);
        }
    }

    public User updateUser(String id, User user) {
        try {
            Optional<User> existingUserOpt = userRepository.findById(id);
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();

                // Update only the fields that should be updated, preserve system fields
                if (user.getUsername() != null) {
                    existingUser.setUsername(user.getUsername());
                }
                if (user.getName() != null) {
                    existingUser.setName(user.getName());
                }
                if (user.getDateOfBirth() != null) {
                    existingUser.setDateOfBirth(user.getDateOfBirth());
                }
                if (user.getEmail() != null) {
                    existingUser.setEmail(user.getEmail());
                }
                if (user.getPhoneNumber() != null) {
                    existingUser.setPhoneNumber(user.getPhoneNumber());
                }
                if (user.getPassword() != null) {
                    existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                }

                return userRepository.save(existingUser);
            }
            throw new ResourceNotFoundException("User", id);
        } catch (Exception e) {
            throw new RuntimeException("Error updating user with id: " + id + ". " + e.getMessage(), e);
        }
    }

    public void deleteUser(String id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
            } else {
                throw new ResourceNotFoundException("User", id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user with id: " + id + ". " + e.getMessage(), e);
        }
    }

    public User registerUser(User user) {
        try {
            if (user.getUsername() == null || user.getPassword() == null) {
                throw new IllegalArgumentException("Username and password are required");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Error registering user: " + e.getMessage(), e);
        }
    }

    public AuthResponse verifyUser(User user) {
        try {
            // Find user by username or email
            User foundUser = userRepository.findByUsername(user.getUsername());
            if (foundUser == null) {
                foundUser = userRepository.findByEmail(user.getUsername()); // username field might contain email
            }
            
            if (foundUser == null) {
                throw new UnauthorizedException("Invalid username or password");
            }

            // Use the actual username for authentication
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(foundUser.getUsername(), user.getPassword()));
            if (authentication.isAuthenticated()) {
                String token = authService.generateToken(authentication);
                return new AuthResponse(token, foundUser);
            } else {
                throw new UnauthorizedException("Invalid username or password");
            }
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    public User getUserByToken(String token) {
        try {
            String username = authService.extractUsername(token);
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            throw new RuntimeException("Error extracting user from token: " + e.getMessage(), e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            String username = authService.extractUsername(token);
            User user = userRepository.findByUsername(username);
            if (user != null) {
                // Create UserDetails-like object for validation
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                    org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities("USER")
                        .build();
                return authService.validateToken(token, userDetails);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
