package gov.nih.nci.bento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@SpringBootApplication(scanBasePackages = {"gov.nih.nci"})
public class BentoApplication extends SpringBootServletInitializer {
	private static final Logger logger = LogManager.getLogger(BentoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BentoApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		logger.info("Server started");

		// Get the Java Runtime object
        Runtime runtime = Runtime.getRuntime();

        // Get the maximum heap size (in bytes)
        long maxMemory = runtime.maxMemory();
        // Get the initial heap size (in bytes)
        long initialMemory = runtime.totalMemory();
        // Get the current available memory (in bytes)
        long freeMemory = runtime.freeMemory();

        // Convert to MB for better readability
        System.out.println("Initial Heap Size: " + (initialMemory / (1024 * 1024)) + " MB");
        System.out.println("Maximum Heap Size: " + (maxMemory / (1024 * 1024)) + " MB");
        System.out.println("Free Memory: " + (freeMemory / (1024 * 1024)) + " MB");

        // Optionally log the memory usage using MemoryMXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        
        System.out.println("Used Heap Memory: " + (heapMemoryUsage.getUsed() / (1024 * 1024)) + " MB");
        System.out.println("Committed Heap Memory: " + (heapMemoryUsage.getCommitted() / (1024 * 1024)) + " MB");

		return application.sources(BentoApplication.class);

	}
}
