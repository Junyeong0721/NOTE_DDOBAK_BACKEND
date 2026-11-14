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

/** 글씨 연습 한 판(랜덤 문제 5개)의 기록. 표 설계는 db/migration/V2__create_practice_attempt.sql + V3__practice_rounds.sql */
@Entity
@Table(name = "practice_attempt", indexes = {
        @Index(name = "idx_practice_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_practice_user_stage", columnList = "user_id, stage")})
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

    /** 단계 id: syllable / word / sentence */
    @Column(nullable = false, length = 20)
    private String stage;

    /** 한 판에 나온 문제들 (줄바꿈으로 이어 붙임) */
    @Column(name = "target_text", nullable = false, length = 300)
    private String text;

    @Column(name = "font_id", nullable = false, length = 40)
    private String fontId;

    /** 가이드(연한 목표 글자)를 켜고 썼는지 */
    @Column(nullable = false)
    private boolean guide;

    /** 문제별 { text, cells, charScores, score } JSON 배열 (다시 보기용) */
    @Column(nullable = false, columnDefinition = LONGTEXT)
    private String strokes;

    /** 문제별 점수 JSON 배열 */
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
    PracticeAttempt(Long userId, String stage, String text, String fontId, boolean guide, String strokes,
            String charScores, int totalScore, int shapeScore, int positionScore, int tiltScore) {
        this.userId = userId;
        this.stage = stage;
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
