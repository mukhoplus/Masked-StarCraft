package com.mukho.maskedstarcraft.controller;

import com.mukho.maskedstarcraft.dto.request.ApplyRequest;
import com.mukho.maskedstarcraft.dto.request.LoginRequest;
import com.mukho.maskedstarcraft.dto.response.ApiResponse;
import com.mukho.maskedstarcraft.dto.response.LoginResponse;
import com.mukho.maskedstarcraft.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void>> apply(@Valid @RequestBody ApplyRequest request) {
        authService.apply(request);
        return ResponseEntity.ok(ApiResponse.success("참가 신청이 완료되었습니다.", null));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인되었습니다.", response));
    }
}
