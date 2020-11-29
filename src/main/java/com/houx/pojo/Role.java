package com.houx.pojo;

import java.io.Serializable;

/**
 * @Author: HouX
 * @Date: 2020/11/29
 * @Description:
 */
public class Role implements Serializable {

    private static final long serialVersionUID = 6977402643848374753L;

    private long id;

    private String roleName;

    private String note;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}