package com.yourproject.service.impl;

import com.yourproject.dto.ProfileUpdateRequestDto;
import com.yourproject.dto.RoomSlimDto;
import com.yourproject.dto.UserDto;
import com.yourproject.dto.UserRegistrationRequestDto;
import com.yourproject.entity.Role;
import com.yourproject.entity.User;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.repository.UserRepository;
// import com.yourproject.repository.RoomRepository; // If handling room assignment here
// import com.yourproject.repository.OccupancyRepository; // If handling room assignment here
import com.yourproject.service.FileUploadService; // Added
import com.yourproject.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile; // Added
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder; // For admin creating user with password
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // For checking empty strings

import java.time.LocalDateTime; // For ID generation example
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder; // Needed if admin creates users with passwords
    private final FileUploadService fileUploadService; // Added

    // @Autowired
    // private RoomRepository roomRepository; // If assign/remove room logic is here
    // @Autowired
    // private OccupancyRepository occupancyRepository; // If assign/remove room logic is here


    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           ModelMapper modelMapper,
                           PasswordEncoder passwordEncoder,
                           FileUploadService fileUploadService) { // Added
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService; // Added
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setFullName(user.getFirstName() + " " + user.getLastName());
        user.getCurrentOccupancy().ifPresent(occupancy -> {
            if (occupancy.getRoom() != null) {
                RoomSlimDto roomDto = modelMapper.map(occupancy.getRoom(), RoomSlimDto.class);
                userDto.setCurrentRoom(roomDto);
            }
        });
        return userDto;
    }

    @Override
    @Transactional
    public UserDto updateUserProfile(String email, ProfileUpdateRequestDto profileUpdateRequest) {
        User user = findUserEntityByEmail(email);

        // Update allowed fields
        if (StringUtils.hasText(profileUpdateRequest.getFirstName())) {
            user.setFirstName(profileUpdateRequest.getFirstName());
        }
        if (StringUtils.hasText(profileUpdateRequest.getLastName())) {
            user.setLastName(profileUpdateRequest.getLastName());
        }
        if (StringUtils.hasText(profileUpdateRequest.getPhone())) {
            user.setPhone(profileUpdateRequest.getPhone());
        }
        if (profileUpdateRequest.getAddress() != null) {
            user.setAddress(profileUpdateRequest.getAddress());
        }
        if (profileUpdateRequest.getEmergencyContact() != null) {
            user.setEmergencyContact(profileUpdateRequest.getEmergencyContact());
        }
        if (StringUtils.hasText(profileUpdateRequest.getProfileImageUrl())) {
            user.setProfileImageUrl(profileUpdateRequest.getProfileImageUrl());
        }

        // Role-specific fields (ensure user has the correct role before updating)
        if (user.getRole() == Role.STUDENT) {
            if (StringUtils.hasText(profileUpdateRequest.getCourse())) {
                user.setCourse(profileUpdateRequest.getCourse());
            }
            if (profileUpdateRequest.getYear() != null) {
                user.setYear(profileUpdateRequest.getYear());
            }
        } else if (user.getRole() == Role.ADMIN || user.getRole() == Role.WARDEN) {
            if (StringUtils.hasText(profileUpdateRequest.getDepartment())) {
                user.setDepartment(profileUpdateRequest.getDepartment());
            }
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        return convertToDto(findUserEntityByEmail(email));
    }

    @Override
    public UserDto getUserById(Long id) {
        return convertToDto(findUserEntityById(id));
    }

    @Override
    @Transactional
    public UserDto createUser(UserRegistrationRequestDto registrationRequest) {
        // This is for admin creating users. Differs from self-registration in AuthService
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BadRequestException("Email address already in use.");
        }

        User user = modelMapper.map(registrationRequest, User.class);
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword())); // Admin sets initial password
        user.setActive(true);
        user.setEmailVerified(true); // Admin-created users might be auto-verified

        // ID Generation
        if (user.getRole() == Role.STUDENT) {
            user.setStudentId("STU" + LocalDateTime.now().getYear() + (10000 + (int)(Math.random() * 90000)));
             if (registrationRequest.getCourse() != null) user.setCourse(registrationRequest.getCourse());
             if (registrationRequest.getYear() != null) user.setYear(registrationRequest.getYear());
        } else if (user.getRole() == Role.ADMIN || user.getRole() == Role.WARDEN) {
            user.setEmployeeId("EMP" + (10000 + (int)(Math.random() * 90000)));
            user.setJoinDate(java.time.LocalDate.now());
            if (registrationRequest.getDepartment() != null) user.setDepartment(registrationRequest.getDepartment());
        }

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, ProfileUpdateRequestDto userUpdateDto) {
        // This is for admin updating any user. More fields might be updatable by admin.
        User user = findUserEntityById(userId);

        // Example: Admin can update more fields than user's own profile update
        if (StringUtils.hasText(userUpdateDto.getFirstName())) user.setFirstName(userUpdateDto.getFirstName());
        if (StringUtils.hasText(userUpdateDto.getLastName())) user.setLastName(userUpdateDto.getLastName());
        // Potentially email, role (with caution), isActive status etc.
        // For now, keeping it similar to profile update for simplicity
        if (StringUtils.hasText(userUpdateDto.getPhone())) user.setPhone(userUpdateDto.getPhone());
        if (userUpdateDto.getAddress() != null) user.setAddress(userUpdateDto.getAddress());
        if (userUpdateDto.getEmergencyContact() != null) user.setEmergencyContact(userUpdateDto.getEmergencyContact());

        if (user.getRole() == Role.STUDENT) {
            if (StringUtils.hasText(userUpdateDto.getCourse())) user.setCourse(userUpdateDto.getCourse());
            if (userUpdateDto.getYear() != null) user.setYear(userUpdateDto.getYear());
        } else {
             if (StringUtils.hasText(userUpdateDto.getDepartment())) user.setDepartment(userUpdateDto.getDepartment());
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        User user = findUserEntityById(userId);
        if (!user.isActive()) {
            throw new BadRequestException("User is already deactivated.");
        }
        // Add logic: remove from room if student?
        // if (user.getRole() == Role.STUDENT && user.getCurrentRoom().isPresent()) {
        //     removeStudentFromRoom(userId, user.getCurrentRoom().get().getId());
        // }
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        User user = findUserEntityById(userId);
        if (user.isActive()) {
            throw new BadRequestException("User is already active.");
        }
        user.setActive(true);
        userRepository.save(user);
    }


    @Override
    @Transactional
    public void deleteUser(Long userId) {
        // This should be a hard delete, typically not recommended. Deactivation is preferred.
        // For now, let's implement deactivation as "delete" from admin perspective.
        User user = findUserEntityById(userId);
        // If truly hard delete:
        // userRepository.delete(user);
        // For soft delete / deactivation:
        deactivateUser(userId); // Or throw exception if true hard delete is expected.
    }


    @Override
    public Page<UserDto> getAllUsers(Pageable pageable, String roleFilter, String searchTerm) {
        Specification<User> spec = Specification.where(null);

        if (StringUtils.hasText(roleFilter)) {
            try {
                Role role = Role.valueOf(roleFilter.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
            } catch (IllegalArgumentException e) {
                // Handle invalid role string, e.g., log or ignore
            }
        }

        if (StringUtils.hasText(searchTerm)) {
            String term = "%" + searchTerm.toLowerCase() + "%";
            Specification<User> searchSpec = (root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("firstName")), term),
                    cb.like(cb.lower(root.get("lastName")), term),
                    cb.like(cb.lower(root.get("email")), term),
                    cb.like(cb.lower(root.get("studentId")), term),
                    cb.like(cb.lower(root.get("employeeId")), term)
                );
            spec = spec.and(searchSpec);
        }

        return userRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    @Override
    public List<UserDto> findUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public User findUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    public User findUserEntityByEmail(String email) {
         return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional
    public UserDto updateUserProfileImage(String email, MultipartFile profileImageFile) {
        User user = findUserEntityByEmail(email);

        if (profileImageFile == null || profileImageFile.isEmpty()) {
            throw new BadRequestException("Profile image file cannot be empty.");
        }

        // Delete old profile image if it exists and is managed by our system
        if (StringUtils.hasText(user.getProfileImageUrl())) {
            // Assuming profileImageUrl stores a relative path that fileUploadService can understand
            fileUploadService.deleteFile(user.getProfileImageUrl());
        }

        // Store the new file, e.g., in "profiles/{userId}/" subdirectory
        String relativePath = fileUploadService.storeFile(profileImageFile, "profiles", user.getId().toString());

        // Construct full URL if needed for DTO, or store relative path and construct URL on retrieval
        // For now, storing relative path. The DTO or a utility can construct full URL.
        user.setProfileImageUrl(relativePath);

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }
}
