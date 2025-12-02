package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.auditorium.AuditoriumRequest;
import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.entity.Seat;
import com.cinema.hub.backend.entity.SeatType;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.mapper.AuditoriumMapper;
import com.cinema.hub.backend.repository.AuditoriumRepository;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.SeatRepository;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.service.AuditoriumService;
import com.cinema.hub.backend.specification.AuditoriumSpecifications;
import com.cinema.hub.backend.util.TimeProvider;
import com.cinema.hub.backend.util.SeatLayoutCalculator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuditoriumServiceImpl implements AuditoriumService {

    private static final int STANDARD_SEAT_TYPE_ID = 1;
    private static final int VIP_SEAT_TYPE_ID = 2;
    private static final int COUPLE_SEAT_TYPE_ID = 3;
    private static final EnumSet<BookingStatus> PROTECTED_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.Pending, BookingStatus.Confirmed);

    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumMapper auditoriumMapper;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AuditoriumServiceImpl(AuditoriumRepository auditoriumRepository,
                                 AuditoriumMapper auditoriumMapper,
                                 SeatRepository seatRepository,
                                 ShowtimeRepository showtimeRepository,
                                 BookingRepository bookingRepository) {
        this.auditoriumRepository = auditoriumRepository;
        this.auditoriumMapper = auditoriumMapper;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public AuditoriumResponse create(AuditoriumRequest request) {
        Auditorium auditorium = new Auditorium();
        applyRequest(auditorium, request);
        SeatLayoutCalculator.SeatRowDistribution distribution = distributionFromRequest(request);
        Auditorium saved = auditoriumRepository.save(auditorium);
        createDefaultSeats(saved, distribution);
        return auditoriumMapper.toResponse(saved, distribution);
    }

    @Override
    public AuditoriumResponse update(int id, AuditoriumRequest request) {
        Auditorium auditorium = getEntity(id);
        Boolean previousActive = auditorium.getActive();
        int currentRows = auditorium.getNumberOfRows();
        int currentColumns = auditorium.getNumberOfColumns();
        SeatLayoutCalculator.SeatRowDistribution currentDistribution = summarizeSeatRows(auditorium);
        SeatLayoutCalculator.SeatRowDistribution requestedDistribution = distributionFromRequest(request);

        boolean layoutChanged = hasSeatLayoutChanged(currentRows, currentColumns, currentDistribution, request, requestedDistribution);

        applyRequest(auditorium, request);
        Auditorium saved;
        if (layoutChanged) {
            if (showtimeRepository.existsByAuditorium_Id(auditorium.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể thay đổi sơ đồ ghế vì phòng vẫn đang có suất chiếu.");
            }
            seatRepository.deleteByAuditorium_Id(auditorium.getId());
            entityManager.flush();
            saved = auditoriumRepository.save(auditorium);
            createDefaultSeats(saved, requestedDistribution);
        } else {
            saved = auditoriumRepository.save(auditorium);
        }
        if (!Objects.equals(previousActive, saved.getActive())) {
            propagateActivation(saved.getId(), Boolean.TRUE.equals(saved.getActive()));
        }
        SeatLayoutCalculator.SeatRowDistribution responseDistribution =
                layoutChanged ? requestedDistribution : currentDistribution;
        return auditoriumMapper.toResponse(saved, responseDistribution);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriumResponse get(int id) {
        Auditorium auditorium = getEntity(id);
        return auditoriumMapper.toResponse(auditorium, summarizeSeatRows(auditorium));
    }

    @Override
    public void delete(int id) {
        Auditorium auditorium = getEntity(id);
        if (showtimeRepository.existsByAuditorium_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phòng chiếu này vẫn còn suất chiếu đang sử dụng. Hãy xóa hoặc chuyển các suất chiếu trước khi xóa phòng.");
        }
        try {
            seatRepository.deleteByAuditorium_Id(id);
            auditoriumRepository.delete(auditorium);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa phòng chiếu đang được sử dụng.");
        }
    }

    @Override
    public AuditoriumResponse updateActive(int id, boolean active) {
        Auditorium auditorium = getEntity(id);
        if (!active && hasFutureBookingsForAuditorium(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "KhA'ng th��� vA' hi���u hA3a phA�ng chi���u �`ang cA3 khA�ch �`A�t vAc cho cA�c su���t chi���u ch��a kA�t thA�c.");
        }
        auditorium.setActive(active);
        propagateActivation(id, active);
        Auditorium saved = auditoriumRepository.save(auditorium);
        return auditoriumMapper.toResponse(saved, summarizeSeatRows(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditoriumResponse> search(String name, Boolean active, Pageable pageable) {
        Page<Auditorium> page = auditoriumRepository.findAll(
                AuditoriumSpecifications.filter(name, active), pageable);
        return PageResponse.from(page.map(auditorium ->
                auditoriumMapper.toResponse(auditorium, summarizeSeatRows(auditorium))));
    }

    private Auditorium getEntity(int id) {
        return auditoriumRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phòng chiếu: " + id));
    }

    private void applyRequest(Auditorium auditorium, AuditoriumRequest request) {
        auditorium.setName(request.getName());
        auditorium.setNumberOfRows(request.getNumberOfRows());
        auditorium.setNumberOfColumns(request.getNumberOfColumns());
        auditorium.setActive(request.getActive());
    }

    private void createDefaultSeats(Auditorium auditorium,
                                    SeatLayoutCalculator.SeatRowDistribution distribution) {
        Integer rows = auditorium.getNumberOfRows();
        Integer columns = auditorium.getNumberOfColumns();
        if (rows == null || rows <= 0 || columns == null || columns <= 0) {
            return;
        }

        int totalRows = rows;
        int totalColumns = columns;
        int normalRows = distribution.standardRows();
        int coupleRows = distribution.coupleRows();

        SeatType standardSeat = entityManager.getReference(SeatType.class, STANDARD_SEAT_TYPE_ID);
        SeatType vipSeat = entityManager.getReference(SeatType.class, VIP_SEAT_TYPE_ID);
        SeatType coupleSeat = entityManager.getReference(SeatType.class, COUPLE_SEAT_TYPE_ID);

        List<Seat> seats = new ArrayList<>(totalRows * Math.max(1, totalColumns));
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            String rowLabel = toRowLabel(rowIndex);
            SeatType seatTypeForRow = resolveSeatTypeForRow(rowIndex, totalRows,
                    normalRows, coupleRows, vipSeat, standardSeat, coupleSeat);
            int seatsInRow = totalColumns;
            if (seatTypeForRow == coupleSeat && (totalColumns % 2 != 0)) {
                seatsInRow = Math.max(0, totalColumns - 1);
            }
            if (seatsInRow <= 0) {
                continue;
            }
            boolean seatActive = Boolean.TRUE.equals(auditorium.getActive());
            for (int colIndex = 1; colIndex <= seatsInRow; colIndex++) {
                Seat seat = Seat.builder()
                        .auditorium(auditorium)
                        .rowLabel(rowLabel)
                        .seatNumber(colIndex)
                        .seatType(seatTypeForRow)
                        .active(seatActive)
                        .build();
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
    }

    private SeatType resolveSeatTypeForRow(int rowIndex,
                                           int totalRows,
                                           int normalRows,
                                           int coupleRows,
                                           SeatType vipSeat,
                                           SeatType standardSeat,
                                           SeatType coupleSeat) {
        if (rowIndex < normalRows) {
            return standardSeat;
        }
        if (coupleRows > 0 && rowIndex >= totalRows - coupleRows) {
            return coupleSeat;
        }
        return vipSeat;
    }

    private String toRowLabel(int index) {
        StringBuilder builder = new StringBuilder();
        int current = index;
        while (current >= 0) {
            int remainder = current % 26;
            builder.insert(0, (char) ('A' + remainder));
            current = (current / 26) - 1;
        }
        return builder.toString();
    }

    private SeatLayoutCalculator.SeatRowDistribution distributionFromRequest(AuditoriumRequest request) {
        int totalRows = request.getNumberOfRows() != null ? request.getNumberOfRows() : 0;
        return SeatLayoutCalculator.fromUserInput(totalRows,
                request.getNormalRowCount(),
                request.getCoupleRowCount());
    }

    private SeatLayoutCalculator.SeatRowDistribution summarizeSeatRows(Auditorium auditorium) {
        List<Object[]> rows = seatRepository.countDistinctRowsBySeatType(auditorium.getId());
        int standard = 0;
        int vip = 0;
        int couple = 0;
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Integer typeId = row[0] != null ? ((Number) row[0]).intValue() : null;
            int count = row[1] != null ? ((Number) row[1]).intValue() : 0;
            if (typeId == STANDARD_SEAT_TYPE_ID) {
                standard = count;
            } else if (typeId == VIP_SEAT_TYPE_ID) {
                vip = count;
            } else if (typeId == COUPLE_SEAT_TYPE_ID) {
                couple = count;
            }
        }
        return new SeatLayoutCalculator.SeatRowDistribution(standard, vip, couple);
    }

    private boolean hasSeatLayoutChanged(Integer currentRows,
                                         Integer currentColumns,
                                         SeatLayoutCalculator.SeatRowDistribution currentDistribution,
                                         AuditoriumRequest request,
                                         SeatLayoutCalculator.SeatRowDistribution requestedDistribution) {
        if (!Objects.equals(currentRows, request.getNumberOfRows())) {
            return true;
        }
        if (!Objects.equals(currentColumns, request.getNumberOfColumns())) {
            return true;
        }
        return !Objects.equals(currentDistribution, requestedDistribution);
    }

    private void propagateActivation(int auditoriumId, boolean active) {
        showtimeRepository.updateActiveByAuditoriumId(auditoriumId, active);
        seatRepository.updateActiveByAuditoriumId(auditoriumId, active);
    }

    private boolean hasFutureBookingsForAuditorium(int auditoriumId) {
        return bookingRepository.existsActiveBookingForAuditorium(
                auditoriumId,
                TimeProvider.now().toLocalDateTime(),
                PROTECTED_BOOKING_STATUSES);
    }
}
