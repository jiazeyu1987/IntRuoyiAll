package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.server.controller.admin.profileworkbench.ProfileWorkbenchTodoController;
import cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.*;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.handler.*;
import cn.iocoder.yudao.framework.web.core.filter.CacheRequestBodyFilter;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.framework.security.core.handler.AuthenticationEntryPointImpl;
import cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
@SpringJUnitConfig(WorkbenchRequestPrivacyTest.Config.class) @ActiveProfiles("local")
class WorkbenchRequestPrivacyTest {
 @Configuration static class Config {@Bean SpringUtils springUtils(){return new SpringUtils();}}
 static final String SECRET="opaque-queryVersion-confidential-fixture";
 ApiAccessLogCommonApi access;ApiErrorLogCommonApi errors;ProfileWorkbenchTodoQueryService service;WorkbenchRequestLogMetadataFilter metadata;GlobalExceptionHandler handler;MockMvc mvc;
 WebProperties properties;ListAppender<ILoggingEvent> events;List<Logger> loggers;List<Level> levels;
 @BeforeEach void setup(){
  access=mock(ApiAccessLogCommonApi.class);errors=mock(ApiErrorLogCommonApi.class);service=mock(ProfileWorkbenchTodoQueryService.class);properties=new WebProperties();new WebFrameworkUtils(properties);
  var controller=new ProfileWorkbenchTodoController();ReflectionTestUtils.setField(controller,"service",service);ReflectionTestUtils.setField(controller,"mapper",new ObjectMapper().findAndRegisterModules());
  var factory=new DefaultListableBeanFactory();factory.registerSingleton("controller",controller);metadata=new WorkbenchRequestLogMetadataFilter(properties,factory.getBeanProvider(ProfileWorkbenchTodoController.class));handler=new GlobalExceptionHandler("workbench-privacy",errors);
  mvc=standaloneSetup(controller).setCustomHandlerMapping(()->{var mapping=new RequestMappingHandlerMapping();mapping.setPathPrefixes(Map.of("/admin-api",type->true));return mapping;})
   .setControllerAdvice(handler,new GlobalResponseBodyHandler()).addInterceptors(new ApiAccessLogInterceptor()).addFilters(new CacheRequestBodyFilter(),metadata,new ApiAccessLogFilter(properties,"workbench-privacy",access)).build();
  events=new ListAppender<>();events.start();loggers=new ArrayList<>();levels=new ArrayList<>();for(Class<?> type:List.of(ApiAccessLogFilter.class,ApiAccessLogInterceptor.class,GlobalExceptionHandler.class,AuthenticationEntryPointImpl.class)){var logger=(Logger)LoggerFactory.getLogger(type);loggers.add(logger);levels.add(logger.getLevel());logger.setLevel(Level.DEBUG);logger.addAppender(events);}
 }
 @AfterEach void cleanup(){for(int i=0;i<loggers.size();i++){loggers.get(i).detachAppender(events);loggers.get(i).setLevel(levels.get(i));}events.stop();}
 void privateLogs(){
  var capture=ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);verify(access).createApiAccessLogAsync(capture.capture());assertNull(capture.getValue().getRequestParams());assertFalse(JsonUtils.toJsonString(capture.getValue()).contains(SECRET));
  for(var event:events.list){assertFalse(event.getFormattedMessage().contains(SECRET));assertNull(event.getThrowableProxy());}
 }
 @Test void actualMvcSuccessTypedFailureWrongMethodAndMalformedBodyKeepPayloadPrivate()throws Exception{
  mvc.perform(get("/admin-api/workbench/tasks/page").param("queryVersion",SECRET).param("contextEpoch","epoch").param("pageNo","1").param("pageSize","10")).andExpect(jsonPath("$.code").value(0));privateLogs();verify(service).page(SECRET,"epoch",1,10);
  reset(access);events.list.clear();when(service.count(SECRET,"epoch")).thenThrow(WorkbenchFailure.QUERY_TOKEN_INVALID.exception());
  mvc.perform(get("/admin-api/workbench/tasks/count").param("queryVersion",SECRET).param("contextEpoch","epoch")).andExpect(jsonPath("$.code").value(WorkbenchFailure.QUERY_TOKEN_INVALID.code()));privateLogs();
  reset(access);events.list.clear();mvc.perform(patch("/admin-api/workbench/tasks/count").param("queryVersion",SECRET)).andExpect(jsonPath("$.code").value(405));privateLogs();
  reset(access);events.list.clear();mvc.perform(post("/admin-api/workbench/tasks/visibility").contentType("application/json").content("{\"queryVersion\":\""+SECRET+"\"," )).andExpect(jsonPath("$.code").value(400));privateLogs();
 }
 @Test void everyProtectedPathAndMethodHasEarlyMetadataBeforeActualAuthenticationRejection()throws Exception{
  for(String path:List.of("/context","/tasks/page","/tasks/count","/tasks/visibility"))for(String method:List.of("GET","POST","PATCH")){
   reset(access);events.list.clear();var request=new MockHttpServletRequest(method,"/admin-api/workbench"+path+";queryVersion="+SECRET);request.setServletPath(request.getRequestURI());request.addParameter("queryVersion",SECRET);request.setContentType("application/json");request.setContent(("{\"queryVersion\":\""+SECRET+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));var response=new MockHttpServletResponse();
   metadata.doFilter(request,response,(r,s)->new ApiAccessLogFilter(properties,"workbench-privacy",access).doFilter(r,s,(a,b)->new AuthenticationEntryPointImpl().commence((jakarta.servlet.http.HttpServletRequest)a,(jakarta.servlet.http.HttpServletResponse)b,new InsufficientAuthenticationException(SECRET))));
   assertEquals(401,JsonUtils.parseObject(response.getContentAsString(),cn.iocoder.yudao.framework.common.pojo.CommonResult.class).getCode());assertEquals("/admin-api/workbench"+path,ApiAccessLogInterceptor.getLogRequestUri(request));privateLogs();verifyNoInteractions(service);
  }
 }
 @Test void actualErrorLogAndStackDoNotExposeTokenOnPreMvcUnknownFailure()throws Exception{
  var request=new MockHttpServletRequest("POST","/admin-api/workbench/context;queryVersion="+SECRET);request.addParameter("queryVersion",SECRET);var response=new MockHttpServletResponse();
  metadata.doFilter(request,response,(r,s)->new ApiAccessLogFilter(properties,"workbench-privacy",access).doFilter(r,s,(a,b)->{var result=handler.allExceptionHandler((jakarta.servlet.http.HttpServletRequest)a,new IllegalStateException(SECRET));assertEquals(500,result.getCode());}));privateLogs();
  var capture=ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);verify(errors).createApiErrorLogAsync(capture.capture());assertFalse(JsonUtils.toJsonString(capture.getValue()).contains(SECRET));
 }
}
