const nodemailer = require('nodemailer');

// Create email transporter
const createTransporter = () => {
  return nodemailer.createTransporter({
    host: process.env.EMAIL_HOST,
    port: process.env.EMAIL_PORT,
    secure: false, // true for 465, false for other ports
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS
    }
  });
};

// Send email function
const sendEmail = async (options) => {
  try {
    const transporter = createTransporter();
    
    const mailOptions = {
      from: process.env.EMAIL_FROM || process.env.EMAIL_USER,
      to: options.to,
      subject: options.subject,
      html: options.html || options.text,
      text: options.text,
      attachments: options.attachments || []
    };
    
    const result = await transporter.sendMail(mailOptions);
    console.log('Email sent successfully:', result.messageId);
    return { success: true, messageId: result.messageId };
  } catch (error) {
    console.error('Email sending failed:', error);
    return { success: false, error: error.message };
  }
};

// Email templates
const emailTemplates = {
  welcome: (user) => ({
    subject: 'Welcome to Hostel Management System',
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #333;">Welcome to Hostel Management System!</h2>
        <p>Dear ${user.firstName} ${user.lastName},</p>
        <p>Your account has been successfully created. Here are your details:</p>
        <ul>
          <li><strong>Name:</strong> ${user.firstName} ${user.lastName}</li>
          <li><strong>Email:</strong> ${user.email}</li>
          <li><strong>Role:</strong> ${user.role}</li>
          ${user.studentId ? `<li><strong>Student ID:</strong> ${user.studentId}</li>` : ''}
          ${user.employeeId ? `<li><strong>Employee ID:</strong> ${user.employeeId}</li>` : ''}
        </ul>
        <p>Please keep your login credentials secure.</p>
        <p>Best regards,<br>Hostel Management Team</p>
      </div>
    `
  }),

  feeReminder: (fee, user) => ({
    subject: `Fee Payment Reminder - ${fee.feeType}`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #d32f2f;">Fee Payment Reminder</h2>
        <p>Dear ${user.firstName} ${user.lastName},</p>
        <p>This is a reminder that your fee payment is due:</p>
        <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
          <p><strong>Fee Type:</strong> ${fee.feeType}</p>
          <p><strong>Amount:</strong> ₹${fee.finalAmount}</p>
          <p><strong>Due Date:</strong> ${new Date(fee.dueDate).toLocaleDateString()}</p>
          <p><strong>Status:</strong> ${fee.status}</p>
        </div>
        <p>Please make the payment at your earliest convenience to avoid late fees.</p>
        <p>Best regards,<br>Hostel Management Team</p>
      </div>
    `
  }),

  complaintUpdate: (complaint, user) => ({
    subject: `Complaint Update - ${complaint.complaintId}`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #1976d2;">Complaint Status Update</h2>
        <p>Dear ${user.firstName} ${user.lastName},</p>
        <p>Your complaint has been updated:</p>
        <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
          <p><strong>Complaint ID:</strong> ${complaint.complaintId}</p>
          <p><strong>Title:</strong> ${complaint.title}</p>
          <p><strong>Status:</strong> ${complaint.status}</p>
          <p><strong>Category:</strong> ${complaint.category}</p>
        </div>
        <p>You can track your complaint status in the hostel management system.</p>
        <p>Best regards,<br>Hostel Management Team</p>
      </div>
    `
  }),

  leaveApproval: (leave, user, status) => ({
    subject: `Leave Application ${status} - ${leave.leaveId}`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: ${status === 'approved' ? '#4caf50' : '#d32f2f'};">
          Leave Application ${status}
        </h2>
        <p>Dear ${user.firstName} ${user.lastName},</p>
        <p>Your leave application has been ${status}:</p>
        <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
          <p><strong>Leave ID:</strong> ${leave.leaveId}</p>
          <p><strong>Leave Type:</strong> ${leave.leaveType}</p>
          <p><strong>Start Date:</strong> ${new Date(leave.startDate).toLocaleDateString()}</p>
          <p><strong>End Date:</strong> ${new Date(leave.endDate).toLocaleDateString()}</p>
          <p><strong>Status:</strong> ${leave.status}</p>
        </div>
        ${leave.rejectionReason ? `<p><strong>Reason:</strong> ${leave.rejectionReason}</p>` : ''}
        <p>Best regards,<br>Hostel Management Team</p>
      </div>
    `
  }),

  announcement: (announcement) => ({
    subject: `${announcement.priority === 'critical' ? '[URGENT] ' : ''}${announcement.title}`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: ${announcement.priority === 'critical' ? '#d32f2f' : '#1976d2'};">
          ${announcement.title}
        </h2>
        <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
          <p>${announcement.content}</p>
        </div>
        <p><strong>Type:</strong> ${announcement.type}</p>
        <p><strong>Priority:</strong> ${announcement.priority}</p>
        <p>Best regards,<br>Hostel Management Team</p>
      </div>
    `
  })
};

// Send bulk emails
const sendBulkEmails = async (recipients, template, data) => {
  const results = [];
  
  for (const recipient of recipients) {
    try {
      const emailContent = template(data, recipient);
      const result = await sendEmail({
        to: recipient.email,
        ...emailContent
      });
      results.push({ recipient: recipient.email, ...result });
    } catch (error) {
      results.push({ 
        recipient: recipient.email, 
        success: false, 
        error: error.message 
      });
    }
  }
  
  return results;
};

module.exports = {
  sendEmail,
  emailTemplates,
  sendBulkEmails
};