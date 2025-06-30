const mongoose = require('mongoose');

const feeSchema = new mongoose.Schema({
  student: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'Student is required']
  },
  feeType: {
    type: String,
    enum: ['room_rent', 'mess_fee', 'security_deposit', 'maintenance', 'electricity', 'water', 'internet', 'other'],
    required: [true, 'Fee type is required']
  },
  amount: {
    type: Number,
    required: [true, 'Amount is required'],
    min: [0, 'Amount cannot be negative']
  },
  dueDate: {
    type: Date,
    required: [true, 'Due date is required']
  },
  paidDate: Date,
  status: {
    type: String,
    enum: ['pending', 'paid', 'overdue', 'partial', 'waived'],
    default: 'pending'
  },
  paymentMethod: {
    type: String,
    enum: ['cash', 'card', 'upi', 'bank_transfer', 'cheque', 'online'],
    required: function() {
      return this.status === 'paid' || this.status === 'partial';
    }
  },
  transactionId: String,
  receiptNumber: String,
  month: {
    type: Number,
    min: 1,
    max: 12,
    required: [true, 'Month is required']
  },
  year: {
    type: Number,
    required: [true, 'Year is required']
  },
  description: String,
  lateFee: {
    type: Number,
    default: 0,
    min: [0, 'Late fee cannot be negative']
  },
  discount: {
    type: Number,
    default: 0,
    min: [0, 'Discount cannot be negative']
  },
  finalAmount: {
    type: Number,
    required: true
  },
  paidAmount: {
    type: Number,
    default: 0,
    min: [0, 'Paid amount cannot be negative']
  },
  balanceAmount: {
    type: Number,
    default: 0
  },
  paymentHistory: [{
    amount: Number,
    paidDate: Date,
    paymentMethod: String,
    transactionId: String,
    receiptNumber: String,
    paidBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    }
  }],
  reminders: [{
    sentDate: Date,
    type: {
      type: String,
      enum: ['email', 'sms', 'notification']
    },
    status: {
      type: String,
      enum: ['sent', 'delivered', 'failed']
    }
  }],
  room: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Room'
  },
  createdBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  updatedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  notes: String
}, {
  timestamps: true
});

// Indexes for better query performance
feeSchema.index({ student: 1 });
feeSchema.index({ status: 1 });
feeSchema.index({ dueDate: 1 });
feeSchema.index({ month: 1, year: 1 });
feeSchema.index({ feeType: 1 });

// Pre-save middleware to calculate final amount and balance
feeSchema.pre('save', function(next) {
  // Calculate final amount
  this.finalAmount = this.amount + this.lateFee - this.discount;
  
  // Calculate balance amount
  this.balanceAmount = this.finalAmount - this.paidAmount;
  
  // Update status based on payment
  if (this.paidAmount === 0) {
    this.status = new Date() > this.dueDate ? 'overdue' : 'pending';
  } else if (this.paidAmount >= this.finalAmount) {
    this.status = 'paid';
    if (!this.paidDate) {
      this.paidDate = new Date();
    }
  } else {
    this.status = 'partial';
  }
  
  // Generate receipt number if paid
  if (this.status === 'paid' && !this.receiptNumber) {
    const year = new Date().getFullYear();
    const month = String(new Date().getMonth() + 1).padStart(2, '0');
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
    this.receiptNumber = `RCP${year}${month}${random}`;
  }
  
  next();
});

// Method to add payment
feeSchema.methods.addPayment = function(paymentData) {
  this.paymentHistory.push({
    amount: paymentData.amount,
    paidDate: paymentData.paidDate || new Date(),
    paymentMethod: paymentData.paymentMethod,
    transactionId: paymentData.transactionId,
    receiptNumber: paymentData.receiptNumber,
    paidBy: paymentData.paidBy
  });
  
  this.paidAmount += paymentData.amount;
  this.paymentMethod = paymentData.paymentMethod;
  this.transactionId = paymentData.transactionId;
  
  return this.save();
};

// Method to add reminder
feeSchema.methods.addReminder = function(type, status = 'sent') {
  this.reminders.push({
    sentDate: new Date(),
    type: type,
    status: status
  });
  
  return this.save();
};

// Virtual for days overdue
feeSchema.virtual('daysOverdue').get(function() {
  if (this.status === 'overdue' || (this.status === 'pending' && new Date() > this.dueDate)) {
    const diffMs = new Date() - this.dueDate;
    return Math.floor(diffMs / (1000 * 60 * 60 * 24));
  }
  return 0;
});

// Virtual for payment percentage
feeSchema.virtual('paymentPercentage').get(function() {
  if (this.finalAmount === 0) return 100;
  return Math.round((this.paidAmount / this.finalAmount) * 100);
});

module.exports = mongoose.model('Fee', feeSchema);