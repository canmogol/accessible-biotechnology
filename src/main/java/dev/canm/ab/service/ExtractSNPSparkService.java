package dev.canm.ab.service;

import dev.canm.ab.service.model.ChangeResult;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     *  @param file     SNP file name
     * @param coverage minimum coverage
     * @return created file name
     */
    @Override
    public final String extractSNPsLargerThenCoverage(
        final String file,
        final Integer coverage) throws ExtractSNPException {
        try {
            // extracted file name
            String extractedFileName = getExtractedFileName(file, PREFIX_LT_COVERAGE, coverage, FORMAT_PARQUET);
            Map<String, Set<Long>> chromPosMap = new ConcurrentHashMap<>();

            // filter and create file.
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
                }).write()
                .mode(SaveMode.Overwrite)
                .format(FORMAT_PARQUET)
                .save(extractedFileName);

            // return created file name
            return extractedFileName;
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
    public final ChangeResult changeChromosomeNames(
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
            Dataset<Row> dataFrame = sparkSession.createDataFrame(javaRDD, schema);
            Map<String, Long> customNameAndSNPs = findSNPEntryPerCustomName(dataFrame);
            Map<String, Double> customNameAverage = getCustomNameAndAverage(dataFrame);

            // return change result
            return ChangeResult.builder()
                .customNamesAndNumberOfSNPs(customNameAndSNPs)
                .customNamesAndAverages(customNameAverage)
                .build();

        } catch (IOException e) {
            throw new ChangeChromosomeNamesException(e.getMessage(), e);
        }
    }

    /**
     * Finds the number of SNPs per Custom Name.
     *
     * @param dataFrame Rows
     * @return Map with CustomName to Number of Entries
     */
    private Map<String, Long> findSNPEntryPerCustomName(
        final Dataset<Row> dataFrame) {
        return dataFrame
            .groupBy(KEY_CHROM)
            .count()
            .collectAsList()
            .stream()
            .map(r -> new HashMap.SimpleEntry<String, Long>(r.getAs(KEY_CHROM), r.getAs(KEY_COUNT)))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    /**
     * Finds avarage POS for Custom Name.
     *
     * @param dataFrame rows
     * @return Custom name to avarage mapping
     */
    private Map<String, Double> getCustomNameAndAverage(
        final Dataset<Row> dataFrame) {
        Map<String, List<Long>> customNameToPositions = getCustomNamesAndPositions(dataFrame);
        return customNameToPositions
            .entrySet()
            .stream()
            .map((Function<Map.Entry<String, List<Long>>, Map.Entry<String, Double>>) entry -> {
                double average = entry.getValue()
                    .stream()
                    .mapToDouble(v -> v)
                    .average()
                    .orElse(0);
                return new HashMap.SimpleEntry<>(entry.getKey(), average);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets custom names and positions as a list.
     *
     * @param dataFrame rows
     * @return Map
     */
    private Map<String, List<Long>> getCustomNamesAndPositions(
        final Dataset<Row> dataFrame) {
        return dataFrame
            .select(KEY_CHROM, KEY_POS)
            .collectAsList()
            .stream()
            .map(r -> new HashMap.SimpleEntry<String, List<Long>>(
                r.getAs(KEY_CHROM),
                Collections.singletonList(r.<Long>getAs(KEY_POS))))
            .collect(Collectors.toMap(
                AbstractMap.SimpleEntry::getKey,
                AbstractMap.SimpleEntry::getValue,
                (first, second) ->
                    Stream.concat(first.stream(), second.stream()).collect(Collectors.toList())));
    }

    /**
     * Save the Rows to a new File.
     *
     * @param snpFile       SNP file
     * @param datasetSchema Schema for the Row
     * @param javaRDD       Rows
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
     *
     * @param chromToCustomNameMap Map
     * @param dataset              Rows
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
     *
     * @param snpFile              SNP file
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
     *
     * @param csvFile CSV file
     * @return Map with Chrom to Custom
     * @throws IOException message at reading file
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
     *
     * @param snpFile SNP File
     * @param format  file format
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
