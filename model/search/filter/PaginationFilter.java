package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.model.search.query.QueryFactory;
import org.opensearch.search.builder.SearchSourceBuilder;

public class PaginationFilter extends AbstractFilter {

    public PaginationFilter(FilterParam param) {
        super(param);
    }

    @Override
    SearchSourceBuilder getFilter(FilterParam param, QueryFactory bentoParam) {
        FilterParam.Pagination page = param.getPagination();
        return new SearchSourceBuilder()
                .query(
                        bentoParam.getQuery()
                )
                .from(page.getOffSet())
                .sort(
                        page.getOrderBy(),
                        page.getSortDirection())
                .size(page.getPageSize());
    }
}
