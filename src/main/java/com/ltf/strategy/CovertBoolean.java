package com.ltf.strategy;

public class CovertBoolean implements Convert {

    public Boolean covertType(String o) {
        return Boolean.valueOf(o);
    }
}
