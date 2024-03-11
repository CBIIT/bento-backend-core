package gov.nih.nci.bento.model.search.yaml;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.filter.*;
import gov.nih.nci.bento.model.search.mapper.TypeMapperImpl;
import gov.nih.nci.bento.model.search.mapper.TypeMapperService;
import gov.nih.nci.bento.model.search.yaml.filter.YamlFilter;
import gov.nih.nci.bento.model.search.yaml.type.AbstractYamlType;
import gov.nih.nci.bento.model.search.yaml.type.GlobalTypeYaml;
import gov.nih.nci.bento.model.search.yaml.type.GroupTypeYaml;
import gov.nih.nci.bento.model.search.yaml.type.SingleTypeYaml;
import gov.nih.nci.bento.service.ESService;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class YamlQueryFactory {

    private final ESService esService;
    private final TypeMapperService typeMapper = new TypeMapperImpl();
    private static final Logger logger = LogManager.getLogger(YamlQueryFactory.class);

    public Map<String, DataFetcher> createYamlQueries(Const.ES_ACCESS_TYPE accessType) throws IOException {
        logger.info("Loading Yaml File Queries");
        // Set Single Request API
        List<AbstractYamlType> yamlFileList = List.of(new SingleTypeYaml(esService, accessType), new GroupTypeYaml(esService, accessType),new GlobalTypeYaml(esService, accessType));
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
            case Const.YAML_QUERY.RESULT_TYPE.MAP:
                return typeMapper.getMap(param.getReturnTypes());
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
            case Const.YAML_QUERY.RESULT_TYPE.GLOBAL_MULTIPLE_MODEL:
                return typeMapper.getMapWithHighlightedFields(param.getGlobalSearchResultTypes());
            case Const.YAML_QUERY.RESULT_TYPE.GLOBAL:
                return typeMapper.getList(param.getGlobalSearchResultTypes());
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
                            .extraFilters(filterType.getExtraFilters())
                            .rangeFilterFields(filterType.getRangeFilterFields())
                            .ignoreIfEmpty(filterType.getIgnoreIfEmpty()).build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.PAGINATION:
                    return new PaginationFilter(FilterParam.builder()
                            .args(param.getArgs())
                            .defaultSortField(filterType.getDefaultSortField())
                            .ignoreIfEmpty(filterType.getIgnoreIfEmpty())
                            .rangeFilterFields(filterType.getRangeFilterFields())
                            .alternativeSortField(filterType.getAlternativeSortField())
                            .returnFields(param.getReturnTypes())
                            .extraFilters(filterType.getExtraFilters())
                            .build()).getSourceFilter();
                case Const.YAML_QUERY.FILTER.AGGREGATION:
                    return new AggregationFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .isIgnoreSelectedField(filterType.isIgnoreSelectedField())
                                    .selectedField(filterType.getSelectedField())
                                    .extraFilters(filterType.getExtraFilters())
                                    .rangeFilterFields(filterType.getRangeFilterFields())
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.RANGE:
                    return new RangeFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .selectedField(filterType.getSelectedField())
                                    .extraFilters(filterType.getExtraFilters())
                                    .rangeFilterFields(filterType.getRangeFilterFields())
                                    .isRangeFilter(true)
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.SUB_AGGREGATION:
                    return new SubAggregationFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .extraFilters(filterType.getExtraFilters())
                                    .selectedField(filterType.getSelectedField())
                                    .subAggSelectedField(filterType.getSubAggSelectedField())
                                    .rangeFilterFields(filterType.getRangeFilterFields())
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.NESTED:
                    return new NestedFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .isIgnoreSelectedField(filterType.isIgnoreSelectedField())
                                    .extraFilters(filterType.getExtraFilters())
                                    .selectedField(filterType.getSelectedField())
                                    .nestedPath(filterType.getNestedPath())
                                    .nestedParameters(filterType.getNestedParameters())
                                    .rangeFilterFields(filterType.getRangeFilterFields())
                                    .build())
                            .getSourceFilter();
                case Const.YAML_QUERY.FILTER.GLOBAL:
                    return new GlobalQueryFilter(FilterParam.builder()
                            .args(param.getArgs())
                            .isIgnoreSelectedField(filterType.isIgnoreSelectedField())
                            .selectedField(filterType.getSelectedField())
                            .nestedPath(filterType.getNestedPath())
                            .extraFilters(filterType.getExtraFilters())
                            .nestedParameters(filterType.getNestedParameters())
                            .build(), query).getSourceFilter();
                case Const.YAML_QUERY.FILTER.SUM:
                    return new SumFilter(
                            FilterParam.builder()
                                    .args(param.getArgs())
                                    .extraFilters(filterType.getExtraFilters())
                                    .selectedField(filterType.getSelectedField())
                                    .rangeFilterFields(filterType.getRangeFilterFields())
                                    .build())
                            .getSourceFilter();
                default:
                    throw new IllegalArgumentException(filterType + " is not correctly declared as a filter type in yaml file. Please, correct it and try again.");
            }
        };
    }
}
