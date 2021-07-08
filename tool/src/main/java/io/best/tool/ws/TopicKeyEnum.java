package io.best.tool.ws;

public enum TopicKeyEnum {
    UNKNOWN,
    REQ,
    UNSUB,
    SUB;
    public static TopicKeyEnum fromValue(String value) {
        if (value.equalsIgnoreCase("rep")) {
            return TopicKeyEnum.REQ;
        }
        if (value.equalsIgnoreCase("ch") || value.equalsIgnoreCase("topic")) {
            return TopicKeyEnum.SUB;
        }
        for (TopicKeyEnum keyEnum : TopicKeyEnum.values()) {
            if (keyEnum.name().equalsIgnoreCase(value)) {
                return keyEnum;
            }
        }
        return UNKNOWN;
    }
}

