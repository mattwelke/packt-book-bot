package com.mattwelke.packtbookbot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.owextendedruntimes.actiontest.Action;

/**
 * Given data about the Packt free book of the day, shares it publicly by
 * writing it to Google Cloud Platform. Right now, that means inserting a row
 * into a BigQuery table in a public BigQuery dataset.
 */
public class GcpDataSharerAction extends Action {
    private static final String datasetName = "public_data";
    private static final String tableName = "free_ebook_of_the_day";

    /**
     * Given data about the title, shares it via GCP.
     */
    @Override
    public Map<String, Object> invoke(Map<String, Object> params) throws RuntimeException {
        try {
            var data = TitleData.of(params);
            var bigquery = bigqueryClient(params);

            InsertAllResponse response = bigquery.insertAll(
                    InsertAllRequest.newBuilder(TableId.of(datasetName, tableName))
                            .addRow(row(data))
                            .build());

            if (response.hasErrors()) {
                var insertErrors = response.getInsertErrors().entrySet();
                throw new RuntimeException(String.format("failed to insert into BigQuery: %s", insertErrors));
            }

            return Map.of("bigqueryInsertResponse", response);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("failed to share data via GCP: %s", ex.getMessage()), ex);
        }
    }

    /**
     * Given a params map, returns an authenticated BigQuery client.
     *
     * @param params The params map from the action invocation.
     * @return A BigQuery client build from credentials in the params map.
     * @throws IOException when it fails to load GCP credentials from the provided params map.
     */
    private static BigQuery bigqueryClient(Map<String, Object> params) throws IOException {
        if (!params.containsKey("gcpCreds") || ((String) params.get("gcpCreds")).length() < 1) {
            throw new IllegalArgumentException("missing param gcpCreds");
        }

        var gcpCredsBytes = Base64.getDecoder().decode((String) params.get("gcpCreds"));

        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(gcpCredsBytes));

        return BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    /**
     * Given title data, returns a row to insert into BigQuery.
     *
     * @param data The title data.
     * @return The row to insert.
     */
    private static Map<String, Object> row(TitleData data) {
        Authors authors = data.authors();

        Map<String, Object> rowContent = new HashMap<>();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        rowContent.put("day", dateFormat.format(date));

        rowContent.put("title", data.title());

        rowContent.put("publication_date", String.format("%s-%s-01",
                data.pubDateYear(), PublicationDateMonths.monthNumber(data.pubDateMonth())));

        // TODO: Deprecate this field because we have the new authors_v2 field.
        rowContent.put("authors", authors.names());

        rowContent.put("inserted_at", Instant.now().toString());

        rowContent.put("authors_v2", Map.of(
                "names", authors.names(),
                "all_present", !authors.more()));

        return rowContent;
    }

    /**
     * For local testing.
     */
    public static void main(String[] args) throws RuntimeException {
        new GcpDataSharerAction().invoke(Map.of(
                "title", "Hands-On Software Engineering with Python",
                "pubDateMonth", "October",
                "pubDateYear", "2018",
                "authors", List.of("Brian Allbee"),
                "gcpCreds", System.getenv("GCP_CREDS")));
    }
}
