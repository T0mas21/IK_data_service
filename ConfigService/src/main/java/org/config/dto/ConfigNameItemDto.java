package org.config.dto;

public class ConfigNameItemDto {
    private String name;

    public ConfigNameItemDto() {}

    public ConfigNameItemDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}