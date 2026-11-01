-- ============================================================================
-- V27: Seed citation quotes must be verbatim excerpts of their source chunk
-- V10 seeded shortened quotes ending in "...", which QuestionApprovalPolicy rejects
-- (INVALID_CITATION_QUOTE) when a seed draft is submitted for review.
-- Only rows still holding the original seed text are touched (copies included).
-- ============================================================================

UPDATE question_sources
SET citation_quote = 'Onion Architecture đặt Domain Entity và Enterprise Business Rules ở trung tâm lõi. Mọi luồng phụ thuộc mã nguồn bắt buộc phải hướng tâm (Inward Dependency).'
WHERE chunk_id = 'f1000000-0000-0000-0000-000000000001'
  AND citation_quote = 'Onion Architecture đặt Domain Entity và Enterprise Business Rules ở trung tâm lõi... phụ thuộc vào Interface Ports định nghĩa tại Application Core theo Dependency Inversion Principle.';

UPDATE question_sources
SET citation_quote = 'Transactional Outbox Pattern giải quyết bài toán Dual-Write và đảm bảo At-Least-Once Delivery trong hệ thống phân tán.'
WHERE chunk_id = 'f1000000-0000-0000-0000-000000000002'
  AND citation_quote = 'Transactional Outbox Pattern giải quyết bài toán Dual-Write và đảm bảo At-Least-Once Delivery trong hệ thống phân tán...';

UPDATE question_sources
SET citation_quote = 'Trong trường hợp thí sinh bị rớt mạng do sự cố kết nối, hệ thống kích hoạt cửa sổ cứu vớt (Reconnect Recovery Window) 60 giây.'
WHERE chunk_id = 'f1000000-0000-0000-0000-000000000003'
  AND citation_quote = 'Trong trường hợp thí sinh bị rớt mạng do sự cố kết nối, hệ thống kích hoạt cửa sổ cứu vớt (Reconnect Recovery Window) 60 giây...';
