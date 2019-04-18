package dev.canm.ab.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Spark configuration.
 */
@Configuration
public class SparkConfig {

    @Value("${spark.app.name:accessible-biotechnology}")
    private String applicationName;

    @Value("${spark.home}")
    private String sparkHome;

    @Value("${spark.master:local}")
    private String sparkMaster;

    /**
     * SparkConf bean creator.
     * @return SparkConf
     */
    @Bean
    public SparkConf getSparkConf() {
        return new SparkConf()
            .setAppName(applicationName)
            .setSparkHome(sparkHome)
            .setMaster(sparkMaster);
    }

    /**
     * Java Spark Context bean creator.
     * @return JavaSparkContext
     */
    @Bean
    public JavaSparkContext getJavaSparkContext() {
        return new JavaSparkContext(getSparkConf());
    }

    /**
     * Spark Session bean creator.
     * @return SparkSession
     */
    @Bean
    public SparkSession getSparkSession() {
        return SparkSession
            .builder()
            .sparkContext(getJavaSparkContext().sc())
            .appName(applicationName)
            .getOrCreate();
    }

}
