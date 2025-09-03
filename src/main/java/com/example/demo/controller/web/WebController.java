package com.example.demo.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "modern-login";
    }

    @GetMapping("/register")
    public String register() {
        return "modern-register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "modern-dashboard";
    }
}
