package com.mukho.maskedstarcraft.dto.response;

import com.mukho.maskedstarcraft.entity.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MapResponse {
    private Long id;
    private String name;
    
    public static MapResponse from(Map map) {
        return MapResponse.builder()
                .id(map.getId())
                .name(map.getName())
                .build();
    }
}
