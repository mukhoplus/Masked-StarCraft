package com.mukho.maskedstarcraft.service;

import com.mukho.maskedstarcraft.dto.request.ApplyRequest;
import com.mukho.maskedstarcraft.dto.request.LoginRequest;
import com.mukho.maskedstarcraft.dto.response.LoginResponse;
import com.mukho.maskedstarcraft.entity.User;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.UserRepository;
import com.mukho.maskedstarcraft.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    
    public void apply(ApplyRequest request) {
        // 닉네임 중복 체크
        if (userRepository.existsByNicknameAndIsDeletedFalse(request.getNickname())) {
            throw new UserAlreadyExistsException(request.getNickname());
        }
        
        // 사용자 생성
        User user = User.builder()
                .name(request.getName())
                .nickname(request.getNickname())
                .password(request.getPassword())  // 평문 저장
                .race(request.getRace())
                .role(User.Role.PLAYER)
                .build();
        
        userRepository.save(user);
        log.info("New player registered: {}", request.getNickname());
    }
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByNicknameAndIsDeletedFalse(request.getNickname())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다"));
        
        if (!request.getPassword().equals(user.getPassword())) {  // 평문 비교
            throw new InvalidPasswordException();
        }
        
        String token = jwtUtil.generateToken(user.getNickname(), user.getRole().name());
        log.info("User logged in: {} ({})", user.getNickname(), user.getRole());
        
        return LoginResponse.from(token, user);
    }
    
    // 예외 클래스들을 static inner class로 정의
    public static class UserAlreadyExistsException extends BusinessException {
        public UserAlreadyExistsException(String nickname) {
            super("이미 존재하는 닉네임입니다: " + nickname);
        }
    }
    
    public static class UserNotFoundException extends BusinessException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class InvalidPasswordException extends BusinessException {
        public InvalidPasswordException() {
            super("잘못된 비밀번호입니다");
        }
    }
}
