# 복면스타왕 (Masked StarCraft) 🏆

운영자가 주도하여 '승자 연전(King of the Hill)' 방식으로 진행하는 토너먼트 서비스

## 🚀 로컬 개발 환경 설정

### 1. 필수 요구사항

- Java 17 이상
- MySQL 8.0 이상
- Git

### 2. 데이터베이스 설정

MySQL에 데이터베이스를 생성합니다:

```sql
CREATE DATABASE maskedstarcraft;
```

### 3. 로컬 설정 파일 생성

`src/main/resources/application-dev.properties` 파일을 생성하고 MySQL 정보를 입력하세요:

```properties
# Local Development Database Configuration
spring.datasource.username=root
spring.datasource.password=your_mysql_password_here
```

> ⚠️ 이 파일은 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

### 4. 관리자 계정 설정

`src/main/resources/admin.properties` 파일을 생성하고 관리자 정보를 입력하세요:

```properties
# Admin Account Configuration
admin.name=관리자
admin.nickname=admin
admin.password=admin123!
admin.race=관리자
```

> ⚠️ 이 파일도 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

### 5. 애플리케이션 실행

```bash
# 프로젝트 클론
git clone https://github.com/mukhoplus/Masked-StarCraft.git
cd Masked-StarCraft

# 애플리케이션 실행
./gradlew bootRun
```

### 6. 관리자 계정

애플리케이션이 시작되면 `admin.properties` 파일의 정보를 바탕으로 관리자 계정이 자동으로 생성됩니다.

## 🛠️ API 엔드포인트

### 인증 (Auth)

- `POST /api/v1/auth/apply` - 참가 신청
- `POST /api/v1/auth/login` - 로그인

### 참가자 (Players)

- `GET /api/v1/players` - 참가자 목록 조회
- `DELETE /api/v1/players/{playerId}` - 특정 참가자 삭제 (관리자)
- `DELETE /api/v1/players` - 모든 참가자 초기화 (관리자)

### 맵 관리 (Maps) - 관리자 전용

- `POST /api/v1/maps` - 새 맵 추가
- `GET /api/v1/maps` - 맵 목록 조회
- `DELETE /api/v1/maps/{mapId}` - 맵 삭제

### 대회 및 게임 (Tournament & Game)

- `GET /api/v1/tournaments/current` - 현재 대회 정보 조회
- `POST /api/v1/tournaments/start` - 대회 시작 (관리자)
- `POST /api/v1/games/result` - 게임 결과 기록 (관리자)

### 로그 관리 (Logs) - 관리자 전용

- `GET /api/v1/logs/tournaments` - 완료된 모든 대회 로그 목록 조회
- `GET /api/v1/logs/tournaments/{tournamentId}` - 특정 대회 상세 로그 조회
- `GET /api/v1/logs/tournaments/{tournamentId}/download` - 대회 로그 텍스트 파일 다운로드

## 🌐 배포

### Railway 배포

Railway에서 다음 환경변수들을 설정하세요:

```
DATABASE_URL=mysql://user:password@host:port/database
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password
DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect
JWT_SECRET=your_production_jwt_secret
SPRING_PROFILES_ACTIVE=prod
```

## 🔧 기술 스택

- **Backend:** Java 17, Spring Boot 3.x, Spring Security, JWT
- **Database:** MySQL (Production), H2 (Test)
- **Build:** Gradle
- **Real-time:** WebSocket (STOMP)

## 📝 프로젝트 구조

```
src/main/java/com/mukho/maskedstarcraft/
├── controller/     # REST API 컨트롤러
├── service/        # 비즈니스 로직
├── repository/     # 데이터 접근 계층
├── entity/         # JPA 엔티티
├── dto/            # 데이터 전송 객체
├── security/       # 보안 설정
├── config/         # 애플리케이션 설정
└── exception/      # 예외 처리
```
