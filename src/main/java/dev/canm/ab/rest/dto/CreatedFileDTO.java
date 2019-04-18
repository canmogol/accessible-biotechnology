package dev.canm.ab.rest.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO for file creation response.
 */
@Getter
@Builder
public class CreatedFileDTO {

    private String createdFileName;
    private String message;

}
