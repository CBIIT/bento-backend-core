package gov.nih.nci.bento.model;

import gov.nih.nci.bento.service.ESService;

public abstract class AbstractPublicESDataFetcher extends AbstractESDataFetcher{
    public AbstractPublicESDataFetcher(ESService esService) {
        super(esService);
    }
}
