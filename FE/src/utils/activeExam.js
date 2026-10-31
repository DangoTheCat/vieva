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
  // BE bands: EXCELLENT >= 8.5, GOOD 7.0–8.5, AVERAGE 5.0–7.0, POOR < 5.0
  GOOD: 'Khá',
  AVERAGE: 'Trung bình',
  POOR: 'Chưa đạt'
};

export function gradeLabel(gradeLevel) {
  return GRADE_LABELS[gradeLevel] || gradeLevel || '—';
}

// BE TurnType is MAIN_QUESTION | FOLLOW_UP; 'PROBING' kept for older payloads
export function isProbeTurn(turnType) {
  return turnType === 'FOLLOW_UP' || turnType === 'PROBING';
}
