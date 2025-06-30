const User = require('../models/User');
const Room = require('../models/Room');
const Complaint = require('../models/Complaint');
const Fee = require('../models/Fee');
const Leave = require('../models/Leave');
const Announcement = require('../models/Announcement');

// Get student dashboard
const getDashboard = async (req, res) => {
  try {
    const studentId = req.user._id;

    const [
      student,
      pendingComplaints,
      resolvedComplaints,
      pendingFees,
      paidFees,
      activeLeaves,
      recentAnnouncements
    ] = await Promise.all([
      User.findById(studentId).populate('roomNumber').select('-password'),
      Complaint.countDocuments({ reportedBy: studentId, status: { $in: ['pending', 'in_progress'] } }),
      Complaint.countDocuments({ reportedBy: studentId, status: 'resolved' }),
      Fee.countDocuments({ student: studentId, status: { $in: ['pending', 'overdue'] } }),
      Fee.countDocuments({ student: studentId, status: 'paid' }),
      Leave.countDocuments({ student: studentId, status: 'approved', endDate: { $gte: new Date() } }),
      Announcement.find({
        $or: [
          { targetAudience: 'all' },
          { targetAudience: 'students' },
          { targetUsers: studentId }
        ],
        status: 'published',
        $or: [
          { expiryDate: { $exists: false } },
          { expiryDate: { $gte: new Date() } }
        ]
      }).sort({ createdAt: -1 }).limit(5)
    ]);

    // Calculate total pending fees
    const pendingFeesAmount = await Fee.aggregate([
      { $match: { student: studentId, status: { $in: ['pending', 'overdue', 'partial'] } } },
      { $group: { _id: null, total: { $sum: '$balanceAmount' } } }
    ]);

    res.json({
      success: true,
      data: {
        student,
        stats: {
          complaints: {
            pending: pendingComplaints,
            resolved: resolvedComplaints
          },
          fees: {
            pending: pendingFees,
            paid: paidFees,
            pendingAmount: pendingFeesAmount[0]?.total || 0
          },
          leaves: {
            active: activeLeaves
          }
        },
        recentAnnouncements
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch dashboard data',
      error: error.message
    });
  }
};

// Get student's complaints
const getMyComplaints = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    const query = { reportedBy: req.user._id };
    if (status) {
      query.status = status;
    }

    const complaints = await Complaint.find(query)
      .populate('assignedTo', 'firstName lastName')
      .populate('roomNumber', 'roomNumber block floor')
      .sort({ createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Complaint.countDocuments(query);

    res.json({
      success: true,
      data: {
        complaints,
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
      message: 'Failed to fetch complaints',
      error: error.message
    });
  }
};

// Get student's fees
const getMyFees = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;
    const year = req.query.year;

    const query = { student: req.user._id };
    if (status) {
      query.status = status;
    }
    if (year) {
      query.year = parseInt(year);
    }

    const fees = await Fee.find(query)
      .sort({ dueDate: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Fee.countDocuments(query);

    // Get fee summary
    const summary = await Fee.aggregate([
      { $match: { student: req.user._id } },
      {
        $group: {
          _id: '$status',
          count: { $sum: 1 },
          totalAmount: { $sum: '$finalAmount' },
          paidAmount: { $sum: '$paidAmount' }
        }
      }
    ]);

    res.json({
      success: true,
      data: {
        fees,
        summary,
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
      message: 'Failed to fetch fees',
      error: error.message
    });
  }
};

// Get student's leaves
const getMyLeaves = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    const query = { student: req.user._id };
    if (status) {
      query.status = status;
    }

    const leaves = await Leave.find(query)
      .populate('approvedBy', 'firstName lastName')
      .sort({ createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Leave.countDocuments(query);

    res.json({
      success: true,
      data: {
        leaves,
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
      message: 'Failed to fetch leaves',
      error: error.message
    });
  }
};

// Get announcements for student
const getAnnouncements = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const type = req.query.type;

    const query = {
      $or: [
        { targetAudience: 'all' },
        { targetAudience: 'students' },
        { targetUsers: req.user._id }
      ],
      status: 'published',
      $or: [
        { expiryDate: { $exists: false } },
        { expiryDate: { $gte: new Date() } }
      ]
    };

    if (type) {
      query.type = type;
    }

    const announcements = await Announcement.find(query)
      .populate('createdBy', 'firstName lastName')
      .sort({ isSticky: -1, createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Announcement.countDocuments(query);

    res.json({
      success: true,
      data: {
        announcements,
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
      message: 'Failed to fetch announcements',
      error: error.message
    });
  }
};

// Mark announcement as read
const markAnnouncementRead = async (req, res) => {
  try {
    const { id } = req.params;

    const announcement = await Announcement.findById(id);
    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    await announcement.markAsRead(req.user._id);

    res.json({
      success: true,
      message: 'Announcement marked as read'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to mark announcement as read',
      error: error.message
    });
  }
};

// Get room details
const getRoomDetails = async (req, res) => {
  try {
    const student = await User.findById(req.user._id).populate('roomNumber');
    
    if (!student.roomNumber) {
      return res.status(404).json({
        success: false,
        message: 'No room assigned'
      });
    }

    const room = await Room.findById(student.roomNumber._id)
      .populate('occupants.student', 'firstName lastName email phone');

    res.json({
      success: true,
      data: { room }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch room details',
      error: error.message
    });
  }
};

// Update profile
const updateProfile = async (req, res) => {
  try {
    const allowedUpdates = [
      'firstName', 'lastName', 'phone', 'address', 
      'emergencyContact', 'course', 'year'
    ];
    
    const updates = {};
    Object.keys(req.body).forEach(key => {
      if (allowedUpdates.includes(key)) {
        updates[key] = req.body[key];
      }
    });

    const student = await User.findByIdAndUpdate(
      req.user._id,
      updates,
      { new: true, runValidators: true }
    ).select('-password');

    res.json({
      success: true,
      message: 'Profile updated successfully',
      data: { student }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update profile',
      error: error.message
    });
  }
};

module.exports = {
  getDashboard,
  getMyComplaints,
  getMyFees,
  getMyLeaves,
  getAnnouncements,
  markAnnouncementRead,
  getRoomDetails,
  updateProfile
};