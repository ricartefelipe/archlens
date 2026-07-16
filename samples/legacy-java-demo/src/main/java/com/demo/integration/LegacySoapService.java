package com.demo.integration;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class LegacySoapService {

    @WebMethod
    public String ping(String message) {
        return "pong:" + message;
    }
}
