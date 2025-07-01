package com.yourproject.service;

import com.yourproject.dto.AuthResponseDto;
import com.yourproject.dto.LoginRequestDto;
import com.yourproject.dto.UserRegistrationRequestDto;
import com.yourproject.dto.ForgotPasswordRequestDto;
import com.yourproject.dto.ResetPasswordRequestDto;
import com.yourproject.dto.PasswordChangeRequestDto;
import com.yourproject.dto.UserDto;
import com.yourproject.entity.User;

public interface AuthService {
    AuthResponseDto registerUser(UserRegistrationRequestDto registrationRequest);
    AuthResponseDto loginUser(LoginRequestDto loginRequest);
    AuthResponseDto refreshToken(String refreshTokenValue);
    void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequest);
    void resetPassword(ResetPasswordRequestDto resetPasswordRequest);
    void changePassword(String userEmail, PasswordChangeRequestDto passwordChangeRequest);
    UserDto getAuthenticatedUserProfile(String email);
    // UserDto updateAuthenticatedUserProfile(String email, ProfileUpdateRequestDto profileUpdateRequest); // This might go into UserService
}
