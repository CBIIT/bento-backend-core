package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
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
    // Table Param
    private final int pageSize;
    private final int offSet;
    private final SortOrder sortDirection;
    private final String orderBy;
    private final String defaultSortField;
    private Map<String, String> alternativeSortField;
    private final String pageOrderBy;
    private final Set<String> rangeFilterFields;


    @Builder
    public FilterParam(Map<String, Object> args, String selectedField, Set<String> ignoreIfEmpty,
                       boolean caseInsensitive, String defaultSortField, Map<String, String> alternativeSortField,
                       Set<String> rangeFilterFields) {
        this.args = args;
        this.selectedField = selectedField;
        this.ignoreIfEmpty = ignoreIfEmpty;
        this.caseInsensitive = caseInsensitive;
        this.offSet = getPageOffSet();
        this.pageSize = getSize();
        this.sortDirection = getSortType();
        this.orderBy = getOrderByText();
        this.pageOrderBy = getCustomOrderBy(alternativeSortField);
        this.alternativeSortField = alternativeSortField;
        this.defaultSortField = defaultSortField;
        this.rangeFilterFields = rangeFilterFields;
    }

    private int getPageOffSet() {
        return args.containsKey(Const.ES_PARAMS.OFFSET) ?  (int) args.get(Const.ES_PARAMS.OFFSET) : -1;
    }

    private int getSize() {
        if (!args.containsKey(Const.ES_PARAMS.PAGE_SIZE)) return -1;
        return Math.min((int) args.get(Const.ES_PARAMS.PAGE_SIZE), Const.ES_UNITS.MAX_SIZE);
    }

    private String getOrderByText() {
        return args.containsKey(Const.ES_PARAMS.ORDER_BY) ? (String) args.get(Const.ES_PARAMS.ORDER_BY) : "";
    }

    private SortOrder getSortType() {
        if (args.containsKey(Const.ES_PARAMS.SORT_DIRECTION))
            return getSortType((String) args.get(Const.ES_PARAMS.SORT_DIRECTION));
        return SortOrder.DESC;
    }

    public static SortOrder getSortType(String sort) {
        if (sort==null) return SortOrder.DESC;
        return sort.equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
    }

    private String getCustomOrderBy(Map<String, String> alternativeSortField) {
        String orderKey = getOrderByText();
        if (alternativeSortField == null) return orderKey;
        Map<String, String> alternativeSortMap = alternativeSortField;
        return alternativeSortMap.getOrDefault(orderKey, "");
    }
}