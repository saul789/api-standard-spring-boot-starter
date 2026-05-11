package io.github.saul789.api.standard.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;

@SuppressWarnings("PMD.LooseCoupling")
public class ApiStandardOpenApiCustomizer implements OpenApiCustomizer {

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getPaths() != null) {
      openApi.getPaths().values().forEach(this::processPathItem);
    }
  }

  private void processPathItem(io.swagger.v3.oas.models.PathItem pathItem) {
    pathItem.readOperations().forEach(this::processOperation);
  }

  private void processOperation(io.swagger.v3.oas.models.Operation operation) {
    ApiResponses responses = operation.getResponses();
    if (responses != null) {
      wrapSuccessResponses(responses);
      addStandardErrorResponses(responses);
    }
  }

  private void wrapSuccessResponses(ApiResponses responses) {
    responses.keySet().stream()
        .filter(code -> code.startsWith("2"))
        .forEach(code -> processSuccessResponse(responses.get(code)));
  }

  private void processSuccessResponse(ApiResponse response) {
    if (response.getContent() == null) {
      return;
    }

    MediaType jsonMedia = response.getContent().get("application/json");
    if (jsonMedia == null || jsonMedia.getSchema() == null) {
      return;
    }

    Schema<?> originalSchema = jsonMedia.getSchema();
    if (shouldSkipWrapping(originalSchema)) {
      return;
    }

    jsonMedia.setSchema(createWrappedSchema(originalSchema));
  }

  private boolean shouldSkipWrapping(Schema<?> schema) {
    if (schema.get$ref() != null && schema.get$ref().contains("ApiResponse")) {
      return true;
    }
    if ("string".equals(schema.getType())) {
      return schema.getFormat() == null || "binary".equals(schema.getFormat());
    }
    return false;
  }

  private ObjectSchema createWrappedSchema(Schema<?> originalSchema) {
    ObjectSchema wrappedSchema = new ObjectSchema();
    wrappedSchema.addProperty("success", new BooleanSchema()._default(true));
    wrappedSchema.addProperty("data", originalSchema);
    return wrappedSchema;
  }

  private void addStandardErrorResponses(ApiResponses responses) {
    addErrorResponse(responses, "400", "Bad Request / Validation Error");
    addErrorResponse(responses, "401", "Unauthorized");
    addErrorResponse(responses, "403", "Forbidden");
    addErrorResponse(responses, "404", "Not Found");
    addErrorResponse(responses, "500", "Internal Server Error");
  }

  private void addErrorResponse(ApiResponses responses, String statusCode, String description) {
    if (!responses.containsKey(statusCode)) {
      ApiResponse response = new ApiResponse().description(description);
      Content content = new Content();
      MediaType mediaType = new MediaType();

      ObjectSchema problemSchema = new ObjectSchema();
      problemSchema.addProperty(
          "type",
          new StringSchema()
              .description("URI reference identifying the problem type")
              .example("urn:problem-type:error"));
      problemSchema.addProperty(
          "title",
          new StringSchema()
              .description("Short, human-readable problem title")
              .example(description));
      problemSchema.addProperty(
          "status",
          new IntegerSchema()
              .description("HTTP status code")
              .example(Integer.parseInt(statusCode)));
      problemSchema.addProperty(
          "detail",
          new StringSchema()
              .description("Human-readable explanation")
              .example("Detailed message about the error..."));
      problemSchema.addProperty(
          "instance", new StringSchema().description("URI reference of the specific occurrence"));
      problemSchema.addProperty(
          "code",
          new StringSchema().description("Machine-readable error code").example("ERROR_CODE"));
      problemSchema.addProperty(
          "traceId",
          new StringSchema()
              .description("Unique request trace identifier")
              .example("uuid-1234-5678"));

      mediaType.setSchema(problemSchema);
      content.addMediaType("application/problem+json", mediaType);
      response.setContent(content);
      responses.addApiResponse(statusCode, response);
    }
  }
}
