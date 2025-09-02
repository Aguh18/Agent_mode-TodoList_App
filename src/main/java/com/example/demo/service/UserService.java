package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.model.UserEntity;

@Service
public class UserService {

    public UserEntity teguhString() {

        UserEntity user = new UserEntity();

        user.setUsername("hallo");

        return user;
    }

}
