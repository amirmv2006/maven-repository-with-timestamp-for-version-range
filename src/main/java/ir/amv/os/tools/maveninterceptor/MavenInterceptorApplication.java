package ir.amv.os.tools.maveninterceptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.ParseException;

@SpringBootApplication
public class MavenInterceptorApplication {

	public static void main(String[] args) throws ParseException {
		SpringApplication.run(MavenInterceptorApplication.class, args);
	}
}
