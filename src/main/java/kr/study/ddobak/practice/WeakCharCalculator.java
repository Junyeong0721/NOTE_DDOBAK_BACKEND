package kr.study.ddobak.practice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 연습 기록으로 약점 글자를 고른다. (노트 보정은 거의 모든 글자에 일어나서 약점 판단에 쓰지 않는다)
 * - 글자마다 연습 점수 평균이 WEAK_BELOW 미만이면 약점, 평균이 낮은 순
 * - 쓰지 않은 칸(empty)과 채점하지 않은 글자(skipped)는 세지 않는다
 * - 최근 RECOVER_COUNT번 평균이 RECOVERED_AT 이상이면 극복한 것으로 보고 뺀다
 */
public final class WeakCharCalculator {

    static final int WEAK_BELOW = 80;
    static final int RECOVER_COUNT = 3;
    static final int RECOVERED_AT = 85;
    static final int MAX_RESULTS = 30;

    private WeakCharCalculator() {
    }

    /** 글자 하나의 점수 기록 (오래된 것부터) */
    private static final class Tally {
        final List<Double> totals = new ArrayList<>();
        double shape; // 각 항목 만점 대비 비율의 합
        double position;
        double tilt;

        void add(JsonNode s) {
            totals.add(s.path("total").asDouble(0));
            shape += s.path("shape").asDouble(0) / 50.0;
            position += s.path("position").asDouble(0) / 25.0;
            tilt += s.path("tilt").asDouble(0) / 25.0;
        }

        double average() {
            return totals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        double recentAverage() {
            return totals.subList(totals.size() - RECOVER_COUNT, totals.size()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);
        }

        String weakest() {
            if (shape <= position && shape <= tilt) return "shape";
            return position <= tilt ? "position" : "tilt";
        }
    }

    /** @param attempts 연습 기록, 오래된 것부터 */
    public static List<WeakChar> calculate(List<PracticeAttempt> attempts, JsonMapper jsonMapper) {
        Map<String, Tally> byChar = new LinkedHashMap<>();
        for (PracticeAttempt attempt : attempts) {
            for (JsonNode item : jsonMapper.readTree(attempt.getStrokes())) {
                int[] chars = item.path("text").asString("").codePoints().toArray();
                JsonNode scores = item.path("charScores");
                for (int i = 0; i < chars.length && i < scores.size(); i++) {
                    JsonNode s = scores.get(i);
                    if (s == null || !s.isObject() || s.path("empty").asBoolean(false) || s.path("skipped").asBoolean(false)) {
                        continue;
                    }
                    byChar.computeIfAbsent(new String(Character.toChars(chars[i])), k -> new Tally()).add(s);
                }
            }
        }
        List<WeakChar> weak = new ArrayList<>();
        byChar.forEach((ch, t) -> {
            double avg = t.average();
            if (avg >= WEAK_BELOW) return;
            if (t.totals.size() >= RECOVER_COUNT && t.recentAverage() >= RECOVERED_AT) return;
            weak.add(new WeakChar(ch, (int) Math.round(avg), t.totals.size(), t.weakest()));
        });
        weak.sort(Comparator.comparingInt(WeakChar::average).thenComparing(Comparator.comparingInt(WeakChar::count).reversed())
                .thenComparing(WeakChar::ch));
        return weak.size() > MAX_RESULTS ? List.copyOf(weak.subList(0, MAX_RESULTS)) : weak;
    }
}
