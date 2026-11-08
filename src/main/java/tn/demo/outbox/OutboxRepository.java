package tn.demo.outbox;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OutboxRepository extends CrudRepository<OutboxEntry, UUID> {
}