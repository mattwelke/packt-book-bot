package com.mattwelke.packtbookbot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import com.owextendedruntimes.actiontest.Action;


/**
 * Reads data from the BigQuery table.
 */
public class DataReaderApiAction extends Action {
    private final Logger logger = Logger.getLogger(DataReaderApiAction.class.getName());

    private BigQuery bqClient;

    /**
     * Implementation of action invoke method.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) {
        try {
            if (bqClient == null) {
                bqClient = bigqueryClient(params);
                logger.log(Level.INFO, "Created new BigQuery client instance.");
            } else {
                logger.log(Level.INFO, "Reusing existing BigQuery client instance.");
            }
            return Map.of("bqResults", queryData());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read data from BigQuery.", ex);
        }
    }

    /**
     * Queries BigQuery to get the 50 most recent free eBooks of the day.
     *
     * @return The list of books.
     * @throws InterruptedException when the BigQuery client library does.
     */
    private List<BookResult> queryData() throws InterruptedException {
        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(
                                "SELECT day, title, ARRAY_TO_STRING(authors_v2.names, ', ') AS authors "
                                        + "FROM `packt-book-bot.public_data.free_ebook_of_the_day` "
                                        + "WHERE day >= '2022-01-30'"
                                        + "ORDER BY day DESC "
                                        + "LIMIT 50")
                        // Use standard SQL syntax for queries.
                        // See: https://cloud.google.com/bigquery/sql-reference/
                        .setUseLegacySql(false)
                        .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bqClient.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job with job ID " + jobId + " no longer exists.");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw new RuntimeException("Job had error(s): " + queryJob.getStatus().getError().toString());
        }

        // Get the results.
        TableResult result = queryJob.getQueryResults();

        // Parse the results.
        List<BookResult> books = StreamSupport.stream(result.iterateAll().spliterator(), false)
                .map(row -> new BookResult(
                        row.get("day").getStringValue(),
                        row.get("title").getStringValue(),
                        row.get("authors").getStringValue()
                )).toList();
        logger.log(Level.INFO, "Finished parsing books. Ended up with {} in total.", books.size());

        return books;
    }

    /**
     * Returns a BigQuery client authenticated with credentials in an OpenWhisk params map.
     *
     * @param params the params map from the action invocation.
     * @return a BigQuery client built from credentials in the params map.
     * @throws IOException when GoogleCredentials.fromStream throws the exception.
     */
    private static BigQuery bigqueryClient(Map<String, Object> params) throws IOException {
        if (!params.containsKey("gcpCreds") || ((String) params.get("gcpCreds")).length() < 1) {
            throw new IllegalArgumentException("missing param gcpCreds");
        }

        byte[] gcpCredsBytes = Base64.getDecoder().decode((String) params.get("gcpCreds"));
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(gcpCredsBytes));

        return BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
    }

    /**
     * For local testing.
     */
    public static void main(String[] args) {
        Map<String, Object> data = new DataReaderApiAction().invoke(Map.of(
                "gcpCreds", System.getenv("gcp_creds")
        ));
        System.out.println("Read data from BigQuery: " + data);
    }
}