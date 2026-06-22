package com.library.management.service;

import com.library.management.dto.request.CreateLibrarianRequest;
import com.library.management.dto.request.UpdateProfileRequest;
import com.library.management.dto.response.UserProfileResponse;
import com.library.management.dto.response.UserSummaryResponse;

import java.util.List;

public interface UserService {

    /**
     * Retrieves user profile by id.
     *
     * @param userId user identifier
     * @return profile response
     */
    UserProfileResponse getProfile(Long userId);

    /**
     * Updates profile fields for a user.
     *
     * @param userId  user identifier
     * @param request update request
     * @return updated profile
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Returns list of users with summaries.
     *
     * @return list of user summaries
     */
    List<UserSummaryResponse> getAllUsers();

    /**
     * Creates a librarian user.
     *
     * @param request   librarian creation request
     * @param createdBy admin id
     * @return created user profile
     */
    UserProfileResponse createLibrarian(CreateLibrarianRequest request, Long createdBy);

    /**
     * Activates a user.
     *
     * @param userId user identifier
     */
    void activateUser(Long userId);

    /**
     * Deactivates a user.
     *
     * @param userId user identifier
     */
    void deactivateUser(Long userId);

    /**
     * Determines whether user can borrow books.
     *
     * @param userId user identifier
     * @return true if eligible
     */
    boolean canUserBorrow(Long userId);
}


