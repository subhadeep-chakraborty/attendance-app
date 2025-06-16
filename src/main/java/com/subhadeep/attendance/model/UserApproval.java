package com.subhadeep.attendance.model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserApproval {
    private Long userId;
    private boolean approved;
}
