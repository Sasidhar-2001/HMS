const express = require('express');
const {
  getAllFees,
  getFee,
  createFee,
  updateFee,
  addPayment,
  sendFeeReminder,
  sendBulkReminders,
  getFeeStats,
  getDefaulters
} = require('../controllers/feeController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validateFee,
  validatePagination,
  validateObjectId
} = require('../middleware/validation');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Common routes
router.get('/', validatePagination, getAllFees);
router.get('/stats', authorizeRoles('admin', 'warden'), getFeeStats);
router.get('/defaulters', authorizeRoles('admin', 'warden'), getDefaulters);
router.get('/:id', validateObjectId, getFee);

// Payment routes
router.post('/:id/payment', validateObjectId, addPayment);

// Admin/Warden only routes
router.post('/', authorizeRoles('admin', 'warden'), validateFee, createFee);
router.put('/:id', authorizeRoles('admin', 'warden'), validateObjectId, updateFee);
router.post('/:id/reminder', authorizeRoles('admin', 'warden'), validateObjectId, sendFeeReminder);
router.post('/bulk-reminders', authorizeRoles('admin', 'warden'), sendBulkReminders);

module.exports = router;