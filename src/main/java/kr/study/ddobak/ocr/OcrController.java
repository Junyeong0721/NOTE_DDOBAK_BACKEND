package kr.study.ddobak.ocr;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final OcrClient ocrClient;

    @PostMapping("/line")
    public OcrResult recognizeLine(@Valid @RequestBody OcrLineRequest req) {
        byte[] png = decode(req.imageBase64());
        if (png.length > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("이미지가 너무 큽니다.");
        }
        if (png.length < PNG_SIGNATURE.length
                || !Arrays.equals(Arrays.copyOf(png, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            throw new InvalidImageException("이미지 형식이 올바르지 않습니다.");
        }
        return ocrClient.recognize(png, req.width(), req.height());
    }

    private byte[] decode(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new InvalidImageException("이미지 형식이 올바르지 않습니다.");
        }
    }
}
