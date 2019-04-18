package dev.canm.ab.model;

import lombok.Data;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit Event Model.
 */
@Data
@Entity
@Table(name = "T_AUDIT_EVENT")
public class AuditEventModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "AEM_ID")
    private Long id;

    @Column(name = "AEM_PRINCIPAL")
    private String principal;

    @Column(name = "AEM_EVENT_DATE")
    private LocalDateTime date;

    @Column(name = "AEM_EVENT_TYPE")
    private String type;

    @ElementCollection
    @MapKeyColumn(name = "AEM_DATA_KEY")
    @Column(name = "AEM_DATA_VALUE")
    @CollectionTable(name = "T_AUDIT_EVENT_DATA", joinColumns = @JoinColumn(name = "AEM_ID"))
    private Map<String, String> data = new HashMap<>();

}
