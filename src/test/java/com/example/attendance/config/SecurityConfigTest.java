package com.example.attendance.config;

import com.example.attendance.controller.WorkScheduleController;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkSchedule;
import com.example.attendance.service.CustomUserDetailsService;
import com.example.attendance.service.JwtService;
import com.example.attendance.service.WorkScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkScheduleController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class SecurityConfigTest {

    private static final String EMPLOYEE_EMAIL =
            "second.employee@example.com";

    private static final String ADMIN_EMAIL =
            "admin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private WorkScheduleService workScheduleService;

    @Test
    void protectedEndpointWithoutTokenShouldReturn401()
            throws Exception {

        mockMvc.perform(
                        get("/api/work-schedules/user/2")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void employeeTokenOnAdminEndpointShouldReturn403()
            throws Exception {

        String token = "employee-token";

        UserDetails employee =
                new org.springframework.security.core.userdetails.User(
                        EMPLOYEE_EMAIL,
                        "password",
                        List.of(
                                () -> "ROLE_EMPLOYEE"
                        )
                );

        when(jwtService.extractUsername(token))
                .thenReturn(EMPLOYEE_EMAIL);

        when(userDetailsService.loadUserByUsername(
                EMPLOYEE_EMAIL
        )).thenReturn(employee);

        when(jwtService.isTokenValid(
                token,
                employee
        )).thenReturn(true);

        mockMvc.perform(
                        get("/api/work-schedules/user/2")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void adminTokenOnAdminEndpointShouldBeAllowed()
            throws Exception {

        String token = "admin-token";

        UserDetails admin =
                new org.springframework.security.core.userdetails.User(
                        ADMIN_EMAIL,
                        "password",
                        List.of(
                                () -> "ROLE_ADMIN"
                        )
                );

        when(jwtService.extractUsername(token))
                .thenReturn(ADMIN_EMAIL);

        when(userDetailsService.loadUserByUsername(
                ADMIN_EMAIL
        )).thenReturn(admin);

        when(jwtService.isTokenValid(
                token,
                admin
        )).thenReturn(true);

        User employeeEntity =
                new User();

        employeeEntity.setId(2L);
        employeeEntity.setName(
                "Second Employee"
        );
        employeeEntity.setEmail(
                EMPLOYEE_EMAIL
        );

        WorkSchedule workSchedule =
                new WorkSchedule();

        workSchedule.setId(1L);
        workSchedule.setUser(
                employeeEntity
        );
        workSchedule.setRequiredMinutes(
                480
        );

        when(workScheduleService.getScheduleByUserId(2L))
                .thenReturn(workSchedule);

        mockMvc.perform(
                        get("/api/work-schedules/user/2")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }
}