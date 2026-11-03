package kr.study.ddobak.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FolderRequest(
        @NotBlank(message = "폴더 이름을 입력해 주세요.")
        @Size(max = 30, message = "폴더 이름은 30자 이하로 입력해 주세요.")
        String name,

        @NotNull(message = "폴더 색을 골라 주세요.")
        FolderColor color
) {
}
