package kr.study.ddobak.community;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.study.ddobak.community.CommunityDtos.CommentResponse;
import kr.study.ddobak.community.CommunityDtos.CopyResponse;
import kr.study.ddobak.community.CommunityDtos.LikeResponse;
import kr.study.ddobak.community.CommunityDtos.PostCard;
import kr.study.ddobak.community.CommunityDtos.PostDetail;
import kr.study.ddobak.community.CommunityDtos.PostPage;
import kr.study.ddobak.community.CommunityDtos.SubjectCount;
import kr.study.ddobak.note.Note;
import kr.study.ddobak.note.NoteRepository;
import kr.study.ddobak.user.User;
import kr.study.ddobak.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 커뮤니티 둘러보기·보기·좋아요·가져오기·댓글·신고 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    static final int PAGE_SIZE = 20;
    static final int SUBJECT_LIMIT = 15;
    static final int MAX_QUERY = 50;
    static final long HIDE_AT_REPORTS = 3;
    static final Set<String> REPORT_REASONS = Set.of("inappropriate", "spam", "other");
    static final String DELETED_AUTHOR = "(알 수 없음)";

    private final EntityManager em;
    private final CommunityPostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostCommentRepository commentRepository;
    private final PostReportRepository reportRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    /** 목록: 숨긴 글은 주인에게만. sort = latest | popular */
    public PostPage list(Long me, String sort, String subject, String q, int page) {
        if (page < 0) throw new InvalidCommunityException("페이지 번호가 올바르지 않아요.");
        StringBuilder jpql = new StringBuilder("select p from CommunityPost p where (p.hidden = false or p.userId = :me)");
        String s = subject == null || subject.isBlank() ? null : subject.strip();
        String query = q == null ? null : q.strip();
        if (query != null && query.isEmpty()) query = null;
        if (query != null && query.length() > MAX_QUERY) {
            throw new InvalidCommunityException("검색어는 " + MAX_QUERY + "자까지 쓸 수 있어요.");
        }
        if (s != null) jpql.append(" and p.subject = :subject");
        if (query != null) {
            jpql.append(" and (lower(p.title) like :q escape '!' or lower(p.subject) like :q escape '!'"
                    + " or lower(coalesce(p.intro, '')) like :q escape '!' or lower(coalesce(p.summary, '')) like :q escape '!')");
        }
        jpql.append("popular".equals(sort)
                ? " order by p.likeCount desc, p.publishedAt desc, p.id desc"
                : " order by p.publishedAt desc, p.id desc");
        TypedQuery<CommunityPost> tq = em.createQuery(jpql.toString(), CommunityPost.class).setParameter("me", me);
        if (s != null) tq.setParameter("subject", s);
        if (query != null) tq.setParameter("q", "%" + escapeLike(query.toLowerCase()) + "%");
        List<CommunityPost> rows = tq.setFirstResult(page * PAGE_SIZE).setMaxResults(PAGE_SIZE + 1).getResultList();
        boolean hasMore = rows.size() > PAGE_SIZE;
        List<CommunityPost> posts = hasMore ? rows.subList(0, PAGE_SIZE) : rows;
        Map<Long, String> names = nicknames(posts.stream().map(CommunityPost::getUserId).toList());
        Set<Long> liked = posts.isEmpty() ? Set.of()
                : new HashSet<>(likeRepository.findLikedPostIds(me, posts.stream().map(CommunityPost::getId).toList()));
        List<PostCard> cards = posts.stream().map(p -> new PostCard(p.getId(), p.getTitle(), p.getSubject(), p.getIntro(),
                firstLine(p.getSummary()), p.getThumbnail(), names.getOrDefault(p.getUserId(), DELETED_AUTHOR),
                p.getLikeCount(), p.getCommentCount(), liked.contains(p.getId()), p.getUserId().equals(me), p.isHidden(),
                p.getPublishedAt())).toList();
        return new PostPage(cards, hasMore);
    }

    /** 공개 노트가 있는 과목, 많은 순 (숨긴 글 제외) */
    public List<SubjectCount> subjects() {
        return em.createQuery("""
                        select new kr.study.ddobak.community.CommunityDtos$SubjectCount(p.subject, count(p))
                        from CommunityPost p where p.hidden = false group by p.subject order by count(p) desc, p.subject""",
                        SubjectCount.class)
                .setMaxResults(SUBJECT_LIMIT).getResultList();
    }

    public PostDetail get(Long me, Long postId) {
        CommunityPost p = visible(me, postId);
        String author = nicknames(List.of(p.getUserId())).getOrDefault(p.getUserId(), DELETED_AUTHOR);
        boolean liked = likeRepository.existsById(new PostUserId(postId, me));
        boolean reported = reportRepository.existsById(new PostUserId(postId, me));
        boolean mine = p.getUserId().equals(me);
        return new PostDetail(p.getId(), p.getTitle(), p.getSubject(), p.getIntro(), p.getSummary(), p.getContent(), author,
                p.getLikeCount(), p.getCommentCount(), p.getCopyCount(), liked, mine, p.isHidden(), reported,
                mine ? p.getNoteId() : null, p.getPublishedAt(), p.getUpdatedAt());
    }

    /** 좋아요 켜기/끄기. 여러 번 눌러도 결과가 같다 */
    @Transactional
    public LikeResponse like(Long me, Long postId, boolean on) {
        CommunityPost p = visible(me, postId);
        if (p.getUserId().equals(me)) throw new InvalidCommunityException("내 노트에는 좋아요를 누를 수 없어요.");
        PostUserId id = new PostUserId(postId, me);
        boolean exists = likeRepository.existsById(id);
        if (on && !exists) likeRepository.saveAndFlush(new PostLike(postId, me));
        if (!on && exists) likeRepository.deleteById(id);
        postRepository.recountLikes(postId);
        return new LikeResponse(on, postRepository.findById(postId).orElseThrow().getLikeCount());
    }

    /** 내 노트로 가져오기: 공개본을 복사한 새 노트 (요약은 가져오지 않음) */
    @Transactional
    public CopyResponse copy(Long me, Long postId) {
        CommunityPost p = visible(me, postId);
        String title = p.getTitle() + " (가져옴)";
        if (title.length() > 100) title = p.getTitle();
        Note note = new Note(me, title, p.getContent(), null);
        note.changeThumbnail(p.getThumbnail());
        Long noteId = noteRepository.save(note).getId();
        postRepository.addCopy(postId);
        return new CopyResponse(noteId);
    }

    public List<CommentResponse> comments(Long me, Long postId) {
        CommunityPost p = visible(me, postId);
        List<PostComment> list = commentRepository.findTop200ByPostIdOrderByCreatedAtAscIdAsc(postId);
        Map<Long, String> names = nicknames(list.stream().map(PostComment::getUserId).toList());
        boolean ownerOfPost = p.getUserId().equals(me);
        return list.stream().map(c -> new CommentResponse(c.getId(), names.getOrDefault(c.getUserId(), DELETED_AUTHOR),
                c.getBody(), c.getUserId().equals(me), ownerOfPost || c.getUserId().equals(me), c.getCreatedAt())).toList();
    }

    @Transactional
    public CommentResponse addComment(Long me, Long postId, String body) {
        visible(me, postId);
        String text = body == null ? "" : body.strip();
        if (text.isEmpty()) throw new InvalidCommunityException("댓글 내용을 입력해 주세요.");
        if (text.length() > 500) throw new InvalidCommunityException("댓글은 500자까지 쓸 수 있어요.");
        PostComment c = commentRepository.save(new PostComment(postId, me, text));
        postRepository.recountComments(postId);
        String author = nicknames(List.of(me)).getOrDefault(me, DELETED_AUTHOR);
        return new CommentResponse(c.getId(), author, c.getBody(), true, true, c.getCreatedAt());
    }

    /** 댓글 지우기: 쓴 사람이나 그 노트를 공개한 사람만 */
    @Transactional
    public void deleteComment(Long me, Long postId, Long commentId) {
        CommunityPost p = visible(me, postId);
        PostComment c = commentRepository.findById(commentId).filter(x -> x.getPostId().equals(postId))
                .orElseThrow(PostNotFoundException::new);
        if (!c.getUserId().equals(me) && !p.getUserId().equals(me)) {
            throw new ForbiddenException("내 댓글이나 내 노트에 달린 댓글만 지울 수 있어요.");
        }
        commentRepository.delete(c);
        commentRepository.flush();
        postRepository.recountComments(postId);
    }

    /** 신고: 한 사람 한 번, 3명이면 숨김 */
    @Transactional
    public void report(Long me, Long postId, String reason) {
        CommunityPost p = visible(me, postId);
        if (p.getUserId().equals(me)) throw new InvalidCommunityException("내 노트는 신고할 수 없어요.");
        if (reason == null || !REPORT_REASONS.contains(reason)) throw new InvalidCommunityException("신고 이유를 골라 주세요.");
        PostUserId id = new PostUserId(postId, me);
        if (reportRepository.existsById(id)) throw new AlreadyReportedException();
        reportRepository.saveAndFlush(new PostReport(postId, me, reason));
        postRepository.recountReports(postId, HIDE_AT_REPORTS);
    }

    /** 없거나, 숨겨졌는데 내 글이 아니면 404 */
    private CommunityPost visible(Long me, Long postId) {
        CommunityPost p = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        if (p.isHidden() && !p.getUserId().equals(me)) throw new PostNotFoundException();
        return p;
    }

    private Map<Long, String> nicknames(Collection<Long> userIds) {
        Map<Long, String> names = new HashMap<>();
        if (userIds.isEmpty()) return names;
        for (User u : userRepository.findAllById(new HashSet<>(userIds))) names.put(u.getId(), u.getNickname());
        return names;
    }

    static String firstLine(String summary) {
        if (summary == null) return null;
        for (String line : summary.split("\n")) {
            if (!line.isBlank()) return line.strip();
        }
        return null;
    }

    /** LIKE의 %·_를 글자 그대로 찾게. 이스케이프 글자는 '!' (MySQL에서 '\\'는 문자열 안 이스케이프라 피함) */
    static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
