package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.model.search.query.TableParam;
import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
import gov.nih.nci.bento.utility.ElasticUtil;
import lombok.Builder;
import lombok.Getter;
import org.opensearch.search.sort.SortOrder;

import java.util.Map;
import java.util.Set;

@Getter
public class FilterParam {

    private final Map<String, Object> args;
    private final String selectedField;
    private final Set<String> ignoreIfEmpty;
    private final boolean caseInsensitive;
    private final String defaultSortField;
    private final int size;
    private final int offSet;
    private final String customOrderBy;
    private final SortOrder sortDirection;


    @Builder
    public FilterParam(Map<String, Object> args, String selectedField, Set<String> ignoreIfEmpty,
                       boolean caseInsensitive, String defaultSortField) {
        this.args = args;
        this.selectedField = selectedField;
        this.defaultSortField = defaultSortField;
        this.ignoreIfEmpty = ignoreIfEmpty;
        this.caseInsensitive = caseInsensitive;
        this.customOrderBy = getOrderByText();
        this.sortDirection = getSortType();
        this.offSet = getPageOffset();
        this.size = getPageSize();
    }

    private String getIntCustomOrderBy(QueryParam param, YamlQuery query) {
        String orderKey = param.getTableParam().getOrderBy();
        if (query.getFilter().getAlternativeSortField() == null) return orderKey;
        Map<String, String> alternativeSortMap = query.getFilter().getAlternativeSortField();
        return alternativeSortMap.getOrDefault(orderKey, "");
    }

    private int getPageOffset() {
        return args.containsKey(Const.ES_PARAMS.OFFSET) ?  (int) args.get(Const.ES_PARAMS.OFFSET) : -1;
    }

    private int getPageSize() {
        if (!args.containsKey(Const.ES_PARAMS.PAGE_SIZE)) return -1;
        return Math.min((int) args.get(Const.ES_PARAMS.PAGE_SIZE), Const.ES_UNITS.MAX_SIZE);
    }

    private String getOrderByText() {
        return args.containsKey(Const.ES_PARAMS.ORDER_BY) ? (String) args.get(Const.ES_PARAMS.ORDER_BY) : "";
    }

    private SortOrder getSortType() {
        if (args.containsKey(Const.ES_PARAMS.SORT_DIRECTION))
            return ElasticUtil.getSortType((String) args.get(Const.ES_PARAMS.SORT_DIRECTION));
        return SortOrder.DESC;
    }
}