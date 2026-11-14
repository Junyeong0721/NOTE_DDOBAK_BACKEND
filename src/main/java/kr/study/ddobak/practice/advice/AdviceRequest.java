package kr.study.ddobak.practice.advice;

import java.util.List;

/**
 * 조언에 필요한 것: 목표 글씨체로 그린 글자와 내 글씨를 위아래로 맞춘 그림(PNG) + 앱이 계산한 글자별 점수.
 * 회원 정보(이메일·닉네임)는 넣지 않는다.
 */
public record AdviceRequest(String fontLabel, String text, boolean guide, List<CharScore> scores, byte[] png) {

    /** 글자 하나의 점수. 모양 50 + 위치·크기 25 + 기울기 25 = total 100. empty = 안 쓴 칸 */
    public record CharScore(String ch, int total, int shape, int position, int tilt, boolean empty) {
    }
}
