package com.ltf;

import com.ltf.annotaiton.MyController;
import com.ltf.annotaiton.MyMapping;
import com.ltf.annotaiton.MyRequestParam;

@MyController
public class HelloController {

    @MyMapping("/hello")
    public String hello() {
        return "hello";
    }

    @MyMapping("/say")
    public String say(@MyRequestParam(value = "name") String name) {
        return name;
    }
}
