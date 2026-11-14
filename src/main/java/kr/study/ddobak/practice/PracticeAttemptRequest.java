package kr.study.ddobak.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * 연습 한 판(문제 최대 5개)의 결과. 점수는 앱(브라우저)이 계산해서 보낸다.
 * items: 문제마다 {
 *   text,                                  // 문제 글
 *   cells: [{ ch, strokes } | null],       // 글자마다 쓴 획, 띄어쓰기는 null — 글자 수와 같아야 한다
 *   charScores: [{ total, shape, position, tilt, empty, skipped } | null],
 *   score: { total, shape, position, tilt } // 문제 점수(항목은 % 0~100)
 * }
 * image: AI 조언용 그림(PNG data URL). 없으면 조언 없이 저장만 한다.
 */
public record PracticeAttemptRequest(
        @NotBlank(message = "연습 단계를 골라 주세요.")
        String stage,

        @NotBlank(message = "글씨체를 골라 주세요.")
        @Size(max = 40, message = "글씨체 이름이 너무 깁니다.")
        String fontId,

        @Size(max = 40, message = "글씨체 이름이 너무 깁니다.")
        String fontLabel,

        boolean guide,

        @NotNull(message = "문제가 없습니다.")
        JsonNode items,

        @NotNull(message = "점수가 없습니다.") @Min(value = 0, message = "점수는 0~100이어야 합니다.") @Max(value = 100, message = "점수는 0~100이어야 합니다.")
        Integer totalScore,

        @NotNull(message = "점수가 없습니다.") @Min(value = 0, message = "점수는 0~100이어야 합니다.") @Max(value = 100, message = "점수는 0~100이어야 합니다.")
        Integer shapeScore,

        @NotNull(message = "점수가 없습니다.") @Min(value = 0, message = "점수는 0~100이어야 합니다.") @Max(value = 100, message = "점수는 0~100이어야 합니다.")
        Integer positionScore,

        @NotNull(message = "점수가 없습니다.") @Min(value = 0, message = "점수는 0~100이어야 합니다.") @Max(value = 100, message = "점수는 0~100이어야 합니다.")
        Integer tiltScore,

        String image
) {
}
