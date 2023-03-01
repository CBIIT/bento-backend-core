package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.query.QueryFactory;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

public class AggregationFilter extends AbstractFilter {

    public AggregationFilter(FilterParam param) {
        super(param);
    }

    @Override
    SearchSourceBuilder getFilter(FilterParam param, QueryFactory bentoParam) {
        return new SearchSourceBuilder()
                .size(0)
                .query(bentoParam.getQuery())
                .aggregation(AggregationBuilders
                        .terms(Const.ES_PARAMS.TERMS_AGGS)
                        .size(Const.ES_PARAMS.AGGS_SIZE)
                        .field(param.getSelectedField()));
    }
}
