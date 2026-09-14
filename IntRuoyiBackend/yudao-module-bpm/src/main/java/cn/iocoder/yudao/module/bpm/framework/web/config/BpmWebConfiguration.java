package cn.iocoder.yudao.module.bpm.framework.web.config;

import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormTemplateJimuReportSaveSyncFilter;
import cn.iocoder.yudao.module.bpm.framework.web.core.FlowableWebFilter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bpm 模块的 web 组件的 Configuration
 *
 * @author 瑛泰源码
 */
@Configuration(proxyBeanMethods = false)
public class BpmWebConfiguration {

    /**
     * bpm 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi bpmGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("bpm");
    }

    /**
     * 配置 Flowable Web 过滤器
     */
    @Bean
    public FilterRegistrationBean<FlowableWebFilter> flowableWebFilter() {
        FilterRegistrationBean<FlowableWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new FlowableWebFilter());
        registrationBean.setOrder(WebFilterOrderEnum.FLOWABLE_FILTER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<FormTemplateJimuReportSaveSyncFilter> formTemplateJimuReportSaveSyncFilter(
            FormCenterRuntimeService formCenterRuntimeService) {
        FilterRegistrationBean<FormTemplateJimuReportSaveSyncFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new FormTemplateJimuReportSaveSyncFilter(formCenterRuntimeService));
        registrationBean.setOrder(WebFilterOrderEnum.FLOWABLE_FILTER + 1);
        registrationBean.addUrlPatterns("/jmreport/save");
        return registrationBean;
    }

}
