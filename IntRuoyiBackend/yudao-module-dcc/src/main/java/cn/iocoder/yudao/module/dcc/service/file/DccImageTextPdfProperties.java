package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yudao.dcc.conversion")
public class DccImageTextPdfProperties {
    private Resource textFont;

    public Resource getTextFont() { return textFont; }
    public void setTextFont(Resource textFont) { this.textFont = textFont; }
}
