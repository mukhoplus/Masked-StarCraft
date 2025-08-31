package com.mukho.maskedstarcraft.dto.response;

import com.mukho.maskedstarcraft.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String nickname;
    private String role;
    
    public static LoginResponse from(String token, User user) {
        return LoginResponse.builder()
                .token(token)
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .build();
    }
}
