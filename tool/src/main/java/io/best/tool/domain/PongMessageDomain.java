package io.best.tool.domain;

import lombok.Data;

@Data
public class PongMessageDomain {

    private String message;

    private Long time;

    public PongMessageDomain(String message, Long time) {
        this.message = message;
        this.time = time;
    }
}
