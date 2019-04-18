package dev.canm.ab.service.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Change Result.
 */
@Getter
@Builder
public class ChangeResult {

    private Map<String, Long> customNamesAndNumberOfSNPs;

    private Map<String, Double> customNamesAndAverages;

    private String customNameFile;

}
