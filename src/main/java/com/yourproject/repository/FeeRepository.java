package com.yourproject.repository;

import com.yourproject.entity.Fee;
import com.yourproject.entity.FeeStatus;
import com.yourproject.entity.FeeType;
import com.yourproject.entity.User;
import com.yourproject.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long>, JpaSpecificationExecutor<Fee> {

    Page<Fee> findByStudent(User student, Pageable pageable);
    List<Fee> findByStudent(User student);

    Page<Fee> findByStatus(FeeStatus status, Pageable pageable);
    List<Fee> findByStatus(FeeStatus status);

    Page<Fee> findByFeeType(FeeType feeType, Pageable pageable);
    List<Fee> findByFeeType(FeeType feeType);

    Page<Fee> findByMonthAndYear(int month, int year, Pageable pageable);
    List<Fee> findByMonthAndYear(int month, int year);

    Page<Fee> findByStudentAndStatus(User student, FeeStatus status, Pageable pageable);
    List<Fee> findByStudentAndStatus(User student, FeeStatus status);

    // For defaulters list
    Page<Fee> findByStatusInAndBalanceAmountGreaterThan(List<FeeStatus> statuses, BigDecimal minBalance, Pageable pageable);
    List<Fee> findByStatusInAndBalanceAmountGreaterThan(List<FeeStatus> statuses, BigDecimal minBalance);


    // For stats
    long countByStatus(FeeStatus status);
    long countByFeeType(FeeType feeType);

    @Query("SELECT SUM(f.finalAmount) FROM Fee f WHERE f.status = :status AND f.year = :year")
    Optional<BigDecimal> sumFinalAmountByStatusAndYear(@Param("status") FeeStatus status, @Param("year") int year);

    @Query("SELECT SUM(f.paidAmount) FROM Fee f WHERE f.year = :year")
    Optional<BigDecimal> sumPaidAmountByYear(@Param("year") int year);

    @Query("SELECT SUM(f.balanceAmount) FROM Fee f WHERE f.status IN :statuses AND f.year = :year")
    Optional<BigDecimal> sumBalanceAmountByStatusInAndYear(@Param("statuses") List<FeeStatus> statuses, @Param("year") int year);

    List<Fee> findByDueDateBeforeAndStatusIn(LocalDate date, List<FeeStatus> statuses);
}
