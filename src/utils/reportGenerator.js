const PDFDocument = require('pdfkit');
const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

// Ensure reports directory exists
const ensureReportsDirectory = () => {
  const reportsDir = path.join(__dirname, '../../reports');
  if (!fs.existsSync(reportsDir)) {
    fs.mkdirSync(reportsDir, { recursive: true });
  }
  return reportsDir;
};

// Generate PDF report
const generatePDFReport = async (data, reportType, options = {}) => {
  return new Promise((resolve, reject) => {
    try {
      const reportsDir = ensureReportsDirectory();
      const filename = `${reportType}_${Date.now()}.pdf`;
      const filepath = path.join(reportsDir, filename);
      
      const doc = new PDFDocument();
      const stream = fs.createWriteStream(filepath);
      doc.pipe(stream);
      
      // Header
      doc.fontSize(20).text('Hostel Management System', { align: 'center' });
      doc.fontSize(16).text(`${reportType.replace('_', ' ').toUpperCase()} REPORT`, { align: 'center' });
      doc.moveDown();
      
      // Date range if provided
      if (options.startDate && options.endDate) {
        doc.fontSize(12).text(`Period: ${new Date(options.startDate).toLocaleDateString()} - ${new Date(options.endDate).toLocaleDateString()}`);
        doc.moveDown();
      }
      
      // Generate report based on type
      switch (reportType) {
        case 'student_report':
          generateStudentPDFReport(doc, data);
          break;
        case 'fee_report':
          generateFeePDFReport(doc, data);
          break;
        case 'complaint_report':
          generateComplaintPDFReport(doc, data);
          break;
        case 'room_report':
          generateRoomPDFReport(doc, data);
          break;
        case 'leave_report':
          generateLeavePDFReport(doc, data);
          break;
        default:
          generateGenericPDFReport(doc, data);
      }
      
      doc.end();
      
      stream.on('finish', () => {
        resolve({
          filename,
          filepath,
          url: `/reports/${filename}`
        });
      });
      
      stream.on('error', reject);
    } catch (error) {
      reject(error);
    }
  });
};

// Generate Excel report
const generateExcelReport = async (data, reportType, options = {}) => {
  try {
    const reportsDir = ensureReportsDirectory();
    const filename = `${reportType}_${Date.now()}.xlsx`;
    const filepath = path.join(reportsDir, filename);
    
    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet(reportType.replace('_', ' ').toUpperCase());
    
    // Generate report based on type
    switch (reportType) {
      case 'student_report':
        generateStudentExcelReport(worksheet, data);
        break;
      case 'fee_report':
        generateFeeExcelReport(worksheet, data);
        break;
      case 'complaint_report':
        generateComplaintExcelReport(worksheet, data);
        break;
      case 'room_report':
        generateRoomExcelReport(worksheet, data);
        break;
      case 'leave_report':
        generateLeaveExcelReport(worksheet, data);
        break;
      default:
        generateGenericExcelReport(worksheet, data);
    }
    
    await workbook.xlsx.writeFile(filepath);
    
    return {
      filename,
      filepath,
      url: `/reports/${filename}`
    };
  } catch (error) {
    throw error;
  }
};

// PDF Report Generators
const generateStudentPDFReport = (doc, students) => {
  doc.fontSize(14).text('Student Details', { underline: true });
  doc.moveDown();
  
  students.forEach((student, index) => {
    doc.fontSize(12)
       .text(`${index + 1}. ${student.firstName} ${student.lastName}`)
       .text(`   Email: ${student.email}`)
       .text(`   Student ID: ${student.studentId || 'N/A'}`)
       .text(`   Room: ${student.roomNumber?.roomNumber || 'Not Assigned'}`)
       .text(`   Course: ${student.course || 'N/A'}`)
       .moveDown(0.5);
  });
};

const generateFeePDFReport = (doc, fees) => {
  doc.fontSize(14).text('Fee Details', { underline: true });
  doc.moveDown();
  
  let totalAmount = 0;
  let paidAmount = 0;
  
  fees.forEach((fee, index) => {
    totalAmount += fee.finalAmount;
    paidAmount += fee.paidAmount;
    
    doc.fontSize(12)
       .text(`${index + 1}. ${fee.student?.firstName} ${fee.student?.lastName}`)
       .text(`   Fee Type: ${fee.feeType}`)
       .text(`   Amount: ₹${fee.finalAmount}`)
       .text(`   Paid: ₹${fee.paidAmount}`)
       .text(`   Status: ${fee.status}`)
       .text(`   Due Date: ${new Date(fee.dueDate).toLocaleDateString()}`)
       .moveDown(0.5);
  });
  
  doc.moveDown()
     .fontSize(14)
     .text(`Summary:`)
     .text(`Total Amount: ₹${totalAmount}`)
     .text(`Paid Amount: ₹${paidAmount}`)
     .text(`Pending Amount: ₹${totalAmount - paidAmount}`);
};

const generateComplaintPDFReport = (doc, complaints) => {
  doc.fontSize(14).text('Complaint Details', { underline: true });
  doc.moveDown();
  
  const statusCounts = {};
  
  complaints.forEach((complaint, index) => {
    statusCounts[complaint.status] = (statusCounts[complaint.status] || 0) + 1;
    
    doc.fontSize(12)
       .text(`${index + 1}. ${complaint.title}`)
       .text(`   ID: ${complaint.complaintId}`)
       .text(`   Category: ${complaint.category}`)
       .text(`   Status: ${complaint.status}`)
       .text(`   Priority: ${complaint.priority}`)
       .text(`   Reported By: ${complaint.reportedBy?.firstName} ${complaint.reportedBy?.lastName}`)
       .text(`   Date: ${new Date(complaint.createdAt).toLocaleDateString()}`)
       .moveDown(0.5);
  });
  
  doc.moveDown()
     .fontSize(14)
     .text('Summary:');
  
  Object.entries(statusCounts).forEach(([status, count]) => {
    doc.text(`${status}: ${count}`);
  });
};

const generateRoomPDFReport = (doc, rooms) => {
  doc.fontSize(14).text('Room Details', { underline: true });
  doc.moveDown();
  
  rooms.forEach((room, index) => {
    doc.fontSize(12)
       .text(`${index + 1}. Room ${room.roomNumber}`)
       .text(`   Block: ${room.block}, Floor: ${room.floor}`)
       .text(`   Type: ${room.type}`)
       .text(`   Capacity: ${room.capacity}`)
       .text(`   Current Occupancy: ${room.currentOccupancy}`)
       .text(`   Status: ${room.status}`)
       .text(`   Monthly Rent: ₹${room.monthlyRent}`)
       .moveDown(0.5);
  });
};

const generateLeavePDFReport = (doc, leaves) => {
  doc.fontSize(14).text('Leave Details', { underline: true });
  doc.moveDown();
  
  leaves.forEach((leave, index) => {
    doc.fontSize(12)
       .text(`${index + 1}. ${leave.student?.firstName} ${leave.student?.lastName}`)
       .text(`   Leave ID: ${leave.leaveId}`)
       .text(`   Type: ${leave.leaveType}`)
       .text(`   Start Date: ${new Date(leave.startDate).toLocaleDateString()}`)
       .text(`   End Date: ${new Date(leave.endDate).toLocaleDateString()}`)
       .text(`   Status: ${leave.status}`)
       .text(`   Duration: ${leave.durationDays} days`)
       .moveDown(0.5);
  });
};

const generateGenericPDFReport = (doc, data) => {
  doc.fontSize(12).text(JSON.stringify(data, null, 2));
};

// Excel Report Generators
const generateStudentExcelReport = (worksheet, students) => {
  worksheet.columns = [
    { header: 'S.No', key: 'sno', width: 10 },
    { header: 'Name', key: 'name', width: 30 },
    { header: 'Email', key: 'email', width: 30 },
    { header: 'Student ID', key: 'studentId', width: 15 },
    { header: 'Phone', key: 'phone', width: 15 },
    { header: 'Room Number', key: 'roomNumber', width: 15 },
    { header: 'Course', key: 'course', width: 20 },
    { header: 'Year', key: 'year', width: 10 },
    { header: 'Status', key: 'status', width: 15 }
  ];
  
  students.forEach((student, index) => {
    worksheet.addRow({
      sno: index + 1,
      name: `${student.firstName} ${student.lastName}`,
      email: student.email,
      studentId: student.studentId || 'N/A',
      phone: student.phone,
      roomNumber: student.roomNumber?.roomNumber || 'Not Assigned',
      course: student.course || 'N/A',
      year: student.year || 'N/A',
      status: student.isActive ? 'Active' : 'Inactive'
    });
  });
  
  // Style the header row
  worksheet.getRow(1).font = { bold: true };
  worksheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFE0E0E0' }
  };
};

const generateFeeExcelReport = (worksheet, fees) => {
  worksheet.columns = [
    { header: 'S.No', key: 'sno', width: 10 },
    { header: 'Student Name', key: 'studentName', width: 30 },
    { header: 'Fee Type', key: 'feeType', width: 20 },
    { header: 'Amount', key: 'amount', width: 15 },
    { header: 'Paid Amount', key: 'paidAmount', width: 15 },
    { header: 'Balance', key: 'balance', width: 15 },
    { header: 'Status', key: 'status', width: 15 },
    { header: 'Due Date', key: 'dueDate', width: 15 },
    { header: 'Month/Year', key: 'monthYear', width: 15 }
  ];
  
  fees.forEach((fee, index) => {
    worksheet.addRow({
      sno: index + 1,
      studentName: `${fee.student?.firstName} ${fee.student?.lastName}`,
      feeType: fee.feeType,
      amount: fee.finalAmount,
      paidAmount: fee.paidAmount,
      balance: fee.balanceAmount,
      status: fee.status,
      dueDate: new Date(fee.dueDate).toLocaleDateString(),
      monthYear: `${fee.month}/${fee.year}`
    });
  });
  
  // Style the header row
  worksheet.getRow(1).font = { bold: true };
  worksheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFE0E0E0' }
  };
};

const generateComplaintExcelReport = (worksheet, complaints) => {
  worksheet.columns = [
    { header: 'S.No', key: 'sno', width: 10 },
    { header: 'Complaint ID', key: 'complaintId', width: 20 },
    { header: 'Title', key: 'title', width: 30 },
    { header: 'Category', key: 'category', width: 15 },
    { header: 'Priority', key: 'priority', width: 15 },
    { header: 'Status', key: 'status', width: 15 },
    { header: 'Reported By', key: 'reportedBy', width: 25 },
    { header: 'Date', key: 'date', width: 15 },
    { header: 'Room', key: 'room', width: 15 }
  ];
  
  complaints.forEach((complaint, index) => {
    worksheet.addRow({
      sno: index + 1,
      complaintId: complaint.complaintId,
      title: complaint.title,
      category: complaint.category,
      priority: complaint.priority,
      status: complaint.status,
      reportedBy: `${complaint.reportedBy?.firstName} ${complaint.reportedBy?.lastName}`,
      date: new Date(complaint.createdAt).toLocaleDateString(),
      room: complaint.roomNumber?.roomNumber || 'N/A'
    });
  });
  
  // Style the header row
  worksheet.getRow(1).font = { bold: true };
  worksheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFE0E0E0' }
  };
};

const generateRoomExcelReport = (worksheet, rooms) => {
  worksheet.columns = [
    { header: 'S.No', key: 'sno', width: 10 },
    { header: 'Room Number', key: 'roomNumber', width: 15 },
    { header: 'Block', key: 'block', width: 10 },
    { header: 'Floor', key: 'floor', width: 10 },
    { header: 'Type', key: 'type', width: 15 },
    { header: 'Capacity', key: 'capacity', width: 10 },
    { header: 'Occupancy', key: 'occupancy', width: 10 },
    { header: 'Status', key: 'status', width: 15 },
    { header: 'Monthly Rent', key: 'rent', width: 15 },
    { header: 'Security Deposit', key: 'deposit', width: 15 }
  ];
  
  rooms.forEach((room, index) => {
    worksheet.addRow({
      sno: index + 1,
      roomNumber: room.roomNumber,
      block: room.block,
      floor: room.floor,
      type: room.type,
      capacity: room.capacity,
      occupancy: room.currentOccupancy,
      status: room.status,
      rent: room.monthlyRent,
      deposit: room.securityDeposit
    });
  });
  
  // Style the header row
  worksheet.getRow(1).font = { bold: true };
  worksheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFE0E0E0' }
  };
};

const generateLeaveExcelReport = (worksheet, leaves) => {
  worksheet.columns = [
    { header: 'S.No', key: 'sno', width: 10 },
    { header: 'Leave ID', key: 'leaveId', width: 20 },
    { header: 'Student Name', key: 'studentName', width: 30 },
    { header: 'Leave Type', key: 'leaveType', width: 15 },
    { header: 'Start Date', key: 'startDate', width: 15 },
    { header: 'End Date', key: 'endDate', width: 15 },
    { header: 'Duration', key: 'duration', width: 10 },
    { header: 'Status', key: 'status', width: 15 },
    { header: 'Applied Date', key: 'appliedDate', width: 15 }
  ];
  
  leaves.forEach((leave, index) => {
    worksheet.addRow({
      sno: index + 1,
      leaveId: leave.leaveId,
      studentName: `${leave.student?.firstName} ${leave.student?.lastName}`,
      leaveType: leave.leaveType,
      startDate: new Date(leave.startDate).toLocaleDateString(),
      endDate: new Date(leave.endDate).toLocaleDateString(),
      duration: leave.durationDays,
      status: leave.status,
      appliedDate: new Date(leave.appliedDate).toLocaleDateString()
    });
  });
  
  // Style the header row
  worksheet.getRow(1).font = { bold: true };
  worksheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FFE0E0E0' }
  };
};

const generateGenericExcelReport = (worksheet, data) => {
  if (data.length > 0) {
    const headers = Object.keys(data[0]);
    worksheet.columns = headers.map(header => ({
      header: header.charAt(0).toUpperCase() + header.slice(1),
      key: header,
      width: 20
    }));
    
    data.forEach(item => {
      worksheet.addRow(item);
    });
    
    // Style the header row
    worksheet.getRow(1).font = { bold: true };
    worksheet.getRow(1).fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FFE0E0E0' }
    };
  }
};

module.exports = {
  generatePDFReport,
  generateExcelReport
};