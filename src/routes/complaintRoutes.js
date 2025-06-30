const express = require('express');
const {
  getAllComplaints,
  getComplaint,
  createComplaint,
  updateComplaint,
  updateComplaintStatus,
  deleteComplaint,
  uploadComplaintImages,
  getComplaintStats
} = require('../controllers/complaintController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validateComplaint,
  validatePagination,
  validateObjectId
} = require('../middleware/validation');
const { uploadMultiple } = require('../utils/fileUpload');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Common routes
router.get('/', validatePagination, getAllComplaints);
router.get('/stats', authorizeRoles('admin', 'warden'), getComplaintStats);
router.get('/:id', validateObjectId, getComplaint);
router.post('/', validateComplaint, createComplaint);
router.put('/:id', validateObjectId, updateComplaint);
router.delete('/:id', validateObjectId, deleteComplaint);

// File upload
router.post('/:id/images', validateObjectId, uploadMultiple('images', 5), uploadComplaintImages);

// Admin/Warden only routes
router.put('/:id/status', authorizeRoles('admin', 'warden'), validateObjectId, updateComplaintStatus);

module.exports = router;