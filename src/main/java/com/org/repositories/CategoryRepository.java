package com.org.repositories;

import com.org.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByDescription(String description);

    @Query("SELECT c FROM CategoryEntity c")
    Optional<CategoryEntity> getAllCategories();
}

