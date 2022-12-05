package gov.nih.nci.bento.model.search.mapper;

import java.util.List;
import java.util.Map;

public interface HighLightMapper {
    Map<String, Object> getMap(Map<String, Object> source, List<String> fragments);
}