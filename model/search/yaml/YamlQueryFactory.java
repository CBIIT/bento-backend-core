package gov.nih.nci.bento.model.search.yaml;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.filter.*;
import gov.nih.nci.bento.model.search.mapper.TypeMapperImpl;
import gov.nih.nci.bento.model.search.mapper.TypeMapperService;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.model.search.yaml.filter.YamlFilter;
import gov.nih.nci.bento.model.search.yaml.filter.YamlGlobalFilterType;
import gov.nih.nci.bento.model.search.yaml.filter.YamlHighlight;
import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
import gov.nih.nci.bento.model.search.yaml.type.AbstractYamlType;
import gov.nih.nci.bento.model.search.yaml.type.GroupTypeYaml;
import gov.nih.nci.bento.model.search.yaml.type.SingleTypeYaml;
import gov.nih.nci.bento.service.ESService;
import gov.nih.nci.bento.utility.StrUtil;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.index.query.*;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class YamlQueryFactory {

    private final ESService esService;
    private final TypeMapperService typeMapper = new TypeMapperImpl();
    private static final Logger logger = LogManager.getLogger(YamlQueryFactory.class);

    public Map<String, DataFetcher> createYamlQueries() throws IOException {
        logger.info("Loading Yaml File Queries");
        // Set Single Request API
        List<AbstractYamlType> yamlFileList = List.of(new SingleTypeYaml(esService), new GroupTypeYaml(esService));
        Map<String, DataFetcher> result = new HashMap<>();
        for (AbstractYamlType yamlFile : yamlFileList) {
            yamlFile.createSearchQuery(result, getReturnType(), getFilterType());
        }
        return result;
    }

    private ITypeQuery getReturnType() {
        return (param, query) -> {
            String method = query.getResult().getMethod();
            switch (query.getResult().getType()) {
            case Const.YAML_QUERY.RESULT_TYPE.OBJECT_ARRAY:
                return typeMapper.getList(param.getReturnTypes());
            case Const.YAML_QUERY.RESULT_TYPE.STRING_ARRAY:
                return typeMapper.getStrList(query.getFilter().getSelectedField());
            case Const.YAML_QUERY.RESULT_TYPE.GROUP_COUNT:
                return typeMapper.getAggregate();
            case Const.YAML_QUERY.RESULT_TYPE.INT:
                if (method.equals(Const.YAML_QUERY.RESULT_TYPE.INT_METHOD.COUNT_BUCKET_KEY)) {
                    return typeMapper.getAggregateTotalCnt();
                } else if (method.equals(Const.YAML_QUERY.RESULT_TYPE.INT_METHOD.NESTED_COUNT)) {
                    return typeMapper.getNestedAggregateTotalCnt();
                }
                throw new IllegalArgumentException("Illegal int return types");
            case Const.YAML_QUERY.RESULT_TYPE.FLOAT:
                if (method.equals(Const.YAML_QUERY.RESULT_TYPE.FLOAT_METHOD.SUM_AGG)) return typeMapper.getSumAggregate();
                throw new IllegalArgumentException("This is an illegal return type value for query configuration file");
            case Const.YAML_QUERY.RESULT_TYPE.RANGE:
                return typeMapper.getRange();
            case Const.YAML_QUERY.RESULT_TYPE.ARM_PROGRAM:
                return typeMapper.getArmProgram();
            case Const.YAML_QUERY.RESULT_TYPE.INT_TOTAL_COUNT:
                return typeMapper.getIntTotal();
            case Const.YAML_QUERY.RESULT_TYPE.NESTED:
                return typeMapper.getNestedAggregate();
            case Const.YAML_QUERY.RESULT_TYPE.NESTED_LIST:
                return typeMapper.getNestedAggregateList();
            case Const.YAML_QUERY.RESULT_TYPE.GLOBAL_ABOUT:
                return typeMapper.getHighLightFragments(query.getFilter().getSelectedField(),
                        (source, text) -> Map.of(
                                Const.BENTO_FIELDS.TYPE, Const.BENTO_FIELDS.ABOUT,
                                Const.BENTO_FIELDS.PAGE, source.get(Const.BENTO_FIELDS.PAGE),
                                Const.BENTO_FIELDS.TITLE,source.get(Const.BENTO_FIELDS.TITLE),
                                Const.BENTO_FIELDS.TEXT, text));
            default:
                throw new IllegalArgumentException(query.getResult().getType() + " is not correctly declared as a return type in yaml file. Please, correct it and try again.");
            }
        };
    }

    private IFilterType getFilterType() {
        return (param, query) -> {
            // Set Arguments
            YamlFilter filterType = query.getFilter();
            switch (filterType.getType()) {
                case Const.YAML_QUERY.FILTER.DEFAULT:
                    return new DefaultFilter(FilterParam.builder()
                            .args(param.getArgs())
                            .caseInsensitive(filterType.isCaseInsensitive())
                            .ignoreIfEmpty(filterType.getIgnoreIfEmpty()).build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.PAGINATION:
                    return new PaginationFilter(FilterParam.builder()
                            .args(param.getArgs())
                            .defaultSortField(filterType.getDefaultSortField())
                            .ignoreIfEmpty(filterType.getIgnoreIfEmpty())
                            .rangeFilterFields(filterType.getRangeFilterFields())
                            .alternativeSortField(filterType.getAlternativeSortField())
                            .build()).getSourceFilter();
                case Const.YAML_QUERY.FILTER.AGGREGATION:
                    return new AggregationFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .isExcludeFilter(filterType.isIgnoreSelectedField())
                                    .selectedField(filterType.getSelectedField())
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.RANGE:
                    return new RangeFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .selectedField(filterType.getSelectedField())
                                    .isRangeFilter(true)
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.SUB_AGGREGATION:
                    return new SubAggregationFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .selectedField(filterType.getSelectedField())
                                    .subAggSelectedField(filterType.getSubAggSelectedField())
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.NESTED:
                    return new NestedFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .isExcludeFilter(filterType.isIgnoreSelectedField())
                                    .selectedField(filterType.getSelectedField())
                                    .nestedPath(filterType.getNestedPath())
                                    .nestedParameters(filterType.getNestedParameters())
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.GLOBAL:
                    return createGlobalQuery(param,query);
                case Const.YAML_QUERY.FILTER.SUM:
                    return new SumFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .selectedField(filterType.getSelectedField())
                                    .build())
                            .getSourceFilter();
                default:
                    throw new IllegalArgumentException(filterType + " is not correctly declared as a filter type in yaml file. Please, correct it and try again.");
            }
        };
    }

    // TODO
    private SearchSourceBuilder createGlobalQuery(QueryParam param, YamlQuery query) {
        FilterParam.Pagination page = param.getPagination();
        // Store Conditional Query
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .size(page.getPageSize())
                .from(page.getOffSet())
                .query(
                        addConditionalQuery(
                                createGlobalQuerySets(param, query),
                                createGlobalConditionalQueries(param, query))
                );
        // Set Sort
        if (query.getFilter().getDefaultSortField() !=null) builder.sort(query.getFilter().getDefaultSortField(), SortOrder.DESC);
        // Set Highlight Query
        setGlobalHighlightQuery(query, builder);
        return builder;
    }



    private List<QueryBuilder> createGlobalConditionalQueries(QueryParam param, YamlQuery query) {
        if (query.getFilter().getTypedSearch() == null) return new ArrayList<>();
        List<QueryBuilder> conditionalList = new ArrayList<>();
        List<YamlGlobalFilterType.GlobalQuerySet> typeQuerySets = query.getFilter().getTypedSearch() ;
        AtomicReference<String> filterString = new AtomicReference<>("");
        typeQuerySets.forEach(option-> {
            if (option.getOption().equals(Const.YAML_QUERY.QUERY_TERMS.BOOLEAN)) {
                filterString.set(StrUtil.getBoolText(param.getSearchText()));
            } else if (option.getOption().equals(Const.YAML_QUERY.QUERY_TERMS.INTEGER)) {
                filterString.set(StrUtil.getIntText(param.getSearchText()));
            } else {
                throw new IllegalArgumentException();
            }

            if (option.getType().equals(Const.YAML_QUERY.QUERY_TERMS.MATCH)) {
                conditionalList.add(QueryBuilders.matchQuery(option.getField(), filterString));
            } else if (option.getType().equals(Const.YAML_QUERY.QUERY_TERMS.TERM)) {
                conditionalList.add(QueryBuilders.termQuery(option.getField(), filterString.get()));
            } else {
                throw new IllegalArgumentException();
            }
        });
        return conditionalList;
    }

    // Add Conditional Query
    private BoolQueryBuilder addConditionalQuery(BoolQueryBuilder builder, List<QueryBuilder> builders) {
        builders.forEach(q->{
            if (q.getName().equals(Const.YAML_QUERY.QUERY_TERMS.MATCH)) {
                MatchQueryBuilder matchQuery = getQuery(q);
                if (!matchQuery.value().equals("")) builder.should(q);
            } else if (q.getName().equals(Const.YAML_QUERY.QUERY_TERMS.TERM)) {
                TermQueryBuilder termQuery = getQuery(q);
                if (!termQuery.value().equals("")) builder.should(q);
            }
        });
        return builder;
    }

    @SuppressWarnings("unchecked")
    private <T> T getQuery(QueryBuilder q) {
        String queryType = q.getName();
        return (T) q.queryName(queryType);
    }

    private void setGlobalHighlightQuery(YamlQuery query, SearchSourceBuilder builder) {
        if (query.getHighlight() != null) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            YamlHighlight yamlHighlight = query.getHighlight();
            // Set Multiple Highlight Fields
            yamlHighlight.getFields().forEach(highlightBuilder::field);
            highlightBuilder.preTags(yamlHighlight.getPreTag() == null ? "" : yamlHighlight.getPreTag());
            highlightBuilder.postTags(yamlHighlight.getPostTag() == null ? "" : yamlHighlight.getPostTag());
            if (highlightBuilder.fragmentSize() != null) highlightBuilder.fragmentSize(yamlHighlight.getFragmentSize());
            builder.highlighter(highlightBuilder);
        }
    }


    private BoolQueryBuilder createGlobalQuerySets(QueryParam param, YamlQuery query) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        List<YamlGlobalFilterType.GlobalQuerySet> globalQuerySets = query.getFilter().getSearches();
        // Add Should Query
        globalQuerySets.forEach(globalQuery -> {
            switch (globalQuery.getType()) {
                case Const.YAML_QUERY.QUERY_TERMS.TERM:
                    boolQueryBuilder.should(QueryBuilders.termQuery(globalQuery.getField(), param.getSearchText()));
                    break;
                case Const.YAML_QUERY.QUERY_TERMS.WILD_CARD:
                    boolQueryBuilder.should(QueryBuilders.wildcardQuery(globalQuery.getField(), "*" + param.getSearchText()+ "*").caseInsensitive(true));
                    break;
                case Const.YAML_QUERY.QUERY_TERMS.MATCH:
                    boolQueryBuilder.should(QueryBuilders.matchQuery(globalQuery.getField(), param.getSearchText()));
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        });
        return boolQueryBuilder;
    }
}
