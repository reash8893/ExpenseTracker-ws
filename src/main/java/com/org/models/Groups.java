package com.org.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Groups {
    public long groupId;
    public long userId;
    public String groupName;
    public String userName;
    public String createdBy;

}
