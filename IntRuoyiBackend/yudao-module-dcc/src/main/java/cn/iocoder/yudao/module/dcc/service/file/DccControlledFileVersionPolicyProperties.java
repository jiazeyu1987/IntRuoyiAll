package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.dcc.controlled-file.version-policy")
public class DccControlledFileVersionPolicyProperties {

    /**
     * Leading slash-separated segment count that forms the major-version identity.
     *
     * <p>Default {@code 1}: {@code A/1 -> A/2} is minor, {@code A/1 -> B/1} is major.
     * Set {@code 2}: {@code A/1/1 -> A/1/2} is minor, {@code A/1 -> B/1} identifies
     * different major versions.</p>
     */
    private Integer majorIdentitySegmentCount = 1;
}
