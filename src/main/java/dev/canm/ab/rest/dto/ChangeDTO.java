package dev.canm.ab.rest.dto;

import lombok.Data;

import java.util.Map;

/**
 * DTO for custom name change response.
 */
@Data
public class ChangeDTO {

    private Map<String, Long> customNamesAndNumberOfSNPs;

    private Map<String, Double> customNamesAndAverages;

}
