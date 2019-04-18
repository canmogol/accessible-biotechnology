package dev.canm.ab.rest.mapper;

import dev.canm.ab.rest.dto.ChangeDTO;
import dev.canm.ab.service.model.ChangeResult;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.springframework.stereotype.Component;

/**
 * Change Mapper.
 */
@Component
public class ChangeMapper extends ConfigurableMapper {

    /**
     * Configures conversion between Model and DTO.
     *
     * @param factory mapping factory
     */
    @Override
    protected final void configure(final MapperFactory factory) {
        factory.classMap(ChangeResult.class, ChangeDTO.class)
            .byDefault()
            .register();
        factory.classMap(ChangeDTO.class, ChangeResult.class)
            .byDefault()
            .register();
    }

}
