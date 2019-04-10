package com.ltf.strategy;

public class CovertDouble implements Convert {

    public Double covertType(String o) {
        return Double.valueOf(o);
    }
}
