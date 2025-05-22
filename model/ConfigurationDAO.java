package gov.nih.nci.bento.model;

import jakarta.annotation.PostConstruct;
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

	@Value("${graphql.schema:}")
	private String schemaFile;
	@Value("${docs.schema:}")
	private String schemaDoc;

	//Operation Type Enable
	@Value("${allow.graphql.query:true}")
	private boolean allowGraphQLQuery;
	@Value("${allow.graphql.mutation:false}")
	private boolean allowGraphQLMutation;

	//Elasticsearch Configuration
	@Value("${es.host:}")
	private String esHost;
	@Value("${es.port:-1}")
	private int esPort;
	@Value("${es.scheme:http}")
	private String esScheme;
	@Value("${es.filter.enabled:false}")
	private boolean esFilterEnabled;
	@Value("${es.sign.requests:true}")
	private boolean esSignRequests;

	@Value("${es.service.name:}")
	private String serviceName;
	@Value("${es.region:}")
	private String region;

	@Value("${memgraph.endpoint}")
	private String memgraphEndpoint;

	@Value("${memgraph.port}")
	private String memgraphPort;

	@Value("${memgraph.user:}")
	private String memgraphUsername;

	@Value("${memgraph.password:}")
	private String memgraphPassword;

	//Query Limits
	@Value("${validations.max_page_size:10000}")
	private int maxPageSize;

	@PostConstruct
	private void configValidation(){
		boolean abortInitialization = false;
		if(this.esFilterEnabled && (this.esHost.isBlank() || this.esPort == -1)){
			logger.error("Please specify an OpenSearch host and port");
			abortInitialization = true;
		}
		if(this.memgraphEndpoint.isBlank() || this.memgraphPort.isBlank() || this.memgraphUsername.isBlank() || this.memgraphPassword.isBlank()){
			logger.error("Please provide all Memgraph connection variables");
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
