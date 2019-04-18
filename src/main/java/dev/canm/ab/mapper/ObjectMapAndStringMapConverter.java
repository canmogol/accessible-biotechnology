package dev.canm.ab.mapper;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Map converter for {@link org.springframework.boot.actuate.audit.AuditEvent}.
 */
public class ObjectMapAndStringMapConverter extends BidirectionalConverter<Map<String, Object>, Map<String, String>> {

    /**
     * Object to String mapping.
     *
     * @param source Object map
     * @param destinationType String map type
     * @param mappingContext mapping context.
     * @return String map
     */
    @Override
    public final Map<String, String> convertTo(
        final Map<String, Object> source,
        final Type<Map<String, String>> destinationType,
        final MappingContext mappingContext) {
        return source
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> String.valueOf(e.getValue())
            ));
    }

    /**
     * String to Object mapping.
     *
     * @param source String map
     * @param destinationType Object map type
     * @param mappingContext mapping context
     * @return Object map
     */
    @Override
    public final Map<String, Object> convertFrom(
        final Map<String, String> source,
        final Type<Map<String, Object>> destinationType,
        final MappingContext mappingContext) {
        return source
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }
}
