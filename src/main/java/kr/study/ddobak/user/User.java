package kr.study.ddobak.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원. 테이블 이름 user는 H2·MySQL 예약어라 users로 쓴다 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_provider", columnNames = {"provider", "provider_id"}),
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소문자로 정리해서 저장. 소셜 로그인은 이메일을 안 줄 수도 있어 null 허용 */
    @Column(length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String nickname;

    /** PasswordHasher 형식의 해시. 소셜 가입은 null (원문 비밀번호는 어디에도 저장하지 않는다) */
    @Column(length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @JdbcTypeCode(SqlTypes.VARCHAR) // MySQL enum 대신 글자로: 나중에 값을 추가해도 표를 고칠 필요가 없다
    private AuthProvider provider;

    /** 소셜 로그인 서비스가 준 회원 번호 */
    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static User local(String email, String nickname, String passwordHash) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.passwordHash = passwordHash;
        user.provider = AuthProvider.LOCAL;
        return user;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
