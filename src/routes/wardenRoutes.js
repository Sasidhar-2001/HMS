const express = require('express');
const {
  getDashboard,
  getAllComplaints,
  updateComplaintStatus,
  getAllLeaves,
  updateLeaveStatus,
  getStudents,
  getRoomOccupancy,
  createAnnouncement,
  getMaintenanceRequests
} = require('../controllers/wardenController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validatePagination,
  validateObjectId,
  validateAnnouncement
} = require('../middleware/validation');

const router = express.Router();

// All routes require warden authentication
router.use(authenticateToken);
router.use(authorizeRoles('warden', 'admin'));

// Dashboard
router.get('/dashboard', getDashboard);

// Students - Add this new route
router.get('/students', validatePagination, getStudents);

// Complaints
router.get('/complaints', validatePagination, getAllComplaints);
router.put('/complaints/:id/status', validateObjectId, updateComplaintStatus);
router.get('/maintenance-requests', getMaintenanceRequests);

// Leaves
router.get('/leaves', validatePagination, getAllLeaves);
router.put('/leaves/:id/status', validateObjectId, updateLeaveStatus);

// Rooms
router.get('/rooms/occupancy', getRoomOccupancy);

// Announcements
router.post('/announcements', validateAnnouncement, createAnnouncement);

module.exports = router;