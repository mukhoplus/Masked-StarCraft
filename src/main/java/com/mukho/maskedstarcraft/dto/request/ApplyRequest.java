package com.mukho.maskedstarcraft.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다")
    private String name;
    
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 100, message = "닉네임은 100자 이내여야 합니다")
    private String nickname;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
    private String password;
    
    @NotBlank(message = "종족은 필수입니다")
    @Pattern(regexp = "^(프로토스|테란|저그)$", message = "종족은 프로토스, 테란, 저그 중 하나여야 합니다")
    private String race;
}
