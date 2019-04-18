package dev.canm.ab.service;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
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
                })
                .write()
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
     * @return
     */
    @Override
    public final Map<String, Long> changeChromosomeNames(
        final String snpFile,
        final String csvFile) throws ChangeChromosomeNamesException {
        try {
            Map<String, String> chromToCustomNameMap = Files.readAllLines(Paths.get(csvFile))
                .stream()
                .map(l -> {
                    String[] snpNameCustomName = l.split(SEMICOLON);
                    return new HashMap.SimpleEntry<>(snpNameCustomName[0], snpNameCustomName[1]);
                })
                .collect(Collectors.toMap(HashMap.SimpleEntry::getKey, HashMap.SimpleEntry::getValue));

            Dataset<Row> dataset = sparkSession
                .read()
                .format(FORMAT_PARQUET)
                .load(snpFile)
                .filter((FilterFunction<Row>) row ->
                    chromToCustomNameMap.containsKey(row.<String>getAs(KEY_CHROM)));

            JavaRDD<Row> javaRDD = dataset
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

            sparkSession.createDataFrame(javaRDD, dataset.schema())
                .write()
                .mode(SaveMode.Overwrite)
                .format(FORMAT_JSON)
                .save(getMappedFileName(snpFile, FORMAT_JSON));

            return sparkSession.createDataFrame(javaRDD, dataset.schema())
                .groupBy(KEY_CHROM)
                .count()
                .collectAsList()
                .stream()
                .map(r -> new HashMap.SimpleEntry<String, Long>(r.getAs(KEY_CHROM), r.getAs(KEY_COUNT)))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        } catch (IOException e) {
            throw new ChangeChromosomeNamesException(e.getMessage(), e);
        }
    }

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
