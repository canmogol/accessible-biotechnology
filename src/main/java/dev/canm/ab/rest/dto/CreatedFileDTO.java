package dev.canm.ab.rest.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatedFileDTO {

    private String createdFileName;
    private String message;

}
