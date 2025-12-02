package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.entity.Role;
import com.cinema.hub.backend.repository.RoleRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleAdminController {

    private final RoleRepository roleRepository;

    public RoleAdminController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> findAll() {
        List<RoleDto> data = roleRepository.findAll().stream()
                .map(role -> new RoleDto(role.getId(), role.getName()))
                .toList();
        return ResponseEntity.ok(data);
    }

    public record RoleDto(Integer id, String name) {
    }
}
