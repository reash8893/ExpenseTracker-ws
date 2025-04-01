package com.org.repositories;


import com.org.entities.GroupEntity;
import com.org.entities.UserEntity;
import com.org.models.Groups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

//    @Query("SELECT new com.org.models.UserGroupDetails( " +
//            "ug.id, u.userId, u.userName, g.groupId, g.groupName, g.createdBy, ug.role) " +
//            "FROM UsersGroupEntity ug " +
//            "JOIN ug.user u " +
//            "JOIN ug.group g " +
//            "WHERE g.groupId = :groupId")
//    List<Groups> findGroupDetailsByGroupId(@Param("groupId") Long groupId);
}

