package com.vapedealspot.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageSectionDto {

    private String id;
    private String type;
    private boolean enabled = true;
    private int order = 0;
    private Map<String, Object> props = new LinkedHashMap<>();

    public PageSectionDto() {
    }

    public PageSectionDto(String id, String type, boolean enabled, int order, Map<String, Object> props) {
        this.id = id;
        this.type = type;
        this.enabled = enabled;
        this.order = order;
        if (props != null) {
            this.props = props;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props != null ? props : new LinkedHashMap<>();
    }
}

