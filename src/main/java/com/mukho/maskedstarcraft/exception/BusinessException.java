package com.mukho.maskedstarcraft.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException(String nickname) {
        super("이미 존재하는 닉네임입니다: " + nickname);
    }
}

class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super("잘못된 비밀번호입니다");
    }
}

class TournamentNotFoundException extends BusinessException {
    public TournamentNotFoundException() {
        super("진행 중인 대회가 없습니다");
    }
}

class TournamentAlreadyInProgressException extends BusinessException {
    public TournamentAlreadyInProgressException() {
        super("이미 진행 중인 대회가 있습니다");
    }
}

class InsufficientPlayersException extends BusinessException {
    public InsufficientPlayersException() {
        super("대회를 시작하려면 최소 2명의 참가자가 필요합니다");
    }
}

class InsufficientMapsException extends BusinessException {
    public InsufficientMapsException() {
        super("대회를 시작하려면 최소 1개의 맵이 필요합니다");
    }
}

class MapAlreadyExistsException extends BusinessException {
    public MapAlreadyExistsException(String mapName) {
        super("이미 존재하는 맵 이름입니다: " + mapName);
    }
}

class MapNotFoundException extends BusinessException {
    public MapNotFoundException() {
        super("맵을 찾을 수 없습니다");
    }
}

class InvalidGameResultException extends BusinessException {
    public InvalidGameResultException(String message) {
        super(message);
    }
}
