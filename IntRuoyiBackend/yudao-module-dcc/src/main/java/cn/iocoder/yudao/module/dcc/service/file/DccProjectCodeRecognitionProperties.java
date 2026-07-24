package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.dcc.project-code-recognition")
public class DccProjectCodeRecognitionProperties {

    private String codexCliCommand = "cmd.exe /c codex.cmd";
    private Integer timeoutSeconds = 120;
    private String version;
    private Integer workerCount = 1;
}
