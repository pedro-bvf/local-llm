package com.demo.assistant;

import com.demo.assistant.support.AssistantTestBase;
import com.demo.assistant.support.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deterministic proof of the self-correcting retry, with no Ollama: the scripted model first
 * returns HQL that Hibernate rejects, then a valid query. The assistant must feed the parse
 * error back and recover — three model calls total (bad HQL, good HQL, answer).
 *
 * <p>This is the behaviour the regex demos cannot offer: there, an invalid query is simply
 * rejected; here Hibernate's own error message drives a correction.
 */
class SelfCorrectionTest extends AssistantTestBase {

  @Test
  void recoversFromInvalidHqlByFeedingTheErrorBack() {
    final ScriptedChatModel model = new ScriptedChatModel(
        "{\"hql\":\"SELECT p FROM Prodct p\"}",          // 1) typo entity -> Hibernate rejects
        "{\"hql\":\"SELECT COUNT(p) FROM Product p\"}",  // 2) corrected query
        "There are 25 products."                          // 3) natural-language answer
    );
    final HibernateAssistantLC4J assistant =
        new HibernateAssistantLC4J(model, sessionFactory().getMetamodel());

    final String answer = inSession(session ->
        assistant.executeQuery("how many products are there?", session));

    assertThat(answer).isEqualTo("There are 25 products.");
    // 2 HQL-generation attempts + 1 answer phrasing => the retry happened
    assertThat(model.callCount()).isEqualTo(3);
  }

  @Test
  void givesUpAfterMaxAttemptsWhenItNeverProducesValidHql() {
    final ScriptedChatModel model = new ScriptedChatModel(
        "{\"hql\":\"SELECT p FROM Prodct p\"}",   // attempt 1 invalid
        "{\"hql\":\"SELECT p FROM Wrong w\"}"     // attempt 2 invalid -> exhausts retries
    );
    final HibernateAssistantLC4J assistant =
        new HibernateAssistantLC4J(model, sessionFactory().getMetamodel());

    assertThatThrownBy(() ->
        inSession(session -> assistant.executeQuery("how many products are there?", session)))
        .isInstanceOf(RuntimeException.class);
    assertThat(model.callCount()).isEqualTo(2); // both attempts used, no answer phase reached
  }
}
