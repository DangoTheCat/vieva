package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.input.user.EnsureDefaultRolesUseCase;
import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.domain.entities.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class EnsureDefaultRolesUseCaseImpl implements EnsureDefaultRolesUseCase {

    private final RoleRepository roleRepository;

    @Override
    public void ensureDefaultRoles() {
        initRoleIfNotExist("ROLE_ADMIN", "Administrator", "Quản trị viên hệ thống");
        initRoleIfNotExist("ROLE_LECTURER", "Lecturer", "Giảng viên phụ trách môn học và ngân hàng câu hỏi");
        initRoleIfNotExist("ROLE_STUDENT", "Student", "Sinh viên tham gia thi vấn đáp");
    }

    private void initRoleIfNotExist(String roleCode, String roleName, String description) {
        if (roleRepository.findByRoleCode(roleCode).isEmpty()) {
            Role role = Role.builder()
                    .roleCode(roleCode)
                    .roleName(roleName)
                    .description(description)
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(role);
            log.info("Initialized default role: {}", roleCode);
        }
    }
}
