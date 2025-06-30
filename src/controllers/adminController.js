const User = require('../models/User');
const Room = require('../models/Room');
const Complaint = require('../models/Complaint');
const Fee = require('../models/Fee');
const Leave = require('../models/Leave');
const Announcement = require('../models/Announcement');
const { generatePDFReport, generateExcelReport } = require('../utils/reportGenerator');
const { sendEmail, emailTemplates } = require('../utils/email');

// Dashboard statistics
const getDashboardStats = async (req, res) => {
  try {
    const [
      totalStudents,
      totalRooms,
      occupiedRooms,
      pendingComplaints,
      overdueComplaints,
      pendingFees,
      overdueFees,
      pendingLeaves,
      totalRevenue,
      pendingRevenue
    ] = await Promise.all([
      User.countDocuments({ role: 'student', isActive: true }),
      Room.countDocuments({ isActive: true }),
      Room.countDocuments({ status: 'occupied' }),
      Complaint.countDocuments({ status: 'pending' }),
      Complaint.countDocuments({ 
        status: { $in: ['pending', 'in_progress'] },
        expectedResolutionDate: { $lt: new Date() }
      }),
      Fee.countDocuments({ status: 'pending' }),
      Fee.countDocuments({ status: 'overdue' }),
      Leave.countDocuments({ status: 'pending' }),
      Fee.aggregate([
        { $match: { status: 'paid' } },
        { $group: { _id: null, total: { $sum: '$finalAmount' } } }
      ]),
      Fee.aggregate([
        { $match: { status: { $in: ['pending', 'overdue', 'partial'] } } },
        { $group: { _id: null, total: { $sum: '$balanceAmount' } } }
      ])
    ]);

    const roomOccupancyRate = totalRooms > 0 ? ((occupiedRooms / totalRooms) * 100).toFixed(2) : 0;

    res.json({
      success: true,
      data: {
        students: {
          total: totalStudents,
          active: totalStudents
        },
        rooms: {
          total: totalRooms,
          occupied: occupiedRooms,
          available: totalRooms - occupiedRooms,
          occupancyRate: parseFloat(roomOccupancyRate)
        },
        complaints: {
          pending: pendingComplaints,
          overdue: overdueComplaints
        },
        fees: {
          pending: pendingFees,
          overdue: overdueFees,
          totalRevenue: totalRevenue[0]?.total || 0,
          pendingRevenue: pendingRevenue[0]?.total || 0
        },
        leaves: {
          pending: pendingLeaves
        }
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch dashboard statistics',
      error: error.message
    });
  }
};

// Get all students
const getAllStudents = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const search = req.query.search || '';
    const status = req.query.status;

    const query = { role: 'student' };
    
    if (search) {
      query.$or = [
        { firstName: { $regex: search, $options: 'i' } },
        { lastName: { $regex: search, $options: 'i' } },
        { email: { $regex: search, $options: 'i' } },
        { studentId: { $regex: search, $options: 'i' } }
      ];
    }

    if (status) {
      query.isActive = status === 'active';
    }

    const students = await User.find(query)
      .populate('roomNumber')
      .select('-password')
      .sort({ createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await User.countDocuments(query);

    res.json({
      success: true,
      data: {
        students,
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
      message: 'Failed to fetch students',
      error: error.message
    });
  }
};

// Create new student
const createStudent = async (req, res) => {
  try {
    const studentData = {
      ...req.body,
      role: 'student'
    };

    const student = new User(studentData);
    student.generateId();
    await student.save();

    // Send welcome email
    try {
      const emailContent = emailTemplates.welcome(student);
      await sendEmail({
        to: student.email,
        ...emailContent
      });
    } catch (emailError) {
      console.error('Failed to send welcome email:', emailError);
    }

    res.status(201).json({
      success: true,
      message: 'Student created successfully',
      data: { student: student.toJSON() }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create student',
      error: error.message
    });
  }
};

// Update student
const updateStudent = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const student = await User.findByIdAndUpdate(
      id,
      updates,
      { new: true, runValidators: true }
    ).select('-password');

    if (!student) {
      return res.status(404).json({
        success: false,
        message: 'Student not found'
      });
    }

    res.json({
      success: true,
      message: 'Student updated successfully',
      data: { student }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update student',
      error: error.message
    });
  }
};

// Delete student
const deleteStudent = async (req, res) => {
  try {
    const { id } = req.params;

    const student = await User.findById(id);
    if (!student) {
      return res.status(404).json({
        success: false,
        message: 'Student not found'
      });
    }

    // Remove from room if assigned
    if (student.roomNumber) {
      await Room.findByIdAndUpdate(
        student.roomNumber,
        { $pull: { occupants: { student: student._id } } }
      );
    }

    // Soft delete - deactivate instead of removing
    student.isActive = false;
    await student.save();

    res.json({
      success: true,
      message: 'Student deactivated successfully'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to delete student',
      error: error.message
    });
  }
};

// Assign room to student
const assignRoom = async (req, res) => {
  try {
    const { studentId, roomId, bedNumber } = req.body;

    const student = await User.findById(studentId);
    const room = await Room.findById(roomId);

    if (!student || !room) {
      return res.status(404).json({
        success: false,
        message: 'Student or room not found'
      });
    }

    if (student.roomNumber) {
      return res.status(400).json({
        success: false,
        message: 'Student is already assigned to a room'
      });
    }

    if (!room.isAvailable) {
      return res.status(400).json({
        success: false,
        message: 'Room is not available'
      });
    }

    // Add student to room
    await room.addOccupant(studentId, bedNumber);
    
    // Update student's room assignment
    student.roomNumber = roomId;
    await student.save();

    res.json({
      success: true,
      message: 'Room assigned successfully',
      data: {
        student: student.toJSON(),
        room
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to assign room',
      error: error.message
    });
  }
};

// Generate reports
const generateReport = async (req, res) => {
  try {
    const { type, format, startDate, endDate } = req.query;

    let data = [];
    let reportData = {};

    switch (type) {
      case 'students':
        data = await User.find({ role: 'student' })
          .populate('roomNumber')
          .select('-password');
        break;
      
      case 'fees':
        const feeQuery = {};
        if (startDate && endDate) {
          feeQuery.createdAt = {
            $gte: new Date(startDate),
            $lte: new Date(endDate)
          };
        }
        data = await Fee.find(feeQuery).populate('student');
        break;
      
      case 'complaints':
        const complaintQuery = {};
        if (startDate && endDate) {
          complaintQuery.createdAt = {
            $gte: new Date(startDate),
            $lte: new Date(endDate)
          };
        }
        data = await Complaint.find(complaintQuery).populate('reportedBy');
        break;
      
      case 'rooms':
        data = await Room.find().populate('occupants.student');
        break;
      
      case 'leaves':
        const leaveQuery = {};
        if (startDate && endDate) {
          leaveQuery.startDate = {
            $gte: new Date(startDate),
            $lte: new Date(endDate)
          };
        }
        data = await Leave.find(leaveQuery).populate('student');
        break;
      
      default:
        return res.status(400).json({
          success: false,
          message: 'Invalid report type'
        });
    }

    const reportOptions = { startDate, endDate };

    if (format === 'pdf') {
      reportData = await generatePDFReport(data, `${type}_report`, reportOptions);
    } else if (format === 'excel') {
      reportData = await generateExcelReport(data, `${type}_report`, reportOptions);
    } else {
      return res.status(400).json({
        success: false,
        message: 'Invalid format. Use pdf or excel'
      });
    }

    res.json({
      success: true,
      message: 'Report generated successfully',
      data: reportData
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to generate report',
      error: error.message
    });
  }
};

// Get all wardens
const getAllWardens = async (req, res) => {
  try {
    const wardens = await User.find({ role: 'warden' })
      .select('-password')
      .sort({ createdAt: -1 });

    res.json({
      success: true,
      data: { wardens }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch wardens',
      error: error.message
    });
  }
};

// Create new warden
const createWarden = async (req, res) => {
  try {
    const wardenData = {
      ...req.body,
      role: 'warden'
    };

    const warden = new User(wardenData);
    warden.generateId();
    await warden.save();

    res.status(201).json({
      success: true,
      message: 'Warden created successfully',
      data: { warden: warden.toJSON() }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create warden',
      error: error.message
    });
  }
};

module.exports = {
  getDashboardStats,
  getAllStudents,
  createStudent,
  updateStudent,
  deleteStudent,
  assignRoom,
  generateReport,
  getAllWardens,
  createWarden
};