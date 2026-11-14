package kr.study.ddobak.practice;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * 획 JSON 칸이 MySQL에서 255바이트(tinytext)로 만들어지면 저장이 실패한다.
 * 엔티티와 표 설계 SQL 둘 다 longtext인지, 회원을 지우면 기록도 지워지게 돼 있는지 지킨다.
 * (엔티티와 SQL이 맞는지는 모든 테스트가 validate로 확인)
 */
class PracticeSchemaTest {

    @Test
    void strokesAndScores_areLongtext_andCascadeOnUserDelete() throws Exception {
        for (String field : new String[] {"strokes", "charScores"}) {
            Column column = PracticeAttempt.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(column.columnDefinition()).as(field).isEqualTo("longtext");
        }
        String sql;
        try (var in = getClass().getResourceAsStream("/db/migration/V2__create_practice_attempt.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql)
                .containsPattern("strokes\\s+LONGTEXT\\s+NOT NULL")
                .containsPattern("char_scores\\s+LONGTEXT\\s+NOT NULL")
                .containsPattern("target_text\\s+VARCHAR\\(30\\)\\s+NOT NULL")
                .containsPattern("fk_practice_user FOREIGN KEY \\(user_id\\) REFERENCES users \\(id\\) ON DELETE CASCADE");
    }
}
