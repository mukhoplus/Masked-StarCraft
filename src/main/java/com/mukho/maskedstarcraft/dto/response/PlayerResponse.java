package com.mukho.maskedstarcraft.dto.response;

import com.mukho.maskedstarcraft.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerResponse {
    private Long id;
    private String name;
    private String nickname;
    private String race;
    
    public static PlayerResponse from(User user) {
        return PlayerResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .race(user.getRace())
                .build();
    }
    
    public static PlayerResponse fromPublic(User user) {
        return PlayerResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .race(user.getRace())
                .build();
    }
}
