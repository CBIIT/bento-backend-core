package gov.nih.nci.bento.controller;


import gov.nih.nci.bento.model.ConfigurationDAO;
import gov.nih.nci.bento.service.ESService;
import graphql.GraphQL;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.index.Index;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;


@Controller
public class IndexController {

	private static final Logger logger = LogManager.getLogger(IndexController.class);

	private final ConfigurationDAO config;
	private final ESService esService;

	public IndexController(ConfigurationDAO config, ESService esService){
		this.config = config;
		this.esService = esService;
	}
	
	@RequestMapping(value = "/", produces = "text/html")
    public ModelAndView errorHtml(HttpServletRequest request) {
		
        return new ModelAndView("/index");
    }

	@CrossOrigin
	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	@ResponseBody
	public String ping(HttpServletRequest request, HttpServletResponse response) {
		HttpStatus responseStatus = HttpStatus.OK;
		String responseBody = "pong";
		if (config.isEsFilterEnabled()){
			if (esService.ping()){
				logger.info("OpenSearch ping successful");
			}
			else{
				responseBody = "OpenSearch ping unsuccessful";
				responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
				logger.error(responseBody);
			}
		}
		else{
			logger.info("Ping successful");
		}
		response.setStatus(responseStatus.value());
		return responseBody;
	}
}
