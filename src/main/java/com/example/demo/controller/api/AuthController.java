package com.example.demo.controller.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.model.UserEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.PasswordUtils;
import com.example.demo.utils.ResponseUtils;

@RestController
@RequestMapping("/api/auth/")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseUtils.fail("Username atau password salah", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = userOpt.get();

        if (!PasswordUtils.matches(request.getPassword(), user.getPassword())) {
            return ResponseUtils.fail("Username atau password salah", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getRole());

        return ResponseUtils.ok("Login berhasil", new LoginResponse(token));
    }

    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, BindingResult result) {

        if (result.hasErrors()) {
            String errorMessage = result.getAllErrors().get(0).getDefaultMessage();
            return ResponseUtils.fail(errorMessage, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseUtils.fail("Username sudah digunakan", HttpStatus.CONFLICT);
        }

        UserEntity newUser = new UserEntity();

        newUser.setRole(request.getRole() != null ? request.getRole() : "USER");
        newUser.setUsername(request.getUsername());
        newUser.setPassword(PasswordUtils.hashPassword(request.getPassword()));

        userRepository.saveAndFlush(newUser);

        return ResponseUtils.ok("success Register", newUser);
    }
}
