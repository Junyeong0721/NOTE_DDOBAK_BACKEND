package kr.study.ddobak.common;

import kr.study.ddobak.auth.DuplicateEmailException;
import kr.study.ddobak.auth.InvalidLoginException;
import kr.study.ddobak.auth.UnauthorizedException;
import kr.study.ddobak.folder.FolderNotFoundException;
import kr.study.ddobak.note.InvalidContentException;
import kr.study.ddobak.note.InvalidThumbnailException;
import kr.study.ddobak.note.NoteNotFoundException;
import kr.study.ddobak.ocr.InvalidImageException;
import kr.study.ddobak.ocr.OcrFailedException;
import kr.study.ddobak.ocr.OcrLimitExceededException;
import kr.study.ddobak.practice.InvalidPracticeException;
import kr.study.ddobak.practice.PracticeNotFoundException;
import kr.study.ddobak.note.summary.SummaryFailedException;
import kr.study.ddobak.note.summary.SummaryLimitExceededException;
import kr.study.ddobak.community.AlreadyReportedException;
import kr.study.ddobak.community.ForbiddenException;
import kr.study.ddobak.community.InvalidCommunityException;
import kr.study.ddobak.community.PostNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLogin(InvalidLoginException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoteNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFolderNotFound(FolderNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PracticeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePracticeNotFound(PracticeNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidPracticeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPractice(InvalidPracticeException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidThumbnailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidThumbnail(InvalidThumbnailException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidContentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidContent(InvalidContentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(InvalidImageException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(SummaryLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleSummaryLimit(SummaryLimitExceededException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(SummaryFailedException.class)
    public ResponseEntity<ErrorResponse> handleSummaryFailed(SummaryFailedException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "지금은 요약을 만들 수 없어요. 잠시 뒤 다시 해 주세요.");
    }

    @ExceptionHandler(OcrLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleOcrLimit(OcrLimitExceededException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(OcrFailedException.class)
    public ResponseEntity<ErrorResponse> handleOcrFailed(OcrFailedException e) {
        return error(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AlreadyReportedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyReported(AlreadyReportedException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCommunityException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCommunity(InvalidCommunityException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값이 올바르지 않습니다.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** 요청 본문이 JSON이 아니거나 형식이 깨진 경우 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
    }

    /** /api/notes/abc 처럼 id 자리에 숫자가 아닌 값이 온 경우 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "잘못된 번호입니다.");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
