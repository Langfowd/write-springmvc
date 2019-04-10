package com.ltf;

import com.ltf.annotaiton.MyService;

@MyService
public class HelloServiceImpl implements HelloService {

    public String hello() {
        return "hello";
    }

    public String say(String name) {
        return name;
    }
}
