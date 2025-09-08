package com.example.gateway.model;

import java.util.Map;

/**
 * Configuration model for route predicates
 */
public class PredicateConfig {
    private String name;
    private Map<String, String> args;

    public PredicateConfig() {}

    public PredicateConfig(String name, Map<String, String> args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "PredicateConfig{" +
                "name='" + name + '\'' +
                ", args=" + args +
                '}';
    }
}