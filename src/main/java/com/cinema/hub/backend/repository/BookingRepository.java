package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from Booking b
        join fetch b.showtime s
        where b.id = :id
    """)
    Optional<Booking> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByBookingCode(String bookingCode);

    @Query("""
        select b from Booking b
        join fetch b.showtime st
        join fetch st.movie m
        join fetch st.auditorium a
        where b.bookingCode = :bookingCode
    """)
    Optional<Booking> findByBookingCodeWithShowtime(@Param("bookingCode") String bookingCode);

    @Query("""
        select b from Booking b
        join fetch b.showtime st
        join fetch st.movie m
        join fetch st.auditorium a
        where b.bookingCode = :bookingCode
        and b.user = :user
    """)
    Optional<Booking> findByBookingCodeAndUserWithShowtime(@Param("bookingCode") String bookingCode,
                                                           @Param("user") UserAccount user);

    Page<Booking> findByUserAndBookingStatusOrderByCreatedAtDesc(UserAccount user,
                                                                 BookingStatus bookingStatus,
                                                                 Pageable pageable);

    List<Booking> findTop10ByUserAndBookingStatusOrderByCreatedAtDesc(UserAccount user,
                                                                      BookingStatus bookingStatus);

    @Query("""
        select b from Booking b
        join fetch b.showtime st
        join fetch st.movie m
        join fetch st.auditorium a
        where b.id = :bookingId
    """)
    Optional<Booking> findDetailedById(@Param("bookingId") Integer bookingId);

    @Query("""
        select b from Booking b
        join fetch b.showtime st
        join fetch st.movie m
        join fetch st.auditorium a
        where b.bookingStatus = :status
        order by b.createdAt asc
    """)
    List<Booking> findByStatusWithShowtime(@Param("status") BookingStatus status);

    @Query("""
        select b from Booking b
        join fetch b.showtime st
        join fetch st.movie m
        join fetch st.auditorium a
        left join fetch b.createdByStaff
        where b.createdAt >= :startOfDay and b.createdAt < :endOfDay
        order by b.createdAt desc
    """)
    List<Booking> findByDateWithShowtime(@Param("startOfDay") OffsetDateTime startOfDay,
                                         @Param("endOfDay") OffsetDateTime endOfDay);

    @Query("""
        select b from Booking b
        join fetch b.showtime st
        join fetch st.movie m
        join fetch st.auditorium a
        left join fetch b.createdByStaff
        where b.createdAt >= :startOfDay and b.createdAt < :endOfDay
        and b.bookingStatus IN :statuses
        order by b.createdAt desc
    """)
    List<Booking> findByDateAndStatusesWithShowtime(@Param("startOfDay") OffsetDateTime startOfDay,
                                                    @Param("endOfDay") OffsetDateTime endOfDay,
                                                    @Param("statuses") List<BookingStatus> statuses);

    List<Booking> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    Page<Booking> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    List<Booking> findByPaidAtBetween(OffsetDateTime start, OffsetDateTime end);

    List<Booking> findByCancelledAtBetween(OffsetDateTime start, OffsetDateTime end);

    @Query("""
        select coalesce(sum(b.finalAmount), 0)
        from Booking b
        where b.paymentStatus = com.cinema.hub.backend.entity.enums.PaymentStatus.Paid
          and b.createdAt between :start and :end
    """)
    BigDecimal sumPaidAmountBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
        select b.paymentStatus, count(b)
        from Booking b
        where b.createdAt between :start and :end
        group by b.paymentStatus
    """)
    List<Object[]> countByPaymentStatusBetween(@Param("start") OffsetDateTime start,
                                               @Param("end") OffsetDateTime end);

    @Query("""
        select b.bookingStatus, count(b)
        from Booking b
        where b.createdAt between :start and :end
        group by b.bookingStatus
    """)
    List<Object[]> countByBookingStatusBetween(@Param("start") OffsetDateTime start,
                                               @Param("end") OffsetDateTime end);

    @Query("""
        select coalesce(lower(b.paymentMethod), 'unknown'), count(b), coalesce(sum(b.finalAmount), 0)
        from Booking b
        where b.paymentStatus = com.cinema.hub.backend.entity.enums.PaymentStatus.Paid
          and b.createdAt between :start and :end
        group by lower(b.paymentMethod)
    """)
    List<Object[]> aggregateRevenueByPaymentMethodBetween(@Param("start") OffsetDateTime start,
                                                          @Param("end") OffsetDateTime end);

    @Query("select min(b.createdAt) from Booking b")
    OffsetDateTime findEarliestCreatedAt();

    @Query("select max(b.createdAt) from Booking b")
    OffsetDateTime findLatestCreatedAt();

    @Query("""
        select case when count(b) > 0 then true else false end
        from Booking b
        join b.showtime st
        where st.id = :showtimeId
          and st.endTime > :threshold
          and b.bookingStatus in :protectedStatuses
    """)
    boolean existsActiveBookingForShowtime(@Param("showtimeId") Integer showtimeId,
                                           @Param("threshold") LocalDateTime threshold,
                                           @Param("protectedStatuses") Collection<BookingStatus> protectedStatuses);

    @Query("""
        select case when count(b) > 0 then true else false end
        from Booking b
        join b.showtime st
        where st.auditorium.id = :auditoriumId
          and st.endTime > :threshold
          and b.bookingStatus in :protectedStatuses
    """)
    boolean existsActiveBookingForAuditorium(@Param("auditoriumId") Integer auditoriumId,
                                             @Param("threshold") LocalDateTime threshold,
                                             @Param("protectedStatuses") Collection<BookingStatus> protectedStatuses);
}
