package gov.nih.nci.bento.model.search.query;

import gov.nih.nci.bento.constants.Const;
import gov.nih.nci.bento.model.search.filter.FilterParam;
import graphql.schema.*;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class QueryParam {
    private final Map<String, Object> args;
    private final Set<String> returnTypes;
    private final String searchText;
    private final Set<String> globalSearchResultTypes;

//    private final FilterParam.Pagination pagination;

    @Builder
    public QueryParam(Map<String, Object> args, GraphQLOutputType outputType) {
        ReturnType returnType = getReturnType(outputType);
        this.args = args;
        this.returnTypes = returnType.fields;
        this.globalSearchResultTypes = returnType.globalSet;
        //        this.pagination = setTableParam(args);
        this.searchText = args.containsKey(Const.ES_PARAMS.INPUT) ?  (String) args.get(Const.ES_PARAMS.INPUT) : "";
    }

//    private FilterParam.Pagination setTableParam(Map<String, Object> args) {
//        return FilterParam.Pagination.builder()
//                .offSet(args.containsKey(Const.ES_PARAMS.OFFSET) ?  (int) args.get(Const.ES_PARAMS.OFFSET) : -1)
//                .pageSize(getPageSize(args))
//                .orderBy(getOrderByText(args))
//                .sortDirection(getSortType())
//                .build();
//    }

    @Getter
    private static class ReturnType {
        private final Set<String> fields;
        private final Set<String> globalSet;
        @Builder
        protected ReturnType(Set<String> fields, Set<String> globalSet) {
            this.fields = fields;
            this.globalSet = globalSet;
        }
    }

    private ReturnType getReturnType(GraphQLOutputType outputType) {
        Set<String> defaultSet = new HashSet<>();
        Set<String> globalSearchSet = new HashSet<>();
        SchemaElementChildrenContainer container = outputType.getChildrenWithTypeReferences();

        List<GraphQLSchemaElement> elements = container.getChildrenAsList();
        for (GraphQLSchemaElement e : elements) {
            if (e instanceof GraphQLScalarType) continue;
            if (e instanceof GraphQLObjectType) {
                GraphQLObjectType type = (GraphQLObjectType) e;


                if (type.getName().contains("GlobalSearch") && ((GraphQLFieldDefinition) e).getName().equals("result")) {
                    e.getChildren().forEach(c -> {
                        SchemaElementChildrenContainer container1 = c.getChildrenWithTypeReferences();
                        List<?> fieldTypes = container1.getChildren("wrappedType");
                        fieldTypes.forEach(fileType -> {
                            GraphQLObjectType graphQLType = (GraphQLObjectType) fileType;
                            List<GraphQLFieldDefinition> graphQLFieldDefinitionList = graphQLType.getFieldDefinitions();
                            graphQLFieldDefinitionList.forEach(g -> globalSearchSet.add(g.getName()));
                        });
                    });
                }
                List<GraphQLFieldDefinition> lists = type.getFieldDefinitions();
                lists.forEach(field -> defaultSet.add(field.getName()));
                // TODO
            } else if (e instanceof GraphQLFieldDefinition){
                GraphQLFieldDefinition field = (GraphQLFieldDefinition) e;
                defaultSet.add(field.getName());
            }
        }
        return ReturnType.builder()
                .fields(defaultSet)
                .globalSet(globalSearchSet)
                .build();
    }
}
