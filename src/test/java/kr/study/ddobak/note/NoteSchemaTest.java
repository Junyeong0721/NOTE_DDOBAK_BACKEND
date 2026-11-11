package kr.study.ddobak.note;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

/**
 * MySQL에서 필기 데이터 칸이 255바이트(tinytext)로 만들어지면 노트 저장이 실패한다.
 * 칸 길이가 16MB(mediumtext 한계)를 넘어 longtext가 되도록 지정돼 있는지 지킨다.
 */
class NoteSchemaTest {

    @Test
    void contentAndThumbnail_becomeLongtextInMysql() throws Exception {
        for (String field : new String[] {"content", "thumbnail"}) {
            Column column = Note.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(column).as(field).isNotNull();
            assertThat(column.length()).as(field).isGreaterThan(16_777_215);
        }
    }
}
