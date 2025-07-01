package com.yourproject.service.impl;

import com.yourproject.dto.*;
import com.yourproject.entity.*;
import com.yourproject.entity.embeddable.FeePaymentHistoryItem;
import com.yourproject.entity.embeddable.FeeReminder;
import com.yourproject.exception.AccessDeniedException;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.repository.FeeRepository;
import com.yourproject.repository.UserRepository;
import com.yourproject.repository.RoomRepository; // If fees are linked to rooms
import com.yourproject.service.FeeService;
import com.yourproject.service.EmailService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Map; // For stats

@Service
public class FeeServiceImpl implements FeeService {

    private final FeeRepository feeRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    @Autowired
    public FeeServiceImpl(FeeRepository feeRepository,
                          UserRepository userRepository,
                          RoomRepository roomRepository,
                          ModelMapper modelMapper,
                          EmailService emailService) {
        this.feeRepository = feeRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
    }

    private FeeDto convertToDto(Fee fee) {
        FeeDto dto = modelMapper.map(fee, FeeDto.class);
        if (fee.getStudent() != null) {
            dto.setStudent(modelMapper.map(fee.getStudent(), UserSlimDto.class));
        }
        if (fee.getRoom() != null) {
            dto.setRoom(modelMapper.map(fee.getRoom(), RoomSlimDto.class));
        }
        if (fee.getCreatedBy() != null) {
            dto.setCreatedBy(modelMapper.map(fee.getCreatedBy(), UserSlimDto.class));
        }
        if (fee.getUpdatedBy() != null) {
            dto.setUpdatedBy(modelMapper.map(fee.getUpdatedBy(), UserSlimDto.class));
        }
        // Calculate derived DTO fields
        dto.setDaysOverdue(calculateDaysOverdue(fee));
        dto.setPaymentPercentage(calculatePaymentPercentage(fee));
        return dto;
    }

    private Fee findFeeEntityById(Long feeId) {
        return feeRepository.findById(feeId)
            .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with ID: " + feeId));
    }

    private String generateReceiptNumber() {
        // RCPYYYYMMXXXX
        String yearMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        return "RCP" + yearMonth + randomSuffix;
    }

    private void calculateFeeAmountsAndStatus(Fee fee) {
        fee.setFinalAmount(fee.getAmount().add(fee.getLateFee()).subtract(fee.getDiscount()));
        fee.setBalanceAmount(fee.getFinalAmount().subtract(fee.getPaidAmount()));

        if (fee.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            fee.setStatus(LocalDate.now().isAfter(fee.getDueDate()) ? FeeStatus.OVERDUE : FeeStatus.PENDING);
        } else if (fee.getPaidAmount().compareTo(fee.getFinalAmount()) >= 0) {
            fee.setStatus(FeeStatus.PAID);
            if (fee.getPaidDate() == null) {
                fee.setPaidDate(LocalDate.now());
            }
            if (!StringUtils.hasText(fee.getReceiptNumber())) { // Only generate if not already set (e.g. during final payment)
                 fee.setReceiptNumber(generateReceiptNumber());
            }
        } else {
            fee.setStatus(FeeStatus.PARTIAL);
        }
    }

    @Override
    @Transactional
    public FeeDto createFee(FeeRequestDto requestDto, User currentUser) {
        User student = userRepository.findById(requestDto.getStudentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + requestDto.getStudentId()));

        Fee fee = modelMapper.map(requestDto, Fee.class);
        fee.setStudent(student);
        fee.setCreatedBy(currentUser);

        if (requestDto.getRoomId() != null) {
            Room room = roomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + requestDto.getRoomId()));
            fee.setRoom(room);
        }

        // Set defaults if not provided in request
        if (fee.getLateFee() == null) fee.setLateFee(BigDecimal.ZERO);
        if (fee.getDiscount() == null) fee.setDiscount(BigDecimal.ZERO);
        fee.setPaidAmount(BigDecimal.ZERO); // New fee starts with zero paid

        calculateFeeAmountsAndStatus(fee); // Calculate final, balance, and initial status

        Fee savedFee = feeRepository.save(fee);
        // emailService.sendFeeGeneratedEmail(student, savedFee); // Notify student
        return convertToDto(savedFee);
    }

    @Override
    public FeeDto getFeeById(Long feeId, User currentUser) {
        Fee fee = findFeeEntityById(feeId);
        if (currentUser.getRole() == Role.STUDENT && !fee.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this fee record.");
        }
        return convertToDto(fee);
    }

    @Override
    public Page<FeeDto> getAllFees(Pageable pageable, User currentUser, String statusFilter, String feeTypeFilter, Integer monthFilter, Integer yearFilter) {
        Specification<Fee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (currentUser.getRole() == Role.STUDENT) {
                predicates.add(cb.equal(root.get("student"), currentUser));
            }
            if (StringUtils.hasText(statusFilter)) {
                try {
                    predicates.add(cb.equal(root.get("status"), FeeStatus.valueOf(statusFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/*ignore*/}
            }
            if (StringUtils.hasText(feeTypeFilter)) {
                 try {
                    predicates.add(cb.equal(root.get("feeType"), FeeType.valueOf(feeTypeFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/*ignore*/}
            }
            if (monthFilter != null) {
                predicates.add(cb.equal(root.get("month"), monthFilter));
            }
            if (yearFilter != null) {
                predicates.add(cb.equal(root.get("year"), yearFilter));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return feeRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    @Override
    @Transactional
    public FeeDto updateFee(Long feeId, FeeRequestDto requestDto, User currentUser) {
        // Generally, only admins/wardens update core fee details. Students make payments.
        if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot update fee records directly.");
        }
        Fee fee = findFeeEntityById(feeId);
        fee.setUpdatedBy(currentUser);

        // Map updatable fields
        if(requestDto.getFeeType() != null) fee.setFeeType(requestDto.getFeeType());
        if(requestDto.getAmount() != null) fee.setAmount(requestDto.getAmount());
        if(requestDto.getDueDate() != null) fee.setDueDate(requestDto.getDueDate());
        if(requestDto.getMonth() != null) fee.setMonth(requestDto.getMonth());
        if(requestDto.getYear() != null) fee.setYear(requestDto.getYear());
        if(StringUtils.hasText(requestDto.getDescription())) fee.setDescription(requestDto.getDescription());
        if(requestDto.getLateFee() != null) fee.setLateFee(requestDto.getLateFee()); else fee.setLateFee(BigDecimal.ZERO);
        if(requestDto.getDiscount() != null) fee.setDiscount(requestDto.getDiscount()); else fee.setDiscount(BigDecimal.ZERO);
        if(StringUtils.hasText(requestDto.getNotes())) fee.setNotes(requestDto.getNotes());

        if (requestDto.getStudentId() != null && !fee.getStudent().getId().equals(requestDto.getStudentId())) {
             User student = userRepository.findById(requestDto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + requestDto.getStudentId()));
            fee.setStudent(student);
        }
        if (requestDto.getRoomId() != null) {
            Room room = roomRepository.findById(requestDto.getRoomId()).orElse(null); // Room can be nullable
            fee.setRoom(room);
        } else {
            fee.setRoom(null);
        }

        calculateFeeAmountsAndStatus(fee); // Recalculate amounts and status

        Fee updatedFee = feeRepository.save(fee);
        return convertToDto(updatedFee);
    }

    @Override
    @Transactional
    public FeeDto addPayment(Long feeId, FeePaymentRequestDto paymentDto, User currentUser) {
        Fee fee = findFeeEntityById(feeId);
        if (currentUser.getRole() == Role.STUDENT && !fee.getStudent().getId().equals(currentUser.getId())) {
            // Allow admin/warden to make payment on behalf of student too
             throw new AccessDeniedException("You cannot make a payment for this fee record.");
        }

        if (fee.getStatus() == FeeStatus.PAID) {
            throw new BadRequestException("Fee is already fully paid.");
        }

        FeePaymentHistoryItem paymentItem = modelMapper.map(paymentDto, FeePaymentHistoryItem.class);
        paymentItem.setPaidById(currentUser.getId());
        if(paymentDto.getPaidDate() == null) paymentItem.setPaidDate(LocalDate.now());

        fee.getPaymentHistory().add(paymentItem);
        fee.setPaidAmount(fee.getPaidAmount().add(paymentDto.getAmount()));
        fee.setPaymentMethod(paymentDto.getPaymentMethod()); // Update latest payment method on Fee
        fee.setTransactionId(paymentDto.getTransactionId()); // Update latest transaction ID

        calculateFeeAmountsAndStatus(fee); // Recalculate balance and update status

        Fee updatedFee = feeRepository.save(fee);
        // emailService.sendPaymentConfirmationEmail(fee.getStudent(), updatedFee, paymentItem);
        return convertToDto(updatedFee);
    }

    @Override
    @Transactional
    public void sendFeeReminder(Long feeId, User currentUser, String reminderTypeStr) {
        // Only admin/warden can send reminders
        if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot send fee reminders.");
        }
        Fee fee = findFeeEntityById(feeId);
        if (fee.getStatus() == FeeStatus.PAID) {
            throw new BadRequestException("Fee is already paid, no reminder needed.");
        }

        ReminderType reminderType;
        try {
            reminderType = ReminderType.valueOf(reminderTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid reminder type: " + reminderTypeStr);
        }

        // emailService.sendFeeReminderEmail(fee.getStudent(), fee); // Actual email sending

        FeeReminder reminder = new FeeReminder(LocalDateTime.now(), reminderType, ReminderStatus.SENT);
        fee.getReminders().add(reminder);
        feeRepository.save(fee);
    }

    @Override
    @Transactional
    public void sendBulkFeeReminders(User currentUser, String statusFilter, String reminderTypeStr) {
        if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot send bulk fee reminders.");
        }

        FeeStatus status;
        try {
            status = FeeStatus.valueOf(statusFilter.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status filter for bulk reminders: " + statusFilter);
        }
        if (status == FeeStatus.PAID) {
             throw new BadRequestException("Cannot send bulk reminders for already paid fees.");
        }

        ReminderType reminderType;
        try {
            reminderType = ReminderType.valueOf(reminderTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid reminder type: " + reminderTypeStr);
        }

        List<Fee> feesToRemind = feeRepository.findByStatus(status);
        for (Fee fee : feesToRemind) {
            // emailService.sendFeeReminderEmail(fee.getStudent(), fee);
            FeeReminder reminder = new FeeReminder(LocalDateTime.now(), reminderType, ReminderStatus.SENT);
            fee.getReminders().add(reminder);
            feeRepository.save(fee); // Consider batch saving if performance is an issue
        }
    }

    @Override
    public FeeStatsDto getFeeStats(User currentUser, Integer yearFilter) {
        int year = (yearFilter == null) ? LocalDate.now().getYear() : yearFilter;
        // Basic counts, actual sums would need @Query in repository or more complex logic
        long total = feeRepository.count(); // Filter by year in a real scenario
        long paid = feeRepository.countByStatus(FeeStatus.PAID);
        long pending = feeRepository.countByStatus(FeeStatus.PENDING);
        long overdue = feeRepository.countByStatus(FeeStatus.OVERDUE);

        // These sums should ideally be year-specific from repository queries
        BigDecimal totalRevenue = feeRepository.sumFinalAmountByStatusAndYear(FeeStatus.PAID, year).orElse(BigDecimal.ZERO);
        BigDecimal pendingRevenue = feeRepository.sumBalanceAmountByStatusInAndYear(
            List.of(FeeStatus.PENDING, FeeStatus.OVERDUE, FeeStatus.PARTIAL), year
        ).orElse(BigDecimal.ZERO);

        double collectionRate = total > 0 ? ((double)paid / total) * 100 : 0;

        // Monthly and FeeType stats would require aggregation queries in repository
        return new FeeStatsDto(year, total, paid, pending, overdue, totalRevenue, pendingRevenue, collectionRate, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public Page<FeeDto> getDefaulters(Pageable pageable, User currentUser) {
         if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot view defaulters list.");
        }
        List<FeeStatus> defaulterStatuses = List.of(FeeStatus.OVERDUE, FeeStatus.PARTIAL);
        return feeRepository.findByStatusInAndBalanceAmountGreaterThan(defaulterStatuses, BigDecimal.ZERO, pageable)
            .map(this::convertToDto);
    }

    private int calculateDaysOverdue(Fee fee) {
        if (fee.getStatus() == FeeStatus.OVERDUE || (fee.getStatus() == FeeStatus.PENDING && LocalDate.now().isAfter(fee.getDueDate()))) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(fee.getDueDate(), LocalDate.now());
            return (int) Math.max(0, days);
        }
        return 0;
    }

    private int calculatePaymentPercentage(Fee fee) {
        if (fee.getFinalAmount() == null || fee.getFinalAmount().compareTo(BigDecimal.ZERO) == 0) {
            return (fee.getPaidAmount() != null && fee.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) ? 100 : 0;
        }
        if (fee.getPaidAmount() == null || fee.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal percentage = fee.getPaidAmount().multiply(new BigDecimal(100)).divide(fee.getFinalAmount(), 0, BigDecimal.ROUND_HALF_UP);
        return Math.min(100, percentage.intValue());
    }
}
