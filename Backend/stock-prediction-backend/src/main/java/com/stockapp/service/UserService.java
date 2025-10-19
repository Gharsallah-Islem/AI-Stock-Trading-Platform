package com.stockapp.service;

import com.stockapp.dto.UserDTO;
import com.stockapp.exception.ResourceNotFoundException;
import com.stockapp.model.User;
import com.stockapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDTO registerUser(UserDTO userDTO, String password) {
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent() ||
                userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRiskTolerance(User.RiskTolerance.valueOf(userDTO.getRiskTolerance().name()));
        user.setInvestmentHorizon(User.InvestmentHorizon.valueOf(userDTO.getInvestmentHorizon().name()));

        user = userRepository.save(user);

        return mapToDTO(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToDTO(user);
    }

    public Long getUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return user.getId();
    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRiskTolerance(User.RiskTolerance.valueOf(user.getRiskTolerance().name()));
        dto.setInvestmentHorizon(User.InvestmentHorizon.valueOf(user.getInvestmentHorizon().name()));
        return dto;
    }
}