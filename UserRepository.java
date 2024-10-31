package com.family_tree.familytree;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;
import com.family_tree.familytree.User;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface UserRepository extends CrudRepository<User, Integer> {
    //Check if user already exists by email address
    Optional<User> findByEmail(String email);

    // Find user by username
    Optional<User> findByUsername(String username);

    // Find all users by role
    List<User> findByRole(Role role);

    // Check if a user with a specific email or username exists
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Custom query for searching by email or username keyword
    @Query("SELECT u FROM User u WHERE u.email LIKE %:keyword% OR u.username LIKE %:keyword%")
    List<User> searchUsersByKeyword(@Param("keyword") String keyword);
}
