package gov.nih.nci.bento.service;

import com.google.gson.*;

import gov.nih.nci.bento.model.ConfigurationDAO;
import gov.nih.nci.bento.model.search.MultipleRequests;
import gov.nih.nci.bento.service.connector.AWSClient;
import gov.nih.nci.bento.service.connector.AbstractClient;
import gov.nih.nci.bento.service.connector.DefaultClient;

import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.opensearch.OpenSearchException;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.MultiSearchResponse;
import org.opensearch.client.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Service("ESService")
public class ESService {
    public static final String SCROLL_ENDPOINT = "/_search/scroll";
    public static final String JSON_OBJECT = "jsonObject";
    public static final String AGGS = "aggs";
    public static final int MAX_ES_SIZE = 200000; // Do not return more than this number of records
    public static final int SCROLL_THRESHOLD = 10000; // Use scroll when trying to retrieve past this number of records
    public static final int SCROLL_SIZE = 10000; // How big each scroll should be

    private static final Logger logger = LogManager.getLogger(RedisService.class);
    private RestClient client;
    private final RestHighLevelClient restHighLevelClient;
    private final Gson gson;

    public ESService(ConfigurationDAO config){
        this.gson = new GsonBuilder().serializeNulls().create();
        logger.info("Initializing Elasticsearch client");
        // Base on host name to use signed request (AWS) or not (local)
        AbstractClient abstractClient = config.isEsSignRequests() ? new AWSClient(config) : new DefaultClient(config);
        restHighLevelClient = abstractClient.getElasticClient();
        client = abstractClient.getLowLevelElasticClient();
    }

    @PreDestroy
    private void close() throws IOException {
        client.close();
    }

    public JsonObject send(Request request) throws IOException{
        Response response = client.performRequest(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            String msg = "Elasticsearch returned code: " + statusCode;
            logger.error(msg);
            throw new IOException(msg);
        }
        return getJSonFromResponse(response);
    }

    public void checkMemoryInit() {
        // Get the Java Runtime object
        Runtime runtime = Runtime.getRuntime();

        // Get the maximum heap size (in bytes)
        long maxMemory = runtime.maxMemory();
        // Get the initial heap size (in bytes)
        long initialMemory = runtime.totalMemory();
        // Get the current available memory (in bytes)
        long freeMemory = runtime.freeMemory();

        // Convert to MB for better readability
        System.out.println("Initial Heap Size: " + (initialMemory / (1024 * 1024)) + " MB");
        System.out.println("Maximum Heap Size: " + (maxMemory / (1024 * 1024)) + " MB");
        System.out.println("Free Memory: " + (freeMemory / (1024 * 1024)) + " MB");
    }

    public void checkMemoryNow() {
        // Optionally log the memory usage using MemoryMXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        
        System.out.println("Used Heap Memory: " + (heapMemoryUsage.getUsed() / (1024 * 1024)) + " MB");
        System.out.println("Committed Heap Memory: " + (heapMemoryUsage.getCommitted() / (1024 * 1024)) + " MB");
    }

    public <T> Map<String, T> elasticMultiSend(@NotNull List<MultipleRequests> requests) throws IOException {
        try {
            MultiSearchRequest multiRequests = new MultiSearchRequest();
            requests.forEach(r->multiRequests.add(r.getRequest()));

            MultiSearchResponse response = restHighLevelClient.msearch(multiRequests, RequestOptions.DEFAULT);
            return getMultiResponse(response.getResponses(), requests);
        }
        catch (IOException | OpenSearchException e) {
            logger.error(e.toString());
            throw new IOException(e.toString());
        }
    }

    private <T> Map<String, T> getMultiResponse(MultiSearchResponse.Item[] response, List<MultipleRequests> requests) {
        Map<String, T> result = new HashMap<>();
        final int[] index = {0};
        List.of(response).forEach(item->{
            MultipleRequests req = requests.get(index[0]);
            result.put(req.getName(), (T) req.getTypeMapper().get(item.getResponse()));
            index[0] += 1;
        });
        return result;
    }

    public JsonObject getJSonFromResponse(Response response) throws IOException {
        String responseBody = EntityUtils.toString(response.getEntity());
        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
        return jsonObject;
    }

    // This function build queries with following rules:
    //  - If a list is empty, query will return empty dataset
    //  - If a list has only one element which is empty string, query will return all data available
    //  - If a list is null, query will return all data available
    public Map<String, Object> buildListQuery(Map<String, Object> params, Set<String> excludedParams) {
        return buildListQuery(params, excludedParams, false);
    }

    public Map<String, Object> buildListQuery() {
        return buildListQuery(Map.of(), Set.of(), true);
    }

    public Map<String, Object> buildListQuery(Map<String, Object> params, Set<String> excludedParams, boolean ignoreCase) {
        Map<String, Object> result = new HashMap<>();

        List<Object> filter = new ArrayList<>();
        for (var key: params.keySet()) {
            if (excludedParams.contains(key)) {
                continue;
            }
            List<String> valueSet = formatParameter(params.get(key));
            if (ignoreCase) {
                List<String> lowerCaseValueSet = new ArrayList<>();
                for (String value: valueSet) {
                    lowerCaseValueSet.add(value.toLowerCase());
                }
                valueSet = lowerCaseValueSet;
            }
            // list with only one empty string [""] means return all records
            if (valueSet.size() == 1) {
                if (valueSet.get(0).equals("")) {
                    continue;
                }
            }
            filter.add(Map.of(
                "terms", Map.of( key, valueSet)
            ));
        }

        result.put("query", Map.of("bool", Map.of("filter", filter)));
        return result;
    }

    public Map<String, Object> buildFacetFilterQuery(Map<String, Object> params) throws IOException {
        return buildFacetFilterQuery(params, Set.of());
    }

    public Map<String, Object> buildFacetFilterQuery(Map<String, Object> params, Set<String> rangeParams)  throws IOException {
        return buildFacetFilterQuery(params, rangeParams, Set.of());
    }

    public Map<String, Object> buildFacetFilterQuery(Map<String, Object> params, Set<String> rangeParams, Set<String> excludedParams) throws IOException {
        Map<String, Object> result = new HashMap<>();

        List<Object> filter = new ArrayList<>();
        for (var key: params.keySet()) {
            if (excludedParams.contains(key)) {
                continue;
            }

            if (rangeParams.contains(key)) {
                // Range parameters, should contain two doubles, first lower bound, then upper bound
                // Any other values after those two will be ignored
                List<Double> bounds = (List<Double>) params.get(key);
                if (bounds.size() >= 2) {
                    Double lower = bounds.get(0);
                    Double higher = bounds.get(1);
                    if (lower == null && higher == null) {
                        throw new IOException("Lower bound and Upper bound can't be both null!");
                    }
                    Map<String, Double> range = new HashMap<>();
                    if (lower != null) {
                        range.put("gte", lower);
                    }
                    if (higher != null) {
                        range.put("lte", higher);
                    }
                    filter.add(Map.of(
                            "range", Map.of( key, range)
                    ));
                }
            } else {
                // Term parameters (default)
                List<String> valueSet = formatParameter(params.get(key));
                if (valueSet.size() > 0) {
                    filter.add(Map.of(
                            "terms", Map.of( key, valueSet)
                    ));
                }
            }
        }

        result.put("query", Map.of("bool", Map.of("filter", filter)));
        return result;
    }

    public Map<String, Object> addAggregations(Map<String, Object> query, String[] termAggNames) {
        return addAggregations(query, termAggNames, new String[]{});
    }

    public Map<String, Object> addAggregations(Map<String, Object> query, String[] termAggNames, String[] rangeAggNames) {
        Map<String, Object> newQuery = new HashMap<>(query);
        newQuery.put("size", 0);
        newQuery.put("aggregations", getAllAggregations(termAggNames, rangeAggNames));
        return newQuery;
    }

    public void addSubAggregations(Map<String, Object> query, String mainAggName, String[] subTermAggNames) {
        addSubAggregations(query, mainAggName, subTermAggNames, new String[]{});
    }

    public void addSubAggregations(Map<String, Object> query, String mainAggName, String[] subTermAggNames, String[] subRangeAggNames) {
        Map<String, Object> mainAgg = (Map<String, Object>) ((Map<String, Object>) query.get("aggregations")).get(mainAggName);
        Map<String, Object> subAggs = getAllAggregations(subTermAggNames, subRangeAggNames);
        mainAgg.put("aggregations", subAggs);
    }

    private Map<String, Object> getAllAggregations(String[]  termAggNames, String[] rangeAggNames) {
        Map<String, Object> aggs = new HashMap<>();
        for (String aggName: termAggNames) {
            // Terms
            aggs.put(aggName, getTermAggregation(aggName));
        }

        for (String aggName: rangeAggNames) {
            // Range
            aggs.put(aggName, getRangeAggregation(aggName));
        }
        return aggs;
    }

    private Map<String, Object> getTermAggregation(String aggName) {
        Map<String, Object> agg = new HashMap<>();
        agg.put("terms", Map.of("field", aggName, "size", MAX_ES_SIZE));
        return agg;
    }

    private Map<String, Object> getRangeAggregation(String aggName) {
        Map<String, Object> agg = new HashMap<>();
        agg.put("stats", Map.of("field", aggName));
        return agg;
    }

    public Map<String, JsonArray> collectTermAggs(JsonObject jsonObject, String[] termAggNames) {
        Map<String, JsonArray> data = new HashMap<>();
        JsonObject aggs = jsonObject.getAsJsonObject("aggregations");
        for (String aggName: termAggNames) {
            // Terms buckets
            data.put(aggName, aggs.getAsJsonObject(aggName).getAsJsonArray("buckets"));
        }
        return data;
    }

    public List<String> collectTerms(JsonObject jsonObject, String aggName) {
        List<String> data = new ArrayList<>();
        JsonObject aggs = jsonObject.getAsJsonObject("aggregations");
        JsonArray buckets = aggs.getAsJsonObject(aggName).getAsJsonArray("buckets");
        for (var bucket: buckets) {
            data.add(bucket.getAsJsonObject().get("key").getAsString());
        }
        return data;
    }

    public Map<String, JsonObject> collectRangeAggs(JsonObject jsonObject, String[] rangeAggNames) {
        Map<String, JsonObject> data = new HashMap<>();
        JsonObject aggs = jsonObject.getAsJsonObject("aggregations");
        for (String aggName: rangeAggNames) {
            // Range/stats
            data.put(aggName, aggs.getAsJsonObject(aggName));
        }
        return data;
    }

    public List<String> collectBucketKeys(JsonArray buckets) {
        List<String> keys = new ArrayList<>();
        for (var bucket: buckets) {
            keys.add(bucket.getAsJsonObject().get("key").getAsString());
        }
        return keys;
    }

    public List<String> collectField(Request request, String fieldName) throws IOException {
        List<String> results = new ArrayList<>();

        request.addParameter("scroll", "1m");
        JsonObject jsonObject = send(request);
        JsonArray searchHits = jsonObject.getAsJsonObject("hits").getAsJsonArray("hits");

        while (searchHits != null && searchHits.size() > 0) {
            logger.info("Current " + fieldName + " records: " + results.size() + " collecting...");
            for (int i = 0; i < searchHits.size(); i++) {
                String value = searchHits.get(i).getAsJsonObject().get("_source").getAsJsonObject().get(fieldName).getAsString();
                results.add(value);
            }

            Request scrollRequest = new Request("POST", SCROLL_ENDPOINT);
            String scrollId = jsonObject.get("_scroll_id").getAsString();
            Map<String, Object> scrollQuery = Map.of(
                    "scroll", "1m",
                    "scroll_id", scrollId
            );
            scrollRequest.setJsonEntity(gson.toJson(scrollQuery));
            jsonObject = send(scrollRequest);
            searchHits = jsonObject.getAsJsonObject("hits").getAsJsonArray("hits");
        }

        String scrollId = jsonObject.get("_scroll_id").getAsString();
        Request clearScrollRequest = new Request("DELETE", SCROLL_ENDPOINT);
        clearScrollRequest.setJsonEntity("{\"scroll_id\":\"" + scrollId +"\"}");
        send(clearScrollRequest);

        return results;
    }

    public int getTotalHits(JsonObject jsonObject) {
        return jsonObject.get("hits").getAsJsonObject().get("total").getAsJsonObject().get("value").getAsInt();
    }

    public List<Map<String, Object>> collectPage(Map<String, Object> params, String endpoint, String[][] properties) throws IOException {
        Map<String, Object> query = buildListQuery(params, Set.of(),false);
        Request request = new Request("GET", endpoint);
        return collectPage(request, query, properties);
    }

    public List<Map<String, Object>> collectPage(Request request, Map<String, Object> query, String[][] properties) throws IOException {
        return collectPage(request, query, properties, ESService.MAX_ES_SIZE, 0);
    }

    public List<Map<String, Object>> collectPage(Request request, Map<String, Object> query, String[][] properties, int pageSize, int offset) throws IOException {
        // Make sure page size is less than max allowed size
        if (pageSize > MAX_ES_SIZE) {
            throw new IOException("Parameter 'first' must not exceeded " + MAX_ES_SIZE);
        }

        // Check whether to use scroll
        if (pageSize + offset > SCROLL_THRESHOLD) {
            return collectPageWithScroll(request, query, properties, pageSize, offset);
        }

        // data within limit can use just from/size
        query.put("size", pageSize);
        query.put("from", offset);
        String queryJson = gson.toJson(query);
        request.setJsonEntity(queryJson);

        JsonObject jsonObject = send(request);
        return collectPage(jsonObject, properties, pageSize);
    }

    /**
     * Uses scroll to obtain results
     * @param request The Opensearch request
     * @param query The query to be sent in the body of the Opensearch request
     * @param properties The Opensearch properties to retrieve
     * @param pageSize The desired number of results to obtain
     * @param offset The desired offset of the results
     * @return
     * @throws IOException
     */
    private List<Map<String, Object>> collectPageWithScroll(
            Request request, Map<String, Object> query, String[][] properties, int pageSize, int offset) throws IOException {
        query.put("size", SCROLL_SIZE);
        String jsonizedQuery = gson.toJson(query);
        request.setJsonEntity(jsonizedQuery);
        request.addParameter("scroll", "1m");
        // JsonObject page = rollToPage(request, pageSize, offset);
        // return collectPage(page, properties, pageSize, offset % SCROLL_SIZE);
        JsonArray page = rollToPage(request, pageSize, offset);
        checkMemoryInit();
        return collectScrollPage(page, properties, pageSize, offset % SCROLL_SIZE);
    }

    /**
     * Sends consecutive scroll requests to get the desired number of records
     * @param request The Opensearch request
     * @param pageSize How many records to obtain
     * @param offset How many records to skip
     * @return
     * @throws IOException
     */
    private JsonArray rollToPage(Request request, int pageSize, int offset) throws IOException {
        // Variables involved with the return object
        JsonArray allHits = new JsonArray(); // All the hits gathered so far
        // JsonObject outerHits = new JsonObject(); // Helper JSON object for the results
        // JsonObject results = new JsonObject(); // The results to return

        // Variables used for scrolling
        Request clearScrollRequest = new Request("DELETE", SCROLL_ENDPOINT);
        int numCumulativeHits = 0; // Number of hits gathered so far
        String scrollId = null;
        Request scrollRequest = request;

        // Send scroll requests
        while (numCumulativeHits < pageSize + offset) {
            logger.info("Current records: " + numCumulativeHits + ". Collecting more records...");

            // Execute the scroll request
            JsonObject scrollResults = send(scrollRequest);
            JsonArray searchHits = scrollResults.getAsJsonObject("hits").getAsJsonArray("hits");
            int numScrollHits = searchHits.size();
            numCumulativeHits += numScrollHits;
            scrollId = scrollResults.get("_scroll_id").getAsString();

            logger.info("...collected " + numScrollHits + " records. Current records: " + numCumulativeHits);

            // Stop scrolling if there are no records left
            if (numScrollHits <= 0) {
                break;
            }

            // Only add the hits if we've reached the scroll window of the desired results
            if (numCumulativeHits > offset) {
                allHits.addAll(searchHits);
                System.out.println("added " + numCumulativeHits + " records. ");
            }

            // Form the next scroll request
            scrollRequest = new Request("POST", SCROLL_ENDPOINT);
            Map<String, Object> scrollQuery = Map.of(
                    "scroll", "1m",
                    "scroll_id", scrollId
            );
            String scrollQueryJson = gson.toJson(scrollQuery);
            scrollRequest.setJsonEntity(scrollQueryJson);
        }

        // Close the scroll context
        if (scrollId != null) {
            clearScrollRequest.setJsonEntity("{\"scroll_id\":\"" + scrollId +"\"}");
            send(clearScrollRequest);
        }

        // Format the return object
        // outerHits.add("hits", allHits);
        // results.add("hits", outerHits);
        System.out.println("total added: " + numCumulativeHits + " records. ");
        return allHits;
    }

    // Collect a page of data, result will be of pageSize or less if not enough data remains
    public List<Map<String, Object>> collectPage(JsonObject jsonObject, String[][] properties, int pageSize) throws IOException {
        return collectPage(jsonObject, properties, pageSize, 0);
    }

    private List<Map<String, Object>> collectPage(JsonObject jsonObject, String[][] properties, int pageSize, int offset) throws IOException {
        return collectPage(jsonObject, properties, null, pageSize, offset);
    }

    public List<Map<String, Object>> collectPage(JsonObject jsonObject, String[][] properties, String[][] highlights, int pageSize, int offset) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        JsonArray searchHits = jsonObject.getAsJsonObject("hits").getAsJsonArray("hits");
        for (int i = 0; i < searchHits.size(); i++) {
            // skip offset number of documents
            if (i + 1 <= offset) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            for (String[] prop: properties) {
                String propName = prop[0];
                String dataField = prop[1];
                JsonElement element = searchHits.get(i).getAsJsonObject().get("_source").getAsJsonObject().get(dataField);
                row.put(propName, getValue(element));
            }
            if (highlights != null) {
                for (String[] highlight: highlights) {
                    String hlName = highlight[0];
                    String hlField = highlight[1];
                    JsonElement element = searchHits.get(i).getAsJsonObject().get("highlight").getAsJsonObject().get(hlField);
                    if (element != null) {
                        row.put(hlName, ((List<String>)getValue(element)).get(0));
                    }
                }
            }
            data.add(row);
            System.out.println("total hashmap size: " + data.size() + " rows. ");
            if (data.size() >= pageSize) {
                break;
            }
        }
        return data;
    }

    public List<Map<String, Object>> collectScrollPage(JsonArray searchHits, String[][] properties, int pageSize, int offset) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        System.out.println("total processing size: " + searchHits.size() + " rows. ");
        //JsonArray searchHits = jsonObject.getAsJsonObject("hits").getAsJsonArray("hits");
        for (int i = 0; i < searchHits.size(); i++) {
            // skip offset number of documents
            if (i + 1 <= offset) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            for (String[] prop: properties) {
                String propName = prop[0];
                String dataField = prop[1];
                JsonElement element = searchHits.get(i).getAsJsonObject().get("_source").getAsJsonObject().get(dataField);
                row.put(propName, getValue(element));
            }
            data.add(row);
            if (data.size() % 1000 == 0) {
                System.out.println("total hashmap size: " + data.size() + " rows. ");
                checkMemoryNow();
            }
            if (data.size() >= pageSize) {
                break;
            }
        }
        return data;
    }

    public List<Map<String, Object>> getFilteredGroupCount(Map<String, Object> params, String endpoint, String aggregationField) throws IOException {
        return getFilteredGroupCount(params, endpoint, new String[]{aggregationField});
    }

    public List<Map<String, Object>> getFilteredGroupCount(Map<String, Object> params, String endpoint, String[] aggregationFields) throws IOException {
        Map<String, Object> query = buildFacetFilterQuery(params);
        query = addAggregations(query, aggregationFields);
        Request request = new Request("GET", endpoint);
        request.setJsonEntity(gson.toJson(query));
        JsonObject result = send(request);
        List<Map<String, Object>> groupCounts = new ArrayList<>();
        JsonArray buckets = result
                .getAsJsonObject("aggregations")
                .getAsJsonObject("diagnoses")
                .getAsJsonArray("buckets");
        for (JsonElement element: buckets){
            JsonObject groupCount = element.getAsJsonObject();
            String group = groupCount.get("key").getAsString();
            groupCounts.add(
                    Map.of(
                            "group", group,
                            "subjects", groupCount.get("doc_count").getAsInt()
                    )
            );
        }
        return groupCounts;
    }

    // Convert JsonElement into Java collections and primitives
    private Object getValue(JsonElement element) {
        Object value = null;
        if (element == null || element.isJsonNull()) {
            return null;
        } else if (element.isJsonObject()) {
            value = new HashMap<String, Object>();
            JsonObject object = element.getAsJsonObject();
            for (String key: object.keySet()) {
                ((Map<String, Object>) value).put(key, getValue(object.get(key)));
            }
        } else if (element.isJsonArray()) {
            value = new ArrayList<>();
            for (JsonElement entry: element.getAsJsonArray()) {
                ((List<Object>)value).add(getValue(entry));
            }
        } else {
            value = element.getAsString();
        }
        return value;
    }

    private List<String> formatParameter(Object param){
        if (param instanceof List) {
            return (List<String>) param;
        } else {
            String value = (String)param;
            return List.of(value);
        }
    }
}