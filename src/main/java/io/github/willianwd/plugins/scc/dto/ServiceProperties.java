package io.github.willianwd.plugins.scc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceProperties {
    private String name;
    private String label;
    private String version;
    private List<String> profiles;
    private List<PropertySource> propertySources;
}
