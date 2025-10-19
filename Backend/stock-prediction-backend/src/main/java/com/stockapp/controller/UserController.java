package com.stockapp.controller;

import com.stockapp.model.User;
import com.stockapp.repository.UserRepository;
import com.stockapp.security.JwtUtil;
import com.stockapp.security.UserDetailsServiceImpl;
import com.stockapp.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserRepository userRepository, AuthenticationManager authenticationManager,
            UserDetailsServiceImpl userDetailsService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());

        // Validate input
        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            logger.warn("Login failed - Email is empty");
            return ResponseEntity.badRequest().body("Email cannot be empty");
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            logger.warn("Login failed - Password is empty");
            return ResponseEntity.badRequest().body("Password cannot be empty");
        }

        try {
            // Find user by email first
            String trimmedEmail = loginRequest.getEmail().trim();
            User user = userRepository.findByEmail(trimmedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Use username for authentication (since Spring Security expects username)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(),
                            loginRequest.getPassword()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String jwt = jwtUtil.generateToken(userDetails.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("username", userDetails.getUsername());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("message", "Login successful");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("Login successful for email: {} (username: {})", trimmedEmail, user.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for email: {} - Error: {}", loginRequest.getEmail(), e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid credentials");
            errorResponse.put("message", "Email or password is incorrect");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        logger.info("Test endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running!");
        response.put("timestamp", System.currentTimeMillis());
        response.put("port", 8083);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Stock Prediction Backend");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        logger.info("Registration attempt for email: {}", registerRequest.getEmail());

        // Validate input
        if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
            logger.warn("Registration failed - Email is empty");
            return ResponseEntity.badRequest().body("Email cannot be empty");
        }

        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            logger.warn("Registration failed - Password is empty");
            return ResponseEntity.badRequest().body("Password cannot be empty");
        }

        if (registerRequest.getFirstName() == null || registerRequest.getFirstName().trim().isEmpty()) {
            logger.warn("Registration failed - First name is empty");
            return ResponseEntity.badRequest().body("First name cannot be empty");
        }

        if (registerRequest.getLastName() == null || registerRequest.getLastName().trim().isEmpty()) {
            logger.warn("Registration failed - Last name is empty");
            return ResponseEntity.badRequest().body("Last name cannot be empty");
        }

        // Basic email validation
        if (!registerRequest.getEmail().contains("@")) {
            logger.warn("Registration failed - Invalid email format: {}", registerRequest.getEmail());
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        // Check password strength
        if (registerRequest.getPassword().length() < 6) {
            logger.warn("Registration failed - Password too short");
            return ResponseEntity.badRequest().body("Password must be at least 6 characters long");
        }

        String trimmedEmail = registerRequest.getEmail().trim();
        String trimmedFirstName = registerRequest.getFirstName().trim();
        String trimmedLastName = registerRequest.getLastName().trim();

        // Generate username from email (part before @) if not provided
        String username = registerRequest.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = trimmedEmail.substring(0, trimmedEmail.indexOf("@"));
            // Make username unique if it already exists
            String originalUsername = username;
            int counter = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = originalUsername + counter;
                counter++;
            }
        } else {
            username = username.trim();
        }

        if (userRepository.findByUsername(username).isPresent()) {
            logger.warn("Registration failed - Username already exists: {}", username);
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userRepository.findByEmail(trimmedEmail).isPresent()) {
            logger.warn("Registration failed - Email already exists: {}", trimmedEmail);
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(trimmedEmail);
        user.setFirstName(trimmedFirstName);
        user.setLastName(trimmedLastName);
        user.setRiskTolerance(registerRequest.getRiskTolerance() != null ? registerRequest.getRiskTolerance()
                : User.RiskTolerance.Medium);
        user.setInvestmentHorizon(
                registerRequest.getInvestmentHorizon() != null ? registerRequest.getInvestmentHorizon()
                        : User.InvestmentHorizon.Medium);

        userRepository.save(user);
        logger.info("Registration successful for username: {} and email: {}", username, trimmedEmail);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("user",
                new UserDTO(user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRiskTolerance(), user.getInvestmentHorizon()));
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String firstName;
        private String lastName;
        private User.RiskTolerance riskTolerance;
        private User.InvestmentHorizon investmentHorizon;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public User.RiskTolerance getRiskTolerance() {
            return riskTolerance;
        }

        public void setRiskTolerance(User.RiskTolerance riskTolerance) {
            this.riskTolerance = riskTolerance;
        }

        public User.InvestmentHorizon getInvestmentHorizon() {
            return investmentHorizon;
        }

        public void setInvestmentHorizon(User.InvestmentHorizon investmentHorizon) {
            this.investmentHorizon = investmentHorizon;
        }
    }
}