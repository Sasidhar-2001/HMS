const { body, param, query, validationResult } = require('express-validator');

// Middleware to handle validation errors
const handleValidationErrors = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({
      success: false,
      message: 'Validation failed',
      errors: errors.array()
    });
  }
  next();
};

// User validation rules
const validateUserRegistration = [
  body('firstName')
    .trim()
    .isLength({ min: 2, max: 50 })
    .withMessage('First name must be between 2 and 50 characters'),
  
  body('lastName')
    .trim()
    .isLength({ min: 2, max: 50 })
    .withMessage('Last name must be between 2 and 50 characters'),
  
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Please provide a valid email'),
  
  body('password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
    .withMessage('Password must contain at least one uppercase letter, one lowercase letter, and one number'),
  
  body('phone')
    .matches(/^[0-9]{10}$/)
    .withMessage('Phone number must be 10 digits'),
  
  body('role')
    .isIn(['admin', 'warden', 'student'])
    .withMessage('Role must be admin, warden, or student'),
  
  body('dateOfBirth')
    .isISO8601()
    .withMessage('Please provide a valid date of birth'),
  
  body('gender')
    .isIn(['male', 'female', 'other'])
    .withMessage('Gender must be male, female, or other'),
  
  handleValidationErrors
];

const validateUserLogin = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Please provide a valid email'),
  
  body('password')
    .notEmpty()
    .withMessage('Password is required'),
  
  handleValidationErrors
];

// Room validation rules
const validateRoom = [
  body('roomNumber')
    .trim()
    .notEmpty()
    .withMessage('Room number is required'),
  
  body('floor')
    .isInt({ min: 0 })
    .withMessage('Floor must be a non-negative integer'),
  
  body('block')
    .trim()
    .notEmpty()
    .withMessage('Block is required'),
  
  body('type')
    .isIn(['single', 'double', 'triple'])
    .withMessage('Room type must be single, double, or triple'),
  
  body('capacity')
    .isInt({ min: 1 })
    .withMessage('Capacity must be at least 1'),
  
  body('monthlyRent')
    .isFloat({ min: 0 })
    .withMessage('Monthly rent must be a positive number'),
  
  body('securityDeposit')
    .isFloat({ min: 0 })
    .withMessage('Security deposit must be a positive number'),
  
  handleValidationErrors
];

// Complaint validation rules
const validateComplaint = [
  body('title')
    .trim()
    .isLength({ min: 5, max: 100 })
    .withMessage('Title must be between 5 and 100 characters'),
  
  body('description')
    .trim()
    .isLength({ min: 10, max: 1000 })
    .withMessage('Description must be between 10 and 1000 characters'),
  
  body('category')
    .isIn(['plumbing', 'electrical', 'cleaning', 'maintenance', 'security', 'food', 'internet', 'other'])
    .withMessage('Invalid complaint category'),
  
  body('priority')
    .optional()
    .isIn(['low', 'medium', 'high', 'urgent'])
    .withMessage('Priority must be low, medium, high, or urgent'),
  
  handleValidationErrors
];

// Fee validation rules
const validateFee = [
  body('student')
    .isMongoId()
    .withMessage('Invalid student ID'),
  
  body('feeType')
    .isIn(['room_rent', 'mess_fee', 'security_deposit', 'maintenance', 'electricity', 'water', 'internet', 'other'])
    .withMessage('Invalid fee type'),
  
  body('amount')
    .isFloat({ min: 0 })
    .withMessage('Amount must be a positive number'),
  
  body('dueDate')
    .isISO8601()
    .withMessage('Please provide a valid due date'),
  
  body('month')
    .isInt({ min: 1, max: 12 })
    .withMessage('Month must be between 1 and 12'),
  
  body('year')
    .isInt({ min: 2020, max: 2030 })
    .withMessage('Year must be between 2020 and 2030'),
  
  handleValidationErrors
];

// Leave validation rules
const validateLeave = [
  body('leaveType')
    .isIn(['home', 'medical', 'emergency', 'personal', 'academic', 'other'])
    .withMessage('Invalid leave type'),
  
  body('startDate')
    .isISO8601()
    .withMessage('Please provide a valid start date'),
  
  body('endDate')
    .isISO8601()
    .withMessage('Please provide a valid end date'),
  
  body('reason')
    .trim()
    .isLength({ min: 10, max: 500 })
    .withMessage('Reason must be between 10 and 500 characters'),
  
  handleValidationErrors
];

// Announcement validation rules
const validateAnnouncement = [
  body('title')
    .trim()
    .isLength({ min: 5, max: 200 })
    .withMessage('Title must be between 5 and 200 characters'),
  
  body('content')
    .trim()
    .isLength({ min: 10, max: 2000 })
    .withMessage('Content must be between 10 and 2000 characters'),
  
  body('type')
    .optional()
    .isIn(['general', 'urgent', 'event', 'maintenance', 'fee', 'academic', 'holiday'])
    .withMessage('Invalid announcement type'),
  
  body('priority')
    .optional()
    .isIn(['low', 'medium', 'high', 'critical'])
    .withMessage('Priority must be low, medium, high, or critical'),
  
  body('targetAudience')
    .optional()
    .isIn(['all', 'students', 'wardens', 'admins', 'specific_rooms', 'specific_users'])
    .withMessage('Invalid target audience'),
  
  handleValidationErrors
];

// Parameter validation
const validateObjectId = [
  param('id').isMongoId().withMessage('Invalid ID format'),
  handleValidationErrors
];

// Query validation
const validatePagination = [
  query('page')
    .optional()
    .isInt({ min: 1 })
    .withMessage('Page must be a positive integer'),
  
  query('limit')
    .optional()
    .isInt({ min: 1, max: 100 })
    .withMessage('Limit must be between 1 and 100'),
  
  handleValidationErrors
];

module.exports = {
  handleValidationErrors,
  validateUserRegistration,
  validateUserLogin,
  validateRoom,
  validateComplaint,
  validateFee,
  validateLeave,
  validateAnnouncement,
  validateObjectId,
  validatePagination
};