package org.config.dto;

import java.util.List;

public class ConfigNamesDto {
    private List<ConfigNameItemDto> names;

    public ConfigNamesDto() {}

    public ConfigNamesDto(List<ConfigNameItemDto> names) {
        this.names = names;
    }

    public List<ConfigNameItemDto> getNames() {
        return names;
    }

    public void setNames(List<ConfigNameItemDto> names) {
        this.names = names;
    }
}
