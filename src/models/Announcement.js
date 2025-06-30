const mongoose = require('mongoose');

const announcementSchema = new mongoose.Schema({
  title: {
    type: String,
    required: [true, 'Title is required'],
    trim: true,
    maxlength: [200, 'Title cannot exceed 200 characters']
  },
  content: {
    type: String,
    required: [true, 'Content is required'],
    maxlength: [2000, 'Content cannot exceed 2000 characters']
  },
  type: {
    type: String,
    enum: ['general', 'urgent', 'event', 'maintenance', 'fee', 'academic', 'holiday'],
    default: 'general'
  },
  priority: {
    type: String,
    enum: ['low', 'medium', 'high', 'critical'],
    default: 'medium'
  },
  targetAudience: {
    type: String,
    enum: ['all', 'students', 'wardens', 'admins', 'specific_rooms', 'specific_users'],
    default: 'all'
  },
  targetRooms: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Room'
  }],
  targetUsers: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  createdBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'Creator is required']
  },
  publishDate: {
    type: Date,
    default: Date.now
  },
  expiryDate: Date,
  status: {
    type: String,
    enum: ['draft', 'published', 'archived', 'expired'],
    default: 'draft'
  },
  attachments: [{
    fileName: String,
    filePath: String,
    fileSize: Number,
    uploadDate: {
      type: Date,
      default: Date.now
    }
  }],
  readBy: [{
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    readAt: {
      type: Date,
      default: Date.now
    }
  }],
  likes: [{
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    likedAt: {
      type: Date,
      default: Date.now
    }
  }],
  comments: [{
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    comment: {
      type: String,
      maxlength: [500, 'Comment cannot exceed 500 characters']
    },
    commentedAt: {
      type: Date,
      default: Date.now
    }
  }],
  tags: [String],
  isSticky: {
    type: Boolean,
    default: false
  },
  emailSent: {
    type: Boolean,
    default: false
  },
  smsSent: {
    type: Boolean,
    default: false
  },
  notificationSent: {
    type: Boolean,
    default: false
  },
  viewCount: {
    type: Number,
    default: 0
  }
}, {
  timestamps: true
});

// Indexes for better query performance
announcementSchema.index({ createdBy: 1 });
announcementSchema.index({ status: 1 });
announcementSchema.index({ type: 1 });
announcementSchema.index({ priority: 1 });
announcementSchema.index({ publishDate: -1 });
announcementSchema.index({ expiryDate: 1 });
announcementSchema.index({ targetAudience: 1 });

// Pre-save middleware to update status based on expiry
announcementSchema.pre('save', function(next) {
  if (this.expiryDate && new Date() > this.expiryDate && this.status === 'published') {
    this.status = 'expired';
  }
  next();
});

// Method to mark as read by user
announcementSchema.methods.markAsRead = function(userId) {
  const existingRead = this.readBy.find(read => 
    read.user.toString() === userId.toString()
  );
  
  if (!existingRead) {
    this.readBy.push({
      user: userId,
      readAt: new Date()
    });
    this.viewCount += 1;
  }
  
  return this.save();
};

// Method to add like
announcementSchema.methods.addLike = function(userId) {
  const existingLike = this.likes.find(like => 
    like.user.toString() === userId.toString()
  );
  
  if (!existingLike) {
    this.likes.push({
      user: userId,
      likedAt: new Date()
    });
  }
  
  return this.save();
};

// Method to remove like
announcementSchema.methods.removeLike = function(userId) {
  this.likes = this.likes.filter(like => 
    like.user.toString() !== userId.toString()
  );
  
  return this.save();
};

// Method to add comment
announcementSchema.methods.addComment = function(userId, commentText) {
  this.comments.push({
    user: userId,
    comment: commentText,
    commentedAt: new Date()
  });
  
  return this.save();
};

// Virtual for read percentage
announcementSchema.virtual('readPercentage').get(function() {
  // This would need to be calculated based on total target users
  // For now, returning a simple calculation
  return this.readBy.length;
});

// Virtual for like count
announcementSchema.virtual('likeCount').get(function() {
  return this.likes.length;
});

// Virtual for comment count
announcementSchema.virtual('commentCount').get(function() {
  return this.comments.length;
});

// Virtual for active status
announcementSchema.virtual('isActive').get(function() {
  const now = new Date();
  return this.status === 'published' && 
         (!this.expiryDate || now <= this.expiryDate);
});

module.exports = mongoose.model('Announcement', announcementSchema);