import axios from 'axios'

const API_BASE_URL = 'http://localhost:5000/api'

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle response errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/auth/register', userData),
  getProfile: () => api.get('/auth/profile'),
  updateProfile: (profileData) => api.put('/auth/profile', profileData),
}

// Health API
export const healthAPI = {
  check: () => api.get('/health'),
}

// Students API (Admin/Warden only)
export const studentsAPI = {
  getAll: (params) => api.get('/admin/students', { params }),
  create: (studentData) => api.post('/admin/students', studentData),
  update: (id, studentData) => api.put(`/admin/students/${id}`, studentData),
  delete: (id) => api.delete(`/admin/students/${id}`),
  assignRoom: (data) => api.post('/admin/assign-room', data),
}

// Warden API
export const wardenAPI = {
  getStudents: (params) => api.get('/warden/students', { params }),
  getDashboard: () => api.get('/warden/dashboard'),
}

// Admin API
export const adminAPI = {
  getDashboard: () => api.get('/admin/dashboard'),
  generateReport: (params) => api.get('/admin/reports', { params }),
}

// Rooms API
export const roomsAPI = {
  getAll: (params) => api.get('/rooms', { params }),
  getAvailable: () => api.get('/rooms/available'),
  getStats: () => api.get('/rooms/stats'),
  create: (roomData) => api.post('/rooms', roomData),
  update: (id, roomData) => api.put(`/rooms/${id}`, roomData),
  delete: (id) => api.delete(`/rooms/${id}`),
  assignStudent: (id, data) => api.post(`/rooms/${id}/assign`, data),
  removeStudent: (id, data) => api.post(`/rooms/${id}/remove`, data),
}

// Complaints API
export const complaintsAPI = {
  getAll: (params) => api.get('/complaints', { params }),
  getById: (id) => api.get(`/complaints/${id}`),
  create: (complaintData) => api.post('/complaints', complaintData),
  update: (id, complaintData) => api.put(`/complaints/${id}`, complaintData),
  updateStatus: (id, statusData) => api.put(`/complaints/${id}/status`, statusData),
  delete: (id) => api.delete(`/complaints/${id}`),
  getStats: () => api.get('/complaints/stats'),
}

// Fees API
export const feesAPI = {
  getAll: (params) => api.get('/fees', { params }),
  getById: (id) => api.get(`/fees/${id}`),
  create: (feeData) => api.post('/fees', feeData),
  update: (id, feeData) => api.put(`/fees/${id}`, feeData),
  addPayment: (id, paymentData) => api.post(`/fees/${id}/payment`, paymentData),
  getStats: () => api.get('/fees/stats'),
  getDefaulters: () => api.get('/fees/defaulters'),
}

// Leaves API
export const leavesAPI = {
  getAll: (params) => api.get('/leaves', { params }),
  getById: (id) => api.get(`/leaves/${id}`),
  create: (leaveData) => api.post('/leaves', leaveData),
  update: (id, leaveData) => api.put(`/leaves/${id}`, leaveData),
  updateStatus: (id, statusData) => api.put(`/leaves/${id}/status`, statusData),
  cancel: (id, reason) => api.post(`/leaves/${id}/cancel`, { reason }),
  getStats: () => api.get('/leaves/stats'),
}

// Announcements API
export const announcementsAPI = {
  getAll: (params) => api.get('/announcements', { params }),
  getById: (id) => api.get(`/announcements/${id}`),
  create: (announcementData) => api.post('/announcements', announcementData),
  update: (id, announcementData) => api.put(`/announcements/${id}`, announcementData),
  delete: (id) => api.delete(`/announcements/${id}`),
  toggleLike: (id) => api.post(`/announcements/${id}/like`),
  addComment: (id, comment) => api.post(`/announcements/${id}/comment`, { comment }),
  getStats: () => api.get('/announcements/stats'),
}

// Dashboard API
export const dashboardAPI = {
  getAdminStats: () => api.get('/admin/dashboard'),
  getStudentDashboard: () => api.get('/student/dashboard'),
  getWardenDashboard: () => api.get('/warden/dashboard'),
}

export default api