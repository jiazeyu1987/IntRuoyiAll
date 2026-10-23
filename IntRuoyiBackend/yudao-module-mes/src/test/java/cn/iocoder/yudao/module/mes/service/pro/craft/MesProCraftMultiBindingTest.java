package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigService;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.*;

class MesProCraftMultiBindingTest {
    private MesProCraftService service;
    private MesProCraftMapper crafts;
    private MesProCraftChangeMapper changes;
    private MesProRouteVersionMapper versions;
    private MesProRouteCandidateConfigService candidate;
    private MesProRouteVersionDO version;
    private List<MesProCraftDO> catalog;
    private JSONObject original;
    private MesProCraftBindingTargetReqVO target;

    private JSONArray parameters(String code, int value) {
        return JSON.parseArray("[{\"parameterCode\":\""+code+"\",\"parameterName\":\"次数\",\"valueType\":\"INTEGER\",\"standardText\":\"清洗次数\",\"defaultValue\":"+value+",\"sort\":1}]");
    }
    private MesProCraftDO craft(long id, String code, int value) {
        MesProCraftDO c = new MesProCraftDO(); c.setId(id); c.setCode("GY-000"+id); c.setName("清洗工艺"+id);
        c.setProcessId(22L); c.setParametersJson(normalize(parameters(code,value)).toJSONString());
        c.setFingerprint(rowFingerprint(c.getName(),22L,parameters(code,value))); return c;
    }
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        TenantContextHolder.setTenantId(1L);
        service=new MesProCraftService(); crafts=mock(MesProCraftMapper.class); changes=mock(MesProCraftChangeMapper.class);
        versions=mock(MesProRouteVersionMapper.class); candidate=mock(MesProRouteCandidateConfigService.class);
        ObjectProvider<MesProRouteCandidateConfigService> provider=mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(candidate);
        when(crafts.lockSequence(1L)).thenReturn(2L);
        when(changes.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of());
        ReflectionTestUtils.setField(service,"craftMapper",crafts); ReflectionTestUtils.setField(service,"changeMapper",changes);
        ReflectionTestUtils.setField(service,"versionMapper",versions); ReflectionTestUtils.setField(service,"candidateConfig",provider);
        var processes=mock(MesProProcessMapper.class); when(processes.selectById(22L)).thenReturn(new MesProProcessDO());
        ReflectionTestUtils.setField(service,"processMapper",processes);
        original=JSON.parseObject("{\"routeProcessId\":11,\"processId\":22,\"deviceSelectionGroups\":[{\"deviceGroupKey\":\"clean\",\"selectionMode\":\"MULTIPLE\",\"deviceIds\":[31,32,33]}]}");
        JSONArray rules=expand(original,parameters("DEVICE_31",2),List.of(31L));
        rules.addAll(expand(original,parameters("DEVICE_32",3),List.of(32L))); Collections.reverse(rules);
        original.put("parameterRules",rules);
        version=new MesProRouteVersionDO(); version.setId(101L); version.setRouteId(100L); version.setLifecycleStatus("DRAFT");
        version.setRouteSnapshotSha256("original-hash");
        setVersionConfig(original);
        when(versions.selectById(101L)).thenReturn(version);
        catalog=new ArrayList<>(List.of(craft(1,"DEVICE_31",2),craft(2,"DEVICE_32",3)));
        when(crafts.selectList(any(LambdaQueryWrapperX.class))).thenAnswer(call -> catalog);
        when(crafts.selectById(anyLong())).thenAnswer(call -> catalog.stream().filter(c -> c.getId().equals(call.getArgument(0))).findFirst().orElse(null));
        target=new MesProCraftBindingTargetReqVO();target.setRouteVersionId(101L); target.setRouteProcessId(11L); target.setExpectedRouteSnapshotSha256("original-hash");
    }
    private void setVersionConfig(JSONObject config) {
        version.setRouteSnapshotJson(JSON.toJSONString(Map.of("configSnapshots",Map.of("productionProcessConfigs",List.of(config)))));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @SuppressWarnings("unchecked") private JSONObject savedConfig() {
        ArgumentCaptor<Map<String,Object>> capture=ArgumentCaptor.forClass(Map.class);
        verify(candidate).saveConfigSnapshots(eq(101L),eq("original-hash"),capture.capture());
        JSONObject data=JSON.parseObject(JSON.toJSONString(capture.getValue()));
        service.validateBindings(data);
        return data.getJSONArray("productionProcessConfigs").getJSONObject(0);
    }
    @Test void associatingExistingSchemesPreservesExactRulesAndUnusedDevices() {
        String before=version.getRouteSnapshotJson();
        service.bindExistingCandidate(target);
        JSONObject saved=savedConfig();
        assertEquals(canonical(original.getJSONArray("parameterRules")),canonical(saved.getJSONArray("parameterRules")));
        assertEquals(canonical(original.getJSONArray("deviceSelectionGroups")),canonical(saved.getJSONArray("deviceSelectionGroups")));
        assertEquals(2,bindings(saved).size());
        assertEquals(List.of(31L),saved.getJSONArray("craftDeviceIds").toJavaList(Long.class));
        assertEquals(List.of(32L),bindings(saved).get(1).getJSONArray("craftDeviceIds").toJavaList(Long.class));
        assertEquals(before,version.getRouteSnapshotJson(),"关联只通过原候选保存入口写入");
        assertTrue(usesCraft(saved,2L));
    }
    @Test void unmatchedSecondSchemeDoesNotSavePartialFirstBinding() {
        catalog.remove(1);
        assertThrows(ServiceException.class,()->service.bindExistingCandidate(target));
        verifyNoInteractions(candidate);
    }
    @Test void ambiguousSchemeRequiresExplicitChoice() {
        catalog.add(craft(3,"DEVICE_32",3));
        assertThrows(ServiceException.class,()->service.bindExistingCandidate(target));
        verifyNoInteractions(candidate);
    }
    @Test void pendingAdditionalCraftRejectsWholeAssociation() {
        MesProCraftChangeDO pending=new MesProCraftChangeDO();pending.setCraftId(2L);pending.setStatus("PENDING");pending.setChangeType("UPDATE");
        when(changes.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(pending));
        assertThrows(ServiceException.class,()->service.bindExistingCandidate(target));
        verifyNoInteractions(candidate);
    }
    @Test void activeVersionCannotBeAssociated() {
        version.setLifecycleStatus("ACTIVE");
        assertThrows(ServiceException.class,()->service.bindExistingCandidate(target));
        verifyNoInteractions(candidate);
    }
    @Test void saveFailureIsPropagatedAndCannotChangeOriginalSnapshot() {
        String before=version.getRouteSnapshotJson();
        doThrow(new IllegalStateException("stale snapshot")).when(candidate).saveConfigSnapshots(anyLong(),anyString(),anyMap());
        assertThrows(IllegalStateException.class,()->service.bindExistingCandidate(target));
        assertEquals(before,version.getRouteSnapshotJson());
    }
    @Test void upgradingAdditionalCraftRetainsOtherCraftRulesAndDeviceScope() {
        service.bindExistingCandidate(target); JSONObject saved=savedConfig();
        JSONObject primaryRule=saved.getJSONArray("parameterRules").getJSONObject(1);
        String primary=primaryRule.toJSONString();
        replaceCraft(saved,2L,"GY-0002",parameters("DEVICE_32",5));
        assertEquals(primary,saved.getJSONArray("parameterRules").getJSONObject(0).toJSONString());
        JSONObject updated=saved.getJSONArray("parameterRules").getJSONObject(1);
        assertEquals(32L,updated.getLong("deviceId")); assertEquals(5,updated.getIntValue("defaultValue"));
        assertEquals(List.of(31L),bindings(saved).get(0).getJSONArray("craftDeviceIds").toJavaList(Long.class));
        catalog.get(1).setParametersJson(parameters("DEVICE_32",5).toJSONString());
        assertDoesNotThrow(()->service.validateBindings(JSON.parseObject(JSON.toJSONString(Map.of("productionProcessConfigs",List.of(saved))))));
    }
    @Test void tamperingAdditionalCraftRulesCannotBypassLibrary() {
        service.bindExistingCandidate(target); JSONObject saved=savedConfig();
        saved.getJSONArray("parameterRules").getJSONObject(0).put("defaultValue",99);
        assertThrows(ServiceException.class,()->service.validateBindings(JSON.parseObject(JSON.toJSONString(Map.of("productionProcessConfigs",List.of(saved))))));
    }
    @Test void repeatedDevicesCannotOverwriteAnotherScheme() {
        String before=original.toJSONString();
        assertThrows(ServiceException.class,()->bindAll(original,List.of(
            new Binding(1L,"GY-0001",parameters("DEVICE_31",2),List.of(31L)),
            new Binding(2L,"GY-0002",parameters("DEVICE_32",3),List.of(31L)))));
        assertEquals(before,original.toJSONString());
    }
    @Test void additionalCraftAppearsInImpactPreviewAndOpenDraftBlocksModification() {
        service.bindExistingCandidate(target); JSONObject saved=savedConfig();
        setVersionConfig(saved); version.setLifecycleStatus("ACTIVE");
        when(versions.selectActiveListWithExistingRoutes()).thenReturn(List.of(version));
        when(versions.selectOpenCandidateByRouteId(100L)).thenReturn(new MesProRouteVersionDO());
        var open=versions.selectOpenCandidateByRouteId(100L);open.setRouteSnapshotJson("{\"configSnapshots\":{}}");
        MesProCraftSaveReqVO request=new MesProCraftSaveReqVO();request.setId(2L);request.setName("修改后工艺");request.setProcessId(22L);
        request.setExpectedFingerprint(catalog.get(1).getFingerprint());request.setParameters(parameters("DEVICE_32",4));request.setReason("标准更新");
        Map<String,Object> preview=service.preview(request);
        assertEquals(1,((JSONArray)preview.get("routes")).size());
        assertFalse(((List<?>)preview.get("blockers")).isEmpty());
    }
}
