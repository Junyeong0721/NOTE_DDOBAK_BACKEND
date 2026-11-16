package kr.study.ddobak.common;

import java.util.Arrays;
import java.util.Base64;

/** 브라우저가 보낸 PNG 그림(data URL)을 바이트로. AI에게 보내는 그림(연습 조언, 노트 요약)에 쓴다 */
public final class PngDataUrl {

    public static final String PREFIX = "data:image/png;base64,";
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private PngDataUrl() {
    }

    /**
     * @return PNG 바이트
     * @throws IllegalArgumentException PNG data URL이 아니면 "그림 형식이 올바르지 않습니다.", 너무 크면 "그림이 너무 큽니다."
     */
    public static byte[] decode(String dataUrl, int maxBytes) {
        if (dataUrl == null || !dataUrl.startsWith(PREFIX)) {
            throw new IllegalArgumentException("그림 형식이 올바르지 않습니다.");
        }
        byte[] png;
        try {
            png = Base64.getDecoder().decode(dataUrl.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("그림 형식이 올바르지 않습니다.");
        }
        if (png.length > maxBytes) {
            throw new IllegalArgumentException("그림이 너무 큽니다.");
        }
        if (png.length < SIGNATURE.length || !Arrays.equals(Arrays.copyOf(png, SIGNATURE.length), SIGNATURE)) {
            throw new IllegalArgumentException("그림 형식이 올바르지 않습니다.");
        }
        return png;
    }
}
