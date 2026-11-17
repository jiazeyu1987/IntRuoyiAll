package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.SimpleParserPublishReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class MesSimpleParserReuseServiceTest {
    private final MesSimpleParserReuseService service = new MesSimpleParserReuseService();
    private final MesProProcessMapper processes = mock(MesProProcessMapper.class);
    private final MesProCraftMapper crafts = mock(MesProCraftMapper.class);
    private JdbcTemplate jdbc;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("create table mes_pro_route(id bigint,tenant_id bigint,deleted int)");
        jdbc.execute("create table mes_pro_route_process(route_id bigint,process_id bigint,workstation_id bigint,tenant_id bigint,deleted int)");
        jdbc.execute("create table mes_md_workstation(id bigint,workshop_id bigint,tenant_id bigint,deleted int)");
        jdbc.execute("create table mes_md_workshop(id bigint,name varchar(80),tenant_id bigint,deleted int)");
        ReflectionTestUtils.setField(service,"processMapper",processes);
        ReflectionTestUtils.setField(service,"craftMapper",crafts);
        ReflectionTestUtils.setField(service,"jdbc",new NamedParameterJdbcTemplate(jdbc));
    }
    @AfterEach void close() { TenantContextHolder.clear(); jdbc.execute("SHUTDOWN"); }

    private JSONArray groups() {
        return JSON.parseArray("""
                [{"equipmentRequired":true,"parameters":[
                  {"name":"温度","referenceValue":"180±5℃","ui":{"control":"number","unit":"℃","defaultValue":180,"min":175,"max":185,"step":1}}
                ]}]
                """);
    }
    private MesProCraftDO craft(long id, JSONArray parameters) {
        var craft = new MesProCraftDO(); craft.setId(id); craft.setProcessId(10L);
        craft.setCode("GY-"+id); craft.setName("热合工艺"); craft.setParametersJson(parameters.toJSONString()); return craft;
    }
    private SimpleParserPublishReqVO request() {
        var request = new SimpleParserPublishReqVO(); request.setWorkshopId(2L);
        request.setRecognitionJson(new JSONObject(Map.of("processes", List.of(new JSONObject(Map.of("name","热合", "equipmentGroups",groups()))))));
        return request;
    }
    @Test void keepsAllEqualCraftCandidatesAndDoesNotMatchDifferentLimits() {
        JSONArray parameters = MesSimpleParserPublishService.parameters(groups().getJSONObject(0).getJSONArray("parameters"));
        var first = craft(1, parameters); var second = craft(2, parameters);
        parameters.getJSONObject(0).put("upperLimit",190);
        var different = craft(3, parameters);
        var matches = MesSimpleParserReuseService.matchGroups(groups(),List.of(first,second,different));
        assertEquals(List.of(1L,2L), matches.get(0).getJSONArray("crafts").toJavaList(JSONObject.class).stream().map(c -> c.getLong("id")).toList());
        assertEquals(185, matches.get(0).getJSONArray("parameters").getJSONObject(0).getIntValue("upperLimit"));
    }
    @Test void absentAndDisabledGroupsNeverCreateEmptyCraftMatches() {
        assertTrue(MesSimpleParserReuseService.matchGroups(JSON.parseArray("[{\"parameters\":[]}]"),List.of()).isEmpty());
        assertTrue(MesSimpleParserReuseService.matchGroups(JSON.parseArray("[{\"equipmentRequired\":false}]"),List.of()).isEmpty());
        assertThrows(RuntimeException.class, () -> MesSimpleParserReuseService.matchGroups(JSON.parseArray("[{}]"),List.of()));
    }
    @Test void showsAllRealUsageWorkshopsAndPrioritizesCurrentWorkshopWithoutCrossTenantLeaks() {
        when(processes.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                MesProProcessDO.builder().id(10L).code("GX-10").name("热合").build(),
                MesProProcessDO.builder().id(20L).code("GX-20").name("热合").build()));
        when(crafts.selectList(any())).thenReturn(List.of());
        jdbc.update("insert into mes_pro_route values (1,1,0),(2,2,0)");
        jdbc.update("insert into mes_md_workshop values (1,'一车间',1,0),(2,'二车间',1,0),(3,'其他租户',2,0)");
        jdbc.update("insert into mes_md_workstation values (1,1,1,0),(2,2,1,0),(3,3,2,0)");
        jdbc.update("insert into mes_pro_route_process values (1,10,1,1,0),(1,20,1,1,0),(1,20,2,1,0),(2,20,3,2,0)");
        var candidates = service.matches(request()).get(0).getJSONArray("candidates");
        assertEquals(20L,candidates.getJSONObject(0).getLong("processId"));
        assertEquals(2,candidates.getJSONObject(0).getJSONArray("workshops").size());
        assertFalse(candidates.toJSONString().contains("其他租户"));
        assertEquals(0,candidates.getJSONObject(0).getJSONArray("groups").getJSONObject(0).getJSONArray("crafts").size());
    }
    @Test void differentNamesDoNotBecomeReuseCandidates() {
        when(processes.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(MesProProcessDO.builder().id(10L).name("热合检查").build()));
        when(crafts.selectList(any())).thenReturn(List.of());
        assertTrue(service.matches(request()).get(0).getJSONArray("candidates").isEmpty());
    }
}
