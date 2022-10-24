package gov.nih.nci.bento.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.nih.nci.bento.model.search.query.QueryParam;
import gov.nih.nci.bento.service.ESService;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;

import java.io.IOException;

public abstract class AbstractESDataFetcher {
    // parameters used in queries
    protected final String PROGRAMS_END_POINT = "/programs/_search";
    protected final String STUDIES_END_POINT = "/studies/_search";
    protected final String SUBJECTS_END_POINT = "/subjects/_search";
    protected final String SUBJECT_IDS_END_POINT = "/subject_ids/_search";
    protected final String SAMPLES_END_POINT = "/samples/_search";
    protected final String FILES_END_POINT = "/files/_search";
    protected final Gson gson;
    protected final ESService esService;

    public AbstractESDataFetcher(ESService esService){
        this.esService = esService;
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    // abstract methods
    public abstract RuntimeWiring buildRuntimeWiring() throws IOException;

    protected QueryParam createQueryParam(DataFetchingEnvironment env) {
        return QueryParam.builder()
                .args(env.getArguments())
                .outputType(env.getFieldType())
                .build();
    }
}
