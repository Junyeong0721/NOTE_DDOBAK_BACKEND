package kr.study.ddobak.auth;

import kr.study.ddobak.user.AuthProvider;
import kr.study.ddobak.user.User;

public record UserResponse(Long id, String email, String nickname, AuthProvider provider) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getProvider());
    }
}
