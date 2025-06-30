const Room = require('../models/Room');
const User = require('../models/User');

// Get all rooms
const getAllRooms = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const block = req.query.block;
    const floor = req.query.floor;
    const type = req.query.type;
    const status = req.query.status;

    const query = { isActive: true };
    if (block) query.block = block;
    if (floor) query.floor = parseInt(floor);
    if (type) query.type = type;
    if (status) query.status = status;

    const rooms = await Room.find(query)
      .populate('occupants.student', 'firstName lastName email studentId')
      .sort({ block: 1, floor: 1, roomNumber: 1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Room.countDocuments(query);

    res.json({
      success: true,
      data: {
        rooms,
        pagination: {
          current: page,
          pages: Math.ceil(total / limit),
          total
        }
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch rooms',
      error: error.message
    });
  }
};

// Get single room
const getRoom = async (req, res) => {
  try {
    const { id } = req.params;

    const room = await Room.findById(id)
      .populate('occupants.student', 'firstName lastName email phone studentId course year');

    if (!room) {
      return res.status(404).json({
        success: false,
        message: 'Room not found'
      });
    }

    res.json({
      success: true,
      data: { room }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch room',
      error: error.message
    });
  }
};

// Create new room
const createRoom = async (req, res) => {
  try {
    const room = new Room(req.body);
    await room.save();

    res.status(201).json({
      success: true,
      message: 'Room created successfully',
      data: { room }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create room',
      error: error.message
    });
  }
};

// Update room
const updateRoom = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const room = await Room.findByIdAndUpdate(
      id,
      updates,
      { new: true, runValidators: true }
    );

    if (!room) {
      return res.status(404).json({
        success: false,
        message: 'Room not found'
      });
    }

    res.json({
      success: true,
      message: 'Room updated successfully',
      data: { room }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update room',
      error: error.message
    });
  }
};

// Delete room
const deleteRoom = async (req, res) => {
  try {
    const { id } = req.params;

    const room = await Room.findById(id);
    if (!room) {
      return res.status(404).json({
        success: false,
        message: 'Room not found'
      });
    }

    if (room.currentOccupancy > 0) {
      return res.status(400).json({
        success: false,
        message: 'Cannot delete room with occupants'
      });
    }

    room.isActive = false;
    await room.save();

    res.json({
      success: true,
      message: 'Room deleted successfully'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to delete room',
      error: error.message
    });
  }
};

// Get available rooms
const getAvailableRooms = async (req, res) => {
  try {
    const type = req.query.type;
    const block = req.query.block;

    const query = {
      isActive: true,
      status: 'available',
      $expr: { $lt: ['$currentOccupancy', '$capacity'] }
    };

    if (type) query.type = type;
    if (block) query.block = block;

    const rooms = await Room.find(query)
      .sort({ block: 1, floor: 1, roomNumber: 1 });

    res.json({
      success: true,
      data: { rooms }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch available rooms',
      error: error.message
    });
  }
};

// Assign student to room
const assignStudent = async (req, res) => {
  try {
    const { id } = req.params;
    const { studentId, bedNumber } = req.body;

    const room = await Room.findById(id);
    const student = await User.findById(studentId);

    if (!room || !student) {
      return res.status(404).json({
        success: false,
        message: 'Room or student not found'
      });
    }

    if (student.role !== 'student') {
      return res.status(400).json({
        success: false,
        message: 'Only students can be assigned to rooms'
      });
    }

    if (student.roomNumber) {
      return res.status(400).json({
        success: false,
        message: 'Student is already assigned to a room'
      });
    }

    // Add student to room
    await room.addOccupant(studentId, bedNumber);
    
    // Update student's room assignment
    student.roomNumber = id;
    await student.save();

    const updatedRoom = await Room.findById(id)
      .populate('occupants.student', 'firstName lastName email studentId');

    res.json({
      success: true,
      message: 'Student assigned to room successfully',
      data: { room: updatedRoom }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to assign student to room',
      error: error.message
    });
  }
};

// Remove student from room
const removeStudent = async (req, res) => {
  try {
    const { id } = req.params;
    const { studentId } = req.body;

    const room = await Room.findById(id);
    const student = await User.findById(studentId);

    if (!room || !student) {
      return res.status(404).json({
        success: false,
        message: 'Room or student not found'
      });
    }

    // Remove student from room
    await room.removeOccupant(studentId);
    
    // Update student's room assignment
    student.roomNumber = null;
    await student.save();

    const updatedRoom = await Room.findById(id)
      .populate('occupants.student', 'firstName lastName email studentId');

    res.json({
      success: true,
      message: 'Student removed from room successfully',
      data: { room: updatedRoom }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to remove student from room',
      error: error.message
    });
  }
};

// Get room statistics
const getRoomStats = async (req, res) => {
  try {
    const [
      totalRooms,
      occupiedRooms,
      availableRooms,
      maintenanceRooms,
      roomsByType,
      roomsByBlock,
      occupancyRate
    ] = await Promise.all([
      Room.countDocuments({ isActive: true }),
      Room.countDocuments({ status: 'occupied' }),
      Room.countDocuments({ status: 'available' }),
      Room.countDocuments({ status: 'maintenance' }),
      Room.aggregate([
        { $match: { isActive: true } },
        { $group: { _id: '$type', count: { $sum: 1 } } }
      ]),
      Room.aggregate([
        { $match: { isActive: true } },
        { $group: { _id: '$block', count: { $sum: 1 } } }
      ]),
      Room.aggregate([
        { $match: { isActive: true } },
        {
          $group: {
            _id: null,
            totalCapacity: { $sum: '$capacity' },
            totalOccupancy: { $sum: '$currentOccupancy' }
          }
        }
      ])
    ]);

    const occupancyPercentage = occupancyRate[0] 
      ? ((occupancyRate[0].totalOccupancy / occupancyRate[0].totalCapacity) * 100).toFixed(2)
      : 0;

    res.json({
      success: true,
      data: {
        total: totalRooms,
        occupied: occupiedRooms,
        available: availableRooms,
        maintenance: maintenanceRooms,
        occupancyPercentage: parseFloat(occupancyPercentage),
        byType: roomsByType,
        byBlock: roomsByBlock
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch room statistics',
      error: error.message
    });
  }
};

module.exports = {
  getAllRooms,
  getRoom,
  createRoom,
  updateRoom,
  deleteRoom,
  getAvailableRooms,
  assignStudent,
  removeStudent,
  getRoomStats
};