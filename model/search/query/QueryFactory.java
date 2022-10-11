package gov.nih.nci.bento.model.search.query;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.filter.FilterParam;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;

import java.util.*;

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
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) args.get(key);
            if (list.size() > 0) {
                // add range filter
                if (filterParam.getRangeFilterFields().contains(key)) {
                    boolBuilder.filter(getRangeType(key, list));
                    continue;
                }
                // add case-insensitive
                if (filterParam.isCaseInsensitive()) {
                    boolBuilder.filter(getCaseInsensitiveQuery(list, key));
                    continue;
                }
                boolBuilder.filter(QueryBuilders.termsQuery(key, (List<String>) args.get(key)));
            }
        }
        return boolBuilder.filter().size() > 0 ? boolBuilder : QueryBuilders.matchAllQuery();
    }

    private QueryBuilder getRangeType(String field, List<String> strList) {
        RangeQueryBuilder rangeBuilder = QueryBuilders.rangeQuery(field)
                .gte(strList.get(0));
        if (strList.size() > 1) rangeBuilder.lte(strList.get(1));
        return rangeBuilder;
    }

    private void removeSortParams(Map<String, Object> map) {
        sortParams.forEach(key -> {
            if (map.containsKey(key)) map.remove(key);
        });
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
}