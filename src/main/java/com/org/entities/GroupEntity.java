package com.org.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "GROUPS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GROUP_ID")
    private Long groupId;

    @Column(name = "GROUP_NAME", nullable = false)
    private String groupName;

    @ManyToOne
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "group")
    private List<ExpenseEntity> expenses;

    @ManyToMany
    @JoinTable(
            name = "USER_GROUPS",
            joinColumns = @JoinColumn(name = "GROUP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USER_ID")
    )
    private List<UserEntity> users;
}

