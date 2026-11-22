package kr.study.ddobak.community;

public class AlreadyReportedException extends RuntimeException {
    public AlreadyReportedException() {
        super("이미 신고한 노트예요.");
    }
}
