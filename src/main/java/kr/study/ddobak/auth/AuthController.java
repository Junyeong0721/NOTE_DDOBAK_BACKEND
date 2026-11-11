package kr.study.ddobak.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 가입하면 바로 로그인된다 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest req, HttpServletRequest request) {
        UserResponse user = authService.signup(req);
        LoginSession.login(request, user.id());
        return user;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        UserResponse user = authService.login(req);
        LoginSession.login(request, user.id());
        return user;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        LoginSession.logout(request);
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        Long userId = LoginSession.userId(request);
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return authService.me(userId);
    }
}
