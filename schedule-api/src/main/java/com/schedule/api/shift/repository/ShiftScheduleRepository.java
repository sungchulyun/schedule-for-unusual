package com.schedule.api.shift.repository;

import com.schedule.api.shift.domain.ShiftSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, String> {

    Optional<ShiftSchedule> findByGroupIdAndDateAndDeletedAtIsNull(String groupId, LocalDate date);

    @Query("""
            select s
            from ShiftSchedule s
            where s.groupId = :groupId
              and s.deletedAt is null
              and s.date between :startDate and :endDate
            order by s.date asc
            """)
    List<ShiftSchedule> findActiveShiftsInRange(String groupId, LocalDate startDate, LocalDate endDate);
}
