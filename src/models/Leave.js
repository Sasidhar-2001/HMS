const mongoose = require('mongoose');

const leaveSchema = new mongoose.Schema({
  leaveId: {
    type: String,
    unique: true,
    required: true
  },
  student: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'Student is required']
  },
  leaveType: {
    type: String,
    enum: ['home', 'medical', 'emergency', 'personal', 'academic', 'other'],
    required: [true, 'Leave type is required']
  },
  startDate: {
    type: Date,
    required: [true, 'Start date is required']
  },
  endDate: {
    type: Date,
    required: [true, 'End date is required']
  },
  reason: {
    type: String,
    required: [true, 'Reason is required'],
    maxlength: [500, 'Reason cannot exceed 500 characters']
  },
  status: {
    type: String,
    enum: ['pending', 'approved', 'rejected', 'cancelled'],
    default: 'pending'
  },
  appliedDate: {
    type: Date,
    default: Date.now
  },
  approvedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  approvedDate: Date,
  rejectionReason: String,
  emergencyContact: {
    name: String,
    phone: String,
    relation: String
  },
  destination: {
    address: String,
    city: String,
    state: String,
    pincode: String
  },
  attachments: [String],
  actualReturnDate: Date,
  isExtended: {
    type: Boolean,
    default: false
  },
  extensionRequests: [{
    requestedEndDate: Date,
    reason: String,
    requestedDate: Date,
    status: {
      type: String,
      enum: ['pending', 'approved', 'rejected']
    },
    approvedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    approvedDate: Date
  }],
  statusHistory: [{
    status: {
      type: String,
      enum: ['pending', 'approved', 'rejected', 'cancelled']
    },
    updatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    updatedAt: {
      type: Date,
      default: Date.now
    },
    comment: String
  }],
  parentApproval: {
    required: {
      type: Boolean,
      default: false
    },
    obtained: {
      type: Boolean,
      default: false
    },
    contactNumber: String,
    approvalDate: Date
  },
  medicalCertificate: {
    required: {
      type: Boolean,
      default: false
    },
    uploaded: {
      type: Boolean,
      default: false
    },
    fileName: String,
    uploadDate: Date
  }
}, {
  timestamps: true
});

// Indexes for better query performance
leaveSchema.index({ leaveId: 1 });
leaveSchema.index({ student: 1 });
leaveSchema.index({ status: 1 });
leaveSchema.index({ startDate: 1 });
leaveSchema.index({ endDate: 1 });
leaveSchema.index({ leaveType: 1 });

// Pre-save middleware to generate leave ID
leaveSchema.pre('save', function(next) {
  if (!this.leaveId) {
    const year = new Date().getFullYear();
    const month = String(new Date().getMonth() + 1).padStart(2, '0');
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
    this.leaveId = `LV${year}${month}${random}`;
  }
  
  // Set requirements based on leave type
  if (this.leaveType === 'medical') {
    this.medicalCertificate.required = true;
  }
  
  // Require parent approval for leaves longer than 7 days
  const leaveDays = Math.ceil((this.endDate - this.startDate) / (1000 * 60 * 60 * 24));
  if (leaveDays > 7) {
    this.parentApproval.required = true;
  }
  
  next();
});

// Validation for date range
leaveSchema.pre('validate', function(next) {
  if (this.startDate && this.endDate && this.startDate >= this.endDate) {
    next(new Error('End date must be after start date'));
  } else {
    next();
  }
});

// Method to update status
leaveSchema.methods.updateStatus = function(newStatus, updatedBy, comment) {
  this.status = newStatus;
  
  this.statusHistory.push({
    status: newStatus,
    updatedBy: updatedBy,
    updatedAt: new Date(),
    comment: comment
  });
  
  if (newStatus === 'approved') {
    this.approvedBy = updatedBy;
    this.approvedDate = new Date();
  } else if (newStatus === 'rejected') {
    this.rejectionReason = comment;
  }
  
  return this.save();
};

// Method to request extension
leaveSchema.methods.requestExtension = function(newEndDate, reason) {
  this.extensionRequests.push({
    requestedEndDate: newEndDate,
    reason: reason,
    requestedDate: new Date(),
    status: 'pending'
  });
  
  return this.save();
};

// Virtual for leave duration in days
leaveSchema.virtual('durationDays').get(function() {
  if (this.startDate && this.endDate) {
    const diffMs = this.endDate - this.startDate;
    return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  }
  return 0;
});

// Virtual for current status
leaveSchema.virtual('currentStatus').get(function() {
  const now = new Date();
  
  if (this.status !== 'approved') {
    return this.status;
  }
  
  if (now < this.startDate) {
    return 'upcoming';
  } else if (now >= this.startDate && now <= this.endDate) {
    return 'active';
  } else {
    return this.actualReturnDate ? 'completed' : 'overdue';
  }
});

// Virtual for overdue days
leaveSchema.virtual('overdueDays').get(function() {
  if (this.status === 'approved' && !this.actualReturnDate) {
    const now = new Date();
    if (now > this.endDate) {
      const diffMs = now - this.endDate;
      return Math.floor(diffMs / (1000 * 60 * 60 * 24));
    }
  }
  return 0;
});

module.exports = mongoose.model('Leave', leaveSchema);