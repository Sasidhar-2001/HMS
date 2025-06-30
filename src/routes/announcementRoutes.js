const express = require('express');
const {
  getAllAnnouncements,
  getAnnouncement,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  toggleLike,
  addComment,
  publishAnnouncement,
  getAnnouncementStats
} = require('../controllers/announcementController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validateAnnouncement,
  validatePagination,
  validateObjectId
} = require('../middleware/validation');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Common routes
router.get('/', validatePagination, getAllAnnouncements);
router.get('/stats', authorizeRoles('admin', 'warden'), getAnnouncementStats);
router.get('/:id', validateObjectId, getAnnouncement);

// Interaction routes
router.post('/:id/like', validateObjectId, toggleLike);
router.post('/:id/comment', validateObjectId, addComment);

// Admin/Warden only routes
router.post('/', authorizeRoles('admin', 'warden'), validateAnnouncement, createAnnouncement);
router.put('/:id', authorizeRoles('admin', 'warden'), validateObjectId, updateAnnouncement);
router.delete('/:id', authorizeRoles('admin', 'warden'), validateObjectId, deleteAnnouncement);
router.post('/:id/publish', authorizeRoles('admin', 'warden'), validateObjectId, publishAnnouncement);

module.exports = router;