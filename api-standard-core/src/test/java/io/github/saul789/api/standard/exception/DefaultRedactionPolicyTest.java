package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DefaultRedactionPolicyTest {

  private DefaultRedactionPolicy policy;
  private ApiStandardProperties properties;

  @BeforeEach
  void setUp() {
    properties = new ApiStandardProperties();
    policy = new DefaultRedactionPolicy(properties);
  }

  @ParameterizedTest
  @CsvSource({
    "password, true",
    "PASSWORD, true",   // case-insensitive
    "token, true",
    "secret, true",
    "creditCard, true",
    "cvv, true",
    "ssn, true",
    "authorization, true",
    "name, false",
    "email, false",
    "age, false"
  })
  void testShouldRedact(String fieldName, boolean expected) {
    assertEquals(expected, policy.shouldRedact(fieldName));
  }

  @Test
  void whenFieldIsNull_shouldReturnFalse() {
    assertFalse(policy.shouldRedact(null));
  }

  @Test
  void whenCustomFieldsConfigured_shouldUseCustomList() {
    properties.getRedaction().setFields(java.util.List.of("mySecret"));
    assertTrue(policy.shouldRedact("mySecret"));
    assertFalse(policy.shouldRedact("password")); // no longer in list
  }
}
