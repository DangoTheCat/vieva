package com.example.vieva;

import com.example.vieva.infrastructure.persistence.jpa.RoleJpaRepository;
import com.example.vieva.infrastructure.persistence.jpa.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class VievaApplicationTests {

    @MockitoBean
    private RoleJpaRepository roleJpaRepository;

    @MockitoBean
    private UserJpaRepository userJpaRepository;

	@Test
	void contextLoads() {
	}
}
