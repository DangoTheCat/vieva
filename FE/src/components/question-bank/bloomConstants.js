// Bloom Taxonomy Levels & Color Tokens (Pedagogical UI)
// Bloom levels. Labels match the BE enum (BloomLevel.viLabel).
// The requirement names four levels (nhớ, hiểu, vận dụng, phân tích): those are offered when creating
// questions. EVALUATE/CREATE stay in ALL_BLOOM_LEVELS so existing questions still display and filter.
export const ALL_BLOOM_LEVELS = [
  { value: 'REMEMBER', label: '1. Nhớ (Remember)', short: 'Nhớ', color: 'bg-emerald-50 text-emerald-800 border border-pink-200' },
  { value: 'UNDERSTAND', label: '2. Hiểu (Understand)', short: 'Hiểu', color: 'bg-emerald-50 text-emerald-800 border border-pink-200' },
  { value: 'APPLY', label: '3. Vận dụng (Apply)', short: 'Vận dụng', color: 'bg-emerald-50 text-emerald-800 border border-pink-200' },
  { value: 'ANALYZE', label: '4. Phân tích (Analyze)', short: 'Phân tích', color: 'bg-emerald-50 text-emerald-800 border border-pink-200' },
  { value: 'EVALUATE', label: '5. Đánh giá (Evaluate)', short: 'Đánh giá', color: 'bg-amber-50 text-amber-800 border border-amber-200' },
  { value: 'CREATE', label: '6. Sáng tạo (Create)', short: 'Sáng tạo', color: 'bg-pink-50 text-pink-800 border border-pink-200' }
];

/** The four levels required for oral exams; used wherever a new question gets a level. */
export const BLOOM_LEVELS = ALL_BLOOM_LEVELS.slice(0, 4);

/** Display metadata for any stored level (falls back to the first level for unknown values). */
export const bloomMeta = (value) => ALL_BLOOM_LEVELS.find((b) => b.value === value) || ALL_BLOOM_LEVELS[0];

/** Options for a select that edits an existing question: the four levels plus its current one if outside them. */
export const bloomOptionsFor = (current) =>
  BLOOM_LEVELS.some((b) => b.value === current) || !current
    ? BLOOM_LEVELS
    : [...BLOOM_LEVELS, bloomMeta(current)];

// Presets for RAG Bloom Taxonomy distribution (four required levels only)
export const BLOOM_PRESETS = [
  {
    id: 'BALANCED',
    name: 'Cân Bằng Chuẩn',
    desc: 'Bao quát từ nhớ, hiểu đến vận dụng và phân tích',
    weights: { REMEMBER: 0.2, UNDERSTAND: 0.3, APPLY: 0.3, ANALYZE: 0.2 }
  },
  {
    id: 'VIVA_ORAL',
    name: 'Vấn Đáp Viva',
    desc: 'Ưu tiên vận dụng và phân tích, lập luận',
    weights: { REMEMBER: 0.1, UNDERSTAND: 0.25, APPLY: 0.35, ANALYZE: 0.3 }
  },
  {
    id: 'FOUNDATIONAL',
    name: 'Nền Tảng',
    desc: 'Tập trung định nghĩa cốt lõi & hiểu nguyên lý',
    weights: { REMEMBER: 0.4, UNDERSTAND: 0.4, APPLY: 0.2, ANALYZE: 0 }
  },
  {
    id: 'ADVANCED',
    name: 'Bậc Cao',
    desc: 'Đào sâu vận dụng và phân tích',
    weights: { REMEMBER: 0, UNDERSTAND: 0.2, APPLY: 0.35, ANALYZE: 0.45 }
  },
  {
    id: 'CUSTOM',
    name: 'Tùy Chỉnh',
    desc: 'Tự điều chỉnh số câu cho từng bậc Bloom',
    weights: null
  }
];

export function calculateBloomDistribution(total, weights) {
  const dist = { REMEMBER: 0, UNDERSTAND: 0, APPLY: 0, ANALYZE: 0, EVALUATE: 0, CREATE: 0 };
  const n = Math.max(1, Math.min(20, Number(total) || 1));
  if (!weights) {
    dist.UNDERSTAND = n;
    return dist;
  }
  let assigned = 0;
  const entries = Object.entries(weights).filter(([_, w]) => w > 0);
  if (entries.length === 0) {
    dist.UNDERSTAND = n;
    return dist;
  }
  entries.forEach(([level, weight]) => {
    dist[level] = Math.floor(n * weight);
    assigned += dist[level];
  });
  const remainders = entries.map(([level, weight]) => ({
    level,
    remainder: (n * weight) - Math.floor(n * weight)
  })).sort((a, b) => b.remainder - a.remainder);

  for (let i = 0; assigned < n; i = (i + 1) % remainders.length) {
    dist[remainders[i].level] += 1;
    assigned += 1;
  }
  return dist;
}

// Rubric criteria rules mirror BE CriterionRequest: name + description required, maxScore > 0
export function validateCriteria(criteria) {
  if (!Array.isArray(criteria) || criteria.length === 0) return 'Rubric cần ít nhất 1 tiêu chí.';
  if (criteria.some(c => !c.name?.trim() || !c.description?.trim())) {
    return 'Mỗi tiêu chí rubric cần có tên và mô tả.';
  }
  if (criteria.some(c => !(Number(c.maxScore) > 0))) {
    return 'Điểm tối đa của mỗi tiêu chí phải lớn hơn 0.';
  }
  return null;
}

export function toRubricPayload(criteria) {
  return {
    totalScore: criteria.reduce((sum, c) => sum + Number(c.maxScore), 0),
    criteria: criteria.map((c, idx) => ({
      name: c.name.trim(),
      description: c.description.trim(),
      maxScore: Number(c.maxScore),
      levels: c.levels,
      orderIndex: idx + 1
    }))
  };
}
