package gov.nih.nci.bento.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LogManager.getLogger(CustomErrorController.class);

    @RequestMapping
    public ModelAndView handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        try {
            int statusCode = status instanceof Integer 
                ? (Integer) status 
                : Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return new ModelAndView("errors/errorPage404");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return new ModelAndView("errors/errorPage500");
            }
        } catch (NumberFormatException ex) {
            logger.warn("Could not parse error status code: {}", ex.getMessage());
        }
        return new ModelAndView("errors/generalErrorPage");
    }
}
