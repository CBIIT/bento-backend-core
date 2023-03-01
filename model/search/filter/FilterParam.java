package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
import lombok.Builder;
import lombok.Getter;
import org.opensearch.search.sort.SortOrder;

import java.util.*;

@Getter
public class FilterParam {

    private final Map<String, Object> args;
    private final String selectedField;
    private final Set<String> ignoreIfEmpty;
    private final boolean caseInsensitive;
    private final Set<String> rangeFilterFields;
    private final Pagination pagination;
    private final boolean isIgnoreSelectedField;
    private final String subAggSelectedField;
    private final String nestedPath;
    private final boolean isRangeFilter;
    private final Set<String> nestedParameters;
    private final String searchText;
    private final Map<String, List<String>> extraFilters;

    @Builder
    public FilterParam(Map<String, Object> args, String selectedField, Set<String> ignoreIfEmpty,
                       boolean caseInsensitive, String defaultSortField, Map<String, String> alternativeSortField,
                       boolean isIgnoreSelectedField,String subAggSelectedField,
                       boolean isRangeFilter, Set<String> returnFields,
                       Set<String> rangeFilterFields, Set<String> nestedParameters, String nestedPath,
                       Map<String, List<String>> extraFilters) {
        this.args = args;
        this.selectedField = selectedField;
        this.ignoreIfEmpty = ignoreIfEmpty;
        this.caseInsensitive = caseInsensitive;
        this.subAggSelectedField = subAggSelectedField;
        this.rangeFilterFields = rangeFilterFields == null ? new HashSet<>() : rangeFilterFields;
        this.nestedParameters = nestedParameters != null ? nestedParameters : new HashSet<>();
        this.nestedPath = nestedPath;
        this.isIgnoreSelectedField = isIgnoreSelectedField;
        this.isRangeFilter = isRangeFilter;
        this.pagination = new Pagination.PaginationBuilder()
                .args(args)
                .returnFields(returnFields)
                .alternativeSortField(alternativeSortField)
                .defaultSortField(defaultSortField).build();
        this.searchText = args.containsKey(Const.ES_PARAMS.INPUT) ?  (String) args.get(Const.ES_PARAMS.INPUT) : "";
        this.extraFilters = extraFilters != null ? extraFilters : new HashMap<>();
    }

    @Getter
    public static class Pagination {
        private final Map<String, Object> args;
        private final int pageSize;
        private final int offSet;
        private final SortOrder sortDirection;
        private final String orderBy;
        private final String defaultSortField;
        private final String pageOrderBy;
        private final Set<String> returnFields;

        @Builder
        public Pagination(Map<String, Object> args, String defaultSortField, Map<String, String> alternativeSortField,
                          Set<String> returnFields) {
            this.args = args;
            this.returnFields = returnFields != null ? returnFields : new HashSet<>();
            this.defaultSortField = defaultSortField;
            this.offSet = getPageOffSet();
            this.pageSize = getSize();
            this.sortDirection = getSortType();
            this.orderBy = getOrderByText();
            this.pageOrderBy = getCustomOrderBy(alternativeSortField);
        }

        private int getPageOffSet() {
            return args.containsKey(Const.ES_PARAMS.OFFSET) ?  (int) args.get(Const.ES_PARAMS.OFFSET) : -1;
        }

        private int getSize() {
            if (!args.containsKey(Const.ES_PARAMS.PAGE_SIZE)) return -1;
            return Math.min((int) args.get(Const.ES_PARAMS.PAGE_SIZE), Const.ES_UNITS.MAX_SIZE);
        }

        private String getOrderByText() {
            String orderBy = args.containsKey(Const.ES_PARAMS.ORDER_BY) ? (String) args.get(Const.ES_PARAMS.ORDER_BY) : "";
            if (!returnFields.contains(orderBy)) orderBy = "";
            return orderBy.equals("") ? defaultSortField : orderBy;
        }

        private SortOrder getSortType() {
            if (args.containsKey(Const.ES_PARAMS.SORT_DIRECTION))
                return getSortType((String) args.get(Const.ES_PARAMS.SORT_DIRECTION));
            return SortOrder.DESC;
        }

        private SortOrder getSortType(String sort) {
            if (sort==null) return SortOrder.DESC;
            return sort.equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
        }

        private String getCustomOrderBy(Map<String, String> alternativeSortMap) {
            String orderKey = getOrderByText();
            if (alternativeSortMap == null) return orderKey;
            return alternativeSortMap.getOrDefault(orderKey, orderKey);
        }
    }
}