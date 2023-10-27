package gov.nih.nci.bento.graphql;


import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.VariableReference;
import graphql.validation.ValidationError;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
class PageSizeLimitInstrumentation extends SimpleInstrumentation {



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
                    if (variables.containsKey(alias) && (Double) variables.get(alias) > 2500){
                        result.add(buildValidationError(String.format("The page size variable '%s' exceeds the maximum limit for this API", alias)));
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

        parameters.getDocument().getDefinitions().forEach(definition -> {
            OperationDefinition operationDefinition = (OperationDefinition) definition;
            operationDefinition.getSelectionSet().getSelections().forEach((selection) -> {
                Field field = (Field) selection;
                field.getArguments().forEach((arg) -> {
                    try{
                        if (arg.getName().equals("first")) {
                            VariableReference variableReference = (VariableReference) arg.getValue();
                            firstAliases.add(variableReference.getName());
                        }
                    }
                    catch (ClassCastException e){
                        // no action needed
                    }
                });
            });
        });
        return firstAliases;
    }
}