package gov.nih.nci.bento;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"gov.nih.nci"})
public class BentoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BentoApplication.class, args);
	}
}
