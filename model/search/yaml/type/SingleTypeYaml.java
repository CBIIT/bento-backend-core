package gov.nih.nci.bento.model.search.yaml.type;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.MultipleRequests;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.model.search.query.QueryResult;
import gov.nih.nci.bento.model.search.yaml.IFilterType;
import gov.nih.nci.bento.model.search.yaml.ITypeQuery;
import gov.nih.nci.bento.model.search.yaml.SingleTypeQuery;
import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
import gov.nih.nci.bento.service.ESService;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.List;
import java.util.Map;
@RequiredArgsConstructor
public class SingleTypeYaml extends AbstractYamlType {

    private final ESService esService;
    private final Const.ES_ACCESS_TYPE accessType;
    private static final Logger logger = LogManager.getLogger(SingleTypeYaml.class);

    private List<YamlQuery> readYamlFile(ClassPathResource resource) throws IOException {
        logger.info(String.format("%s Yaml single file query loading...", accessType.toString()));
        Yaml yaml = new Yaml();
        SingleTypeQuery singleTypeQuery = yaml.loadAs(resource.getInputStream(), SingleTypeQuery.class);
        return singleTypeQuery.getQueries();
    }

    private Object multipleSend(YamlQuery query, QueryParam param, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
        logger.info(String.format("%s single Yaml search API requested: %s", accessType.toString(), query.getName()));
        Map<String, QueryResult> multipleSendResult = esService.elasticMultiSend(
                List.of(MultipleRequests.builder()
                        .name(query.getName())
                        .request(new SearchRequest()
                        .indices(query.getIndex())
                        .source(iFilterType.getQueryFilter(param, query)))
                        .typeMapper(iTypeQuery.getReturnType(param, query)).build()));
        return multipleSendResult.get(query.getName());
    }

    @Override
    public void createSearchQuery(Map<String, DataFetcher> resultMap, ITypeQuery iTypeQuery, IFilterType iFilterType) throws IOException {
        String fileName = Const.YAML_QUERY.SUB_FOLDER + getYamlFileName(accessType, Const.YAML_QUERY.FILE_NAMES_BENTO.SINGLE);
        ClassPathResource resource = new ClassPathResource(fileName);
        if (!resource.exists()) return;
        readYamlFile(resource).forEach(query->
                resultMap.put(query.getName(), env -> multipleSend(query, createQueryParam(env), iTypeQuery, iFilterType))
        );
    }
}
