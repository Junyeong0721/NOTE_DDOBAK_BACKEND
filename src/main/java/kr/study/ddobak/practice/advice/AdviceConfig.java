package kr.study.ddobak.practice.advice;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * API 키가 있으면 Google Gemini, 없으면 가짜 조언을 쓴다.
 * 키는 백엔드 폴더의 secret.yaml(.gitignore) 또는 PC 환경 변수 GEMINI_API_KEY에서 읽는다 (저장소에 넣지 않음).
 */
@Slf4j
@Configuration
public class AdviceConfig {

    @Bean
    AdviceClient adviceClient(
            @Value("${ddobak.advice.gemini.api-key:}") String apiKey,
            @Value("${ddobak.advice.gemini.model:gemini-3.5-flash-lite}") String model,
            JsonMapper jsonMapper) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("연습 조언: 가짜 조언 (secret.yaml이나 환경 변수에 Gemini API 키 없음)");
            return new FakeAdviceClient();
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10)); // 이보다 오래 걸리면 "지금은 조언을 할 수 없어요"
        log.info("연습 조언: Google Gemini ({})", model.trim());
        return new GeminiAdviceClient(RestClient.builder().requestFactory(factory), jsonMapper, apiKey.trim(), model.trim());
    }
}
