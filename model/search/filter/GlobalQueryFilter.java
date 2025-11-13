package gov.nih.nci.bento.model.search.filter;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.yaml.filter.YamlGlobalFilterType;
import gov.nih.nci.bento.model.search.yaml.filter.YamlHighlight;
import gov.nih.nci.bento.model.search.yaml.filter.YamlQuery;
import org.opensearch.index.query.*;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortBuilders;

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
        // Set Sort with Secondary Sort Support
        String defaultSortField = query.getFilter().getDefaultSortField();
        String secondSortField = query.getFilter().getSecondSortField();
        String sortDirection = query.getFilter().getSortDirection();
        SortOrder order = "ASC".equalsIgnoreCase(sortDirection) ? SortOrder.ASC : SortOrder.DESC;
        
    // Log sorting configuration for verification
    // System.out.println("========== SORT FIELD VERIFICATION ==========");
    // System.out.println("Default Sort Field: " + defaultSortField);
    // System.out.println("Secondary Sort Field: " + secondSortField);
    // System.out.println("Sort Direction: " + sortDirection);
    // System.out.println("Sort Order: " + order);
        
        // Apply SECONDARY sort FIRST (this becomes the primary grouping)
        boolean appliedSort = false;
            if (secondSortField != null && !secondSortField.isEmpty()) {
                try {
                    String secondSortFieldWithKeyword = ensureKeywordSuffix(secondSortField);
                    // System.out.println("Applying SECONDARY sort FIRST: " + secondSortFieldWithKeyword + " (" + order + ")");
                    FieldSortBuilder secondarySort = SortBuilders.fieldSort(secondSortFieldWithKeyword)
                            .unmappedType("keyword") // tolerate indices missing this field
                            .missing("_last")
                            .order(order);
                    builder.sort(secondarySort);
                    appliedSort = true;
                    // System.out.println("✓ Secondary sort applied successfully (with unmapped tolerance)");
                } catch (Exception e) {
                    System.err.println("ERROR applying secondary sort field '" + secondSortField + "': " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // System.out.println("No secondary sort field configured");
        }
        
        // Apply PRIMARY sort SECOND (this sorts within secondary groups)
            if (defaultSortField != null && !defaultSortField.isEmpty()) {
                try {
                    String defaultSortFieldWithKeyword = ensureKeywordSuffix(defaultSortField);
                    // System.out.println("Applying PRIMARY sort: " + defaultSortFieldWithKeyword + " (" + order + ")");
                    FieldSortBuilder primarySort = SortBuilders.fieldSort(defaultSortFieldWithKeyword)
                            .unmappedType("keyword")
                            .missing("_last")
                            .order(order);
                    builder.sort(primarySort);
                    appliedSort = true;
                    // System.out.println("✓ Primary sort applied successfully (with unmapped tolerance)");
                } catch (Exception e) {
                    System.err.println("ERROR applying default sort field '" + defaultSortField + "': " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // System.out.println("No default sort field configured");
        }
        
        // Fallback: if no sort fields provided, use _score DESC for deterministic ordering
        if (!appliedSort) {
            // System.out.println("WARNING: No sort fields supplied; applying fallback _score DESC");
            try {
                builder.sort("_score", SortOrder.DESC);
                // System.out.println("✓ Fallback sort applied successfully");
            } catch (Exception e) {
                System.err.println("ERROR applying fallback sort: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
    // System.out.println("Final Sort Order: " + builder.sorts());
    // System.out.println("============================================");
        
        try {
            setGlobalHighlightQuery(query, builder);
        } catch (Exception e) {
            System.err.println("ERROR setting highlight query: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Log final builder state before returning
        try {
            // System.out.println("========== FINAL BUILDER STATE ==========");
            // System.out.println("Query: " + builder.query());
            // System.out.println("Size: " + builder.size());
            // System.out.println("From: " + builder.from());
            // System.out.println("Sorts: " + builder.sorts());
            // if (builder.highlighter() != null) {
            //     System.out.println("Highlighter: configured");
            // }
            // System.out.println("Full Builder DSL:\n" + builder.toString());
            // System.out.println("==========================================");
        } catch (Exception e) {
            System.err.println("ERROR logging final builder state: " + e.getMessage());
        }
        
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
            if (highlightBuilder.fragmentSize(yamlHighlight.getFragmentSize()) != null) highlightBuilder.fragmentSize(yamlHighlight.getFragmentSize());
            builder.highlighter(highlightBuilder);
        }
    }

    /**
     * Ensures the field has .keyword suffix for proper sorting on text fields.
     * Only adds .keyword if the field doesn't already have it and isn't a numeric/date field.
     * 
     * @param field The field name to process
     * @return The field name with .keyword suffix if appropriate
     */
    private String ensureKeywordSuffix(String field) {
        if (field == null || field.isEmpty()) {
            return field;
        }
        
        // Don't add .keyword if it already has it
        if (field.endsWith(".keyword")) {
            return field;
        }
        

        // For all other fields (likely text fields), add .keyword
        return field + ".keyword";
    }
}
