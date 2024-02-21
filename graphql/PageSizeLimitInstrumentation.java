package gov.nih.nci.bento.graphql;


import gov.nih.nci.bento.model.ConfigurationDAO;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.VariableReference;
import graphql.validation.ValidationError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class PageSizeLimitInstrumentation extends SimpleInstrumentation {

    private static final Logger logger = LogManager.getLogger(PageSizeLimitInstrumentation.class);
    private final int MAX_PAGE_SIZE;

    public PageSizeLimitInstrumentation(ConfigurationDAO config){
        this.MAX_PAGE_SIZE = config.getMaxPageSize();
    }

    @NotNull
    public InstrumentationContext<List<ValidationError>> beginValidation(InstrumentationValidationParameters parameters) {

        return new InstrumentationContext<>() {
            @Override
            public void onDispatched(CompletableFuture<List<ValidationError>> result) {
                // do nothing
            }

            @Override
            public void onCompleted(List<ValidationError> result, Throwable t) {
                Map<String, Object> variables = parameters.getExecutionInput().getVariables();
                ArrayList<String> firstAliases = getFirstVariableAliases(parameters);
                firstAliases.forEach((alias) -> {
                    try{
                        if (variables.containsKey(alias) && (Double) variables.get(alias) > MAX_PAGE_SIZE){
                            result.add(buildValidationError(String.format("The page size variable '%s' must be less than or equal to %s", alias, MAX_PAGE_SIZE)));
                            logger.warn("The API page size limit was exceeded by a query, the query was not run");
                        }
                    }
                    catch (ClassCastException e){
                        //No action required, the graphql library will handle this error and report it to the user
                    }
                });
                if (t != null){
                    result.add(buildValidationError(t.getMessage()));
                }
            }
        };
    }

    private ValidationError buildValidationError(String errorMessage){
        return ValidationError.newValidationError().description(errorMessage).build();
    }

    private ArrayList<String> getFirstVariableAliases(InstrumentationValidationParameters parameters){
        ArrayList<String> firstAliases = new ArrayList<>();
        try{
            parameters.getDocument().getDefinitions().forEach(definition -> {
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                operationDefinition.getSelectionSet().getSelections().forEach((selection) -> {
                    Field field = (Field) selection;
                    field.getArguments().forEach((arg) -> {
                        String argName = arg.getName();
                        try{
                            if (argName.equals("first")) {
                                VariableReference variableReference = (VariableReference) arg.getValue();
                                firstAliases.add(variableReference.getName());
                            }
                        }
                        catch (ClassCastException e){
                            logger.warn(String.format("A class cast exception was thrown while checking page size limits for variable %s", argName));
                        }
                    });
                });
            });
        }
        catch (Exception e){
            logger.error("An exception was thrown while checking page size limits, the limit will not be checked for this query");
            logger.error(e.getMessage());
        }
        return firstAliases;
    }
}