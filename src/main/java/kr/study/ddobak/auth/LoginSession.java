package kr.study.ddobak.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** 로그인 상태는 서버 세션에 회원 번호 하나만 둔다 (브라우저에는 세션 쿠키만) */
public final class LoginSession {

    static final String USER_ID = "ddobak.userId";

    private LoginSession() {
    }

    /** 로그인: 세션 번호를 새로 바꿔서(세션 고정 공격 방지) 회원 번호를 넣는다 */
    public static void login(HttpServletRequest request, Long userId) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        request.getSession(true).setAttribute(USER_ID, userId);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static Long userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Long) session.getAttribute(USER_ID);
    }
}
