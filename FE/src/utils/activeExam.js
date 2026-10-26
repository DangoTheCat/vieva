// The exam a student entered from DiscoverPage, carried through calibration into the exam room.
// sessionStorage keeps it across reloads of the same tab without leaking into other tabs.
const KEY = 'aives_active_exam';

export function getActiveExam() {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (_) {
    return null;
  }
}

/** @param {{ examId: string, attemptId: string, title?: string, durationSeconds?: number }} exam */
export function setActiveExam(exam) {
  sessionStorage.setItem(KEY, JSON.stringify(exam));
}

export function clearActiveExam() {
  sessionStorage.removeItem(KEY);
}

const GRADE_LABELS = {
  EXCELLENT: 'Xuất sắc',
  GOOD: 'Giỏi',
  AVERAGE: 'Khá',
  POOR: 'Chưa đạt'
};

export function gradeLabel(gradeLevel) {
  return GRADE_LABELS[gradeLevel] || gradeLevel || '—';
}
