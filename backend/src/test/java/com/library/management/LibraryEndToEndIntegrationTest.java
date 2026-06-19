package com.library.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.LoginRequest;
import com.library.management.dto.request.RegisterRequest;
import com.library.management.entity.User;
import com.library.management.enums.UserRole;
import com.library.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests exercising the whole stack
 * (HTTP request -> security filter chain -> controller -> service -> JPA -> H2).
 * Runs under the {@code test} profile against an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "server.servlet.context-path=/")
class LibraryEndToEndIntegrationTest {

    private static final String PASSWORD = "Password1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /** Scenario 1: a student registers and then logs in, receiving a JWT each time. */
    @Test
    void registerThenLogin_returnsJwt() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("alice@student.test", "S-1001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("alice@student.test"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        assertThat(login("alice@student.test")).isNotBlank();
    }

    /** Scenario 2: an invalid registration payload is rejected with HTTP 400 and validation errors. */
    @Test
    void register_withInvalidPayload_returnsValidationErrors() throws Exception {
        RegisterRequest bad = new RegisterRequest();
        bad.setEmail("not-an-email");
        bad.setPassword("weak");
        bad.setConfirmPassword("weak");
        bad.setFirstName("");
        bad.setLastName("");
        bad.setStudentId("");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    /** Scenario 3: role-based access control — an authenticated STUDENT cannot create a book (403). */
    @Test
    void studentCannotCreateBook_isForbidden() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("bob@student.test", "S-2002"))))
                .andExpect(status().isCreated());
        String studentToken = login("bob@student.test");

        mockMvc.perform(post("/v1/books")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook(Set.of(1L)))))
                .andExpect(status().isForbidden());
    }

    /** Scenario 4: an ADMIN creates a category and a book; the catalog search returns a paginated payload. */
    @Test
    void adminCreatesCatalog_andSearchIsPaginated() throws Exception {
        seedUser("admin@lib.test", UserRole.ADMIN, null);
        String adminToken = login("admin@lib.test");

        CreateCategoryRequest cat = new CreateCategoryRequest();
        cat.setName("Software Engineering");
        cat.setDescription("Programming and software design");
        String catBody = mockMvc.perform(post("/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long categoryId = objectMapper.readTree(catBody).get("categoryId").asLong();

        mockMvc.perform(post("/v1/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook(Set.of(categoryId)))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/books/search")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "title")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    /** Scenario 5: paginated + sortable lending endpoints (req. #7) — Loans and Fines. */
    @Test
    void adminCanPageLoansAndFines() throws Exception {
        seedUser("pager@lib.test", UserRole.ADMIN, null);
        String adminToken = login("pager@lib.test");

        mockMvc.perform(get("/v1/loans")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0").param("size", "5").param("sort", "dueDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(5));

        mockMvc.perform(get("/v1/fines")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0").param("size", "5").param("sort", "amount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    // ---------- helpers ----------

    private RegisterRequest registerRequest(String email, String studentId) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword(PASSWORD);
        r.setConfirmPassword(PASSWORD);
        r.setFirstName("Test");
        r.setLastName("User");
        r.setStudentId(studentId);
        return r;
    }

    private CreateBookRequest sampleBook(Set<Long> categoryIds) {
        CreateBookRequest b = new CreateBookRequest();
        b.setTitle("Clean Code");
        b.setAuthor("Robert C. Martin");
        b.setIsbn("9780132350884");
        b.setPublicationYear(2008);
        b.setQuantity(5);
        b.setCategoryIds(categoryIds);
        return b;
    }

    private void seedUser(String email, UserRole role, String studentId) {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .firstName("Seed")
                .lastName(role.name())
                .role(role)
                .studentId(studentId)
                .isActive(true)
                .build());
    }

    private String login(String email) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(PASSWORD);
        String body = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
