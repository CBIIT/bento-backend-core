package gov.nih.nci.bento.service.connector;

import gov.nih.nci.bento.model.ConfigurationDAO;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;

public class DefaultClient extends AbstractClient {

    public DefaultClient(ConfigurationDAO config) {
        super(config);
    }

    @Override
    public RestHighLevelClient getElasticClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(config.getEsHost().trim(), config.getEsPort(), config.getEsScheme()))
                // Per-connection/request defaults:
                .setRequestConfigCallback((RequestConfig.Builder b) -> b
                        .setConnectTimeout(5_000)              // TCP connect timeout
                        .setConnectionRequestTimeout(5_000)    // wait for a connection from pool
                        .setSocketTimeout(120_000));           // no-bytes-read timeout

        return new RestHighLevelClient(builder);
    }

    @Override
    public RestClient getLowLevelElasticClient() {
        var lowLevelBuilder = RestClient.builder(new HttpHost(config.getEsHost().trim(), config.getEsPort(), config.getEsScheme()))
                // Per-connection/request defaults:
                .setRequestConfigCallback((RequestConfig.Builder b) -> b
                        .setConnectTimeout(5_000)              // TCP connect timeout
                        .setConnectionRequestTimeout(5_000)    // wait for a connection from pool
                        .setSocketTimeout(120_000));           // no-bytes-read timeout
        return lowLevelBuilder.build();
    }
}
