package com.example.vieva.application.usecases.user;

import com.example.vieva.application.ports.output.RoleRepository;
import com.example.vieva.domain.entities.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnsureDefaultRolesUseCaseImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private EnsureDefaultRolesUseCaseImpl useCase;

    @Test
    @DisplayName("Saves default roles when none exist")
    void savesDefaultRoles_whenNoneExist() {
        when(roleRepository.findByRoleCode(any())).thenReturn(Optional.empty());

        useCase.ensureDefaultRoles();

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(3)).save(captor.capture());

        List<Role> saved = captor.getAllValues();
        assertThat(saved).extracting(Role::getRoleCode)
                .containsExactly("ROLE_ADMIN", "ROLE_LECTURER", "ROLE_STUDENT");
        assertThat(saved).extracting(Role::getRoleName)
                .containsExactly("Administrator", "Lecturer", "Student");
        assertThat(saved).extracting(Role::getDescription)
                .containsExactly(
                        "Quản trị viên hệ thống",
                        "Giảng viên phụ trách môn học và ngân hàng câu hỏi",
                        "Sinh viên tham gia thi vấn đáp"
                );
    }

    @Test
    @DisplayName("Skips roles that already exist")
    void skipsRoles_whenAlreadyExist() {
        when(roleRepository.findByRoleCode("ROLE_ADMIN")).thenReturn(Optional.of(Role.builder().roleCode("ROLE_ADMIN").build()));
        when(roleRepository.findByRoleCode("ROLE_LECTURER")).thenReturn(Optional.of(Role.builder().roleCode("ROLE_LECTURER").build()));
        when(roleRepository.findByRoleCode("ROLE_STUDENT")).thenReturn(Optional.empty());

        useCase.ensureDefaultRoles();

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getRoleCode()).isEqualTo("ROLE_STUDENT");
    }
}
