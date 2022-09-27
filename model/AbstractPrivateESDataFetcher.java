package gov.nih.nci.bento.model;

import gov.nih.nci.bento.service.ESService;

public abstract class AbstractPrivateESDataFetcher extends AbstractESDataFetcher{
    public AbstractPrivateESDataFetcher(ESService esService) {
        super(esService);
    }
}
