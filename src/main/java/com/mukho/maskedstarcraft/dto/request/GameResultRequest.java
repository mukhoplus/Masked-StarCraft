package com.mukho.maskedstarcraft.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameResultRequest {
    
    @NotNull(message = "승자 ID는 필수입니다")
    private Long winnerId;
}
