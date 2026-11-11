package kr.study.ddobak.folder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과목별 폴더 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주인 회원 번호 (로그인 기능 전에 만든 폴더는 null → 보이지 않음) */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FolderColor color;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Folder(Long userId, String name, FolderColor color) {
        this.userId = userId;
        this.name = name;
        this.color = color;
    }

    public void update(String name, FolderColor color) {
        this.name = name;
        this.color = color;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
