package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Service dedicated to creating and enriching standardized ProblemDetail
 * responses.
 * Implements Single Responsibility Principle (SRP) and Open/Closed Principle
 * (OCP).
 */
@Service
public class ProblemDetailService {

    private final MessageSource messageSource;
    private final ApiStandardProperties properties;
    private final List<ProblemDetailEnricher> enrichers;

    public ProblemDetailService(MessageSource messageSource,
            ApiStandardProperties properties,
            List<ProblemDetailEnricher> enrichers) {
        this.messageSource = messageSource;
        this.properties = properties;
        // Sort enrichers by order before initializing
        this.enrichers = enrichers.stream()
                .sorted(java.util.Comparator.comparingInt(ProblemDetailEnricher::order))
                .toList();
    }

    /**
     * Creates and enriches a ProblemDetail for a given context.
     * 
     * @param status     HTTP Status
     * @param request    The request context
     * @param detailKey  The translation key or literal message for detail
     * @param code       The error code name
     * @param ex         Optional exception to extract type from
     * @param locale     Locale for translation
     * @return The fully enriched ProblemDetail
     */
    public ProblemDetail createProblem(HttpStatus status, HttpServletRequest request, 
                                      String detailKey, String code, 
                                      Exception ex, Locale locale) {
        
        URI customType = extractCustomType(ex);
        ProblemDetail problem = ProblemDetail.forStatus(status);

        // 1. Core Translations (Title & Detail)
        String defaultTitle;
        try {
            defaultTitle = status.getReasonPhrase();
        } catch (Exception _) {
            defaultTitle = "Error";
        }
        problem.setTitle(messageSource.getMessage("error." + code, null, defaultTitle, locale));
        problem.setDetail(messageSource.getMessage(detailKey, null, detailKey, locale));

        // 2. Core Attributes
        problem.setProperty("code", code);
        problem.setType(resolveType(code, customType));

        // 3. Extensible Metadata Enrichment (OCP)
        for (ProblemDetailEnricher enricher : enrichers) {
            enricher.enrich(problem, request, locale);
        }

        return problem;
    }

    private URI extractCustomType(Exception ex) {
        if (ex == null) return null;
        if (ex instanceof ProblemTypeProvider provider) {
            URI type = provider.getProblemType();
            if (type != null) return type;
        }
        ProblemType annotation = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ProblemType.class);
        if (annotation != null) {
            try {
                return URI.create(annotation.value());
            } catch (Exception _) {
                // Return null if annotation value is not a valid URI
            }
        }
        return null;
    }

    private URI resolveType(String code, URI customType) {
        if (customType != null)
            return customType;

        String typeVal = properties.getErrors().getTypeOverrides().get(code);
        if (typeVal != null) {
            try {
                return URI.create(typeVal);
            } catch (Exception _) {
                // If override is an invalid URI, proceed to default generation
            }
        }

        // Default URN generation
        String baseUri = properties.getErrors().getTypeBaseUri();
        String typeSuffix;
        if (code == null || code.isBlank()) {
            typeSuffix = "unknown-error";
        } else {
            try {
                typeSuffix = ErrorCode.valueOf(code).toKebabCase();
            } catch (Exception _) {
                typeSuffix = code.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
            }
        }
        return URI.create(baseUri + typeSuffix);
    }
}
