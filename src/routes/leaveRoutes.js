const express = require('express');
const {
  getAllLeaves,
  getLeave,
  createLeave,
  updateLeave,
  updateLeaveStatus,
  requestExtension,
  updateExtensionStatus,
  markReturn,
  cancelLeave,
  getLeaveStats
} = require('../controllers/leaveController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validateLeave,
  validatePagination,
  validateObjectId
} = require('../middleware/validation');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Common routes
router.get('/', validatePagination, getAllLeaves);
router.get('/stats', authorizeRoles('admin', 'warden'), getLeaveStats);
router.get('/:id', validateObjectId, getLeave);
router.post('/', validateLeave, createLeave);
router.put('/:id', validateObjectId, updateLeave);
router.post('/:id/cancel', validateObjectId, cancelLeave);

// Extension routes
router.post('/:id/extension', validateObjectId, requestExtension);
router.put('/:id/extension/:extensionId', authorizeRoles('admin', 'warden'), updateExtensionStatus);

// Admin/Warden only routes
router.put('/:id/status', authorizeRoles('admin', 'warden'), validateObjectId, updateLeaveStatus);
router.post('/:id/return', authorizeRoles('admin', 'warden'), validateObjectId, markReturn);

module.exports = router;