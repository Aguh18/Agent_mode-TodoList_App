package com.example.demo.controller.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todo/")
public class TodoController {

    @PostMapping("add")
    public String addTodo() {

        return "hallo dekk";
    }

}
