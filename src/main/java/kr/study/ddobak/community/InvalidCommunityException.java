package kr.study.ddobak.community;

/** 커뮤니티 요청 값이 올바르지 않음 → 400 */
public class InvalidCommunityException extends RuntimeException {
    public InvalidCommunityException(String message) {
        super(message);
    }
}
