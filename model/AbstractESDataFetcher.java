package gov.nih.nci.bento.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.service.ESService;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import org.opensearch.client.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractESDataFetcher {
    // parameters used in queries
    protected final String PROGRAMS_END_POINT = "/programs/_search";
    protected final String PROGRAMS_COUNT_END_POINT = "/programs/_count";
    protected final String STUDIES_END_POINT = "/studies/_search";
    protected final String STUDIES_COUNT_END_POINT = "/studies/_count";
    protected final String SUBJECTS_END_POINT = "/subjects/_search";
    protected final String SUBJECTS_COUNT_END_POINT = "/subjects/_count";
    protected final String SUBJECT_IDS_END_POINT = "/subject_ids/_search";
    protected final String SAMPLES_END_POINT = "/samples/_search";
    protected final String SAMPLES_COUNT_END_POINT = "/samples/_count";
    protected final String FILES_END_POINT = "/files/_search";
    protected final String FILES_COUNT_END_POINT = "/files/_count";
    protected final String LAB_PROCEDURE_COUNT_END_POINT = "/lab_procedures/_count";
    protected final Gson gson;
    protected final ESService esService;

    public AbstractESDataFetcher(ESService esService){
        this.esService = esService;
        this.gson = new GsonBuilder().serializeNulls().create();
    }


    protected int getNodeCount(String countEndpoint) throws IOException {
        Request countRequest = new Request("GET", countEndpoint);
        JsonObject countResult = esService.send(countRequest);
        return countResult.get("count").getAsInt();
    }

    protected QueryParam createQueryParam(DataFetchingEnvironment env) {
        return QueryParam.builder()
                .args(env.getArguments())
                .outputType(env.getFieldType())
                .build();
    }
}
