package kr.study.ddobak.note;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
        @Index(name = "idx_note_folder", columnList = "folder_id"),
        @Index(name = "idx_note_user", columnList = "user_id"),
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주인 회원 번호. 로그인 기능 전에 만든 노트는 주인이 없어(null) 아무에게도 보이지 않는다.
     * (예전 데이터가 있는 DB에서도 서버가 켜지도록 DB에서는 null을 허용)
     */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    /** 프론트가 만든 필기 데이터(JSON 문자열). 획이 많으면 커지므로 길이 제한 없이 저장한다. */
    @Lob
    @Column(nullable = false)
    private String content;

    /** 폴더 id. null이면 미분류 */
    @Column(name = "folder_id")
    private Long folderId;

    /** 목록용 첫 페이지 미리보기 (PNG data URL) */
    @Lob
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
