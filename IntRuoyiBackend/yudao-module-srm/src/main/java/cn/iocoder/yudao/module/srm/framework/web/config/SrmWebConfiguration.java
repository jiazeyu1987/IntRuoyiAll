package cn.iocoder.yudao.module.srm.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * srm 模块的 web 组件 Configuration。
 */
@Configuration(proxyBeanMethods = false)
public class SrmWebConfiguration {

    /**
     * srm 模块的 API 分组。
     */
    @Bean
    public GroupedOpenApi srmGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("srm");
    }

}
