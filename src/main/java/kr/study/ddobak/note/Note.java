package kr.study.ddobak.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
/** 표 설계는 db/migration/V1__create_tables.sql (Flyway). 여기 이름·길이는 그 SQL과 맞춰 둔다 */
@Table(indexes = @Index(name = "idx_note_user_updated", columnList = "user_id, updated_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note {

    /** 큰 글자 칸. 길이를 안 정하면 MySQL에서 255바이트(tinytext)가 되므로 명시한다 */
    static final String LONGTEXT = "longtext";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주인 회원 번호 (회원을 지우면 노트도 같이 지워진다: fk_note_user ON DELETE CASCADE) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    /** 프론트가 만든 필기 데이터(JSON 문자열). 획이 많으면 수 MB → MySQL longtext (V1__create_tables.sql) */
    @Column(nullable = false, columnDefinition = Note.LONGTEXT)
    private String content;

    /** 폴더 id. null이면 미분류 */
    @Column(name = "folder_id")
    private Long folderId;

    /** 목록용 첫 페이지 미리보기 (PNG data URL) */
    @Column(columnDefinition = Note.LONGTEXT)
    private String thumbnail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Note(Long userId, String title, String content, Long folderId) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.folderId = folderId;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void changeThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void moveToFolder(Long folderId) {
        this.folderId = folderId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
