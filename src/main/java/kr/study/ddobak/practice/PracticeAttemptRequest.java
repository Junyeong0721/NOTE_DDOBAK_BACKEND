package kr.study.ddobak.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * 연습 한 세트의 결과. 점수는 앱(브라우저)이 계산해서 보낸다.
 * cells: 글자마다 { ch, strokes } 또는 null(띄어쓰기) — 글자 수와 같아야 한다.
 * charScores: 글자마다 { total, shape, position, tilt, empty, skipped } 또는 null.
 * image: AI 조언용 그림(PNG data URL). 없으면 조언 없이 저장만 한다.
 */
public record PracticeAttemptRequest(
        @Size(max = 40, message = "세트 이름이 너무 깁니다.")
        String setId,

        @NotBlank(message = "연습할 글을 입력해 주세요.")
        String text,

        @NotBlank(message = "글씨체를 골라 주세요.")
        @Size(max = 40, message = "글씨체 이름이 너무 깁니다.")
        String fontId,

        @Size(max = 40, message = "글씨체 이름이 너무 깁니다.")
        String fontLabel,

        boolean guide,

        @NotNull(message = "쓴 글씨가 없습니다.")
        JsonNode cells,

        @NotNull(message = "글자별 점수가 없습니다.")
        JsonNode charScores,

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
