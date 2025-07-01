package com.yourproject.service;

import com.yourproject.dto.ProfileUpdateRequestDto;
import com.yourproject.dto.UserDto;
import com.yourproject.dto.UserRegistrationRequestDto; // For admin creating users
import com.yourproject.entity.Role;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    // User profile operations (could also be part of AuthService or separate ProfileService)
    UserDto updateUserProfile(String email, ProfileUpdateRequestDto profileUpdateRequest);
    UserDto updateUserProfileImage(String email, MultipartFile profileImageFile); // New method
    UserDto getUserByEmail(String email);
    UserDto getUserById(Long id);

    // Admin/Staff operations for managing users
    UserDto createUser(UserRegistrationRequestDto userDto); // Admin creating any user
    UserDto updateUser(Long userId, ProfileUpdateRequestDto userUpdateDto); // Admin updating any user
    void deleteUser(Long userId); // Or deactivateUser
    void deactivateUser(Long userId);
    void activateUser(Long userId);

    Page<UserDto> getAllUsers(Pageable pageable, String role, String searchTerm);
    List<UserDto> findUsersByRole(Role role);

    // Room assignment related (might be better in a dedicated RoomAssignmentService or AdminService)
    // UserDto assignRoomToStudent(Long studentId, Long roomId, Integer bedNumber);
    // UserDto removeStudentFromRoom(Long studentId);

    User findUserEntityById(Long id); // Helper to get raw entity for internal service use
    User findUserEntityByEmail(String email);
}
