package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Validates at startup that all canonical ErrorCodes have matching i18n title translations.
 * Supports a flexible validation mode (warnings by default, strict exceptions when configured).
 */
@Component
public class TranslationValidator implements SmartInitializingSingleton {

  private static final Logger log = LoggerFactory.getLogger(TranslationValidator.class);
  private static final String MISSING_MARKER = "__MISSING_TRANSLATION__";

  private final MessageSource messageSource;
  private final ApiStandardProperties properties;

  public TranslationValidator(MessageSource messageSource, ApiStandardProperties properties) {
    this.messageSource = messageSource;
    this.properties = properties;
  }

  @Override
  public void afterSingletonsInstantiated() {
    List<String> missingEnglish = new ArrayList<>();
    List<String> missingSpanish = new ArrayList<>();

    Locale localeEs = Locale.forLanguageTag("es");

    for (ErrorCode code : ErrorCode.values()) {
      String key = "error." + code.name();

      // Check English / Default
      String enVal = messageSource.getMessage(key, null, MISSING_MARKER, Locale.ENGLISH);
      if (MISSING_MARKER.equals(enVal)) {
        missingEnglish.add(code.name());
      }

      // Check Spanish
      String esVal = messageSource.getMessage(key, null, MISSING_MARKER, localeEs);
      if (MISSING_MARKER.equals(esVal)) {
        missingSpanish.add(code.name());
      }
    }

    boolean hasMissing = !missingEnglish.isEmpty() || !missingSpanish.isEmpty();

    if (hasMissing) {
      StringBuilder builder = new StringBuilder();
      builder.append("\n====================================================================\n");
      builder.append("⚠️  API STANDARD I18N VALIDATION WARNING  ⚠️\n");
      builder.append("Some canonical ErrorCodes are missing translations in properties files.\n");
      
      if (!missingEnglish.isEmpty()) {
        builder.append("Missing ENGLISH translations (messages.properties):\n");
        for (String code : missingEnglish) {
          builder.append("  - error.").append(code).append("\n");
        }
      }
      
      if (!missingSpanish.isEmpty()) {
        builder.append("Missing SPANISH translations (messages_es.properties):\n");
        for (String code : missingSpanish) {
          builder.append("  - error.").append(code).append("\n");
        }
      }
      
      builder.append("====================================================================");

      if (properties.getErrors().isStrictTranslationCheck()) {
        throw new IllegalStateException("Strict I18N Translation Check Failed!" + builder.toString());
      } else {
        log.warn(builder.toString());
      }
    } else {
      log.info("API Standard: All canonical ErrorCodes are successfully translated.");
    }
  }
}
