const QUESTION_STATUS_LABEL = {
  DRAFT: 'Bản nháp',
  UNDER_REVIEW: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  SUPERSEDED: 'Đã thay thế',
  ARCHIVED: 'Đã lưu trữ',
  ACTIVE: 'Đang dùng'
};

export const questionStatusLabel = (status) => QUESTION_STATUS_LABEL[status] || status || '—';
