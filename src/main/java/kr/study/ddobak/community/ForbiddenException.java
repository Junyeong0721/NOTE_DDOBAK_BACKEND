package kr.study.ddobak.community;

/** 할 수 없는 일 (남의 댓글 지우기 등) → 403 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
