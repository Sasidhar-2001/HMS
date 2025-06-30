const Fee = require('../models/Fee');
const User = require('../models/User');
const { sendEmail, emailTemplates } = require('../utils/email');

// Get all fees
const getAllFees = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;
    const feeType = req.query.feeType;
    const month = req.query.month;
    const year = req.query.year;

    const query = {};
    if (status) query.status = status;
    if (feeType) query.feeType = feeType;
    if (month) query.month = parseInt(month);
    if (year) query.year = parseInt(year);

    // If user is student, only show their fees
    if (req.user.role === 'student') {
      query.student = req.user._id;
    }

    const fees = await Fee.find(query)
      .populate('student', 'firstName lastName email studentId')
      .populate('room', 'roomNumber block')
      .sort({ dueDate: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Fee.countDocuments(query);

    res.json({
      success: true,
      data: {
        fees,
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

// Get single fee
const getFee = async (req, res) => {
  try {
    const { id } = req.params;

    const fee = await Fee.find(id)
      .populate('student', 'firstName lastName email phone studentId')
      .populate('room', 'roomNumber block floor')
      .populate('createdBy', 'firstName lastName')
      .populate('paymentHistory.paidBy', 'firstName lastName');

    if (!fee) {
      return res.status(404).json({
        success: false,
        message: 'Fee record not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student' && fee.student._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    res.json({
      success: true,
      data: { fee }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch fee',
      error: error.message
    });
  }
};

// Create new fee
const createFee = async (req, res) => {
  try {
    const feeData = {
      ...req.body,
      createdBy: req.user._id
    };

    const fee = new Fee(feeData);
    await fee.save();

    const populatedFee = await Fee.findById(fee._id)
      .populate('student', 'firstName lastName email')
      .populate('room', 'roomNumber block');

    // Send fee notification email
    const student = await User.findById(fee.student);
    if (student) {
      try {
        const emailContent = emailTemplates.feeReminder(fee, student);
        await sendEmail({
          to: student.email,
          ...emailContent
        });
      } catch (emailError) {
        console.error('Failed to send fee notification email:', emailError);
      }
    }

    res.status(201).json({
      success: true,
      message: 'Fee created successfully',
      data: { fee: populatedFee }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create fee',
      error: error.message
    });
  }
};

// Update fee
const updateFee = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = {
      ...req.body,
      updatedBy: req.user._id
    };

    const fee = await Fee.findByIdAndUpdate(
      id,
      updates,
      { new: true, runValidators: true }
    ).populate('student', 'firstName lastName email');

    if (!fee) {
      return res.status(404).json({
        success: false,
        message: 'Fee record not found'
      });
    }

    res.json({
      success: true,
      message: 'Fee updated successfully',
      data: { fee }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update fee',
      error: error.message
    });
  }
};

// Add payment
const addPayment = async (req, res) => {
  try {
    const { id } = req.params;
    const paymentData = {
      ...req.body,
      paidBy: req.user._id
    };

    const fee = await Fee.findById(id);
    if (!fee) {
      return res.status(404).json({
        success: false,
        message: 'Fee record not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student' && fee.student.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    await fee.addPayment(paymentData);

    const updatedFee = await Fee.findById(id)
      .populate('student', 'firstName lastName email')
      .populate('paymentHistory.paidBy', 'firstName lastName');

    res.json({
      success: true,
      message: 'Payment added successfully',
      data: { fee: updatedFee }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to add payment',
      error: error.message
    });
  }
};

// Send fee reminder
const sendFeeReminder = async (req, res) => {
  try {
    const { id } = req.params;
    const { type = 'email' } = req.body;

    const fee = await Fee.findById(id).populate('student');
    if (!fee) {
      return res.status(404).json({
        success: false,
        message: 'Fee record not found'
      });
    }

    if (fee.status === 'paid') {
      return res.status(400).json({
        success: false,
        message: 'Fee is already paid'
      });
    }

    // Send reminder email
    if (type === 'email') {
      try {
        const emailContent = emailTemplates.feeReminder(fee, fee.student);
        await sendEmail({
          to: fee.student.email,
          ...emailContent
        });

        await fee.addReminder('email', 'sent');

        res.json({
          success: true,
          message: 'Fee reminder sent successfully'
        });
      } catch (emailError) {
        await fee.addReminder('email', 'failed');
        throw emailError;
      }
    } else {
      res.status(400).json({
        success: false,
        message: 'Invalid reminder type'
      });
    }
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to send fee reminder',
      error: error.message
    });
  }
};

// Send bulk fee reminders
const sendBulkReminders = async (req, res) => {
  try {
    const { status = 'overdue', type = 'email' } = req.body;

    const fees = await Fee.find({ status }).populate('student');

    if (fees.length === 0) {
      return res.json({
        success: true,
        message: 'No fees found for reminder',
        data: { sent: 0, failed: 0 }
      });
    }

    let sent = 0;
    let failed = 0;

    for (const fee of fees) {
      try {
        const emailContent = emailTemplates.feeReminder(fee, fee.student);
        await sendEmail({
          to: fee.student.email,
          ...emailContent
        });
        await fee.addReminder(type, 'sent');
        sent++;
      } catch (error) {
        await fee.addReminder(type, 'failed');
        failed++;
      }
    }

    res.json({
      success: true,
      message: 'Bulk reminders processed',
      data: { sent, failed, total: fees.length }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to send bulk reminders',
      error: error.message
    });
  }
};

// Get fee statistics
const getFeeStats = async (req, res) => {
  try {
    const year = parseInt(req.query.year) || new Date().getFullYear();

    const [
      totalFees,
      paidFees,
      pendingFees,
      overdueFees,
      totalRevenue,
      pendingRevenue,
      monthlyStats,
      feeTypeStats
    ] = await Promise.all([
      Fee.countDocuments({ year }),
      Fee.countDocuments({ year, status: 'paid' }),
      Fee.countDocuments({ year, status: 'pending' }),
      Fee.countDocuments({ year, status: 'overdue' }),
      Fee.aggregate([
        { $match: { year, status: 'paid' } },
        { $group: { _id: null, total: { $sum: '$finalAmount' } } }
      ]),
      Fee.aggregate([
        { $match: { year, status: { $in: ['pending', 'overdue', 'partial'] } } },
        { $group: { _id: null, total: { $sum: '$balanceAmount' } } }
      ]),
      Fee.aggregate([
        { $match: { year } },
        {
          $group: {
            _id: '$month',
            totalAmount: { $sum: '$finalAmount' },
            paidAmount: { $sum: '$paidAmount' },
            count: { $sum: 1 }
          }
        },
        { $sort: { _id: 1 } }
      ]),
      Fee.aggregate([
        { $match: { year } },
        {
          $group: {
            _id: '$feeType',
            totalAmount: { $sum: '$finalAmount' },
            paidAmount: { $sum: '$paidAmount' },
            count: { $sum: 1 }
          }
        }
      ])
    ]);

    const collectionRate = totalFees > 0 ? ((paidFees / totalFees) * 100).toFixed(2) : 0;

    res.json({
      success: true,
      data: {
        year,
        total: totalFees,
        paid: paidFees,
        pending: pendingFees,
        overdue: overdueFees,
        totalRevenue: totalRevenue[0]?.total || 0,
        pendingRevenue: pendingRevenue[0]?.total || 0,
        collectionRate: parseFloat(collectionRate),
        monthlyStats,
        feeTypeStats
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch fee statistics',
      error: error.message
    });
  }
};

// Get defaulters list
const getDefaulters = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;

    const defaulters = await Fee.find({
      status: { $in: ['overdue', 'partial'] },
      balanceAmount: { $gt: 0 }
    })
      .populate('student', 'firstName lastName email phone studentId')
      .populate('room', 'roomNumber block')
      .sort({ dueDate: 1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Fee.countDocuments({
      status: { $in: ['overdue', 'partial'] },
      balanceAmount: { $gt: 0 }
    });

    res.json({
      success: true,
      data: {
        defaulters,
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
      message: 'Failed to fetch defaulters list',
      error: error.message
    });
  }
};

module.exports = {
  getAllFees,
  getFee,
  createFee,
  updateFee,
  addPayment,
  sendFeeReminder,
  sendBulkReminders,
  getFeeStats,
  getDefaulters
};