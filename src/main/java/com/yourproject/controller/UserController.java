package com.yourproject.controller;

import com.yourproject.dto.ApiResponse;
import com.yourproject.dto.UserDto;
import com.yourproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users") // General user-related endpoints
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint for current user to upload/update their profile image
    @PostMapping("/me/profile-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserDto updatedUser = userService.updateUserProfileImage(email, file);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile image updated successfully"));
    }

    // Endpoint to get a specific user's details (e.g., by admin or for public profiles if any)
    // This overlaps with AdminController.getStudentById if that's intended for admin only.
    // For now, an admin might get any user.
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')") // Or a more complex permission
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId) {
        UserDto userDto = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userDto, "User details fetched."));
    }
}
