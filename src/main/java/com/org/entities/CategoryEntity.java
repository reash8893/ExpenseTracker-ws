package com.org.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "DESCRIPTION", nullable = false, unique = true, length = 100)
    private String description;

    @OneToMany(mappedBy = "categoryEntity", cascade = CascadeType.ALL)
    private List<ExpenseEntity> expenses;


}
