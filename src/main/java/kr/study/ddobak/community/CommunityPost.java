package kr.study.ddobak.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import kr.study.ddobak.note.Note;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 커뮤니티에 공개한 노트. 공개한 순간의 필기 복사본을 들고 있다. 표 설계는 db/migration/V5__community.sql */
@Entity
@Table(name = "community_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    static final String LONGTEXT = "longtext";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 원본 노트. 노트를 지우면 DB가 NULL로 바꾼다 */
    @Column(name = "note_id")
    private Long noteId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 30)
    private String subject;

    @Column(length = 200)
    private String intro;

    @Column(length = 1000)
    private String summary;

    @Column(nullable = false, columnDefinition = LONGTEXT)
    private String content;

    @Column(columnDefinition = LONGTEXT)
    private String thumbnail;

    // 개수는 CommunityPostRepository의 recount 쿼리로만 바꾼다 (엔티티에서 고치지 않음)
    @Column(name = "like_count", nullable = false, insertable = false, updatable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
    private int commentCount;

    @Column(name = "copy_count", nullable = false, insertable = false, updatable = false)
    private int copyCount;

    @Column(name = "report_count", nullable = false, insertable = false, updatable = false)
    private int reportCount;

    @Column(nullable = false, insertable = false, updatable = false)
    private boolean hidden;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "content_at", nullable = false)
    private Instant contentAt;

    static CommunityPost publish(Note note, String title, String subject, String intro) {
        CommunityPost p = new CommunityPost();
        p.userId = note.getUserId();
        p.noteId = note.getId();
        p.title = title;
        p.subject = subject;
        p.intro = intro;
        Instant now = now();
        p.publishedAt = now;
        p.copyFrom(note, now);
        return p;
    }

    void updateInfo(String title, String subject, String intro) {
        this.title = title;
        this.subject = subject;
        this.intro = intro;
        this.updatedAt = now();
    }

    /** [공개본 업데이트]: 지금 노트의 필기·미리보기·요약으로 바꾼다. 좋아요·댓글은 그대로 */
    void refreshContent(Note note) {
        copyFrom(note, now());
    }

    private void copyFrom(Note note, Instant at) {
        this.content = note.getContent();
        this.thumbnail = note.getThumbnail();
        this.summary = note.getSummary();
        this.contentAt = at;
        this.updatedAt = at;
    }

    /** 원본 노트가 공개본을 만든 뒤에 바뀌었는지 */
    boolean outdatedBy(Note note) {
        return note.getUpdatedAt() != null && note.getUpdatedAt().isAfter(contentAt);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
