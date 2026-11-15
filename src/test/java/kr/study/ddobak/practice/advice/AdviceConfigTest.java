package kr.study.ddobak.practice.advice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AdviceConfigTest {

    final AdviceConfig config = new AdviceConfig();
    final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void noApiKey_usesFake() {
        assertThat(config.adviceClient("", "m", jsonMapper)).isInstanceOf(FakeAdviceClient.class);
        assertThat(config.adviceClient("   ", "m", jsonMapper)).isInstanceOf(FakeAdviceClient.class);
    }

    @Test
    void apiKey_usesGemini() {
        assertThat(config.adviceClient("some-key", "gemini-test", jsonMapper)).isInstanceOf(GeminiAdviceClient.class);
    }
}
