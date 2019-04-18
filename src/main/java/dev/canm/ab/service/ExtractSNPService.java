package dev.canm.ab.service;

import java.util.Map;

/**
 * SNP extract service.
 */
public interface ExtractSNPService {

    /**
     * Extract SNPs larger then the given coverage value.
     * @param file SNP file name
     * @param coverage minimum coverage
     * @throws ExtractSNPException error
     */
    void extractSNPsLargerThenCoverage(String file, Integer coverage) throws ExtractSNPException;

    /**
     * Change chromosome names in the SNP File with the csv mappings.
     * @param snpFile SNP file
     * @param csvFile CSV file
     * @return Data Map
     * @throws ChangeChromosomeNamesException error
     */
    Map<String, Long> changeChromosomeNames(String snpFile, String csvFile) throws ChangeChromosomeNamesException;

}
