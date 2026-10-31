package kr.study.ddobak.ocr;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** imageBase64: "data:" 접두어 없는 PNG Base64 */
public record OcrLineRequest(
        @NotBlank(message = "이미지가 비어 있습니다.")
        String imageBase64,

        @Min(value = 1, message = "이미지 크기가 올바르지 않습니다.")
        @Max(value = 4096, message = "이미지 크기가 올바르지 않습니다.")
        int width,

        @Min(value = 1, message = "이미지 크기가 올바르지 않습니다.")
        @Max(value = 4096, message = "이미지 크기가 올바르지 않습니다.")
        int height
) {
}
