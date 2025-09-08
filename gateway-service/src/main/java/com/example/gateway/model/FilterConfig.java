package com.example.gateway.model;

import java.util.Map;

/**
 * Configuration model for route filters
 */
public class FilterConfig {
    private String name;
    private Map<String, Object> args;

    public FilterConfig() {}

    public FilterConfig(String name, Map<String, Object> args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "FilterConfig{" +
                "name='" + name + '\'' +
                ", args=" + args +
                '}';
    }
}