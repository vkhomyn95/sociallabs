package com.workflow.sociallabs.controller;

import com.workflow.sociallabs.domain.entity.User;
import com.workflow.sociallabs.dto.request.AuthRequest;
import com.workflow.sociallabs.dto.request.RegisterRequest;
import com.workflow.sociallabs.dto.response.AuthResponse;
import com.workflow.sociallabs.security.JwtTokenProvider;
import com.workflow.sociallabs.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(user.toDTO())
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(user.toDTO())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<User.UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(user.toDTO());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // В JWT токени є stateless, тому logout виконується на клієнті
        // Можна додати blacklist токенів, якщо потрібно
        return ResponseEntity.ok().build();
    }
}
