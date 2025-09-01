package com.mukho.maskedstarcraft.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mukho.maskedstarcraft.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString(); // 암호화하지 않음
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword.toString().equals(encodedPassword); // 평문 비교
            }
        };
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - 인증 불필요
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/players").permitAll()        // 참가 신청
                .requestMatchers(HttpMethod.GET, "/api/v1/players").permitAll()         // 참가자 목록 조회
                .requestMatchers(HttpMethod.GET, "/api/v1/tournaments/current").permitAll() // 현재 대회 정보 조회
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()                 // CORS OPTIONS 요청 허용
                
                // Player can cancel their own participation (구체적인 패턴을 먼저)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/players/me").hasAnyRole("PLAYER", "ADMIN")  // 자신의 참가 취소
                
                // Admin only endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/tournaments/start").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/games/result").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/players/{playerId}").hasRole("ADMIN")  // 특정 플레이어 삭제 (관리자만)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/players").hasRole("ADMIN")            // 모든 플레이어 삭제 (관리자만)
                .requestMatchers("/api/v1/maps/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/logs/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            ); // For H2 console
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
