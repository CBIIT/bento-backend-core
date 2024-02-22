package gov.nih.nci.bento.model.search.mapper;

import gov.nih.nci.bento.model.search.query.QueryResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TypeMapperService {

    TypeMapper<QueryResult> getQueryResult(Set<String> returnTypes);
    TypeMapper<List<Map<String, Object>>> getList(Set<String> returnTypes);
    TypeMapper<Map<String, Object>> getMap(Set<String> returnTypes);
    TypeMapper<List<String>> getStrList(String field);
    TypeMapper<Map<String, Object>> getRange();
    TypeMapper<Long> getIntTotal();
    TypeMapper<List<Map<String, Object>>> getAggregate();
    TypeMapper<Integer> getAggregateTotalCnt();
    TypeMapper<Integer> getNestedAggregateTotalCnt();
    TypeMapper<QueryResult> getNestedAggregate();
    TypeMapper<Float> getSumAggregate();
    TypeMapper<List<Map<String, Object>>> getNestedAggregateList();
    TypeMapper<List<Map<String, Object>>> getArmProgram();
    TypeMapper<List<Map<String, Object>>> getHighLightFragments(String field, HighLightMapper mapper);
    TypeMapper<List<Map<String, Object>>> getMapWithHighlightedFields(Set<String> returnTypes);

}