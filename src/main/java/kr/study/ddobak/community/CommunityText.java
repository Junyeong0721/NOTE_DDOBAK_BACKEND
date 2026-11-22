package kr.study.ddobak.community;

/** 제목·과목·소개·댓글 글자 검사 (글자 수는 String.length — 화면 maxlength·테스트 DB와 같은 기준, 이모지는 2자) */
final class CommunityText {

    private CommunityText() {
    }

    static String required(String value, int max, String emptyMessage, String tooLongMessage) {
        String v = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (v.isEmpty()) throw new InvalidCommunityException(emptyMessage);
        if (v.length() > max) throw new InvalidCommunityException(tooLongMessage);
        return v;
    }

    static String optional(String value, int max, String tooLongMessage) {
        String v = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        if (v.isEmpty()) return null;
        if (v.length() > max) throw new InvalidCommunityException(tooLongMessage);
        return v;
    }
}
