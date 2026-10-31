package kr.study.ddobak.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "필기 데이터가 비어 있습니다.")
        String content
) {
}
