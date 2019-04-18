package dev.canm.ab.service;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SNP Extractor service Spark implementation.
 */
@Service
public class ExtractSNPSparkService implements ExtractSNPService {

    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_PARQUET = "parquet";
    private static final String FORMAT_VCF = "com.lifeomic.variants";

    private static final String KEY_COVERAGE = "dp";
    private static final String KEY_CHROM = "chrom";
    private static final String KEY_POS = "pos";
    private static final String KEY_REF = "ref";
    private static final String KEY_ALT = "alt";
    private static final String KEY_COUNT = "count";

    private static final String PREFIX_LT_COVERAGE = "LargerThenCoverage";
    private static final String SEMICOLON = ";";

    @Autowired
    private SparkSession sparkSession;

    /**
     * Extracts the SNPs with the given coverage.
     *
     * @param file     SNP file name
     * @param coverage minimum coverage
     */
    @Override
    public final void extractSNPsLargerThenCoverage(
        final String file,
        final Integer coverage) throws ExtractSNPException {
        try {
            Map<String, Set<Long>> chromPosMap = new ConcurrentHashMap<>();
            sparkSession
                .read()
                .format(FORMAT_VCF)
                .load(file)
                .select(KEY_CHROM, KEY_POS, KEY_COVERAGE, KEY_REF, KEY_ALT)
                .filter((FilterFunction<Row>) row -> {
                    String chrom = row.getAs(KEY_CHROM);
                    Long pos = row.<Long>getAs(KEY_POS);
                    chromPosMap.putIfAbsent(chrom, new HashSet<>());
                    if (chromPosMap.get(chrom).contains(pos)) {
                        return false;
                    } else {
                        chromPosMap.get(chrom).add(pos);
                        return coverage <= row.<Integer>getAs(KEY_COVERAGE);
                    }
                })                .write()
                .mode(SaveMode.Overwrite)
                .format(FORMAT_PARQUET)
                .save(getExtractedFileName(file, PREFIX_LT_COVERAGE, coverage, FORMAT_PARQUET));
        } catch (Exception e) {
            throw new ExtractSNPException(e.getMessage(), e);
        }
    }

    /**
     * Change chromosome names in the SNP File with the csv mappings.
     *
     * @param snpFile SNP file
     * @param csvFile CSV file
     * @return Map
     */
    @Override
    public final Map<String, Long> changeChromosomeNames(
        final String snpFile,
        final String csvFile) throws ChangeChromosomeNamesException {
        try {

            // Chrom to Custom Name map
            Map<String, String> chromToCustomNameMap = getChromosomeAndCustomNameMap(csvFile);

            // Filtered Dataset
            Dataset<Row> dataset = getDatasetFromSNPFile(snpFile, chromToCustomNameMap);

            // replace the chromosome with custom name, create new Rows
            JavaRDD<Row> javaRDD = createRowsWithCustomName(chromToCustomNameMap, dataset);

            StructType schema = dataset.schema();

            // save the new Rows to a new File
            saveNewFileWithCustomNames(snpFile, schema, javaRDD);

            // find number of SNPs per CustomName
            return findSNPEntryPerCustomName(javaRDD, schema);

        } catch (IOException e) {
            throw new ChangeChromosomeNamesException(e.getMessage(), e);
        }
    }

    /**
     * Finds the number of SNPs per Custom Name.
     * @param javaRDD Rows
     * @param schema Row schema
     * @return Map with CustomName to Number of Entries
     */
    private Map<String, Long> findSNPEntryPerCustomName(
        final JavaRDD<Row> javaRDD,
        final StructType schema) {
        return sparkSession.createDataFrame(javaRDD, schema)
            .groupBy(KEY_CHROM)
            .count()
            .collectAsList()
            .stream()
            .map(r -> new HashMap.SimpleEntry<String, Long>(r.getAs(KEY_CHROM), r.getAs(KEY_COUNT)))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    /**
     * Save the Rows to a new File.
     * @param snpFile SNP file
     * @param datasetSchema Schema for the Row
     * @param javaRDD Rows
     */
    private void saveNewFileWithCustomNames(
        final String snpFile,
        final StructType datasetSchema,
        final JavaRDD<Row> javaRDD) {
        sparkSession.createDataFrame(javaRDD, datasetSchema)
            .write()
            .mode(SaveMode.Overwrite)
            .format(FORMAT_JSON)
            .save(getMappedFileName(snpFile, FORMAT_JSON));
    }

    /**
     * Creates new set of Rows with Custom Name instead of Chromosome.
     * @param chromToCustomNameMap Map
     * @param dataset Rows
     * @return New set of Rows
     */
    private JavaRDD<Row> createRowsWithCustomName(
        final Map<String, String> chromToCustomNameMap,
        final Dataset<Row> dataset) {
        return dataset
            .javaRDD()
            .map(row -> {
                String customName = chromToCustomNameMap.get(row.<String>getAs(KEY_CHROM));
                return RowFactory.create(
                    customName,
                    row.getAs(KEY_POS),
                    row.getAs(KEY_COVERAGE),
                    row.getAs(KEY_REF),
                    row.getAs(KEY_ALT)
                );
            });
    }

    /**
     * Loads SNP file and filters out the Rows that are not in the Custom Name Map.
     * @param snpFile SNP file
     * @param chromToCustomNameMap Map
     * @return Dataset
     */
    private Dataset<Row> getDatasetFromSNPFile(
        final String snpFile,
        final Map<String, String> chromToCustomNameMap) {
        return sparkSession
            .read()
            .format(FORMAT_PARQUET)
            .load(snpFile)
            .filter((FilterFunction<Row>) row ->
                chromToCustomNameMap.containsKey(row.<String>getAs(KEY_CHROM)));
    }

    /**
     * Creates a Map with Chromosome and its Custom Name map.
     * @param csvFile CSV file
     * @return Map with Chrom to Custom
     * @throws IOException error at reading file
     */
    private Map<String, String> getChromosomeAndCustomNameMap(
        final String csvFile) throws IOException {
        return Files.readAllLines(Paths.get(csvFile))
            .stream()
            .map(l -> {
                String[] snpNameCustomName = l.split(SEMICOLON);
                return new HashMap.SimpleEntry<>(snpNameCustomName[0], snpNameCustomName[1]);
            })
            .collect(Collectors.toMap(HashMap.SimpleEntry::getKey, HashMap.SimpleEntry::getValue));
    }

    /**
     * Create an CSV-Mapped file name for SNP file.
     * @param snpFile SNP File
     * @param format file format
     * @return mapped file name
     */
    private String getMappedFileName(
        final String snpFile,
        final String format) {
        return String.format("%s.CSV-Mapped.%s", snpFile, format);
    }

    /**
     * Creates the file name to save.
     *
     * @param file   original file name
     * @param prefix prefix
     * @param value  value
     * @param format file format
     * @return file name
     */
    private String getExtractedFileName(
        final String file,
        final String prefix,
        final Integer value,
        final String format) {
        return String.format("%s.%s-%s.%s", file, prefix, value, format);
    }

}
