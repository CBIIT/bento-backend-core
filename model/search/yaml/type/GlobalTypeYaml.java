//package gov.nih.nci.bento.model.search.yaml.type;//package gov.nih.nci.bento.model.search.yaml.type;
//
//import gov.nih.nci.bento.constants.Const;
//import gov.nih.nci.bento.model.search.MultipleRequests;
//import gov.nih.nci.bento.model.search.query.QueryParam;
//import gov.nih.nci.bento.model.search.query.QueryResult;
//import gov.nih.nci.bento.model.search.yaml.IFilterType;
//import gov.nih.nci.bento.model.search.yaml.ITypeQuery;
//import gov.nih.nci.bento.model.search.yaml.SingleTypeQuery;
//import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
//import gov.nih.nci.bento.service.ESService;
//import graphql.schema.DataFetcher;
//import lombok.RequiredArgsConstructor;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.opensearch.action.search.SearchRequest;
//import org.opensearch.search.builder.SearchSourceBuilder;
//import org.springframework.core.io.ClassPathResource;
//import org.yaml.snakeyaml.Yaml;
//import org.yaml.snakeyaml.constructor.Constructor;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//@RequiredArgsConstructor
//public class GlobalTypeYaml extends AbstractYamlType {
//
//    private final ESService esService;
//    private static final Logger logger = LogManager.getLogger(GlobalTypeYaml.class);
//
//    private List<YamlQuery> readYamlFile(ClassPathResource resource) throws IOException {
//        logger.info("Yaml global file query loading...");
//        Yaml yaml = new Yaml(new Constructor(SingleTypeQuery.class));
//        return yaml.load(resource.getInputStream());
//    }
//
//    private Object globalMultipleSend(QueryParam param, YamlQuery query, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
//        logger.info("ES Search Global API Requested: " + query.getName());
//        // Set Bool Filter
//        SearchSourceBuilder builder = iFilterType.getQueryFilter(param, query);
//        MultipleRequests request = MultipleRequests.builder()
//                .name(query.getName())
//                .request(new SearchRequest()
//                        .indices(query.getIndex())
//                        .source(builder))
//                .typeMapper(iTypeQuery.getReturnType(param, query)).build();
//        Map<String, Object> multiResult = esService.elasticMultiSend(List.of(request));
//        return getGlobalTypeResult(multiResult, param, query);
//    }
//
//    private <T> Object getGlobalTypeResult(Map<String, T> multiResult, QueryParam param, YamlQuery query) {
//        Map<String, Object> result = new HashMap<>();
//        QueryResult<List<Object>> queryResult = (QueryResult<List<Object>>) multiResult.get(query.getName());
//        List<Object> searchHits = queryResult.getSearchHits();
//        result.put("result", checkEmptySearch(param, searchHits.size()));
//        result.put("count", checkEmptySearch(param, queryResult.getTotalHits()));
//        return result;
//    }
//
//    // TODO TO BE DELETED
//    private int checkEmptySearch(QueryParam param, int result) {
//        return param.getSearchText().equals("") ? 0 :result;
//    }
//
//
//    @Override
//    public void createSearchQuery(Map<String, DataFetcher> resultMap, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
//        ClassPathResource resource = new ClassPathResource(Const.YAML_QUERY.FILE_NAMES_BENTO.GLOBAL);
//        if (!resource.exists()) return;
//        readYamlFile(resource).forEach(query->
//                resultMap.put(query.getName(), env -> globalMultipleSend(createQueryParam(env), query, iTypeQuery, iFilterType))
//        );
//    }
//}
