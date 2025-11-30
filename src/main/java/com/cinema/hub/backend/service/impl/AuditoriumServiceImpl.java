package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.auditorium.AuditoriumRequest;
import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.entity.Seat;
import com.cinema.hub.backend.entity.SeatType;
import com.cinema.hub.backend.mapper.AuditoriumMapper;
import com.cinema.hub.backend.repository.AuditoriumRepository;
import com.cinema.hub.backend.repository.SeatRepository;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.service.AuditoriumService;
import com.cinema.hub.backend.specification.AuditoriumSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
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

    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumMapper auditoriumMapper;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AuditoriumServiceImpl(AuditoriumRepository auditoriumRepository,
                                 AuditoriumMapper auditoriumMapper,
                                 SeatRepository seatRepository,
                                 ShowtimeRepository showtimeRepository) {
        this.auditoriumRepository = auditoriumRepository;
        this.auditoriumMapper = auditoriumMapper;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public AuditoriumResponse create(AuditoriumRequest request) {
        Auditorium auditorium = new Auditorium();
        applyRequest(auditorium, request);
        Auditorium saved = auditoriumRepository.save(auditorium);
        createDefaultSeats(saved);
        return auditoriumMapper.toResponse(saved);
    }

    @Override
    public AuditoriumResponse update(int id, AuditoriumRequest request) {
        Auditorium auditorium = getEntity(id);
        applyRequest(auditorium, request);
        return auditoriumMapper.toResponse(auditoriumRepository.save(auditorium));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriumResponse get(int id) {
        return auditoriumMapper.toResponse(getEntity(id));
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
        auditorium.setActive(active);
        return auditoriumMapper.toResponse(auditoriumRepository.save(auditorium));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditoriumResponse> search(String name, Boolean active, Pageable pageable) {
        Page<Auditorium> page = auditoriumRepository.findAll(
                AuditoriumSpecifications.filter(name, active), pageable);
        return PageResponse.from(page.map(auditoriumMapper::toResponse));
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

    private void createDefaultSeats(Auditorium auditorium) {
        Integer rows = auditorium.getNumberOfRows();
        Integer columns = auditorium.getNumberOfColumns();
        if (rows == null || rows <= 0 || columns == null || columns <= 0) {
            return;
        }

        int totalRows = rows;
        int totalColumns = columns;
        int coupleRows = determineCoupleRows(totalRows);
        int normalRows = (int) Math.floor(totalRows * 0.2);
        if (normalRows <= 0 && totalRows > 0) {
            normalRows = 1;
        }
        if (normalRows + coupleRows > totalRows) {
            normalRows = Math.max(0, totalRows - coupleRows);
        }
        int vipRows = totalRows - normalRows - coupleRows;
        if (vipRows < 0) {
            vipRows = 0;
            normalRows = totalRows - coupleRows;
        }

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
            for (int colIndex = 1; colIndex <= seatsInRow; colIndex++) {
                Seat seat = Seat.builder()
                        .auditorium(auditorium)
                        .rowLabel(rowLabel)
                        .seatNumber(colIndex)
                        .seatType(seatTypeForRow)
                        .active(Boolean.TRUE)
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

    private int determineCoupleRows(int totalRows) {
        if (totalRows >= 20) {
            return 2;
        }
        if (totalRows >= 10) {
            return 1;
        }
        return 0;
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
}
