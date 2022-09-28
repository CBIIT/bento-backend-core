package gov.nih.nci.bento.model.search.query;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.filter.FilterParam;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryFactory {
    private FilterParam filterParam;
    // Parameters Exceptions
    private final Set<String> sortParams = Set.of(Const.ES_PARAMS.ORDER_BY, Const.ES_PARAMS.SORT_DIRECTION, Const.ES_PARAMS.OFFSET, Const.ES_PARAMS.PAGE_SIZE);
    public QueryFactory(FilterParam param) {
        this.filterParam = param;
    }

    public QueryBuilder getQuery() {
        BoolQueryBuilder boolBuilder = new BoolQueryBuilder();
        Map<String, Object> args = new HashMap<>(filterParam.getArgs());
        // remove sort params
        removeSortParams(args);
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            List<String> list = (List<String>) args.get(key);
            if (list.size() > 0 && filterParam.isCaseInsensitive()) {
                boolBuilder.filter(getCaseInsensitiveQuery(list, key));
                continue;
            }
            if (list.size() > 0) boolBuilder.filter(QueryBuilders.termsQuery(key, (List<String>) args.get(key)));
        }
        return boolBuilder.filter().size() > 0 ? boolBuilder : QueryBuilders.matchAllQuery();
    }

    private QueryBuilder getCaseInsensitiveQuery(List<String> list, String key) {
        BoolQueryBuilder bool = new BoolQueryBuilder();
        list.forEach(value->
                bool.should(
                        QueryBuilders.wildcardQuery(key, value).caseInsensitive(true)
                )
        );
        return bool;
    }

    private void removeSortParams(Map<String, Object> map) {
        sortParams.forEach(key -> {
            if (map.containsKey(key)) map.remove(key);
        });
    }

}
