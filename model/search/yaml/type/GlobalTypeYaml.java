package gov.nih.nci.bento.model.search.yaml.type;


import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.MultipleRequests;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.model.search.yaml.GroupTypeQuery;
import gov.nih.nci.bento.model.search.yaml.IFilterType;
import gov.nih.nci.bento.model.search.yaml.ITypeQuery;
import gov.nih.nci.bento.service.ESService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@RequiredArgsConstructor
public class GlobalTypeYaml extends AbstractYamlType {

    private static final Logger logger = LogManager.getLogger(GlobalTypeYaml.class);
    private final ESService esService;

    private List<GroupTypeQuery.Group> readYamlFile(ClassPathResource resource) throws IOException {
        logger.info("Yaml global file query loading...");
        Yaml groupYaml = new Yaml(new Constructor(GroupTypeQuery.class));
        GroupTypeQuery groupTypeQuery = groupYaml.load(resource.getInputStream());
        return groupTypeQuery.getQueries();
    }

    private <T> Object multipleSend(GroupTypeQuery.Group group, QueryParam param, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
        logger.info("Global Search API Requested: " + group.getName());
        List<MultipleRequests> requests = new ArrayList<>();
        group.getReturnFields().forEach(q->
                requests.add(MultipleRequests.builder()
                        .name(q.getName())
                        .request(new SearchRequest()
                                .indices(q.getIndex())
                                .source(iFilterType.getQueryFilter(param, q)))
                        .typeMapper(iTypeQuery.getReturnType(param, q)).build()));

        Map<String, Object> result = esService.elasticMultiSend(requests);
        checkEmptySearch(result, param);
        return result;
    }

    @Override
    public void createSearchQuery(Map<String, DataFetcher> resultMap, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
        ClassPathResource resource = new ClassPathResource(Const.YAML_QUERY.FILE_NAMES_BENTO.GLOBAL);
        if (!resource.exists()) return;
        readYamlFile(resource).forEach(group->{
            String queryName = group.getName();
            resultMap.put(queryName, (DataFetchingEnvironment env) -> multipleSend(group, createQueryParam(env), iTypeQuery, iFilterType));
        });
    }

    // TODO TO BE DELETED; work with FE to adjust GraphQL Scheme
    private void checkEmptySearch(Map<String, Object> result, QueryParam param) {
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (entry.getKey().contains("count")) {
                Object obj = param.getSearchText().equals("") ? 0 : entry.getValue();
                result.put(entry.getKey(), obj);
            }
        }
    }
}