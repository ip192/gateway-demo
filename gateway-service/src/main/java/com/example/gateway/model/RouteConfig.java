package com.example.gateway.model;

import java.util.List;
import java.util.Map;

/**
 * Configuration model for a single route definition
 */
public class RouteConfig {
    private String id;
    private String uri;
    private List<PredicateConfig> predicates;
    private List<FilterConfig> filters;
    private RouteMetadata metadata;

    public RouteConfig() {}

    public RouteConfig(String id, String uri, List<PredicateConfig> predicates, 
                      List<FilterConfig> filters, RouteMetadata metadata) {
        this.id = id;
        this.uri = uri;
        this.predicates = predicates;
        this.filters = filters;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<PredicateConfig> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<PredicateConfig> predicates) {
        this.predicates = predicates;
    }

    public List<FilterConfig> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterConfig> filters) {
        this.filters = filters;
    }

    public RouteMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(RouteMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "RouteConfig{" +
                "id='" + id + '\'' +
                ", uri='" + uri + '\'' +
                ", predicates=" + predicates +
                ", filters=" + filters +
                ", metadata=" + metadata +
                '}';
    }
}