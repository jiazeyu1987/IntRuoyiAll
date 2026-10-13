package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
import cn.iocoder.yudao.server.controller.admin.profileworkbench.ProfileWorkbenchTodoController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WorkbenchHttpSerializationTest {
 static ObjectMapper productionMapper() {
  var configuration=new YudaoJacksonAutoConfiguration();
  var builder=Jackson2ObjectMapperBuilder.json().modulesToInstall(configuration.timestampSupportModuleBean());
  configuration.ldtEpochMillisCustomizer().customize(builder);
  return builder.build();
 }
 @Test void productionJacksonModuleAndBuilderSerializeActualHttpResponsesWithIsoDates() throws Exception {
  var mapper=productionMapper();
  var time=LocalDateTime.of(2026,10,9,1,2,3,123456000);
  var readTime=time.withNano(123456789); // HTTP read clock retains nanos; SQL row dates remain DATETIME(6).
  // Prove this fixture installed the production global epoch serializer rather than a default mapper.
  assertTrue(mapper.readTree(mapper.writeValueAsString(time)).isNumber());
  var service=mock(ProfileWorkbenchTodoQueryService.class);
  var controller=new ProfileWorkbenchTodoController();ReflectionTestUtils.setField(controller,"service",service);ReflectionTestUtils.setField(controller,"mapper",mapper);
  var mvc=standaloneSetup(controller).setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
  var row=new TaskSummary();row.setCreatedAt(time);row.setDueAt(null);row.setBusinessId("9223372036854775807");
  var spec=new WorkbenchQuerySpec("visible",null,null,null);
  when(service.context(any())).thenReturn(new WorkbenchContextResponse(3,"revision",List.of(),"epoch","1","token",Instant.parse("2026-10-09T01:02:03.123456Z"),spec));
  when(service.page("token","epoch",1,10)).thenReturn(new ProfileWorkbenchTodoPageRespVO(3,"revision","token","epoch","1",List.of(row),1,9_007_199_254_740_991L,0,1,10,readTime));
  when(service.count("token","epoch")).thenReturn(new ProfileWorkbenchTodoCountRespVO(3,"revision","token","epoch","1",1,9_007_199_254_740_991L,0,readTime));
  var context=mapper.readTree(mvc.perform(post("/workbench/context").contentType("application/json").content("{\"registryRevision\":\"revision\",\"sources\":[],\"contextEpoch\":\"epoch\",\"querySpec\":{\"visibility\":\"visible\"}}")).andReturn().getResponse().getContentAsString());
  assertEquals("2026-10-09T01:02:03.123456Z",context.at("/data/expiresAt").textValue());
  var page=mapper.readTree(mvc.perform(get("/workbench/tasks/page").param("queryVersion","token").param("contextEpoch","epoch").param("pageNo","1").param("pageSize","10")).andReturn().getResponse().getContentAsString());
  assertEquals("2026-10-09T01:02:03.123456789",page.at("/data/readAt").textValue());
  assertEquals("2026-10-09T01:02:03.123456",page.at("/data/rows/0/createdAt").textValue());
  assertTrue(page.at("/data/rows/0/dueAt").isNull());assertEquals("9223372036854775807",page.at("/data/rows/0/businessId").textValue());
  // Production serializer treats MAX_SAFE itself as a string even for primitive long records.
  assertTrue(page.at("/data/businessTotal").isTextual());assertEquals("9007199254740991",page.at("/data/businessTotal").textValue());
  var count=mapper.readTree(mvc.perform(get("/workbench/tasks/count").param("queryVersion","token").param("contextEpoch","epoch")).andReturn().getResponse().getContentAsString());
  assertEquals("2026-10-09T01:02:03.123456789",count.at("/data/readAt").textValue());
  assertTrue(count.at("/data/businessTotal").isTextual());assertEquals("9007199254740991",count.at("/data/businessTotal").textValue());
  when(service.count("token","epoch")).thenReturn(new ProfileWorkbenchTodoCountRespVO(3,"revision","token","epoch","1",1,9_007_199_254_740_990L,0,readTime));
  var lastNumber=mapper.readTree(mvc.perform(get("/workbench/tasks/count").param("queryVersion","token").param("contextEpoch","epoch")).andReturn().getResponse().getContentAsString());
  assertTrue(lastNumber.at("/data/businessTotal").isIntegralNumber());assertEquals(9_007_199_254_740_990L,lastNumber.at("/data/businessTotal").longValue());
 }
 @Test void bothNullableRowDatesPreserveNullAndMicroseconds() throws Exception {
  var mapper=productionMapper();var row=new TaskSummary();row.setDueAt(LocalDateTime.of(2026,10,9,0,0,0,1000));
  var json=mapper.readTree(mapper.writeValueAsString(row));assertTrue(json.get("createdAt").isNull());assertEquals("2026-10-09T00:00:00.000001",json.get("dueAt").textValue());
 }
}
