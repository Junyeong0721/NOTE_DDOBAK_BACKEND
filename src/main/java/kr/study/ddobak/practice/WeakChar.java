package kr.study.ddobak.practice;

/**
 * 약점 글자 하나.
 * average: 최근 30일 연습에서 이 글자의 평균 점수(0~100), count: 쓴 횟수,
 * weakest: 세 항목 중 가장 약한 것 (shape 모양 / position 위치·크기 / tilt 기울기)
 */
public record WeakChar(String ch, int average, int count, String weakest) {
}
