package org.config.dto;

import java.util.List;

public class ConfigNamesDto {
    private List<String> names;

    public ConfigNamesDto() {}

    public ConfigNamesDto(List<String> names) {
        this.names = names;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }
}
