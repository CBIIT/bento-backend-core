package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
import lombok.Builder;
import lombok.Getter;
import org.opensearch.search.sort.SortOrder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class FilterParam {

    private final Map<String, Object> args;
    private final String selectedField;
    private final Set<String> ignoreIfEmpty;
    private final boolean caseInsensitive;
    private final Set<String> rangeFilterFields;
    private final Pagination pagination;
    private final boolean isExcludeFilter;
    private boolean ignoreSelectedField;
    private final String subAggSelectedField;
    private final String nestedPath;
    // TODO check
    private final boolean isRangeFilter;
    private final Set<String> nestedParameters;

    @Builder
    public FilterParam(Map<String, Object> args, String selectedField, Set<String> ignoreIfEmpty,
                       boolean caseInsensitive, String defaultSortField, Map<String, String> alternativeSortField,
                       boolean isExcludeFilter,boolean ignoreSelectedField,String subAggSelectedField,
                       boolean isRangeFilter,
                       Set<String> rangeFilterFields, Set<String> nestedParameters, String nestedPath) {
        this.args = args;
        this.selectedField = selectedField;
        this.ignoreIfEmpty = ignoreIfEmpty;
        this.caseInsensitive = caseInsensitive;
        this.subAggSelectedField = subAggSelectedField;
        this.rangeFilterFields = rangeFilterFields == null ? new HashSet<>() : rangeFilterFields;
        this.nestedParameters = nestedParameters != null ? nestedParameters : new HashSet<>();
        this.ignoreSelectedField = ignoreSelectedField;
        this.nestedPath = nestedPath;
        this.isExcludeFilter = isExcludeFilter;
        this.isRangeFilter = isRangeFilter;
        this.pagination = new Pagination.PaginationBuilder()
                .args(args)
                .alternativeSortField(alternativeSortField)
                .defaultSortField(defaultSortField).build();
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

        @Builder
        public Pagination(Map<String, Object> args, String defaultSortField, Map<String, String> alternativeSortField) {
            this.args = args;
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