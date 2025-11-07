package tn.demo.consent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.demo.consent.repository.EmailOptOutRepository;
import tn.demo.consent.service.OptOutNotificationPolicy;
import tn.demo.project.domain.EmailNotificationPolicy;

@Configuration
public class ConsentConfiguration {
    @Bean
    public EmailNotificationPolicy emailNotificationPolicy(EmailOptOutRepository emailOptOuts){
        return new OptOutNotificationPolicy(emailOptOuts);
    }
}
