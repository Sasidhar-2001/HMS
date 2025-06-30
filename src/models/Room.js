const mongoose = require('mongoose');

const roomSchema = new mongoose.Schema({
  roomNumber: {
    type: String,
    required: [true, 'Room number is required'],
    unique: true,
    trim: true
  },
  floor: {
    type: Number,
    required: [true, 'Floor number is required'],
    min: [0, 'Floor cannot be negative']
  },
  block: {
    type: String,
    required: [true, 'Block is required'],
    trim: true,
    uppercase: true
  },
  type: {
    type: String,
    enum: ['single', 'double', 'triple'],
    required: [true, 'Room type is required']
  },
  capacity: {
    type: Number,
    required: [true, 'Room capacity is required'],
    min: [1, 'Capacity must be at least 1']
  },
  currentOccupancy: {
    type: Number,
    default: 0,
    min: [0, 'Occupancy cannot be negative']
  },
  occupants: [{
    student: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    allocatedDate: {
      type: Date,
      default: Date.now
    },
    bedNumber: Number
  }],
  amenities: [{
    type: String,
    enum: ['ac', 'fan', 'wifi', 'study_table', 'wardrobe', 'attached_bathroom', 'balcony', 'tv']
  }],
  monthlyRent: {
    type: Number,
    required: [true, 'Monthly rent is required'],
    min: [0, 'Rent cannot be negative']
  },
  securityDeposit: {
    type: Number,
    required: [true, 'Security deposit is required'],
    min: [0, 'Security deposit cannot be negative']
  },
  status: {
    type: String,
    enum: ['available', 'occupied', 'maintenance', 'reserved'],
    default: 'available'
  },
  maintenanceHistory: [{
    issue: String,
    reportedDate: Date,
    resolvedDate: Date,
    cost: Number,
    description: String,
    reportedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    }
  }],
  images: [String],
  description: String,
  isActive: {
    type: Boolean,
    default: true
  }
}, {
  timestamps: true
});

// Indexes for better query performance
roomSchema.index({ roomNumber: 1 });
roomSchema.index({ block: 1, floor: 1 });
roomSchema.index({ type: 1 });
roomSchema.index({ status: 1 });

// Virtual for availability
roomSchema.virtual('isAvailable').get(function() {
  return this.status === 'available' && this.currentOccupancy < this.capacity;
});

// Virtual for occupancy percentage
roomSchema.virtual('occupancyPercentage').get(function() {
  return Math.round((this.currentOccupancy / this.capacity) * 100);
});

// Pre-save middleware to update occupancy
roomSchema.pre('save', function(next) {
  this.currentOccupancy = this.occupants.length;
  
  // Update status based on occupancy
  if (this.currentOccupancy === 0) {
    this.status = 'available';
  } else if (this.currentOccupancy >= this.capacity) {
    this.status = 'occupied';
  }
  
  next();
});

// Method to add occupant
roomSchema.methods.addOccupant = function(studentId, bedNumber) {
  if (this.currentOccupancy >= this.capacity) {
    throw new Error('Room is at full capacity');
  }
  
  // Check if student is already in this room
  const existingOccupant = this.occupants.find(occ => 
    occ.student.toString() === studentId.toString()
  );
  
  if (existingOccupant) {
    throw new Error('Student is already assigned to this room');
  }
  
  this.occupants.push({
    student: studentId,
    allocatedDate: new Date(),
    bedNumber: bedNumber || this.occupants.length + 1
  });
  
  return this.save();
};

// Method to remove occupant
roomSchema.methods.removeOccupant = function(studentId) {
  this.occupants = this.occupants.filter(occ => 
    occ.student.toString() !== studentId.toString()
  );
  
  return this.save();
};

module.exports = mongoose.model('Room', roomSchema);