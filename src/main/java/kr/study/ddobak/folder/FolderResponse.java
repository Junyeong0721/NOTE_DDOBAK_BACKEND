package kr.study.ddobak.folder;

import java.time.Instant;

public record FolderResponse(Long id, String name, FolderColor color, Instant createdAt) {

    static FolderResponse from(Folder folder) {
        return new FolderResponse(folder.getId(), folder.getName(), folder.getColor(), folder.getCreatedAt());
    }
}
