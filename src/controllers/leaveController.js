const Leave = require('../models/Leave');
const User = require('../models/User');
const { sendEmail, emailTemplates } = require('../utils/email');

// Get all leaves
const getAllLeaves = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;
    const leaveType = req.query.leaveType;

    const query = {};
    if (status) query.status = status;
    if (leaveType) query.leaveType = leaveType;

    // If user is student, only show their leaves
    if (req.user.role === 'student') {
      query.student = req.user._id;
    }

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
      message: 'Failed to fetch leaves',
      error: error.message
    });
  }
};

// Get single leave
const getLeave = async (req, res) => {
  try {
    const { id } = req.params;

    const leave = await Leave.findById(id)
      .populate('student', 'firstName lastName email phone studentId')
      .populate('approvedBy', 'firstName lastName')
      .populate('statusHistory.updatedBy', 'firstName lastName')
      .populate('extensionRequests.approvedBy', 'firstName lastName');

    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student' && leave.student._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    res.json({
      success: true,
      data: { leave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch leave',
      error: error.message
    });
  }
};

// Create new leave application
const createLeave = async (req, res) => {
  try {
    const leaveData = {
      ...req.body,
      student: req.user._id
    };

    const leave = new Leave(leaveData);
    await leave.save();

    const populatedLeave = await Leave.findById(leave._id)
      .populate('student', 'firstName lastName email');

    res.status(201).json({
      success: true,
      message: 'Leave application submitted successfully',
      data: { leave: populatedLeave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create leave application',
      error: error.message
    });
  }
};

// Update leave application
const updateLeave = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const leave = await Leave.findById(id);
    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student') {
      if (leave.student.toString() !== req.user._id.toString()) {
        return res.status(403).json({
          success: false,
          message: 'Access denied'
        });
      }
      // Students can only update pending leaves
      if (leave.status !== 'pending') {
        return res.status(400).json({
          success: false,
          message: 'Cannot update leave application that is already processed'
        });
      }
      // Students can only update certain fields
      const allowedUpdates = ['reason', 'emergencyContact', 'destination'];
      const filteredUpdates = {};
      Object.keys(updates).forEach(key => {
        if (allowedUpdates.includes(key)) {
          filteredUpdates[key] = updates[key];
        }
      });
      updates = filteredUpdates;
    }

    Object.assign(leave, updates);
    await leave.save();

    const updatedLeave = await Leave.findById(id)
      .populate('student', 'firstName lastName email')
      .populate('approvedBy', 'firstName lastName');

    res.json({
      success: true,
      message: 'Leave application updated successfully',
      data: { leave: updatedLeave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update leave application',
      error: error.message
    });
  }
};

// Update leave status (approve/reject)
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

    // Only admin and warden can update status
    if (!['admin', 'warden'].includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
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

// Request leave extension
const requestExtension = async (req, res) => {
  try {
    const { id } = req.params;
    const { newEndDate, reason } = req.body;

    const leave = await Leave.findById(id);
    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student' && leave.student.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    // Can only request extension for approved leaves
    if (leave.status !== 'approved') {
      return res.status(400).json({
        success: false,
        message: 'Can only request extension for approved leaves'
      });
    }

    await leave.requestExtension(newEndDate, reason);

    res.json({
      success: true,
      message: 'Extension request submitted successfully',
      data: { leave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to request extension',
      error: error.message
    });
  }
};

// Approve/Reject extension request
const updateExtensionStatus = async (req, res) => {
  try {
    const { id, extensionId } = req.params;
    const { status, comment } = req.body;

    const leave = await Leave.findById(id);
    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    // Only admin and warden can update extension status
    if (!['admin', 'warden'].includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    const extension = leave.extensionRequests.id(extensionId);
    if (!extension) {
      return res.status(404).json({
        success: false,
        message: 'Extension request not found'
      });
    }

    extension.status = status;
    extension.approvedBy = req.user._id;
    extension.approvedDate = new Date();

    // If approved, update the leave end date
    if (status === 'approved') {
      leave.endDate = extension.requestedEndDate;
      leave.isExtended = true;
    }

    await leave.save();

    res.json({
      success: true,
      message: `Extension ${status} successfully`,
      data: { leave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update extension status',
      error: error.message
    });
  }
};

// Mark return from leave
const markReturn = async (req, res) => {
  try {
    const { id } = req.params;
    const { actualReturnDate } = req.body;

    const leave = await Leave.findById(id);
    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    // Only admin and warden can mark return
    if (!['admin', 'warden'].includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    leave.actualReturnDate = actualReturnDate || new Date();
    await leave.save();

    res.json({
      success: true,
      message: 'Return marked successfully',
      data: { leave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to mark return',
      error: error.message
    });
  }
};

// Cancel leave application
const cancelLeave = async (req, res) => {
  try {
    const { id } = req.params;
    const { reason } = req.body;

    const leave = await Leave.findById(id);
    if (!leave) {
      return res.status(404).json({
        success: false,
        message: 'Leave application not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student' && leave.student.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    // Can only cancel pending or approved leaves
    if (!['pending', 'approved'].includes(leave.status)) {
      return res.status(400).json({
        success: false,
        message: 'Cannot cancel this leave application'
      });
    }

    await leave.updateStatus('cancelled', req.user._id, reason);

    res.json({
      success: true,
      message: 'Leave application cancelled successfully',
      data: { leave }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to cancel leave application',
      error: error.message
    });
  }
};

// Get leave statistics
const getLeaveStats = async (req, res) => {
  try {
    const year = parseInt(req.query.year) || new Date().getFullYear();

    const [
      totalLeaves,
      pendingLeaves,
      approvedLeaves,
      rejectedLeaves,
      activeLeaves,
      overdueLeaves,
      leavesByType,
      monthlyStats
    ] = await Promise.all([
      Leave.countDocuments({
        $expr: { $eq: [{ $year: '$startDate' }, year] }
      }),
      Leave.countDocuments({ status: 'pending' }),
      Leave.countDocuments({ status: 'approved' }),
      Leave.countDocuments({ status: 'rejected' }),
      Leave.countDocuments({
        status: 'approved',
        startDate: { $lte: new Date() },
        endDate: { $gte: new Date() }
      }),
      Leave.countDocuments({
        status: 'approved',
        endDate: { $lt: new Date() },
        actualReturnDate: { $exists: false }
      }),
      Leave.aggregate([
        { $match: { $expr: { $eq: [{ $year: '$startDate' }, year] } } },
        { $group: { _id: '$leaveType', count: { $sum: 1 } } }
      ]),
      Leave.aggregate([
        { $match: { $expr: { $eq: [{ $year: '$startDate' }, year] } } },
        {
          $group: {
            _id: { $month: '$startDate' },
            count: { $sum: 1 },
            avgDuration: { $avg: '$durationDays' }
          }
        },
        { $sort: { _id: 1 } }
      ])
    ]);

    res.json({
      success: true,
      data: {
        year,
        total: totalLeaves,
        pending: pendingLeaves,
        approved: approvedLeaves,
        rejected: rejectedLeaves,
        active: activeLeaves,
        overdue: overdueLeaves,
        byType: leavesByType,
        monthlyStats
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch leave statistics',
      error: error.message
    });
  }
};

module.exports = {
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
};