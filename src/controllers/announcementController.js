const Announcement = require('../models/Announcement');
const User = require('../models/User');
const { sendEmail, emailTemplates, sendBulkEmails } = require('../utils/email');

// Get all announcements
const getAllAnnouncements = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const type = req.query.type;
    const status = req.query.status;
    const priority = req.query.priority;

    const query = {};
    if (type) query.type = type;
    if (status) query.status = status;
    if (priority) query.priority = priority;

    // For students, filter announcements based on target audience
    if (req.user.role === 'student') {
      query.$or = [
        { targetAudience: 'all' },
        { targetAudience: 'students' },
        { targetUsers: req.user._id }
      ];
      query.status = 'published';
      query.$or = [
        { expiryDate: { $exists: false } },
        { expiryDate: { $gte: new Date() } }
      ];
    }

    const announcements = await Announcement.find(query)
      .populate('createdBy', 'firstName lastName')
      .populate('targetUsers', 'firstName lastName email')
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

// Get single announcement
const getAnnouncement = async (req, res) => {
  try {
    const { id } = req.params;

    const announcement = await Announcement.findById(id)
      .populate('createdBy', 'firstName lastName')
      .populate('targetUsers', 'firstName lastName email')
      .populate('readBy.user', 'firstName lastName')
      .populate('likes.user', 'firstName lastName')
      .populate('comments.user', 'firstName lastName');

    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    // Mark as read if user is student
    if (req.user.role === 'student') {
      await announcement.markAsRead(req.user._id);
    }

    res.json({
      success: true,
      data: { announcement }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch announcement',
      error: error.message
    });
  }
};

// Create new announcement
const createAnnouncement = async (req, res) => {
  try {
    const announcementData = {
      ...req.body,
      createdBy: req.user._id
    };

    const announcement = new Announcement(announcementData);
    await announcement.save();

    // If published, send notifications
    if (announcement.status === 'published') {
      await sendAnnouncementNotifications(announcement);
    }

    const populatedAnnouncement = await Announcement.findById(announcement._id)
      .populate('createdBy', 'firstName lastName');

    res.status(201).json({
      success: true,
      message: 'Announcement created successfully',
      data: { announcement: populatedAnnouncement }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create announcement',
      error: error.message
    });
  }
};

// Update announcement
const updateAnnouncement = async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const announcement = await Announcement.findById(id);
    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    // Check permissions
    if (req.user.role !== 'admin' && announcement.createdBy.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    Object.assign(announcement, updates);
    await announcement.save();

    // If status changed to published, send notifications
    if (updates.status === 'published' && !announcement.emailSent) {
      await sendAnnouncementNotifications(announcement);
    }

    const updatedAnnouncement = await Announcement.findById(id)
      .populate('createdBy', 'firstName lastName');

    res.json({
      success: true,
      message: 'Announcement updated successfully',
      data: { announcement: updatedAnnouncement }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update announcement',
      error: error.message
    });
  }
};

// Delete announcement
const deleteAnnouncement = async (req, res) => {
  try {
    const { id } = req.params;

    const announcement = await Announcement.findById(id);
    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    // Check permissions
    if (req.user.role !== 'admin' && announcement.createdBy.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    await Announcement.findByIdAndDelete(id);

    res.json({
      success: true,
      message: 'Announcement deleted successfully'
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to delete announcement',
      error: error.message
    });
  }
};

// Like/Unlike announcement
const toggleLike = async (req, res) => {
  try {
    const { id } = req.params;

    const announcement = await Announcement.findById(id);
    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    const existingLike = announcement.likes.find(like => 
      like.user.toString() === req.user._id.toString()
    );

    if (existingLike) {
      await announcement.removeLike(req.user._id);
    } else {
      await announcement.addLike(req.user._id);
    }

    const updatedAnnouncement = await Announcement.findById(id)
      .populate('likes.user', 'firstName lastName');

    res.json({
      success: true,
      message: existingLike ? 'Like removed' : 'Like added',
      data: { 
        announcement: updatedAnnouncement,
        liked: !existingLike
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to toggle like',
      error: error.message
    });
  }
};

// Add comment to announcement
const addComment = async (req, res) => {
  try {
    const { id } = req.params;
    const { comment } = req.body;

    const announcement = await Announcement.findById(id);
    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    await announcement.addComment(req.user._id, comment);

    const updatedAnnouncement = await Announcement.findById(id)
      .populate('comments.user', 'firstName lastName');

    res.json({
      success: true,
      message: 'Comment added successfully',
      data: { announcement: updatedAnnouncement }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to add comment',
      error: error.message
    });
  }
};

// Publish announcement
const publishAnnouncement = async (req, res) => {
  try {
    const { id } = req.params;

    const announcement = await Announcement.findById(id);
    if (!announcement) {
      return res.status(404).json({
        success: false,
        message: 'Announcement not found'
      });
    }

    // Check permissions
    if (req.user.role !== 'admin' && announcement.createdBy.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    announcement.status = 'published';
    announcement.publishDate = new Date();
    await announcement.save();

    // Send notifications
    await sendAnnouncementNotifications(announcement);

    res.json({
      success: true,
      message: 'Announcement published successfully',
      data: { announcement }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to publish announcement',
      error: error.message
    });
  }
};

// Get announcement statistics
const getAnnouncementStats = async (req, res) => {
  try {
    const [
      totalAnnouncements,
      publishedAnnouncements,
      draftAnnouncements,
      expiredAnnouncements,
      announcementsByType,
      announcementsByPriority,
      recentActivity
    ] = await Promise.all([
      Announcement.countDocuments(),
      Announcement.countDocuments({ status: 'published' }),
      Announcement.countDocuments({ status: 'draft' }),
      Announcement.countDocuments({ status: 'expired' }),
      Announcement.aggregate([
        { $group: { _id: '$type', count: { $sum: 1 } } }
      ]),
      Announcement.aggregate([
        { $group: { _id: '$priority', count: { $sum: 1 } } }
      ]),
      Announcement.find({ status: 'published' })
        .populate('createdBy', 'firstName lastName')
        .sort({ createdAt: -1 })
        .limit(5)
    ]);

    res.json({
      success: true,
      data: {
        total: totalAnnouncements,
        published: publishedAnnouncements,
        draft: draftAnnouncements,
        expired: expiredAnnouncements,
        byType: announcementsByType,
        byPriority: announcementsByPriority,
        recentActivity
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to fetch announcement statistics',
      error: error.message
    });
  }
};

// Helper function to send announcement notifications
const sendAnnouncementNotifications = async (announcement) => {
  try {
    let recipients = [];

    switch (announcement.targetAudience) {
      case 'all':
        recipients = await User.find({ isActive: true });
        break;
      case 'students':
        recipients = await User.find({ role: 'student', isActive: true });
        break;
      case 'wardens':
        recipients = await User.find({ role: 'warden', isActive: true });
        break;
      case 'admins':
        recipients = await User.find({ role: 'admin', isActive: true });
        break;
      case 'specific_users':
        recipients = await User.find({ _id: { $in: announcement.targetUsers } });
        break;
    }

    if (recipients.length > 0) {
      const emailContent = emailTemplates.announcement(announcement);
      const results = await sendBulkEmails(recipients, emailTemplates.announcement, announcement);
      
      const successCount = results.filter(r => r.success).length;
      const failureCount = results.filter(r => !r.success).length;

      announcement.emailSent = successCount > 0;
      announcement.notificationSent = true;
      await announcement.save();

      console.log(`Announcement notifications sent: ${successCount} success, ${failureCount} failed`);
    }
  } catch (error) {
    console.error('Failed to send announcement notifications:', error);
  }
};

module.exports = {
  getAllAnnouncements,
  getAnnouncement,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  toggleLike,
  addComment,
  publishAnnouncement,
  getAnnouncementStats
};