package gov.nih.nci.bento.model;

import com.google.gson.JsonObject;
import gov.nih.nci.bento.service.ESService;
import org.opensearch.client.Request;

import java.io.IOException;

public abstract class AbstractPublicESDataFetcher extends AbstractESDataFetcher{
    public AbstractPublicESDataFetcher(ESService esService) {
        super(esService);
    }

    public String esVersion() throws IOException {
        Request versionRequest = new Request("GET", "/");
        JsonObject response = esService.send(versionRequest);
        return response.getAsJsonObject("version").getAsJsonPrimitive("number").getAsString();
    }
}
