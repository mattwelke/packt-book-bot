package com.mattwelke.packtbookbot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import com.mattwelke.owr.java.Action;


public class GcpDataSharerAction extends Action {
    private static final String datasetName = "public_data";
    private static final String tableName = "free_ebook_of_the_day";

    private final Logger logger = Logger.getLogger(GcpDataSharerAction.class.getName());

    /**
     * Given data about the Packt free book of the day, shares it publicly by writing it to Google Cloud Platform. Right
     * now, that means inserting a row into a BigQuery table in a public BigQuery dataset.
     *
     * @param params the OpenWhisk action invocation input.
     * @return the output. Right now, this is just information about the BigQuery insert response, so that OpenWhisk
     * logs it.
     * @throws RuntimeException
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) throws RuntimeException {
        try {
            TitleData data = TitleData.of(params);
            BigQuery bigquery = bigqueryClient(params);

            InsertAllResponse response = bigquery.insertAll(InsertAllRequest.newBuilder(
                    TableId.of(datasetName, tableName)).addRow(row(data)).build());

            if (response.hasErrors()) {
                logger.log(Level.SEVERE, "Errors from BigQuery insert: {}", response.getInsertErrors());
                throw new CouldNotShareDataException("BigQuery insert unsuccessful.");
            }
            return Map.of("bigqueryInsertResponse", response);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to share data via GCP.", ex);
        }
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
     * Given title data, returns a row to insert into BigQuery.
     *
     * @param data the title data.
     * @return the row to insert.
     */
    private static Map<String, Object> row(TitleData data) {
        Authors authors = data.authors();

        Map<String, Object> rowContent = new HashMap<>();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        rowContent.put("day", dateFormat.format(date));

        rowContent.put("title", data.title());

        rowContent.put("publication_date", String.format("%s-%s-01", data.pubDate().year(),
                PublicationDateMonths.monthNumber(data.pubDate().month())));

        // TODO: Deprecate this field because we have the new authors_v2 field.
        rowContent.put("authors", authors.names());

        rowContent.put("inserted_at", Instant.now().toString());

        rowContent.put("authors_v2", Map.of("names", authors.names(), "all_present", !authors.more()));

        return rowContent;
    }

    /**
     * For local testing.
     */
    public static void main(String[] args) {
        new GcpDataSharerAction().invoke(Map.of(
                "title", "Hands-On Software Engineering with Python",
                "pubDateMonth", "October",
                "pubDateYear", "2018",
                "authors", List.of("Brian Allbee"),
                "gcpCreds", System.getenv("GCP_CREDS")));
    }
}
