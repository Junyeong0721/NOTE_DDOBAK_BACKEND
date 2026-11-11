package kr.study.ddobak.auth;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("로그인이 필요합니다.");
    }
}
