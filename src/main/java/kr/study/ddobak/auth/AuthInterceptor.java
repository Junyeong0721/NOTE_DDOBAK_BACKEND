package kr.study.ddobak.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/auth 를 뺀 모든 /api 요청은 로그인해야 한다. 회원 번호를 요청에 실어 둔다 */
public class AuthInterceptor implements HandlerInterceptor {

    static final String ATTR = "ddobak.loginUserId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = LoginSession.userId(request);
        if (userId == null) {
            throw new UnauthorizedException();
        }
        request.setAttribute(ATTR, userId);
        return true;
    }
}
