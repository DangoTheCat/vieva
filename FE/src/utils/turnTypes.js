/**
 * Turn types returned by the BE (TurnType enum): MAIN_QUESTION and FOLLOW_UP.
 * 'PROBING' / 'MAIN' are legacy FE spellings kept readable for old data.
 */
export const TURN_TYPE = Object.freeze({
  MAIN: 'MAIN_QUESTION',
  FOLLOW_UP: 'FOLLOW_UP'
});

export const isFollowUp = (turnType) => turnType === TURN_TYPE.FOLLOW_UP || turnType === 'PROBING';

/** Student-facing labels: plain Vietnamese, no technical jargon. */
export const turnBadge = (turnType) => (isFollowUp(turnType) ? 'Câu hỏi làm rõ' : 'Câu hỏi chính');

export const turnTitle = (turnType, order) =>
  `Câu ${order}: ${isFollowUp(turnType) ? 'Câu hỏi làm rõ' : 'Câu hỏi chính'}`;
