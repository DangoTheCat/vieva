// Role codes seeded by BE DataInitializer
export const ROLE_OPTIONS = [
  { code: 'ROLE_USER', description: 'Người dùng thông thường' },
  { code: 'ROLE_LECTURER', description: 'Giảng viên phụ trách môn học' },
  { code: 'ROLE_STUDENT', description: 'Sinh viên dự thi vấn đáp' },
  { code: 'ROLE_ADMIN', description: 'Quản trị viên hệ thống' }
];

/**
 * An account has exactly one role. Reads `role` (new BE field), falling back to the
 * legacy `roles` array, and normalizes aliases like 'ADMIN' to 'ROLE_ADMIN'.
 */
export function getUserRole(user) {
  const code = user?.role || (Array.isArray(user?.roles) ? user.roles[0] : null);
  if (!code) return null;
  return code.startsWith('ROLE_') ? code : `ROLE_${code}`;
}
