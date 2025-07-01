package com.yourproject.service.impl;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import com.yourproject.entity.Role;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.exception.UnauthorizedException;
import com.yourproject.repository.UserRepository;
import com.yourproject.service.AuthService;
import com.yourproject.service.EmailService; // Assuming an EmailService will be created
import com.yourproject.util.JwtUtil;
import org.modelmapper.ModelMapper; // Or manual mapping
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID; // For simple token generation for password reset

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper; // For DTO-entity mapping
    private final EmailService emailService; // For sending emails

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           ModelMapper modelMapper,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public AuthResponseDto registerUser(UserRegistrationRequestDto registrationRequest) {
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BadRequestException("Email address already in use.");
        }

        User user = modelMapper.map(registrationRequest, User.class);
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setActive(true); // Default to active
        user.setEmailVerified(false); // Requires verification step if implemented

        // ID Generation (studentId/employeeId) - This logic was in Mongoose model
        // This should be a more robust and centralized generation strategy.
        if (user.getRole() == Role.STUDENT) {
            // Example: "STU" + year + random number
            user.setStudentId("STU" + LocalDateTime.now().getYear() + (1000 + (int)(Math.random() * 9000)));
        } else if (user.getRole() == Role.ADMIN || user.getRole() == Role.WARDEN) {
            user.setEmployeeId("EMP" + (1000 + (int)(Math.random() * 9000)));
            user.setJoinDate(java.time.LocalDate.now());
        }

        User savedUser = userRepository.save(user);

        // Send welcome email (simplified, actual template usage in EmailService)
        // emailService.sendWelcomeEmail(savedUser);

        String accessToken = jwtUtil.generateAccessToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getEmail(), savedUser.getId());

        UserDto userDto = modelMapper.map(savedUser, UserDto.class);
        // Populate derived fields in DTO if not handled by ModelMapper directly
        userDto.setFullName(savedUser.getFirstName() + " " + savedUser.getLastName());


        return new AuthResponseDto(accessToken, refreshToken, userDto);
    }

    @Override
    @Transactional
    public AuthResponseDto loginUser(LoginRequestDto loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated. Please contact administrator.");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setFullName(user.getFirstName() + " " + user.getLastName());
        // Potentially map currentRoom for UserDto
        user.getCurrentOccupancy().ifPresent(occupancy -> {
            RoomSlimDto roomDto = modelMapper.map(occupancy.getRoom(), RoomSlimDto.class);
            userDto.setCurrentRoom(roomDto);
        });


        return new AuthResponseDto(accessToken, refreshToken, userDto);
    }

    @Override
    public AuthResponseDto refreshToken(String refreshTokenValue) {
        if (!jwtUtil.validateToken(refreshTokenValue)) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        String email = jwtUtil.extractUsername(refreshTokenValue);
        Long userId = jwtUtil.extractUserId(refreshTokenValue);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for refresh token."));

        if (!user.isActive() || !user.getId().equals(userId)) {
             throw new UnauthorizedException("Invalid refresh token user context.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), user.getRole());
        // Optionally, issue a new refresh token as well for sliding sessions
        // String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        UserDto userDto = modelMapper.map(user, UserDto.class); // For consistency, though not always needed for refresh
        userDto.setFullName(user.getFirstName() + " " + user.getLastName());


        return new AuthResponseDto(newAccessToken, refreshTokenValue, userDto); // Or newRefreshToken
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequest) {
        User user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User with this email not found."));

        String resetToken = UUID.randomUUID().toString(); // Simple token
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Token expires in 1 hour
        userRepository.save(user);

        // Send password reset email
        // String resetUrl = "YOUR_FRONTEND_URL/reset-password?token=" + resetToken;
        // emailService.sendPasswordResetEmail(user, resetUrl);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto resetPasswordRequest) {
        User user = userRepository.findByPasswordResetToken(resetPasswordRequest.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired.");
        }

        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String userEmail, PasswordChangeRequestDto passwordChangeRequest) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public UserDto getAuthenticatedUserProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setFullName(user.getFirstName() + " " + user.getLastName());
        user.getCurrentOccupancy().ifPresent(occupancy -> {
            RoomSlimDto roomDto = modelMapper.map(occupancy.getRoom(), RoomSlimDto.class);
            userDto.setCurrentRoom(roomDto);
        });
        return userDto;
    }
}
