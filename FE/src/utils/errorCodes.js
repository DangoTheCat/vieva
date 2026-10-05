export const ERROR_MESSAGES = {
  '9999': 'Lỗi hệ thống không xác định. Vui lòng thử lại sau.',
  '1001': 'Không tìm thấy người dùng trong hệ thống.',
  '1002': 'Email này đã tồn tại trên hệ thống. Vui lòng dùng email khác.',
  '1003': 'Phiên đăng nhập đã hết hạn hoặc chưa xác thực. Vui lòng đăng nhập lại.',
  '1004': 'Bạn không có quyền thực hiện thao tác này.',
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
  
  // Subject & Lecturer Assignment (1018 - 1024, 1059)
  '1018': 'Không tìm thấy thông tin môn học trong hệ thống.',
  '1019': 'Mã môn học đã tồn tại trên hệ thống. Vui lòng chọn mã môn học khác.',
  '1020': 'Người dùng được chọn không có vai trò Giảng viên (ROLE_LECTURER).',
  '1021': 'Không thể phân công môn học cho giảng viên có tài khoản đang bị khóa.',
  '1022': 'Không thể phân công giảng viên cho môn học đang ngưng hoạt động (INACTIVE).',
  '1023': 'Giảng viên này đã được phân công phụ trách môn học này từ trước.',
  '1024': 'Không tìm thấy thông tin phân công giảng viên cho môn học.',
  '1059': 'Môn học hiện đang ở trạng thái ngưng hoạt động.',

  // Course Document & RAG Indexing (1025 - 1028, 1045 - 1051)
  '1025': 'Không tìm thấy tài liệu giáo trình này.',
  '1026': 'Tài liệu chưa hoàn tất lập chỉ mục (Chưa ở trạng thái READY). Chỉ tài liệu READY mới được dùng cho AI.',
  '1027': 'Không thể trích xuất văn bản từ tài liệu (File rỗng hoặc là file scan không có lớp text OCR).',
  '1028': 'Không tìm thấy nội dung ngữ cảnh phù hợp trong các tài liệu giáo trình đã chọn.',
  '1045': 'Định dạng tệp không được hỗ trợ. Vui lòng tải file PDF, DOCX, PPTX hoặc TXT.',
  '1046': 'Dung lượng tệp vượt quá giới hạn tối đa cho phép (20MB).',
  '1047': 'Chuyển đổi trạng thái xử lý tài liệu không hợp lệ.',
  '1048': 'Chỉ các tài liệu gặp sự cố lập chỉ mục (FAILED) mới có thể thử lại (Retry).',
  '1049': 'Đã vượt quá số lần thử lại lập chỉ mục tối đa cho tài liệu này.',
  '1050': 'Tài liệu đang được trích dẫn làm nguồn cho câu hỏi thi nên không thể xóa.',
  '1051': 'Các tài liệu được chọn không thuộc về môn học hiện tại.',

  // Question Bank & Review Queue (1029 - 1033, 1038 - 1044, 1052 - 1055)
  '1029': 'Không tìm thấy câu hỏi này trong hệ thống.',
  '1030': 'Không tìm thấy phiên bản câu hỏi yêu cầu.',
  '1031': 'Đã tồn tại một bản nháp (DRAFT) đang chỉnh sửa cho câu hỏi này.',
  '1032': 'Chỉ các phiên bản ở trạng thái DRAFT mới có thể chỉnh sửa, phê duyệt, từ chối hoặc xóa.',
  '1033': 'Đoạn trích dẫn không khớp với nội dung phân đoạn trong tài liệu gốc.',
  '1035': 'Bạn không được phân công quyền phụ trách môn học này.',
  '1036': 'Không tìm thấy chủ đề (Topic) yêu cầu.',
  '1037': 'Chủ đề này không thuộc về môn học hiện tại.',
  '1038': 'Câu hỏi sinh bởi AI bắt buộc phải gắn ít nhất một đoạn trích dẫn nguồn (Source Chunk) trước khi duyệt.',
  '1039': 'Cấp độ Bloom do AI đề xuất bắt buộc phải được Giảng viên kiểm tra và bấm xác nhận trước khi duyệt.',
  '1040': 'Câu hỏi bắt buộc phải có ma trận Rubric với ít nhất một tiêu chí chấm điểm.',
  '1041': 'Nội dung câu hỏi và đáp án mong đợi không được để trống.',
  '1042': 'Câu hỏi này đã được lưu trữ (ARCHIVED) và không còn hiệu lực.',
  '1043': 'Câu hỏi chưa có phiên bản nào được phê duyệt (APPROVED) chính thức.',
  '1044': 'Dữ liệu đã bị thay đổi bởi phiên làm việc khác. Vui lòng tải lại trang và thử lại.',
  '1052': 'Tỷ lệ phân bố cấp độ nhận thức Bloom không hợp lệ (Tổng số lượng không khớp).',
  '1053': 'Không tìm thấy yêu cầu sinh câu hỏi tự động này.',
  '1054': 'Dịch vụ AI tạo sinh hiện không phản hồi. Vui lòng thử lại sau ít phút.',
  '1055': 'Chỉ các bản nháp sinh từ AI RAG có lưu trữ ngữ cảnh mới có thể tạo lại (Regenerate).',

  // Rubric & Import (1034, 1056 - 1058)
  '1034': 'Tổng điểm tối đa của các tiêu chí con phải bằng chính xác tổng điểm của Rubric.',
  '1056': 'Không tìm thấy tiêu chí chấm điểm (Rubric Criterion) này.',
  '1057': 'Tệp nhập dữ liệu (Excel/CSV) không đúng cấu trúc mẫu quy định.',
  '1058': 'Số lượng câu hỏi trong tệp nhập vượt quá giới hạn tối đa cho phép.',

  // AI rules
  '1060': 'Chưa có cấu hình AI rule đang hoạt động. Vui lòng liên hệ quản trị viên.',

  // Rate limiting
  '4290': 'Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút.',
  '4291': 'Đã vượt giới hạn số yêu cầu AI. Vui lòng thử lại sau.'
};

// Codes whose BE message carries specifics (field names, Bloom sums) that the static text would hide
const PREFER_BACKEND_MESSAGE = new Set(['1006', '1052']);

export function getErrorMessage(error) {
  if (!error) return 'Đã xảy ra lỗi không xác định.';
  if (typeof error === 'string') return error;

  const code = error.code || (error.response?.data?.code);

  const fieldErrors = error.data?.errors;
  if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
    return fieldErrors.map(fe => `${fe.field}: ${fe.message}`).join('; ');
  }

  if (PREFER_BACKEND_MESSAGE.has(code) && error.data?.message) {
    return error.data.message;
  }

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
