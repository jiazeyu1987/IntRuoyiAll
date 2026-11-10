package cn.iocoder.yudao.module.mes.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mes 模块的 web 组件的 Configuration
 *
 * @author 瑛泰源码
 */
@Configuration(proxyBeanMethods = false)
public class MesWebConfiguration {

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<cn.iocoder.yudao.module.mes.framework.web.core.filter.EdhrDecisionRequestLogMetadataFilter> edhrDecisionRequestLogMetadataFilter(
            cn.iocoder.yudao.framework.web.config.WebProperties properties,
            org.springframework.beans.factory.ObjectProvider<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrReleaseController> releases,
            org.springframework.beans.factory.ObjectProvider<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrNonconformanceReviewController> reviews,
            org.springframework.beans.factory.ObjectProvider<cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.MesFrontlineDeviceAccountController> frontline) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(
                new cn.iocoder.yudao.module.mes.framework.web.core.filter.EdhrDecisionRequestLogMetadataFilter(properties, releases, reviews, frontline));
        registration.setOrder(cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum.API_ACCESS_LOG_FILTER - 2);
        return registration;
    }


    /**
     * mes 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi mesGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("mes");
    }

}
