package dev.canm.ab.service;

import dev.canm.ab.service.model.ChangeResult;

/**
 * SNP extract service.
 */
public interface ExtractSNPService {

    /**
     * Extract SNPs larger then the given coverage value.
     * @param file SNP file name
     * @param coverage minimum coverage
     * @throws ExtractSNPException message
     * @return created file name
     */
    String extractSNPsLargerThenCoverage(String file, Integer coverage) throws ExtractSNPException;

    /**
     * Change chromosome names in the SNP File with the csv mappings.
     * @param snpFile SNP file
     * @param csvFile CSV file
     * @return ChangeResult change result
     * @throws ChangeChromosomeNamesException message
     */
    ChangeResult changeChromosomeNames(String snpFile, String csvFile) throws ChangeChromosomeNamesException;

}
