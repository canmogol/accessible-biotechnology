package dev.canm.ab.rest;

import dev.canm.ab.service.ChangeChromosomeNamesException;
import dev.canm.ab.service.ExtractSNPException;
import dev.canm.ab.service.ExtractSNPService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SNP Operations Controller.
 */
@RestController
@RequestMapping("snp")
@Api(tags = {"Provides SNP operations."})
public class SNPController {

    @Autowired
    private ExtractSNPService extractSNPService;

    /**
     * REST method to find SNPs with coverage larger than the given number.
     *
     * @param file     SNP source file.
     * @param coverage minimum coverage number.
     * @return REST Response with success if the .
     */
    @GetMapping(path = "/extract")
    @ApiOperation("Extracts the SNPs with coverage larger then the given number.")
    public ResponseEntity<String> extractSNPsLargerThenCoverage(
        @RequestParam("file") final String file,
        @RequestParam("coverage") final Integer coverage) {
        try {
            extractSNPService.extractSNPsLargerThenCoverage(file, coverage);
            return ResponseEntity.ok().build();
        } catch (ExtractSNPException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    /**
     * REST method to change the chromosome names.
     *
     * @param snpFile SNP source file.
     * @param csvFile CSV mapping file
     * @return REST Response with success if the .
     */
    @GetMapping(path = "/mapped")
    @ApiOperation("Change chromosome names with the CVS mapped values.")
    public ResponseEntity<Map<String, Long>> changeChromosomeNames(
        @RequestParam("snpFile") final String snpFile,
        @RequestParam("csvFile") final String csvFile) {
        try {
            Map<String, Long> stringIntegerMap = extractSNPService.changeChromosomeNames(snpFile, csvFile);
            return ResponseEntity.ok(stringIntegerMap);
        } catch (ChangeChromosomeNamesException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
