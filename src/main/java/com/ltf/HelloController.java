package com.ltf;

import com.ltf.annotaiton.MyAutowried;
import com.ltf.annotaiton.MyController;
import com.ltf.annotaiton.MyMapping;
import com.ltf.annotaiton.MyRequestParam;

@MyController
public class HelloController {
    @MyAutowried
    private HelloService helloService;

    @MyMapping("/hello")
    public String hello() {
        return helloService.hello();
    }

    @MyMapping("/say")
    public String say(@MyRequestParam(value = "name") String name) {
        return helloService.say(name);
    }
}
