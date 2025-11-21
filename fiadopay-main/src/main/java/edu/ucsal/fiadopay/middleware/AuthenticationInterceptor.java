package edu.ucsal.fiadopay.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.annotation.Public;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            boolean isPublicMethod = method.hasMethodAnnotation(Public.class);
            boolean isPublicClass = method.getBeanType().isAnnotationPresent(Public.class);
            if (isPublicMethod || isPublicClass) {
                return true;
            }
        }

        String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer FAKE-")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }

            Merchant merchant = authService.merchantFromAuth(authHeader);
            request.setAttribute("merchant", merchant);
            
            return true;

        } catch (ResponseStatusException ex) {
            sendJsonError(response, request, ex.getStatusCode().value(), ex.getReason());
            return false;
        } catch (Exception ex) {
            sendJsonError(response, request, HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            return false;
        }
    }

    private void sendJsonError(HttpServletResponse response, HttpServletRequest request, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("status", status);
        
        String errorPhrase = "Error";
        try {
            errorPhrase = HttpStatus.valueOf(status).getReasonPhrase();
        } catch (Exception e) {}

        errorDetails.put("error", errorPhrase);
        errorDetails.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}