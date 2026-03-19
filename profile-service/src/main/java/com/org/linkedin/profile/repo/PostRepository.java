package com.org.linkedin.profile.repo;

import com.org.linkedin.profile.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);
}
