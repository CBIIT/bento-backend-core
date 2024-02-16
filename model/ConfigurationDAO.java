package gov.nih.nci.bento.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;

/**
 * The Configuration Bean, reads configuration setting from classpath:application.properties.
 */
@Configuration
@PropertySource("classpath:application.properties")
@RequiredArgsConstructor
@Getter
public class ConfigurationDAO implements ApplicationContextAware {
	private static final Logger logger = LogManager.getLogger(ConfigurationDAO.class);

	//Bento API Version
	@Value("${bento.api.version:version not set}")
	private String bentoApiVersion;

	//Enable authentication check
	@Value("${auth.enabled:false}")
	private boolean authEnabled;
	@Value("${auth_endpoint:}")
	private String authEndpoint;

	//Neo4j Connection
	@Value("${neo4j.enabled:true}")
	private boolean neo4jEnabled;
	@Value("${neo4j.url:}")
	private String neo4jUrl;
	@Value("${neo4j.user:}")
	private String neo4jUser;
	@Value("${neo4j.password:}")
	private String neo4jPassword;

	//Private GraphQL Schemas
	@Value("${graphql.schema:}")
	private String schemaFile;
	@Value("${graphql.es_schema:}")
	private String esSchemaFile;

	//Public Graphql Schemas
	@Value("${graphql.public.schema:}")
	private String publicSchemaFile;
	@Value("${graphql.public.es_schema:}")
	private String publicEsSchemaFile;

	//Operation Type Enable
	@Value("${allow_graphql_query:true}")
	private boolean allowGraphQLQuery;
	@Value("${allow_graphql_mutation:false}")
	private boolean allowGraphQLMutation;

	//Redis Cache Configuration
	@Value("${redis.enable:false}")
	private boolean redisEnabled;
	@Value("${redis.use_cluster:false}")
	private boolean redisUseCluster;
	@Value("${redis.host:}")
	private String redisHost;
	@Value("${redis.port:-1}")
	private int redisPort;
	@Value("${redis.ttl:-1}")
	private int redisTTL;

	//Elasticsearch Configuration
	@Value("${es.host:}")
	private String esHost;
	@Value("${es.port:-1}")
	private int esPort;
	@Value("${es.scheme:http}")
	private String esScheme;
	@Value(("${es.filter.enabled:false}"))
	private boolean esFilterEnabled;
	@Value("${es.sign.requests:true}")
	private boolean esSignRequests;

	@Value("${es.service_name:}")
	private String serviceName;
	@Value("${es.region:}")
	private String region;

	//Testing
	@Value("${test.queries_file:}")
	private String testQueriesFile;

	//Query Limits
	@Value("${validations.max_page_size:10000}")
	private int maxPageSize;

	@PostConstruct
	private void configValidation(){
		boolean abortInitialization = false;
		if (this.neo4jEnabled && (this.neo4jUrl.isBlank() || this.neo4jUser.isBlank() || this.neo4jPassword.isBlank())){
			logger.error("Please specify all Neo4j connection variables or disable Neo4j");
			abortInitialization = true;
		}
		if(this.authEnabled && this.authEndpoint.isBlank()){
			logger.error("Please specify an authorization endpoint or disable authorization");
			abortInitialization = true;
		}
		if(this.esFilterEnabled && (this.esHost.isBlank() || this.esPort == -1)){
			logger.error("Please specify an OpenSearch host and port or disable OpenSearch");
			abortInitialization = true;
		}
		if(this.esSchemaFile.isBlank() || this.publicEsSchemaFile.isBlank()){
			logger.error("Please specify a public and private OpenSearch GraphQL schema or disable OpenSearch");
			abortInitialization = true;
		}
		if(this.neo4jEnabled && (this.schemaFile.isBlank() || this.publicSchemaFile.isBlank())){
			logger.error("Please specify a public and private Neo4j GraphQL schema or disable Neo4j");
			abortInitialization = true;
		}
		if (abortInitialization) {
			logger.error("Initialization aborted");
			((ConfigurableApplicationContext) this.context).close();
			System.exit(0);
		}
	}

	private ApplicationContext context;
	@Override
	public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}
}
