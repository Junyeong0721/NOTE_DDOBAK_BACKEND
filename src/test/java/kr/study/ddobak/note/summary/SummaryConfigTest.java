package kr.study.ddobak.note.summary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SummaryConfigTest {

    @Test
    void usesFakeWithoutAKey_andGeminiWithOne() {
        SummaryConfig config = new SummaryConfig();
        JsonMapper json = JsonMapper.builder().build();
        assertThat(config.summaryClient(" ", "m", json)).isInstanceOf(FakeSummaryClient.class);
        assertThat(config.summaryClient("k", "m", json)).isInstanceOf(GeminiSummaryClient.class);
    }
}
