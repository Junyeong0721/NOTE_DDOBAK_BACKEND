package kr.study.ddobak.user;

/** 가입 방식. LOCAL = 이메일·비밀번호, 나머지는 소셜 로그인(2단계에서 연결) */
public enum AuthProvider {
    LOCAL, GOOGLE, KAKAO
}
