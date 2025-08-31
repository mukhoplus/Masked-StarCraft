package com.mukho.maskedstarcraft.config;

import com.mukho.maskedstarcraft.entity.User;
import com.mukho.maskedstarcraft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final UserRepository userRepository;
    
    @Override
    public void run(String... args) throws Exception {
        createAdminIfNotExists();
        log.info("Application startup completed");
    }
    
    private void createAdminIfNotExists() {
        if (!userRepository.existsByNicknameAndIsDeletedFalse("admin")) {
            try {
                Properties adminProps = loadAdminProperties();
                
                User admin = User.builder()
                        .name(adminProps.getProperty("admin.name", "관리자"))
                        .nickname(adminProps.getProperty("admin.nickname", "admin"))
                        .password(adminProps.getProperty("admin.password", "admin123!"))
                        .race(adminProps.getProperty("admin.race", "관리자"))
                        .role(User.Role.ADMIN)
                        .build();
                
                userRepository.save(admin);
                log.info("Default admin account created: {}", admin.getNickname());
                
            } catch (IOException e) {
                log.error("Failed to load admin properties, using default values", e);
                
                // 기본값으로 관리자 생성
                User admin = User.builder()
                        .name("관리자")
                        .nickname("admin")
                        .password("admin123!")
                        .race("관리자")
                        .role(User.Role.ADMIN)
                        .build();
                
                userRepository.save(admin);
                log.info("Default admin account created with default values");
            }
        }
    }
    
    private Properties loadAdminProperties() throws IOException {
        Properties props = new Properties();
        ClassPathResource resource = new ClassPathResource("admin.properties");
        
        try (InputStream inputStream = resource.getInputStream()) {
            props.load(inputStream);
        }
        
        return props;
    }
}
