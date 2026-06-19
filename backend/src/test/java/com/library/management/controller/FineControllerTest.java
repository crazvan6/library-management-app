package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.PayFineRequest;
import com.library.management.dto.request.WaiveFineRequest;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.UserFinesSummaryResponse;
import com.library.management.entity.User;
import com.library.management.enums.FineStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.repository.UserRepository;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.FineService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FineController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class FineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FineService fineService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setupFilter() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void getMyFines_shouldReturnSummary() throws Exception {
        UserFinesSummaryResponse summary = UserFinesSummaryResponse.builder()
                .userId(10L)
                .userFullName("Student User")
                .totalOutstandingFines(BigDecimal.valueOf(5))
                .pendingFinesCount(1)
                .paidFinesCount(0)
                .waivedFinesCount(0)
                .canBorrow(true)
                .build();

        when(fineService.getMyFinesSummary(10L)).thenReturn(summary);

        mockMvc.perform(get("/v1/fines/my-fines")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutstandingFines").value(5));
    }

    @Test
    void getMyPendingFines_shouldReturnList() throws Exception {
        when(fineService.getPendingFines(10L)).thenReturn(List.of(baseFineResponse()));

        mockMvc.perform(get("/v1/fines/my-fines/pending")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fineId").value(1L));
    }

    @Test
    void getFineById_ownerShouldReturnOk() throws Exception {
        when(fineService.getFineById(1L)).thenReturn(baseFineResponse());

        mockMvc.perform(get("/v1/fines/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fineId").value(1L));
    }

    @Test
    void getFineById_forbiddenForOtherUser() throws Exception {
        FineResponse otherUserFine = baseFineResponse();
        otherUserFine.setUserId(999L);
        when(fineService.getFineById(1L)).thenReturn(otherUserFine);

        mockMvc.perform(get("/v1/fines/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFineByLoanId_staffShouldReturnOk() throws Exception {
        when(fineService.getFineByLoanId(5L)).thenReturn(baseFineResponse());

        mockMvc.perform(get("/v1/fines/loan/5")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(20L));
    }

    @Test
    void getAllPendingFines_shouldReturnList() throws Exception {
        when(fineService.getAllPendingFines()).thenReturn(List.of(baseFineResponse()));

        mockMvc.perform(get("/v1/fines/pending")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fineId").value(1L));
    }

    @Test
    void payFine_shouldReturnUpdatedFine() throws Exception {
        PayFineRequest request = new PayFineRequest();
        request.setFineId(1L);
        request.setPaymentMethod("CASH");

        FineResponse paid = baseFineResponse();
        paid.setStatus(FineStatus.PAID);

        when(fineService.payFine(any(PayFineRequest.class), any())).thenReturn(paid);

        mockMvc.perform(post("/v1/fines/pay")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void waiveFine_shouldReturnWaivedFine() throws Exception {
        WaiveFineRequest request = new WaiveFineRequest();
        request.setFineId(1L);
        request.setReason("Waived");

        FineResponse waived = baseFineResponse();
        waived.setStatus(FineStatus.WAIVED);

        when(fineService.waiveFine(any(WaiveFineRequest.class), any())).thenReturn(waived);

        mockMvc.perform(post("/v1/fines/waive")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAIVED"));
    }

    private FineResponse baseFineResponse() {
        return FineResponse.builder()
                .fineId(1L)
                .loanId(20L)
                .userId(10L)
                .userFullName("Student User")
                .amount(BigDecimal.valueOf(5))
                .status(FineStatus.PENDING)
                .daysOverdue(2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User student() {
        return User.builder()
                .userId(10L)
                .email("student@test.com")
                .password("Password1!")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
    }

    private User librarian() {
        return User.builder()
                .userId(2L)
                .email("lib@test.com")
                .password("Password1!")
                .role(UserRole.LIBRARIAN)
                .isActive(true)
                .build();
    }

    private User admin() {
        return User.builder()
                .userId(1L)
                .email("admin@test.com")
                .password("Password1!")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
    }
}


