package com.mukho.maskedstarcraft.service;

import com.mukho.maskedstarcraft.dto.request.MapCreateRequest;
import com.mukho.maskedstarcraft.dto.response.MapResponse;
import com.mukho.maskedstarcraft.entity.Map;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.MapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MapService {
    
    private final MapRepository mapRepository;
    
    public MapResponse createMap(MapCreateRequest request) {
        // 맵 이름 중복 체크
        if (mapRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new MapAlreadyExistsException(request.getName());
        }
        
        Map map = Map.builder()
                .name(request.getName())
                .build();
        
        Map savedMap = mapRepository.save(map);
        log.info("New map created: {}", request.getName());
        
        return MapResponse.from(savedMap);
    }
    
    @Transactional(readOnly = true)
    public List<MapResponse> getAllMaps() {
        return mapRepository.findAllActiveMaps().stream()
                .map(MapResponse::from)
                .collect(Collectors.toList());
    }
    
    public void deleteMap(Long mapId) {
        Map map = mapRepository.findByIdAndIsDeletedFalse(mapId)
                .orElseThrow(() -> new MapNotFoundException());
        
        map.setIsDeleted(true);
        mapRepository.save(map);
        
        log.info("Map deleted: {}", map.getName());
    }
    
    public static class MapAlreadyExistsException extends BusinessException {
        public MapAlreadyExistsException(String mapName) {
            super("이미 존재하는 맵 이름입니다: " + mapName);
        }
    }
    
    public static class MapNotFoundException extends BusinessException {
        public MapNotFoundException() {
            super("맵을 찾을 수 없습니다");
        }
    }
}
