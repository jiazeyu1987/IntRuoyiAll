package cn.iocoder.yudao.module.showroom.framework.security.config;

import cn.iocoder.yudao.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Showroom 模块的 Security 配置
 */
@Configuration(proxyBeanMethods = false, value = "showroomSecurityConfiguration")
public class SecurityConfiguration {

    @Bean("showroomAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {

            @Override
            public void customize(
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                registry.requestMatchers("/showroom/display/website-config").permitAll();
                registry.requestMatchers("/showroom/release/**").permitAll();
                registry.requestMatchers("/showroom/assets/**").permitAll();
                registry.requestMatchers("/showroom/sites/**").permitAll();
            }

        };
    }

}
