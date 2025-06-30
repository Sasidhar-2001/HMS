const express = require('express');
const {
  getDashboard,
  getMyComplaints,
  getMyFees,
  getMyLeaves,
  getAnnouncements,
  markAnnouncementRead,
  getRoomDetails,
  updateProfile
} = require('../controllers/studentController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const { validatePagination, validateObjectId } = require('../middleware/validation');

const router = express.Router();

// All routes require student authentication
router.use(authenticateToken);
router.use(authorizeRoles('student'));

// Dashboard
router.get('/dashboard', getDashboard);

// Profile
router.put('/profile', updateProfile);

// Complaints
router.get('/complaints', validatePagination, getMyComplaints);

// Fees
router.get('/fees', validatePagination, getMyFees);

// Leaves
router.get('/leaves', validatePagination, getMyLeaves);

// Announcements
router.get('/announcements', validatePagination, getAnnouncements);
router.post('/announcements/:id/read', validateObjectId, markAnnouncementRead);

// Room
router.get('/room', getRoomDetails);

module.exports = router;