const User = require('../models/User');
const Room = require('../models/Room');
const Complaint = require('../models/Complaint');
const Fee = require('../models/Fee');
const Leave = require('../models/Leave');
const Announcement = require('../models/Announcement');
const { sendEmail, emailTemplates } = require('../utils/email');

// Get warden dashboard
const getDashboard = async (req, res) => {
  try {
    const [
      totalStudents,
      totalRooms,
      occupiedRooms,
      pendingComplaints,
      urgentComplaints,
      pendingLeaves,
      overdueLeaves,
      recentComplaints
    ] = await Promise.all([
      User.countDocuments({ role: 'student', isActive: true }),
      Room.countDocuments({ isActive: true }),
      Room.countDocuments({ status: 'occupied' }),
      Complaint.countDocuments({ status: 'pending' }),
      Complaint.countDocuments({ priority: 'urgent', status: { $in: ['pending', 'in_progress'] } }),
      Leave.countDocuments({ status: 'pending' }),
      Leave.countDocuments({ 
        status: 'approved',
        endDate: { $lt: new Date() },
        actualReturnDate: { $exists: false }
      }),
      Complaint.find({ status: { $in: ['pending', 'in_progress'] } })
        .populate('reportedBy', 'firstName lastName')
        .populate('roomNumber', 'roomNumber')
        .sort({ createdAt: -1 })
        .limit(5)
    ]);

    res.json({
      success: true,
      data: {
        stats: {
          students: totalStudents,
          rooms: {
            total: totalRooms,
            occupied: occupiedRooms,
            available: totalRooms - occupiedRooms
          },
          complaints: {
            pending: pendingComplaints,
            urgent: urgentComplaints
          },
          leaves: {
            pending: pendingLeaves,
            overdue: overdueLeaves
          }
        },
        recentComplaints
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

// Get all complaints for warden
const getAllComplaints = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;
    const priority = req.query.priority;
    const category = req.query.category;

    const query = {};
    if (status) query.status = status;
    if (priority) query.priority = priority;
    if (category) query.category = category;

    const complaints = await Complaint.find(query)
      .populate('reportedBy', 'firstName lastName email')
      .populate('assignedTo', 'firstName lastName')
      .populate('roomNumber', 'roomNumber block floor')
      .sort({ priority: -1, createdAt: -1 })
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

// Update complaint status
const updateComplaintStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status, comment, assignedTo } = req.body;

    const complaint = await Complaint.findById(id);
    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found'
      });
    }

    // Update complaint
    await complaint.updateStatus(status, req.user._id, comment);

    if (assignedTo) {
      complaint.assignedTo = assignedTo;
      await complaint.save();
    }

    // Send notification email to student
    const student = await User.findById(complaint.reportedBy);
    if (student) {
      try {
        const emailContent = emailTemplates.complaintUpdate(complaint, student);
        await sendEmail({
          to: student.email,
          ...emailContent
        });
      } catch (emailError) {
        console.error('Failed to send complaint update email:', emailError);
      }
    }

    const updatedComplaint = await Complaint.findById(id)
      .populate('reportedBy', 'firstName lastName email')
      .populate('assignedTo', 'firstName lastName');

    res.json({
      success: true,
      message: 'Complaint status updated successfully',
      data: { complaint: updatedComplaint }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update complaint status',
      error: error.message
    });
  }
};

// Get all leave applications
const getAllLeaves = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    const query = {};
    if (status) query.status = status;

    const leaves = await Leave.find(query)
      .populate('student', 'firstName lastName email studentId')
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
      message: 'Failed to fetch leave applications',
      error: error.message
    });
  }
};

// Approve/Reject leave
const updateLeaveStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status, comment } = req.body;

    const leave = await Leave.findById(id);
    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    await leave.updateStatus(status, req.user._id, comment);

    // Send notification email to student
    const student = await User.findById(leave.student);
    if (student) {
      try {
        const emailContent = emailTemplates.leaveApproval(leave, student, status);
        await sendEmail({
          to: student.email,
          ...emailContent
        });
      } catch (emailError) {
        console.error('Failed to send leave approval email:', emailError);
      }
    }

    const updatedLeave = await Leave.findById(id)
      .populate('student', 'firstName lastName email')
      .populate('approvedBy', 'firstName lastName');

    res.json({
      success: true,
      message: `Leave ${status} successfully`,
      data: { leave: updatedLeave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update leave status',
      error: error.message
    });
  }
};

// Get all students under warden supervision
const getStudents = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const search = req.query.search || '';

    const query = { role: 'student', isActive: true };
    
    if (search) {
      query.$or = [
        { firstName: { $regex: search, $options: 'i' } },
        { lastName: { $regex: search, $options: 'i' } },
        { email: { $regex: search, $options: 'i' } },
        { studentId: { $regex: search, $options: 'i' } }
      ];
    }

    const students = await User.find(query)
      .populate('roomNumber', 'roomNumber block floor')
      .select('-password')
      .sort({ firstName: 1 })
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

// Get room occupancy details
const getRoomOccupancy = async (req, res) => {
  try {
    const rooms = await Room.find({ isActive: true })
      .populate('occupants.student', 'firstName lastName email studentId')
      .sort({ block: 1, floor: 1, roomNumber: 1 });

    const occupancyStats = {
      total: rooms.length,
      occupied: rooms.filter(room => room.currentOccupancy > 0).length,
      available: rooms.filter(room => room.isAvailable).length,
      maintenance: rooms.filter(room => room.status === 'maintenance').length
    };

    res.json({
      success: true,
      data: {
        rooms,
        stats: occupancyStats
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch room occupancy',
      error: error.message
    });
  }
};

// Create announcement
const createAnnouncement = async (req, res) => {
  try {
    const announcementData = {
      ...req.body,
      createdBy: req.user._id
    };

    const announcement = new Announcement(announcementData);
    await announcement.save();

    // If published, send emails to target audience
    if (announcement.status === 'published') {
      let recipients = [];

      switch (announcement.targetAudience) {
        case 'all':
          recipients = await User.find({ isActive: true });
          break;
        case 'students':
          recipients = await User.find({ role: 'student', isActive: true });
          break;
        case 'specific_users':
          recipients = await User.find({ _id: { $in: announcement.targetUsers } });
          break;
      }

      if (recipients.length > 0) {
        try {
          const emailContent = emailTemplates.announcement(announcement);
          for (const recipient of recipients) {
            await sendEmail({
              to: recipient.email,
              ...emailContent
            });
          }
          announcement.emailSent = true;
          await announcement.save();
        } catch (emailError) {
          console.error('Failed to send announcement emails:', emailError);
        }
      }
    }

    res.status(201).json({
      success: true,
      message: 'Announcement created successfully',
      data: { announcement }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create announcement',
      error: error.message
    });
  }
};

// Get maintenance requests
const getMaintenanceRequests = async (req, res) => {
  try {
    const maintenanceComplaints = await Complaint.find({
      category: { $in: ['plumbing', 'electrical', 'maintenance'] },
      status: { $in: ['pending', 'in_progress'] }
    })
      .populate('reportedBy', 'firstName lastName')
      .populate('roomNumber', 'roomNumber block floor')
      .sort({ priority: -1, createdAt: -1 });

    res.json({
      success: true,
      data: { complaints: maintenanceComplaints }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch maintenance requests',
      error: error.message
    });
  }
};

module.exports = {
  getDashboard,
  getAllComplaints,
  updateComplaintStatus,
  getAllLeaves,
  updateLeaveStatus,
  getStudents,
  getRoomOccupancy,
  createAnnouncement,
  getMaintenanceRequests
};