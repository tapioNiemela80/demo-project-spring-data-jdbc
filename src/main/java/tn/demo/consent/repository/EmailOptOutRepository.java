package tn.demo.consent.repository;

import org.springframework.data.repository.CrudRepository;
import tn.demo.consent.domain.EmailOptOut;

public interface EmailOptOutRepository extends CrudRepository<EmailOptOut, String> {
    boolean existsByEmail(String email);
}
