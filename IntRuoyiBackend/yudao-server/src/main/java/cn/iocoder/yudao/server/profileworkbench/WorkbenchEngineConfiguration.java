package cn.iocoder.yudao.server.profileworkbench;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkbenchEngineConfiguration {
    @Value("${profile.workbench.query-engine:s3}") private String engine;
    @Value("${profile.workbench.commit-protocol:disabled}") private String commits;
    @PostConstruct void validate() {
        if(!"s3".equals(engine)&&!"s4".equals(engine)) throw new IllegalStateException("Unknown explicit workbench query engine");
        if(!"enabled".equals(commits)&&!"disabled".equals(commits)) throw new IllegalStateException("Unknown workbench producer protocol");
        if("s4".equals(engine)&&!"enabled".equals(commits)) throw new IllegalStateException("S4 requires reliable producer protocol");
    }
}
