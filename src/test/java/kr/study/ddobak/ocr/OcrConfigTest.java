package kr.study.ddobak.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OcrConfigTest {

    final OcrConfig config = new OcrConfig();
    final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void noApiKey_usesFake() {
        assertThat(config.ocrClient("", jsonMapper)).isInstanceOf(FakeOcrClient.class);
        assertThat(config.ocrClient("   ", jsonMapper)).isInstanceOf(FakeOcrClient.class);
    }

    @Test
    void apiKey_usesGoogleVision() {
        assertThat(config.ocrClient("some-key", jsonMapper)).isInstanceOf(GoogleVisionOcrClient.class);
    }
}
