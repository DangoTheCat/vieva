// Role codes seeded by BE DataInitializer
export const ROLE_OPTIONS = [
  { code: 'ROLE_USER', description: 'Người dùng thông thường' },
  { code: 'ROLE_LECTURER', description: 'Giảng viên phụ trách môn học' },
  { code: 'ROLE_STUDENT', description: 'Sinh viên dự thi vấn đáp' },
  { code: 'ROLE_ADMIN', description: 'Quản trị viên hệ thống' }
];

// Roles an admin may assign (BE rejects ROLE_USER with 1063; it is only given by self-registration)
export const ASSIGNABLE_ROLE_OPTIONS = ROLE_OPTIONS.filter(({ code }) => code !== 'ROLE_USER');

/**
 * An account has exactly one role. Reads `role` (new BE field), falling back to the
 * legacy `roles` array, and normalizes aliases like 'ADMIN' to 'ROLE_ADMIN'.
 */
export function getUserRole(user) {
  const code = user?.role || (Array.isArray(user?.roles) ? user.roles[0] : null);
  if (!code) return null;
  return code.startsWith('ROLE_') ? code : `ROLE_${code}`;
}
