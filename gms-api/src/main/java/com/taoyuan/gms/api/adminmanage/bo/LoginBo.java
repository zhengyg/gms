package com.taoyuan.gms.api.adminmanage.bo;

import lombok.Data;

@Data
public class LoginBo {
    private String id;

    private String type;

    private String ip;

    private String name;

    private String status;

    private String time;
}