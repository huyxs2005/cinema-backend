package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
