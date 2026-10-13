package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.server.controller.admin.profileworkbench.ProfileWorkbenchTodoController;
import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.util.Assert;
@Configuration(proxyBeanMethods=false)
public class WorkbenchWebConfiguration {
 @Bean public FilterRegistrationBean<WorkbenchRequestLogMetadataFilter> workbenchRequestLogMetadataFilter(WebProperties properties,ObjectProvider<ProfileWorkbenchTodoController> controller){
  int order=WebFilterOrderEnum.API_ACCESS_LOG_FILTER-2;
  Assert.state(order>WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER&&order<WebFilterOrderEnum.API_ACCESS_LOG_FILTER&&order<-100,"Workbench metadata must follow body cache and precede access/security");
  var registration=new FilterRegistrationBean<>(new WorkbenchRequestLogMetadataFilter(properties,controller));registration.setOrder(order);return registration;
 }
}
