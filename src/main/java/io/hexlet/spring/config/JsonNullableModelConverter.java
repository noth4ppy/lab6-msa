package io.hexlet.spring.config;

import java.util.Iterator;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Documents JsonNullable&lt;T&gt; fields (BookUpdateDTO) as plain T in the OpenAPI description:
 * without it springdoc shows wrapper schemas JsonNullableString {present: boolean}.
 * springdoc registers every ModelConverter bean automatically.
 */
@Component
public class JsonNullableModelConverter implements ModelConverter {

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        JavaType javaType = Json.mapper().constructType(type.getType());
        if (javaType != null && JsonNullable.class.equals(javaType.getRawClass())
                && javaType.containedTypeCount() == 1) {
            var inner = new AnnotatedType(javaType.containedType(0))
                    .name(type.getName())
                    .parent(type.getParent())
                    .propertyName(type.getPropertyName())
                    .ctxAnnotations(type.getCtxAnnotations())
                    .jsonViewAnnotation(type.getJsonViewAnnotation())
                    .schemaProperty(type.isSchemaProperty())
                    .resolveAsRef(type.isResolveAsRef());
            return chain.next().resolve(inner, context, chain);
        }
        return chain.next().resolve(type, context, chain);
    }
}
