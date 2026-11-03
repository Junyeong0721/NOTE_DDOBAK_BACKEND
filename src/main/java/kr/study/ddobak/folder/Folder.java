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

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FolderColor color;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Folder(String name, FolderColor color) {
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
