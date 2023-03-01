package gov.nih.nci.bento.model.search.query;

import gov.nih.nci.bento.constants.Const;
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

    @Builder
    public QueryParam(Map<String, Object> args, GraphQLOutputType outputType) {
        ReturnType returnType = getReturnType(outputType);
        this.args = args;
        this.returnTypes = returnType.fields;
        this.globalSearchResultTypes = returnType.globalSet;
        this.searchText = args.containsKey(Const.ES_PARAMS.INPUT) ?  (String) args.get(Const.ES_PARAMS.INPUT) : "";
    }

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
                List<GraphQLFieldDefinition> lists = type.getFieldDefinitions();
                lists.forEach(field -> defaultSet.add(field.getName()));
            } else if (e instanceof GraphQLFieldDefinition) {
                GraphQLFieldDefinition field = (GraphQLFieldDefinition) e;
                List<GraphQLSchemaElement> obj = field.getChildren();
                GraphQLObjectType outputObject = (GraphQLObjectType) outputType;
                for (GraphQLSchemaElement global : obj) {
                    if (outputObject.getName().contains("GlobalSearch") && global instanceof GraphQLScalarType == false) {
                        // only one
                        GraphQLObjectType graphQLObjectType = (GraphQLObjectType) global.getChildren().get(0);
                        List<GraphQLFieldDefinition> lists = graphQLObjectType.getFieldDefinitions();
                        lists.forEach(globalData -> globalSearchSet.add(globalData.getName()));
                        continue;
                    }
                }
                defaultSet.add(field.getName());
            }
        }
        return ReturnType.builder()
                .fields(defaultSet)
                .globalSet(globalSearchSet)
                .build();
    }
}
