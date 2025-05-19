package gov.nih.nci.bento.graphql;

import gov.nih.nci.bento.model.ConfigurationDAO;
import gov.nih.nci.bento_ri.model.GeneralCommonsRuntimeWiring;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Getter
@Component
public class BentoGraphQL {

    private static final Logger logger = LogManager.getLogger(BentoGraphQL.class);

    private final GraphQL privateGraphQL;

    public BentoGraphQL(
        ConfigurationDAO config,
        GeneralCommonsRuntimeWiring generalCommonsRuntimeWiring,
        PageSizeLimitInstrumentation pageSizeLimitInstrumentation
    ) throws IOException {
        File schemaFile = new DefaultResourceLoader().getResource("classpath:" + config.getSchemaFile()).getFile();
        TypeDefinitionRegistry schemaParser = new SchemaParser().parse(schemaFile);
        GraphQLSchema esSchema = new SchemaGenerator().makeExecutableSchema(schemaParser, generalCommonsRuntimeWiring.getRuntimeWiring());
        this.privateGraphQL = GraphQL.newGraphQL(esSchema).instrumentation(pageSizeLimitInstrumentation).build();
    }
}
