package kr.study.ddobak.practice;

import java.time.Instant;

/** 연습한 글 하나의 최고 점수와 연습 횟수 (고르기 화면의 세트 카드에 표시) */
public record PracticeBest(String text, Integer best, Long count, Instant lastAt) {
}
