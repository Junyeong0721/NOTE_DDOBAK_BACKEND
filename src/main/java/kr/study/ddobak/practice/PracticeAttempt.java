package kr.study.ddobak.practice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 글씨 연습 한 번의 기록. 표 설계는 db/migration/V2__create_practice_attempt.sql */
@Entity
@Table(name = "practice_attempt", indexes = {
        @Index(name = "idx_practice_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_practice_user_text", columnList = "user_id, target_text")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeAttempt {

    static final String LONGTEXT = "longtext";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주인 회원 번호 (회원을 지우면 기록도 같이 지워진다: fk_practice_user ON DELETE CASCADE) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 미리 넣어 둔 세트 이름. 직접 입력한 글이면 null */
    @Column(name = "set_id", length = 40)
    private String setId;

    /** 연습한 글 */
    @Column(name = "target_text", nullable = false, length = 30)
    private String text;

    @Column(name = "font_id", nullable = false, length = 40)
    private String fontId;

    /** 가이드(연한 목표 글자)를 켜고 썼는지 */
    @Column(nullable = false)
    private boolean guide;

    /** 칸별 획 JSON 배열. 빈칸은 null */
    @Column(nullable = false, columnDefinition = LONGTEXT)
    private String strokes;

    /** 글자별 점수 JSON 배열 */
    @Column(name = "char_scores", nullable = false, columnDefinition = LONGTEXT)
    private String charScores;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "shape_score", nullable = false)
    private int shapeScore;

    @Column(name = "position_score", nullable = false)
    private int positionScore;

    @Column(name = "tilt_score", nullable = false)
    private int tiltScore;

    /** AI 조언. 받지 못했으면 null */
    @Column(length = 300)
    private String advice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    PracticeAttempt(Long userId, String setId, String text, String fontId, boolean guide, String strokes,
            String charScores, int totalScore, int shapeScore, int positionScore, int tiltScore) {
        this.userId = userId;
        this.setId = setId;
        this.text = text;
        this.fontId = fontId;
        this.guide = guide;
        this.strokes = strokes;
        this.charScores = charScores;
        this.totalScore = totalScore;
        this.shapeScore = shapeScore;
        this.positionScore = positionScore;
        this.tiltScore = tiltScore;
    }

    public void attachAdvice(String advice) {
        this.advice = advice;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
