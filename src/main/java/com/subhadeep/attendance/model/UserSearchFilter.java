package com.subhadeep.attendance.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchFilter {
    private String role;
    private Boolean approved;
    private String className;

}
