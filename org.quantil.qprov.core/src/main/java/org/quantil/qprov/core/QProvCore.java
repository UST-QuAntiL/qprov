package org.quantil.qprov.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
// spring application as its needed for the repository/entities/jpa things
@SpringBootApplication(scanBasePackages = "org.quantil.qprov.*")
@OpenAPIDefinition(info = @Info(title = "QProv Core", version = "0.0", license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html"), contact = @Contact(url = "https://github.com/UST-QuAntiL/QProv", name = "GitHub Repository")))
public class QProvCore {

}
