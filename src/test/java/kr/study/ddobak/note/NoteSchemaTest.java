package kr.study.ddobak.note;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * MySQL에서 필기 데이터 칸이 255바이트(tinytext)로 만들어지면 노트 저장이 실패한다.
 * 엔티티와 표 설계 SQL 둘 다 longtext인지 지킨다. (엔티티와 SQL이 맞는지는 모든 테스트가 validate로 확인)
 */
class NoteSchemaTest {

    @Test
    void contentAndThumbnail_areLongtext_inEntityAndMigration() throws Exception {
        for (String field : new String[] {"content", "thumbnail"}) {
            Column column = Note.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(column.columnDefinition()).as(field).isEqualTo("longtext");
        }
        String sql;
        try (var in = getClass().getResourceAsStream("/db/migration/V1__create_tables.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).containsPattern("content\\s+LONGTEXT\\s+NOT NULL").containsPattern("thumbnail\\s+LONGTEXT");
    }
}
