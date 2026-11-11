package kr.study.ddobak.auth;

import java.util.Locale;
import kr.study.ddobak.user.User;
import kr.study.ddobak.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Transactional
    public UserResponse signup(SignupRequest req) {
        String email = normalize(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        try {
            User user = userRepository.saveAndFlush(User.local(email, req.nickname().trim(), passwordHasher.hash(req.password())));
            return UserResponse.from(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(); // 거의 동시에 같은 이메일로 가입한 경우
        }
    }

    /** 이메일이 없을 때와 비밀번호가 틀릴 때 같은 오류를 돌려줘서 가입 여부를 알 수 없게 한다 */
    public UserResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.email()))
                .filter((u) -> passwordHasher.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(InvalidLoginException::new);
        return UserResponse.from(user);
    }

    public UserResponse me(Long userId) {
        return userRepository.findById(userId).map(UserResponse::from).orElseThrow(UnauthorizedException::new);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
