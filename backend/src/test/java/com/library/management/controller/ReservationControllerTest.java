package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.CreateReservationRequest;
import com.library.management.dto.response.MessageResponse;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.entity.User;
import com.library.management.enums.ReservationStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.repository.UserRepository;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.ReservationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

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
    void createReservation_shouldReturnCreated() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setBookId(10L);

        ReservationResponse response = baseResponse(1L, 10L, ReservationStatus.PENDING);
        when(reservationService.createReservation(any(CreateReservationRequest.class), eq(5L))).thenReturn(response);

        mockMvc.perform(post("/v1/reservations/")
                        .with(SecurityMockMvcRequestPostProcessors.user(student()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(1L))
                .andExpect(jsonPath("$.bookId").value(10L));
    }

    @Test
    void getPending_shouldReturnList() throws Exception {
        when(reservationService.getPendingReservations(5L)).thenReturn(List.of(baseResponse(2L, 11L, ReservationStatus.PENDING)));

        mockMvc.perform(get("/v1/reservations/pending")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value(2L));
    }

    @Test
    void getMyReservations_shouldReturnList() throws Exception {
        when(reservationService.getMyReservations(5L)).thenReturn(List.of(baseResponse(5L, 14L, ReservationStatus.PENDING)));

        mockMvc.perform(get("/v1/reservations/my-reservations")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value(5L));
    }

    @Test
    void getReservationById_forbiddenForOtherUser() throws Exception {
        ReservationResponse response = baseResponse(3L, 12L, ReservationStatus.PENDING);
        response.setUserId(99L);
        when(reservationService.getReservationById(3L)).thenReturn(response);

        mockMvc.perform(get("/v1/reservations/3")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReservationById_ownerShouldReturnOk() throws Exception {
        ReservationResponse response = baseResponse(6L, 15L, ReservationStatus.PENDING);
        when(reservationService.getReservationById(6L)).thenReturn(response);

        mockMvc.perform(get("/v1/reservations/6")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(6L));
    }

    @Test
    void getReservationsForBook_shouldReturnList() throws Exception {
        when(reservationService.getReservationsForBook(9L)).thenReturn(List.of(baseResponse(7L, 9L, ReservationStatus.PENDING)));

        mockMvc.perform(get("/v1/reservations/book/9")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(9L));
    }

    @Test
    void cancelReservation_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/v1/reservations/4")
                        .with(SecurityMockMvcRequestPostProcessors.user(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation canceled successfully"));
        Mockito.verify(reservationService).cancelReservation(4L, 5L);
    }

    private User student() {
        return User.builder()
                .userId(5L)
                .email("student@test.com")
                .password("Password1!")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
    }

    private User librarian() {
        return User.builder()
                .userId(1L)
                .email("lib@test.com")
                .password("Password1!")
                .role(UserRole.LIBRARIAN)
                .isActive(true)
                .build();
    }

    private ReservationResponse baseResponse(Long resId, Long bookId, ReservationStatus status) {
        return ReservationResponse.builder()
                .reservationId(resId)
                .userId(5L)
                .userFullName("Test Student")
                .bookId(bookId)
                .bookTitle("Title")
                .bookAuthor("Author")
                .bookIsbn("123")
                .status(status)
                .requestDate(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusHours(48))
                .canBeCanceled(true)
                .isExpired(false)
                .queuePosition(1)
                .build();
    }
}


