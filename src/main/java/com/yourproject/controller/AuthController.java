package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.service.AuthService;
import com.yourproject.service.UserService; // For profile updates
import jakarta.servlet.http.HttpServletRequest; // For refresh token from header or body
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // To get current user
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService; // For profile update

    @Autowired
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> registerUser(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        AuthResponseDto authResponse = authService.registerUser(registrationRequest);
        return new ResponseEntity<>(ApiResponse.success(authResponse, "User registered successfully"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> loginUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        AuthResponseDto authResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(@RequestBody Map<String, String> requestBody) {
        // Assuming refresh token is sent in the request body as {"refreshToken": "value"}
        String refreshTokenValue = requestBody.get("refreshToken");
        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            return new ResponseEntity<>(ApiResponse.error("Refresh token is required"), HttpStatus.BAD_REQUEST);
        }
        AuthResponseDto authResponse = authService.refreshToken(refreshTokenValue);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // Assuming email is the username
        UserDto userProfile = authService.getAuthenticatedUserProfile(email);
        return ResponseEntity.ok(ApiResponse.success(userProfile, "Profile fetched successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@Valid @RequestBody ProfileUpdateRequestDto profileUpdateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        // Assuming UserService will have the update logic based on authenticated user
        UserDto updatedUser = userService.updateUserProfile(email, profileUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody PasswordChangeRequestDto passwordChangeRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        authService.changePassword(email, passwordChangeRequest);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto forgotPasswordRequest) {
        authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email (if user exists)"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto resetPasswordRequest) {
        authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
    }
}
