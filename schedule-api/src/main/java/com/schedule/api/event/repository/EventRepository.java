package com.schedule.api.event.repository;

import com.schedule.api.event.domain.Event;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, String> {

    Optional<Event> findByIdAndGroupIdAndDeletedAtIsNull(String id, String groupId);

    @Query("""
            select e
            from Event e
            where e.groupId = :groupId
              and e.deletedAt is null
              and e.startDate <= :endDate
              and e.endDate >= :startDate
            order by e.startDate asc, e.endDate asc, e.createdAt asc
            """)
    List<Event> findActiveEventsInRange(String groupId, LocalDate startDate, LocalDate endDate);
}
