package com.example.vieva;

import com.example.vieva.infrastructure.database.RoleJpaRepository;
import com.example.vieva.infrastructure.database.UserJpaRepository;
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
