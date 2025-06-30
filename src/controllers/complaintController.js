const Complaint = require('../models/Complaint');
const User = require('../models/User');
const Room = require('../models/Room');
const { uploadMultiple } = require('../utils/fileUpload');

// Get all complaints
const getAllComplaints = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;
    const category = req.query.category;
    const priority = req.query.priority;

    const query = {};
    if (status) query.status = status;
    if (category) query.category = category;
    if (priority) query.priority = priority;

    // If user is student, only show their complaints
    if (req.user.role === 'student') {
      query.reportedBy = req.user._id;
    }

    const complaints = await Complaint.find(query)
      .populate('reportedBy', 'firstName lastName email')
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

// Get single complaint
const getComplaint = async (req, res) => {
  try {
    const { id } = req.params;

    const complaint = await Complaint.findById(id)
      .populate('reportedBy', 'firstName lastName email phone')
      .populate('assignedTo', 'firstName lastName email')
      .populate('roomNumber', 'roomNumber block floor')
      .populate('statusHistory.updatedBy', 'firstName lastName');

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found'
      });
    }

    // Check if user has permission to view this complaint
    if (req.user.role === 'student' && complaint.reportedBy._id.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    res.json({
      success: true,
      data: { complaint }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch complaint',
      error: error.message
    });
  }
};

// Create new complaint
const createComplaint = async (req, res) => {
  try {
    const complaintData = {
      ...req.body,
      reportedBy: req.user._id
    };

    // If student, auto-assign their room
    if (req.user.role === 'student' && req.user.roomNumber) {
      complaintData.roomNumber = req.user.roomNumber;
    }

    const complaint = new Complaint(complaintData);
    await complaint.save();

    const populatedComplaint = await Complaint.findById(complaint._id)
      .populate('reportedBy', 'firstName lastName email')
      .populate('roomNumber', 'roomNumber block floor');

    res.status(201).json({
      success: true,
      message: 'Complaint created successfully',
      data: { complaint: populatedComplaint }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create complaint',
      error: error.message
    });
  }
};

// Update complaint
const updateComplaint = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const complaint = await Complaint.findById(id);
    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student') {
      if (complaint.reportedBy.toString() !== req.user._id.toString()) {
        return res.status(403).json({
          success: false,
          message: 'Access denied'
        });
      }
      // Students can only update certain fields
      const allowedUpdates = ['title', 'description', 'location'];
      const filteredUpdates = {};
      Object.keys(updates).forEach(key => {
        if (allowedUpdates.includes(key)) {
          filteredUpdates[key] = updates[key];
        }
      });
      updates = filteredUpdates;
    }

    Object.assign(complaint, updates);
    await complaint.save();

    const updatedComplaint = await Complaint.findById(id)
      .populate('reportedBy', 'firstName lastName email')
      .populate('assignedTo', 'firstName lastName')
      .populate('roomNumber', 'roomNumber block floor');

    res.json({
      success: true,
      message: 'Complaint updated successfully',
      data: { complaint: updatedComplaint }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update complaint',
      error: error.message
    });
  }
};

// Update complaint status
const updateComplaintStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status, comment, assignedTo, resolution } = req.body;

    const complaint = await Complaint.findById(id);
    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found'
      });
    }

    // Only admin and warden can update status
    if (!['admin', 'warden'].includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    // Update status
    await complaint.updateStatus(status, req.user._id, comment);

    // Update assigned person if provided
    if (assignedTo) {
      complaint.assignedTo = assignedTo;
    }

    // Add resolution details if provided
    if (resolution && (status === 'resolved' || status === 'closed')) {
      complaint.resolution = {
        ...resolution,
        resolvedBy: req.user._id,
        resolvedAt: new Date()
      };
    }

    await complaint.save();

    const updatedComplaint = await Complaint.findById(id)
      .populate('reportedBy', 'firstName lastName email')
      .populate('assignedTo', 'firstName lastName')
      .populate('roomNumber', 'roomNumber block floor');

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

// Delete complaint
const deleteComplaint = async (req, res) => {
  try {
    const { id } = req.params;

    const complaint = await Complaint.findById(id);
    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student') {
      if (complaint.reportedBy.toString() !== req.user._id.toString()) {
        return res.status(403).json({
          success: false,
          message: 'Access denied'
        });
      }
      // Students can only delete pending complaints
      if (complaint.status !== 'pending') {
        return res.status(400).json({
          success: false,
          message: 'Cannot delete complaint that is already being processed'
        });
      }
    }

    await Complaint.findByIdAndDelete(id);

    res.json({
      success: true,
      message: 'Complaint deleted successfully'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to delete complaint',
      error: error.message
    });
  }
};

// Upload complaint images
const uploadComplaintImages = async (req, res) => {
  try {
    const { id } = req.params;

    const complaint = await Complaint.findById(id);
    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found'
      });
    }

    // Check permissions
    if (req.user.role === 'student' && complaint.reportedBy.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    if (!req.files || req.files.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'No files uploaded'
      });
    }

    const imagePaths = req.files.map(file => file.path);
    complaint.images = [...complaint.images, ...imagePaths];
    await complaint.save();

    res.json({
      success: true,
      message: 'Images uploaded successfully',
      data: { images: imagePaths }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to upload images',
      error: error.message
    });
  }
};

// Get complaint statistics
const getComplaintStats = async (req, res) => {
  try {
    const [
      totalComplaints,
      pendingComplaints,
      inProgressComplaints,
      resolvedComplaints,
      complaintsByCategory,
      complaintsByPriority,
      avgResolutionTime
    ] = await Promise.all([
      Complaint.countDocuments(),
      Complaint.countDocuments({ status: 'pending' }),
      Complaint.countDocuments({ status: 'in_progress' }),
      Complaint.countDocuments({ status: 'resolved' }),
      Complaint.aggregate([
        { $group: { _id: '$category', count: { $sum: 1 } } }
      ]),
      Complaint.aggregate([
        { $group: { _id: '$priority', count: { $sum: 1 } } }
      ]),
      Complaint.aggregate([
        { $match: { status: 'resolved', actualResolutionDate: { $exists: true } } },
        {
          $group: {
            _id: null,
            avgTime: {
              $avg: {
                $divide: [
                  { $subtract: ['$actualResolutionDate', '$createdAt'] },
                  1000 * 60 * 60 // Convert to hours
                ]
              }
            }
          }
        }
      ])
    ]);

    res.json({
      success: true,
      data: {
        total: totalComplaints,
        pending: pendingComplaints,
        inProgress: inProgressComplaints,
        resolved: resolvedComplaints,
        byCategory: complaintsByCategory,
        byPriority: complaintsByPriority,
        avgResolutionTimeHours: avgResolutionTime[0]?.avgTime || 0
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch complaint statistics',
      error: error.message
    });
  }
};

module.exports = {
  getAllComplaints,
  getComplaint,
  createComplaint,
  updateComplaint,
  updateComplaintStatus,
  deleteComplaint,
  uploadComplaintImages,
  getComplaintStats
};