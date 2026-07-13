package com.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Internal audit trail. It is a fully mapped entity (a real table), but it must NOT be
 * reachable by the assistant. It is hidden as a whole entity via the custom
 * MetamodelSerializer, so the LLM never learns this table exists.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "entity_name")
  private String entityName;

  private String action;

  @Column(name = "performed_by")
  private String performedBy;

  @Column(name = "occurred_at")
  private Instant occurredAt;

}
