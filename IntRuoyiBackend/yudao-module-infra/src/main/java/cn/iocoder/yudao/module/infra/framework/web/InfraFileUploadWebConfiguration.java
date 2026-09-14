package cn.iocoder.yudao.module.infra.framework.web;

import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InfraFileUploadWebConfiguration {

    private final WebProperties webProperties;

    public InfraFileUploadWebConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Bean
    public FilterRegistrationBean<InfraFileUploadHttpContractFilter> infraFileUploadHttpContractFilter() {
        FilterRegistrationBean<InfraFileUploadHttpContractFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new InfraFileUploadHttpContractFilter(webProperties));
        registrationBean.setOrder(WebFilterOrderEnum.XSS_FILTER + 1);
        registrationBean.addUrlPatterns(InfraFileUploadHttpContractFilter.resolveTargetPath(webProperties));
        return registrationBean;
    }
}
