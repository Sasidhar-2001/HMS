const express = require('express');
const {
  getDashboardStats,
  getAllStudents,
  createStudent,
  updateStudent,
  deleteStudent,
  assignRoom,
  generateReport,
  getAllWardens,
  createWarden
} = require('../controllers/adminController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validateUserRegistration,
  validatePagination,
  validateObjectId
} = require('../middleware/validation');

const router = express.Router();

// All routes require admin authentication
router.use(authenticateToken);
router.use(authorizeRoles('admin'));

// Dashboard
router.get('/dashboard', getDashboardStats);

// Student management
router.get('/students', validatePagination, getAllStudents);
router.post('/students', validateUserRegistration, createStudent);
router.put('/students/:id', validateObjectId, updateStudent);
router.delete('/students/:id', validateObjectId, deleteStudent);

// Room assignment
router.post('/assign-room', assignRoom);

// Warden management
router.get('/wardens', getAllWardens);
router.post('/wardens', validateUserRegistration, createWarden);

// Reports
router.get('/reports', generateReport);

module.exports = router;