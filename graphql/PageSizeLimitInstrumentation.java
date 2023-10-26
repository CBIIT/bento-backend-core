package gov.nih.nci.bento.graphql;


import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class PageSizeLimitInstrumentation extends SimpleInstrumentation {
    private static final String FIRST = "first";
    private final int maxPageSize;

    public PageSizeLimitInstrumentation(int maxPageSize){
        this.maxPageSize = maxPageSize;
    }

    @NotNull
    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters params) {
        Map<String, Object> variables = params.getExecutionContext().getExecutionInput().getVariables();
        Object input = variables.get(FIRST);
        int pageSize;
        try{
            pageSize = (Integer) input;
        }
        catch (ClassCastException|NullPointerException e){
            pageSize = maxPageSize;
            variables.put(FIRST, maxPageSize);
        }
        if (pageSize > maxPageSize){
            throw new IllegalArgumentException("Page size is too large");
        }
        return super.beginExecuteOperation(params);
    }

}
