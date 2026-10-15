package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

/** Deployment-owned exact transitions. An empty registry grants no cross-key preparation. */
@Data
@Component
@ConfigurationProperties(prefix="dcc.application-execution")
public class DccExecutionDefinitionTransitions {
    private List<Transition> definitionTransitions=List.of();
    public record Transition(String sourceProcessDefinitionId,String sourceDefinitionDigest,
                             String targetProcessDefinitionId,String targetDefinitionDigest) { }
}
