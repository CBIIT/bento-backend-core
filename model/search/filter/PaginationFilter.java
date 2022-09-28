package gov.nih.nci.bento.model.search.filter;


import gov.nih.nci.bento.model.search.query.QueryFactory;
import org.opensearch.search.builder.SearchSourceBuilder;

public class PaginationFilter extends AbstractFilter {

    public PaginationFilter(FilterParam param) {
        super(param);
    }

    @Override
    SearchSourceBuilder getFilter(FilterParam param, QueryFactory bentoParam) {
        return new SearchSourceBuilder()
                .query(
                        bentoParam.getQuery()
                )
                .from(param.getOffSet())
                .sort(
                        param.getCustomOrderBy(),
                        param.getSortDirection())
                .size(param.getSize());
    }
}
