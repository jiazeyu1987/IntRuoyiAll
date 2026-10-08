package cn.iocoder.yudao.module.system.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.system.controller.admin.user.UserController;
import cn.iocoder.yudao.module.system.controller.admin.user.UserProfileController;
import cn.iocoder.yudao.module.system.framework.web.core.filter.PasswordRequestLogMetadataFilter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块的 web 组件的 Configuration
 *
 * @author 瑛泰源码
 */
@Configuration(proxyBeanMethods = false)
public class SystemWebConfiguration {

    @Bean
    public FilterRegistrationBean<PasswordRequestLogMetadataFilter> passwordRequestLogMetadataFilter(
            WebProperties properties, ObjectProvider<UserController> users,
            ObjectProvider<UserProfileController> profiles) {
        FilterRegistrationBean<PasswordRequestLogMetadataFilter> registration = new FilterRegistrationBean<>(
                new PasswordRequestLogMetadataFilter(properties, users, profiles));
        registration.setOrder(WebFilterOrderEnum.API_ACCESS_LOG_FILTER - 2);
        return registration;
    }

    /**
     * system 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi systemGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("system");
    }

}
