package com.hostel.service;

import com.hostel.model.User;
import com.hostel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User saveUser(User user) {
        if (user.getRole().equals("STUDENT")) {
            user.setStudentId(generateStudentId());
        } else {
            user.setEmployeeId(generateEmployeeId());
        }
        return userRepository.save(user);
    }

    public User updateUser(String id, User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setFirstName(userDetails.getFirstName());
            user.setLastName(userDetails.getLastName());
            user.setPhone(userDetails.getPhone());
            user.setAddress(userDetails.getAddress());
            user.setEmergencyContact(userDetails.getEmergencyContact());
            user.setCourse(userDetails.getCourse());
            user.setYear(userDetails.getYear());
            user.setDepartment(userDetails.getDepartment());
            return userRepository.save(user);
        }
        return null;
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public List<User> getActiveUsersByRole(String role) {
        return userRepository.findByRoleAndIsActive(role, true);
    }

    public List<User> searchUsers(String role, String searchTerm) {
        return userRepository.findByRoleAndSearch(role, searchTerm);
    }

    public long countUsersByRole(String role) {
        return userRepository.countByRole(role);
    }

    public long countActiveUsersByRole(String role) {
        return userRepository.countByRoleAndIsActive(role, true);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User updateLastLogin(String userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }
        return null;
    }

    public User deactivateUser(String id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setActive(false);
            return userRepository.save(user);
        }
        return null;
    }

    public Map<String, Object> getStudentsWithPagination(int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("firstName"));
        
        Page<User> studentsPage;
        if (search != null && !search.trim().isEmpty()) {
            studentsPage = userRepository.findByRoleAndSearch("STUDENT", search, pageable);
        } else {
            studentsPage = userRepository.findByRoleAndIsActive("STUDENT", true, pageable);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("students", studentsPage.getContent());
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current", page);
        pagination.put("pages", studentsPage.getTotalPages());
        pagination.put("total", studentsPage.getTotalElements());
        result.put("pagination", pagination);
        
        return result;
    }

    private String generateStudentId() {
        int year = LocalDateTime.now().getYear();
        int random = (int) (Math.random() * 10000);
        return String.format("STU%d%04d", year, random);
    }

    private String generateEmployeeId() {
        int random = (int) (Math.random() * 10000);
        return String.format("EMP%04d", random);
    }
}