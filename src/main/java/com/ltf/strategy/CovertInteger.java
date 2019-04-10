package com.ltf.strategy;

public class CovertInteger implements Convert {

    public Integer covertType(String o) {
        return Integer.valueOf(o);
    }
}
