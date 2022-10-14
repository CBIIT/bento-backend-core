package gov.nih.nci.bento.model.search.mapper;

import org.opensearch.common.text.Text;

import java.util.Map;

public interface HighLightMapper {
    Map<String, Object> getMap(Map<String, Object> source, Text fragment);
}