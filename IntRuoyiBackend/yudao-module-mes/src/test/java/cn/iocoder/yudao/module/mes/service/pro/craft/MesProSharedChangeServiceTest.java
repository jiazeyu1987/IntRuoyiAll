package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesSharedChangeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftChangeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import com.alibaba.fastjson.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProSharedChangeServiceTest {
    private final MesProSharedChangeService service = new MesProSharedChangeService();
    private final MesProCraftService crafts = mock(MesProCraftService.class);
    private final MesProCraftChangeMapper changes = mock(MesProCraftChangeMapper.class);
    private final MesProProcessMapper processes = mock(MesProProcessMapper.class);
    private final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    private final cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessContentService contents =
            mock(cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessContentService.class);
    private final MesProProcessDO source = MesProProcessDO.builder().id(10L).name("热合").code("GX-10").status(0).build();
    private final cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService security = mock(cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService.class);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"security",security);
        when(security.hasPermission(anyString())).thenReturn(true);
        ReflectionTestUtils.setField(service,"crafts",crafts); ReflectionTestUtils.setField(service,"changeMapper",changes);
        ReflectionTestUtils.setField(service,"processMapper",processes); ReflectionTestUtils.setField(service,"versionMapper",versions);
        ReflectionTestUtils.setField(service,"processContents",contents);
        when(processes.selectById(10L)).thenReturn(source);
        when(versions.selectMaxVersionNoByRouteId(anyLong())).thenReturn("V2");
    }
    private MesSharedChangeReqVO request() {
        var req = new MesSharedChangeReqVO(); req.setSubjectType("PROCESS"); req.setAction("UPDATE"); req.setSourceId(10L);
        req.setExpectedFingerprint((String) service.source("PROCESS",10L).get("fingerprint"));
        req.setRequestId(UUID.randomUUID().toString()); req.setReason("");
        var process = new MesProProcessSaveReqVO(); process.setId(10L); process.setCode("GX-10"); process.setName("热合新版"); process.setStatus(0);
        req.setProcess(process); return req;
    }
    private MesProRouteVersionDO version(long routeId, long processId) {
        var version = new MesProRouteVersionDO(); version.setId(routeId+10000); version.setRouteId(routeId);
        version.setVersionNo("V2"); version.setActive(true); version.setLifecycleStatus("ACTIVE");
        JSONObject root = JSON.parseObject("""
            {"routeName":"路线","configSnapshots":{"flowGraph":{"nodes":[]},"productionProcessConfigSchemaVersion":1,
            "productionProcessConfigs":[],"batchRecordReports":[{"id":5}],"formBindings":[{"id":6}]}}
            """);
        root.put("routeId",routeId);
        JSONObject node = new JSONObject(true); node.put("routeProcessId",routeId*10); node.put("processId",processId); node.put("processName","热合");
        root.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes").add(node);
        JSONObject config = JSON.parseObject(node.toJSONString()); config.put("overagePercent",0); config.put("lossReasons",new JSONArray());
        config.put("deviceSelectionGroups",new JSONArray()); config.put("parameterRules",new JSONArray());
        root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").add(config);
        version.setRouteSnapshotJson(root.toJSONString()); version.setRouteSnapshotSha256(new MesProRouteSnapshotCanonicalizer().sha256(root.toJSONString()));
        return version;
    }
    private MesSharedChangeReqVO deviceRequest() {
        var mapper = mock(cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper.class);
        var devices = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper.class);
        ReflectionTestUtils.setField(service, "machineryMapper", mapper);
        ReflectionTestUtils.setField(service, "deviceMapper", devices);
        var device = new cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO();
        device.setId(10L); device.setCode("DEVICE-10");
        when(mapper.selectById(10L)).thenReturn(device);
        var req = new MesSharedChangeReqVO(); req.setSubjectType("DEVICE"); req.setAction("UPDATE"); req.setSourceId(10L);
        req.setExpectedFingerprint((String) service.source("DEVICE",10L).get("fingerprint"));
        req.setRequestId(UUID.randomUUID().toString()); req.setReason("remark update");
        var update = new cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachinerySaveReqVO();
        update.setId(10L); update.setCode("DEVICE-10"); update.setRemark("updated"); req.setDevice(update);
        return req;
    }
    @Test void unreferencedDeviceSubmitKeepsSharedApprovalPath() {
        var req = deviceRequest();
        when(security.hasPermission(anyString())).thenReturn(false);
        req.setImpactToken((String) service.preview(req).get("impactToken"));
        service.submit(req);
        verify(crafts).submitShared(anyString(), any(), anyString(), eq(List.of()), any());
        verifyNoInteractions(security);
    }
    @Test void retryChecksStoredRouteImpact() {
        var req = deviceRequest();
        var batch = new MesProCraftChangeDO(); batch.setId(99L); batch.setChangeType("SHARED");
        JSONObject content = (JSONObject) JSON.toJSON(req); content.remove("impactToken");
        JSONObject metadata = new JSONObject(true); metadata.put("requestId", req.getRequestId());
        metadata.put("requestHash", new MesProRouteSnapshotCanonicalizer().sha256(content.toJSONString()));
        metadata.put("routes", new JSONArray()); batch.setAfterJson(metadata.toJSONString());
        when(changes.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(batch));
        when(security.hasPermission(anyString())).thenReturn(false);
        assertSame(batch, service.submit(req));
        metadata.getJSONArray("routes").add(new JSONObject()); batch.setAfterJson(metadata.toJSONString());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.submit(req));
    }
    @Test void onlyDeviceUpdateWithoutReferencesAvoidsVersionPermissions() {
        when(security.hasPermission(anyString())).thenReturn(false);
        var req = new MesSharedChangeReqVO(); req.setSubjectType("DEVICE"); req.setAction("UPDATE");
        assertDoesNotThrow(() -> service.requireSubmitPermissions(req, false));
        for (String permission : List.of("mes:pro-route:version-create", "mes:pro-route:version-update", "mes:pro-route:version-submit")) {
            when(security.hasPermission(anyString())).thenReturn(true);
            when(security.hasPermission(permission)).thenReturn(false);
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.requireSubmitPermissions(req, true));
        }
        when(security.hasPermission(anyString())).thenReturn(true);
        assertDoesNotThrow(() -> service.requireSubmitPermissions(req, true));
        when(security.hasPermission(anyString())).thenReturn(false);
        req.setAction("REPLACE");
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.requireSubmitPermissions(req, false));
        req.setSubjectType("PROCESS"); req.setAction("UPDATE");
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.requireSubmitPermissions(req, false));
    }
    @Test void newlyReferencedDeviceRequiresPermissionsEvenAfterZeroImpactPreview() {
        var req = deviceRequest();
        req.setImpactToken((String) service.preview(req).get("impactToken"));
        var devices = (cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper)
                ReflectionTestUtils.getField(service, "deviceMapper");
        var execution = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO();
        execution.setId(77L);
        when(devices.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(execution));
        var active = version(1,10);
        var snapshot = JSON.parseObject(active.getRouteSnapshotJson());
        snapshot.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0)
                .getJSONArray("deviceSelectionGroups").add(JSON.parseObject("{\"deviceIds\":[77]}"));
        active.setRouteSnapshotJson(snapshot.toJSONString());
        when(versions.selectActiveListWithExistingRoutes()).thenReturn(List.of(active));
        when(security.hasPermission("mes:pro-route:version-submit")).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.submit(req));
        verify(crafts,never()).submitShared(anyString(),any(),anyString(),anyList(),any());
    }
    @Test void fiveThousandRoutesYieldExactlyOneThousandReferencesWithoutWrites() {
        var all = IntStream.rangeClosed(1,5000).mapToObj(i->version(i,i<=1000?10L:20L)).toList();
        when(versions.selectActiveListWithExistingRoutes()).thenReturn(all);
        var preview = service.preview(request());
        assertEquals(1000,((JSONArray)preview.get("routes")).size());
        assertEquals("V3",((JSONArray)preview.get("routes")).getJSONObject(0).getString("nextVersionNo"));
        assertTrue(((List<?>)preview.get("blockers")).isEmpty());
        assertEquals("热合",source.getName());
        verify(changes,never()).insert(any(MesProCraftChangeDO.class));
        verify(processes,never()).updateById(any(MesProProcessDO.class));
        verify(versions,never()).updateById(any(MesProRouteVersionDO.class));
    }
    @Test void draftOnlyReferenceBlocksChangeAndDirectBypass() {
        var draft=version(1,10); draft.setActive(false);draft.setLifecycleStatus("DRAFT");
        when(versions.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(draft));
        var preview=service.preview(request());
        assertEquals(1,((List<?>)preview.get("blockers")).size());
        assertThrows(RuntimeException.class,()->service.assertDirectUpdateAllowed("PROCESS",10L));
    }
    @Test void nameMatchAloneDoesNotExpandImpactAndReasonDoesNotInvalidatePreview() {
        var original=version(1,10);var unrelated=version(2,20);
        when(versions.selectActiveListWithExistingRoutes()).thenReturn(List.of(original,unrelated));
        var req=request(); var first=service.preview(req); req.setReason("修订说明");var second=service.preview(req);
        assertEquals(first.get("impactToken"),second.get("impactToken"));
        assertEquals(1,((JSONArray)first.get("routes")).size());
        source.setName("他人已修改");
        assertThrows(RuntimeException.class,()->service.preview(req));
    }
    @Test void changedAffectedVersionRejectsPreviouslyConfirmedImpact() {
        var active=version(1,10);when(versions.selectActiveListWithExistingRoutes()).thenReturn(List.of(active));
        var req=request();req.setReason("修订");req.setImpactToken((String)service.preview(req).get("impactToken"));
        active.setRouteSnapshotSha256("changed");
        assertThrows(RuntimeException.class,()->service.submit(req));
        verify(crafts,never()).submitShared(anyString(),any(),anyString(),anyList(),any());
    }
    @Test void retryReturnsOriginalBatchButRejectsReusingRequestIdForDifferentContent() {
        var req=request();req.setReason("修订");
        var batch=new MesProCraftChangeDO();batch.setId(99L);batch.setChangeType("SHARED");batch.setStatus("PENDING");
        JSONObject content=(JSONObject)JSON.toJSON(req);content.remove("impactToken");
        JSONObject proposal=new JSONObject(true);proposal.put("requestId",req.getRequestId());
        proposal.put("routes", new JSONArray());
        proposal.put("requestHash",new MesProRouteSnapshotCanonicalizer().sha256(content.toJSONString()));
        batch.setAfterJson(proposal.toJSONString());
        when(changes.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(batch));
        assertEquals(99L,service.submit(req).getId());
        req.getProcess().setName("another");
        assertThrows(RuntimeException.class,()->service.submit(req));
        verify(crafts,never()).submitShared(anyString(),any(),anyString(),anyList(),any());
    }

    private MesProCraftChangeDO pendingChange(MesSharedChangeReqVO request, Object before, Object after) {
        var change = new MesProCraftChangeDO();
        change.setCandidateIdsJson("[101]");
        change.setBeforeJson(JSON.toJSONString(before));
        var proposal = new JSONObject(true);
        proposal.put("request", request); proposal.put("data", after);
        proposal.put("routes", new JSONArray()); proposal.put("sourceDeviceIds", new JSONArray());
        change.setAfterJson(proposal.toJSONString());
        return change;
    }

    @Test void newDraftReferenceDuringApprovalBlocksPublicationButOwnCandidateDoesNot() {
        var change = pendingChange(request(), service.source("PROCESS", 10L).get("data"), source);
        var own = version(1, 10); own.setId(101L);
        when(versions.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(own));
        assertDoesNotThrow(() -> service.verifyBeforePublish(change));
        var other = version(2, 10); other.setLifecycleStatus("DRAFT");
        when(versions.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(own, other));
        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.verifyBeforePublish(change));
        assertTrue(failure.getMessage().contains("新增了引用"));
    }

    @Test void contentsArePreviewedWithoutWritesAndConcurrentStepEditsBlockPublication() {
        var old = cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessContentDO.builder()
                .id(1L).processId(10L).sort(1).content("旧步骤").build();
        when(contents.getProcessContentListByProcessId(10L)).thenReturn(List.of(old));
        var req = request();
        var step = new cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO();
        step.setId(1L); step.setProcessId(10L); step.setSort(1); step.setContent("新步骤");
        req.setProcessContents(List.of(step));
        var preview = service.preview(req);
        assertEquals("旧步骤", ((JSONObject) preview.get("before")).getJSONArray("processContents").getJSONObject(0).getString("content"));
        assertEquals("新步骤", ((JSONObject) preview.get("after")).getJSONArray("processContents").getJSONObject(0).getString("content"));
        verify(contents, never()).updateProcessContent(any());
        var change = pendingChange(req, preview.get("before"), preview.get("after"));
        old.setContent("他人修改");
        assertThrows(RuntimeException.class, () -> service.verifyBeforePublish(change));
    }

    @Test void contentsRejectForeignIdsAndDuplicateSorts() {
        var req = request();
        var step = new cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO();
        step.setId(99L); step.setProcessId(10L); step.setSort(1);
        req.setProcessContents(List.of(step));
        assertThrows(RuntimeException.class, () -> service.preview(req));
        step.setId(null); req.setProcessContents(List.of(step, step));
        assertThrows(RuntimeException.class, () -> service.preview(req));
        req.setProcessContents(List.of(step)); step.setProcessId(20L);
        assertThrows(RuntimeException.class, () -> service.preview(req));
    }

    @Test void confirmedContentsApplyTogetherWithProcessAndRetainExistingIds() {
        var old = cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessContentDO.builder()
                .id(1L).processId(10L).sort(1).content("删除步骤").build();
        var kept = cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessContentDO.builder()
                .id(2L).processId(10L).sort(2).content("保留步骤").build();
        when(contents.getProcessContentListByProcessId(10L)).thenReturn(List.of(old, kept));
        var req = request();
        var updated = new cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO();
        updated.setId(2L); updated.setProcessId(10L); updated.setSort(1); updated.setContent("修改步骤");
        var created = new cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO();
        created.setProcessId(10L); created.setSort(2); created.setContent("新增步骤");
        req.setProcessContents(List.of(updated, created));
        var provider = mock(org.springframework.beans.factory.ObjectProvider.class);
        var processService = mock(cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService.class);
        when(provider.getObject()).thenReturn(processService);
        ReflectionTestUtils.setField(service,"processes",provider);
        service.apply(pendingChange(req, source, source));
        verify(processService).updateProcess(any());
        verify(contents).deleteProcessContent(1L);
        verify(contents).updateProcessContent(argThat(value -> Long.valueOf(2).equals(value.getId()) && "修改步骤".equals(value.getContent())));
        verify(contents).createProcessContent(argThat(value -> value.getId() == null && "新增步骤".equals(value.getContent())));
        verify(contents, never()).deleteProcessContent(2L);
    }

    @Test void persistedSourceSnapshotWithAuditTimesRemainsUnchanged() {
        source.setCreateTime(java.time.LocalDateTime.of(2026, 11, 17, 23, 30, 47));
        source.setUpdateTime(java.time.LocalDateTime.of(2026, 11, 17, 23, 30, 47));
        source.setCreator("1"); source.setUpdater("1"); source.setDeleted(false);
        JSONObject before = (JSONObject) service.source("PROCESS", 10L).get("data");
        var change = pendingChange(request(), before, before);
        change.setBeforeJson(before.toJSONString());
        assertDoesNotThrow(() -> service.verifyBeforePublish(change));
        source.setUpdateTime(source.getUpdateTime().plusSeconds(1));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.verifyBeforePublish(change));
    }

    @Test void replacementDeviceMustKeepItsConfirmedIdentityAndRemainEnabledUntilPublication() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "shared-device-test"),
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO.class);
        var machinery = mock(cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper.class);
        var devices = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper.class);
        ReflectionTestUtils.setField(service, "machineryMapper", machinery);
        ReflectionTestUtils.setField(service, "deviceMapper", devices);
        var old = cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO.builder().id(1L).code("A").build();
        var next = cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO.builder().id(2L).code("B").build();
        when(machinery.selectById(1L)).thenReturn(old); when(machinery.selectById(2L)).thenReturn(next);
        var oldExecution = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO();
        oldExecution.setId(7L);
        var target = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO();
        target.setId(8L); target.setEnabled(true); target.setDeviceStatus("ENABLED");
        when(devices.selectById(8L)).thenReturn(target);
        when(devices.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenAnswer(invocation -> {
                    var query = (com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) invocation.getArgument(0);
                    query.getSqlSegment();
                    return query.getParamNameValuePairs().containsValue("A") ? List.of(oldExecution) : List.of(target);
                });
        var req = new MesSharedChangeReqVO(); req.setSubjectType("DEVICE"); req.setAction("REPLACE");
        req.setSourceId(1L); req.setTargetId(2L);
        var change = pendingChange(req, old, next);
        var proposal = JSON.parseObject(change.getAfterJson());
        proposal.put("sourceDeviceIds", List.of(7L)); proposal.put("targetDeviceId", 8L);
        change.setAfterJson(proposal.toJSONString());
        assertDoesNotThrow(() -> service.verifyBeforePublish(change));
        target.setEnabled(false);
        var disabled = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.verifyBeforePublish(change));
        assertTrue(disabled.getMessage().contains("未启用"));
        target.setEnabled(true); target.setId(9L);
        var replaced = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.verifyBeforePublish(change));
        assertTrue(replaced.getMessage().contains("执行身份已变化"));
    }

    @Test void missingReplacementChoicesRemainInlineInsteadOfGeneratingOneErrorPerRoute() {
        var req = request(); req.setAction("REPLACE"); req.setTargetId(20L); req.setProcess(null);
        when(processes.selectById(20L)).thenReturn(MesProProcessDO.builder().id(20L).code("GX-20").name("新工序").status(0).build());
        var active = version(1, 10);
        JSONObject root = JSON.parseObject(active.getRouteSnapshotJson());
        root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0)
                .put("parameterRules", JSON.parseArray("[{\"parameterName\":\"温度\",\"standardText\":\"180\",\"deviceId\":7}]"));
        active.setRouteSnapshotJson(root.toJSONString());
        when(versions.selectActiveListWithExistingRoutes()).thenReturn(List.of(active));
        var preview = service.preview(req);
        assertEquals(1, preview.get("pendingSelectionCount"));
        assertTrue(((List<?>) preview.get("blockers")).isEmpty());
        req.setImpactToken((String) preview.get("impactToken")); req.setReason("换工序");
        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.submit(req));
        assertTrue(failure.getMessage().contains("请选择全部工序"));
        verify(crafts, never()).submitShared(anyString(), any(), anyString(), anyList(), any());
    }
}
