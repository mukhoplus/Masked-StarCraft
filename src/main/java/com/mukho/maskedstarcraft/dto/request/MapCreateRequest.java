package com.mukho.maskedstarcraft.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MapCreateRequest {
    
    @NotBlank(message = "맵 이름은 필수입니다")
    @Size(max = 100, message = "맵 이름은 100자 이내여야 합니다")
    private String name;
}
