package kr.study.ddobak.community;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException() {
        super("공개된 노트를 찾을 수 없어요.");
    }
}
