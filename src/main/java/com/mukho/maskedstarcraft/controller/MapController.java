package com.mukho.maskedstarcraft.controller;

import com.mukho.maskedstarcraft.dto.request.MapCreateRequest;
import com.mukho.maskedstarcraft.dto.response.ApiResponse;
import com.mukho.maskedstarcraft.dto.response.MapResponse;
import com.mukho.maskedstarcraft.service.MapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class MapController {
    
    private final MapService mapService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<MapResponse>> createMap(@Valid @RequestBody MapCreateRequest request) {
        MapResponse response = mapService.createMap(request);
        return ResponseEntity.ok(ApiResponse.success("맵이 생성되었습니다.", response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MapResponse>>> getAllMaps() {
        List<MapResponse> maps = mapService.getAllMaps();
        return ResponseEntity.ok(ApiResponse.success(maps));
    }
    
    @DeleteMapping("/{mapId}")
    public ResponseEntity<ApiResponse<Void>> deleteMap(@PathVariable Long mapId) {
        mapService.deleteMap(mapId);
        return ResponseEntity.ok(ApiResponse.success("맵이 삭제되었습니다.", null));
    }
}
