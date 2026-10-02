export const ERROR_MESSAGES = {
  '9999': 'Lỗi hệ thống không xác định. Vui lòng thử lại sau.',
  '1001': 'Không tìm thấy người dùng trong hệ thống.',
  '1002': 'Email này đã tồn tại trên hệ thống. Vui lòng dùng email khác.',
  '1003': 'Phiên đăng nhập đã hết hạn hoặc chưa xác thực. Vui lòng đăng nhập lại.',
  '1004': 'Bạn không có quyền thực hiện thao tác này (Yêu cầu quyền Administrator).',
  '1005': 'Mã thông điệp không hợp lệ.',
  '1006': 'Dữ liệu yêu cầu không hợp lệ. Vui lòng kiểm tra lại các trường thông tin.',
  '1007': 'Tài khoản người dùng đang bị khóa hoặc chưa kích hoạt.',
  '1008': 'Vai trò (Role) không tồn tại trong hệ thống.',
  '1009': 'Mật khẩu cũ không chính xác.',
  '1010': 'Mật khẩu mới phải khác với mật khẩu cũ hiện tại.',
  '1011': 'Bạn không thể tự xóa tài khoản của chính mình.',
  '1012': 'Bạn không thể tự hạ quyền Administrator của chính mình.',
  '1013': 'Không thể xóa quản trị viên (Admin) duy nhất còn lại trong hệ thống.',
  '1014': 'Không thể hạ quyền quản trị viên (Admin) duy nhất còn lại trong hệ thống.',
  '1015': 'Mã người dùng (User Code / MSSV) đã tồn tại. Vui lòng chọn mã khác.',
  '1016': 'Bạn không thể tự khóa hoặc vô hiệu hóa tài khoản của chính mình.',
  '1017': 'Không thể khóa hoặc vô hiệu hóa quản trị viên (Admin) đang hoạt động duy nhất.',
  '4090': 'Xung đột dữ liệu đồng thời. Tài nguyên đã được cập nhật bởi phiên làm việc khác. Vui lòng tải lại.',
  '401': 'Phiên đăng nhập chưa có hoặc đã hết hạn. Vui lòng bấm Đăng nhập để kết nối Backend.',
  '403': 'Tài khoản chưa có quyền Administrator. Vui lòng đăng nhập tài khoản Admin.'
};

export function getErrorMessage(error) {
  if (!error) return 'Đã xảy ra lỗi không xác định.';
  if (typeof error === 'string') return error;

  const code = String(error.code || error.status || error.response?.data?.code || '');
  if (code && ERROR_MESSAGES[code]) {
    return ERROR_MESSAGES[code];
  }

  if (error.response?.data?.message) {
    return error.response.data.message;
  }

  if (error.message) {
    return error.message;
  }

  return 'Đã xảy ra lỗi kết nối với máy chủ Backend.';
}
