package com.joeun.jwtsecurity5.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.joeun.jwtsecurity5.dto.AuthenticationRequest;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
public class LonginContrller {

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        log.info("username : " + username);
        log.info("password : " + password);

        Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);
        authentication = authenticationManager.authenticate(authentication);

        log.info("isAuthenticated : " + authentication.isAuthenticated());
        
        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }
    
}
