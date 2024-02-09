package gov.nih.nci.bento.model.search.yaml.type;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.MultipleRequests;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.model.search.yaml.GroupTypeQuery;
import gov.nih.nci.bento.model.search.yaml.IFilterType;
import gov.nih.nci.bento.model.search.yaml.ITypeQuery;
import gov.nih.nci.bento.service.ESService;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class GroupTypeYaml extends AbstractYamlType {
    private static final Logger logger = LogManager.getLogger(GroupTypeYaml.class);

    private final ESService esService;
    private final Const.ES_ACCESS_TYPE accessType;

    private void setGlobalRangeFields(GroupTypeQuery groupTypeQuery) {
        groupTypeQuery.getQueries().stream().forEach(query->{
            // Manual global range fields
            Set<String> globalRangeFields = query.getGlobalRangeFields();
            if (globalRangeFields != null && globalRangeFields.size() > 0) {
                query.getReturnFields().stream().forEach(f->{
                    f.getFilter().setRangeFilterFields(globalRangeFields);
                });
            }
        });
    }

    private List<GroupTypeQuery.Group> readYamlFile(ClassPathResource resource) throws IOException {
        logger.info(String.format("%s Yaml group file query loading...", accessType.toString()));
        Yaml groupYaml = new Yaml();
        GroupTypeQuery groupTypeQuery = groupYaml.load(resource.getInputStream());
        setGlobalRangeFields(groupTypeQuery);
        return groupTypeQuery.getQueries();
    }

    private <T> Map<String, T> multipleSend(GroupTypeQuery.Group group, QueryParam param, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
        logger.info(String.format("%s group Yaml search API requested: %s", accessType.toString(), group.getName()));
        List<MultipleRequests> requests = new ArrayList<>();
        group.getReturnFields().forEach(q->
                requests.add(MultipleRequests.builder()
                .name(q.getName())
                .request(new SearchRequest()
                        .indices(q.getIndex())
                        .source(iFilterType.getQueryFilter(param, q)))
                .typeMapper(iTypeQuery.getReturnType(param, q)).build()));
        return esService.elasticMultiSend(requests);
    }

    @Override
    public void createSearchQuery(Map<String, DataFetcher> resultMap, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
        String fileName = Const.YAML_QUERY.SUB_FOLDER + getYamlFileName(accessType, Const.YAML_QUERY.FILE_NAMES_BENTO.GROUP);
        ClassPathResource resource = new ClassPathResource(fileName);
        if (!resource.exists()) return;
        readYamlFile(resource).forEach(group->{
            String queryName = group.getName();
            resultMap.put(queryName, env -> multipleSend(group, createQueryParam(env), iTypeQuery, iFilterType));
        });
    }
}
