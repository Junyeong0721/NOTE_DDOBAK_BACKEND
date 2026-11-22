package kr.study.ddobak.community;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** 공개본 필기는 길다(longtext), 원본 노트를 지워도 공개본은 남는다(SET NULL), 노트 하나에 공개본 하나 */
class CommunitySchemaTest {

    @Test
    void contentIsLongtext_noteDeleteKeepsPost_userDeleteRemovesAll() throws Exception {
        for (String field : new String[] {"content", "thumbnail"}) {
            Column column = CommunityPost.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(column.columnDefinition()).as(field).isEqualTo("longtext");
        }
        String sql;
        try (var in = getClass().getResourceAsStream("/db/migration/V5__community.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql)
                .containsPattern("content\\s+LONGTEXT\\s+NOT NULL")
                .containsPattern("CONSTRAINT uk_post_note UNIQUE \\(note_id\\)")
                .containsPattern("FOREIGN KEY \\(note_id\\) REFERENCES note \\(id\\) ON DELETE SET NULL")
                .containsPattern("fk_post_user FOREIGN KEY \\(user_id\\) REFERENCES users \\(id\\) ON DELETE CASCADE")
                .containsPattern("hidden\\s+BIT\\(1\\)\\s+NOT NULL DEFAULT 0");
        for (String table : new String[] {"post_like", "post_comment", "post_report"}) {
            assertThat(sql).as(table).contains("CREATE TABLE " + table);
        }
        assertThat(sql.split("ON DELETE CASCADE", -1).length - 1).isGreaterThanOrEqualTo(7);
    }
}
