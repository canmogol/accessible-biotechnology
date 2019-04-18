package dev.canm.ab.rest;

import dev.canm.ab.rest.dto.ChangeDTO;
import dev.canm.ab.rest.dto.CreatedFileDTO;
import dev.canm.ab.rest.mapper.ChangeMapper;
import dev.canm.ab.service.ChangeChromosomeNamesException;
import dev.canm.ab.service.ExtractSNPException;
import dev.canm.ab.service.ExtractSNPService;
import dev.canm.ab.service.model.ChangeResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SNP Operations Controller.
 */
@RestController
@RequestMapping("snp")
@Api(tags = {"Provides SNP operations."})
public class SNPController {

    public static final String FILE_CREATED = "File Created.";
    @Autowired
    private ExtractSNPService extractSNPService;

    @Autowired
    private ChangeMapper changeMapper;

    /**
     * REST method to find SNPs with coverage larger than the given number.
     *
     * @param file     SNP source file.
     * @param coverage minimum coverage number.
     * @return REST Response with success if the .
     */
    @GetMapping(path = "/extract")
    @ApiOperation("Extracts the SNPs with coverage larger then the given number.")
    public ResponseEntity<CreatedFileDTO> extractSNPsLargerThenCoverage(
        @RequestParam("file") @ApiParam(defaultValue = "/tmp/crop.test.snp.vcf") final String file,
        @RequestParam("coverage") @ApiParam(defaultValue = "40")final Integer coverage) {
        try {
            String createdFileName = extractSNPService.extractSNPsLargerThenCoverage(file, coverage);
            CreatedFileDTO createdFileDTO = CreatedFileDTO.builder()
                .createdFileName(createdFileName)
                .message(HttpStatus.CREATED.name())
                .build();
            return ResponseEntity.ok(createdFileDTO);
        } catch (ExtractSNPException e) {
            CreatedFileDTO createdFileDTO = CreatedFileDTO.builder()
                .message(e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createdFileDTO);
        }
    }

    /**
     * REST method to change the chromosome names.
     *
     * @param snpFile SNP source file.
     * @param csvFile CSV mapping file
     * @return REST Response with success if the .
     */
    @GetMapping(path = "/change")
    @ApiOperation("Change chromosome names with the CVS mapped values.")
    public ResponseEntity<ChangeDTO> changeChromosomeNames(
        @RequestParam("snpFile") @ApiParam(defaultValue = "/tmp/crop.test.snp.vcf.LargerThenCoverage-40.parquet") final String snpFile,
        @RequestParam("csvFile") @ApiParam(defaultValue = "/tmp/dictionary.csv") final String csvFile) {
        try {
            ChangeResult changeResult = extractSNPService.changeChromosomeNames(snpFile, csvFile);
            ChangeDTO changeDTO = changeMapper.map(changeResult, ChangeDTO.class);
            return ResponseEntity.ok(changeDTO);
        } catch (ChangeChromosomeNamesException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
