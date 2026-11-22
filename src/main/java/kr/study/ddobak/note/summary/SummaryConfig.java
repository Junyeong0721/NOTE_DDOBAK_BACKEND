package kr.study.ddobak.note.summary;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * 연습 조언과 같은 Gemini 키(ddobak.advice.gemini.api-key)를 쓴다. 키가 없으면 가짜 요약.
 * 페이지가 여러 장이라 조언보다 응답이 오래 걸릴 수 있어 읽기 제한을 30초로 둔다.
 */
@Slf4j
@Configuration
public class SummaryConfig {

    @Bean
    SummaryClient summaryClient(
            @Value("${ddobak.advice.gemini.api-key:}") String apiKey,
            @Value("${ddobak.summary.model:${ddobak.advice.gemini.model:gemini-3.5-flash-lite}}") String model,
            JsonMapper jsonMapper) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("노트 요약: 가짜 요약 (secret.yaml이나 환경 변수에 Gemini API 키 없음)");
            return new FakeSummaryClient();
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        log.info("노트 요약: Google Gemini ({})", model.trim());
        return new GeminiSummaryClient(RestClient.builder().requestFactory(factory), jsonMapper, apiKey.trim(), model.trim());
    }
}
