package gov.nih.nci.bento.graphql;

import gov.nih.nci.bento.model.AbstractESDataFetcher;
import gov.nih.nci.bento.model.AbstractNeo4jDataFetcher;
import gov.nih.nci.bento.model.AbstractPrivateESDataFetcher;
// import gov.nih.nci.bento.model.AbstractPublicESDataFetcher;
import gov.nih.nci.bento.model.ConfigurationDAO;
import gov.nih.nci.bento.model.PrivateNeo4jDataFetcher;
// import gov.nih.nci.bento.model.PublicNeo4jDataFetcher;
import gov.nih.nci.bento.service.RedisService;
import graphql.GraphQL;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLNamedType;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphql.SchemaBuilder;
import org.neo4j.graphql.SchemaConfig;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

@Component
public class BentoGraphQL {

    private static final Logger logger = LogManager.getLogger(BentoGraphQL.class);

    private final ConfigurationDAO config;
    private final GraphQL privateGraphQL;
    // private final GraphQL publicGraphQL;

    public BentoGraphQL(
            ConfigurationDAO config,
            RedisService redisService,
            AbstractPrivateESDataFetcher privateESDataFetcher
            // AbstractPublicESDataFetcher publicESDataFetcher
    ) throws IOException {
        this.config = config;
        // PublicNeo4jDataFetcher publicNeo4JDataFetcher = new PublicNeo4jDataFetcher(config, redisService);
        PrivateNeo4jDataFetcher privateNeo4jDataFetcher = new PrivateNeo4jDataFetcher(config, redisService);

        if (config.isEsFilterEnabled()){
            // this.publicGraphQL = buildGraphQLWithES(config.getPublicSchemaFile(),
            //         config.getPublicEsSchemaFile(), publicNeo4JDataFetcher, publicESDataFetcher);
            this.privateGraphQL = buildGraphQLWithES(config.getSchemaFile(), config.getEsSchemaFile(),
                    privateNeo4jDataFetcher, privateESDataFetcher);
        }
        else{
            // this.publicGraphQL = buildGraphQL(config.getPublicSchemaFile(), publicNeo4JDataFetcher);
            this.privateGraphQL = buildGraphQL(config.getSchemaFile(), privateNeo4jDataFetcher);
        }
    }

    // public GraphQL getPublicGraphQL() {
    //     return publicGraphQL;
    // }

    public GraphQL getPrivateGraphQL() {
        return privateGraphQL;
    }

    private GraphQL buildGraphQL(String neo4jSchemaFile, AbstractNeo4jDataFetcher neo4jDataFetcher) throws IOException {
        GraphQLSchema neo4jSchema = getNeo4jSchema(neo4jSchemaFile, neo4jDataFetcher);
        return GraphQL.newGraphQL(applyIntrospectionVisibility(neo4jSchema)).build();
    }

    private GraphQL buildGraphQLWithES(String neo4jSchemaFile, String esSchemaFile,
            AbstractNeo4jDataFetcher privateNeo4JDataFetcher, AbstractESDataFetcher esBentoDataFetcher) throws IOException {
        GraphQLSchema neo4jSchema = getNeo4jSchema(neo4jSchemaFile, privateNeo4JDataFetcher);
        GraphQLSchema esSchema = getEsSchema(esSchemaFile, esBentoDataFetcher);
        GraphQLSchema mergedSchema = mergeSchema(neo4jSchema, esSchema);
        return GraphQL.newGraphQL(applyIntrospectionVisibility(mergedSchema)).build();
    }

    private GraphQLSchema applyIntrospectionVisibility(GraphQLSchema schema) {
        if (config.isGraphqlIntrospectionEnabled()) {
            return schema;
        }
        return schema.transform(builder -> builder.codeRegistry(
            schema.getCodeRegistry().transform(cr ->
                cr.fieldVisibility(NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY)
            )
        ));
    }

    private GraphQLSchema getNeo4jSchema(String schema, AbstractNeo4jDataFetcher dataFetcher) throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:" + schema);
        File schemaFile = resource.getFile();
        String schemaString = Files.readString(schemaFile.toPath());
        SchemaConfig schemaConfig = new SchemaConfig();

        // Workaround: neo4j-graphql-java uses the GraphQL type name as the Cypher node
        // variable. 'type case' produces the variable 'case', which is a Cypher reserved
        // keyword. We replace the broken auto-generated resolver with a hand-written one
        // and install PropertyDataFetchers for all @relation sub-fields so they read from
        // the pre-fetched Map returned by CaseDataFetcher instead of generating new Cypher.
        CaseDataFetcher caseDataFetcher = new CaseDataFetcher(dataFetcher.getDriver());
        GraphQLSchema builtSchema = SchemaBuilder.buildSchema(schemaString, schemaConfig, dataFetcher);
        GraphQLSchema neo4jSchema = builtSchema.transform(builder ->
            builder.codeRegistry(builtSchema.getCodeRegistry().transform(crBuilder -> {
                crBuilder.dataFetcher(FieldCoordinates.coordinates("QueryType", "case"), caseDataFetcher);
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "cohort"),            PropertyDataFetcher.fetching("cohort"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "study"),             PropertyDataFetcher.fetching("study"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "enrollment"),        PropertyDataFetcher.fetching("enrollment"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "demographic"),       PropertyDataFetcher.fetching("demographic"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "diagnoses"),         PropertyDataFetcher.fetching("diagnoses"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "cycles"),            PropertyDataFetcher.fetching("cycles"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "follow_ups"),        PropertyDataFetcher.fetching("follow_ups"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "samples"),           PropertyDataFetcher.fetching("samples"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "files"),             PropertyDataFetcher.fetching("files"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "visits"),            PropertyDataFetcher.fetching("visits"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "adverse_events"),    PropertyDataFetcher.fetching("adverse_events"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "registrations"),     PropertyDataFetcher.fetching("registrations"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "study_arm"),         PropertyDataFetcher.fetching("study_arm"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "adverse_event"),     PropertyDataFetcher.fetching("adverse_event"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "off_study"),         PropertyDataFetcher.fetching("off_study"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "off_treatment"),     PropertyDataFetcher.fetching("off_treatment"));
                crBuilder.dataFetcher(FieldCoordinates.coordinates("case", "canine_individual"),  PropertyDataFetcher.fetching("canine_individual"));
            }))
        );

        return neo4jSchema;
    }

    private GraphQLSchema getEsSchema(String esSchema, AbstractESDataFetcher bentoDataFetcher) throws IOException {
        File schemaFile = new DefaultResourceLoader().getResource("classpath:" + esSchema).getFile();
        TypeDefinitionRegistry schemaParser = new SchemaParser().parse(schemaFile);
        return new SchemaGenerator().makeExecutableSchema(schemaParser, bentoDataFetcher.buildRuntimeWiring());
    }

    private GraphQLSchema mergeSchema(GraphQLSchema schema1, GraphQLSchema schema2) {
        String QUERY_TYPE_NAME = "Query";
        String MUTATION_TYPE_NAME = "Mutation";
        String SUBSCRIPTION_TYPE_NAME = "Subscription";
        if (schema1 == null) {
            return schema2;
        }
        if (schema2 == null) {
            return schema1;
        }
        var builder = GraphQLSchema.newSchema(schema1);
        var codeRegistry2 = schema2.getCodeRegistry();
        builder.codeRegistry(schema1.getCodeRegistry().transform( crBuilder -> {crBuilder.dataFetchers(codeRegistry2);
            crBuilder.typeResolvers(codeRegistry2);}));
        var allTypes = new HashMap<String, GraphQLNamedType>(schema1.getTypeMap());
        allTypes.putAll(schema2.getTypeMap());
        //Remove individual schema query, mutation, and subscription types from all types to prevent naming conflicts
        allTypes = removeQueryMutationSubscription(allTypes, schema1);
        allTypes = removeQueryMutationSubscription(allTypes, schema2);
        //Add merged query, mutation, and subscription types
        GraphQLNamedType mergedQuery = mergeType(schema1.getQueryType(), schema2.getQueryType());
        if (mergedQuery != null){
            allTypes.put(QUERY_TYPE_NAME, mergedQuery);
        }
        GraphQLNamedType mergedMutation = mergeType(schema1.getMutationType(), schema2.getMutationType());
        if (mergedMutation != null){
            allTypes.put(MUTATION_TYPE_NAME, mergedMutation);
        }
        GraphQLNamedType mergedSubscription = mergeType(schema1.getSubscriptionType(), schema2.getSubscriptionType());
        if (mergedSubscription != null){
            allTypes.put(SUBSCRIPTION_TYPE_NAME, mergedSubscription);
        }
        builder.query((GraphQLObjectType) allTypes.get(QUERY_TYPE_NAME));
        builder.mutation((GraphQLObjectType) allTypes.get(MUTATION_TYPE_NAME));
        builder.subscription((GraphQLObjectType) allTypes.get(SUBSCRIPTION_TYPE_NAME));
        builder.clearAdditionalTypes();
        allTypes.values().forEach(builder::additionalType);
        return builder.build();
    }

    private HashMap<String, GraphQLNamedType> removeQueryMutationSubscription(
            HashMap<String, GraphQLNamedType> allTypes, GraphQLSchema schema){
        try{
            String name = schema.getQueryType().getName();
            allTypes.remove(name);
        }
        catch (NullPointerException e){}

        try{
            String name = schema.getMutationType().getName();
            allTypes.remove(name);
        }
        catch (NullPointerException e){}
        try{
            String name = schema.getSubscriptionType().getName();
            allTypes.remove(name);
        }
        catch (NullPointerException e){}
        return allTypes;
    }

    private GraphQLNamedType mergeType(GraphQLObjectType type1, GraphQLObjectType type2) {
        if (type1 == null) {
            return type2;
        }
        if (type2 == null) {
            return type1;
        }
        var builder = GraphQLObjectType.newObject(type1);
        type2.getFieldDefinitions().forEach(builder::field);
        return builder.build();
    }
}
