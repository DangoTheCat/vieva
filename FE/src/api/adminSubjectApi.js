import { apiClient } from './client';

export const adminSubjectApi = {
  getSubjects(params = {}) {
    return apiClient.get('/admin/subjects', params);
  },

  getSubjectById(id) {
    return apiClient.get(`/admin/subjects/${id}`);
  },

  createSubject(payload) {
    return apiClient.post('/admin/subjects', payload);
  },

  updateSubject(id, payload) {
    return apiClient.put(`/admin/subjects/${id}`, payload);
  },

  deactivateSubject(id) {
    return apiClient.patch(`/admin/subjects/${id}/deactivate`);
  },

  // Lecturer - Subject assignments
  assignLecturers(subjectId, lecturerIds) {
    return apiClient.post('/admin/lecturer-subjects', { subjectId, lecturerIds });
  },

  revokeAssignment(assignmentId) {
    return apiClient.delete(`/admin/lecturer-subjects/${assignmentId}`);
  },

  getLecturersBySubject(subjectId) {
    return apiClient.get(`/admin/lecturer-subjects/by-subject/${subjectId}`);
  },

  getSubjectsByLecturer(lecturerId) {
    return apiClient.get(`/admin/lecturer-subjects/by-lecturer/${lecturerId}`);
  }
};
