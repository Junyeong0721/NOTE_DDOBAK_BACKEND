package kr.study.ddobak.community;

import kr.study.ddobak.note.Note;
import kr.study.ddobak.note.NoteNotFoundException;
import kr.study.ddobak.note.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 노트 공개하기·정보 수정·공개본 업데이트·공개 그만하기 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublishService {

    private final NoteRepository noteRepository;
    private final CommunityPostRepository postRepository;
    private final JsonMapper jsonMapper;

    public PublishStatus status(Long userId, Long noteId) {
        Note note = findNote(userId, noteId);
        return postRepository.findByNoteId(noteId).map(p -> toStatus(p, note)).orElseGet(PublishStatus::none);
    }

    @Transactional
    public PublishStatus publish(Long userId, Long noteId, PublishRequest req) {
        Note note = findNote(userId, noteId);
        String title = CommunityText.required(req.title(), 100, "제목을 입력해 주세요.", "제목은 100자까지 쓸 수 있어요.");
        String subject = CommunityText.required(req.subject(), 30, "과목을 입력해 주세요.", "과목은 30자까지 쓸 수 있어요.");
        String intro = CommunityText.optional(req.intro(), 200, "한 줄 소개는 200자까지 쓸 수 있어요.");
        CommunityPost post = postRepository.findByNoteId(noteId).orElse(null);
        if (post == null) {
            requireWriting(note);
            post = postRepository.save(CommunityPost.publish(note, title, subject, intro));
        } else {
            post.updateInfo(title, subject, intro);
            if (req.refreshContent()) {
                requireWriting(note);
                post.refreshContent(note);
            }
            postRepository.flush();
        }
        return toStatus(post, note);
    }

    @Transactional
    public void unpublish(Long userId, Long noteId) {
        findNote(userId, noteId);
        postRepository.findByNoteId(noteId).ifPresent(postRepository::delete);
    }

    private Note findNote(Long userId, Long noteId) {
        return noteRepository.findByIdAndUserId(noteId, userId).orElseThrow(() -> new NoteNotFoundException(noteId));
    }

    /** 획이 하나도 없는 노트는 공개하지 않는다 */
    private void requireWriting(Note note) {
        boolean any = false;
        try {
            for (JsonNode page : jsonMapper.readTree(note.getContent()).path("pages")) {
                if (!page.path("strokes").isEmpty()) {
                    any = true;
                    break;
                }
            }
        } catch (JacksonException e) {
            any = false;
        }
        if (!any) throw new InvalidCommunityException("필기가 있어야 공개할 수 있어요.");
    }

    private static PublishStatus toStatus(CommunityPost p, Note note) {
        return new PublishStatus(true, p.getId(), p.getTitle(), p.getSubject(), p.getIntro(), p.getLikeCount(),
                p.getCommentCount(), p.getCopyCount(), p.isHidden(), p.outdatedBy(note), p.getPublishedAt(), p.getUpdatedAt());
    }
}
