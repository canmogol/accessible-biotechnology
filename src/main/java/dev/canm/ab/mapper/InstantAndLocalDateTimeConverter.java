package dev.canm.ab.mapper;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * {@link Instant} to {@link LocalDateTime} mapper.
 */
public class InstantAndLocalDateTimeConverter extends BidirectionalConverter<Instant, LocalDateTime> {

    /**
     * Converter for {@link Instant} to {@link LocalDateTime}.
     *
     * @param source Instant
     * @param destinationType LocalDateTime type
     * @param mappingContext mapping context
     * @return LocalDateTime
     */
    @Override
    public final LocalDateTime convertTo(
        final Instant source,
        final Type<LocalDateTime> destinationType,
        final MappingContext mappingContext) {
        return LocalDateTime.from(source);
    }

    /**
     * Converter {@link LocalDateTime} to {@link Instant}.
     *
     * @param source LocalDateTime
     * @param destinationType Instant type
     * @param mappingContext mapping context
     * @return Instant
     */
    @Override
    public final Instant convertFrom(
        final LocalDateTime source,
        final Type<Instant> destinationType,
        final MappingContext mappingContext) {
        return Instant.from(source);
    }

}
