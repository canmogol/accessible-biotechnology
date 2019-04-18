package dev.canm.ab.mapper;

import dev.canm.ab.model.AuditEventModel;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.stereotype.Component;

/**
 * Audit Event Mapper.
 */
@Component
public class AuditEventMapper extends ConfigurableMapper {

    /**
     * Configures conversion between Event and Model.
     *
     * @param factory mapping factory
     */
    protected final void configure(final MapperFactory factory) {
        // use factory to register converters.
        ConverterFactory converterFactory = factory.getConverterFactory();
        // register Instant and LocalDateTime bidirectional converter
        converterFactory.registerConverter(getInstantAndLocalDateTimeConverter());
        // register Map converter
        converterFactory.registerConverter(getObjectMapAndStringMapConverter());

        // Event to Model
        factory.classMap(AuditEvent.class, AuditEventModel.class)
            .byDefault()
            .register();

        // Model to Event
        factory.classMap(AuditEventModel.class, AuditEvent.class)
            .byDefault()
            .register();
    }

    private ObjectMapAndStringMapConverter getObjectMapAndStringMapConverter() {
        return new ObjectMapAndStringMapConverter();
    }

    private InstantAndLocalDateTimeConverter getInstantAndLocalDateTimeConverter() {
        return new InstantAndLocalDateTimeConverter();
    }

}
