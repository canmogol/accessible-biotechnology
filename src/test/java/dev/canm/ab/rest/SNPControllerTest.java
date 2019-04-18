package dev.canm.ab.rest;

import dev.canm.ab.SpringBootABApplication;
import dev.canm.ab.rest.dto.ChangeDTO;
import dev.canm.ab.rest.dto.CreatedFileDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootABApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SNPControllerTest {

    private static final String CREATED = HttpStatus.CREATED.name();
    private static final String EXTRACT_EXISTING_FILE = "src/test/resources/crop.test.snp.vcf";
    private static final String PREFIX_LT_COVERAGE = "LargerThenCoverage";
    private static final Integer COVERAGE = 40;
    private static final String FORMAT_PARQUET = "parquet";
    private static final String EXTRACT_CREATED_PARQUET_FILE =
        String.format("%s.%s-%s.%s", EXTRACT_EXISTING_FILE, PREFIX_LT_COVERAGE, COVERAGE, FORMAT_PARQUET);

    private static final String SNP_EXISTING_FILE = "src/test/resources/crop.test.snp.vcf.LargerThenCoverage-50.parquet";
    private static final String CSV_EXISTING_FILE = "src/test/resources/dictionary.csv";
    private static final String CSV_MAPPED_PARQUET = "CSV-Mapped";
    private static final String MAPPED_CUSTOM_NAME_PARQUET_FILE = String.format("%s.%s.%s", SNP_EXISTING_FILE, CSV_MAPPED_PARQUET, FORMAT_PARQUET);

    @Autowired
    private SNPController snpController;

    @Test
    public void whenExistingFileAndPositiveCoverage_thenCreateParquetFile() {
        ResponseEntity<CreatedFileDTO> responseEntity = snpController.extractSNPsLargerThenCoverage(EXTRACT_EXISTING_FILE, COVERAGE);
        CreatedFileDTO createdFileDTO = responseEntity.getBody();
        String actualMessage = createdFileDTO.getMessage();
        String actualCreatedFileName = createdFileDTO.getCreatedFileName();
        assertEquals("Message should be 'CREATED'.", CREATED, actualMessage);
        assertEquals("Message should be 'CREATED'.", EXTRACT_CREATED_PARQUET_FILE, actualCreatedFileName);
    }

    @Test
    public void whenExistingSNPandCSVFile_thenCreateCustomNameParquetFile() {
        ResponseEntity<ChangeDTO> responseEntity = snpController.changeChromosomeNames(SNP_EXISTING_FILE, CSV_EXISTING_FILE);
        ChangeDTO changeDTO = responseEntity.getBody();
        String actualCustomNameFile = changeDTO.getCustomNameFile();
        assertEquals("Custom Name file should be.", MAPPED_CUSTOM_NAME_PARQUET_FILE, actualCustomNameFile);
    }
}
