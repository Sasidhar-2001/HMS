const mongoose = require('mongoose');

const complaintSchema = new mongoose.Schema({
  complaintId: {
    type: String,
    unique: true,
    required: true
  },
  title: {
    type: String,
    required: [true, 'Complaint title is required'],
    trim: true,
    maxlength: [100, 'Title cannot exceed 100 characters']
  },
  description: {
    type: String,
    required: [true, 'Complaint description is required'],
    maxlength: [1000, 'Description cannot exceed 1000 characters']
  },
  category: {
    type: String,
    enum: ['plumbing', 'electrical', 'cleaning', 'maintenance', 'security', 'food', 'internet', 'other'],
    required: [true, 'Complaint category is required']
  },
  priority: {
    type: String,
    enum: ['low', 'medium', 'high', 'urgent'],
    default: 'medium'
  },
  status: {
    type: String,
    enum: ['pending', 'in_progress', 'resolved', 'closed', 'rejected'],
    default: 'pending'
  },
  reportedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'Reporter is required']
  },
  assignedTo: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  roomNumber: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Room'
  },
  location: {
    type: String,
    trim: true
  },
  images: [String],
  statusHistory: [{
    status: {
      type: String,
      enum: ['pending', 'in_progress', 'resolved', 'closed', 'rejected']
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
  resolution: {
    description: String,
    resolvedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    resolvedAt: Date,
    cost: Number,
    rating: {
      type: Number,
      min: 1,
      max: 5
    },
    feedback: String
  },
  expectedResolutionDate: Date,
  actualResolutionDate: Date,
  isUrgent: {
    type: Boolean,
    default: false
  },
  tags: [String]
}, {
  timestamps: true
});

// Indexes for better query performance
complaintSchema.index({ complaintId: 1 });
complaintSchema.index({ reportedBy: 1 });
complaintSchema.index({ assignedTo: 1 });
complaintSchema.index({ status: 1 });
complaintSchema.index({ category: 1 });
complaintSchema.index({ priority: 1 });
complaintSchema.index({ createdAt: -1 });

// Pre-save middleware to generate complaint ID
complaintSchema.pre('save', function(next) {
  if (!this.complaintId) {
    const year = new Date().getFullYear();
    const month = String(new Date().getMonth() + 1).padStart(2, '0');
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
    this.complaintId = `CMP${year}${month}${random}`;
  }
  
  // Set urgent flag based on priority
  this.isUrgent = this.priority === 'urgent';
  
  next();
});

// Method to update status
complaintSchema.methods.updateStatus = function(newStatus, updatedBy, comment) {
  this.status = newStatus;
  
  this.statusHistory.push({
    status: newStatus,
    updatedBy: updatedBy,
    updatedAt: new Date(),
    comment: comment
  });
  
  if (newStatus === 'resolved' || newStatus === 'closed') {
    this.actualResolutionDate = new Date();
  }
  
  return this.save();
};

// Virtual for resolution time in hours
complaintSchema.virtual('resolutionTimeHours').get(function() {
  if (this.actualResolutionDate) {
    const diffMs = this.actualResolutionDate - this.createdAt;
    return Math.round(diffMs / (1000 * 60 * 60));
  }
  return null;
});

// Virtual for overdue status
complaintSchema.virtual('isOverdue').get(function() {
  if (this.expectedResolutionDate && this.status !== 'resolved' && this.status !== 'closed') {
    return new Date() > this.expectedResolutionDate;
  }
  return false;
});

module.exports = mongoose.model('Complaint', complaintSchema);