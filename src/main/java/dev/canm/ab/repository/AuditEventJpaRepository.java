package dev.canm.ab.repository;

import dev.canm.ab.model.AuditEventModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuditEvent JPA implementation.
 */
@Repository
public interface AuditEventJpaRepository extends JpaRepository<AuditEventModel, Long> {

    /**
     * Finds all models by principal.
     *
     * @param principal principal of the event.
     * @return List of AuditEventModels
     */
    List<AuditEventModel> findByPrincipal(final String principal);

    /**
     * Finds all models by principal.
     *
     * @param principal principal of the event.
     * @param after     starting from date
     * @param type      type of the event
     * @return List of AuditEventModels
     */
    List<AuditEventModel> findByPrincipalAndDateAfterAndType(
        final String principal,
        LocalDateTime after,
        String type);

    /**
     * Finds all models after a date.
     *
     * @param after date after.
     * @return List of AuditEventModels
     */
    List<AuditEventModel> findByDateAfter(final LocalDateTime after);

    /**
     * Finds all models before a date.
     *
     * @param before date before.
     * @return List of AuditEventModels
     */
    List<AuditEventModel> findByDateBefore(final LocalDateTime before);

    /**
     * Finds all models between dates.
     *
     * @param from date
     * @param to   date
     * @return List of AuditEventModels
     */
    List<AuditEventModel> findAllByDateBetween(
        final LocalDateTime from, final LocalDateTime to
    );

}
