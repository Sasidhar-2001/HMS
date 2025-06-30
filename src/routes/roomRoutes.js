const express = require('express');
const {
  getAllRooms,
  getRoom,
  createRoom,
  updateRoom,
  deleteRoom,
  getAvailableRooms,
  assignStudent,
  removeStudent,
  getRoomStats
} = require('../controllers/roomController');
const { authenticateToken, authorizeRoles } = require('../middleware/auth');
const {
  validateRoom,
  validatePagination,
  validateObjectId
} = require('../middleware/validation');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Public room routes (all authenticated users)
router.get('/', validatePagination, getAllRooms);
router.get('/available', getAvailableRooms);
router.get('/stats', getRoomStats);
router.get('/:id', validateObjectId, getRoom);

// Admin/Warden only routes
router.post('/', authorizeRoles('admin', 'warden'), validateRoom, createRoom);
router.put('/:id', authorizeRoles('admin', 'warden'), validateObjectId, updateRoom);
router.delete('/:id', authorizeRoles('admin', 'warden'), validateObjectId, deleteRoom);
router.post('/:id/assign', authorizeRoles('admin', 'warden'), validateObjectId, assignStudent);
router.post('/:id/remove', authorizeRoles('admin', 'warden'), validateObjectId, removeStudent);

module.exports = router;