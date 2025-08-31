package com.mukho.maskedstarcraft.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
