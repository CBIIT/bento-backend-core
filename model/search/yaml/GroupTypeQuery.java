package gov.nih.nci.bento.model.search.yaml;

import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class GroupTypeQuery {

    private List<Group> queries;

    @Data
    public static class Group {
        private String name;
        private List<YamlQuery> returnFields;
        private Set<String> globalRangeFields;
    }
}