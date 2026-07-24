package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.dcc.dmr-sheet")
public class DccDmrSheetExportProperties {

    private String rootPath;
}

