package com.stockapp.dto;

import com.stockapp.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private User.RiskTolerance riskTolerance;
    private User.InvestmentHorizon investmentHorizon;
}