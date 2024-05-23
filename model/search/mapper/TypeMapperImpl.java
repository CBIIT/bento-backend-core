package gov.nih.nci.bento.model.search.mapper;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.query.QueryResult;
import org.apache.lucene.search.TotalHits;
import org.jetbrains.annotations.NotNull;
import org.opensearch.action.search.SearchResponse;

import org.opensearch.core.common.text.Text;
import org.opensearch.search.SearchHit;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.Aggregations;
import org.opensearch.search.aggregations.bucket.filter.ParsedFilter;
import org.opensearch.search.aggregations.bucket.nested.ParsedNested;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.aggregations.bucket.terms.Terms;
import org.opensearch.search.aggregations.metrics.ParsedMax;
import org.opensearch.search.aggregations.metrics.ParsedMin;
import org.opensearch.search.aggregations.metrics.Sum;
import org.opensearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TypeMapperImpl implements TypeMapperService {

    @Override
    public TypeMapper<List<Map<String, Object>>> getList(Set<String> returnTypes) {
        return (response) -> getMaps(response, returnTypes);
    }

    @Override
    public TypeMapper<Map<String, Object>> getMap(Set<String> returnTypes) {
        return (response) -> getSingleHit(response, returnTypes);
    }

    @Override
    public TypeMapper<List<String>> getStrList(String field) {
        return (response) -> createStrList(response, field);
    }

    @Override
    public TypeMapper<Map<String, Object>> getRange() {
        return (response) -> {
            Aggregations aggregate = response.getAggregations();
            Map<String, Aggregation> responseMap = aggregate.getAsMap();
            Map<String, Object> result = new HashMap<>();

            ParsedMax max = (ParsedMax) responseMap.get("max");
            ParsedMin min = (ParsedMin) responseMap.get("min");

            long total = response.getHits().getTotalHits().value;
            result.put(Const.YAML_QUERY.RESULT_TYPE.RANGE_PARAMS.LOWER_BOUND, total > 0 ? (float) min.getValue() : 0);
            result.put(Const.YAML_QUERY.RESULT_TYPE.RANGE_PARAMS.UPPER_BOUND, total > 0 ? (float) max.getValue() : 0);
            // TODO this is only for bento
            result.put("subjects", total);
            return result;
        };
    }

    @Override
    public TypeMapper<Long> getIntTotal() {
        return (response) -> {
            TotalHits hits = response.getHits().getTotalHits();
            return hits.value;
        };
    }

    @Override
    public TypeMapper<List<Map<String, Object>>> getAggregate() {
        return (response) -> {
            List<Map<String, Object>> result = new ArrayList<>();
            Aggregations aggregate = response.getAggregations();
            Terms terms = aggregate.get(Const.ES_PARAMS.TERMS_AGGS);
            List<Terms.Bucket> buckets = (List<Terms.Bucket>) terms.getBuckets();
            buckets.forEach(bucket->
                    result.add(
                            Map.of(
                                    Const.BENTO_FIELDS.GROUP,bucket.getKey(),
                                    Const.BENTO_FIELDS.SUBJECTS,bucket.getDocCount()
                            )
                    )
            );
            return result;
        };
    }

    @Override
    public TypeMapper<Integer> getAggregateTotalCnt() {
        return (response) -> {
            Aggregations aggregate = response.getAggregations();
            Terms terms = aggregate.get(Const.ES_PARAMS.TERMS_AGGS);
            long totalCount = terms.getBuckets().size() + terms.getSumOfOtherDocCounts();
            return (int) totalCount;
        };
    }

    @Override
    public TypeMapper<Integer> getNestedAggregateTotalCnt() {
        return (response) -> {
            ParsedFilter aggFilters = getParsedFilter(response);
            ParsedStringTerms aggTerms = aggFilters.getAggregations().get(Const.ES_PARAMS.TERMS_AGGS);
            List<Terms.Bucket> buckets = (List<Terms.Bucket>) aggTerms.getBuckets();
            long totalCount = buckets.size() + aggTerms.getSumOfOtherDocCounts();
            return (int) totalCount;
        };
    }

    @Override
    public TypeMapper<QueryResult> getNestedAggregate() {
        return this::getNestedAggregate;
    }

    private QueryResult getNestedAggregate(SearchResponse response) {
        List<Map<String, Object>> result = new ArrayList<>();
        ParsedFilter aggFilters = getParsedFilter(response);
        ParsedStringTerms aggTerms = aggFilters.getAggregations().get(Const.ES_PARAMS.TERMS_AGGS);
        List<Terms.Bucket> buckets = (List<Terms.Bucket>) aggTerms.getBuckets();

        AtomicLong totalCount = new AtomicLong();
        buckets.forEach((b)-> {
            totalCount.addAndGet(b.getDocCount());
            result.add(Map.of(
                    // TODO only for bento
                    Const.BENTO_FIELDS.GROUP, b.getKey(),
                    Const.BENTO_INDEX.SUBJECTS, b.getDocCount()
            ));
        });
        return QueryResult.builder()
                .searchHits(result)
                .totalHits(aggTerms.getSumOfOtherDocCounts() + totalCount.longValue())
                .build();
    }

    private ParsedFilter getParsedFilter(SearchResponse response) {
        Aggregations aggregate = response.getAggregations();
        ParsedNested aggNested = (ParsedNested) aggregate.getAsMap().get(Const.ES_PARAMS.NESTED_SEARCH);
        // Get Nested Sub aggregation
        return aggNested.getAggregations().get(Const.ES_PARAMS.NESTED_FILTER);
    }

    @Override
    public TypeMapper<Float> getSumAggregate() {
        return (response) -> {
            Sum agg = response.getAggregations().get(Const.ES_PARAMS.TERMS_AGGS);
            return (float) agg.getValue();
        };
    }

    @Override
    public TypeMapper<List<Map<String, Object>>> getNestedAggregateList() {
        return (response) -> {
            QueryResult<List<Map<String, Object>>> queryResult = getNestedAggregate(response);
            List<Map<String, Object>> result =queryResult.getSearchHits();
            return result;
        };
    }

    // Required Only One Argument
    @NotNull
    private List<String> createStrList(SearchResponse response, String field) {
        List<String> result = new ArrayList<>();
        SearchHit[] hits = response.getHits().getHits();
        Arrays.asList(hits).forEach(hit-> {
            Map<String, Object> source = hit.getSourceAsMap();
            if (!source.containsKey(field)) throw new IllegalArgumentException();
            result.add((String) source.get(field));
        });
        return result;
    }

    @NotNull
    private Map<String, Object> getSingleHit(SearchResponse response, Set<String> returnTypes) {
        return getFirstHits(response, returnTypes);
    }

    @NotNull
    private List<Map<String, Object>> getMaps(SearchResponse response, Set<String> returnTypes) {
        return getListHits(response, returnTypes);
    }

    @Override
    public TypeMapper<QueryResult> getQueryResult(Set<String> returnTypes) {
        return (response) -> getDefaultMaps(response, returnTypes);
    }

    @NotNull
    private QueryResult getDefaultMaps(SearchResponse response, Set<String> returnTypes) {
        return QueryResult.builder()
                .searchHits(getListHits(response, returnTypes))
                .totalHits(response.getHits().getTotalHits().value)
                .build();
    }

    // TODO isolate this because it is only suitable for bento project
    @SuppressWarnings("unchecked")
    public TypeMapper<List<Map<String, Object>>> getArmProgram() {
        return (response) -> {
            Aggregations aggregate = response.getAggregations();
            Terms terms = aggregate.get(Const.ES_PARAMS.TERMS_AGGS);
            List<Terms.Bucket> buckets = (List<Terms.Bucket>) terms.getBuckets();
            List<Map<String, Object>> result = new ArrayList<>();
            buckets.forEach(bucket-> {
                        Aggregations subAggregate = bucket.getAggregations();
                        Terms subTerms = subAggregate.get(Const.ES_PARAMS.TERMS_AGGS);
                        List<Terms.Bucket> subBuckets = (List<Terms.Bucket>) subTerms.getBuckets();
                        List<Map<String, Object>> studies = new ArrayList<>();
                        subBuckets.forEach((subBucket)->
                                studies.add(Map.of(
                                        // TODO mapping needs to be isolated
                                        Const.BENTO_FIELDS.ARM,subBucket.getKey(),
                                        Const.BENTO_FIELDS.CASE_SIZE,subBucket.getDocCount(),
                                        Const.BENTO_FIELDS.SIZE,subBucket.getDocCount()
                                ))
                        );
                        result.add(
                                Map.of(
                                        // TODO mapping needs to be isolated
                                        Const.BENTO_FIELDS.PROGRAM, bucket.getKey(),
                                        Const.BENTO_FIELDS.CASE_SIZE,bucket.getDocCount(),
                                        Const.BENTO_FIELDS.CHILDREN, studies
                                )
                        );
                    }
            );
            return result;
        };
    }

    private Map<String, Object> getFirstHits(SearchResponse response, Set<String> returnTypes) {
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            Map<String, Object> source = hit.getSourceAsMap();
            Map<String, Object> returnMap = parseReturnMap(returnTypes, source);
            if (!returnMap.isEmpty()) {
                return returnMap;
            }
        }
        return Collections.emptyMap();

    }

    private List<Map<String, Object>> getListHits(SearchResponse response, Set<String> returnTypes) {
        List<Map<String, Object>> result = new ArrayList<>();
        SearchHit[] hits = response.getHits().getHits();
        Arrays.asList(hits).forEach(hit-> {
            Map<String, Object> source = hit.getSourceAsMap();
            // codeless mapper to match with fields in graph.ql file
            Map<String, Object> returnMap = parseReturnMap(returnTypes,source);
            if (returnMap.size() > 0) result.add(returnMap);
        });
        return result;
    }

    private Map<String, Object> parseReturnMap(Set<String> returnTypes, Map<String, Object> source) {
        return returnTypes.stream()
                .filter(source::containsKey)
                .collect(HashMap::new, (k,v)->k.put(v, source.get(v)), HashMap::putAll);
    }

    @Override
    public TypeMapper<List<Map<String, Object>>> getHighLightFragments(String field, HighLightMapper mapper) {
        return (response) -> {
            List<Map<String, Object>> result = new ArrayList<>();
            SearchHit[] hits = response.getHits().getHits();
            Arrays.asList(hits).forEach(hit-> {
                Map<String, HighlightField> highlightFieldMap = hit.getHighlightFields();
                Map<String, Object> source = hit.getSourceAsMap();
                HighlightField highlightField = highlightFieldMap.get(field);
                Text[] texts = highlightField.getFragments();
                List<String> highlightedList = new ArrayList<>();
                Arrays.stream(texts).forEach(text->
                        highlightedList.add(text.toString())
                );
                result.add(
                        mapper.getMap(source, highlightedList)
                );
            });
            return result;
        };
    }


    @Override
    public TypeMapper<List<Map<String, Object>>> getMapWithHighlightedFields(Set<String> returnTypes) {
        return (response) -> {
            List<Map<String, Object>> result = new ArrayList<>();
            SearchHit[] hits = response.getHits().getHits();
            Arrays.asList(hits).forEach(hit-> {
                Map<String, HighlightField> highlightFieldMap = hit.getHighlightFields();
                Map<String, Object> source = hit.getSourceAsMap();

                Map<String, Object> returnMap = parseReturnMap(returnTypes, source);
                highlightFieldMap.forEach((k,highlightField)->{
                    Text[] texts = highlightField.getFragments();
                    Optional<String> text = Arrays.stream(texts).findFirst().map(Text::toString).stream().findFirst();
                    // Set Highlight Field & Get First Found Match Keyword
                    text.ifPresent(v->returnMap.put(Const.BENTO_FIELDS.HIGHLIGHT, v));
                });
                if (returnMap.size() > 0) result.add(returnMap);
            });
            return result;
        };
    }

}
