package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.auditorium.AuditoriumRequest;
import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.mapper.AuditoriumMapper;
import com.cinema.hub.backend.repository.AuditoriumRepository;
import com.cinema.hub.backend.service.AuditoriumService;
import com.cinema.hub.backend.specification.AuditoriumSpecifications;
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

    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumMapper auditoriumMapper;

    public AuditoriumServiceImpl(AuditoriumRepository auditoriumRepository,
                                 AuditoriumMapper auditoriumMapper) {
        this.auditoriumRepository = auditoriumRepository;
        this.auditoriumMapper = auditoriumMapper;
    }

    @Override
    public AuditoriumResponse create(AuditoriumRequest request) {
        Auditorium auditorium = new Auditorium();
        applyRequest(auditorium, request);
        return auditoriumMapper.toResponse(auditoriumRepository.save(auditorium));
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
        try {
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
}
