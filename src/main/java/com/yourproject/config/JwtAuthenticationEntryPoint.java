package com.yourproject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourproject.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorMessage = "Unauthorized access. Please provide a valid token.";
        Object expiredAttribute = request.getAttribute("expired"); // Check if set by JwtRequestFilter
        if (expiredAttribute != null) {
            errorMessage = "JWT Token has expired: " + expiredAttribute.toString();
        } else if (authException.getMessage() != null) {
            // Use Spring Security's message if more specific, otherwise default
            // errorMessage = authException.getMessage();
        }


        ApiResponse<Object> apiResponse = ApiResponse.error(errorMessage);

        OutputStream out = response.getOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, apiResponse);
        out.flush();
    }
}
