package kr.study.ddobak.ocr;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * API 키가 있으면 Google Cloud Vision, 없으면 가짜 응답을 쓴다.
 * 키는 PC 환경 변수 GOOGLE_VISION_API_KEY에서만 읽는다 (저장소에 넣지 않음).
 */
@Slf4j
@Configuration
public class OcrConfig {

    @Bean
    OcrClient ocrClient(@Value("${ddobak.ocr.google.api-key:}") String apiKey, JsonMapper jsonMapper) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("글자 인식: 가짜 응답 (환경 변수 GOOGLE_VISION_API_KEY 없음)");
            return new FakeOcrClient();
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        log.info("글자 인식: Google Cloud Vision");
        return new GoogleVisionOcrClient(RestClient.builder().requestFactory(factory), jsonMapper, apiKey.trim());
    }
}
