/**
 * Pure helpers of the oral exam room (no React state): turn mapping, result shaping, time format.
 */
import { gradeLabel } from '../../utils/activeExam';
import { TURN_TYPE, isFollowUp, turnBadge, turnTitle } from '../../utils/turnTypes';

// Placeholder rendered until the attempt's first turn arrives from the BE
export const EMPTY_QUESTION = { content: '', title: 'Đang tải câu hỏi...', badge: '', type: TURN_TYPE.MAIN };

/** Text recorded when the answer time runs out with nothing said, so the turn can still be graded. */
export const NO_ANSWER_TEXT = '[Thí sinh không trả lời trong thời gian quy định]';

/**
 * Maps backend ExamTurnDetailResponse list to frontend Question structure
 */
export const mapTurnsToQuestions = (turns, totalQuestions) => {
  if (!Array.isArray(turns) || turns.length === 0) return null;
  // Number main questions on their own; a follow-up belongs to the main question before it
  let mainNumber = 0;
  return turns.map((t, idx) => {
    if (!isFollowUp(t.turnType)) mainNumber += 1;
    const number = Math.max(mainNumber, 1);
    return {
      id: t.questionId || idx + 1,
      turnId: t.turnId,
      questionNumber: number,
      totalQuestions: totalQuestions || turns.length,
      type: t.turnType || TURN_TYPE.MAIN,
      badge: turnBadge(t.turnType),
      title: turnTitle(t.turnType, number),
      content: t.questionText || '',
      ttsAudioUrl: t.ttsAudioUrl,
      hint: isFollowUp(t.turnType) ? 'Giải thích rõ hơn ý bạn vừa trả lời.' : '',
      rubricCriteria: t.rubricEvaluations || 'Đánh giá năng lực chuyên môn và phản xạ kiến trúc.'
    };
  });
};

/** Main-question number for a turn added at {@code index}: a follow-up keeps its main question's number. */
export const mainNumberFor = (questions, index, followUp) => {
  const mainsBefore = questions.slice(0, index).filter((q) => !isFollowUp(q.type)).length;
  return followUp ? Math.max(mainsBefore, 1) : mainsBefore + 1;
};

/**
 * Maps backend turns to per-turn rubric results
 */
export const mapRubricsFromTurns = (turns) => {
  if (Array.isArray(turns) && turns.length > 0) {
    const list = [];
    turns.forEach((t, i) => {
      if (t.rubricEvaluations) {
        try {
          const parsed = JSON.parse(t.rubricEvaluations);
          if (Array.isArray(parsed) && parsed.length > 0) {
            list.push(...parsed);
            return;
          } else if (parsed && parsed.name) {
            list.push(parsed);
            return;
          }
        } catch (e) {}
      }
      list.push({
        name: `Lượt ${t.turnOrder || i + 1}: ${turnBadge(t.turnType)}`,
        score: t.score != null ? Number(t.score) : null,
        max: 10,
        comment: t.feedback || ''
      });
    });
    return list;
  }
  return [];
};

/** Builds the COMPLETED-screen result from the BE attempt detail; score stays null while grading is pending */
export const toExamResult = (detail) => ({
  totalScore: detail?.totalScore != null ? Number(detail.totalScore) : null,
  grade: gradeLabel(detail?.gradeLevel),
  maxScore: 10,
  feedback: detail?.detailedFeedback || '',
  rubrics: mapRubricsFromTurns(detail?.turns)
});

/** Formats seconds as mm:ss. */
export const formatTime = (secs) => {
  const safe = Math.max(0, Number(secs) || 0);
  const m = Math.floor(safe / 60).toString().padStart(2, '0');
  const sec = (safe % 60).toString().padStart(2, '0');
  return `${m}:${sec}`;
};
