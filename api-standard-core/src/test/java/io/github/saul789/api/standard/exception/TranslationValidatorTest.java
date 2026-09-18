package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class TranslationValidatorTest {

  @Test
  void shouldPassWhenAllTranslationsExist() {
    MessageSource messageSource = mock(MessageSource.class);
    ApiStandardProperties properties = new ApiStandardProperties();

    // Mock that all messages are translated successfully (not returning the marker)
    when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
        .thenReturn("Translated Value");

    TranslationValidator validator = new TranslationValidator(messageSource, properties);
    assertDoesNotThrow(validator::afterSingletonsInstantiated);
  }

  @Test
  void shouldLogWarningWhenTranslationsAreMissingAndNotStrict() {
    MessageSource messageSource = mock(MessageSource.class);
    ApiStandardProperties properties = new ApiStandardProperties();
    properties.getErrors().setStrictTranslationCheck(false);

    // Mock that translations are missing (returning the default marker value)
    when(messageSource.getMessage(anyString(), any(), eq("__MISSING_TRANSLATION__"), any(Locale.class)))
        .thenReturn("__MISSING_TRANSLATION__");

    TranslationValidator validator = new TranslationValidator(messageSource, properties);
    
    // Should log a warning and not throw any exception
    assertDoesNotThrow(validator::afterSingletonsInstantiated);
  }

  @Test
  void shouldThrowExceptionWhenTranslationsAreMissingAndStrict() {
    MessageSource messageSource = mock(MessageSource.class);
    ApiStandardProperties properties = new ApiStandardProperties();
    properties.getErrors().setStrictTranslationCheck(true);

    // Mock that translations are missing (returning the default marker value)
    when(messageSource.getMessage(anyString(), any(), eq("__MISSING_TRANSLATION__"), any(Locale.class)))
        .thenReturn("__MISSING_TRANSLATION__");

    TranslationValidator validator = new TranslationValidator(messageSource, properties);

    // Should throw IllegalStateException because strict check is enabled
    IllegalStateException ex = assertThrows(IllegalStateException.class, validator::afterSingletonsInstantiated);
    assertTrue(ex.getMessage().contains("Strict I18N Translation Check Failed"));
    assertTrue(ex.getMessage().contains("error.INTERNAL_ERROR"));
  }
}
