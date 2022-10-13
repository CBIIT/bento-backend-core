package gov.nih.nci.bento.error;

import lombok.Getter;
import java.util.List;

@Getter
public class BentoGraphQLException extends Exception{
    BentoGraphqlError bentoGraphqlError;

    public BentoGraphQLException(BentoGraphqlError bentoGraphqlError){
        this.bentoGraphqlError = bentoGraphqlError;
    }

    public BentoGraphQLException(List<String> errors){
        this.bentoGraphqlError = new BentoGraphqlError(errors);
    }
}
