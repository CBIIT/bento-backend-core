package gov.nih.nci.bento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {"gov.nih.nci"}, exclude = {org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.class}
)
public class BentoApplication extends SpringBootServletInitializer {
	private static final Logger logger = LogManager.getLogger(BentoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BentoApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		logger.info("Server started");

		return application.sources(BentoApplication.class);

	}
}
