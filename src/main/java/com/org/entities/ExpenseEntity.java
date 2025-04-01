package com.org.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "EXPENSES") // Force Hibernate to use exact uppercase name
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPENSE_ID") // Ensure Hibernate uses the exact column name
    private Long expenseId;

    @Column(name = "TITLE", nullable = false, length = 255)
    private String title;

    @Column(name = "AMOUNT", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Temporal(TemporalType.DATE)
    @Column(name = "EXPENSE_DATE", nullable = false)
    private Date expenseDate;

    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID", nullable = false) // Force exact column name
    private CategoryEntity categoryEntity;

    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

    @ManyToOne
    @JoinColumn(name = "GROUP_ID")
    private GroupEntity group;

    @ManyToMany
    @JoinTable(
            name = "USER_GROUP",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "GROUP_ID")
    )
    private Set<GroupEntity> groups = new HashSet<>();

}
