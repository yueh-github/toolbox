package io.best.tool.netty.pojo;

import java.io.Serializable;
import java.util.List;

public class TransportObject implements Serializable {

    private String name;

    private Integer id;

    private List<String> userList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<String> getUserList() {
        return userList;
    }

    public void setUserList(List<String> userList) {
        this.userList = userList;
    }


    @Override
    public String toString() {
        return "TransportObject{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", list=" + userList +
                '}';
    }
}
