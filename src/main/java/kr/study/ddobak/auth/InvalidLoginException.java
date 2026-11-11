package kr.study.ddobak.auth;

public class InvalidLoginException extends RuntimeException {

    public InvalidLoginException() {
        super("이메일 또는 비밀번호가 맞지 않습니다.");
    }
}
