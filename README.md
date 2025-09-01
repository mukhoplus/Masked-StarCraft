# 복면스타왕 (Masked StarCraft) 🏆

운영자가 주도하여 '승자 연전(King of the Hill)' 방식으로 진행하는 스타크래프트 토너먼트 서비스입니다.

## 📋 프로젝트 개요

**복면스타왕**은 스타크래프트 경기를 위한 실시간 토너먼트 관리 시스템입니다. 관리자가 대회를 시작하면 참가자들이 1:1로 경기를 진행하며, 승자는 계속 왕좌를 지키고 패자는 탈락하는 '승자 연전' 방식으로 운영됩니다.

### 클라이언트

[Frontend Repository](https://github.com/mukhoplus/Masted-StarCraft-Client)

### 🎯 주요 기능

- **실시간 참가 신청**: 웹을 통한 참가자 등록 및 직접 참가 취소
- **토너먼트 관리**: 관리자가 대회 시작/종료 및 경기 결과 입력
- **실시간 업데이트**: WebSocket을 통한 실시간 토너먼트 상황 전파
- **경기 로그**: 모든 경기 기록 저장 및 다운로드
- **맵 관리**: 경기에 사용될 맵 등록 및 관리
- **승자 연전 시스템**: King of the Hill 방식의 토너먼트 진행
- **통계 시스템**: 연승 기록 및 최다 연승자 통계 제공

## 🔧 기술 스택

### Backend

- **Java 17**: OpenJDK 17 기반
- **Spring Boot 3.5.5**: 메인 프레임워크
- **Spring Security**: JWT 기반 인증/인가 시스템
- **Spring Data JPA**: 데이터 접근 계층
- **Spring WebSocket**: STOMP 프로토콜을 통한 실시간 통신

### Database

- **MySQL 8.0**: 프로덕션 데이터베이스
- **H2**: 테스트 및 개발용 인메모리 데이터베이스

### Security & Authentication

- **JWT (JSON Web Token)**: 토큰 기반 인증
- **JJWT 0.12.5**: JWT 구현 라이브러리
- **평문 비밀번호**: 개발 편의를 위한 임시 설정 (프로덕션 시 BCrypt 적용 예정)

### Build & Dependencies

- **Gradle**: 빌드 도구
- **Lombok**: 보일러플레이트 코드 자동 생성
- **Spring Boot DevTools**: 개발 편의성
- **Spring Boot Validation**: 입력값 검증

### Additional Features

- **CORS 설정**: 크로스 오리진 요청 처리
- **Hibernate**: ORM 매핑
- **SLF4J & Logback**: 로깅 시스템## 🚀 로컬 개발 환경 설정

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

### 🔐 인증 (Auth)

- `POST /api/v1/auth/apply` - 참가 신청
- `POST /api/v1/auth/login` - 로그인 (JWT 토큰 발급)

### 👥 참가자 (Players)

- `POST /api/v1/players` - 참가자 신청 (인증 불필요)
- `GET /api/v1/players` - 참가자 목록 조회 (인증 불필요)
- `DELETE /api/v1/players/me` - 자신의 참가 취소 (로그인 필요)
- `DELETE /api/v1/players/{playerId}` - 특정 참가자 삭제 (관리자 전용)
- `DELETE /api/v1/players` - 모든 참가자 초기화 (관리자 전용)

### 🗺️ 맵 관리 (Maps) - 관리자 전용

- `POST /api/v1/maps` - 새 맵 추가
- `GET /api/v1/maps` - 맵 목록 조회
- `DELETE /api/v1/maps/{mapId}` - 맵 삭제

### 🏆 대회 및 게임 (Tournament & Game)

- `GET /api/v1/tournaments/current` - 현재 대회 정보 조회 (인증 불필요)
- `POST /api/v1/tournaments/start` - 대회 시작 (관리자 전용)
- `POST /api/v1/games/result` - 게임 결과 기록 (관리자 전용)
- `POST /api/v1/tournaments/refresh` - 토너먼트 상태 새로고침

### 📊 로그 관리 (Logs) - 관리자 전용

- `GET /api/v1/logs/tournaments` - 완료된 모든 대회 로그 목록 조회
- `GET /api/v1/logs/tournaments/{tournamentId}` - 특정 대회 상세 로그 조회
- `GET /api/v1/logs/tournaments/{tournamentId}/download` - 대회 로그 텍스트 파일 다운로드

### 🔄 실시간 통신 (WebSocket)

- `ws://localhost:8080/ws` - WebSocket 연결 엔드포인트
- `/topic/tournament` - 토너먼트 업데이트 구독
- `/topic/game-result` - 게임 결과 알림 구독
- `/topic/refresh` - 새로고침 요청 구독

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

## 🎮 게임 진행 방식

### 승자 연전 (King of the Hill) 시스템

1. **참가 신청**: 사용자들이 웹을 통해 참가 신청 (이름, 닉네임, 비밀번호, 종족)
2. **대회 시작**: 관리자가 대회를 시작하면 첫 번째 경기가 자동으로 매칭
3. **경기 진행**:
   - 두 참가자가 1:1 경기 진행
   - 관리자가 경기 결과 입력
   - 승자는 왕좌 유지, 패자는 탈락
4. **다음 경기**: 새로운 도전자가 현재 왕과 경기
5. **대회 종료**: 모든 도전자가 소진되면 최종 왕이 우승자로 결정

### 실시간 업데이트

- **WebSocket 연결**: 클라이언트가 실시간으로 토너먼트 상황 업데이트 수신
- **경기 결과 알림**: 경기 결과가 입력되면 즉시 모든 참가자에게 알림
- **토너먼트 상태 변경**: 대회 시작/종료 시 실시간 상태 업데이트

## 📊 주요 기능 상세

### 🔐 인증 시스템

- JWT 기반 토큰 인증
- 관리자/일반 사용자 권한 분리
- 자동 관리자 계정 생성 (admin.properties 기반)

### 📋 참가자 관리

- 실시간 참가자 목록 조회
- 참가자 직접 참가 취소 기능 (진행 중인 대회가 없을 때만)
- 관리자의 참가자 개별/전체 삭제 기능
- 종족 정보 관리 (프로토스, 테란, 저그)
- **토너먼트 상태 연동**: 진행 중인 대회 시 참가 신청/취소 자동 차단

### 🗺️ 맵 풀 관리

- 관리자가 경기용 맵 등록/삭제
- 경기 시 등록된 맵 중 랜덤 선택

### 📈 통계 및 로그

- 모든 경기 기록 저장
- 연승 기록 계산 및 관리
- 대회별 상세 로그 다운로드 (텍스트 파일)
- 최다 연승자 통계

### 🔄 실시간 기능

- STOMP 프로토콜 기반 WebSocket 통신
- 토너먼트 상태 실시간 동기화
- 자동 새로고침 알림

## 🔍 디버깅 및 모니터링

### 로깅 시스템

- **JWT 인증 과정**: 토큰 검증, 사용자 인증 과정 상세 로그
- **참가자 관리**: 참가 신청/취소 과정 추적 로그
- **토너먼트 상태**: 대회 진행 상황 및 상태 변경 로그
- **보안 이벤트**: 권한 검증 실패, 인증 오류 등 보안 관련 이벤트

### 개발 도구

- **H2 콘솔**: 개발 환경에서 데이터베이스 직접 접근 (`/h2-console`)
- **디버그 로그**: 상세한 애플리케이션 실행 과정 추적
- **Spring Boot DevTools**: 코드 변경 시 자동 재시작

## 📋 현재 개발 상태

### ✅ 완료된 기능

#### 🏗️ 백엔드 API

- [x] **인증 시스템**: JWT 기반 로그인/인증 구현
- [x] **참가자 관리**: 참가 신청, 목록 조회, 자신의 참가 취소, 관리자 삭제 기능
- [x] **토너먼트 시스템**: 승자 연전 방식 토너먼트 로직 구현
- [x] **게임 로그**: 모든 경기 기록 저장 및 조회
- [x] **맵 관리**: 맵 등록/삭제/조회 기능
- [x] **실시간 통신**: WebSocket 기반 실시간 업데이트
- [x] **보안 설정**: Spring Security + JWT 인증 체계
- [x] **데이터베이스**: MySQL 연동 및 JPA 엔티티 설계

#### 🎯 핵심 비즈니스 로직

- [x] **승자 연전 시스템**: King of the Hill 토너먼트 방식
- [x] **연승 계산**: 각 플레이어의 연승 기록 추적
- [x] **경기 매칭**: 자동 다음 도전자 선별 로직
- [x] **토너먼트 종료**: 모든 도전자 소진 시 자동 종료
- [x] **통계 계산**: 최다 연승자 및 우승자 결정

#### 🔧 기술적 구현

- [x] **RESTful API**: 모든 기능의 REST API 구현
- [x] **예외 처리**: 통합 예외 처리 시스템
- [x] **입력 검증**: Bean Validation을 통한 데이터 유효성 검사
- [x] **CORS 설정**: 프론트엔드 연동을 위한 크로스 오리진 처리
- [x] **환경 설정**: 개발/프로덕션 환경 분리
- [x] **JWT 인증 디버깅**: 토큰 인증 과정 상세 로깅 시스템
- [x] **보안 규칙 최적화**: Security 매처 순서 개선으로 권한 체크 정확성 향상

#### 🛡️ 보안 및 권한 관리

- [x] **토너먼트 상태 기반 접근 제어**: 진행 중인 대회 시 참가 신청/취소 제한
- [x] **역할 기반 권한 분리**: ADMIN/PLAYER 역할별 API 접근 제어
- [x] **JWT 토큰 검증**: Bearer 토큰 형식 및 만료 시간 검증
- [x] **사용자 세션 관리**: Stateless 인증 방식으로 확장성 확보

### 🚧 진행 중/계획된 기능

#### 💻 프론트엔드

- [x] **웹 UI**: 사용자 친화적인 웹 인터페이스 개발
- [x] **실시간 대시보드**: 토너먼트 진행 상황 실시간 표시
- [x] **관리자 패널**: 대회 관리를 위한 관리 화면

#### 🎨 사용자 경험

- [x] **토너먼트 히스토리**: 과거 대회 결과 조회
- [x] **실시간 알림**: 경기 결과 및 상태 변경 알림

## � 프로젝트 구조

```
src/main/java/com/mukho/maskedstarcraft/
├── 📁 controller/          # REST API 컨트롤러
│   ├── AuthController.java      # 인증 관련 API
│   ├── PlayerController.java    # 참가자 관리 API
│   ├── TournamentController.java # 토너먼트 관리 API
│   ├── MapController.java       # 맵 관리 API
│   └── LogController.java       # 로그 조회 API
├── 📁 service/             # 비즈니스 로직
│   ├── AuthService.java         # 인증/참가 신청 서비스
│   ├── PlayerService.java       # 참가자 관리 서비스
│   ├── TournamentService.java   # 토너먼트 진행 서비스
│   ├── MapService.java          # 맵 관리 서비스
│   ├── LogService.java          # 로그 관리 서비스
│   └── WebSocketService.java    # 실시간 통신 서비스
├── 📁 repository/          # 데이터 접근 계층
│   ├── UserRepository.java      # 사용자 데이터 접근
│   ├── TournamentRepository.java # 토너먼트 데이터 접근
│   ├── GameLogRepository.java   # 게임 로그 데이터 접근
│   └── MapRepository.java       # 맵 데이터 접근
├── 📁 entity/              # JPA 엔티티
│   ├── User.java               # 사용자 엔티티
│   ├── Tournament.java         # 토너먼트 엔티티
│   ├── GameLog.java            # 게임 로그 엔티티
│   └── Map.java                # 맵 엔티티
├── 📁 dto/                 # 데이터 전송 객체
│   ├── 📁 request/              # 요청 DTO
│   └── 📁 response/             # 응답 DTO
├── 📁 security/            # 보안 설정
│   ├── SecurityConfig.java      # Spring Security 설정
│   ├── JwtUtil.java             # JWT 토큰 유틸리티
│   ├── JwtAuthenticationFilter.java # JWT 인증 필터
│   └── CustomUserDetailsService.java # 사용자 인증 서비스
├── 📁 config/              # 애플리케이션 설정
│   ├── WebSocketConfig.java     # WebSocket 설정
├── 📁 exception/           # 예외 처리
│   └── BusinessException.java   # 비즈니스 예외
└── MaskedstarcraftApplication.java # 메인 애플리케이션 클래스

src/main/resources/
├── application.properties      # 기본 설정
├── application-dev.properties  # 개발 환경 설정 (Git 제외)
├── admin.properties           # 관리자 계정 설정 (Git 제외)
└── 📁 static/                  # 정적 리소스
    └── index.html             # 기본 페이지
```

### 🏗️ 아키텍처 특징

- **계층 분리**: Controller → Service → Repository → Entity 구조
- **JWT 인증**: 토큰 기반 Stateless 인증 시스템
- **WebSocket 통신**: STOMP 프로토콜을 통한 실시간 업데이트
- **예외 처리**: 통합된 예외 처리 및 응답 구조
- **보안 설정**: Spring Security 기반 인증/인가 시스템
- **환경별 설정**: Profile 기반 환경 분리 (dev, prod)

## �🗂️ 데이터베이스 스키마

### 주요 테이블

```sql
-- 사용자 테이블
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    nickname VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    race VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 토너먼트 테이블
CREATE TABLE tournaments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    winner_user_id BIGINT,
    max_streak_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (winner_user_id) REFERENCES users(id),
    FOREIGN KEY (max_streak_user_id) REFERENCES users(id)
);

-- 게임 로그 테이블
CREATE TABLE game_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    map_id BIGINT NOT NULL,
    player1_id BIGINT NOT NULL,
    player2_id BIGINT NOT NULL,
    winner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id),
    FOREIGN KEY (map_id) REFERENCES maps(id),
    FOREIGN KEY (player1_id) REFERENCES users(id),
    FOREIGN KEY (player2_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
);

-- 맵 테이블
CREATE TABLE maps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

**⚠️ 참고사항 & 트러블슈팅**

### 🔧 개발 환경 설정

- 현재 개발 환경에서는 비밀번호를 평문으로 저장합니다 (프로덕션에서는 BCrypt 암호화 필요)
- H2 콘솔은 개발 시에만 활성화됩니다 (`/h2-console`)
- WebSocket 연결은 SockJS를 통해 브라우저 호환성을 보장합니다

### 🚫 비즈니스 규칙

- **참가 신청**: 진행 중인 토너먼트가 있을 때 신규 참가 신청 불가
- **참가 취소**: 진행 중인 토너먼트가 있을 때 참가 취소 불가
- **관리자 계정**: 참가 신청/취소 불가 (시스템 보호)

### 🔍 인증 관련 이슈 해결

**403 Forbidden 오류가 발생하는 경우:**

1. JWT 토큰이 `Authorization: Bearer {token}` 헤더로 전송되는지 확인
2. 토큰이 만료되지 않았는지 확인 (기본 만료시간: 24시간)
3. 사용자 역할(ADMIN/PLAYER)이 올바른지 확인
4. 애플리케이션 로그에서 JWT 인증 과정 확인

**디버그 로그 활성화:**

```properties
# application.properties 또는 application-dev.properties에 추가
logging.level.com.mukho.maskedstarcraft.security=DEBUG
```

## 📞 연락처

**개발자**: Mukho  
**GitHub**: [@mukhoplus](https://github.com/mukhoplus)  
**저장소**: [Masked-StarCraft](https://github.com/mukhoplus/Masked-StarCraft)

---

_마지막 업데이트: 2025년 9월 2일 - JWT 인증 디버깅 및 보안 설정 개선_
