package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.yaml.filter.YamlGlobalFilterType;
import gov.nih.nci.bento.model.search.yaml.filter.YamlHighlight;
import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
import org.opensearch.index.query.*;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.SortOrder;

import java.util.*;

import static gov.nih.nci.bento.utility.StrUtil.getBoolText;
import static gov.nih.nci.bento.utility.StrUtil.getIntText;

public class GlobalQueryFilter {

    private final FilterParam param;
    private final YamlQuery query;

    public GlobalQueryFilter(FilterParam param, YamlQuery query) {
        this.param = param;
        this.query = query;
    }

    public SearchSourceBuilder getSourceFilter() {
        FilterParam.Pagination page = param.getPagination();
        // Store Conditional Query
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .size(page.getPageSize())
                .from(page.getOffSet())
                .query(
                        addConditionalQuery(
                                createGlobalQuerySets(param, query),
                                createGlobalConditionalQueries(param, query))
                );
        // Set Sort
        if (query.getFilter().getDefaultSortField() !=null) builder.sort(query.getFilter().getDefaultSortField(), SortOrder.DESC);
        // Set Highlight Query
        setGlobalHighlightQuery(query, builder);
        return builder;
    }


    // Add Conditional Query
    private BoolQueryBuilder addConditionalQuery(BoolQueryBuilder builder, List<QueryBuilder> builders) {
        builders.forEach(q->{
            if (q.getName().equals(Const.YAML_QUERY.QUERY_TERMS.MATCH)) {
                MatchQueryBuilder matchQuery = getQuery(q);
                if (!matchQuery.value().equals("")) builder.should(q);
            } else if (q.getName().equals(Const.YAML_QUERY.QUERY_TERMS.TERM)) {
                TermQueryBuilder termQuery = getQuery(q);
                if (!termQuery.value().equals("")) builder.should(q);
            }
        });
        return builder;
    }

    @SuppressWarnings("unchecked")
    private <T> T getQuery(QueryBuilder q) {
        String queryType = q.getName();
        return (T) q.queryName(queryType);
    }

    private BoolQueryBuilder createGlobalQuerySets(FilterParam param, YamlQuery query) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        List<YamlGlobalFilterType.GlobalQuerySet> globalQuerySets = query.getFilter().getSearches();
        // Add Should Query
        globalQuerySets.forEach(globalQuery -> {
            switch (globalQuery.getType()) {
                case Const.YAML_QUERY.QUERY_TERMS.TERM:
                    boolQueryBuilder.should(QueryBuilders.termQuery(globalQuery.getField(), param.getSearchText()));
                    break;
                case Const.YAML_QUERY.QUERY_TERMS.WILD_CARD:
                    boolQueryBuilder.should(QueryBuilders.wildcardQuery(globalQuery.getField(), "*" + param.getSearchText()+ "*").caseInsensitive(true));
                    break;
                case Const.YAML_QUERY.QUERY_TERMS.MATCH:
                    boolQueryBuilder.should(QueryBuilders.matchQuery(globalQuery.getField(), param.getSearchText()));
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        });
        return boolQueryBuilder;
    }


    private List<QueryBuilder> createGlobalConditionalQueries(FilterParam param, YamlQuery query) {
        if (query.getFilter().getTypedSearch() == null) return new ArrayList<>();
        List<QueryBuilder> conditionalList = new ArrayList<>();
        List<YamlGlobalFilterType.GlobalQuerySet> typeQuerySets = query.getFilter().getTypedSearch() ;
        for (YamlGlobalFilterType.GlobalQuerySet option : typeQuerySets) {
            String filterString = "";
            if (option.getOption().equals(Const.YAML_QUERY.QUERY_TERMS.BOOLEAN)) {
                filterString = (getBoolText(param.getSearchText()));
            } else if (option.getOption().equals(Const.YAML_QUERY.QUERY_TERMS.INTEGER)) {
                filterString = (getIntText(param.getSearchText()));
            }

            if (option.getType().equals(Const.YAML_QUERY.QUERY_TERMS.MATCH)) {
                conditionalList.add(QueryBuilders.matchQuery(option.getField(), filterString));
            } else if (option.getType().equals(Const.YAML_QUERY.QUERY_TERMS.TERM)) {
                conditionalList.add(QueryBuilders.termQuery(option.getField(), filterString));
            }
        }
        return conditionalList;
    }

    private void setGlobalHighlightQuery(YamlQuery query, SearchSourceBuilder builder) {
        if (query.getHighlight() != null) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            YamlHighlight yamlHighlight = query.getHighlight();
            // Set Multiple Highlight Fields
            yamlHighlight.getFields().forEach(highlightBuilder::field);
            highlightBuilder.preTags(yamlHighlight.getPreTag() == null ? "" : yamlHighlight.getPreTag());
            highlightBuilder.postTags(yamlHighlight.getPostTag() == null ? "" : yamlHighlight.getPostTag());
            if (highlightBuilder.fragmentSize() != null) highlightBuilder.fragmentSize(yamlHighlight.getFragmentSize());
            builder.highlighter(highlightBuilder);
        }
    }
}
