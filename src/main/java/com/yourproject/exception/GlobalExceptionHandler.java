package com.yourproject.exception;

import com.yourproject.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // Spring Security's Access Denied
import org.springframework.security.core.AuthenticationException; // Spring Security's Auth Exception
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException; // For @RequestParam, @PathVariable validation
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Handle specific custom exceptions
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(com.yourproject.exception.AccessDeniedException.class) // Custom AccessDeniedException
    public ResponseEntity<ApiResponse<Object>> handleCustomAccessDeniedException(com.yourproject.exception.AccessDeniedException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileNotFoundException(FileNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // Handle Spring Security exceptions
    @ExceptionHandler(AccessDeniedException.class) // This is org.springframework.security.access.AccessDeniedException
    public ResponseEntity<ApiResponse<Object>> handleSpringAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error("Access Denied: You do not have permission to perform this action."), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class) // This is org.springframework.security.core.AuthenticationException
    public ResponseEntity<ApiResponse<Object>> handleSpringAuthenticationException(AuthenticationException ex, WebRequest request) {
        // This might be caught by JwtAuthenticationEntryPoint for token issues,
        // but can be a fallback for other auth problems.
        return new ResponseEntity<>(ApiResponse.error("Authentication failed: " + ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtException.class) // General JWT issues not caught by filter for specific response
    public ResponseEntity<ApiResponse<Object>> handleJwtException(JwtException ex, WebRequest request) {
        String message = "Invalid or malformed token.";
        if (ex instanceof ExpiredJwtException) {
            message = "Token has expired.";
        }
        return new ResponseEntity<>(ApiResponse.error(message), HttpStatus.UNAUTHORIZED);
    }


    // Override for @Valid request body validation
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(ApiResponse.error("Validation Failed", errors), HttpStatus.BAD_REQUEST);
    }

    // Handle @RequestParam, @PathVariable validation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());
        return new ResponseEntity<>(ApiResponse.error("Validation Failed", errors), HttpStatus.BAD_REQUEST);
    }


    // Generic fallback handler for other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllOtherExceptions(Exception ex, WebRequest request) {
        // Log the full exception for server-side review
        logger.error("An unexpected error occurred: ", ex);
        // Return a generic error message to the client
        return new ResponseEntity<>(ApiResponse.error("An unexpected internal server error occurred. Please try again later."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
