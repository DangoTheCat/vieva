// The only three roles in the system (BE migration V14); every account has exactly one
export const ROLE_OPTIONS = [
  { code: 'ROLE_STUDENT', description: 'Sinh viên dự thi vấn đáp' },
  { code: 'ROLE_LECTURER', description: 'Giảng viên phụ trách môn học' },
  { code: 'ROLE_ADMIN', description: 'Quản trị viên hệ thống' }
];

/**
 * An account has exactly one role. Reads `role` (BE field), falling back to the
 * `roles` array, and accepts codes without the ROLE_ prefix.
 */
export function getUserRole(user) {
  const code = user?.role || (Array.isArray(user?.roles) ? user.roles[0] : null);
  if (!code) return null;
  return code.startsWith('ROLE_') ? code : `ROLE_${code}`;
}
