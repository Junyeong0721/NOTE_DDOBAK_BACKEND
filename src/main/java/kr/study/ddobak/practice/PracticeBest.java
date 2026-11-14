package kr.study.ddobak.practice;

import java.time.Instant;

/** 단계 하나의 최고 점수와 연습한 판 수 (고르기 화면의 단계 카드에 표시) */
public record PracticeBest(String stage, Integer best, Long count, Instant lastAt) {
}
