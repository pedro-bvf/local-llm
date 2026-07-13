package com.demo.assistant;

import com.demo.assistant.support.AssistantTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Shows that validation is done by <em>Hibernate itself</em> (HQL parsing), not by a
 * hand-rolled regex like in the sibling demos. Unknown fields/entities and write statements
 * are rejected at {@code createSelectionQuery} time.
 */
class HqlValidationTest extends AssistantTestBase {

  @Test
  void validQueryIsAccepted() {
    assertThatCode(() ->
        inSession(s -> s.createSelectionQuery("SELECT p FROM Product p", Object.class)))
        .doesNotThrowAnyException();
  }

  @Test
  void unknownFieldIsRejectedByHibernate() {
    assertThatThrownBy(() ->
        inSession(s -> s.createSelectionQuery("SELECT p.color FROM Product p", Object.class)))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void unknownEntityIsRejectedByHibernate() {
    assertThatThrownBy(() ->
        inSession(s -> s.createSelectionQuery("SELECT u FROM User u", Object.class)))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void writeStatementCannotRideTheSelectionPath() {
    // createSelectionQuery only accepts SELECT — a DELETE is rejected outright.
    assertThatThrownBy(() ->
        inSession(s -> s.createSelectionQuery("DELETE FROM Product p", Object.class)))
        .isInstanceOf(RuntimeException.class);
  }
}
