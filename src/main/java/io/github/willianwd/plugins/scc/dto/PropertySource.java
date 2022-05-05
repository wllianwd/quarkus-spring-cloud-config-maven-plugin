package io.github.willianwd.plugins.scc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertySource {
    private String name;
    private Map<String, Object> source;
}
