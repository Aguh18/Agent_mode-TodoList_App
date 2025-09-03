package com.example.demo.dto.request;

import java.util.Map;

public class ActionRequest {
    private String action;
    private Map<String, Object> parameters;

    public ActionRequest() {}

    public ActionRequest(String action, Map<String, Object> parameters) {
        this.action = action;
        this.parameters = parameters;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
