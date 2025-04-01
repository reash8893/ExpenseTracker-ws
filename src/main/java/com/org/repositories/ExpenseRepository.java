package com.org.repositories;

import com.org.entities.CategoryEntity;
import com.org.entities.ExpenseEntity;
import com.org.entities.GroupEntity;
import com.org.entities.UserEntity;
import com.org.models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    @Query("SELECT new com.org.models.Expense(e.expenseId, e.title, e.amount, e.expenseDate, " +
            "c.description, u.userId, u.userName) " +
            "FROM ExpenseEntity e " +
            "JOIN e.categoryEntity c " +
            "JOIN e.userEntity u " +
            "WHERE u.userId = :userId")
    List<Expense> findExpensesWithUserAndCategory(@Param("userId") Long userId);

    List<ExpenseEntity> findByGroup(GroupEntity group);
}