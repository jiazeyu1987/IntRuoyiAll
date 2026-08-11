package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class DccWebConfiguration implements WebMvcConfigurer {

    private final WebProperties webProperties;

    public DccWebConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DccExplicitTenantRequestValidator(webProperties))
                .addPathPatterns(DccUploadApiContractPaths.resolve(webProperties).toArray(String[]::new));
    }

    @Bean
    public FilterRegistrationBean<DccApiHttpContractFilter> dccApiHttpContractFilter() {
        FilterRegistrationBean<DccApiHttpContractFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DccApiHttpContractFilter(webProperties));
        registrationBean.setOrder(WebFilterOrderEnum.XSS_FILTER + 1);
        registrationBean.addUrlPatterns(DccWorkflowApiContractPaths.registrationPattern(webProperties));
        return registrationBean;
    }
}
