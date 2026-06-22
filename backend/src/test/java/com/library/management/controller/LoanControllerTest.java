package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.CheckoutRequest;
import com.library.management.dto.response.CheckoutResponse;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.request.ReturnBookRequest;
import com.library.management.dto.response.ReturnBookResponse;
import com.library.management.entity.User;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.repository.UserRepository;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.LoanService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoanController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanService loanService;

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
    void checkout_shouldReturnCreated() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setBookId(10L);
        request.setUserId(6L);

        LoanResponse loan = baseLoanResponse();
        CheckoutResponse response = CheckoutResponse.builder()
                .loan(loan)
                .message("Checkout successful")
                .success(true)
                .build();

        when(loanService.checkout(any(CheckoutRequest.class), eq(2L))).thenReturn(response);

        mockMvc.perform(post("/v1/loans/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loan.loanId").value(20L))
                .andExpect(jsonPath("$.message").value("Checkout successful"));
    }

    @Test
    void myLoans_shouldReturnList() throws Exception {
        when(loanService.getMyLoans(6L)).thenReturn(List.of(baseLoanResponse()));

        mockMvc.perform(get("/v1/loans/my-loans")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanId").value(20L));
    }

    @Test
    void myActiveLoans_shouldReturnList() throws Exception {
        when(loanService.getActiveLoans(6L)).thenReturn(List.of(baseLoanResponse()));

        mockMvc.perform(get("/v1/loans/my-active-loans")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanId").value(20L));
    }

    @Test
    void getLoanById_forOwnerShouldReturnOk() throws Exception {
        LoanResponse response = baseLoanResponse();
        when(loanService.getLoanById(20L)).thenReturn(response);

        mockMvc.perform(get("/v1/loans/20")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(20L));
    }

    @Test
    void getAllActiveLoans_shouldReturnList() throws Exception {
        when(loanService.getAllActiveLoans()).thenReturn(List.of(baseLoanResponse()));

        mockMvc.perform(get("/v1/loans/active")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanId").value(20L));
    }

    @Test
    void getOverdueLoans_shouldReturnList() throws Exception {
        when(loanService.getOverdueLoans()).thenReturn(List.of(baseLoanResponse()));

        mockMvc.perform(get("/v1/loans/overdue")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanId").value(20L));
    }

    @Test
    void getLoanByReservation_shouldReturnLoan() throws Exception {
        when(loanService.getLoanByReservation(15L)).thenReturn(baseLoanResponse());

        mockMvc.perform(get("/v1/loans/reservation/15")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(20L));
    }

    @Test
    void returnBook_shouldReturnOk() throws Exception {
        ReturnBookRequest request = new ReturnBookRequest();
        request.setLoanId(20L);

        ReturnBookResponse response = ReturnBookResponse.builder()
                .loanId(20L)
                .wasOverdue(false)
                .fine(null)
                .message("Returned")
                .build();

        when(loanService.returnBook(any(ReturnBookRequest.class), eq(2L))).thenReturn(response);

        mockMvc.perform(post("/v1/loans/return")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Returned"));
    }

    @Test
    void getLoanById_forbiddenForOtherUser() throws Exception {
        LoanResponse response = baseLoanResponse();
        response.setUserId(999L);
        when(loanService.getLoanById(30L)).thenReturn(response);

        mockMvc.perform(get("/v1/loans/30")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isForbidden());
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

    private User student() {
        return User.builder()
                .userId(6L)
                .email("student@test.com")
                .password("Password1!")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
    }

    private LoanResponse baseLoanResponse() {
        return LoanResponse.builder()
                .loanId(20L)
                .userId(6L)
                .userFullName("Student User")
                .bookId(10L)
                .bookTitle("Title")
                .bookAuthor("Author")
                .bookIsbn("123")
                .status(LoanStatus.ACTIVE)
                .checkoutDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .isRenewable(true)
                .canBeRenewed(true)
                .build();
    }
}


