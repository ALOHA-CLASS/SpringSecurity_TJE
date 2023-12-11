package com.joeun.jwt.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.joeun.jwt.domain.AuthenticationRequest;
import com.joeun.jwt.prop.JwtProps;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
public class LoginController {
    
    @Autowired
    private JwtProps jwtProps;

    @PostMapping("login")
    public ResponseEntity login(@RequestBody AuthenticationRequest authReq) {
        String username = authReq.getUsername();
        String password = authReq.getPassword();

        log.info("username : " + username );
        log.info("password : " + password );

        List<String> roles = new ArrayList<>();
        roles.add("ROLE_USER");

        byte[] secretKey = jwtProps.getSecretKey().getBytes();


        
        return new ResponseEntity<>(null);
    }
    


}
