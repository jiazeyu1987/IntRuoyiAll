package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceDraftReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionQualityRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReexecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRouteOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReviewTimelineRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchDossierItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchDossierItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.BatchRecordCellLinkAutoPersistResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProBatchRecordCellLinkAutoPersistService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProBatchRecordCellLinkService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_CLOSE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_CLOSE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ROUTE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_PASSWORD_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        MesProEdhrBatchExecutionServiceImpl.class,
        MesProEdhrBatchTaskVisibilityService.class,
        MesProEdhrBatchExecutionVisibilityService.class,
        MesProEdhrCandidateResolver.class,
        MesProEdhrPreReleaseEditabilityService.class,
        MesProEdhrBatchWorkbenchServiceImpl.class,
        MesProEdhrBatchStageResolver.class,
        MesProBatchRecordRuntimeSnapshotSupport.class
})
class MesProEdhrBatchExecutionServiceTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrBatchExecutionService batchExecutionService;
    @Resource
    private MesProEdhrBatchWorkbenchService batchWorkbenchService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;
    @Resource
    private MesProEdhrBatchDossierItemMapper dossierItemMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordVersionMapper batchRecordVersionMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesProEdhrRecordChangeEventMapper recordChangeEventMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper workTaskAssignmentRuleMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private FormTemplateVersionMapper formTemplateVersionMapper;
    @MockitoBean
    private FormActionInstanceMapper formActionInstanceMapper;

    @MockitoBean
    private MesProBatchRecordExecutionService singleExecutionService;
    @MockitoBean
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;
    @MockitoBean
    private MesProEdhrRecordbookGlobalSettingService recordbookGlobalSettingService;
    @MockitoBean
    private MesProEdhrBatchVoidEffectService batchVoidEffectService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProEdhrPermissionGateService permissionGateService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private RoleApi roleApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @MockitoBean
    private FormCenterRuntimeService formCenterRuntimeService;
    @MockitoBean
    private MesProBatchRecordCellLinkAutoPersistService cellLinkAutoPersistService;
    @MockitoBean
    private MesProBatchRecordCellLinkService cellLinkService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(1L);
        when(permissionApi.hasAnyPermissions(any(), eq(MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION)))
                .thenReturn(true);
        when(recordbookGlobalSettingService.resolveEffectiveRecordbookEnabled(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adminUserApi.getUserMap(any())).thenAnswer(invocation -> {
            Object rawIds = invocation.getArgument(0);
            Collection<?> ids = rawIds instanceof Collection<?> collection ? collection : List.of();
            Map<Long, AdminUserRespDTO> users = new LinkedHashMap<>();
            for (Object rawId : ids) {
                Long userId = Long.valueOf(String.valueOf(rawId));
                users.put(userId, user(userId, "用户" + userId));
            }
            return users;
        });
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), any()))
                .thenAnswer(invocation -> {
                    FormInstanceRespVO respVO = new FormInstanceRespVO();
                    respVO.setId(randomLongId());
                    return respVO;
                });
        when(cellLinkService.buildFormTemplateVersionPrefillData(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(3));
    }

    @Test
    void openOrCreate_generatesRouteOrderedTasksAndIsIdempotent() {
        Fixture fixture = insertRouteFixture(true, true);

        EdhrBatchExecutionOpenOrCreateReqVO reqVO = new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-EDHR-001")
                .setRouteId(fixture.routeId());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(reqVO);
        EdhrBatchExecutionRespVO reopened = batchExecutionService.openOrCreate(reqVO);

        assertEquals(created.getId(), reopened.getId());
        assertEquals("BATCH-EDHR-001", created.getBatchCode());
        assertEquals(6, created.getTaskTotal());
        assertEquals(0, created.getTaskApprovedCount());
        assertEquals(0, created.getBlockedCount());
        assertFalse(created.getCanClose());
        assertEquals(6, created.getTasks().size());
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(10, routeTasks.get(0).getRouteProcessSort());
        assertEquals(20, routeTasks.get(1).getRouteProcessSort());

        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(created.getId());
        assertEquals(6, tasks.size());
        verify(operationAuditService, atLeastOnce()).record(argThat(command ->
                "OPEN".equals(command.getOperationType())
                        && "BATCH_EXECUTION".equals(command.getObjectType())
                        && String.valueOf(created.getId()).equals(command.getObjectId())
                        && "SUCCESS".equals(command.getResultStatus())));
        List<MesProEdhrBatchExecutionTaskDO> persistedRouteTasks = tasks.stream()
                .filter(task -> task.getBatchRecordReportId() != null)
                .toList();
        assertEquals(fixture.reportId1(), persistedRouteTasks.get(0).getBatchRecordReportId());
        assertEquals(fixture.reportId2(), persistedRouteTasks.get(1).getBatchRecordReportId());
    }

    @Test
    void getPage_exposesInitialUpdateTimeAsBatchRowCreateTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 24, 9, 30, 15);
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("EDHRB-TIME-CONTRACT")
                .setWorkOrderId(1001L)
                .setWorkOrderCode("WO-TIME-CONTRACT")
                .setBatchCode("BATCH-TIME-CONTRACT")
                .setAttemptNo(1)
                .setProductId(2001L)
                .setProductCode("PROD-TIME")
                .setProductName("时间契约产品")
                .setRouteId(3001L)
                .setRouteCode("ROUTE-TIME")
                .setRouteName("时间契约路线")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .setTaskTotal(0)
                .setTaskApprovedCount(0)
                .setBlockedCount(0);
        batch.setCreateTime(createdAt);
        batch.setUpdateTime(createdAt);
        batchExecutionMapper.insert(batch);
        EdhrBatchExecutionPageReqVO pageReqVO = new EdhrBatchExecutionPageReqVO();
        pageReqVO.setBatchExecutionCode("EDHRB-TIME-CONTRACT");

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(pageReqVO);

        assertEquals(1, page.getTotal());
        EdhrBatchExecutionRespVO row = page.getList().get(0);
        assertEquals(createdAt, row.getCreateTime());
        assertEquals(createdAt, row.getUpdateTime());
    }

    @Test
    void listRouteOptionsByWorkOrder_returnsEnabledProductRoutesForExplicitOpenOrCreate() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO secondRoute = insertExecutableRoute("eDHR 第二路线", "RPT-OPT");
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(secondRoute.getId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());
        MesProRouteDO disabledRoute = MesProRouteDO.builder()
                .code("ROUTE-DISABLED-" + randomLongId())
                .name("停用路线")
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        routeMapper.insert(disabledRoute);
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(disabledRoute.getId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());

        List<EdhrBatchExecutionRouteOptionRespVO> routeOptions =
                batchExecutionService.listRouteOptionsByWorkOrder(workOrder.getId());

        assertEquals(List.of(fixture.routeId(), secondRoute.getId()),
                routeOptions.stream().map(EdhrBatchExecutionRouteOptionRespVO::getRouteId).toList());
        assertEquals("eDHR 路线", routeOptions.get(0).getRouteName());
        assertEquals("eDHR 第二路线", routeOptions.get(1).getRouteName());
        assertTrue(routeOptions.stream().allMatch(option -> Boolean.TRUE.equals(option.getBatchRouteEnabled())));
    }

    @Test
    void listRouteOptionsByWorkOrder_reportsMissingProductRouteBindingClearly() {
        Fixture fixture = insertRouteFixture(true, true);
        routeProductMapper.deleteByRouteId(fixture.routeId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> batchExecutionService.listRouteOptionsByWorkOrder(fixture.workOrderId()));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_BINDING_REQUIRED.getCode(), exception.getCode());
        assertEquals(PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_BINDING_REQUIRED.getMsg(), exception.getMessage());
    }

    @Test
    void openExistingBatch_shouldRecoverMissingRouteProcessTasks() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-LEGACY-MISSING-PROCESS")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-LEGACY-MISSING-PROCESS")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-LEGACY-MISSING-PROCESS")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        EdhrBatchExecutionRespVO reopened = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(workOrder.getId())
                .setBatchCode("BATCH-LEGACY-MISSING-PROCESS")
                .setRouteId(route.getId()));

        assertEquals(legacyBatch.getId(), reopened.getId());
        assertEquals(6, reopened.getTaskTotal());
        assertEquals(2, routeTasks(reopened).size());
        assertEquals("第一工序", routeTask(reopened, 0).getProcessName());
        assertEquals("第二工序", routeTask(reopened, 1).getProcessName());
        List<MesProEdhrBatchExecutionTaskDO> persistedTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId());
        assertEquals(6, persistedTasks.size());
        assertEquals(2, persistedTasks.stream()
                .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                .count());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-DETAIL-MISSING-PROCESS")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-DETAIL-MISSING-PROCESS")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-DETAIL-MISSING-PROCESS")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        assertEquals(6, detail.getTaskTotal());
        assertEquals(2, routeTasks(detail).size());
        assertEquals("第一工序", routeTask(detail, 0).getProcessName());
        assertEquals("第二工序", routeTask(detail, 1).getProcessName());
        List<MesProEdhrBatchExecutionTaskDO> persistedTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId());
        assertEquals(6, persistedTasks.size());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        MesProProcessDO process = processMapper.selectById(firstProcess.getProcessId());
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO version = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        String productInfoReport = insertVersionedReport(
                "RPT-DETAIL-PRODUCT-INFO-MEMBER", "产品信息", definitionId, version.getId(), 1, "MAIN");
        String processReport = insertVersionedReport(
                "RPT-DETAIL-PRODUCT-INFO-PROCESS", "粗洗工序生产记录", definitionId, version.getId(), 2, "MAIN");
        insertBatchUseConfigWithSlots(route.getId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(processReport, "MAIN", null, null, null, 1)
        ));
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-DETAIL-MISSING-PRODUCT-INFO")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-DETAIL-MISSING-PRODUCT-INFO")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-DETAIL-MISSING-PRODUCT-INFO")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(5)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());
        batchTaskMapper.insert(MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(legacyBatch.getId())
                .nodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .routeProcessId(firstProcess.getId())
                .rootProcessFlag(Boolean.TRUE)
                .routeProcessSort(firstProcess.getSort())
                .processId(process.getId())
                .processCode(process.getCode())
                .processName(process.getName())
                .batchRecordReportId(processReport)
                .batchRecordReportName("粗洗工序生产记录")
                .batchRecordDefinitionId(definitionId)
                .batchRecordVersionId(version.getId())
                .batchRecordSort(1)
                .executionMode("SEQUENTIAL")
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .permissionScopeId(5001L)
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .requiredFlag(Boolean.TRUE)
                .build());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        List<EdhrBatchExecutionTaskRespVO> firstProcessTasks = routeTasks(detail).stream()
                .filter(task -> Objects.equals(firstProcess.getSort(), task.getRouteProcessSort()))
                .toList();
        assertEquals(List.of(processReport), firstProcessTasks.stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportId)
                .toList());
        assertEquals(List.of("粗洗工序生产记录"), firstProcessTasks.stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportName)
                .toList());
        assertEquals(List.of(1), firstProcessTasks.stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordSort)
                .toList());
        assertEquals(Boolean.TRUE, firstProcessTasks.get(0).getAvailable());
        EdhrBatchExecutionTaskRespVO productInfoTask = routeTasks(detail).stream()
                .filter(task -> Objects.equals(productInfoReport, task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        assertEquals(80, productInfoTask.getRouteProcessSort());
        assertEquals("产品信息", productInfoTask.getProcessName());
        assertEquals(80, productInfoTask.getBatchRecordSort());
        assertEquals(Boolean.FALSE, productInfoTask.getAvailable());
        assertEquals("前序批记录表单未全部填写完成", productInfoTask.getGateMessage());
        List<MesProEdhrBatchExecutionTaskDO> persistedTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId());
        assertEquals(6, persistedTasks.size());
        assertEquals(1, persistedTasks.stream()
                .filter(task -> Objects.equals(productInfoReport, task.getBatchRecordReportId()))
                .count());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-PAGE-MISSING-PROCESS")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-PAGE-MISSING-PROCESS")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-PAGE-MISSING-PROCESS")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(
                new EdhrBatchExecutionPageReqVO().setBatchCode("BATCH-PAGE-MISSING-PROCESS"));

        EdhrBatchExecutionRespVO row = page.getList().stream()
                .filter(item -> item.getId().equals(legacyBatch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, routeTasks(row).size());
        assertEquals("第一工序", routeTask(row, 0).getProcessName());
        assertEquals("第二工序", routeTask(row, 1).getProcessName());
        List<MesProEdhrBatchExecutionTaskDO> persistedTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId());
        assertEquals(6, persistedTasks.size());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getPage_marksUnrecoverableLegacyBatchAsBlockedWithoutFailingWholePage() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(record -> routeFlowProcessBatchRecordMapper.deleteById(record.getId()));
        routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(config -> routeFlowProcessConfigMapper.deleteById(config.getId()));
        MesProRouteFlowConfigDO liveFlowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(route.getId(), "BATCH");
        if (liveFlowConfig != null) {
            routeFlowConfigMapper.deleteById(liveFlowConfig.getId());
        }
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-PAGE-BROKEN-SNAPSHOT")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-PAGE-BROKEN-SNAPSHOT")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-PAGE-BROKEN-SNAPSHOT")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(incompleteFrozenBatchTaskConfigSnapshotJson())
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(
                new EdhrBatchExecutionPageReqVO().setBatchCode("BATCH-PAGE-BROKEN-SNAPSHOT"));

        EdhrBatchExecutionRespVO row = page.getList().stream()
                .filter(item -> item.getId().equals(legacyBatch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.FALSE, row.getCanClose());
        assertTrue(row.getCloseBlockers().stream()
                .anyMatch(blocker -> blocker.contains("工艺流程批记录配置")));
        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());
        assertEquals(Boolean.FALSE, detail.getCanClose());
        assertEquals(Boolean.FALSE, detail.getCanArchive());
        assertTrue(detail.getTasks().isEmpty());
        assertTrue(detail.getCloseBlockers().stream()
                .anyMatch(blocker -> blocker.contains("工艺流程批记录配置")));
    }

    @Test
    void getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-DETAIL-CURRENT-CONFIG-RECOVERY")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-DETAIL-CURRENT-CONFIG-RECOVERY")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-DETAIL-CURRENT-CONFIG-RECOVERY")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(incompleteFrozenBatchTaskConfigSnapshotJson())
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        assertEquals(6, detail.getTaskTotal());
        assertEquals(List.of(fixture.reportId1(), fixture.reportId2()), routeTasks(detail).stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportId)
                .toList());
        List<MesProEdhrBatchExecutionTaskDO> persistedRouteTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId()).stream()
                        .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                        .sorted(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                                .thenComparing(MesProEdhrBatchExecutionTaskDO::getBatchRecordSort))
                        .toList();
        assertEquals(List.of(fixture.reportId1(), fixture.reportId2()), persistedRouteTasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getBatchRecordReportId)
                .toList());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getReviewTimeline_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-TIMELINE-CURRENT-CONFIG-RECOVERY")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-TIMELINE-CURRENT-CONFIG-RECOVERY")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-TIMELINE-CURRENT-CONFIG-RECOVERY")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(incompleteFrozenBatchTaskConfigSnapshotJson())
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        EdhrBatchExecutionReviewTimelineRespVO timeline = batchExecutionService.getReviewTimeline(legacyBatch.getId());

        assertEquals(6, timeline.getTaskEvents().size());
        List<MesProEdhrBatchExecutionTaskDO> persistedRouteTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId()).stream()
                        .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                        .sorted(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                                .thenComparing(MesProEdhrBatchExecutionTaskDO::getBatchRecordSort))
                        .toList();
        assertEquals(List.of(fixture.reportId1(), fixture.reportId2()), persistedRouteTasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getBatchRecordReportId)
                .toList());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getPage_marksBrokenExistingRouteTaskAsBlockedWithoutFailingWholePage() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        MesProProcessDO process = processMapper.selectById(routeProcess.getProcessId());
        String brokenReportId = insertLegacyReportWithoutStableIdentity(
                "RPT-PAGE-BROKEN-ROUTE-TASK", "缺稳定身份批记录表");
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-PAGE-BROKEN-ROUTE-TASK")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-PAGE-BROKEN-ROUTE-TASK")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-PAGE-BROKEN-ROUTE-TASK")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(5)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());
        batchTaskMapper.insert(MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(legacyBatch.getId())
                .nodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .routeProcessId(routeProcess.getId())
                .routeProcessSort(routeProcess.getSort())
                .processId(process.getId())
                .processCode(process.getCode())
                .processName(process.getName())
                .batchRecordReportId(brokenReportId)
                .batchRecordReportName("缺稳定身份批记录表")
                .batchRecordSort(1)
                .executionMode("SEQUENTIAL")
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .permissionScopeId(5001L)
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT)
                .requiredFlag(Boolean.TRUE)
                .build());

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(
                new EdhrBatchExecutionPageReqVO().setBatchCode("BATCH-PAGE-BROKEN-ROUTE-TASK"));

        EdhrBatchExecutionRespVO row = page.getList().stream()
                .filter(item -> item.getId().equals(legacyBatch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.FALSE, row.getCanClose());
        assertEquals(Boolean.FALSE, row.getCanArchive());
        assertTrue(row.getTasks().isEmpty());
        assertTrue(row.getCloseBlockers().stream()
                .anyMatch(blocker -> blocker.contains("工艺流程批记录配置")));
        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());
        assertEquals(Boolean.FALSE, detail.getCanClose());
        assertEquals(Boolean.FALSE, detail.getCanArchive());
        assertTrue(detail.getTasks().isEmpty());
        assertTrue(detail.getCloseBlockers().stream()
                .anyMatch(blocker -> blocker.contains("工艺流程批记录配置")));
    }

    @Test
    void getPage_marksMissingBatchRecordAttachmentOwnersAsBlockedWithoutFailingWholePage() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-PAGE-MISSING-ATTACHMENT-OWNERS")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-PAGE-MISSING-ATTACHMENT-OWNERS")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-PAGE-MISSING-ATTACHMENT-OWNERS")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJsonWithoutBatchRecordAttachmentOwners(
                        route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(
                new EdhrBatchExecutionPageReqVO().setBatchCode("BATCH-PAGE-MISSING-ATTACHMENT-OWNERS"));

        EdhrBatchExecutionRespVO row = page.getList().stream()
                .filter(item -> item.getId().equals(legacyBatch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.FALSE, row.getCanClose());
        assertEquals(Boolean.FALSE, row.getCanArchive());
        assertTrue(row.getTasks().isEmpty());
        assertTrue(row.getCloseBlockers().stream()
                .anyMatch(blocker -> blocker.contains("批记录附件负责人配置无效")));
        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());
        assertEquals(Boolean.FALSE, detail.getCanClose());
        assertEquals(Boolean.FALSE, detail.getCanArchive());
        assertTrue(detail.getTasks().isEmpty());
        assertTrue(detail.getCloseBlockers().stream()
                .anyMatch(blocker -> blocker.contains("批记录附件负责人配置无效")));
    }

    @Test
    void getDetail_shouldRecoverMissingRouteProcessTasksFromFrozenRouteSnapshotAfterRouteConfigChanges() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        String frozenRouteSnapshotJson = frozenRouteSnapshotJson(route, routeProcesses);
        String changedReportId = insertReport("RPT-20-CHANGED", "表2已升版");
        MesProRouteFlowProcessBatchRecordDO secondRouteBinding = routeFlowProcessBatchRecordMapper
                .selectListByRouteProcessIdsAndUseType(List.of(routeProcesses.get(1).getId()), "BATCH")
                .get(0);
        secondRouteBinding.setBatchRecordReportId(changedReportId);
        routeFlowProcessBatchRecordMapper.updateById(secondRouteBinding);
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-DETAIL-FROZEN-ROUTE-SNAPSHOT")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-DETAIL-FROZEN-ROUTE-SNAPSHOT")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-DETAIL-FROZEN-ROUTE-SNAPSHOT")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson)
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        assertEquals(6, detail.getTaskTotal());
        assertEquals(fixture.reportId1(), routeTask(detail, 0).getBatchRecordReportId());
        assertEquals(fixture.reportId2(), routeTask(detail, 1).getBatchRecordReportId());
        assertNotEquals(changedReportId, routeTask(detail, 1).getBatchRecordReportId());
        List<MesProEdhrBatchExecutionTaskDO> persistedRouteTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId()).stream()
                        .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                        .sorted(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort))
                        .toList();
        assertEquals(List.of(fixture.reportId1(), fixture.reportId2()), persistedRouteTasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getBatchRecordReportId)
                .toList());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getDetail_shouldRecoverMissingRouteProcessTasksFromHistoricalDeletedRoute() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-DETAIL-DELETED-ROUTE")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-DETAIL-DELETED-ROUTE")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-DETAIL-DELETED-ROUTE")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());
        routeProcessMapper.deleteByRouteId(route.getId());
        routeMapper.deleteById(route.getId());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        assertEquals(6, detail.getTaskTotal());
        assertEquals(2, routeTasks(detail).size());
        assertEquals("第一工序", routeTask(detail, 0).getProcessName());
        assertEquals("第二工序", routeTask(detail, 1).getProcessName());
        List<MesProEdhrBatchExecutionTaskDO> persistedTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId());
        assertEquals(6, persistedTasks.size());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void openOrCreate_freezesActiveRouteVersionOnBatchExecution() {
        Fixture fixture = insertRouteFixture(true, true);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-EDHR-ROUTE-VERSION")
                .setRouteId(fixture.routeId()));

        assertEquals(fixture.routeVersionId(), created.getRouteVersionId());
        assertEquals(fixture.routeVersionNo(), created.getRouteVersionNo());
        MesProEdhrBatchExecutionDO persisted = batchExecutionMapper.selectById(created.getId());
        assertEquals(fixture.routeVersionId(), persisted.getRouteVersionId());
        assertEquals(fixture.routeVersionNo(), persisted.getRouteVersionNo());
        assertNotNull(persisted.getRouteSnapshotJson());
        assertTrue(persisted.getRouteSnapshotJson().contains("ROUTE-"));
    }

    @Test
    void openOrCreate_buildsInitialTasksFromActiveRouteVersionSnapshotWhenLiveBatchConfigEmpty() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId());
        String activeSnapshotJson = frozenRouteSnapshotJson(route, routeProcesses);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(fixture.routeVersionId());
        update.setRouteSnapshotJson(activeSnapshotJson);
        routeVersionMapper.updateById(update);
        routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(record -> routeFlowProcessBatchRecordMapper.deleteById(record.getId()));
        routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(config -> routeFlowProcessConfigMapper.deleteById(config.getId()));
        MesProRouteFlowConfigDO liveFlowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(route.getId(), "BATCH");
        if (liveFlowConfig != null) {
            routeFlowConfigMapper.deleteById(liveFlowConfig.getId());
        }
        assertTrue(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH").isEmpty());
        assertTrue(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH").isEmpty());
        assertNull(routeFlowConfigMapper.selectByRouteIdAndUseType(route.getId(), "BATCH"));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-EDHR-ACTIVE-SNAPSHOT")
                .setRouteId(fixture.routeId()));

        assertEquals(fixture.routeVersionId(), created.getRouteVersionId());
        assertEquals(6, created.getTaskTotal());
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(List.of(fixture.reportId1(), fixture.reportId2()),
                routeTasks.stream().map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportId).toList());
        assertEquals(activeSnapshotJson, batchExecutionMapper.selectById(created.getId()).getRouteSnapshotJson());
    }

    @Test
    void openOrCreate_buildsInitialTasksFromLegacyFlatActiveRouteVersionSnapshotWhenLiveBatchConfigEmpty() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        String activeSnapshotJson = """
                {
                  "routeId": %d,
                  "routeCode": "%s",
                  "routeName": "%s",
                  "status": 1,
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {
                          "routeProcessId": %d,
                          "processId": %d,
                          "sort": %d,
                          "processCode": "LEGACY",
                          "processName": "Legacy flat process"
                        }
                      ],
                      "edges": []
                    },
                    "batchUseConfigs": [
                      {
                        "routeProcessId": %d,
                        "processId": %d,
                        "sort": %d,
                        "useType": "BATCH",
                        "enabled": true,
                        "executionMode": "SEQUENTIAL",
                        "batchRecordReportId": "%s",
                        "productionQuantityFactor": 1.0
                      }
                    ]
                  }
                }
                """.formatted(route.getId(), route.getCode(), route.getName(),
                routeProcess.getId(), routeProcess.getProcessId(), routeProcess.getSort(),
                routeProcess.getId(), routeProcess.getProcessId(), routeProcess.getSort(), fixture.reportId1());
        com.alibaba.fastjson.JSONObject legacySnapshot = JSON.parseObject(activeSnapshotJson);
        legacySnapshot.getJSONObject("configSnapshots")
                .put("batchRecordAttachmentOwners", defaultBatchRecordAttachmentOwners());
        activeSnapshotJson = JSON.toJSONString(legacySnapshot);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(fixture.routeVersionId());
        update.setRouteSnapshotJson(activeSnapshotJson);
        routeVersionMapper.updateById(update);
        routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(record -> routeFlowProcessBatchRecordMapper.deleteById(record.getId()));
        routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(config -> routeFlowProcessConfigMapper.deleteById(config.getId()));
        MesProRouteFlowConfigDO liveFlowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(route.getId(), "BATCH");
        if (liveFlowConfig != null) {
            routeFlowConfigMapper.deleteById(liveFlowConfig.getId());
        }

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-EDHR-LEGACY-FLAT-SNAPSHOT")
                .setRouteId(fixture.routeId()));

        assertEquals(fixture.routeVersionId(), created.getRouteVersionId());
        assertEquals(5, created.getTaskTotal());
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(1, routeTasks.size());
        assertEquals(fixture.reportId1(), routeTasks.get(0).getBatchRecordReportId());
        assertEquals("MAIN", routeTasks.get(0).getFormSlotType());
        assertEquals(Boolean.TRUE, routeTasks.get(0).getRecordbookEnabled());
        assertEquals(activeSnapshotJson, batchExecutionMapper.selectById(created.getId()).getRouteSnapshotJson());
    }

    @Test
    void openOrCreate_prefersActiveRouteVersionSnapshotWhenLiveProcessConfigStale() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId());
        String activeSnapshotJson = frozenRouteSnapshotJson(route, routeProcesses);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(fixture.routeVersionId());
        update.setRouteSnapshotJson(activeSnapshotJson);
        routeVersionMapper.updateById(update);
        assertFalse(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH").isEmpty());
        routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH")
                .forEach(config -> routeFlowProcessConfigMapper.deleteById(config.getId()));
        assertTrue(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), "BATCH").isEmpty());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-EDHR-STALE-LIVE-CONFIG")
                .setRouteId(fixture.routeId()));

        assertEquals(fixture.routeVersionId(), created.getRouteVersionId());
        assertEquals(6, created.getTaskTotal());
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(List.of(fixture.reportId1(), fixture.reportId2()),
                routeTasks.stream().map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportId).toList());
        assertEquals(activeSnapshotJson, batchExecutionMapper.selectById(created.getId()).getRouteSnapshotJson());
    }

    @Test
    void openOrCreate_resolvesRouteFromWorkOrderProductWhenNoProductionTaskExists() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(fixture.routeId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ROUTE-PRODUCT-001"));

        assertEquals(fixture.routeId(), created.getRouteId());
        assertEquals(6, created.getTaskTotal());
        assertEquals(2, routeTasks(created).size());
    }

    @Test
    void openOrCreate_resolvesProductBoundRouteWhenRouteNameDiffersFromProductName() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        route.setName("球囊扩张压力泵方案");
        routeMapper.updateById(route);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ROUTE-NAME-DIFFERS-FROM-PRODUCT"));

        assertEquals(fixture.routeId(), created.getRouteId());
        assertEquals("球囊扩张压力泵方案", created.getRouteName());
        assertEquals(6, created.getTaskTotal());
    }

    @Test
    void openOrCreate_ignoresVoidedBatchAndCreatesNewBatchForSameWorkOrder() {
        Fixture fixture = insertRouteFixture(true, true);
        String batchCode = "BATCH-VOIDED-RECREATE";
        MesProEdhrBatchExecutionDO voidedBatch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("EDHRB-VOIDED-RECREATE")
                .setWorkOrderId(fixture.workOrderId())
                .setWorkOrderCode("WO-VOIDED-RECREATE")
                .setBatchCode(batchCode)
                .setProductId(fixture.productId())
                .setProductCode(String.valueOf(fixture.productId()))
                .setProductName("eDHR 路线")
                .setRouteId(fixture.routeId())
                .setRouteCode("ROUTE-VOIDED-RECREATE")
                .setRouteName("eDHR 路线")
                .setStatus(60)
                .setTaskTotal(0)
                .setTaskApprovedCount(0)
                .setBlockedCount(0);
        batchExecutionMapper.insert(voidedBatch);

        EdhrBatchExecutionRespVO recreated = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode(batchCode));

        assertNotEquals(voidedBatch.getId(), recreated.getId());
        assertEquals(fixture.workOrderId(), recreated.getWorkOrderId());
        assertEquals(fixture.routeId(), recreated.getRouteId());
        assertEquals(0, recreated.getStatus());
        assertEquals(60, batchExecutionMapper.selectById(voidedBatch.getId()).getStatus());
    }

    @Test
    void reexecuteRejectedBatch_createsNewAttemptAndKeepsOriginalRejected() {
        Fixture fixture = insertRouteFixture(true, true);
        String batchCode = "BATCH-REJECTED-REEXECUTE";
        EdhrBatchExecutionRespVO original = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode(batchCode)
                .setRouteId(fixture.routeId()));
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 7, 22, 10, 30);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(original.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED)
                .setRejectSignatureId(9901L)
                .setRejectedBy(188L)
                .setRejectedAt(rejectedAt)
                .setRejectReason("质量终态拒收，确认需要同批号重做。")
                .setAggregateHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"));

        EdhrBatchExecutionRespVO reexecuted = batchExecutionService.reexecuteRejectedBatch(
                new EdhrBatchExecutionReexecuteReqVO()
                        .setSourceRejectedBatchExecutionId(original.getId())
                        .setReason("真拒收后同生产批号重做")
                        .setRemark("同批号新执行尝试"));

        assertNotEquals(original.getId(), reexecuted.getId());
        assertEquals(2, reexecuted.getAttemptNo());
        assertEquals(original.getId(), reexecuted.getSourceRejectedBatchExecutionId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED, reexecuted.getStatus());
        assertEquals(fixture.workOrderId(), reexecuted.getWorkOrderId());
        assertEquals(fixture.routeId(), reexecuted.getRouteId());
        assertEquals(batchCode, reexecuted.getBatchCode());

        MesProEdhrBatchExecutionDO originalAfter = batchExecutionMapper.selectById(original.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED, originalAfter.getStatus());
        assertEquals(9901L, originalAfter.getRejectSignatureId());
        assertEquals(188L, originalAfter.getRejectedBy());
        assertEquals(rejectedAt, originalAfter.getRejectedAt());
        assertEquals("质量终态拒收，确认需要同批号重做。", originalAfter.getRejectReason());
        assertEquals(reexecuted.getId(), originalAfter.getSupersededByBatchExecutionId());

        MesProEdhrBatchExecutionDO newAttempt = batchExecutionMapper.selectById(reexecuted.getId());
        assertEquals(2, newAttempt.getAttemptNo());
        assertEquals(original.getId(), newAttempt.getSourceRejectedBatchExecutionId());
        assertNotNull(newAttempt.getReexecutedByChangeEventId());
        assertNotEquals(originalAfter.getActiveContextKey(), newAttempt.getActiveContextKey());
        assertTrue(newAttempt.getActiveContextKey().endsWith("|attempt=2"));

        MesProEdhrRecordChangeEventDO event =
                recordChangeEventMapper.selectById(newAttempt.getReexecutedByChangeEventId());
        assertEquals("REEXECUTE", event.getChangeType());
        assertEquals("BATCH", event.getTargetScope());
        assertEquals("EFFECTIVE", event.getChangeStatus());
        assertEquals(original.getId(), event.getBatchExecutionId());
        assertEquals(reexecuted.getId(), event.getNewExecutionId());
        assertEquals("50", event.getPreviousStatus());
        assertEquals("0", event.getNewStatus());
    }

    @Test
    void openOrCreate_allowsProductBoundRouteWhenBatchFlowConfigDisabled() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteFlowConfigDO disabledFlowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(fixture.routeId(), "BATCH");
        disabledFlowConfig.setEnabled(Boolean.FALSE);
        routeFlowConfigMapper.updateById(disabledFlowConfig);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-FLOW-ROUTE-001"));

        assertEquals(fixture.routeId(), created.getRouteId());
        assertEquals(6, created.getTaskTotal());
        assertEquals(2, routeTasks(created).size());
    }

    @Test
    void openOrCreate_rejectsExplicitRouteWhenProductProjectRouteDiffers() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO otherRoute = insertExecutableRoute("其它项目路线", "RPT-OTHER");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-EXPLICIT-OTHER-PROJECT-ROUTE")
                        .setRouteId(otherRoute.getId())));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_ROUTE_MISMATCH.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("其它项目路线")
                || exception.getMessage().contains(otherRoute.getCode()));
    }

    @Test
    void openOrCreate_allowsExplicitRouteWhenBatchFlowConfigDisabled() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteFlowConfigDO disabledFlowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(fixture.routeId(), "BATCH");
        disabledFlowConfig.setEnabled(Boolean.FALSE);
        routeFlowConfigMapper.updateById(disabledFlowConfig);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setRouteId(fixture.routeId())
                .setBatchCode("BATCH-EXPLICIT-DISABLED-FLOW"));

        assertEquals(fixture.routeId(), created.getRouteId());
        assertEquals(6, created.getTaskTotal());
        assertEquals(2, routeTasks(created).size());
    }

    @Test
    void openOrCreate_reopensExistingBatchWhenCurrentBatchFlowDisabled() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(fixture.routeId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());
        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(
                new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setRouteId(fixture.routeId())
                        .setBatchCode("BATCH-EXISTING-DISABLED-FLOW"));
        MesProRouteFlowConfigDO disabledFlowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(fixture.routeId(), "BATCH");
        disabledFlowConfig.setEnabled(Boolean.FALSE);
        routeFlowConfigMapper.updateById(disabledFlowConfig);

        EdhrBatchExecutionRespVO reopened = batchExecutionService.openOrCreate(
                new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-EXISTING-DISABLED-FLOW"));

        assertEquals(created.getId(), reopened.getId());
        assertEquals(fixture.routeId(), reopened.getRouteId());
    }

    @Test
    void openOrCreate_rejectsExplicitRouteWhenBindingBelongsToDisabledProcessConfig() {
        Fixture fixture = insertRouteFixture(true, false);
        replaceEnabledProcessConfigWithoutMovingBinding(fixture.routeId());

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setRouteId(fixture.routeId())
                .setBatchCode("BATCH-EXPLICIT-STALE-BINDING")),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void openOrCreate_rejectsRouteWhenOnlyBindingBelongsToDisabledProcessConfig() {
        Fixture fixture = insertRouteFixture(true, false);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(fixture.routeId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());
        replaceEnabledProcessConfigWithoutMovingBinding(fixture.routeId());

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-IMPLICIT-STALE-BINDING")),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void openOrCreate_rejectsExplicitDisabledBaseRoute() {
        Fixture fixture = insertRouteFixture(true, false);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        route.setStatus(CommonStatusEnum.DISABLE.getStatus());
        routeMapper.updateById(route);

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setRouteId(fixture.routeId())
                .setBatchCode("BATCH-EXPLICIT-DISABLED-ROUTE")),
                PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_BINDING_REQUIRED);
    }

    @Test
    void openOrCreate_ignoresDisabledProductRouteWhenResolvingRouteFallback() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(fixture.routeId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());
        MesProRouteDO disabledRoute = MesProRouteDO.builder()
                .code("ROUTE-DISABLED-" + randomLongId())
                .name("关闭路线")
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        routeMapper.insert(disabledRoute);
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(disabledRoute.getId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ENABLED-PRODUCT-ROUTE"));

        assertEquals(fixture.routeId(), created.getRouteId());
        assertEquals(6, created.getTaskTotal());
        assertEquals(2, routeTasks(created).size());
    }

    @Test
    void getPage_includesCurrentExecutableProcessNameWithoutUsingBatchCode() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CURRENT-PROCESS")
                .setRouteId(fixture.routeId()));
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(batch);
        bindFillCompletedExecution(routeTasks.get(0).getId(), 88001L);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(routeTasks.get(1).getId())
                .setExecutionId(88002L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(88002L)
                .setExecutionCode("BRE-88002")
                .setWorkOrderId(fixture.workOrderId())
                .setWorkOrderCode("WO-CURRENT-PROCESS")
                .setBatchCode("BATCH-CURRENT-PROCESS")
                .setStatus(0)
                .setSheetLayoutJson("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"第二工序\"}}}}}")
                .setMetaJson("{\"tableTitle\":\"第二工序\"}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setDomainTraceStatus("VERIFIED"));

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(new EdhrBatchExecutionPageReqVO()
                .setBatchCode("BATCH-CURRENT-PROCESS"));

        EdhrBatchExecutionRespVO row = page.getList().stream()
                .filter(item -> item.getId().equals(batch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(routeTasks.get(1).getProcessName(), row.getCurrentProcessName());
        assertEquals(routeTasks.get(1).getProcessCode(), row.getCurrentProcessCode());
        assertFalse(row.getBatchCode().equals(row.getCurrentProcessName()));
        assertFalse(row.getCurrentProcessName().startsWith("ER"));
    }

    @Test
    void getPage_doesNotSynchronizeTaskStatusOrMutateBatchData() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-READ-ONLY")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO task = routeTask(batch, 0);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(task.getId())
                .setExecutionId(88003L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(88003L)
                .setExecutionCode("BRE-88003")
                .setWorkOrderId(fixture.workOrderId())
                .setWorkOrderCode("WO-READ-ONLY")
                .setBatchCode("BATCH-READ-ONLY")
                .setStatus(3)
                .setSheetLayoutJson("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"只读查询\"}}}}}")
                .setMetaJson("{\"tableTitle\":\"只读查询\"}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setDomainTraceStatus("VERIFIED"));
        MesProEdhrBatchExecutionDO before = batchExecutionMapper.selectById(batch.getId());

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(
                new EdhrBatchExecutionPageReqVO().setBatchCode("BATCH-READ-ONLY"));

        assertTrue(page.getList().stream().anyMatch(item -> item.getId().equals(batch.getId())));
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT,
                batchTaskMapper.selectById(task.getId()).getStatus());
        MesProEdhrBatchExecutionDO after = batchExecutionMapper.selectById(batch.getId());
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getTaskApprovedCount(), after.getTaskApprovedCount());
        assertEquals(before.getBlockedCount(), after.getBlockedCount());
    }

    @Test
    void getPage_includesCurrentProcessProductionEquipmentQualityFillers() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CURRENT-FILLERS")
                .setRouteId(fixture.routeId()));
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(batch);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(routeTasks.get(0).getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(routeTasks.get(1).getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT));
        Long currentRouteProcessId = routeTasks.get(1).getRouteProcessId();
        insertCurrentProcessFillRule(currentRouteProcessId, fixture.reportId2(), "FILL", 101L);
        insertCurrentProcessFillRule(currentRouteProcessId, fixture.reportId2(), "EQUIPMENT_FILL", 102L);
        insertCurrentProcessFillRule(currentRouteProcessId, fixture.reportId2(), "QUALITY_FILL", "DEPT_LEADER", "9103");
        DeptRespDTO qualityDept = new DeptRespDTO();
        qualityDept.setId(9103L);
        qualityDept.setLeaderUserId(103L);
        qualityDept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(deptApi.getDeptList(List.of(9103L))).thenReturn(List.of(qualityDept));
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(
                adminUser(101L, "生产填写人", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserList(List.of(102L))).thenReturn(List.of(
                adminUser(102L, "设备填写人", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserList(List.of(103L))).thenReturn(List.of(
                adminUser(103L, "质量填写人", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                101L, adminUser(101L, "生产填写人", CommonStatusEnum.ENABLE.getStatus()),
                102L, adminUser(102L, "设备填写人", CommonStatusEnum.ENABLE.getStatus()),
                103L, adminUser(103L, "质量填写人", CommonStatusEnum.ENABLE.getStatus()),
                10001L, adminUser(10001L, "附件负责人", CommonStatusEnum.ENABLE.getStatus())));

        PageResult<EdhrBatchExecutionRespVO> page = batchExecutionService.getPage(new EdhrBatchExecutionPageReqVO()
                .setBatchCode("BATCH-CURRENT-FILLERS"));

        EdhrBatchExecutionRespVO row = page.getList().stream()
                .filter(item -> item.getId().equals(batch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(currentRouteProcessId, row.getCurrentProcessRouteProcessId());
        assertEquals(routeTasks.get(1).getProcessName(), row.getCurrentProcessName());
        assertEquals(List.of("生产填写人"), row.getCurrentProcessProductionFillers().stream()
                .map(EdhrBatchExecutionRespVO.CurrentProcessFiller::getDisplayName)
                .toList());
        assertEquals(List.of("设备填写人"), row.getCurrentProcessEquipmentFillers().stream()
                .map(EdhrBatchExecutionRespVO.CurrentProcessFiller::getDisplayName)
                .toList());
        assertEquals(List.of("质量填写人"), row.getCurrentProcessQualityFillers().stream()
                .map(EdhrBatchExecutionRespVO.CurrentProcessFiller::getDisplayName)
                .toList());
    }

    @Test
    void getPage_filtersNonOverviewUserByCurrentProcessFillers() {
        stubCurrentFillerUsers();
        when(permissionApi.hasAnyPermissions(10001L, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION))
                .thenReturn(false);
        VisibleBatchFixture mine = openBatchWithSecondProcessCurrentFillers("BATCH-VISIBLE-MINE", 10001L);
        VisibleBatchFixture others = openBatchWithSecondProcessCurrentFillers("BATCH-VISIBLE-OTHER", 10002L);
        VisibleBatchFixture none = openBatchWithSecondProcessCurrentFillers("BATCH-VISIBLE-NONE", null);
        none.currentTask().setBatchRecordReportId("MISSING-REPORT-FOR-VISIBILITY");
        batchTaskMapper.updateById(none.currentTask());

        PageResult<EdhrBatchExecutionRespVO> page;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            page = batchExecutionService.getPage(new EdhrBatchExecutionPageReqVO()
                    .setBatchCode("BATCH-VISIBLE-"));
        }

        assertEquals(1L, page.getTotal());
        assertEquals(List.of(mine.batch().getId()), page.getList().stream()
                .map(EdhrBatchExecutionRespVO::getId)
                .toList());
        assertFalse(page.getList().stream().anyMatch(row -> Objects.equals(row.getId(), others.batch().getId())));
        assertFalse(page.getList().stream().anyMatch(row -> Objects.equals(row.getId(), none.batch().getId())));
    }

    @Test
    void readOnlyDetails_requireCurrentFillerVisibilityForNonOverviewUser() {
        stubCurrentFillerUsers();
        when(permissionApi.hasAnyPermissions(10001L, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION))
                .thenReturn(false);
        VisibleBatchFixture visible = openBatchWithSecondProcessCurrentFillers("BATCH-DETAIL-VISIBLE", 10001L);
        VisibleBatchFixture hidden = openBatchWithSecondProcessCurrentFillers("BATCH-DETAIL-HIDDEN", 10002L);
        when(jimuReportGateway.getReportJson(any())).thenReturn("{\"sheets\":[]}");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);

            assertEquals(visible.batch().getId(), batchExecutionService.get(visible.batch().getId()).getId());
            assertEquals(visible.batch().getId(), batchWorkbenchService.getWorkbench(visible.batch().getId()).getBatchExecutionId());
            assertEquals(visible.batch().getId(), batchExecutionService.getReviewTimeline(visible.batch().getId()).getBatchExecutionId());
            assertEquals(visible.batch().getId(),
                    batchExecutionService.previewTask(visible.batch().getId(), visible.currentTask().getId())
                            .getBatchExecutionId());

            assertServiceException(() -> batchExecutionService.get(hidden.batch().getId()),
                    PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE);
            assertServiceException(() -> batchWorkbenchService.getWorkbench(hidden.batch().getId()),
                    PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE);
            assertServiceException(() -> batchExecutionService.getReviewTimeline(hidden.batch().getId()),
                    PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE);
            assertServiceException(() -> batchExecutionService.previewTask(hidden.batch().getId(), hidden.currentTask().getId()),
                    PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE);
        }
    }

    @Test
    void getPageAndDetails_allowOverviewPermissionToSeeAllBatches() {
        stubCurrentFillerUsers();
        VisibleBatchFixture mine = openBatchWithSecondProcessCurrentFillers("BATCH-OVERVIEW-MINE", 10001L);
        VisibleBatchFixture others = openBatchWithSecondProcessCurrentFillers("BATCH-OVERVIEW-OTHER", 10002L);
        VisibleBatchFixture none = openBatchWithSecondProcessCurrentFillers("BATCH-OVERVIEW-NONE", null);
        when(permissionApi.hasAnyPermissions(9001L, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION))
                .thenReturn(true);

        PageResult<EdhrBatchExecutionRespVO> page;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            page = batchExecutionService.getPage(new EdhrBatchExecutionPageReqVO()
                    .setBatchCode("BATCH-OVERVIEW-"));
            assertEquals(others.batch().getId(), batchExecutionService.get(others.batch().getId()).getId());
            assertEquals(none.batch().getId(), batchExecutionService.get(none.batch().getId()).getId());
        }

        assertEquals(3L, page.getTotal());
        assertEquals(List.of(none.batch().getId(), others.batch().getId(), mine.batch().getId()),
                page.getList().stream().map(EdhrBatchExecutionRespVO::getId).toList());
    }

    @Test
    void openOrCreate_resolvesLatestApprovedRouteBindingReportAndShowsCurrentFillersToReadonlyViewer() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        MesProRouteFlowProcessBatchRecordDO binding =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                                List.of(routeProcess.getId()), "BATCH").get(0);
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO oldVersion = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        MesProBatchRecordVersionDO latestApprovedVersion =
                insertBatchRecordVersion(definitionId, "V2.0", "APPROVED");
        MesProBatchRecordVersionDO pendingVersion =
                insertBatchRecordVersion(definitionId, "V3.0", "PENDING_APPROVAL");
        MesProBatchRecordReportDO oldReport = reportMapper.selectByReportId(fixture.reportId1());
        oldReport.setBatchRecordDefinitionId(definitionId);
        oldReport.setBatchRecordVersionId(oldVersion.getId());
        oldReport.setFormSlotType("MAIN");
        oldReport.setSourceTableIndex(2);
        oldReport.setReportName("粗洗工序生产记录");
        oldReport.setTableTitle("粗洗工序生产记录");
        reportMapper.updateById(oldReport);
        String latestReportId = insertVersionedReport("RPT-LATEST-APPROVED", "粗洗工序生产记录",
                definitionId, latestApprovedVersion.getId(), 2, "MAIN");
        String pendingReportId = insertVersionedReport("RPT-PENDING", "粗洗工序生产记录",
                definitionId, pendingVersion.getId(), 2, "MAIN");
        binding.setBatchRecordReportId(fixture.reportId1());
        binding.setBatchRecordDefinitionId(definitionId);
        binding.setBatchRecordVersionId(oldVersion.getId());
        routeFlowProcessBatchRecordMapper.updateById(binding);
        insertCurrentProcessFillRule(0L, latestReportId, "FILL", "USERS", "149");
        when(adminUserApi.getUserList(List.of(149L))).thenReturn(List.of(
                adminUser(149L, "黎敏", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                149L, adminUser(149L, "黎敏", CommonStatusEnum.ENABLE.getStatus()),
                10001L, adminUser(10001L, "附件负责人", CommonStatusEnum.ENABLE.getStatus())));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-LATEST-APPROVED-FILLER")
                .setRouteId(fixture.routeId()));

        EdhrBatchExecutionTaskRespVO routeTask = routeTask(created, 0);
        assertEquals(latestReportId, routeTask.getBatchRecordReportId());
        assertEquals(latestApprovedVersion.getId(), routeTask.getBatchRecordVersionId());
        assertNotEquals(fixture.reportId1(), routeTask.getBatchRecordReportId());
        assertNotEquals(pendingReportId, routeTask.getBatchRecordReportId());
        assertEquals(List.of("黎敏"), created.getCurrentProcessProductionFillers().stream()
                .map(EdhrBatchExecutionRespVO.CurrentProcessFiller::getDisplayName)
                .toList());
        assertTrue(routeTask.getAllowedActions() == null
                || !routeTask.getAllowedActions().contains("OPEN_FORM"));
    }

    @Test
    void getDetail_resolvesLatestApprovedRouteBindingReportFromFrozenRouteSnapshot() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        MesProRouteProcessDO firstRouteProcess = routeProcesses.get(0);
        MesProRouteFlowProcessBatchRecordDO binding =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        List.of(firstRouteProcess.getId()), "BATCH").get(0);
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO oldVersion = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        MesProBatchRecordVersionDO latestApprovedVersion =
                insertBatchRecordVersion(definitionId, "V2.0", "APPROVED");
        MesProBatchRecordReportDO oldReport = reportMapper.selectByReportId(fixture.reportId1());
        oldReport.setBatchRecordDefinitionId(definitionId);
        oldReport.setBatchRecordVersionId(oldVersion.getId());
        oldReport.setFormSlotType("MAIN");
        oldReport.setSourceTableIndex(3);
        oldReport.setReportName("粗洗工序生产记录");
        oldReport.setTableTitle("粗洗工序生产记录");
        reportMapper.updateById(oldReport);
        String latestReportId = insertVersionedReport("RPT-FROZEN-LATEST-APPROVED", "粗洗工序生产记录",
                definitionId, latestApprovedVersion.getId(), 3, "MAIN");
        binding.setBatchRecordReportId(fixture.reportId1());
        binding.setBatchRecordDefinitionId(definitionId);
        binding.setBatchRecordVersionId(oldVersion.getId());
        binding.setFormSlotType("MAIN");
        routeFlowProcessBatchRecordMapper.updateById(binding);
        String frozenRouteSnapshotJson = frozenRouteSnapshotJson(route, routeProcesses);
        insertCurrentProcessFillRule(0L, latestReportId, "FILL", "USERS", "149");
        when(adminUserApi.getUserList(List.of(149L))).thenReturn(List.of(
                adminUser(149L, "黎敏", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                149L, adminUser(149L, "黎敏", CommonStatusEnum.ENABLE.getStatus()),
                10001L, adminUser(10001L, "附件负责人", CommonStatusEnum.ENABLE.getStatus())));
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-FROZEN-LATEST-REPORT")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-FROZEN-LATEST-REPORT")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-FROZEN-LATEST-REPORT")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson)
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(4)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        EdhrBatchExecutionTaskRespVO routeTask = routeTask(detail, 0);
        assertEquals(latestReportId, routeTask.getBatchRecordReportId());
        assertEquals(latestApprovedVersion.getId(), routeTask.getBatchRecordVersionId());
        assertEquals(List.of("黎敏"), detail.getCurrentProcessProductionFillers().stream()
                .map(EdhrBatchExecutionRespVO.CurrentProcessFiller::getDisplayName)
                .toList());
        verify(workTaskService).createInitialFillTask(argThat(batch -> Objects.equals(batch.getId(), legacyBatch.getId())));
    }

    @Test
    void getDetail_showsLatestCurrentFillersForExistingOldVersionRouteTaskWithoutMigratingTask() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        MesProRouteProcessDO firstRouteProcess = routeProcesses.get(0);
        MesProProcessDO firstProcess = processMapper.selectById(firstRouteProcess.getProcessId());
        MesProRouteFlowProcessBatchRecordDO binding =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        List.of(firstRouteProcess.getId()), "BATCH").get(0);
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO oldVersion = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        MesProBatchRecordVersionDO latestApprovedVersion =
                insertBatchRecordVersion(definitionId, "V2.0", "APPROVED");
        MesProBatchRecordReportDO oldReport = reportMapper.selectByReportId(fixture.reportId1());
        oldReport.setBatchRecordDefinitionId(definitionId);
        oldReport.setBatchRecordVersionId(oldVersion.getId());
        oldReport.setFormSlotType("MAIN");
        oldReport.setSourceTableIndex(4);
        oldReport.setReportName("粗洗工序生产记录");
        oldReport.setTableTitle("粗洗工序生产记录");
        reportMapper.updateById(oldReport);
        String latestReportId = insertVersionedReport("RPT-EXISTING-OLD-TASK-LATEST", "粗洗工序生产记录",
                definitionId, latestApprovedVersion.getId(), 4, "MAIN");
        binding.setBatchRecordReportId(fixture.reportId1());
        binding.setBatchRecordDefinitionId(definitionId);
        binding.setBatchRecordVersionId(oldVersion.getId());
        binding.setFormSlotType("MAIN");
        routeFlowProcessBatchRecordMapper.updateById(binding);
        insertCurrentProcessFillRule(0L, latestReportId, "FILL", "USERS", "149");
        when(adminUserApi.getUserList(List.of(149L))).thenReturn(List.of(
                adminUser(149L, "黎敏", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                149L, adminUser(149L, "黎敏", CommonStatusEnum.ENABLE.getStatus()),
                10001L, adminUser(10001L, "附件负责人", CommonStatusEnum.ENABLE.getStatus())));
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-EXISTING-OLD-TASK")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-EXISTING-OLD-TASK")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-EXISTING-OLD-TASK")
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcesses))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(5)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        insertLegacySpecialOnlyTasks(legacyBatch.getId());
        batchTaskMapper.insert(MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(legacyBatch.getId())
                .nodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .routeProcessId(firstRouteProcess.getId())
                .routeProcessSort(firstRouteProcess.getSort())
                .processId(firstRouteProcess.getProcessId())
                .processCode(firstProcess.getCode())
                .processName(firstProcess.getName())
                .batchRecordReportId(fixture.reportId1())
                .batchRecordReportName(oldReport.getReportName())
                .batchRecordDefinitionId(definitionId)
                .batchRecordVersionId(oldVersion.getId())
                .batchRecordSort(1)
                .executionMode("SEQUENTIAL")
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .permissionScopeId(5001L)
                .routeBindingId(binding.getId())
                .routeBindingSnapshotHash("2222222222222222222222222222222222222222222222222222222222222222")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT)
                .requiredFlag(Boolean.TRUE)
                .build());

        EdhrBatchExecutionRespVO detail = batchExecutionService.get(legacyBatch.getId());

        assertEquals(fixture.reportId1(), routeTask(detail, 0).getBatchRecordReportId());
        assertEquals(oldVersion.getId(), routeTask(detail, 0).getBatchRecordVersionId());
        assertEquals(List.of("黎敏"), detail.getCurrentProcessProductionFillers().stream()
                .map(EdhrBatchExecutionRespVO.CurrentProcessFiller::getDisplayName)
                .toList());
        List<MesProEdhrBatchExecutionTaskDO> persistedRouteTasks =
                batchTaskMapper.selectListByBatchExecutionId(legacyBatch.getId()).stream()
                        .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                        .toList();
        assertEquals(1, persistedRouteTasks.size());
        assertEquals(fixture.reportId1(), persistedRouteTasks.get(0).getBatchRecordReportId());
        verify(workTaskService, never()).createInitialFillTask(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void openOrCreate_mustGenerateSpecialNoTemplateNodesAroundRouteForms() {
        Fixture fixture = insertRouteFixture(true, true);

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-NODES")
                .setRouteId(fixture.routeId()));

        List<String> actualNodeTypes = batch.getTasks().stream()
                .map(task -> task.getBatchRecordReportId() == null ? task.getProcessCode() : "ROUTE_FORM")
                .toList();
        assertEquals(List.of(
                "INCOMING_INSPECTION_REPORT",
                "ROUTE_FORM",
                "ROUTE_FORM",
                "STERILIZATION_REPORT",
                "FINISHED_PRODUCT_INSPECTION_REPORT",
                "FINISHED_PRODUCT_INSPECTION_RECORD"), actualNodeTypes,
                "eDHR 批次任务必须按 T1-v2 契约生成四类特殊无模板节点，并把普通路线表单夹在来料检与灭菌报告之间。");
    }

    @Test
    void openOrCreateFromScheduleCompletion_isIdempotentAndKeepsSpecialNodeOrder() {
        Fixture fixture = insertRouteFixture(true, true);
        insertInitialFillAssignmentRule(fixture.routeId());
        EdhrScheduleCompletionCreateCommand command = new EdhrScheduleCompletionCreateCommand()
                .setScheduleOrderId(7001L)
                .setScheduleOrderCode("SCH-T4-001")
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-T4-SCHEDULE")
                .setProductId(fixture.productId())
                .setRouteId(fixture.routeId());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreateFromScheduleCompletion(command);
        EdhrBatchExecutionRespVO reopened = batchExecutionService.openOrCreateFromScheduleCompletion(command);

        assertEquals(created.getId(), reopened.getId());
        assertEquals(List.of(
                "INCOMING_INSPECTION_REPORT",
                "ROUTE_FORM",
                "ROUTE_FORM",
                "STERILIZATION_REPORT",
                "FINISHED_PRODUCT_INSPECTION_REPORT",
                "FINISHED_PRODUCT_INSPECTION_RECORD"), created.getTasks().stream()
                .map(task -> task.getBatchRecordReportId() == null ? task.getProcessCode() : "ROUTE_FORM")
                .toList());
        verify(workTaskService).createInitialFillTask(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void openOrCreateFromScheduleCompletion_missingCommandPrerequisitesReturnsList() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreateFromScheduleCompletion(new EdhrScheduleCompletionCreateCommand()));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("工单"));
        assertTrue(ex.getMessage().contains("批次号"));
        assertTrue(ex.getMessage().contains("产品"));
        assertTrue(ex.getMessage().contains("工艺路线"));
    }

    @Test
    void openOrCreateFromScheduleCompletion_missingBindingAndInitialCandidateReturnsList() {
        Fixture noBindingFixture = insertRouteFixture(false, false);

        ServiceException bindingEx = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreateFromScheduleCompletion(new EdhrScheduleCompletionCreateCommand()
                        .setWorkOrderId(noBindingFixture.workOrderId())
                        .setBatchCode("BATCH-T4-NO-BINDING")
                        .setProductId(noBindingFixture.productId())
                        .setRouteId(noBindingFixture.routeId())));
        assertEquals(PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING.getCode(), bindingEx.getCode());
        assertTrue(bindingEx.getMessage().contains("工序与批记录绑定"));

        Fixture noCandidateFixture = insertRouteFixture(true, true);
        ServiceException candidateEx = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreateFromScheduleCompletion(new EdhrScheduleCompletionCreateCommand()
                        .setWorkOrderId(noCandidateFixture.workOrderId())
                        .setBatchCode("BATCH-T4-NO-CANDIDATE")
                        .setProductId(noCandidateFixture.productId())
                        .setRouteId(noCandidateFixture.routeId())));
        assertEquals(PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING.getCode(), candidateEx.getCode());
        assertTrue(candidateEx.getMessage().contains("首任务责任来源/候选池"));
    }

    @Test
    void openOrCreateFromScheduleCompletion_reportsBindingMissingWhenBindingBelongsToDisabledProcessConfig() {
        Fixture fixture = insertRouteFixture(true, false);
        insertInitialFillAssignmentRule(fixture.routeId());
        replaceEnabledProcessConfigWithoutMovingBinding(fixture.routeId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreateFromScheduleCompletion(new EdhrScheduleCompletionCreateCommand()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-SCHEDULE-STALE-BINDING")
                        .setProductId(fixture.productId())
                        .setRouteId(fixture.routeId())));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("工序与批记录绑定"));
    }

    @Test
    void ordinaryRouteForm_skipMustFailWithExplicitMessage() throws Exception {
        String serviceContract = readSource("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrBatchExecutionService.java");
        String serviceImpl = readSource("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrBatchExecutionServiceImpl.java");
        String errorConstants = readSource("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrBatchExecutionErrorCodeConstants.java");

        assertTrue(serviceContract.contains("skipSpecialNode"),
                "T3 必须提供特殊节点跳过 API；普通 ROUTE_FORM 调用该 API 时要 fail fast。");
        assertTrue(serviceContract.contains("String reason, String password"),
                "T3 特殊节点跳过必须显式要求原因和签名密码。");
        assertTrue((serviceImpl + errorConstants).contains("必填路线表单不允许跳过"),
                "必填 ROUTE_FORM 调用跳过 API 时必须失败并提示“必填路线表单不允许跳过”。");
        assertTrue(serviceImpl.contains("特殊节点跳过必须填写原因和签名密码"),
                "特殊节点跳过缺少原因或密码时必须失败并提示。");
        assertTrue(serviceImpl.contains("ROUTE_FORM"),
                "跳过逻辑必须显式区分普通 ROUTE_FORM 与四类允许跳过的特殊节点。");
    }

    @Test
    void requiredRouteForm_skipMustStillFailFast() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-REQUIRED-ROUTE-SKIP")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO requiredTask = routeTask(batch, 0);
        insertFillWorkTask(batch, requiredTask, fixture);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.skipSpecialNode(requiredTask.getId(),
                            "必填表单不得跳过", "secret", List.of()));
            assertEquals(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID.getCode(), exception.getCode());
        }

        MesProEdhrBatchExecutionTaskDO persisted = batchTaskMapper.selectById(requiredTask.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING, persisted.getStatus());
        assertEquals(Boolean.TRUE, persisted.getRequiredFlag());
    }

    @Test
    void batchTaskCardMustHideWorkTaskNoiseWhenFormHasNoActiveFillTask() throws Exception {
        String serviceImpl = readSource("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrBatchExecutionServiceImpl.java");

        assertFalse(serviceImpl.contains("当前节点没有待处理任务"),
                "批次执行填写卡片不得把工作任务内部状态暴露成填写人要处理的文案。");
        assertTrue(serviceImpl.contains("taskGate.available() ? null : taskGate.message()"),
                "门禁通过但没有 active work task 时应保持卡片安静，只禁用操作，不显示待办噪音。");
    }

    @Test
    void specialNodeSkip_requiresReasonAndPasswordAndRecordsSignature() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-SKIP-AUDIT")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO specialNode = batch.getTasks().stream()
                .filter(task -> task.getBatchRecordReportId() == null)
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        doThrow(new ServiceException(USER_PASSWORD_FAILED))
                .when(adminUserApi).validatePassword(188L, "wrong-pass");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            ServiceException reasonException = assertThrows(ServiceException.class,
                    () -> batchExecutionService.skipSpecialNode(specialNode.getId(), "", "wrong-pass", List.of()));
            assertEquals(BAD_REQUEST.getCode(), reasonException.getCode());

            ServiceException passwordException = assertThrows(ServiceException.class,
                    () -> batchExecutionService.skipSpecialNode(specialNode.getId(), "现场确认后跳过", "wrong-pass", List.of()));
            assertEquals(USER_PASSWORD_FAILED.getCode(), passwordException.getCode());
        }

        clearInvocations(workTaskService);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            EdhrBatchExecutionRespVO skipped = batchExecutionService.skipSpecialNode(
                    specialNode.getId(), "现场确认后跳过", "secret", List.of());
            assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED,
                    skipped.getTasks().stream()
                            .filter(task -> task.getId().equals(specialNode.getId()))
                            .findFirst()
                            .orElseThrow()
                            .getStatus());
            MesProEdhrBatchExecutionTaskDO updatedTask = batchTaskMapper.selectById(specialNode.getId());
            assertTrue(updatedTask.getSpecialPayloadJson().contains("\"skipReason\":\"现场确认后跳过\""));
            assertTrue(updatedTask.getSpecialPayloadJson().contains("\"skipSignatureId\":"));
            assertEquals(1, batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).stream()
                    .filter(signature -> "SPECIAL_NODE_SKIP".equals(signature.getActionType()))
                    .count());
            verify(workTaskService).createNextFillAfterSpecialNodeResolved(argThat(task ->
                    task != null
                            && specialNode.getId().equals(task.getId())
                            && Objects.equals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED,
                            task.getStatus())));
        }
    }

    @Test
    void specialNodeWriteApis_requireConfiguredAttachmentOwnerInsteadOfCloseOwner() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-ATTACHMENT-OWNER")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO specialNode = batch.getTasks().stream()
                .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT
                        .equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        configureBatchSpecialAttachmentOwners(batch.getId(), 188L, 190L);
        insertCloseAssignmentRule(fixture.routeId(), 189L);
        byte[] content = "incoming inspection attachment".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(189L);

            ServiceException skipException = assertThrows(ServiceException.class,
                    () -> batchExecutionService.skipSpecialNode(specialNode.getId(),
                            "关闭负责人不能替代附件填写人", "secret", List.of()));
            assertEquals(PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID.getCode(), skipException.getCode());

            ServiceException completeException = assertThrows(ServiceException.class,
                    () -> batchExecutionService.completeSpecialNode(specialNode.getId(), null, List.of()));
            assertEquals(PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID.getCode(), completeException.getCode());

            ServiceException uploadException = assertThrows(ServiceException.class,
                    () -> batchExecutionService.prepareSpecialNodeAttachmentUpload(
                            new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                                    .setTaskId(specialNode.getId())
                            .setFileName("incoming.pdf")
                            .setContentType("application/pdf")
                            .setContent(content)));
            assertEquals(PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID.getCode(), uploadException.getCode());
        }

        String directory = "edhr/special-nodes/" + batch.getId() + "/" + specialNode.getId() + "/attachments";
        when(fileService.createFileAndReturnId(content, "incoming.pdf", directory, "application/pdf"))
                .thenReturn(9301L);
        when(fileService.getFile(9301L)).thenReturn(FileDO.builder()
                .id(9301L)
                .configId(28L)
                .name("incoming.pdf")
                .path(directory + "/incoming.pdf")
                .url("http://127.0.0.1:9000/yudao/" + directory + "/incoming.pdf")
                .type("application/pdf")
                .size((long) content.length)
                .build());
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);

            MesProEdhrSpecialNodeAttachmentPrepareUploadResult upload =
                    batchExecutionService.prepareSpecialNodeAttachmentUpload(
                            new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                                    .setTaskId(specialNode.getId())
                                    .setFileName("incoming.pdf")
                                    .setContentType("application/pdf")
                                    .setContent(content));
            assertEquals(9301L, upload.getFileId());

            EdhrBatchExecutionRespVO completed =
                    batchExecutionService.completeSpecialNode(specialNode.getId(), null, List.of());
            assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED,
                    completed.getTasks().stream()
                            .filter(task -> task.getId().equals(specialNode.getId()))
                            .findFirst()
                            .orElseThrow()
                            .getStatus());
        }
    }

    @Test
    void sterilizationReport_completeWithoutBatchNoMustFail() throws Exception {
        String serviceContract = readSource("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrBatchExecutionService.java");
        String serviceImpl = readSource("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                + "MesProEdhrBatchExecutionServiceImpl.java");

        assertTrue(serviceContract.contains("completeSpecialNode"),
                "T3 必须提供特殊节点完成 API，用于灭菌报告填写完成路径。");
        assertTrue(serviceImpl.contains("sterilizationBatchNo"),
                "灭菌报告完成 API 必须接收并持久化 sterilizationBatchNo。");
        assertTrue(serviceImpl.contains("灭菌批次必填"),
                "灭菌报告选择完成但 sterilizationBatchNo 为空时必须失败并提示“灭菌批次必填”。");
    }

    @Test
    void prepareSpecialNodeAttachmentUpload_returnsTaskScopedMetadata() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-UPLOAD")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO incoming = batch.getTasks().stream()
                .filter(task -> "INCOMING_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        byte[] content = "incoming inspection attachment".getBytes(StandardCharsets.UTF_8);
        String directory = "edhr/special-nodes/" + batch.getId() + "/" + incoming.getId() + "/attachments";
        when(fileService.createFileAndReturnId(content, "incoming.pdf", directory, "application/pdf"))
                .thenReturn(9201L);
        when(fileService.getFile(9201L)).thenReturn(FileDO.builder()
                .id(9201L)
                .configId(28L)
                .name("incoming.pdf")
                .path(directory + "/incoming.pdf")
                .url("http://127.0.0.1:9000/yudao/" + directory + "/incoming.pdf")
                .type("application/pdf")
                .size((long) content.length)
                .build());

        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            result = batchExecutionService.prepareSpecialNodeAttachmentUpload(
                    new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                            .setTaskId(incoming.getId())
                            .setFileName("incoming.pdf")
                            .setContentType("application/pdf")
                            .setContent(content));
        }

        assertEquals(9201L, result.getFileId());
        assertEquals(28L, result.getStorageConfigId());
        assertEquals(directory + "/incoming.pdf", result.getStoragePath());
        assertEquals("incoming.pdf", result.getFileName());
        assertEquals("application/pdf", result.getContentType());
        assertEquals(content.length, result.getFileSize());
        assertTrue(result.getUploadToken().startsWith("EDHR_SPECIAL_NODE_ATTACHMENT:"));
        assertTrue(result.getUploadToken().contains(String.valueOf(incoming.getId())));
    }

    @Test
    void prepareSpecialNodeAttachmentUpload_persistsPendingAttachmentForReload() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-UPLOAD-RELOAD")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO incoming = batch.getTasks().stream()
                .filter(task -> "INCOMING_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        byte[] content = "incoming pending reload attachment".getBytes(StandardCharsets.UTF_8);
        String directory = "edhr/special-nodes/" + batch.getId() + "/" + incoming.getId() + "/attachments";
        when(fileService.createFileAndReturnId(content, "incoming-reload.pdf", directory, "application/pdf"))
                .thenReturn(9211L);
        when(fileService.getFile(9211L)).thenReturn(FileDO.builder()
                .id(9211L)
                .configId(28L)
                .name("incoming-reload.pdf")
                .path(directory + "/incoming-reload.pdf")
                .url("http://127.0.0.1:9000/yudao/" + directory + "/incoming-reload.pdf")
                .type("application/pdf")
                .size((long) content.length)
                .build());

        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            result = batchExecutionService.prepareSpecialNodeAttachmentUpload(
                    new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                            .setTaskId(incoming.getId())
                            .setFileName("incoming-reload.pdf")
                            .setContentType("application/pdf")
                            .setContent(content));
        }

        EdhrBatchExecutionTaskRespVO reloadedIncoming = batchExecutionService.get(batch.getId()).getTasks().stream()
                .filter(task -> Objects.equals(incoming.getId(), task.getId()))
                .findFirst()
                .orElseThrow();
        List<EdhrBatchExecutionSpecialNodeAttachmentVO> pending =
                reloadedIncoming.getPendingSpecialNodeAttachments();
        assertEquals(1, pending.size());
        assertEquals(result.getUploadToken(), pending.get(0).getUploadToken());
        assertEquals(9211L, pending.get(0).getFileId());
        assertEquals("incoming-reload.pdf", pending.get(0).getFileName());
    }

    @Test
    void prepareSpecialNodeAttachmentUpload_allowsApprovedSpecialNodeBeforeRelease() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-UPLOAD-APPROVED")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO incoming = batch.getTasks().stream()
                .filter(task -> "INCOMING_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(incoming.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        byte[] content = "approved incoming attachment".getBytes(StandardCharsets.UTF_8);
        String directory = "edhr/special-nodes/" + batch.getId() + "/" + incoming.getId() + "/attachments";
        when(fileService.createFileAndReturnId(content, "approved-incoming.pdf", directory, "application/pdf"))
                .thenReturn(9202L);
        when(fileService.getFile(9202L)).thenReturn(FileDO.builder()
                .id(9202L)
                .configId(28L)
                .name("approved-incoming.pdf")
                .path(directory + "/approved-incoming.pdf")
                .url("http://127.0.0.1:9000/yudao/" + directory + "/approved-incoming.pdf")
                .type("application/pdf")
                .size((long) content.length)
                .build());

        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            result = batchExecutionService.prepareSpecialNodeAttachmentUpload(
                    new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                            .setTaskId(incoming.getId())
                            .setFileName("approved-incoming.pdf")
                            .setContentType("application/pdf")
                            .setContent(content));
        }

        assertEquals(9202L, result.getFileId());
        assertTrue(result.getUploadToken().contains(String.valueOf(incoming.getId())));
    }

    @Test
    void prepareSpecialNodeAttachmentUpload_allowsClosedBatchBeforeRelease() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-UPLOAD-CLOSED-BEFORE-RELEASE")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO sterilization = batch.getTasks().stream()
                .filter(task -> "STERILIZATION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(sterilization.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED));
        byte[] content = "closed before release special attachment".getBytes(StandardCharsets.UTF_8);
        String directory = "edhr/special-nodes/" + batch.getId() + "/" + sterilization.getId() + "/attachments";
        when(fileService.createFileAndReturnId(content, "closed-before-release.pdf", directory, "application/pdf"))
                .thenReturn(9203L);
        when(fileService.getFile(9203L)).thenReturn(FileDO.builder()
                .id(9203L)
                .configId(28L)
                .name("closed-before-release.pdf")
                .path(directory + "/closed-before-release.pdf")
                .url("http://127.0.0.1:9000/yudao/" + directory + "/closed-before-release.pdf")
                .type("application/pdf")
                .size((long) content.length)
                .build());

        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            result = batchExecutionService.prepareSpecialNodeAttachmentUpload(
                    new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                            .setTaskId(sterilization.getId())
                            .setFileName("closed-before-release.pdf")
                            .setContentType("application/pdf")
                            .setContent(content));
        }

        assertEquals(9203L, result.getFileId());
        assertTrue(result.getUploadToken().contains(String.valueOf(sterilization.getId())));
    }

    @Test
    void savePendingSpecialNodeAttachments_booksAllPendingAttachmentsBeforeRelease() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-UPLOAD-SAVE-PENDING")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO incoming = batch.getTasks().stream()
                .filter(task -> "INCOMING_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO finished = batch.getTasks().stream()
                .filter(task -> "FINISHED_PRODUCT_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        byte[] incomingContent = "pending incoming before release".getBytes(StandardCharsets.UTF_8);
        byte[] finishedContent = "pending finished before release".getBytes(StandardCharsets.UTF_8);
        String incomingDirectory = "edhr/special-nodes/" + batch.getId() + "/" + incoming.getId() + "/attachments";
        String finishedDirectory = "edhr/special-nodes/" + batch.getId() + "/" + finished.getId() + "/attachments";
        when(fileService.createFileAndReturnId(incomingContent, "incoming-save.pdf",
                incomingDirectory, "application/pdf")).thenReturn(9204L);
        when(fileService.createFileAndReturnId(finishedContent, "finished-save.pdf",
                finishedDirectory, "application/pdf")).thenReturn(9205L);
        when(fileService.getFile(9204L)).thenReturn(FileDO.builder()
                .id(9204L)
                .configId(28L)
                .name("incoming-save.pdf")
                .path(incomingDirectory + "/incoming-save.pdf")
                .url("http://127.0.0.1:9000/yudao/" + incomingDirectory + "/incoming-save.pdf")
                .type("application/pdf")
                .size((long) incomingContent.length)
                .build());
        when(fileService.getFile(9205L)).thenReturn(FileDO.builder()
                .id(9205L)
                .configId(28L)
                .name("finished-save.pdf")
                .path(finishedDirectory + "/finished-save.pdf")
                .url("http://127.0.0.1:9000/yudao/" + finishedDirectory + "/finished-save.pdf")
                .type("application/pdf")
                .size((long) finishedContent.length)
                .build());

        EdhrBatchExecutionRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            batchExecutionService.prepareSpecialNodeAttachmentUpload(
                    new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                            .setTaskId(incoming.getId())
                            .setFileName("incoming-save.pdf")
                            .setContentType("application/pdf")
                            .setContent(incomingContent));
            batchExecutionService.prepareSpecialNodeAttachmentUpload(
                    new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                            .setTaskId(finished.getId())
                            .setFileName("finished-save.pdf")
                            .setContentType("application/pdf")
                            .setContent(finishedContent));

            saved = batchExecutionService.savePendingSpecialNodeAttachments(batch.getId(), "保存待提交特殊节点附件");
        }

        EdhrBatchExecutionTaskRespVO savedIncoming = saved.getTasks().stream()
                .filter(task -> Objects.equals(incoming.getId(), task.getId()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO savedFinished = saved.getTasks().stream()
                .filter(task -> Objects.equals(finished.getId(), task.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(savedIncoming.getPendingSpecialNodeAttachments().isEmpty());
        assertTrue(savedFinished.getPendingSpecialNodeAttachments().isEmpty());
        assertTrue(batchTaskMapper.selectById(incoming.getId()).getSpecialPayloadJson()
                .contains("incoming-save.pdf"));
        assertTrue(batchTaskMapper.selectById(finished.getId()).getSpecialPayloadJson()
                .contains("finished-save.pdf"));
        List<MesProBatchRecordExecutionAttachmentDO> incomingRecords =
                attachmentMapper.selectListByBatchTaskId(incoming.getId());
        assertEquals(0, incomingRecords.stream()
                .filter(record -> "PENDING".equals(record.getAttachmentAction()))
                .count());
        assertEquals(1, incomingRecords.stream()
                .filter(record -> "ADD".equals(record.getAttachmentAction())
                        && "incoming-save.pdf".equals(record.getFileName()))
                .count());
    }

    @Test
    void completeSpecialNode_persistsAttachmentsAndArchiveManifestContainsSpecialEvidence() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-ARCHIVE")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO incoming = batch.getTasks().stream()
                .filter(task -> "INCOMING_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        markBatchOwner(batch.getId(), 188L);
        String sha256 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        String retentionJson = "{\"fileId\":9301,\"storageConfigId\":28,\"storagePath\":\"edhr/special/incoming.pdf\",\"sha256\":\""
                + sha256 + "\"}";
        MesProEdhrSpecialNodeAttachment attachment = new MesProEdhrSpecialNodeAttachment()
                .setUploadToken("EDHR_SPECIAL_NODE_ATTACHMENT:" + incoming.getId() + ":9301:" + sha256)
                .setFileId(9301L)
                .setFileUrl("http://127.0.0.1:9000/yudao/edhr/special/incoming.pdf")
                .setStorageConfigId(28L)
                .setStoragePath("edhr/special/incoming.pdf")
                .setFileName("incoming-special.pdf")
                .setContentType("application/pdf")
                .setFileSize(2048L)
                .setSha256(sha256)
                .setStorageRetentionJson(retentionJson)
                .setStorageRetentionHash(MesProBatchRecordExecutionFieldAuditHasher.sha256(
                        "EDHR_SPECIAL_NODE_ATTACHMENT_V1:RETENTION\n"
                                + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(retentionJson)));
        when(fileService.getFile(9301L)).thenReturn(FileDO.builder()
                .id(9301L)
                .configId(28L)
                .name("incoming-special.pdf")
                .path("edhr/special/incoming.pdf")
                .url("http://127.0.0.1:9000/yudao/edhr/special/incoming.pdf")
                .type("application/pdf")
                .size(2048L)
                .build());

        clearInvocations(workTaskService);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            batchExecutionService.completeSpecialNode(incoming.getId(), null, List.of(attachment));
        }

        MesProEdhrBatchExecutionTaskDO persistedTask = batchTaskMapper.selectById(incoming.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED, persistedTask.getStatus());
        assertTrue(persistedTask.getSpecialPayloadJson().contains("incoming-special.pdf"));
        assertTrue(persistedTask.getSpecialPayloadJson().contains("completedBy"));
        verify(workTaskService).createNextFillAfterSpecialNodeResolved(argThat(task ->
                task != null
                        && incoming.getId().equals(task.getId())
                        && Objects.equals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED,
                        task.getStatus())));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9940L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9940L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9940L));

        String manifest = batchArchiveMapper.selectById(archive.getId()).getSourceManifestJson();
        assertTrue(manifest.contains("specialPayloadJson"));
        assertTrue(manifest.contains("specialAttachments"));
        assertTrue(manifest.contains("incoming-special.pdf"));
        assertTrue(manifest.contains("completedBy"));
    }

    @Test
    void openOrCreate_skipsUnconfiguredRouteProcessAndCloseRequiresConfiguredTasks() {
        Fixture fixture = insertRouteFixture(true, false);

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPTIONAL")
                .setRouteId(fixture.routeId()));

        assertEquals(0, batch.getBlockedCount());
        assertFalse(batch.getCanClose());
        assertEquals(5, batch.getTaskTotal());
        assertEquals(5, batch.getTasks().size());
        assertEquals(Boolean.TRUE, routeTask(batch, 0).getRequiredFlag());

        assertCloseBlocked(() -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("close")));

        bindApprovedExecution(routeTask(batch, 0).getId(), 7101L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 0L);
        markReleasePrecheckPassed(batch.getId());
        EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("close"));
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
    }

    @Test
    void openOrCreate_skipsEnabledBatchProcessWithoutReportBinding() {
        Fixture fixture = insertRouteFixture(true, false);
        MesProRouteProcessDO secondRouteProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .filter(routeProcess -> routeProcess.getBatchRecordReportId() == null)
                .findFirst()
                .orElseThrow();
        insertBatchUseConfig(fixture.routeId(), secondRouteProcess.getId(), "SEQUENTIAL");

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ENABLED-WITHOUT-REPORT")
                .setRouteId(fixture.routeId()));

        assertEquals(5, batch.getTaskTotal());
        assertEquals(1, routeTasks(batch).size());
        assertEquals(fixture.reportId1(), routeTasks(batch).get(0).getBatchRecordReportId());
    }

    @Test
    void openTask_rejectsTaskIdThatWasNotGeneratedForUnconfiguredRouteProcess() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPTIONAL-OPEN")
                .setRouteId(fixture.routeId()));
        assertEquals(5, batch.getTasks().size());

        assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(batch.getId())
                .setTaskId(-1L)),
                PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS);
    }

    @Test
    void openTask_bindsExistingSingleExecutionContext() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-TASK")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        Long taskId = batchTask.getId();
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80001L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9001L)
                        .setCreated(true)
                        .setStatus(0)
                        .setCellLinkAutoPersist(new BatchRecordCellLinkAutoPersistResult()
                                .setExecutionId(9001L)
                                .setTrigger("TASK_OPEN")
                                .setAppliedCount(1)
                                .setConflictCount(0)));

        EdhrBatchExecutionTaskOpenRespVO response = openTaskAsFiller(batch.getId(), taskId, workTask.getId());

        assertEquals(9001L, response.getExecutionId());
        assertEquals(taskId, response.getTaskId());
        assertNotNull(response.getCellLinkAutoPersist());
        assertEquals(9001L, response.getCellLinkAutoPersist().getExecutionId());
        assertEquals("TASK_OPEN", response.getCellLinkAutoPersist().getTrigger());
        assertEquals(1, response.getCellLinkAutoPersist().getAppliedCount());
        assertNotNull(response.getExecutionPageQuery());
        assertEquals(workTask.getId(), response.getExecutionPageQuery().get("workTaskId"));
        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(singleExecutionService).openOrCreateByContext(reqCaptor.capture());
        assertEquals(batchTask.getId(), reqCaptor.getValue().getTaskId());
        assertNull(reqCaptor.getValue().getWorkstationId());
        assertEquals(batchTask.getProcessId(), reqCaptor.getValue().getProcessId());
        assertEquals(batchTask.getRouteProcessId(), reqCaptor.getValue().getRouteProcessId());
        assertEquals(9001L, batchTaskMapper.selectById(taskId).getExecutionId());
        verify(operationAuditService, atLeastOnce()).record(argThat(command ->
                "SKIP".equals(command.getOperationType())
                        && "BATCH_EXECUTION_TASK".equals(command.getObjectType())
                        && "SUCCESS".equals(command.getResultStatus())));
        verify(operationAuditService, atLeastOnce()).record(argThat(command ->
                "OPEN".equals(command.getOperationType())
                        && "BATCH_EXECUTION_TASK".equals(command.getObjectType())
                        && String.valueOf(taskId).equals(command.getObjectId())
                        && workTask.getId().equals(command.getWorkTaskId())
                         && "SUCCESS".equals(command.getResultStatus())));
    }

    @Test
    void openTask_exposesOnlyCurrentUsersAssistRowsFromFrozenResponsibilityScope() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-ASSIST-ROWS")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80051L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture)
                .setCandidateUserSnapshot("10001,910245")
                .setResponsibilitySourceType("EDHR_PROCESS_FORM_FILLER")
                .setResponsibilityScopeJson("""
                        {"schemaVersion":2,"sourceType":"EDHR_PROCESS_FORM_FILLER","sourceKey":"ROUTE_PROCESS:4001:MAIN","sourceVersion":"6002","scopes":[
                          {"scopeKey":"AR_OPERATOR","resolvedUserIds":[10001],"fillableScope":{"cells":[{"sourceTableIndex":0,"rowIndex":0,"columnIndex":1}]}},
                          {"scopeKey":"AR_REMARK","resolvedUserIds":[910245],"fillableScope":{"cells":[{"sourceTableIndex":0,"rowIndex":0,"columnIndex":3}]}}
                        ]}
                        """);
        workTaskMapper.updateById(workTask);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenAnswer(invocation -> {
                    executionMapper.insert(new MesProBatchRecordExecutionDO()
                            .setId(9051L)
                            .setExecutionCode("BRE-9051")
                            .setWorkOrderId(fixture.workOrderId())
                            .setWorkOrderCode("WO-OPEN-ASSIST")
                            .setRouteProcessId(batchTask.getRouteProcessId())
                            .setBatchRecordReportId(batchTask.getBatchRecordReportId())
                            .setBatchExecutionId(batch.getId())
                            .setRouteId(fixture.routeId())
                            .setBatchCode(batch.getBatchCode())
                            .setStatus(0)
                            .setSheetLayoutJson("{\"rows\":{}}")
                            .setMetaJson("{\"sourceTableIndex\":0}")
                            .setExecutionSnapshotJson("""
                                    {"snapshotVersion":"EDHR_EXECUTION_V1","fields":[],"assistRows":[
                                      {"rowKey":"AR_OPERATOR","description":"操作信息","sort":1,"fields":[{"rowIndex":0,"columnIndex":1}]},
                                      {"rowKey":"AR_REMARK","description":"备注信息","sort":2,"fields":[{"rowIndex":0,"columnIndex":3}]}
                                    ]}
                                    """)
                            .setCellValuesJson("[]")
                            .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                            .setFieldAuditRevision(0L)
                            .setFieldAuditHeadHash("0000000000000000000000000000000000000000000000000000000000000000"));
                    return new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                            .setId(9051L)
                            .setCreated(true)
                            .setStatus(0);
                });

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(batch.getId(), batchTask.getId(), workTask.getId());

        JSONArray assistRows = JSON.parseArray(JSON.toJSONString(opened.getExecutionPageQuery().get("assistRows")));
        assertNotNull(assistRows);
        assertEquals(1, assistRows.size());
        assertEquals("AR_OPERATOR", assistRows.getJSONObject(0).getString("rowKey"));
        assertEquals("操作信息", assistRows.getJSONObject(0).getString("description"));

        EdhrBatchExecutionTaskOpenRespVO openedBySecondCandidate =
                openTaskAs(910245L, batch.getId(), batchTask.getId(), workTask.getId());
        JSONArray secondCandidateAssistRows = JSON.parseArray(JSON.toJSONString(
                openedBySecondCandidate.getExecutionPageQuery().get("assistRows")));
        assertNotNull(secondCandidateAssistRows);
        assertEquals(1, secondCandidateAssistRows.size());
        assertEquals("AR_REMARK", secondCandidateAssistRows.getJSONObject(0).getString("rowKey"));
        assertEquals("备注信息", secondCandidateAssistRows.getJSONObject(0).getString("description"));

        EdhrBatchExecutionTaskOpenRespVO openedBySelectedAssistUser =
                openTaskAs(10001L, batch.getId(), batchTask.getId(), workTask.getId(), 910245L);
        assertEquals(910245L, openedBySelectedAssistUser.getAssistUserId());
        assertEquals(910245L, openedBySelectedAssistUser.getExecutionPageQuery().get("assistUserId"));
        JSONArray selectedAssistRows = JSON.parseArray(JSON.toJSONString(
                openedBySelectedAssistUser.getExecutionPageQuery().get("assistRows")));
        assertNotNull(selectedAssistRows);
        assertEquals(1, selectedAssistRows.size());
        assertEquals("AR_REMARK", selectedAssistRows.getJSONObject(0).getString("rowKey"));
        assertEquals("备注信息", selectedAssistRows.getJSONObject(0).getString("description"));
    }

    @Test
    void openTask_exposesAssistRowsWhenAllRangeScopeCoversSnapshotSourceTable() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-ALL-RANGE-ASSIST-ROWS")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80052L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture)
                .setCandidateUserSnapshot("10001,910245")
                .setResponsibilitySourceType("EDHR_PROCESS_FORM_FILLER")
                .setResponsibilityScopeJson("""
                        {"schemaVersion":2,"sourceType":"EDHR_PROCESS_FORM_FILLER","sourceKey":"FORM|REPORT-ALL|130","sourceVersion":"130","scopes":[
                          {"scopeKey":"ALL","resolvedUserIds":[10001,910245],"fillableScope":{"ranges":[
                            {"sourceTableIndex":2,"startRow":0,"endRow":99999}
                          ]}}
                        ]}
                        """);
        workTaskMapper.updateById(workTask);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenAnswer(invocation -> {
                    executionMapper.insert(new MesProBatchRecordExecutionDO()
                            .setId(9052L)
                            .setExecutionCode("BRE-9052")
                            .setWorkOrderId(fixture.workOrderId())
                            .setWorkOrderCode("WO-OPEN-ALL-RANGE")
                            .setRouteProcessId(batchTask.getRouteProcessId())
                            .setBatchRecordReportId(batchTask.getBatchRecordReportId())
                            .setBatchExecutionId(batch.getId())
                            .setRouteId(fixture.routeId())
                            .setBatchCode(batch.getBatchCode())
                            .setStatus(0)
                            .setSheetLayoutJson("{\"rows\":{}}")
                            .setMetaJson("{\"sourceTableIndex\":2}")
                            .setExecutionSnapshotJson("""
                                    {"snapshotVersion":"EDHR_EXECUTION_V1","meta":{"sourceTableIndex":2},"fields":[],"assistRows":[
                                      {"rowKey":"AR_OPERATOR","description":"操作信息","sort":1,"fields":[{"rowIndex":0,"columnIndex":1}]},
                                      {"rowKey":"AR_REMARK","description":"备注信息","sort":2,"fields":[{"rowIndex":1,"columnIndex":3}]}
                                    ]}
                                    """)
                            .setCellValuesJson("[]")
                            .setCellValuesHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                            .setFieldAuditRevision(0L)
                            .setFieldAuditHeadHash("0000000000000000000000000000000000000000000000000000000000000000"));
                    return new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                            .setId(9052L)
                            .setCreated(true)
                            .setStatus(0);
                });

        EdhrBatchExecutionTaskOpenRespVO openedBySelectedAssistUser =
                openTaskAs(10001L, batch.getId(), batchTask.getId(), workTask.getId(), 910245L);

        assertEquals(910245L, openedBySelectedAssistUser.getAssistUserId());
        JSONArray selectedAssistRows = JSON.parseArray(JSON.toJSONString(
                openedBySelectedAssistUser.getExecutionPageQuery().get("assistRows")));
        assertNotNull(selectedAssistRows);
        assertEquals(2, selectedAssistRows.size());
        assertEquals("AR_OPERATOR", selectedAssistRows.getJSONObject(0).getString("rowKey"));
        assertEquals("AR_REMARK", selectedAssistRows.getJSONObject(1).getString("rowKey"));
    }

    @Test
    void openTask_freezesDisabledRecordbookStateIntoTaskExecutionAndPageQuery() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteProcessDO firstRouteProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        MesProRouteFlowProcessBatchRecordDO binding = routeFlowProcessBatchRecordMapper
                .selectListByRouteProcessIdsAndUseType(List.of(firstRouteProcess.getId()), "BATCH").stream()
                .findFirst()
                .orElseThrow();
        binding.setRecordbookEnabled(Boolean.FALSE);
        routeFlowProcessBatchRecordMapper.updateById(binding);

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-RECORDBOOK-DISABLED")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        assertEquals(Boolean.FALSE, batchTask.getRecordbookEnabled());
        assertEquals(Boolean.FALSE, batchTaskMapper.selectById(batchTask.getId()).getRecordbookEnabled());
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80011L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9011L)
                        .setCreated(true)
                        .setRecordbookEnabled(Boolean.FALSE)
                        .setStatus(0));

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(batch.getId(), batchTask.getId(), workTask.getId());

        assertEquals(Boolean.FALSE, opened.getRecordbookEnabled());
        assertEquals(Boolean.FALSE, opened.getExecutionPageQuery().get("recordbookEnabled"));
        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(singleExecutionService).openOrCreateByContext(reqCaptor.capture());
        assertEquals(Boolean.FALSE, reqCaptor.getValue().getRecordbookEnabled());
    }

    @Test
    void openTask_pendingVoidRequest_rejectsNormalFillAction() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-PENDING-VOID")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80041L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9041L)
                        .setCreated(true)
                        .setStatus(0));
        recordChangeEventMapper.insert(MesProEdhrRecordChangeEventDO.builder()
                .changeCode("CHG-OPEN-PENDING-VOID-" + randomLongId())
                .changeType("VOID")
                .targetScope("BATCH")
                .batchExecutionId(batch.getId())
                .changeStatus("SUBMITTED")
                .reasonCategory("PRODUCTION_VOID")
                .reasonText("作废申请待审批")
                .requestedBy(10001L)
                .requestedAt(LocalDateTime.now())
                .bpmProcessInstanceId("BPM-OPEN-PENDING-VOID-" + randomLongId())
                .build());

        assertServiceException(() -> openTaskAsFiller(batch.getId(), batchTask.getId(), workTask.getId()),
                PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED);
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void openTask_allowsRouteBindingWithoutPermissionScope() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        String reportId = insertReport("RPT-OPEN-LEGACY-BINDING", "旧绑定打开表单");
        MesProRouteFlowProcessBatchRecordDO binding = insertBatchUseConfig(
                fixture.routeId(), routeProcesses.get(0).getId(), "SEQUENTIAL", reportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", null,
                null);
        binding.setSlotConfigSnapshotHash(null);
        routeFlowProcessBatchRecordMapper.updateById(binding);
        String secondReportId = insertReport("RPT-OPEN-LEGACY-BINDING-2", "旧绑定打开表单2");
        MesProRouteFlowProcessBatchRecordDO secondBinding = insertBatchUseConfig(
                fixture.routeId(), routeProcesses.get(1).getId(), "SEQUENTIAL", secondReportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", null,
                null);
        secondBinding.setSlotConfigSnapshotHash(null);
        routeFlowProcessBatchRecordMapper.updateById(secondBinding);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-LEGACY-BINDING")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80031L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9031L)
                        .setCreated(true)
                        .setStatus(0));

        EdhrBatchExecutionTaskOpenRespVO opened = openTaskAsFiller(batch.getId(), batchTask.getId(), workTask.getId());

        assertEquals(9031L, opened.getExecutionId());
        assertEquals(workTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        assertNull(opened.getPermissionScopeId());
        verify(permissionGateService, never()).requireAbility(any());
    }

    @Test
    void openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        MesProRouteProcessDO firstRouteProcess = routeProcessMapper.selectListByRouteId(route.getId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        MesProProcessDO firstProcess = processMapper.selectById(firstRouteProcess.getProcessId());
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(fixture.reportId1());
        MesProEdhrBatchExecutionDO legacyBatch = MesProEdhrBatchExecutionDO.builder()
                .batchExecutionCode("EDHRB-OPEN-LEGACY-FROZEN")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode("BATCH-OPEN-LEGACY-FROZEN")
                .activeContextKey(workOrder.getId() + "|" + route.getId() + "|BATCH-OPEN-LEGACY-FROZEN")
                .attemptNo(1)
                .productId(workOrder.getProductId())
                .productCode(String.valueOf(workOrder.getProductId()))
                .productName(workOrder.getName())
                .routeId(route.getId())
                .routeVersionId(fixture.routeVersionId())
                .routeVersionNo(fixture.routeVersionNo())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .routeCode(route.getCode())
                .routeName(route.getName())
                .status(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED)
                .taskTotal(1)
                .taskApprovedCount(0)
                .blockedCount(0)
                .build();
        batchExecutionMapper.insert(legacyBatch);
        Long executionId = 9032L;
        MesProEdhrBatchExecutionTaskDO legacyTask = MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(legacyBatch.getId())
                .nodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .routeProcessId(firstRouteProcess.getId())
                .rootProcessFlag(Boolean.TRUE)
                .routeProcessSort(firstRouteProcess.getSort())
                .processId(firstRouteProcess.getProcessId())
                .processCode(firstProcess.getCode())
                .processName(firstProcess.getName())
                .batchRecordReportId(fixture.reportId1())
                .batchRecordReportName(report.getReportName())
                .batchRecordDefinitionId(report.getBatchRecordDefinitionId())
                .batchRecordVersionId(report.getBatchRecordVersionId())
                .batchRecordSort(1)
                .instanceScope("PROCESS")
                .executionMode("SEQUENTIAL")
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                .executionId(executionId)
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT)
                .requiredFlag(Boolean.TRUE)
                .build();
        batchTaskMapper.insert(legacyTask);
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setExecutionCode("BRE-" + executionId)
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setRouteProcessId(firstRouteProcess.getId())
                .setBatchRecordReportId(fixture.reportId1())
                .setBatchRecordDefinitionId(report.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(report.getBatchRecordVersionId())
                .setBatchExecutionId(legacyBatch.getId())
                .setRouteId(route.getId())
                .setInstanceScope("PROCESS")
                .setFormSlotType("MAIN")
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setBatchCode(legacyBatch.getBatchCode())
                .setStatus(0)
                .setSheetLayoutJson("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"传统批记录\"}}}}}")
                .setMetaJson("{\"tableTitle\":\"传统批记录\"}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setDomainTraceStatus("VERIFIED"));
        EdhrBatchExecutionRespVO batchResp = new EdhrBatchExecutionRespVO().setId(legacyBatch.getId());
        EdhrBatchExecutionTaskRespVO taskResp = new EdhrBatchExecutionTaskRespVO()
                .setId(legacyTask.getId())
                .setRouteProcessId(firstRouteProcess.getId())
                .setProcessId(firstRouteProcess.getProcessId())
                .setProcessName(firstProcess.getName());
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batchResp, taskResp, fixture);
        clearInvocations(singleExecutionService);

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(legacyBatch.getId(), legacyTask.getId(), workTask.getId());

        assertEquals(executionId, opened.getExecutionId());
        assertEquals(legacyTask.getId(), opened.getTaskId());
        assertEquals(fixture.reportId1(), opened.getBatchRecordReportId());
        assertEquals(workTask.getId(), opened.getWorkTaskId());
        assertNull(opened.getFormTemplateId());
        assertNull(opened.getFormCenterInstanceId());
        assertEquals(executionId, opened.getExecutionPageQuery().get("id"));
        assertEquals(workTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void openTask_routeFormFillTaskDoesNotRequirePreviousSpecialNodeCompletion() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-SPECIAL-NODE-STILL-PENDING")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80021L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9021L)
                        .setCreated(true)
                        .setStatus(0));

        EdhrBatchExecutionRespVO fillerView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            fillerView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO fillerTask = routeTask(fillerView, 0);
        assertEquals(Boolean.TRUE, fillerTask.getAvailable());
        assertNull(fillerTask.getGateMessage());
        assertEquals("FILLER", fillerTask.getCurrentUserRole());
        assertEquals(workTask.getId(), fillerTask.getActiveWorkTaskId());
        assertTrue(fillerTask.getAllowedActions().contains("OPEN_FORM"));
        assertNull(fillerTask.getDisabledReason());

        EdhrBatchExecutionTaskOpenRespVO opened = openTaskAsFiller(batch.getId(), batchTask.getId(), workTask.getId());

        assertEquals(9021L, opened.getExecutionId());
        assertEquals(workTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
    }

    @Test
    void openTask_rejectsUnrelatedUserWhenActiveFillWorkTaskExists() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-FILLER-ONLY")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80041L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(910245L);
            assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                            .setBatchExecutionId(batch.getId())
                            .setTaskId(batchTask.getId())
                            .setWorkTaskId(workTask.getId())),
                    PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE);
        }
    }

    @Test
    void openTask_rejectsMismatchedWorkTaskContextForActiveFillTask() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-WORK-TASK-MISMATCH")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80043L);
        insertFillWorkTask(batch, batchTask, fixture);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                            .setBatchExecutionId(batch.getId())
                            .setTaskId(batchTask.getId())
                            .setWorkTaskId(990043L)),
                    PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE);
        }
    }

    @Test
    void openTask_allowsCandidateUserWhenActiveFillWorkTaskExists() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-FILL-CANDIDATE")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80042L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture)
                .setCandidateUserSnapshot("10001,910245");
        workTaskMapper.updateById(workTask);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9042L)
                        .setCreated(true)
                        .setStatus(0));

        EdhrBatchExecutionTaskOpenRespVO opened;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(910245L);
            opened = batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                    .setBatchExecutionId(batch.getId())
                    .setTaskId(batchTask.getId())
                    .setWorkTaskId(workTask.getId()));
        }

        assertEquals(9042L, opened.getExecutionId());
        assertEquals(workTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
    }

    @Test
    void openTask_allowsCurrentProcessFillerToOpenExtraFormCandidateWorkTask() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .findFirst()
                .orElseThrow();
        String mainReport = insertReport("RPT-OPEN-ASSIST-MAIN", "粗洗生产记录");
        String lossReport = insertReport("RPT-OPEN-ASSIST-LOSS", "粗洗损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "PARALLEL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5011L, "PRODUCTION", 2)
        ));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-ASSIST-EXTRA")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO mainTask = routeTasks(batch).stream()
                .filter(task -> mainReport.equals(task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO lossTask = routeTasks(batch).stream()
                .filter(task -> lossReport.equals(task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), firstProcess.getProcessId(), 80044L);
        insertWorkTask(batch, mainTask, fixture, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        MesProEdhrWorkTaskDO lossWorkTask = insertWorkTask(batch, lossTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 152L)
                .setCandidateUserSnapshot("152");
        workTaskMapper.updateById(lossWorkTask);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9044L)
                        .setCreated(true)
                        .setStatus(0));

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAs(10001L, batch.getId(), lossTask.getId(), lossWorkTask.getId(), 152L);

        assertEquals(9044L, opened.getExecutionId());
        assertEquals(lossWorkTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        assertEquals(152L, opened.getExecutionPageQuery().get("assistUserId"));
    }

    @Test
    void get_allowsCurrentProcessAnchorFillerAfterOpeningExtraFormAssistTask() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .findFirst()
                .orElseThrow();
        String mainReport = insertReport("RPT-VIEW-ASSIST-MAIN", "粗洗生产记录");
        String lossReport = insertReport("RPT-VIEW-ASSIST-LOSS", "粗洗损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "PARALLEL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5011L, "PRODUCTION", 2)
        ));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-VIEW-ASSIST-EXTRA")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO mainTask = routeTasks(batch).stream()
                .filter(task -> mainReport.equals(task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO lossTask = routeTasks(batch).stream()
                .filter(task -> lossReport.equals(task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), firstProcess.getProcessId(), 80046L);
        insertWorkTask(batch, mainTask, fixture, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        MesProEdhrWorkTaskDO lossWorkTask = insertWorkTask(batch, lossTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 152L)
                .setCandidateUserSnapshot("152");
        workTaskMapper.updateById(lossWorkTask);
        when(permissionApi.hasAnyPermissions(10001L, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION))
                .thenReturn(false);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9045L)
                        .setCreated(true)
                        .setStatus(0));

        openTaskAs(10001L, batch.getId(), lossTask.getId(), lossWorkTask.getId(), 152L);

        EdhrBatchExecutionRespVO detail;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            detail = batchExecutionService.get(batch.getId());
        }
        assertEquals(batch.getId(), detail.getId());
    }

    @Test
    void openTask_rejectsExtraFormAssistUserWithoutCurrentProcessFillerAnchor() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .findFirst()
                .orElseThrow();
        String mainReport = insertReport("RPT-OPEN-ASSIST-NO-ANCHOR-MAIN", "粗洗生产记录");
        String lossReport = insertReport("RPT-OPEN-ASSIST-NO-ANCHOR-LOSS", "粗洗损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "PARALLEL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5011L, "PRODUCTION", 2)
        ));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-ASSIST-NO-ANCHOR")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO lossTask = routeTasks(batch).stream()
                .filter(task -> lossReport.equals(task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), firstProcess.getProcessId(), 80045L);
        MesProEdhrWorkTaskDO lossWorkTask = insertWorkTask(batch, lossTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 152L)
                .setCandidateUserSnapshot("152");
        workTaskMapper.updateById(lossWorkTask);

        assertServiceException(() -> openTaskAs(10001L, batch.getId(), lossTask.getId(),
                        lossWorkTask.getId(), 152L),
                PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE);
    }

    @Test
    void openTask_existingExecution_rebindsOverdueFillTask() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-OVERDUE-REBIND")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        Long taskId = batchTask.getId();
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), batchTask.getProcessId(), 80011L);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(batch, batchTask, fixture)
                .setExecutionId(null)
                .setStatus(MesProEdhrWorkTaskStatus.OVERDUE);
        workTaskMapper.updateById(workTask);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(taskId)
                .setExecutionId(9011L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT));

        EdhrBatchExecutionTaskOpenRespVO response = openTaskAsFiller(batch.getId(), taskId, workTask.getId());

        assertEquals(9011L, response.getExecutionId());
        assertEquals(workTask.getId(), response.getExecutionPageQuery().get("workTaskId"));
        verify(workTaskService).bindExecution(taskId, 9011L);
    }

    @Test
    void openTask_rejectsAlreadyApprovedRouteTask() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-APPROVED-REJECT")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(batchTask.getId())
                .setExecutionId(9012L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));

        assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setTaskId(batchTask.getId())),
                PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
    }

    @Test
    void openTask_allowsApprovedOrdinaryFillCompletedBeforeReleaseForHistoricalFiller() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-APPROVED-PRE-RELEASE")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        Long executionId = 9013L;
        bindFillCompletedExecution(batchTask.getId(), executionId);
        MesProEdhrWorkTaskDO doneFillTask = insertFillWorkTask(batch, batchTask, fixture)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.of(2026, 7, 20, 9, 20));
        workTaskMapper.updateById(doneFillTask);

        EdhrBatchExecutionRespVO fillerView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            fillerView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO visibleTask = routeTask(fillerView, 0);
        assertEquals(doneFillTask.getId(), visibleTask.getActiveWorkTaskId());
        assertTrue(visibleTask.getAllowedActions().contains("OPEN_FORM"));
        assertTrue(visibleTask.getAllowedActions().contains("SUBMIT"));

        EdhrBatchExecutionTaskOpenRespVO opened = openTaskAsFiller(batch.getId(), batchTask.getId(), doneFillTask.getId());

        assertEquals(executionId, opened.getExecutionId());
        assertEquals(doneFillTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("已提交可修改动态损耗单");
        templateVersion.setJimuSchemaJson("{\"sheetLayoutJson\":\"{\\\"rows\\\":{},\\\"cols\\\":{},\\\"merges\\\":[]}\"}");
        templateVersion.setRecognizedSchemaJson("""
                [
                  {"fieldCode":"lossAmount","label":"损耗数量","fieldType":"NUMBER","required":true}
                ]
                """);
        formTemplateVersionMapper.updateById(templateVersion);
        stubFormCenterInstanceIds(84001L);
        insertBatchProcessFormCenterBinding(fixture.routeId(), routeProcess.getId(), templateVersion,
                "FB_APPROVED_DYNAMIC_LOSS");
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-APPROVED-DYNAMIC")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO dynamicTask = routeTasks(batch).get(0);
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, dynamicTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(dynamicTask.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .setSubmittedAt(LocalDateTime.of(2026, 7, 26, 9, 0))
                .setApprovedAt(LocalDateTime.of(2026, 7, 26, 9, 1)));

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(batch.getId(), dynamicTask.getId(), fillTask.getId());

        assertNull(opened.getExecutionId());
        assertEquals(dynamicTask.getId(), opened.getTaskId());
        assertEquals(fillTask.getId(), opened.getWorkTaskId());
        assertEquals(dynamicTask.getFormCenterInstanceId(), opened.getFormCenterInstanceId());
        assertEquals(dynamicTask.getFormTemplateId(), opened.getFormTemplateId());
        assertEquals(templateVersion.getJimuSchemaJson(), opened.getFormTemplateJimuSchemaJson());
        assertEquals("lossAmount", opened.getFormTemplateRecognizedFields().get(0).getFieldCode());
        assertEquals(fillTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        assertEquals(dynamicTask.getFormCenterInstanceId(), opened.getExecutionPageQuery().get("formCenterInstanceId"));
        assertEquals(templateVersion.getJimuSchemaJson(), opened.getExecutionPageQuery().get("formTemplateJimuSchemaJson"));
        assertFalse(((List<?>) opened.getExecutionPageQuery().get("formTemplateRecognizedFields")).isEmpty());
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void openTask_autoPersistsProductionWorkOrderPrefillForExistingDynamicRouteFormInstance() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("动态损耗单");
        stubFormCenterInstanceIds(85001L);
        insertBatchProcessFormCenterBinding(fixture.routeId(), routeProcess.getId(), templateVersion,
                "FB_DYNAMIC_LOSS_PREFILL");
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-DYNAMIC-OPEN-PREFILL")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO dynamicTask = routeTasks(batch).get(0);
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, dynamicTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        when(formActionInstanceMapper.selectById(dynamicTask.getFormCenterInstanceId()))
                .thenReturn(FormActionInstanceDO.builder()
                        .id(dynamicTask.getFormCenterInstanceId())
                        .tenantId(TenantContextHolder.getRequiredTenantId())
                        .status(FormInstanceStatus.DRAFT.name())
                        .formDataJson(JSON.toJSONString(new LinkedHashMap<>(Map.of(
                                "batchExecutionId", batch.getId(),
                                "batchTaskId", dynamicTask.getId()))))
                        .build());
        when(cellLinkService.buildFormTemplateVersionPrefillData(eq(templateVersion.getId()),
                eq(fixture.workOrderId()), eq("BATCH-DYNAMIC-OPEN-PREFILL"), any()))
                .thenAnswer(invocation -> {
                    Map<String, Object> formData = new LinkedHashMap<>(invocation.getArgument(3));
                    formData.put("3:1", "BATCH-DYNAMIC-OPEN-PREFILL");
                    return formData;
                });

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(batch.getId(), dynamicTask.getId(), fillTask.getId());

        assertEquals(dynamicTask.getFormCenterInstanceId(), opened.getFormCenterInstanceId());
        ArgumentCaptor<FormInstanceDraftReqVO> draftCaptor =
                ArgumentCaptor.forClass(FormInstanceDraftReqVO.class);
        verify(formCenterRuntimeService).saveDraft(eq(dynamicTask.getFormCenterInstanceId()),
                draftCaptor.capture(), eq(10001L));
        assertEquals("BATCH-DYNAMIC-OPEN-PREFILL", draftCaptor.getValue().getFormData().get("3:1"));
        assertEquals(batch.getId(), draftCaptor.getValue().getFormData().get("batchExecutionId"));
    }

    @Test
    void openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("动态损耗单辅助填写");
        templateVersion.setJimuSchemaJson(dynamicFormJimuSchemaJsonWithAssistRows());
        formTemplateVersionMapper.updateById(templateVersion);
        stubFormCenterInstanceIds(85011L);
        insertBatchFormCenterBinding(fixture.routeId(), routeProcess.getId(), templateVersion,
                "FB_DYNAMIC_LOSS_ASSIST", "PROCESS", null,
                "{\"cells\":[{\"sourceTableIndex\":0,\"rowIndex\":3,\"columnIndex\":1}]}");
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-DYNAMIC-LOSS-ASSIST")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO dynamicTask = routeTasks(batch).get(0);
        when(formActionInstanceMapper.selectById(dynamicTask.getFormCenterInstanceId()))
                .thenReturn(FormActionInstanceDO.builder()
                        .id(dynamicTask.getFormCenterInstanceId())
                        .tenantId(TenantContextHolder.getRequiredTenantId())
                        .status(FormInstanceStatus.DRAFT.name())
                        .formDataJson(JSON.toJSONString(new LinkedHashMap<>(Map.of(
                                "batchExecutionId", batch.getId(),
                                "batchTaskId", dynamicTask.getId()))))
                        .build());
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, dynamicTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 152L)
                .setCandidateUserSnapshot("10001,152")
                .setResponsibilitySourceType("EDHR_PROCESS_FORM_FILLER")
                .setResponsibilityScopeJson("""
                        {"schemaVersion":2,"sourceType":"EDHR_PROCESS_FORM_FILLER","sourceKey":"FORM|FB_DYNAMIC_LOSS_ASSIST","sourceVersion":"1","scopes":[
                          {"scopeKey":"AR_BATCH_CODE","resolvedUserIds":[152],"fillableScope":{"cells":[{"sourceTableIndex":0,"rowIndex":3,"columnIndex":1}]}}
                        ]}
                        """);
        workTaskMapper.updateById(fillTask);

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAs(10001L, batch.getId(), dynamicTask.getId(), fillTask.getId(), 152L);

        assertNull(opened.getExecutionId());
        assertEquals(dynamicTask.getFormCenterInstanceId(), opened.getFormCenterInstanceId());
        assertEquals(fillTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        assertEquals(152L, opened.getExecutionPageQuery().get("assistUserId"));
        JSONArray assistRows = (JSONArray) opened.getExecutionPageQuery().get("assistRows");
        assertEquals(1, assistRows.size());
        assertEquals("AR_BATCH_CODE", assistRows.getJSONObject(0).getString("rowKey"));
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("动态损耗单预览");
        templateVersion.setJimuSchemaJson(dynamicFormJimuSchemaJson());
        templateVersion.setRemark("动态损耗单预览备注");
        formTemplateVersionMapper.updateById(templateVersion);
        stubFormCenterInstanceIds(85001L);
        insertBatchProcessFormCenterBinding(fixture.routeId(), routeProcess.getId(), templateVersion,
                "FB_DYNAMIC_PREVIEW");
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-DYNAMIC-PREVIEW")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO dynamicTask = routeTasks(batch).get(0);

        EdhrBatchExecutionTaskPreviewRespVO preview =
                batchExecutionService.previewTask(batch.getId(), dynamicTask.getId());

        assertEquals(batch.getId(), preview.getBatchExecutionId());
        assertEquals(dynamicTask.getId(), preview.getTaskId());
        assertNull(preview.getExecutionId());
        assertEquals(Boolean.FALSE, preview.getExecutionCreated());
        assertEquals(dynamicTask.getStatus(), preview.getTaskStatus());
        assertNotNull(preview.getFormViewModel());
        assertEquals("[]", preview.getFormViewModel().getCellValuesJson());
        assertEquals("{\"fields\":[]}", preview.getFormViewModel().getExecutionSnapshotJson());
        assertEquals("动态损耗单预览备注", preview.getFormViewModel().getRemark());
        JSONObject layout = JSON.parseObject(preview.getFormViewModel().getSheetLayoutJson());
        JSONObject fillCell = layout.getJSONObject("rows")
                .getJSONObject("3")
                .getJSONObject("cells")
                .getJSONObject("1");
        assertEquals("生产批号", fillCell.getJSONObject("fillForm").getString("label"));
        assertEquals("input-text", fillCell.getJSONObject("edhrCellRule").getString("componentFlag"));
        JSONObject signatureCell = layout.getJSONObject("rows")
                .getJSONObject("4")
                .getJSONObject("cells")
                .getJSONObject("1");
        assertEquals("复核签名", signatureCell.getJSONObject("edhrSignature").getString("label"));
        assertEquals(1, preview.getFormViewModel().getSignatureCellMarkers().size());
        assertEquals(4, preview.getFormViewModel().getSignatureCellMarkers().get(0).getRowIndex());
        assertEquals(1, preview.getFormViewModel().getSignatureCellMarkers().get(0).getColumnIndex());
        verify(jimuReportGateway, never()).getReportJson(any());
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("动态识别字段表单预览");
        templateVersion.setRecognizedSchemaJson(dynamicFormRecognizedSchemaJson());
        templateVersion.setRemark("动态识别字段表单备注");
        formTemplateVersionMapper.updateById(templateVersion);
        stubFormCenterInstanceIds(85002L);
        insertBatchProcessFormCenterBinding(fixture.routeId(), routeProcess.getId(), templateVersion,
                "FB_DYNAMIC_RECOGNIZED_PREVIEW");
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-DYNAMIC-RECOGNIZED-PREVIEW")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO dynamicTask = routeTasks(batch).get(0);

        EdhrBatchExecutionTaskPreviewRespVO preview =
                batchExecutionService.previewTask(batch.getId(), dynamicTask.getId());

        assertEquals(batch.getId(), preview.getBatchExecutionId());
        assertEquals(dynamicTask.getId(), preview.getTaskId());
        assertNull(preview.getExecutionId());
        assertEquals(Boolean.FALSE, preview.getExecutionCreated());
        assertNotNull(preview.getFormViewModel());
        assertEquals("动态识别字段表单备注", preview.getFormViewModel().getRemark());
        JSONObject layout = JSON.parseObject(preview.getFormViewModel().getSheetLayoutJson());
        JSONObject titleCell = layout.getJSONObject("rows")
                .getJSONObject("0")
                .getJSONObject("cells")
                .getJSONObject("0");
        assertEquals("动态识别字段表单预览", titleCell.getString("text"));
        JSONObject batchCodeCell = layout.getJSONObject("rows")
                .getJSONObject("3")
                .getJSONObject("cells")
                .getJSONObject("1");
        assertEquals("生产批号", batchCodeCell.getJSONObject("fillForm").getString("label"));
        assertEquals("input-text", batchCodeCell.getJSONObject("edhrCellRule").getString("componentFlag"));
        JSONObject reviewDateCell = layout.getJSONObject("rows")
                .getJSONObject("3")
                .getJSONObject("cells")
                .getJSONObject("3");
        assertEquals("复核日期", reviewDateCell.getJSONObject("fillForm").getString("label"));
        assertEquals("date", reviewDateCell.getJSONObject("edhrCellRule").getString("componentFlag"));
        verify(jimuReportGateway, never()).getReportJson(any());
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PREVIEW-ASSIST")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchRecordTask = routeTasks(batch).get(0);
        when(jimuReportGateway.getReportJson(fixture.reportId1()))
                .thenReturn(unopenedBatchRecordPreviewReportJson(true));

        EdhrBatchExecutionTaskPreviewRespVO preview =
                batchExecutionService.previewTask(batch.getId(), batchRecordTask.getId());

        assertEquals(batch.getId(), preview.getBatchExecutionId());
        assertEquals(batchRecordTask.getId(), preview.getTaskId());
        assertEquals(Boolean.FALSE, preview.getExecutionCreated());
        assertNotNull(preview.getFormViewModel());
        assertNotNull(preview.getFormViewModel().getExecutionSnapshotJson());
        JSONObject snapshot = JSON.parseObject(preview.getFormViewModel().getExecutionSnapshotJson());
        JSONArray fields = snapshot.getJSONArray("fields");
        assertEquals(2, fields.size());
        JSONArray assistRows = snapshot.getJSONArray("assistRows");
        assertNotNull(assistRows);
        assertEquals(12, snapshot.getIntValue("assistGridRowCount"));
        assertEquals(9, snapshot.getIntValue("assistGridColumnCount"));
        assertEquals(2, assistRows.size());
        assertEquals("AR_OPERATOR", assistRows.getJSONObject(0).getString("rowKey"));
        assertEquals("操作信息", assistRows.getJSONObject(0).getString("description"));
        assertEquals(0, assistRows.getJSONObject(0).getJSONArray("fields").getJSONObject(0).getIntValue("rowIndex"));
        assertEquals(1, assistRows.getJSONObject(0).getJSONArray("fields").getJSONObject(0).getIntValue("columnIndex"));

        when(jimuReportGateway.getReportJson(fixture.reportId1()))
                .thenReturn(unopenedBatchRecordPreviewReportJson(false));
        EdhrBatchExecutionTaskPreviewRespVO noAssistPreview =
                batchExecutionService.previewTask(batch.getId(), batchRecordTask.getId());
        JSONArray noAssistRows = JSON.parseObject(noAssistPreview.getFormViewModel().getExecutionSnapshotJson())
                .getJSONArray("assistRows");
        assertNotNull(noAssistRows);
        assertEquals(0, noAssistRows.size());
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void openTask_pendingReleaseAllowsApprovedOrdinaryFillCompletedBeforeClose() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-APPROVED-RELEASE-LOCK")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        Long executionId = 9014L;
        bindFillCompletedExecution(batchTask.getId(), executionId);
        MesProEdhrWorkTaskDO doneFillTask = insertFillWorkTask(batch, batchTask, fixture)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.of(2026, 7, 20, 9, 30));
        workTaskMapper.updateById(doneFillTask);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batch.getId())
                .setReleaseCode("REL-OPEN-LOCK")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));

        EdhrBatchExecutionRespVO fillerView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            fillerView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO visibleTask = routeTask(fillerView, 0);
        assertEquals(doneFillTask.getId(), visibleTask.getActiveWorkTaskId());
        assertTrue(visibleTask.getAllowedActions().contains("OPEN_FORM"));
        assertTrue(visibleTask.getAllowedActions().contains("SUBMIT"));
        assertNull(visibleTask.getDisabledReason());
        assertEquals(Boolean.TRUE, fillerView.getReleaseActionLocked());

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(batch.getId(), batchTask.getId(), doneFillTask.getId());

        assertEquals(executionId, opened.getExecutionId());
        assertEquals(doneFillTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        verify(singleExecutionService, never()).openOrCreateByContext(any());
    }

    @Test
    void openTask_goldenFingerBypassesPendingReleaseLockForSubmittedOrdinaryForm() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-GF-RELEASE-LOCK")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        Long executionId = 9015L;
        bindFillCompletedExecution(batchTask.getId(), executionId);
        MesProEdhrWorkTaskDO doneFillTask = insertFillWorkTask(batch, batchTask, fixture)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setCompletedAt(LocalDateTime.of(2026, 7, 20, 9, 30));
        workTaskMapper.updateById(doneFillTask);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batch.getId())
                .setReleaseCode("REL-OPEN-GF-LOCK")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));
        when(goldenFingerPermissionService.hasGoldenFingerPermission(99L)).thenReturn(true);

        EdhrBatchExecutionRespVO goldenView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            goldenView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO visibleTask = routeTask(goldenView, 0);
        assertEquals(doneFillTask.getId(), visibleTask.getActiveWorkTaskId());
        assertTrue(visibleTask.getAllowedActions().contains("OPEN_FORM"));
        assertTrue(visibleTask.getAllowedActions().contains("SUBMIT"));
        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAs(99L, batch.getId(), batchTask.getId(), doneFillTask.getId());
        assertEquals(executionId, opened.getExecutionId());
    }

    @Test
    void openTask_rejectsClosedBatch() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPEN-CLOSED-REJECT")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED));

        assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setTaskId(batchTask.getId())),
                PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
    }

    @Test
    void openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-NO-TASK-CONTEXT")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9002L)
                        .setCreated(true)
                        .setStatus(0));

        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(batch.getId())
                .setTaskId(batchTask.getId()));

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(singleExecutionService).openOrCreateByContext(reqCaptor.capture());
        assertEquals(batchTask.getId(), reqCaptor.getValue().getTaskId());
        assertNull(reqCaptor.getValue().getWorkstationId());
    }

    @Test
    void openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SINGLE-TASK-CONTEXT")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), randomLongId(), 80002L);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9003L)
                        .setCreated(true)
                        .setStatus(0));

        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(batch.getId())
                .setTaskId(batchTask.getId()));

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(singleExecutionService).openOrCreateByContext(reqCaptor.capture());
        assertEquals(batchTask.getId(), reqCaptor.getValue().getTaskId());
        assertNull(reqCaptor.getValue().getWorkstationId());
        assertEquals(batchTask.getProcessId(), reqCaptor.getValue().getProcessId());
    }

    @Test
    void close_requiresReleasePrecheckPassedBeforeBatchClose() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO notPrechecked = prepareClosableBatch(
                fixture, "BATCH-CLOSE-PRECHECK-MISSING", 0L);

        assertServiceException(() -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                        .setId(notPrechecked.getId())
                        .setPassword("secret")
                        .setComment("close without release precheck")),
                PRO_EDHR_BATCH_EXECUTION_CLOSE_PRECHECK_REQUIRED);
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE,
                batchExecutionMapper.selectById(notPrechecked.getId()).getStatus());

        EdhrBatchExecutionRespVO failedPrecheck = prepareClosableBatch(
                fixture, "BATCH-CLOSE-PRECHECK-FAILED", 0L);
        markReleasePrecheckFailed(failedPrecheck.getId());

        assertServiceException(() -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                        .setId(failedPrecheck.getId())
                        .setPassword("secret")
                        .setComment("close after failed release precheck")),
                PRO_EDHR_BATCH_EXECUTION_CLOSE_PRECHECK_REQUIRED);
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE,
                batchExecutionMapper.selectById(failedPrecheck.getId()).getStatus());
    }

    @Test
    void close_allowsBatchCloseOnlyAfterReleasePrecheckPassed() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = prepareClosableBatch(
                fixture, "BATCH-CLOSE-PRECHECK-PASSED", 0L);
        markReleasePrecheckPassed(batch.getId());

        EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("close after release precheck passed"));

        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
        assertNotNull(batchExecutionMapper.selectById(batch.getId()).getClosedAt());
    }

    @Test
    void close_allowsFillCompletedOrdinaryTasksWithSubmitSignatureOnly() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CLOSE")
                .setRouteId(fixture.routeId()));
        bindFillCompletedExecution(routeTask(batch, 0).getId(), 7001L);
        bindFillCompletedExecution(routeTask(batch, 1).getId(), 7002L);
        completeFinalInspectionDossier(batch.getId());
        skipAllSpecialNodes(batch);
        markBatchOwner(batch.getId(), 0L);
        markReleasePrecheckPassed(batch.getId());

        EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("close batch"));
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
        assertTrue(closed.getCanArchive());
        assertNotNull(batchExecutionMapper.selectById(batch.getId()).getClosedAt());
    }

    @Test
    void openOrCreate_generatesMultipleTasksForSameRouteProcessFromBatchUseConfig() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String reportA = insertReport("RPT-10-A", "表1-A");
        String reportB = insertReport("RPT-10-B", "表1-B");
        insertBatchUseConfig(fixture.routeId(), routeProcess.getId(), "SEQUENTIAL", reportA, reportB);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-MULTI-REPORT")
                .setRouteId(fixture.routeId()));

        assertEquals(6, created.getTaskTotal());
        assertEquals(6, created.getTasks().size());
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(routeProcess.getId(), routeTasks.get(0).getRouteProcessId());
        assertEquals(routeProcess.getId(), routeTasks.get(1).getRouteProcessId());
        assertEquals(reportA, routeTasks.get(0).getBatchRecordReportId());
        assertEquals(1, routeTasks.get(0).getBatchRecordSort());
        assertEquals("SEQUENTIAL", routeTasks.get(0).getExecutionMode());
        assertEquals(reportB, routeTasks.get(1).getBatchRecordReportId());
        assertEquals(2, routeTasks.get(1).getBatchRecordSort());
    }

    @Test
    void openOrCreate_dynamicFormsUseFrozenPublishedVersionFromRouteBindingAndCreateInstances() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        MesProRouteFlowConfigDO useConfig = MesProRouteFlowConfigDO.builder()
                .routeId(fixture.routeId())
                .useType("BATCH")
                .enabled(Boolean.TRUE)
                .configVersion("TEST-DYNAMIC-FORM")
                .build();
        routeFlowConfigMapper.insert(useConfig);
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(useConfig.getId())
                .routeId(fixture.routeId())
                .routeProcessId(routeProcess.getId())
                .useType("BATCH")
                .enabled(Boolean.TRUE)
                .executionMode("SEQUENTIAL")
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        FormTemplateVersionDO configuredVersion = insertPublishedFormTemplateVersion("动态生产记录表 V1");
        when(cellLinkService.buildFormTemplateVersionPrefillData(eq(configuredVersion.getId()),
                eq(fixture.workOrderId()), eq("BATCH-DYNAMIC-FORM"), any()))
                .thenAnswer(invocation -> {
                    Map<String, Object> formData = new LinkedHashMap<>(invocation.getArgument(3));
                    formData.put("3:1", "BATCH-DYNAMIC-FORM");
                    return formData;
                });
        FormTemplateVersionDO latestVersion = FormTemplateVersionDO.builder()
                .templateId(configuredVersion.getTemplateId())
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .templateName("动态生产记录表 V2")
                .versionNo("V2.0")
                .status("PUBLISHED")
                .sourceFileName("dynamic-form-v2.docx")
                .build();
        formTemplateVersionMapper.insert(latestVersion);
        routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                .routeFlowProcessConfigId(processConfig.getId())
                .routeId(fixture.routeId())
                .routeProcessId(routeProcess.getId())
                .useType("BATCH")
                .formBindingKey("FB-DYNAMIC-1")
                .formTemplateId(configuredVersion.getTemplateId())
                .formTemplateNameSnapshot(configuredVersion.getTemplateName())
                .lastPublishedTemplateVersionId(configuredVersion.getId())
                .lastPublishedTemplateVersionNo(configuredVersion.getVersionNo())
                .instanceScope("PROCESS")
                .recordCategory("INTERNAL_RECORD")
                .validationProfile("INTERNAL_TRACE")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .reportSort(1)
                .build());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-DYNAMIC-FORM")
                .setRouteId(fixture.routeId()));

        EdhrBatchExecutionTaskRespVO routeTask = routeTasks(created).get(0);
        assertNull(routeTask.getBatchRecordReportId());
        assertEquals("FB-DYNAMIC-1", routeTask.getFormBindingKey());
        assertEquals(configuredVersion.getTemplateId(), routeTask.getFormTemplateId());
        assertEquals("动态生产记录表 V1", routeTask.getFormTemplateName());
        assertEquals(configuredVersion.getId(), routeTask.getFormTemplateVersionId());
        assertEquals("V1.0", routeTask.getFormTemplateVersionNo());
        assertNotNull(routeTask.getFormCenterInstanceId());
        MesProEdhrBatchExecutionTaskDO persistedTask = batchTaskMapper.selectById(routeTask.getId());
        assertEquals(configuredVersion.getId(), persistedTask.getFormTemplateVersionId());
        assertEquals("V1.0", persistedTask.getFormTemplateVersionNo());
        assertEquals(routeTask.getFormCenterInstanceId(), persistedTask.getFormCenterInstanceId());

        ArgumentCaptor<FormInstanceCreateReqVO> requestCaptor =
                ArgumentCaptor.forClass(FormInstanceCreateReqVO.class);
        verify(formCenterRuntimeService).createInstance(requestCaptor.capture(), any());
        FormInstanceCreateReqVO instanceRequest = requestCaptor.getValue();
        assertEquals("EDHR_RF_" + fixture.routeVersionId() + "_FB-DYNAMIC-1",
                instanceRequest.getContext().getActionCode());
        assertEquals(configuredVersion.getTemplateId(), instanceRequest.getFormData().get("formTemplateId"));
        assertEquals(configuredVersion.getId(), instanceRequest.getFormData().get("formTemplateVersionId"));
        assertEquals("V1.0", instanceRequest.getFormData().get("formTemplateVersionNo"));
        assertEquals("BATCH-DYNAMIC-FORM", instanceRequest.getFormData().get("3:1"));
    }

    @Test
    void openOrCreate_usesCurrentRouteFormBindingsInsteadOfFrozenSnapshot() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        String mainReport = insertReport("RPT-FROZEN-MAIN", "冻结主表");
        String sharedReport = insertReport("RPT-FROZEN-SHARED", "冻结共享过程检验单");
        String frozenSharedScope = "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}";
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                optionalSharedBatchSlot(sharedReport, "PROCESS_INSPECTION",
                        "FROZEN_SHARED_KEY", frozenSharedScope, 2)
        ));
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcesses))
                .build());
        routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                        List.of(firstProcess.getId()), "BATCH")
                .forEach(record -> {
                    record.setFormBindingKey("CURRENT_DRAFT_" + record.getReportSort());
                    record.setSharedFormKey("CURRENT_SHARED_KEY");
                    record.setRequiredPolicy("REQUIRED");
                    record.setOwnerRoleKey("PRODUCTION");
                    record.setSlotConfigSnapshotHash(
                            "9999999999999999999999999999999999999999999999999999999999999999");
                    routeFlowProcessBatchRecordMapper.updateById(record);
                });
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9511L).setStatus(0));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-FROZEN-FORM-BINDINGS")
                .setRouteId(fixture.routeId()));

        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(2, routeTasks.size());
        assertEquals("CURRENT_DRAFT_1", routeTasks.get(0).getFormBindingKey());
        assertEquals("PROCESS", routeTasks.get(0).getInstanceScope());
        assertEquals("REQUIRED", routeTasks.get(0).getRequiredPolicy());
        assertEquals("PRODUCTION", routeTasks.get(0).getOwnerRoleKey());
        assertEquals("9999999999999999999999999999999999999999999999999999999999999999",
                routeTasks.get(0).getSlotConfigSnapshotHash());
        assertEquals("CURRENT_DRAFT_2", routeTasks.get(1).getFormBindingKey());
        assertEquals("BATCH_SHARED", routeTasks.get(1).getInstanceScope());
        assertEquals("CURRENT_SHARED_KEY", routeTasks.get(1).getSharedFormKey());
        assertEquals(frozenSharedScope, routeTasks.get(1).getFillableScopeJson());
        assertEquals("REQUIRED", routeTasks.get(1).getRequiredPolicy());
        assertEquals("QUALITY", routeTasks.get(1).getOwnerRoleKey());
        assertEquals("9999999999999999999999999999999999999999999999999999999999999999",
                routeTasks.get(1).getSlotConfigSnapshotHash());
    }

    @Test
    void openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO oldVersion =
                insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        MesProBatchRecordVersionDO latestApprovedVersion =
                insertBatchRecordVersion(definitionId, "V2.0", "APPROVED");
        String reportId = insertReport("RPT-VERSIONED-TASK", "版本化批记录表");
        MesProBatchRecordReportDO boundReport = reportMapper.selectByReportId(reportId);
        boundReport.setBatchRecordDefinitionId(definitionId);
        boundReport.setBatchRecordVersionId(oldVersion.getId());
        boundReport.setFormSlotType("MAIN");
        boundReport.setSourceTableIndex(5);
        reportMapper.updateById(boundReport);
        String latestReportId = insertVersionedReport("RPT-VERSIONED-TASK-LATEST", "版本化批记录表",
                definitionId, latestApprovedVersion.getId(), 5, "MAIN");
        MesProRouteFlowProcessBatchRecordDO binding = insertBatchUseConfig(
                fixture.routeId(), routeProcess.getId(), "SEQUENTIAL", reportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", 5001L,
                "3333333333333333333333333333333333333333333333333333333333333333");
        binding.setBatchRecordDefinitionId(definitionId);
        binding.setBatchRecordVersionId(oldVersion.getId());
        routeFlowProcessBatchRecordMapper.updateById(binding);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-VERSIONED-TASK")
                .setRouteId(fixture.routeId()));

        EdhrBatchExecutionTaskRespVO routeTask = routeTasks(created).get(0);
        assertEquals(latestReportId, routeTask.getBatchRecordReportId());
        assertEquals(definitionId, routeTask.getBatchRecordDefinitionId());
        assertEquals(latestApprovedVersion.getId(), routeTask.getBatchRecordVersionId());
        MesProEdhrBatchExecutionTaskDO persistedTask = batchTaskMapper.selectById(routeTask.getId());
        assertEquals(latestReportId, persistedTask.getBatchRecordReportId());
        assertEquals(definitionId, persistedTask.getBatchRecordDefinitionId());
        assertEquals(latestApprovedVersion.getId(), persistedTask.getBatchRecordVersionId());
    }

    @Test
    void openOrCreate_blocksRouteBindingWhenReportCannotResolveStableDefinitionIdentity() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String reportId = insertLegacyReportWithoutStableIdentity("RPT-LEGACY-NO-STABLE", "缺稳定身份批记录表");
        insertBatchUseConfig(fixture.routeId(), routeProcess.getId(), "SEQUENTIAL", reportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", 5001L,
                "3333333333333333333333333333333333333333333333333333333333333333");

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-LEGACY-NO-STABLE")
                        .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void openOrCreate_propagatesInternalRecordMetadataToTaskAndOpenContext() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String reportId = insertReport("RPT-INTERNAL-TRACE", "内部追溯表");
        MesProRouteFlowProcessBatchRecordDO binding = insertBatchUseConfig(
                fixture.routeId(), routeProcess.getId(), "SEQUENTIAL", reportId,
                "INTERNAL_RECORD", "INTERNAL_TRACE", 5001L,
                "1111111111111111111111111111111111111111111111111111111111111111");

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-INTERNAL-TRACE")
                .setRouteId(fixture.routeId()));

        EdhrBatchExecutionTaskRespVO internalTask = routeTasks(created).get(0);
        assertEquals("INTERNAL_RECORD", internalTask.getRecordCategory());
        assertEquals("INTERNAL_TRACE", internalTask.getValidationProfile());
        assertEquals(5001L, internalTask.getPermissionScopeId());
        assertEquals(binding.getId(), internalTask.getRouteBindingId());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
                internalTask.getRouteBindingSnapshotHash());
        assertNotNull(internalTask.getSlotConfigSnapshotHash());
        assertEquals(64, internalTask.getSlotConfigSnapshotHash().length());
        MesProEdhrBatchExecutionTaskDO persistedTask = batchTaskMapper.selectById(internalTask.getId());
        assertEquals("INTERNAL_RECORD", persistedTask.getRecordCategory());
        assertEquals("INTERNAL_TRACE", persistedTask.getValidationProfile());
        assertEquals(5001L, persistedTask.getPermissionScopeId());
        assertEquals(binding.getId(), persistedTask.getRouteBindingId());
        assertEquals(internalTask.getSlotConfigSnapshotHash(), persistedTask.getSlotConfigSnapshotHash());

        skipAllSpecialNodes(created);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), routeProcess.getProcessId(), 82001L);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9201L)
                        .setStatus(0)
                        .setRecordCategory("INTERNAL_RECORD")
                        .setValidationProfile("INTERNAL_TRACE")
                        .setPermissionScopeId(5001L)
                        .setRouteBindingId(binding.getId())
                        .setRouteBindingSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                        .setSlotConfigSnapshotHash(internalTask.getSlotConfigSnapshotHash()));

        EdhrBatchExecutionTaskOpenRespVO opened = batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(created.getId())
                .setTaskId(internalTask.getId()));

        assertEquals("INTERNAL_RECORD", opened.getRecordCategory());
        assertEquals("INTERNAL_TRACE", opened.getValidationProfile());
        assertEquals(5001L, opened.getPermissionScopeId());
        assertEquals(binding.getId(), opened.getRouteBindingId());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
                opened.getRouteBindingSnapshotHash());
        assertEquals(internalTask.getSlotConfigSnapshotHash(), opened.getSlotConfigSnapshotHash());

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> reqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(singleExecutionService).openOrCreateByContext(reqCaptor.capture());
        MesProBatchRecordExecutionOpenOrCreateByContextReqVO req = reqCaptor.getValue();
        assertEquals("INTERNAL_RECORD", req.getRecordCategory());
        assertEquals("INTERNAL_TRACE", req.getValidationProfile());
        assertEquals(5001L, req.getPermissionScopeId());
        assertEquals(binding.getId(), req.getRouteBindingId());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
                req.getRouteBindingSnapshotHash());
        assertEquals(internalTask.getSlotConfigSnapshotHash(), req.getSlotConfigSnapshotHash());
        verify(permissionGateService).requireAbility(argThat(command ->
                "BATCH_EXECUTION_TASK".equals(command.getObjectType())
                        && String.valueOf(internalTask.getId()).equals(command.getObjectId())
                        && "FILL".equals(command.getAbility())
                        && Long.valueOf(5001L).equals(command.getScopeId())
                        && "mes:pro-edhr-batch-execution:update".equals(command.getPermissionCode())));
    }

    @Test
    void openOrCreate_rejectsRouteBindingWithInvalidRecordCategoryMetadata() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        String reportId = insertReport("RPT-INVALID-ROUTE-BINDING", "无效绑定元数据");
        insertBatchUseConfig(
                fixture.routeId(), routeProcesses.get(0).getId(), "SEQUENTIAL", reportId,
                "ROUTE_FORM", "DEFAULT", 5001L,
                "2222222222222222222222222222222222222222222222222222222222222222");
        String secondReportId = insertReport("RPT-VALID-ROUTE-BINDING", "有效绑定元数据");
        insertBatchUseConfig(
                fixture.routeId(), routeProcesses.get(1).getId(), "SEQUENTIAL", secondReportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", 5001L,
                "2222222222222222222222222222222222222222222222222222222222222222");

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-INVALID-ROUTE-BINDING")
                        .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void openOrCreate_allowsRouteBindingWithoutPermissionScopeAndSlotHash() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        String reportId = insertReport("RPT-LEGACY-BINDING", "旧绑定表单");
        MesProRouteFlowProcessBatchRecordDO binding = insertBatchUseConfig(
                fixture.routeId(), routeProcesses.get(0).getId(), "SEQUENTIAL", reportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", null,
                null);
        binding.setSlotConfigSnapshotHash(null);
        routeFlowProcessBatchRecordMapper.updateById(binding);
        String secondReportId = insertReport("RPT-LEGACY-BINDING-2", "旧绑定表单2");
        MesProRouteFlowProcessBatchRecordDO secondBinding = insertBatchUseConfig(
                fixture.routeId(), routeProcesses.get(1).getId(), "SEQUENTIAL", secondReportId,
                "BATCH_RECORD", "CONTROLLED_BATCH", null,
                null);
        secondBinding.setSlotConfigSnapshotHash(null);
        routeFlowProcessBatchRecordMapper.updateById(secondBinding);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-LEGACY-BINDING")
                .setRouteId(fixture.routeId()));

        EdhrBatchExecutionTaskRespVO routeTask = routeTasks(created).get(0);
        assertEquals(reportId, routeTask.getBatchRecordReportId());
        assertNull(routeTask.getPermissionScopeId());
        assertNotNull(routeTask.getSlotConfigSnapshotHash());
        assertEquals(64, routeTask.getSlotConfigSnapshotHash().length());
        assertEquals(binding.getId(), routeTask.getRouteBindingId());
        assertEquals(2, routeTasks(created).size());
    }

    @Test
    void openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist() {
        Fixture fixture = insertRouteFixture(false, false);
        routeProcessFlowEdgeMapper.delete(new LambdaUpdateWrapper<MesProRouteProcessFlowEdgeDO>()
                .eq(MesProRouteProcessFlowEdgeDO::getRouteId, fixture.routeId()));
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstStart = routeProcesses.get(0);
        MesProRouteProcessDO secondStart = routeProcesses.get(1);
        MesProProcessDO thirdProcess = MesProProcessDO.builder()
                .code("P-MULTI-START-3")
                .name("第三起点")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        processMapper.insert(thirdProcess);
        MesProRouteProcessDO thirdStart = MesProRouteProcessDO.builder()
                .routeId(fixture.routeId())
                .processId(thirdProcess.getId())
                .sort(30)
                .keyFlag(false)
                .checkFlag(false)
                .build();
        routeProcessMapper.insert(thirdStart);
        MesProProcessDO mergeProcess = MesProProcessDO.builder()
                .code("P-MULTI-MERGE")
                .name("汇合工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        processMapper.insert(mergeProcess);
        MesProRouteProcessDO merge = MesProRouteProcessDO.builder()
                .routeId(fixture.routeId())
                .processId(mergeProcess.getId())
                .sort(40)
                .keyFlag(false)
                .checkFlag(false)
                .build();
        routeProcessMapper.insert(merge);
        insertRouteFlowEdge(fixture.routeId(), firstStart.getId(), merge.getId(), 1);
        insertRouteFlowEdge(fixture.routeId(), secondStart.getId(), merge.getId(), 2);
        insertRouteFlowEdge(fixture.routeId(), thirdStart.getId(), merge.getId(), 3);
        insertBatchUseConfig(fixture.routeId(), firstStart.getId(), "SEQUENTIAL",
                insertReport("RPT-MULTI-START-1", "多起点1"));
        insertBatchUseConfig(fixture.routeId(), secondStart.getId(), "SEQUENTIAL",
                insertReport("RPT-MULTI-START-2", "多起点2"));
        insertBatchUseConfig(fixture.routeId(), thirdStart.getId(), "SEQUENTIAL",
                insertReport("RPT-MULTI-START-3", "多起点3"));
        insertBatchUseConfig(fixture.routeId(), merge.getId(), "SEQUENTIAL",
                insertReport("RPT-MULTI-MERGE", "汇合表单"));
        refreshRouteVersionSnapshot(fixture.routeVersionId(), routeMapper.selectById(fixture.routeId()));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-MULTI-START-MERGE")
                .setRouteId(fixture.routeId()));

        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(created);
        assertEquals(4, routeTasks.size());
        EdhrBatchExecutionTaskRespVO firstStartTask = routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), firstStart.getId()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO secondStartTask = routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), secondStart.getId()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO thirdStartTask = routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), thirdStart.getId()))
                .findFirst()
                .orElseThrow();
        EdhrBatchExecutionTaskRespVO mergeTask = routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), merge.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.TRUE, firstStartTask.getAvailable(), firstStartTask.getGateMessage());
        assertEquals(Boolean.TRUE, secondStartTask.getAvailable(), secondStartTask.getGateMessage());
        assertEquals(Boolean.TRUE, thirdStartTask.getAvailable(), thirdStartTask.getGateMessage());
        assertEquals(Boolean.FALSE, mergeTask.getAvailable(), mergeTask.getGateMessage());
        assertEquals("直接前置工序批记录未全部填写完成", mergeTask.getGateMessage());
    }

    @Test
    void getUsesCurrentRouteGraphWhenBatchTasksWereCreatedFromCurrentRouteConfig() {
        Fixture fixture = insertRouteFixture(false, false);
        String staleRouteSnapshotJson = routeVersionMapper.selectById(fixture.routeVersionId()).getRouteSnapshotJson();
        routeProcessFlowEdgeMapper.delete(new LambdaUpdateWrapper<MesProRouteProcessFlowEdgeDO>()
                .eq(MesProRouteProcessFlowEdgeDO::getRouteId, fixture.routeId()));
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstStart = routeProcesses.get(0);
        MesProRouteProcessDO secondStart = routeProcesses.get(1);
        MesProProcessDO thirdProcess = MesProProcessDO.builder()
                .code("P-CURRENT-GRAPH-START-3")
                .name("当前图第三起点")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        processMapper.insert(thirdProcess);
        MesProRouteProcessDO thirdStart = MesProRouteProcessDO.builder()
                .routeId(fixture.routeId())
                .processId(thirdProcess.getId())
                .sort(30)
                .keyFlag(false)
                .checkFlag(false)
                .build();
        routeProcessMapper.insert(thirdStart);
        MesProProcessDO mergeProcess = MesProProcessDO.builder()
                .code("P-CURRENT-GRAPH-MERGE")
                .name("当前图汇合工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        processMapper.insert(mergeProcess);
        MesProRouteProcessDO merge = MesProRouteProcessDO.builder()
                .routeId(fixture.routeId())
                .processId(mergeProcess.getId())
                .sort(40)
                .keyFlag(false)
                .checkFlag(false)
                .build();
        routeProcessMapper.insert(merge);
        insertRouteFlowEdge(fixture.routeId(), firstStart.getId(), merge.getId(), 1);
        insertRouteFlowEdge(fixture.routeId(), secondStart.getId(), merge.getId(), 2);
        insertRouteFlowEdge(fixture.routeId(), thirdStart.getId(), merge.getId(), 3);
        insertBatchUseConfig(fixture.routeId(), firstStart.getId(), "SEQUENTIAL",
                insertReport("RPT-CURRENT-GRAPH-START-1", "当前图起点1"));
        insertBatchUseConfig(fixture.routeId(), secondStart.getId(), "SEQUENTIAL",
                insertReport("RPT-CURRENT-GRAPH-START-2", "当前图起点2"));
        insertBatchUseConfig(fixture.routeId(), thirdStart.getId(), "SEQUENTIAL",
                insertReport("RPT-CURRENT-GRAPH-START-3", "当前图起点3"));
        insertBatchUseConfig(fixture.routeId(), merge.getId(), "SEQUENTIAL",
                insertReport("RPT-CURRENT-GRAPH-MERGE", "当前图汇合表单"));
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(staleRouteSnapshotJson)
                .build());

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CURRENT-GRAPH-MULTI-START")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionRespVO refreshed = batchExecutionService.get(created.getId());

        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(refreshed);
        assertEquals(4, routeTasks.size());
        assertEquals(Boolean.TRUE, routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), firstStart.getId()))
                .findFirst()
                .orElseThrow()
                .getAvailable());
        assertEquals(Boolean.TRUE, routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), secondStart.getId()))
                .findFirst()
                .orElseThrow()
                .getAvailable());
        assertEquals(Boolean.TRUE, routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), thirdStart.getId()))
                .findFirst()
                .orElseThrow()
                .getAvailable());
        EdhrBatchExecutionTaskRespVO mergeTask = routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), merge.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.FALSE, mergeTask.getAvailable(), mergeTask.getGateMessage());
        assertEquals("直接前置工序批记录未全部填写完成", mergeTask.getGateMessage());
    }

    @Test
    void openTask_enforcesSequentialReportsAndNextProcessGate() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        String reportA = insertReport("RPT-GATE-A", "串行表1");
        String reportB = insertReport("RPT-GATE-B", "串行表2");
        String reportC = insertReport("RPT-GATE-C", "并行表1");
        insertBatchUseConfig(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", reportA, reportB);
        insertBatchUseConfig(fixture.routeId(), secondProcess.getId(), "PARALLEL", reportC);

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-GATE")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        EdhrBatchExecutionRespVO refreshedBatch = batchExecutionService.get(batch.getId());
        List<EdhrBatchExecutionTaskRespVO> tasks = routeTasks(refreshedBatch);
        assertEquals(Boolean.TRUE, tasks.get(0).getAvailable());
        assertEquals(Boolean.FALSE, tasks.get(1).getAvailable());
        assertEquals("前一张批记录未填写完成", tasks.get(1).getGateMessage());
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), firstProcess.getProcessId(), 81001L);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), secondProcess.getProcessId(), 81002L);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9101L).setStatus(0))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9102L).setStatus(0))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9103L).setStatus(0));

        assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setTaskId(tasks.get(1).getId())),
                PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED);

        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(batch.getId())
                .setTaskId(tasks.get(0).getId()));
        bindApprovedExecution(tasks.get(0).getId(), 9101L, true, true, true);
        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(batch.getId())
                .setTaskId(tasks.get(1).getId()));

        assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setTaskId(tasks.get(2).getId())),
                PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED);

        bindApprovedExecution(tasks.get(1).getId(), 9102L, true, true, true);
        EdhrBatchExecutionTaskOpenRespVO nextProcess = batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(batch.getId())
                .setTaskId(tasks.get(2).getId()));
        assertEquals(9103L, nextProcess.getExecutionId());
    }

    @Test
    void openTask_requiresAllCompanionFormsBeforeNextProcess() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        String mainReport = insertReport("RPT-COMP-MAIN", "主生产批记录");
        String lossReport = insertReport("RPT-COMP-LOSS", "损耗单");
        String inspectionReport = insertReport("RPT-COMP-IPQC", "过程检验单");
        String parameterReport = insertReport("RPT-COMP-PARAM", "参数记录表");
        String nextReport = insertReport("RPT-COMP-NEXT", "下一工序主表");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5011L, "PRODUCTION", 2),
                batchSlot(inspectionReport, "PROCESS_INSPECTION", "INTERNAL_RECORD", 5012L, "QUALITY", 3),
                batchSlot(parameterReport, "PARAMETER_RECORD", "INTERNAL_RECORD", 5013L, "EQUIPMENT", 4)
        ));
        insertBatchUseConfigWithSlots(fixture.routeId(), secondProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(nextReport, "MAIN", null, null, null, 1)
        ));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-COMPANION-GATE")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(created);
        List<EdhrBatchExecutionTaskRespVO> tasks = routeTasks(batchExecutionService.get(created.getId()));

        assertEquals(5, tasks.size());
        assertEquals(List.of("MAIN", "LOSS_REPORT", "PROCESS_INSPECTION", "PARAMETER_RECORD"),
                tasks.subList(0, 4).stream().map(EdhrBatchExecutionTaskRespVO::getFormSlotType).toList());
        assertEquals(firstProcess.getId(), tasks.get(0).getRouteProcessId());
        assertEquals(firstProcess.getId(), tasks.get(3).getRouteProcessId());
        assertEquals(Boolean.TRUE, tasks.get(0).getAvailable());
        assertEquals(Boolean.FALSE, tasks.get(1).getAvailable());
        assertEquals(Boolean.FALSE, tasks.get(4).getAvailable());
        assertEquals("直接前置工序批记录未全部填写完成", tasks.get(4).getGateMessage());
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), firstProcess.getProcessId(), 81011L);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), secondProcess.getProcessId(), 81012L);
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9111L).setStatus(0))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9112L).setStatus(0))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9113L).setStatus(0))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9114L).setStatus(0))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9115L).setStatus(0));

        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(created.getId())
                .setTaskId(tasks.get(0).getId()));
        bindApprovedExecution(tasks.get(0).getId(), 9111L, true, true, true);
        assertServiceException(() -> batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                        .setBatchExecutionId(created.getId())
                        .setTaskId(tasks.get(4).getId())),
                PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED);

        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(created.getId())
                .setTaskId(tasks.get(1).getId()));
        bindApprovedExecution(tasks.get(1).getId(), 9112L, true, true, true);
        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(created.getId())
                .setTaskId(tasks.get(2).getId()));
        bindApprovedExecution(tasks.get(2).getId(), 9113L, true, true, true);
        batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(created.getId())
                .setTaskId(tasks.get(3).getId()));
        bindApprovedExecution(tasks.get(3).getId(), 9114L, true, true, true);

        EdhrBatchExecutionTaskOpenRespVO nextProcess = batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(created.getId())
                .setTaskId(tasks.get(4).getId()));
        assertEquals(9115L, nextProcess.getExecutionId());
    }

    @Test
    void openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .min(Comparator.comparing(MesProRouteProcessDO::getSort))
                .orElseThrow();
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO version = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        String productInfoReport = insertVersionedReport(
                "RPT-PRODUCT-INFO-MEMBER", "产品信息", definitionId, version.getId(), 1, "MAIN");
        String processReport = insertVersionedReport(
                "RPT-PRODUCT-INFO-PROCESS", "粗洗工序生产记录", definitionId, version.getId(), 2, "MAIN");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(processReport, "MAIN", null, null, null, 1)
        ));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PRODUCT-INFO-MEMBER")
                .setRouteId(fixture.routeId()));

        List<EdhrBatchExecutionTaskRespVO> firstProcessTasks = routeTasks(created).stream()
                .filter(task -> Objects.equals(firstProcess.getSort(), task.getRouteProcessSort()))
                .toList();
        assertEquals(List.of(processReport), firstProcessTasks.stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportId)
                .toList());
        assertEquals(List.of("粗洗工序生产记录"), firstProcessTasks.stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordReportName)
                .toList());
        assertEquals(List.of(1), firstProcessTasks.stream()
                .map(EdhrBatchExecutionTaskRespVO::getBatchRecordSort)
                .toList());
        assertEquals(Boolean.TRUE, firstProcessTasks.get(0).getAvailable());
        EdhrBatchExecutionTaskRespVO productInfoTask = routeTasks(created).stream()
                .filter(task -> Objects.equals(productInfoReport, task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        assertEquals(80, productInfoTask.getRouteProcessSort());
        assertEquals("产品信息", productInfoTask.getProcessName());
        assertEquals(80, productInfoTask.getBatchRecordSort());
        assertEquals(Boolean.FALSE, productInfoTask.getAvailable());
        assertEquals("前序批记录表单未全部填写完成", productInfoTask.getGateMessage());
        assertTrue(firstProcessTasks.stream()
                .allMatch(task -> "MAIN".equals(task.getFormSlotType())
                        && "BATCH_RECORD".equals(task.getRecordCategory())));
    }

    @Test
    void openTask_allowsParallelCompanionFormsButStillBlocksNextProcess() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        String mainReport = insertReport("RPT-PAR-MAIN", "并行主表");
        String lossReport = insertReport("RPT-PAR-LOSS", "并行损耗单");
        String nextReport = insertReport("RPT-PAR-NEXT", "并行后续表");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "PARALLEL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5021L, "PRODUCTION", 2)
        ));
        insertBatchUseConfigWithSlots(fixture.routeId(), secondProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(nextReport, "MAIN", null, null, null, 1)
        ));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-COMPANION-PARALLEL")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(created);
        List<EdhrBatchExecutionTaskRespVO> tasks = routeTasks(batchExecutionService.get(created.getId()));

        assertEquals(Boolean.TRUE, tasks.get(0).getAvailable());
        assertEquals(Boolean.TRUE, tasks.get(1).getAvailable());
        assertEquals(Boolean.FALSE, tasks.get(2).getAvailable());
        assertEquals("直接前置工序批记录未全部填写完成", tasks.get(2).getGateMessage());
    }

    @Test
    void openOrCreate_rejectsCompanionFormWithoutSlotMetadata() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String mainReport = insertReport("RPT-BAD-MAIN", "配置缺失主表");
        String lossReport = insertReport("RPT-BAD-LOSS", "缺失槽位损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                new BatchSlot(lossReport, "UNKNOWN_SLOT", "INTERNAL_RECORD", "INTERNAL_TRACE", 5031L,
                        "REQUIRED", "PRODUCTION", "FINAL_DHR", null, 2,
                        "PROCESS", null, null)
        ));

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-COMPANION-BAD-CONFIG")
                        .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void openOrCreate_rejectsBatchSharedFormWithoutFillableScope() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String mainReport = insertReport("RPT-SHARED-MISSING-MAIN", "Shared main");
        String sharedInspection = insertReport("RPT-SHARED-MISSING-SCOPE", "Shared inspection");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                sharedBatchSlot(sharedInspection, "PROCESS_INSPECTION", "IPQC_MISSING_SCOPE", " ", 2)
        ));

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-SHARED-MISSING-SCOPE")
                        .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        verify(singleExecutionService, never()).openOrCreateByContext(argThat(command ->
                "BATCH_SHARED".equals(command.getInstanceScope())));
    }

    @Test
    void openOrCreate_reusesSingleFormCenterInstanceForBatchSharedSlotsWithDifferentBindingKeys() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        FormTemplateVersionDO sharedTemplate = insertPublishedFormTemplateVersion("批次共享动态主表");
        stubFormCenterInstanceIds(81001L, 81002L);
        insertBatchSharedFormCenterBinding(fixture.routeId(), firstProcess.getId(), sharedTemplate,
                "FB_SHARED_MAIN_A", "MAIN_SHARED_TEMPLATE", "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}");
        insertBatchSharedFormCenterBinding(fixture.routeId(), secondProcess.getId(), sharedTemplate,
                "FB_SHARED_MAIN_B", "MAIN_SHARED_TEMPLATE", "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":2,\"endRow\":3}]}");

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SHARED-FORM-CENTER-BINDINGS")
                .setRouteId(fixture.routeId()));

        List<MesProEdhrBatchExecutionTaskDO> sharedTasks = batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .toList();
        assertEquals(2, sharedTasks.size());
        assertEquals(Set.of(sharedTemplate.getTemplateId()),
                sharedTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFormTemplateId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("FB_SHARED_MAIN_A", "FB_SHARED_MAIN_B"),
                sharedTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFormBindingKey).collect(java.util.stream.Collectors.toSet()));
        assertTrue(sharedTasks.stream().allMatch(task -> task.getFormCenterInstanceId() != null));
        assertEquals(Set.of(81001L),
                sharedTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFormCenterInstanceId)
                        .collect(java.util.stream.Collectors.toSet()));
        ArgumentCaptor<FormInstanceCreateReqVO> requestCaptor =
                ArgumentCaptor.forClass(FormInstanceCreateReqVO.class);
        verify(formCenterRuntimeService, times(1)).createInstance(requestCaptor.capture(), any());
        assertEquals("EDHR_ROUTE_FORM_SHARED:" + created.getId() + ":MAIN_SHARED_TEMPLATE:"
                        + sharedTemplate.getTemplateId() + ":" + sharedTemplate.getId(),
                requestCaptor.getValue().getIdempotencyKey());
    }

    @Test
    void openOrCreate_createsSeparateFormCenterInstancesForProcessScopedSlotsWithSameTemplate() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        FormTemplateVersionDO template = insertPublishedFormTemplateVersion("工序独立动态表单");
        stubFormCenterInstanceIds(82001L, 82002L);
        insertBatchProcessFormCenterBinding(fixture.routeId(), firstProcess.getId(), template, "FB_PROCESS_A");
        insertBatchProcessFormCenterBinding(fixture.routeId(), secondProcess.getId(), template, "FB_PROCESS_B");

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PROCESS-FORM-CENTER-BINDINGS")
                .setRouteId(fixture.routeId()));

        List<MesProEdhrBatchExecutionTaskDO> processTasks = batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .filter(task -> "PROCESS".equals(task.getInstanceScope()))
                .filter(task -> Objects.equals(template.getTemplateId(), task.getFormTemplateId()))
                .toList();
        assertEquals(2, processTasks.size());
        assertEquals(Set.of(82001L, 82002L),
                processTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFormCenterInstanceId)
                        .collect(java.util.stream.Collectors.toSet()));
        verify(formCenterRuntimeService, times(2)).createInstance(any(FormInstanceCreateReqVO.class), any());
    }

    @Test
    void openOrCreate_createsSingleExecutionForBatchSharedFormAcrossProcesses() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        String firstMain = insertReport("RPT-SHARED-MAIN-A", "A 工序主表");
        String secondMain = insertReport("RPT-SHARED-MAIN-B", "B 工序主表");
        String sharedInspection = insertReport("RPT-SHARED-IPQC", "批次共享过程检验单");
        String firstScope = "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}";
        String secondScope = "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":2,\"endRow\":3}]}";
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(firstMain, "MAIN", null, null, null, 1),
                sharedBatchSlot(sharedInspection, "PROCESS_INSPECTION", "IPQC_SHARED", firstScope, 2)
        ));
        insertBatchUseConfigWithSlots(fixture.routeId(), secondProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(secondMain, "MAIN", null, null, null, 1),
                sharedBatchSlot(sharedInspection, "PROCESS_INSPECTION", "IPQC_SHARED", secondScope, 2)
        ));
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9311L).setStatus(0));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SHARED-IPQC")
                .setRouteId(fixture.routeId()));

        List<MesProEdhrBatchExecutionTaskDO> sharedTasks = batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .toList();
        assertEquals(2, sharedTasks.size());
        assertTrue(sharedTasks.stream().allMatch(task -> Objects.equals(9311L, task.getExecutionId())));
        assertEquals(List.of("IPQC_SHARED", "IPQC_SHARED"),
                sharedTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getSharedFormKey).toList());
        assertEquals(List.of(firstScope, secondScope),
                sharedTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFillableScopeJson).toList());
        MesProEdhrBatchExecutionTaskDO representativeTask = batchTaskMapper.selectByExecutionId(9311L);
        assertNotNull(representativeTask);
        assertEquals(sharedTasks.get(0).getId(), representativeTask.getId());
        verify(singleExecutionService, times(1)).openOrCreateByContext(argThat(command ->
                Objects.equals(created.getId(), command.getBatchExecutionId())
                        && "BATCH_SHARED".equals(command.getInstanceScope())
                        && "IPQC_SHARED".equals(command.getSharedFormKey())
                        && Objects.equals(firstProcess.getId(), command.getRouteProcessId())
                        && Objects.equals(firstProcess.getProcessId(), command.getProcessId())));
    }

    @Test
    void openOrCreate_withoutBatchSharedSlotsCreatesNoSharedTasks() {
        Fixture fixture = insertRouteFixture(true, true);

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-NO-SHARED-FORMS")
                .setRouteId(fixture.routeId()));

        assertTrue(batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .noneMatch(task -> "BATCH_SHARED".equals(task.getInstanceScope())));
        verify(singleExecutionService, never()).openOrCreateByContext(argThat(command ->
                "BATCH_SHARED".equals(command.getInstanceScope())));
    }

    @Test
    void openOrCreate_createsOptionalSharedTasksForArbitraryCount() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        String firstMain = insertReport("RPT-OPTIONAL-SHARED-MAIN-A", "A 工序主表");
        String secondMain = insertReport("RPT-OPTIONAL-SHARED-MAIN-B", "B 工序主表");
        String lossReport = insertReport("RPT-OPTIONAL-LOSS", "批次共享损耗单");
        String inspectionReport = insertReport("RPT-OPTIONAL-IPQC", "批次共享过程检验单");
        String parameterReport = insertReport("RPT-OPTIONAL-PARAMETER", "批次共享参数记录");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(firstMain, "MAIN", null, null, null, 1),
                optionalSharedBatchSlot(lossReport, "LOSS_REPORT", "LOSS_SHARED",
                        "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}", 2),
                optionalSharedBatchSlot(inspectionReport, "PROCESS_INSPECTION", "IPQC_SHARED",
                        "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":2,\"endRow\":3}]}", 3)
        ));
        insertBatchUseConfigWithSlots(fixture.routeId(), secondProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(secondMain, "MAIN", null, null, null, 1),
                optionalSharedBatchSlot(parameterReport, "PARAMETER_RECORD", "PARAMETER_SHARED",
                        "{\"ranges\":[{\"sourceTableIndex\":1,\"startRow\":4,\"endRow\":5}]}", 2)
        ));
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9411L).setStatus(0));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPTIONAL-SHARED-COUNT")
                .setRouteId(fixture.routeId()));

        List<MesProEdhrBatchExecutionTaskDO> sharedTasks = batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .toList();
        assertEquals(3, sharedTasks.size());
        assertEquals(List.of("LOSS_SHARED", "IPQC_SHARED", "PARAMETER_SHARED"),
                sharedTasks.stream().map(MesProEdhrBatchExecutionTaskDO::getSharedFormKey).toList());
        assertTrue(sharedTasks.stream().allMatch(task -> Boolean.FALSE.equals(task.getRequiredFlag())));
        assertTrue(sharedTasks.stream().allMatch(task -> "OPTIONAL".equals(task.getRequiredPolicy())));
    }

    @Test
    void openOrCreate_normalizesHistoricalBatchSharedSlotDefaults() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String mainReport = insertReport("RPT-HISTORICAL-DEFAULT-MAIN", "历史默认主表");
        String sharedInspection = insertReport("RPT-HISTORICAL-DEFAULT-IPQC", "历史默认过程检验单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                new BatchSlot(sharedInspection, "PROCESS_INSPECTION", null, null, null,
                        "OPTIONAL", null, null, null, 2,
                        "BATCH_SHARED", "IPQC_HISTORICAL_DEFAULTS",
                        "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}")
        ));
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9431L).setStatus(0));

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-HISTORICAL-DEFAULTS")
                .setRouteId(fixture.routeId()));

        MesProEdhrBatchExecutionTaskDO sharedTask = batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .filter(task -> "IPQC_HISTORICAL_DEFAULTS".equals(task.getSharedFormKey()))
                .findFirst()
                .orElseThrow();
        assertEquals("INTERNAL_RECORD", sharedTask.getRecordCategory());
        assertEquals("INTERNAL_TRACE", sharedTask.getValidationProfile());
        assertEquals("OPTIONAL", sharedTask.getRequiredPolicy());
        assertEquals("QUALITY", sharedTask.getOwnerRoleKey());
        assertEquals("FINAL_DHR", sharedTask.getArchiveVisibility());
    }

    @Test
    void optionalSharedRouteTask_skipMarksTaskDoneAndDoesNotBlockClose() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String mainReport = insertReport("RPT-OPTIONAL-SKIP-MAIN", "可选跳过主表");
        String sharedInspection = insertReport("RPT-OPTIONAL-SKIP-IPQC", "可选跳过过程检验单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                optionalSharedBatchSlot(sharedInspection, "PROCESS_INSPECTION", "IPQC_OPTIONAL_SKIP",
                        "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}", 2)
        ));
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9421L).setStatus(0));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-OPTIONAL-SHARED-SKIP")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO optionalTask = routeTasks(batch).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .findFirst()
                .orElseThrow();
        MesProEdhrWorkTaskDO workTask = insertWorkTask(batch, optionalTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        org.mockito.Mockito.doAnswer(invocation -> {
            String skipReason = invocation.getArgument(1);
            workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                    .setId(workTask.getId())
                    .setStatus(MesProEdhrWorkTaskStatus.DONE)
                    .setCompletedAt(LocalDateTime.now())
                    .setReason(skipReason)
                    .setRemark("OPTIONAL_SKIP:" + skipReason));
            return null;
        }).when(workTaskService).completeOptionalFillTaskBySkip(eq(workTask.getId()),
                eq("本工序无需填写过程检验单"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            EdhrBatchExecutionTaskRespVO activeOptionalTask = batchExecutionService.get(batch.getId()).getTasks().stream()
                    .filter(task -> task.getId().equals(optionalTask.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(workTask.getId(), activeOptionalTask.getActiveWorkTaskId());
            assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_FILL, activeOptionalTask.getActiveWorkTaskType());
            assertTrue(activeOptionalTask.getAllowedActions().contains("SKIP"),
                    "optional task should be skippable, available=" + activeOptionalTask.getAvailable()
                            + ", disabledReason=" + activeOptionalTask.getDisabledReason()
                            + ", actions=" + activeOptionalTask.getAllowedActions());
            assertNull(activeOptionalTask.getDisabledReason());

            EdhrBatchExecutionRespVO skipped = batchExecutionService.skipSpecialNode(optionalTask.getId(),
                    "本工序无需填写过程检验单", "secret", List.of());
            EdhrBatchExecutionTaskRespVO skippedTask = skipped.getTasks().stream()
                    .filter(task -> task.getId().equals(optionalTask.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED, skippedTask.getStatus());
            assertEquals(Boolean.FALSE, skippedTask.getRequiredFlag());
            assertTrue(skippedTask.getAllowedActions() == null || skippedTask.getAllowedActions().isEmpty());
        }

        MesProEdhrBatchExecutionTaskDO persistedTask = batchTaskMapper.selectById(optionalTask.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED, persistedTask.getStatus());
        assertEquals(10001L, persistedTask.getSkippedBy());
        assertTrue(persistedTask.getSpecialPayloadJson().contains("\"skipReason\":\"本工序无需填写过程检验单\""));
        MesProEdhrWorkTaskDO persistedWorkTask = workTaskMapper.selectById(workTask.getId());
        assertEquals(MesProEdhrWorkTaskStatus.DONE, persistedWorkTask.getStatus());
        assertNotNull(persistedWorkTask.getCompletedAt());
        verify(workTaskService).completeOptionalFillTaskBySkip(workTask.getId(), "本工序无需填写过程检验单");

        bindApprovedExecution(routeTasks(batch).stream()
                .filter(task -> "PROCESS".equals(task.getInstanceScope()))
                .findFirst()
                .orElseThrow()
                .getId(), 9422L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 0L);
        markReleasePrecheckPassed(batch.getId());
        EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("close with optional shared form skipped"));
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
    }

    @Test
    void openTask_lazilyBindsBatchSharedLossFormWhenFrozenExecutionMissing() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO firstProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String mainReport = insertReport("RPT-SHARED-LOSS-MAIN", "共享损耗主表");
        String sharedLoss = insertReport("RPT-SHARED-LOSS-LAZY", "共享损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "PARALLEL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                sharedBatchSlot(sharedLoss, "LOSS_REPORT", "LOSS_SHARED_LAZY",
                        "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}", 2)
        ));
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .build());
        when(singleExecutionService.openOrCreateByContext(any()))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                        .setId(9321L)
                        .setBatchRecordDefinitionId(932101L)
                        .setBatchRecordVersionId(932102L)
                        .setStatus(0));
        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SHARED-LOSS-LAZY")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(created);
        insertProductionTask(fixture.workOrderId(), fixture.routeId(), firstProcess.getProcessId(), 81031L);
        EdhrBatchExecutionTaskRespVO sharedRespTask = routeTasks(created).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .findFirst()
                .orElseThrow();
        MesProEdhrBatchExecutionTaskDO sharedTask = batchTaskMapper.selectListByBatchExecutionId(created.getId()).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .findFirst()
                .orElseThrow();
        batchTaskMapper.update(null, new LambdaUpdateWrapper<MesProEdhrBatchExecutionTaskDO>()
                .eq(MesProEdhrBatchExecutionTaskDO::getId, sharedTask.getId())
                .set(MesProEdhrBatchExecutionTaskDO::getExecutionId, null));
        MesProEdhrWorkTaskDO workTask = insertWorkTask(created, sharedRespTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        clearInvocations(singleExecutionService);

        EdhrBatchExecutionTaskOpenRespVO opened =
                openTaskAsFiller(created.getId(), sharedTask.getId(), workTask.getId());

        assertEquals(9321L, opened.getExecutionId());
        assertEquals("LOSS_REPORT", opened.getFormSlotType());
        assertEquals("BATCH_SHARED", opened.getInstanceScope());
        assertEquals("LOSS_SHARED_LAZY", opened.getSharedFormKey());
        assertEquals(workTask.getId(), opened.getExecutionPageQuery().get("workTaskId"));
        assertEquals(9321L, batchTaskMapper.selectById(sharedTask.getId()).getExecutionId());
        verify(singleExecutionService).openOrCreateByContext(argThat(command ->
                Objects.equals(created.getId(), command.getBatchExecutionId())
                        && Objects.equals(sharedTask.getId(), command.getTaskId())
                        && "BATCH_SHARED".equals(command.getInstanceScope())
                        && "LOSS_SHARED_LAZY".equals(command.getSharedFormKey())
                        && "LOSS_REPORT".equals(command.getFormSlotType())));
    }

    @Test
    void openTask_completesLaterBatchSharedRouteFormWhenSharedInstanceAlreadyEffective() {
        Fixture fixture = insertRouteFixture(false, false);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId());
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        MesProRouteProcessDO secondProcess = routeProcesses.get(1);
        FormTemplateVersionDO sharedTemplate = insertPublishedFormTemplateVersion("批次共享已生效损耗单");
        stubFormCenterInstanceIds(83001L);
        insertBatchSharedFormCenterBinding(fixture.routeId(), firstProcess.getId(), sharedTemplate,
                "FB_EFFECTIVE_SHARED_A", "EFFECTIVE_LOSS_SHARED",
                "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}");
        insertBatchSharedFormCenterBinding(fixture.routeId(), secondProcess.getId(), sharedTemplate,
                "FB_EFFECTIVE_SHARED_B", "EFFECTIVE_LOSS_SHARED",
                "{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":2,\"endRow\":3}]}");

        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SHARED-EFFECTIVE-OPEN")
                .setRouteId(fixture.routeId()));
        List<EdhrBatchExecutionTaskRespVO> sharedTasks = routeTasks(created).stream()
                .filter(task -> "BATCH_SHARED".equals(task.getInstanceScope()))
                .sorted(Comparator.comparing(EdhrBatchExecutionTaskRespVO::getRouteProcessSort))
                .toList();
        assertEquals(2, sharedTasks.size());
        assertEquals(sharedTasks.get(0).getFormCenterInstanceId(), sharedTasks.get(1).getFormCenterInstanceId());
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(sharedTasks.get(0).getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .setSubmittedAt(LocalDateTime.now())
                .setApprovedAt(LocalDateTime.now()));
        MesProEdhrWorkTaskDO workTask = insertWorkTask(created, sharedTasks.get(1), fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        when(formActionInstanceMapper.selectById(sharedTasks.get(1).getFormCenterInstanceId()))
                .thenReturn(FormActionInstanceDO.builder()
                .id(sharedTasks.get(1).getFormCenterInstanceId())
                .instanceCode("FCI-EFFECTIVE-SHARED")
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .policyId(1L)
                .applicantUserId(10001L)
                .status(FormInstanceStatus.EFFECTIVE.name())
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_ROUTE_FORM")
                .objectId(String.valueOf(sharedTasks.get(0).getId()))
                .objectVersion(String.valueOf(created.getId()))
                .actionCode("EDHR_RF_" + fixture.routeVersionId() + "_FB_EFFECTIVE_SHARED_A")
                .objectState("ACTIVE")
                .idempotencyKey("EFFECTIVE_SHARED_IDEM")
                .businessContextJson("{}")
                .formDataJson("{}")
                .build());

        EdhrBatchExecutionTaskOpenRespVO opened = openTaskAsFiller(
                created.getId(), sharedTasks.get(1).getId(), workTask.getId());

        assertEquals(sharedTasks.get(1).getFormCenterInstanceId(), opened.getFormCenterInstanceId());
        verify(workTaskService).completeRouteFormFillAndCreateNextFill(sharedTasks.get(1).getId(), 10001L);
    }

    @Test
    void get_returnsRoleSpecificActionsForActiveWorkTasks() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ROLE-ACTIONS")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        List<EdhrBatchExecutionTaskRespVO> tasks = routeTasks(batch);
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, tasks.get(0), fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        MesProEdhrWorkTaskDO approveTask = insertWorkTask(batch, tasks.get(1), fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_APPROVE, 10002L);

        EdhrBatchExecutionRespVO fillerView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            fillerView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO fillerTask = routeTask(fillerView, 0);
        assertEquals("FILLER", fillerTask.getCurrentUserRole());
        assertEquals(fillTask.getId(), fillerTask.getActiveWorkTaskId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_FILL, fillerTask.getActiveWorkTaskType());
        assertTrue(fillerTask.getAllowedActions().contains("OPEN_FORM"));
        assertNull(fillerTask.getDisabledReason());
        EdhrBatchExecutionTaskRespVO unrelatedApproveTask = routeTask(fillerView, 1);
        assertEquals("UNRELATED", unrelatedApproveTask.getCurrentUserRole());
        assertEquals(approveTask.getId(), unrelatedApproveTask.getActiveWorkTaskId());
        assertEquals("当前用户不是该节点的批准人", unrelatedApproveTask.getDisabledReason());

        EdhrBatchExecutionRespVO approverView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10002L);
            approverView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO approverTask = routeTask(approverView, 1);
        assertEquals("APPROVER", approverTask.getCurrentUserRole());
        assertEquals(approveTask.getId(), approverTask.getActiveWorkTaskId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE, approverTask.getActiveWorkTaskType());
        assertEquals(List.of("APPROVE", "REJECT"), approverTask.getAllowedActions());
        assertNull(approverTask.getDisabledReason());

        EdhrBatchExecutionRespVO unrelatedView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10003L);
            unrelatedView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO unrelatedFillTask = routeTask(unrelatedView, 0);
        assertEquals("UNRELATED", unrelatedFillTask.getCurrentUserRole());
        assertEquals(fillTask.getId(), unrelatedFillTask.getActiveWorkTaskId());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_FILL, unrelatedFillTask.getActiveWorkTaskType());
        assertEquals(List.of(), unrelatedFillTask.getAllowedActions());
        assertEquals("当前用户不是该节点的填写人", unrelatedFillTask.getDisabledReason());
    }

    @Test
    void get_returnsAttachmentOwnerActionsForPendingSpecialNodes() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-ATTACHMENT-OWNER-ACTIONS")
                .setRouteId(fixture.routeId()));
        configureBatchSpecialAttachmentOwners(batch.getId(), 188L, 190L);
        insertCloseAssignmentRule(fixture.routeId(), 189L);
        Long specialTaskId = batch.getTasks().stream()
                .filter(task -> task.getBatchRecordReportId() == null)
                .findFirst()
                .orElseThrow()
                .getId();

        EdhrBatchExecutionRespVO ownerView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            ownerView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO ownerTask = ownerView.getTasks().stream()
                .filter(task -> task.getId().equals(specialTaskId))
                .findFirst()
                .orElseThrow();
        assertEquals("FILLER", ownerTask.getCurrentUserRole());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_CLOSE, ownerTask.getActiveWorkTaskType());
        assertEquals(List.of("CLOSE"), ownerTask.getAllowedActions());
        assertNull(ownerTask.getDisabledReason());

        EdhrBatchExecutionRespVO unrelatedView;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            unrelatedView = batchExecutionService.get(batch.getId());
        }

        EdhrBatchExecutionTaskRespVO unrelatedTask = unrelatedView.getTasks().stream()
                .filter(task -> task.getId().equals(specialTaskId))
                .findFirst()
                .orElseThrow();
        assertEquals("UNRELATED", unrelatedTask.getCurrentUserRole());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_CLOSE, unrelatedTask.getActiveWorkTaskType());
        assertEquals(List.of(), unrelatedTask.getAllowedActions());
        assertEquals("当前用户不是该节点的批记录附件填写人", unrelatedTask.getDisabledReason());
    }

    @Test
    void close_pendingEdhrRecordChange_blocksClose() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PENDING-CHANGE")
                .setRouteId(fixture.routeId()));
        Long taskId = batchTaskMapper.selectListByBatchExecutionId(created.getId()).get(0).getId();
        Long executionId = randomLongId();
        bindApprovedExecution(taskId, executionId, true, true, true);
        recordChangeEventMapper.insert(MesProEdhrRecordChangeEventDO.builder()
                .changeCode("CHG-PENDING-" + randomLongId())
                .changeType("SUPPLEMENT")
                .targetScope("EXECUTION")
                .batchExecutionId(created.getId())
                .executionId(executionId)
                .changeStatus("SUBMITTED")
                .reasonCategory("PRODUCTION_SUPPLEMENT")
                .reasonText("补录审批未完成")
                .build());

        assertCloseBlocked(() -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                        .setId(created.getId())
                        .setPassword("secret")
                        .setComment("close")));
    }

    @Test
    void get_pendingVoidChange_locksNormalTaskActionsAndOnlyAllowsWithdraw() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PENDING-VOID-LOCK")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, batchTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        Long changeEventId = randomLongId();
        recordChangeEventMapper.insert(MesProEdhrRecordChangeEventDO.builder()
                .id(changeEventId)
                .changeCode("CHG-PENDING-VOID-" + randomLongId())
                .changeType("VOID")
                .targetScope("BATCH")
                .batchExecutionId(batch.getId())
                .changeStatus("SUBMITTED")
                .reasonCategory("PRODUCTION_VOID")
                .reasonText("作废申请待审批")
                .requestedBy(10001L)
                .requestedAt(LocalDateTime.now())
                .bpmProcessInstanceId("BPM-PENDING-VOID-" + randomLongId())
                .build());

        EdhrBatchExecutionRespVO result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            result = batchExecutionService.get(batch.getId());
        }

        assertEquals(changeEventId, result.getPendingVoidChangeEventId());
        assertEquals("SUBMITTED", result.getPendingVoidChangeStatus());
        assertEquals(Boolean.TRUE, result.getCanWithdrawVoidRequest());
        assertFalse(result.getCanClose());
        assertFalse(result.getCanArchive());
        EdhrBatchExecutionTaskRespVO lockedTask = routeTask(result, 0);
        assertEquals(fillTask.getId(), lockedTask.getActiveWorkTaskId());
        assertEquals(List.of(), lockedTask.getAllowedActions());
        assertEquals("作废申请待处理，只能撤回作废申请", lockedTask.getDisabledReason());
    }

    @Test
    void get_releasePendingApproval_locksNormalTaskActions() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PENDING-RELEASE-LOCK")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, batchTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        releaseTransactionMapper.insert(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batch.getId())
                .setReleaseCode("REL-PENDING-" + randomLongId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBlockingCheckCount(0)
                .setFailedCheckCount(0)
                .setRequiredCheckCount(6));

        EdhrBatchExecutionRespVO result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            result = batchExecutionService.get(batch.getId());
        }

        assertFalse(result.getCanClose());
        assertFalse(result.getCanArchive());
        assertEquals(Boolean.TRUE, result.getReleaseActionLocked());
        assertEquals("放行审批待处理，只能处理放行审批或撤回放行", result.getReleaseActionLockReason());
        EdhrBatchExecutionTaskRespVO lockedTask = routeTask(result, 0);
        assertEquals(fillTask.getId(), lockedTask.getActiveWorkTaskId());
        assertEquals(List.of(), lockedTask.getAllowedActions());
        assertEquals("放行审批待处理，只能处理放行审批或撤回放行", lockedTask.getDisabledReason());
    }

    @Test
    void releasePendingApproval_blocksCloseArchiveAndQualityReject() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PENDING-RELEASE-ACTION-LOCK")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7711L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7712L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 0L);
        releaseTransactionMapper.insert(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batch.getId())
                .setReleaseCode("REL-ACTION-LOCK-" + randomLongId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBlockingCheckCount(0)
                .setFailedCheckCount(0)
                .setRequiredCheckCount(6));

        assertServiceException(() -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                        .setId(batch.getId())
                        .setPassword("secret")
                        .setComment("close while release pending")),
                PRO_EDHR_RELEASE_STATUS_INVALID);

        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9977L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9977L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        assertServiceException(() -> batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setArtifactType("BATCH_FINAL_PDF")
                        .setWorkTaskId(9977L)),
                PRO_EDHR_RELEASE_STATUS_INVALID);
        assertServiceException(() -> batchExecutionService.qualityReject(new EdhrBatchExecutionQualityRejectReqVO()
                        .setId(batch.getId())
                        .setPassword("secret")
                        .setReason("release pending")),
                PRO_EDHR_RELEASE_STATUS_INVALID);
    }

    @Test
    void close_goldenFingerBypassesReleaseAndVoidActionLocks() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-GF-ACTION-LOCK-BYPASS")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7731L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7732L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 0L);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batch.getId())
                .setReleaseCode("REL-GF-ACTION-LOCK-" + randomLongId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBlockingCheckCount(0)
                .setFailedCheckCount(0)
                .setRequiredCheckCount(6));
        recordChangeEventMapper.insert(MesProEdhrRecordChangeEventDO.builder()
                .changeCode("CHG-GF-ACTION-LOCK-" + randomLongId())
                .changeType("VOID")
                .targetScope("BATCH")
                .batchExecutionId(batch.getId())
                .changeStatus("SUBMITTED")
                .reasonCategory("PRODUCTION_VOID")
                .reasonText("作废申请待审批")
                .requestedBy(10001L)
                .requestedAt(LocalDateTime.now())
                .bpmProcessInstanceId("BPM-GF-ACTION-LOCK-" + randomLongId())
                .build());
        when(goldenFingerPermissionService.hasGoldenFingerPermission(0L)).thenReturn(true);

        EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("golden finger closes with action locks"));

        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
    }

    @Test
    void pendingVoidRequest_blocksArchiveAndQualityReject() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PENDING-VOID-ACTION-LOCK")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7721L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7722L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 0L);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        recordChangeEventMapper.insert(MesProEdhrRecordChangeEventDO.builder()
                .changeCode("CHG-PENDING-VOID-ACTION-LOCK-" + randomLongId())
                .changeType("VOID")
                .targetScope("BATCH")
                .batchExecutionId(batch.getId())
                .changeStatus("SUBMITTED")
                .reasonCategory("PRODUCTION_VOID")
                .reasonText("作废申请待审批")
                .requestedBy(10001L)
                .requestedAt(LocalDateTime.now())
                .bpmProcessInstanceId("BPM-PENDING-VOID-ACTION-LOCK-" + randomLongId())
                .build());
        when(workTaskService.validateArchiveTask(9988L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9988L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        assertServiceException(() -> batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                        .setBatchExecutionId(batch.getId())
                        .setArtifactType("BATCH_FINAL_PDF")
                        .setWorkTaskId(9988L)),
                PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED);
        assertServiceException(() -> batchExecutionService.qualityReject(new EdhrBatchExecutionQualityRejectReqVO()
                        .setId(batch.getId())
                        .setPassword("secret")
                        .setReason("void pending")),
                PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED);
    }

    @Test
    void get_voidedBatchExecutionTerminalStateClearsNormalTaskActions() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-VOIDED-TERMINAL-LOCK")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO batchTask = routeTask(batch, 0);
        MesProEdhrWorkTaskDO fillTask = insertWorkTask(batch, batchTask, fixture,
                MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED));

        EdhrBatchExecutionRespVO result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10001L);
            result = batchExecutionService.get(batch.getId());
        }

        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED, result.getStatus());
        assertNull(result.getPendingVoidChangeEventId());
        assertEquals(Boolean.FALSE, result.getCanWithdrawVoidRequest());
        assertFalse(result.getCanClose());
        assertFalse(result.getCanArchive());
        EdhrBatchExecutionTaskRespVO lockedTask = routeTask(result, 0);
        assertEquals(fillTask.getId(), lockedTask.getActiveWorkTaskId());
        assertEquals(List.of(), lockedTask.getAllowedActions());
        assertEquals("批次已作废，只能追溯审计", lockedTask.getDisabledReason());
    }

    @Test
    void close_withBlockersReturnsDetailsAndKeepsBatchUnchanged() {
        Fixture fixture = insertRouteFixture(true, false);
        EdhrBatchExecutionRespVO created = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CLOSE-BLOCKERS")
                .setRouteId(fixture.routeId()));
        MesProEdhrBatchExecutionDO before = batchExecutionMapper.selectById(created.getId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                        .setId(created.getId())
                        .setPassword("secret")
                        .setComment("close with blockers")));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_CLOSE_BLOCKED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("第一工序: 未打开电子批记录"));
        assertFalse(exception.getMessage().contains("成品检卷宗项"));
        MesProEdhrBatchExecutionDO after = batchExecutionMapper.selectById(created.getId());
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getClosedAt(), after.getClosedAt());
        assertEquals(before.getCloseSignatureId(), after.getCloseSignatureId());
    }

    @Test
    void closeCreatesArchiveWorkTaskAfterBatchClosedWhenFinalInspectionDossierPending() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CLOSE-ARCHIVE-TASK")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7011L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7012L, true, true, true);
        skipAllSpecialNodes(batch);
        markBatchOwner(batch.getId(), 0L);
        markReleasePrecheckPassed(batch.getId());

        LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 15, 30);
        EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                .setId(batch.getId())
                .setPassword("secret")
                .setComment("close batch and create archive task")
                .setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                        .setSelectedSignedAt(selectedSignedAt)
                        .setSelectedTimeZone("Asia/Shanghai")
                        .setSelectedTimeReason("批执行关闭按现场复核完成时间显示")));

        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
        MesProEdhrBatchExecutionDO updated = batchExecutionMapper.selectById(batch.getId());
        MesProEdhrBatchExecutionSignatureDO signature = batchSignatureMapper.selectById(updated.getCloseSignatureId());
        assertEquals("BATCH_CLOSE", signature.getActionType());
        assertEquals(selectedSignedAt, signature.getSelectedSignedAt());
        assertEquals(selectedSignedAt, signature.getSignatureDisplayAt());
        assertEquals("USER_SELECTED", signature.getSignatureTimeMode());
        assertEquals("Asia/Shanghai", signature.getSelectedTimeZone());
        assertEquals("批执行关闭按现场复核完成时间显示", signature.getSelectedTimeReason());
        assertEquals("EDHR_SIGNATURE_TIME_V1", signature.getSelectedTimePolicyVersion());
        assertNotNull(signature.getSelectedTimeAuditHash());
        ArgumentCaptor<MesProEdhrBatchExecutionDO> batchCaptor =
                ArgumentCaptor.forClass(MesProEdhrBatchExecutionDO.class);
        verify(workTaskService).createArchiveTaskAfterBatchClose(batchCaptor.capture());
        assertEquals(batch.getId(), batchCaptor.getValue().getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, batchCaptor.getValue().getStatus());
    }

    @Test
    void close_wrongSignaturePassword_rejectsWithoutSignatureOrStatusChange() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CLOSE-WRONG-PASSWORD")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7111L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7112L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 188L);
        markReleasePrecheckPassed(batch.getId());
        MesProEdhrBatchExecutionDO before = batchExecutionMapper.selectById(batch.getId());
        int signatureCountBefore = batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).size();
        doThrow(new ServiceException(USER_PASSWORD_FAILED))
                .when(adminUserApi).validatePassword(188L, "wrong-pass");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                            .setId(batch.getId())
                            .setPassword("wrong-pass")
                            .setComment("wrong password close")));
            assertEquals(USER_PASSWORD_FAILED.getCode(), exception.getCode());
        }

        MesProEdhrBatchExecutionDO after = batchExecutionMapper.selectById(batch.getId());
        assertEquals(before.getStatus(), after.getStatus());
        assertNull(after.getClosedAt());
        assertNull(after.getCloseSignatureId());
        List<MesProEdhrBatchExecutionSignatureDO> signatures =
                batchSignatureMapper.selectListByBatchExecutionId(batch.getId());
        assertEquals(signatureCountBefore, signatures.size());
        assertTrue(signatures.stream().noneMatch(item -> "BATCH_CLOSE".equals(item.getActionType())));
    }

    @Test
    void close_rejectsNonBatchCreatorAndAllowsBatchOwner() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CLOSE-OWNER")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7021L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7022L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchOwner(batch.getId(), 188L);
        markReleasePrecheckPassed(batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(189L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                            .setId(batch.getId())
                            .setPassword("secret")
                            .setComment("non owner close")));
            assertEquals(PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID.getCode(), exception.getCode());
        }

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                    .setId(batch.getId())
                    .setPassword("secret")
                    .setComment("owner close"));
            assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
            assertEquals(188L, batchExecutionMapper.selectById(batch.getId()).getClosedBy());
        }
    }

    @Test
    void close_usesRouteCloseRuleInsteadOfBatchCreator() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CLOSE-ROUTE-RULE")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 7031L, true, true, true);
        bindApprovedExecution(routeTask(batch, 1).getId(), 7032L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        markBatchCreatorOnly(batch.getId(), 188L);
        insertCloseAssignmentRule(fixture.routeId(), 189L);
        markReleasePrecheckPassed(batch.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                            .setId(batch.getId())
                            .setPassword("secret")
                            .setComment("creator is not route close owner")));
            assertEquals(PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID.getCode(), exception.getCode());
        }

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(189L);
            EdhrBatchExecutionRespVO closed = batchExecutionService.close(new EdhrBatchExecutionCloseReqVO()
                    .setId(batch.getId())
                    .setPassword("secret")
                    .setComment("route close owner"));
            assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED, closed.getStatus());
            assertEquals(189L, batchExecutionMapper.selectById(batch.getId()).getClosedBy());
        }
    }

    @Test
    void qualityReject_beforeReleasePrecheckStage_rejected() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-QUALITY-REJECT-BEFORE-PRECHECK")
                .setRouteId(fixture.routeId()));

        assertServiceException(() -> batchExecutionService.qualityReject(new EdhrBatchExecutionQualityRejectReqVO()
                        .setId(batch.getId())
                        .setReason("未进入放行预检阶段不得质量拒收。")
                        .setPassword("sign-pass")),
                PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
    }

    @Test
    void qualityReject_unarchivedBatch_marksRejectedSignsAndCancelsActiveTasks() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-QUALITY-REJECT")
                .setRouteId(fixture.routeId()));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setClosedAt(LocalDateTime.now())
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"));

        EdhrBatchExecutionRespVO result;
        LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 15, 10);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            result = batchExecutionService.qualityReject(new EdhrBatchExecutionQualityRejectReqVO()
                    .setId(batch.getId())
                    .setReason("质量负责人终态拒收。")
                    .setPassword("sign-pass")
                    .setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                            .setSelectedSignedAt(selectedSignedAt)
                            .setSelectedTimeZone("Asia/Shanghai")
                            .setSelectedTimeReason("质量拒收按纸面终判时间显示")));
        }

        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED, result.getStatus());
        assertEquals(188L, result.getRejectedBy());
        assertNotNull(result.getRejectedAt());
        assertEquals("质量负责人终态拒收。", result.getRejectReason());
        MesProEdhrBatchExecutionDO updated = batchExecutionMapper.selectById(batch.getId());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED, updated.getStatus());
        assertEquals(188L, updated.getRejectedBy());
        assertNotNull(updated.getRejectedAt());
        assertEquals("质量负责人终态拒收。", updated.getRejectReason());
        assertNotNull(updated.getRejectSignatureId());
        MesProEdhrBatchExecutionSignatureDO signature = batchSignatureMapper.selectById(updated.getRejectSignatureId());
        assertEquals("QUALITY_REJECT", signature.getActionType());
        assertEquals("质量负责人终态拒收。", signature.getComment());
        assertEquals(188L, signature.getActorId());
        assertEquals(selectedSignedAt, signature.getSelectedSignedAt());
        assertEquals(selectedSignedAt, signature.getSignatureDisplayAt());
        assertEquals("USER_SELECTED", signature.getSignatureTimeMode());
        assertEquals("Asia/Shanghai", signature.getSelectedTimeZone());
        assertEquals("质量拒收按纸面终判时间显示", signature.getSelectedTimeReason());
        assertEquals("EDHR_SIGNATURE_TIME_V1", signature.getSelectedTimePolicyVersion());
        assertNotNull(signature.getSelectedTimeAuditHash());
        verify(workTaskService).cancelActiveTasksByBatch(batch.getId(), "质量终态拒收：质量负责人终态拒收。");
    }

    @Test
    void qualityReject_archivedBatch_rejected() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-QUALITY-ARCHIVED")
                .setRouteId(fixture.routeId()));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED));

        assertServiceException(() -> batchExecutionService.qualityReject(new EdhrBatchExecutionQualityRejectReqVO()
                        .setId(batch.getId())
                        .setReason("已归档后不得质量终态拒收。")
                        .setPassword("sign-pass")),
                PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
    }

    @Test
    void syncStatus_revisionDraftFromRejectedExecutionKeepsBatchInReworkRequired() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-REWORK-REVISION")
                .setRouteId(fixture.routeId()));
        Long taskId = routeTask(batch, 0).getId();

        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(7301L)
                .setExecutionCode("BRE-REJECTED-7301")
                .setWorkOrderId(fixture.workOrderId())
                .setWorkOrderCode("WO")
                .setBatchCode("BATCH-REWORK-REVISION")
                .setStatus(2)
                .setSheetLayoutJson("{}")
                .setMetaJson("{}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setRevisionRootExecutionId(7301L)
                .setRevisionNo(1)
                .setSupersededByExecutionId(7302L)
                .setActiveRevisionFlag(false)
                .setDomainTraceStatus("VERIFIED"));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(7302L)
                .setExecutionCode("BRE-REVISION-7302")
                .setWorkOrderId(fixture.workOrderId())
                .setWorkOrderCode("WO")
                .setBatchCode("BATCH-REWORK-REVISION")
                .setStatus(0)
                .setSheetLayoutJson("{}")
                .setMetaJson("{}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setRevisionRootExecutionId(7301L)
                .setRevisionNo(2)
                .setSourceRejectedExecutionId(7301L)
                .setRevisionReason("补充记录")
                .setRevisionParentHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setActiveRevisionFlag(true)
                .setDomainTraceStatus("VERIFIED"));
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(taskId)
                .setExecutionId(7302L)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT));

        EdhrBatchExecutionRespVO result = batchExecutionService.syncStatus(batch.getId());

        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REWORK_REQUIRED, result.getStatus());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REWORK_REQUIRED,
                routeTask(result, 0).getStatus());
        assertFalse(result.getCanClose());
        assertTrue(result.getCloseBlockers().stream().anyMatch(item -> item.contains("需返工修订")));
        verify(operationAuditService, atLeastOnce()).record(argThat(command ->
                "SYNC".equals(command.getOperationType())
                        && "BATCH_EXECUTION".equals(command.getObjectType())
                        && String.valueOf(batch.getId()).equals(command.getObjectId())
                        && "SUCCESS".equals(command.getResultStatus())));
    }

    @Test
    void syncStatus_reconcilesResolvedSpecialNodeAdvance() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SYNC-SPECIAL-ADVANCE")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO specialNode = batch.getTasks().stream()
                .filter(task -> "INCOMING_INSPECTION_REPORT".equals(task.getNodeType()))
                .findFirst()
                .orElseThrow();
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(specialNode.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED)
                .setSkippedBy(188L)
                .setSkippedAt(LocalDateTime.of(2026, 7, 22, 12, 10))
                .setSpecialPayloadJson("{\"skipReason\":\"历史跳过后补同步\"}"));
        clearInvocations(workTaskService);

        batchExecutionService.syncStatus(batch.getId());

        verify(workTaskService).createNextFillAfterSpecialNodeResolved(argThat(task ->
                task != null
                        && specialNode.getId().equals(task.getId())
                        && Objects.equals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED,
                        task.getStatus())));
    }

    @Test
    void generateArchive_requiresClosedBatch() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ARCHIVE")
                .setRouteId(fixture.routeId()));

        assertServiceException(() -> batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9901L)),
                PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_CLOSED);

        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9902L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9902L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9902L));
        assertNotNull(archive.getId());
        MesProEdhrBatchExecutionArchiveDO deletedArchive = new MesProEdhrBatchExecutionArchiveDO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setArchiveVersion(99)
                .setArchiveStatus("SEALED")
                .setFileName("deleted-hash-only.pdf")
                .setContentType("application/pdf")
                .setFileSize(694L)
                .setContentHash("deleted-hash-only")
                .setSourceManifestJson("{deleted=true}");
        deletedArchive.setDeleted(true);
        batchArchiveMapper.insert(deletedArchive);
        assertEquals(1, batchArchiveMapper.selectListByBatchExecutionId(batch.getId()).size());
        assertEquals(archive.getId(), batchExecutionService.getLatestArchive(batch.getId()).getId());
        assertServiceException(() -> batchExecutionService.downloadArchive(deletedArchive.getId()),
                PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_EXISTS);
        verify(workTaskService).validateArchiveTask(9902L, batch.getId());
        verify(workTaskService).completeArchiveTask(9902L, batch.getId());

        EdhrBatchExecutionArchiveDownloadRespVO download = batchExecutionService.downloadArchive(archive.getId());
        assertEquals(archive.getFileName(), download.getFileName());
        assertEquals("application/pdf", download.getContentType());
        assertTrue(download.getContent().length > 0);
        assertEquals("%PDF-1.4", new String(download.getContent(), 0, 8, StandardCharsets.UTF_8));
    }

    @Test
    void generateArchive_manifestUsesPrintableSnapshotSchema() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PRINTABLE-SCHEMA")
                .setRouteId(fixture.routeId()));
        bindApprovedExecutionPrintable(routeTask(batch, 0).getId(), 8401L, "首件确认");
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9933L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9933L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9933L));

        String manifest = batchArchiveMapper.selectById(archive.getId()).getSourceManifestJson();
        assertTrue(manifest.contains("\"schemaVersion\":\"EDHR_BATCH_PRINTABLE_ARCHIVE_V1\""));
        assertTrue(manifest.contains("\"bodyForms\""));
        assertTrue(manifest.contains("\"appendixSpecialNodes\""));
        assertTrue(manifest.contains("\"sheetLayoutJson\""));
        assertTrue(manifest.contains("\"executionSnapshotJson\""));
        assertTrue(manifest.contains("\"cellValuesJson\""));
        assertTrue(manifest.contains("\"signatureCellMarkers\""));
        assertTrue(manifest.contains("\"signatureRecords\""));
        assertTrue(manifest.contains("\"remark\":\"首件确认\""));
        assertTrue(manifest.contains("\"executionCode\":\"BRE-8401\""));
        assertTrue(manifest.contains("\"processName\":\"第一工序\""));
        assertTrue(manifest.contains("\"nodeType\":\"INCOMING_INSPECTION_REPORT\""));
        assertTrue(manifest.contains("批次演练跳过无模板特殊节点"));
    }

    @Test
    void generateArchive_manifestIncludesEdhrRecordChangeEvents() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ARCHIVE-CHANGE")
                .setRouteId(fixture.routeId()));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        recordChangeEventMapper.insert(new MesProEdhrRecordChangeEventDO()
                .setChangeCode("EDHR-VOID-MANIFEST")
                .setChangeType("VOID")
                .setChangeStatus("EFFECTIVE")
                .setTargetScope("EXECUTION")
                .setBatchExecutionId(batch.getId())
                .setExecutionId(88001L)
                .setPreviousStatus("3")
                .setNewStatus("4")
                .setReasonCategory("DATA_ERROR")
                .setReasonText("归档清单应包含受控作废事件")
                .setRequestedBy(1L)
                .setRequestedAt(LocalDateTime.now().minusMinutes(5))
                .setApprovedBy(2L)
                .setApprovedAt(LocalDateTime.now().minusMinutes(1))
                .setEffectiveAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9920L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9920L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9920L));

        String manifest = batchArchiveMapper.selectById(archive.getId()).getSourceManifestJson();
        assertTrue(manifest.contains("changeEvents"));
        assertTrue(manifest.contains("EDHR-VOID-MANIFEST"));
        assertTrue(manifest.contains("\"changeType\":\"VOID\""));
        assertTrue(manifest.contains("\"changeStatus\":\"EFFECTIVE\""));
    }

    @Test
    void generateArchive_manifestIncludesAttachmentManifest() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ATTACH-MANIFEST")
                .setRouteId(fixture.routeId()));
        Long taskId = routeTask(batch, 0).getId();
        bindApprovedExecution(taskId, 8201L, true, true, true);
        insertAttachmentEvent(batch.getId(), taskId, 8201L, "archive-photo.jpg",
                "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9930L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9930L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9930L));

        String manifest = batchArchiveMapper.selectById(archive.getId()).getSourceManifestJson();
        assertTrue(manifest.contains("\"attachmentCount\":1"));
        assertTrue(manifest.contains("attachmentManifests"));
        assertTrue(manifest.contains("archive-photo.jpg"));
        assertTrue(manifest.contains("\"fieldKey\":\"photo\""));
        assertTrue(manifest.contains("\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\""));
        assertTrue(manifest.contains("\"attachmentHash\":\"dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd\""));
        assertTrue(manifest.contains("\"auditBatchId\":9301"));
        assertTrue(manifest.contains("\"signatureId\":9401"));
    }

    @Test
    void generateArchive_downloadPdfContainsQaReadableBatchSections() throws Exception {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-QA-PDF")
                .setRouteId(fixture.routeId()));
        skipAllSpecialNodes(batch);
        Long taskId = routeTask(batch, 0).getId();
        bindApprovedExecutionPrintable(taskId, 8301L, "归档备注：首件确认完成");
        insertAttachmentEvent(batch.getId(), taskId, 8301L, "qa-readable-photo.jpg",
                "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        completeFinalInspectionDossier(batch.getId());
        recordChangeEventMapper.insert(new MesProEdhrRecordChangeEventDO()
                .setChangeCode("EDHR-REWORK-QA-PDF")
                .setChangeType("REWORK")
                .setChangeStatus("EFFECTIVE")
                .setTargetScope("EXECUTION")
                .setBatchExecutionId(batch.getId())
                .setExecutionId(8301L)
                .setPreviousStatus("REJECTED")
                .setNewStatus("APPROVED")
                .setReasonCategory("QA_REVIEW")
                .setReasonText("QA PDF 应展示返工记录")
                .setEffectiveAt(LocalDateTime.now()));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9932L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9932L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9932L));
        EdhrBatchExecutionArchiveDownloadRespVO download = batchExecutionService.downloadArchive(archive.getId());

        String text;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(download.getContent()))) {
            text = new PDFTextStripper().getText(document);
        }
        assertTrue(text.contains("打印版 eDHR 已填表单归档"));
        assertTrue(text.contains("批次摘要"));
        assertTrue(text.contains("BATCH-QA-PDF"));
        assertTrue(text.contains("已填表单正文"));
        assertTrue(text.contains("第一工序"));
        assertTrue(text.contains("工序一记录表"));
        assertTrue(text.contains("BRE-8301"));
        assertTrue(text.contains("产品信息"));
        assertTrue(text.contains("首件确认"));
        assertTrue(text.contains("37.5 kg"));
        assertTrue(text.contains("☑"));
        assertTrue(text.contains("签名人"));
        assertTrue(text.contains("2026-06-15 09:45"));
        assertTrue(text.contains("必需附件，至少 1 个，最多 3 个，组"));
        assertTrue(text.contains("photo-1"));
        assertTrue(text.contains("备注"));
        assertTrue(text.contains("归档备注：首件确认完成"));
        assertTrue(text.contains("特殊节点附录"));
        assertTrue(text.contains("来料检报告"));
        assertTrue(text.contains("跳过原因"));
        assertTrue(text.contains("批次演练跳过无模板特殊节点"));
        assertTrue(text.contains("操作人"));
        assertTrue(text.contains("操作时间"));
        assertTrue(text.contains("附件清单"));
        assertTrue(text.contains("qa-readable-photo.jpg"));
        assertTrue(text.contains("卷宗项"));
        assertTrue(text.contains("OQC-FINAL-" + batch.getId()));
        assertTrue(text.contains("返工/驳回/变更事件"));
        assertTrue(text.contains("EDHR-REWORK-QA-PDF"));
        assertTrue(text.contains("归档证据附录"));
        assertTrue(text.contains("审计追踪"));
        assertTrue(text.contains("归档哈希"));
    }

    @Test
    void downloadArchive_legacyManifestFailsFast() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-LEGACY-ARCHIVE")
                .setRouteId(fixture.routeId()));
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                .setAggregateHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
                .setClosedAt(LocalDateTime.now()));
        MesProEdhrBatchExecutionArchiveDO legacyArchive = new MesProEdhrBatchExecutionArchiveDO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setArchiveVersion(1)
                .setArchiveStatus("SEALED")
                .setFileName("legacy-manifest.pdf")
                .setContentType("application/pdf")
                .setFileSize(128L)
                .setContentHash("legacy-content-hash")
                .setSourceManifestJson("{\"batchExecutionId\":" + batch.getId() + ",\"batchCode\":\""
                        + batch.getBatchCode() + "\",\"tasks\":[]}")
                .setGeneratedBy(1L)
                .setGeneratedAt(LocalDateTime.now());
        batchArchiveMapper.insert(legacyArchive);

        assertServiceException(() -> batchExecutionService.downloadArchive(legacyArchive.getId()),
                PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED);
    }

    @Test
    void qualityReject_wrongSignaturePassword_rejectsWithoutSignatureOrStatusChange() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-QUALITY-WRONG-PASSWORD")
                .setRouteId(fixture.routeId()));
        MesProEdhrBatchExecutionDO before = batchExecutionMapper.selectById(batch.getId());
        doThrow(new ServiceException(USER_PASSWORD_FAILED))
                .when(adminUserApi).validatePassword(188L, "wrong-pass");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.qualityReject(new EdhrBatchExecutionQualityRejectReqVO()
                            .setId(batch.getId())
                            .setReason("错误密码不得质量拒收")
                            .setPassword("wrong-pass")));
            assertEquals(USER_PASSWORD_FAILED.getCode(), exception.getCode());
        }

        MesProEdhrBatchExecutionDO after = batchExecutionMapper.selectById(batch.getId());
        assertEquals(before.getStatus(), after.getStatus());
        assertNull(after.getRejectedAt());
        assertNull(after.getRejectSignatureId());
        assertTrue(batchSignatureMapper.selectListByBatchExecutionId(batch.getId()).isEmpty());
    }

    @Test
    void generateArchive_manifestIncludesAttachmentRuleSummariesFromExecutionSnapshot() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ATTACH-RULE-MANIFEST")
                .setRouteId(fixture.routeId()));
        Long taskId = routeTask(batch, 0).getId();
        bindApprovedExecutionWithSnapshot(taskId, 8202L, """
                {"fields":[{"fieldPath":"rows[2].cells[3].photo","fieldKey":"photo","label":"现场照片","attachmentRule":{"required":true,"minCount":1,"maxCount":3,"attachmentType":"IMAGE","groupKey":"photo-1"}}]}
                """);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .setClosedAt(LocalDateTime.now()));
        when(workTaskService.validateArchiveTask(9931L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9931L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));

        EdhrBatchExecutionArchiveRespVO archive = batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9931L));

        assertNotNull(archive.getSourceManifestJson());
        assertTrue(archive.getSourceManifestJson().contains("attachmentRuleSummaries"));
        assertTrue(archive.getSourceManifestJson().contains("\"executionId\":8202"));
        assertTrue(archive.getSourceManifestJson().contains("\"batchTaskId\":" + taskId));
        String manifest = batchArchiveMapper.selectById(archive.getId()).getSourceManifestJson();
        assertTrue(manifest.contains("attachmentRuleSummaries"));
        assertTrue(manifest.contains("\"executionId\":8202"));
        assertTrue(manifest.contains("\"batchTaskId\":" + taskId));
        assertTrue(manifest.contains("\"fieldPath\":\"rows[2].cells[3].photo\""));
        assertTrue(manifest.contains("\"fieldKey\":\"photo\""));
        assertTrue(manifest.contains("\"label\":\"现场照片\""));
        assertTrue(manifest.contains("\"required\":true"));
        assertTrue(manifest.contains("\"minCount\":1"));
        assertTrue(manifest.contains("\"maxCount\":3"));
        assertTrue(manifest.contains("\"attachmentType\":\"IMAGE\""));
        assertTrue(manifest.contains("\"groupKey\":\"photo-1\""));
    }

    @Test
    void getReviewTimeline_returnsBatchTasksSignaturesAndArchives() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-REVIEW")
                .setRouteId(fixture.routeId()));
        when(jimuReportGateway.getReportJson(fixture.reportId1())).thenReturn("""
                {"rows":{"0":{"cells":{"0":{"edhrSignature":{"enabled":true,"actionType":"APPROVE","signatureCellKey":"R0C0","reviewSourceType":"POST","reviewSourceId":7001,"reviewSourceName":"QA"}}}}}}
                """);
        bindApprovedExecution(routeTask(batch, 0).getId(), 8001L, true, true, true);
        skipAllSpecialNodes(batch);
        completeFinalInspectionDossier(batch.getId());
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setAggregateHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .setClosedAt(LocalDateTime.now()));
        batchSignatureMapper.insert(new MesProEdhrBatchExecutionSignatureDO()
                .setBatchExecutionId(batch.getId())
                .setActorId(1L)
                .setActorName("关闭人")
                .setActionType("BATCH_CLOSE")
                .setSignatureMode("PASSWORD")
                .setPasswordVerified(true)
                .setComment("close")
                .setAggregateHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .setSignedAt(LocalDateTime.of(2026, 6, 15, 16, 0))
                .setSelectedSignedAt(LocalDateTime.of(2026, 6, 15, 15, 45))
                .setSignatureDisplayAt(LocalDateTime.of(2026, 6, 15, 15, 45))
                .setSignatureTimeMode("USER_SELECTED")
                .setSelectedTimeZone("Asia/Shanghai")
                .setSelectedTimeReason("关闭批次按纸面完成时间显示")
                .setSelectedTimePolicyVersion("EDHR_SIGNATURE_TIME_V1")
                .setSelectedTimeAuditHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"));
        when(workTaskService.validateArchiveTask(9910L, batch.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(9910L)
                        .setBatchExecutionId(batch.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE));
        batchExecutionService.generateArchive(new EdhrBatchExecutionArchiveGenerateReqVO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType("BATCH_FINAL_PDF")
                .setWorkTaskId(9910L));

        EdhrBatchExecutionReviewTimelineRespVO timeline = batchExecutionService.getReviewTimeline(batch.getId());

        assertEquals(batch.getId(), timeline.getBatchExecutionId());
        assertEquals(1, timeline.getBatchEvents().size());
        assertEquals(6, timeline.getTaskEvents().size());
        assertFalse(timeline.getSignatureRecords().isEmpty());
        EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord closeSignature =
                timeline.getSignatureRecords().stream()
                        .filter(item -> "BATCH_CLOSE".equals(item.getActionType()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(LocalDateTime.of(2026, 6, 15, 15, 45), closeSignature.getSelectedSignedAt());
        assertEquals(LocalDateTime.of(2026, 6, 15, 15, 45), closeSignature.getSignatureDisplayAt());
        assertEquals("USER_SELECTED", closeSignature.getSignatureTimeMode());
        assertEquals("Asia/Shanghai", closeSignature.getSelectedTimeZone());
        assertEquals("关闭批次按纸面完成时间显示", closeSignature.getSelectedTimeReason());
        assertEquals("EDHR_SIGNATURE_TIME_V1", closeSignature.getSelectedTimePolicyVersion());
        assertEquals("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                closeSignature.getSelectedTimeAuditHash());
        assertEquals(1, timeline.getApprovalRecords().size());
        assertEquals(1, timeline.getExecutionReviews().size());
        assertEquals("BRE-8001", timeline.getExecutionReviews().get(0).getExecutionCode());
        assertEquals("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"产品信息\"}}}}}",
                timeline.getExecutionReviews().get(0).getFormViewModel().getSheetLayoutJson());
        assertEquals("{\"tableTitle\":\"产品信息\"}",
                timeline.getExecutionReviews().get(0).getFormViewModel().getMetaJson());
        assertEquals("{}", timeline.getExecutionReviews().get(0).getFormViewModel().getExecutionSnapshotJson());
        assertEquals(1, timeline.getArchiveVersions().size());
        assertEquals(1, timeline.getDossierItems().size());
        assertEquals("FINAL_INSPECTION", timeline.getDossierItems().get(0).getItemType());
        assertEquals("OQC", timeline.getDossierItems().get(0).getSourceDocType());
        assertEquals("PASS", timeline.getDossierItems().get(0).getSourceDocResult());
    }

    @Test
    void getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-REVIEW-MISSING-CONFIG")
                .setRouteId(fixture.routeId()));
        bindApprovedExecution(routeTask(batch, 0).getId(), 8051L, true, true, true);
        skipAllSpecialNodes(batch);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                .setRouteSnapshotJson(incompleteFrozenBatchTaskConfigSnapshotJson()));
        routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(fixture.routeId(), "BATCH")
                .forEach(record -> routeFlowProcessBatchRecordMapper.deleteById(record.getId()));
        routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(fixture.routeId(), "BATCH")
                .forEach(config -> routeFlowProcessConfigMapper.deleteById(config.getId()));
        MesProRouteFlowConfigDO liveFlowConfig =
                routeFlowConfigMapper.selectByRouteIdAndUseType(fixture.routeId(), "BATCH");
        if (liveFlowConfig != null) {
            routeFlowConfigMapper.deleteById(liveFlowConfig.getId());
        }

        EdhrBatchExecutionReviewTimelineRespVO timeline = batchExecutionService.getReviewTimeline(batch.getId());

        assertEquals(batch.getId(), timeline.getBatchExecutionId());
        assertEquals(1, timeline.getBatchEvents().size());
        assertFalse(timeline.getTaskEvents().isEmpty());
        assertTrue(timeline.getTaskEvents().stream()
                .allMatch(event -> Boolean.FALSE.equals(event.getAvailable())));
        assertTrue(timeline.getTaskEvents().stream()
                .allMatch(event -> "历史批次只读".equals(event.getGateMessage())));
        assertEquals(1, timeline.getExecutionReviews().size());
        assertEquals("BRE-8051", timeline.getExecutionReviews().get(0).getExecutionCode());
        assertEquals("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"产品信息\"}}}}}",
                timeline.getExecutionReviews().get(0).getFormViewModel().getSheetLayoutJson());
        assertFalse(timeline.getDossierItems().isEmpty());
    }

    @Test
    void getReviewTimeline_includesExecutionAttachmentSummaries() {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ATTACH-HISTORY")
                .setRouteId(fixture.routeId()));
        when(jimuReportGateway.getReportJson(fixture.reportId1())).thenReturn("""
                {"rows":{"0":{"cells":{"0":{"edhrSignature":{"enabled":true,"actionType":"APPROVE","signatureCellKey":"R0C0","reviewSourceType":"POST","reviewSourceId":7001,"reviewSourceName":"QA"}}}}}}
                """);
        Long taskId = routeTask(batch, 0).getId();
        bindApprovedExecution(taskId, 8101L, true, true, true);
        attachmentMapper.insert(new MesProBatchRecordExecutionAttachmentDO()
                .setExecutionId(8101L)
                .setBatchExecutionId(batch.getId())
                .setBatchTaskId(taskId)
                .setWorkTaskId(9101L)
                .setRowIndex(2)
                .setColumnIndex(3)
                .setFieldKey("photo")
                .setFieldPath("rows[2].cells[3]")
                .setFieldLabel("现场照片")
                .setAttachmentType("IMAGE")
                .setAttachmentGroupKey("photo-1")
                .setAttachmentAction("ADD")
                .setVersionNo(1)
                .setFileId(9201L)
                .setFileUrl("http://127.0.0.1:9000/yudao/edhr/photo.jpg")
                .setStorageConfigId(28L)
                .setStoragePath("edhr/photo.jpg")
                .setFileName("photo.jpg")
                .setContentType("image/jpeg")
                .setFileSize(128L)
                .setSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setStorageRetentionHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setAuditBatchId(9301L)
                .setSignatureId(9401L)
                .setPreviousAttachmentHash("0000000000000000000000000000000000000000000000000000000000000000")
                .setAttachmentHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setOperatorId(9501L)
                .setOperatorName("操作员")
                .setOperatedAt(LocalDateTime.now())
                .setReasonCategory("FIELD_ATTACHMENT")
                .setReasonText("历史附件摘要测试"));

        EdhrBatchExecutionReviewTimelineRespVO timeline = batchExecutionService.getReviewTimeline(batch.getId());

        EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview review = timeline.getExecutionReviews().get(0);
        assertEquals(1, review.getAttachmentCount());
        assertEquals(1, review.getAttachmentSummaries().size());
        EdhrBatchExecutionReviewTimelineRespVO.AttachmentSummary attachment = review.getAttachmentSummaries().get(0);
        assertEquals("photo", attachment.getFieldKey());
        assertEquals("现场照片", attachment.getFieldLabel());
        assertEquals("IMAGE", attachment.getAttachmentType());
        assertEquals("photo.jpg", attachment.getFileName());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", attachment.getSha256());
        assertEquals("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                attachment.getAttachmentHash());
    }

    @Test
    void create_requiresBatchCodeAndRouteProcesses() {
        itemMapper.insert(MesMdItemDO.builder()
                .id(1001L)
                .code("ITEM-EMPTY")
                .name("空路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .batchFlag(true)
                .build());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .code("WO-EMPTY")
                .name("工单")
                .productId(1001L)
                .batchCode("BATCH-EMPTY")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(false)
                .build();
        workOrderMapper.insert(workOrder);
        MesProRouteDO route = MesProRouteDO.builder().code("ROUTE-EMPTY").name("空路线").status(0).build();
        routeMapper.insert(route);
        routeVersionMapper.insert(MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson("{\"code\":\"ROUTE-EMPTY\",\"name\":\"空路线\"}")
                .build());
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(route.getId())
                .itemId(1001L)
                .quantity(1)
                .build());

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(workOrder.getId())
                .setBatchCode(" ")
                .setRouteId(route.getId())),
                BAD_REQUEST);

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(workOrder.getId())
                .setBatchCode("BATCH-EMPTY")
                .setRouteId(route.getId())),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void create_requiresAtLeastOneRouteProcessWithDefaultReport() {
        Fixture fixture = insertRouteFixture(false, false);

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-NO-REPORT")
                .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    @Test
    void create_missingBatchUseConfigMustExplainRouteFlowDefaultRecordConfiguration() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectListByRouteId(fixture.routeId()).get(0);
        String legacyRouteProcessReportId = insertReport("RPT-LEGACY-ROUTE-PROCESS", "旧路线工序默认批记录");
        routeProcessMapper.updateById(new MesProRouteProcessDO()
                .setId(routeProcess.getId())
                .setBatchRecordReportId(legacyRouteProcessReportId));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-MISSING-BATCH-USE")
                        .setRouteId(fixture.routeId())));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("工艺流程批记录配置"));
        assertTrue(exception.getMessage().contains("默认批记录"));
    }

    @Test
    void openOrCreate_allowsPreparedUnfrozenWorkOrder() {
        Fixture fixture = insertRouteFixture(true, true);
        workOrderMapper.updateById(new MesProWorkOrderDO()
                .setId(fixture.workOrderId())
                .setStatus(MesProWorkOrderStatusEnum.PREPARE.getStatus()));

        EdhrBatchExecutionRespVO result = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-PREPARE")
                .setRouteId(fixture.routeId()));

        assertNotNull(result.getId());
    }

    @Test
    void openOrCreate_rejectsCanceledWorkOrder() {
        Fixture fixture = insertRouteFixture(true, true);
        workOrderMapper.updateById(new MesProWorkOrderDO()
                .setId(fixture.workOrderId())
                .setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus()));

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-CANCELED")
                .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID);
    }

    @Test
    void openOrCreate_rejectsTemporaryFrozenWorkOrder() {
        Fixture fixture = insertRouteFixture(true, true);
        workOrderMapper.updateById(new MesProWorkOrderDO()
                .setId(fixture.workOrderId())
                .setTemporaryFrozen(true));

        assertServiceException(() -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-FROZEN")
                .setRouteId(fixture.routeId())),
                PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID);
    }

    @Test
    void openOrCreate_doesNotUseProductionTaskRouteWhenResolvingBatchRecordRoute() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(fixture.routeId())
                .itemId(workOrder.getProductId())
                .quantity(1)
                .build());
        MesProRouteDO taskRoute = MesProRouteDO.builder()
                .code("ROUTE-TASK-" + randomLongId())
                .name("生产任务路线")
                .status(0)
                .build();
        routeMapper.insert(taskRoute);
        insertProductionTask(fixture.workOrderId(), taskRoute.getId(), randomLongId(), randomLongId());

        EdhrBatchExecutionRespVO result = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-AUTO-ROUTE"));

        assertNotNull(result.getId());
        assertEquals(fixture.routeId(), result.getRouteId());
        assertFalse(routeTasks(result).isEmpty());
    }

    @Test
    void openOrCreate_failsWhenMissingRouteIdAndWorkOrderHasNoTaskRoute() {
        Fixture fixture = insertRouteFixture(true, true);
        routeProductMapper.deleteByRouteId(fixture.routeId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-NO-AUTO-ROUTE")));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_BINDING_REQUIRED.getCode(), exception.getCode());
        assertEquals(PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_BINDING_REQUIRED.getMsg(), exception.getMessage());
    }

    @Test
    void openOrCreate_failsWhenProductHasMultipleEnabledBoundRoutes() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        route.setName("球囊扩张压力泵方案");
        routeMapper.updateById(route);
        MesProRouteDO anotherRoute = insertExecutableRoute("球囊扩张导管方案", "RPT-MULTI");
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(anotherRoute.getId())
                .itemId(fixture.productId())
                .quantity(1)
                .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                        .setWorkOrderId(fixture.workOrderId())
                        .setBatchCode("BATCH-MULTI-AUTO-ROUTE")));

        assertEquals(PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_DUPLICATE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains(route.getCode()));
        assertTrue(exception.getMessage().contains(anotherRoute.getCode()));
    }

    @Test
    void workbench_prefersReleaseStageWhenReleaseTransactionExists() {
        Fixture fixture = insertRouteFixture(true, true);
        insertInitialFillAssignmentRule(fixture.routeId());
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-WORKBENCH-STAGE")
                .setRouteId(fixture.routeId()));
        releaseTransactionMapper.insert(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batch.getId())
                .setReleaseCode("REL-" + randomLongId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL)
                .setBlockingCheckCount(0)
                .setFailedCheckCount(0)
                .setRequiredCheckCount(6));

        EdhrBatchWorkbenchRespVO workbench = batchWorkbenchService.getWorkbench(batch.getId());

        assertEquals("IN_RELEASE", workbench.getMainStage());
        assertEquals("放行审批中", workbench.getMainStageLabel());
        assertEquals("QA/放行员", workbench.getStageOwnerRole());
        assertEquals("待审批", workbench.getReleaseSummary().getReleaseStatusLabel());
    }

    @Test
    void workbench_resolvesReleaseOwnerLabelFromRouteReleaseUserRule() {
        Fixture fixture = insertRouteFixture(true, true);
        insertInitialFillAssignmentRule(fixture.routeId());
        insertRouteReleaseAssignmentRule(fixture.routeId(), "USER", 10002L);
        when(adminUserApi.getUser(10002L)).thenReturn(user(10002L, "王放行"));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-WORKBENCH-RELEASE-USER")
                .setRouteId(fixture.routeId()));

        EdhrBatchWorkbenchRespVO workbench = batchWorkbenchService.getWorkbench(batch.getId());

        assertTrue(workbench.getReleaseSummary().getReleaseOwnerConfigured());
        assertEquals("USER", workbench.getReleaseSummary().getReleaseOwnerSourceType());
        assertEquals("王放行", workbench.getReleaseSummary().getReleaseOwnerLabel());
    }

    @Test
    void workbench_resolvesReleaseOwnerLabelFromRouteReleaseRoleGroupRule() {
        Fixture fixture = insertRouteFixture(true, true);
        insertInitialFillAssignmentRule(fixture.routeId());
        insertRouteReleaseAssignmentRule(fixture.routeId(), "ROLE_GROUP", 8801L);
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(8801L))).thenReturn(Set.of(10003L, 10004L));
        when(adminUserApi.getUserList(Set.of(10003L, 10004L)))
                .thenReturn(List.of(user(10003L, "放行一"), user(10004L, "放行二")));
        when(roleApi.getRoleList(Set.of(8801L))).thenReturn(List.of(role(8801L, "质量放行组", "qa_release")));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-WORKBENCH-RELEASE-ROLE")
                .setRouteId(fixture.routeId()));

        EdhrBatchWorkbenchRespVO workbench = batchWorkbenchService.getWorkbench(batch.getId());

        assertTrue(workbench.getReleaseSummary().getReleaseOwnerConfigured());
        assertEquals("ROLE_GROUP", workbench.getReleaseSummary().getReleaseOwnerSourceType());
        assertEquals("质量放行组（角色成员均可放行）", workbench.getReleaseSummary().getReleaseOwnerLabel());
    }

    @Test
    void workbench_marksReleaseOwnerMissingWhenRouteReleaseRuleAbsent() {
        Fixture fixture = insertRouteFixture(true, true);
        insertInitialFillAssignmentRule(fixture.routeId());
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-WORKBENCH-RELEASE-MISSING")
                .setRouteId(fixture.routeId()));

        EdhrBatchWorkbenchRespVO workbench = batchWorkbenchService.getWorkbench(batch.getId());

        assertFalse(workbench.getReleaseSummary().getReleaseOwnerConfigured());
        assertEquals("放行责任人未配置", workbench.getReleaseSummary().getReleaseOwnerLabel());
    }

    @Test
    void detailTask_includesFillableUsersFromActiveFillWorkTask() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .build());
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-FILLABLE-USERS")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO task = routeTask(batch, 0);
        workTaskMapper.insert(MesProEdhrWorkTaskDO.builder()
                .taskCode("WT-FILLABLE")
                .taskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .batchExecutionId(batch.getId())
                .batchTaskId(task.getId())
                .businessScopeType("BATCH_TASK")
                .businessScopeId(task.getId())
                .workOrderId(fixture.workOrderId())
                .workOrderCode("WO-FILLABLE")
                .batchCode("BATCH-FILLABLE-USERS")
                .routeId(fixture.routeId())
                .routeProcessId(task.getRouteProcessId())
                .processId(task.getProcessId())
                .processName(task.getProcessName())
                .assigneeUserId(113L)
                .candidateSourceType("ROLE_GROUP")
                .candidateSourceId(9001L)
                .candidateUserSnapshot("113,910245")
                .status(MesProEdhrWorkTaskStatus.TODO)
                .actionUrl("/mes/pro/feedback/edhr-batch-execution/detail?id=" + batch.getId())
                .remark("eDHR填写任务")
                .build());
        when(adminUserApi.getUserMap(argThat(ids ->
                ids != null && ids.containsAll(List.of(113L, 910245L))))).thenReturn(Map.of(
                113L, user(113L, "奥特曼"),
                910245L, user(910245L, "复核员")));

        EdhrBatchExecutionRespVO result = batchExecutionService.get(batch.getId());

        EdhrBatchExecutionTaskRespVO resolvedTask = routeTask(result, 0);
        assertEquals(List.of("奥特曼", "复核员"), resolvedTask.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getDisplayName)
                .toList());
        assertEquals(List.of(113L, 910245L), resolvedTask.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getUserId)
                .toList());
    }

    @Test
    void detailTask_includesFillableUsersFromMultipleAssistRowRulesWhenWorkTaskNotCreated() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .build());
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-MULTI-ASSIST-FILLERS")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO task = routeTask(batch, 0);
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID)
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordVersionId(task.getBatchRecordVersionId())
                .setRuleType("FILL")
                .setScopeKey("AR_001")
                .setSignatureCellKey("")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds("113")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(Boolean.TRUE)
                .setFillableScopeJson("""
                        {"schemaVersion":2,"scopeKey":"AR_001","cells":[{"sourceTableIndex":0,"rowIndex":1,"columnIndex":1}]}
                        """.trim()));
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID)
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordVersionId(task.getBatchRecordVersionId())
                .setRuleType("FILL")
                .setScopeKey("AR_002")
                .setSignatureCellKey("")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds("910245")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(Boolean.TRUE)
                .setFillableScopeJson("""
                        {"schemaVersion":2,"scopeKey":"AR_002","cells":[{"sourceTableIndex":0,"rowIndex":2,"columnIndex":1}]}
                        """.trim()));
        when(adminUserApi.getUserList(List.of(113L))).thenReturn(List.of(user(113L, "员工甲")));
        when(adminUserApi.getUserList(List.of(910245L))).thenReturn(List.of(user(910245L, "员工乙")));
        when(adminUserApi.getUserMap(argThat(ids ->
                ids != null && ids.containsAll(List.of(113L, 910245L))))).thenReturn(Map.of(
                113L, user(113L, "员工甲"),
                910245L, user(910245L, "员工乙")));

        EdhrBatchExecutionRespVO result = batchExecutionService.get(batch.getId());

        EdhrBatchExecutionTaskRespVO resolvedTask = routeTask(result, 0);
        assertEquals(List.of(113L, 910245L), resolvedTask.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getUserId)
                .toList());
        assertEquals(List.of("员工甲", "员工乙"), resolvedTask.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getDisplayName)
                .toList());
    }

    @Test
    void detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .build());
        insertInitialFillAssignmentRule(fixture.routeId());
        when(adminUserApi.getUserMap(argThat(ids ->
                ids != null && ids.contains(10001L)))).thenReturn(Map.of(
                10001L, user(10001L, "首工序填写员")));

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-FILLABLE-RULE")
                .setRouteId(fixture.routeId()));

        assertEquals(List.of("首工序填写员"), routeTask(batch, 0).getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getDisplayName)
                .toList());
        assertTrue(routeTask(batch, 1).getFillableUsers().isEmpty());
    }

    @Test
    void detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        String mainReport = insertReport("RPT-FORM-FILLER-MAIN", "主生产批记录");
        String lossReport = insertReport("RPT-FORM-FILLER-LOSS", "损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5011L, "PRODUCTION", 2)
        ));
        MesProRouteFlowProcessBatchRecordDO lossBinding = routeFlowProcessBatchRecordMapper
                .selectListByRouteProcessIdsAndUseType(List.of(firstProcess.getId()), "BATCH").stream()
                .filter(record -> lossReport.equals(record.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        routeFlowProcessBatchRecordMapper.updateById(MesProRouteFlowProcessBatchRecordDO.builder()
                .id(lossBinding.getId())
                .candidateSourceType("USERS")
                .candidateSourceIds("152")
                .candidateSourceNames("[\"张可莹（zhangkeying）\"]")
                .build());
        String lossBindingKey = lossBinding.getFormBindingKey();
        insertCurrentProcessFillRule(firstProcess.getId(), lossBindingKey, "FILL", "USERS", "152");
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcesses))
                .build());
        when(adminUserApi.getUserList(List.of(152L))).thenReturn(List.of(
                user(152L, "张可莹（zhangkeying）")));
        when(adminUserApi.getUserMap(argThat(ids ->
                ids != null && ids.contains(152L)))).thenReturn(Map.of(
                152L, user(152L, "张可莹（zhangkeying）")));

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-ROUTE-FORM-FILLER")
                .setRouteId(fixture.routeId()));

        EdhrBatchExecutionTaskRespVO lossTask = routeTasks(batch).stream()
                .filter(task -> lossBindingKey.equals(task.getFormBindingKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(152L), lossTask.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getUserId)
                .toList());
        assertEquals(List.of("张可莹（zhangkeying）"), lossTask.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getDisplayName)
                .toList());
    }

    @Test
    void detailTask_triggersCompanionFillTaskBackfillWhenActiveMainTaskExists() {
        Fixture fixture = insertRouteFixture(false, false);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(fixture.routeId()).stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        MesProRouteProcessDO firstProcess = routeProcesses.get(0);
        String mainReport = insertReport("RPT-COMPANION-BACKFILL-MAIN", "粗洗工序生产记录");
        String lossReport = insertReport("RPT-COMPANION-BACKFILL-LOSS", "损耗单");
        insertBatchUseConfigWithSlots(fixture.routeId(), firstProcess.getId(), "SEQUENTIAL", List.of(
                batchSlot(mainReport, "MAIN", null, null, null, 1),
                batchSlot(lossReport, "LOSS_REPORT", "INTERNAL_RECORD", 5011L, "PRODUCTION", 2)
        ));
        MesProRouteFlowProcessBatchRecordDO lossBinding = routeFlowProcessBatchRecordMapper
                .selectListByRouteProcessIdsAndUseType(List.of(firstProcess.getId()), "BATCH").stream()
                .filter(record -> lossReport.equals(record.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        insertCurrentProcessFillRule(firstProcess.getId(), lossBinding.getFormBindingKey(), "FILL", "USERS", "152");
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(fixture.routeVersionId())
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcesses))
                .build());
        when(adminUserApi.getUserList(List.of(152L))).thenReturn(List.of(user(152L, "张可莹")));
        when(adminUserApi.getUserMap(argThat(ids -> ids != null && ids.contains(152L)))).thenReturn(Map.of(
                152L, user(152L, "张可莹"),
                10001L, user(10001L, "主表填写人")));
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-COMPANION-BACKFILL")
                .setRouteId(fixture.routeId()));
        EdhrBatchExecutionTaskRespVO mainTask = routeTasks(batch).stream()
                .filter(task -> mainReport.equals(task.getBatchRecordReportId()))
                .findFirst()
                .orElseThrow();
        insertWorkTask(batch, mainTask, fixture, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
        clearInvocations(workTaskService);

        batchExecutionService.get(batch.getId());

        verify(workTaskService).createInitialFillTask(argThat(latest -> Objects.equals(latest.getId(), batch.getId())));
    }

    @Test
    void detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes() {
        Fixture fixture = insertRouteFixture(true, true);
        MesProRouteDO route = routeMapper.selectById(fixture.routeId());
        List<Map<String, Object>> owners = List.of(
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                        "USERS", List.of(201L, 202L)),
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                        "ROLE", List.of(991L)),
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                        "USERS", List.of(301L, 302L)),
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                        "USERS", List.of(401L, 402L)));
        configureRouteVersionSpecialAttachmentOwners(fixture.routeVersionId(), route, owners);
        when(permissionApi.getUserRoleIdListByRoleIds(argThat(ids -> ids != null && ids.contains(991L))))
                .thenReturn(Set.of(211L, 212L));
        when(adminUserApi.getUserMap(argThat(ids -> ids != null
                && ids.containsAll(List.of(201L, 202L, 211L, 212L, 301L, 302L, 401L, 402L)))))
                .thenReturn(Map.of(
                        201L, user(201L, "来料甲"),
                        202L, user(202L, "来料乙"),
                        211L, user(211L, "灭菌甲"),
                        212L, user(212L, "灭菌乙"),
                        301L, user(301L, "成检报告甲"),
                        302L, user(302L, "成检报告乙"),
                        401L, user(401L, "成检记录甲"),
                        402L, user(402L, "成检记录乙")));

        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode("BATCH-SPECIAL-FILLABLE-USERS")
                .setRouteId(fixture.routeId()));

        assertSpecialNodeFillableUsers(batch, MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                List.of(201L, 202L), List.of("来料甲", "来料乙"));
        assertSpecialNodeFillableUsers(batch, MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                List.of(211L, 212L), List.of("灭菌甲", "灭菌乙"));
        assertSpecialNodeFillableUsers(batch,
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                List.of(301L, 302L), List.of("成检报告甲", "成检报告乙"));
        assertSpecialNodeFillableUsers(batch,
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                List.of(401L, 402L), List.of("成检记录甲", "成检记录乙"));
    }

    private void assertSpecialNodeFillableUsers(EdhrBatchExecutionRespVO batch, String nodeType,
                                                List<Long> expectedUserIds, List<String> expectedDisplayNames) {
        EdhrBatchExecutionTaskRespVO task = batch.getTasks().stream()
                .filter(item -> nodeType.equals(item.getNodeType()))
                .findFirst()
                .orElseThrow();
        assertEquals(expectedUserIds, task.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getUserId)
                .toList());
        assertEquals(expectedDisplayNames, task.getFillableUsers().stream()
                .map(EdhrBatchExecutionTaskRespVO.FillableUser::getDisplayName)
                .toList());
    }

    private List<EdhrBatchExecutionTaskRespVO> routeTasks(EdhrBatchExecutionRespVO batch) {
        return batch.getTasks().stream()
                .filter(task -> MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType())
                        || task.getBatchRecordReportId() != null)
                .toList();
    }

    private AdminUserRespDTO user(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        user.setStatus(0);
        return user;
    }

    private RoleRespDTO role(Long id, String name, String code) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(id);
        role.setName(name);
        role.setCode(code);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return role;
    }

    private void assertCloseBlocked(Executable executable) {
        ServiceException exception = assertThrows(ServiceException.class, executable);
        assertEquals(PRO_EDHR_BATCH_EXECUTION_CLOSE_BLOCKED.getCode(), exception.getCode());
    }

    private EdhrBatchExecutionTaskRespVO routeTask(EdhrBatchExecutionRespVO batch, int index) {
        return routeTasks(batch).get(index);
    }

    private void configureRouteVersionSpecialAttachmentOwners(Long routeVersionId, MesProRouteDO route,
                                                              List<Map<String, Object>> owners) {
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeSnapshotJson(frozenRouteSnapshotJson(route,
                        routeProcessMapper.selectListByRouteId(route.getId()), owners))
                .build());
    }

    private void configureBatchSpecialAttachmentOwners(Long batchExecutionId, Long... userIds) {
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        MesProRouteDO route = routeMapper.selectById(batch.getRouteId());
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batchExecutionId)
                .setRouteSnapshotJson(frozenRouteSnapshotJson(route,
                        routeProcessMapper.selectListByRouteId(route.getId()),
                        batchRecordAttachmentOwners("USERS", List.of(userIds)))));
    }

    private List<Map<String, Object>> defaultBatchRecordAttachmentOwners() {
        return batchRecordAttachmentOwners("USERS", List.of(10001L));
    }

    private String incompleteFrozenBatchTaskConfigSnapshotJson() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<String, Object> configSnapshots = new LinkedHashMap<>();
        configSnapshots.put("flowGraph", Map.of("nodes", List.of(), "edges", List.of()));
        configSnapshots.put("batchUseConfigs", List.of());
        configSnapshots.put("batchRecordAttachmentOwners", defaultBatchRecordAttachmentOwners());
        snapshot.put("configSnapshots", configSnapshots);
        return JSON.toJSONString(snapshot);
    }

    private List<Map<String, Object>> batchRecordAttachmentOwners(String sourceType, List<Long> sourceIds) {
        return List.of(
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                        sourceType, sourceIds),
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                        sourceType, sourceIds),
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                        sourceType, sourceIds),
                batchRecordAttachmentOwner(
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                        sourceType, sourceIds));
    }

    private Map<String, Object> batchRecordAttachmentOwner(String attachmentCode, String sourceType,
                                                           List<Long> sourceIds) {
        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("attachmentCode", attachmentCode);
        owner.put("attachmentName", switch (attachmentCode) {
            case MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT -> "来料检报告";
            case MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT -> "灭菌报告";
            case MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT -> "成品检报告";
            case MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD -> "成品检记录";
            default -> attachmentCode;
        });
        owner.put("candidateSourceType", sourceType);
        owner.put("candidateSourceIds", sourceIds);
        owner.put("candidateSourceNames", sourceIds.stream().map(String::valueOf).toList());
        return owner;
    }

    private String frozenRouteSnapshotJson(MesProRouteDO route, List<MesProRouteProcessDO> routeProcesses) {
        return frozenRouteSnapshotJson(route, routeProcesses, defaultBatchRecordAttachmentOwners());
    }

    @SuppressWarnings("unchecked")
    private String frozenRouteSnapshotJsonWithoutBatchRecordAttachmentOwners(MesProRouteDO route,
                                                                            List<MesProRouteProcessDO> routeProcesses) {
        Map<String, Object> snapshot = JSON.parseObject(frozenRouteSnapshotJson(route, routeProcesses), Map.class);
        ((Map<String, Object>) snapshot.get("configSnapshots")).remove("batchRecordAttachmentOwners");
        return JSON.toJSONString(snapshot);
    }

    private String frozenRouteSnapshotJson(MesProRouteDO route, List<MesProRouteProcessDO> routeProcesses,
                                           List<Map<String, Object>> batchRecordAttachmentOwners) {
        List<MesProRouteProcessDO> orderedRouteProcesses = routeProcesses.stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort))
                .toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("routeId", route.getId());
        snapshot.put("routeCode", route.getCode());
        snapshot.put("routeName", route.getName());
        snapshot.put("status", route.getStatus());

        Map<String, Object> configSnapshots = new LinkedHashMap<>();
        Map<String, Object> flowGraph = new LinkedHashMap<>();
        flowGraph.put("graphVersion", 1L);
        flowGraph.put("nodes", orderedRouteProcesses.stream()
                .map(routeProcess -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("routeProcessId", routeProcess.getId());
                    node.put("processId", routeProcess.getProcessId());
                    node.put("sort", routeProcess.getSort());
                    node.put("keyFlag", routeProcess.getKeyFlag());
                    node.put("checkFlag", routeProcess.getCheckFlag());
                    return node;
                })
                .toList());
        flowGraph.put("edges", routeProcessFlowEdgeMapper.selectListByRouteId(route.getId()).stream()
                .map(edge -> {
                    Map<String, Object> edgeSnapshot = new LinkedHashMap<>();
                    edgeSnapshot.put("sourceRouteProcessId", edge.getSourceRouteProcessId());
                    edgeSnapshot.put("targetRouteProcessId", edge.getTargetRouteProcessId());
                    edgeSnapshot.put("relationType", edge.getRelationType());
                    edgeSnapshot.put("sort", edge.getSort());
                    return edgeSnapshot;
                })
                .toList());
        configSnapshots.put("flowGraph", flowGraph);
        configSnapshots.put("products", List.of());
        configSnapshots.put("scheduleConfigs", List.of());
        configSnapshots.put("scheduleUseConfigs", List.of());
        configSnapshots.put("batchUseConfigs", frozenBatchUseConfigs(route, orderedRouteProcesses));
        configSnapshots.put("batchRecordAttachmentOwners", batchRecordAttachmentOwners);
        snapshot.put("configSnapshots", configSnapshots);
        return JSON.toJSONString(snapshot);
    }

    private List<Map<String, Object>> frozenBatchUseConfigs(MesProRouteDO route,
                                                            List<MesProRouteProcessDO> orderedRouteProcesses) {
        Map<Long, MesProRouteFlowProcessConfigDO> processConfigMap = routeFlowProcessConfigMapper
                .selectListByRouteIdAndUseType(route.getId(), "BATCH").stream()
                .collect(java.util.stream.Collectors.toMap(MesProRouteFlowProcessConfigDO::getRouteProcessId,
                        item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> recordMap = routeFlowProcessBatchRecordMapper
                .selectListByRouteProcessIdsAndUseType(orderedRouteProcesses.stream()
                        .map(MesProRouteProcessDO::getId)
                        .toList(), "BATCH").stream()
                .collect(java.util.stream.Collectors.groupingBy(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MesProRouteProcessDO routeProcess : orderedRouteProcesses) {
            MesProRouteFlowProcessConfigDO processConfig = processConfigMap.get(routeProcess.getId());
            if (processConfig == null) {
                continue;
            }
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("routeProcessId", routeProcess.getId());
            config.put("processId", routeProcess.getProcessId());
            config.put("sort", routeProcess.getSort());
            config.put("useType", "BATCH");
            config.put("enabled", processConfig.getEnabled());
            config.put("executionMode", processConfig.getExecutionMode());
            config.put("productionQuantityFactor", processConfig.getProductionQuantityFactor());
            config.put("remark", processConfig.getRemark());
            config.put("formBindings", recordMap.getOrDefault(routeProcess.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getReportSort))
                    .map(this::frozenBatchRecordReport)
                    .toList());
            result.add(config);
        }
        return result;
    }

    private Map<String, Object> frozenBatchRecordReport(MesProRouteFlowProcessBatchRecordDO record) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("routeBindingId", record.getId());
        report.put("batchRecordReportId", record.getBatchRecordReportId());
        report.put("batchRecordDefinitionId", record.getBatchRecordDefinitionId());
        report.put("batchRecordVersionId", record.getBatchRecordVersionId());
        report.put("formSlotType", record.getFormSlotType());
        report.put("formBindingKey", record.getFormBindingKey());
        report.put("formTemplateId", record.getFormTemplateId());
        report.put("formTemplateName", record.getFormTemplateNameSnapshot());
        report.put("lastPublishedTemplateVersionId", record.getLastPublishedTemplateVersionId());
        report.put("lastPublishedTemplateVersionNo", record.getLastPublishedTemplateVersionNo());
        report.put("instanceScope", record.getInstanceScope());
        report.put("sharedFormKey", record.getSharedFormKey());
        report.put("fillableScopeJson", record.getFillableScopeJson());
        report.put("recordCategory", record.getRecordCategory());
        report.put("validationProfile", record.getValidationProfile());
        report.put("recordbookEnabled", record.getRecordbookEnabled());
        report.put("permissionScopeId", record.getPermissionScopeId());
        report.put("recordCategorySnapshotHash", record.getRecordCategorySnapshotHash());
        report.put("requiredPolicy", record.getRequiredPolicy());
        report.put("requiredConditionJson", record.getRequiredConditionJson());
        report.put("ownerRoleKey", record.getOwnerRoleKey());
        report.put("archiveVisibility", record.getArchiveVisibility());
        report.put("slotConfigSnapshotHash", record.getSlotConfigSnapshotHash());
        report.put("candidateSourceType", record.getCandidateSourceType());
        report.put("candidateSourceIds", record.getCandidateSourceIds());
        report.put("candidateSourceNames", record.getCandidateSourceNames());
        report.put("reportSort", record.getReportSort());
        report.put("remark", record.getRemark());
        return report;
    }

    private void skipAllSpecialNodes(EdhrBatchExecutionRespVO batch) {
        Long ownerUserId = 0L;
        MesProEdhrBatchExecutionDO persistedBatch = batchExecutionMapper.selectById(batch.getId());
        insertCloseAssignmentRule(persistedBatch.getRouteId(), ownerUserId);
        configureBatchSpecialAttachmentOwners(batch.getId(), ownerUserId);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ownerUserId);
            batch.getTasks().stream()
                    .filter(task -> !MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType()))
                    .forEach(task -> batchExecutionService.skipSpecialNode(
                            task.getId(), "批次演练跳过无模板特殊节点", "secret", List.of()));
        }
    }

    private void insertLegacySpecialOnlyTasks(Long batchExecutionId) {
        insertLegacySpecialTask(batchExecutionId, MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                "来料检报告", 0);
        insertLegacySpecialTask(batchExecutionId, MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                "灭菌报告", 9000);
        insertLegacySpecialTask(batchExecutionId,
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                "成品检报告", 9010);
        insertLegacySpecialTask(batchExecutionId,
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                "成品检记录", 9020);
    }

    private void insertLegacySpecialTask(Long batchExecutionId, String nodeType, String nodeName, Integer sort) {
        batchTaskMapper.insert(MesProEdhrBatchExecutionTaskDO.builder()
                .batchExecutionId(batchExecutionId)
                .nodeType(nodeType)
                .routeProcessSort(sort)
                .processCode(nodeType)
                .processName(nodeName)
                .batchRecordSort(0)
                .executionMode("SEQUENTIAL")
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .requiredFlag(Boolean.TRUE)
                .build());
    }

    private Fixture insertRouteFixture(boolean firstReport, boolean secondReport) {
        Long productId = randomLongId();
        itemMapper.insert(MesMdItemDO.builder()
                .id(productId)
                .code("ITEM-" + productId)
                .name("eDHR 路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .batchFlag(true)
                .build());
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .code("WO-" + randomLongId())
                .name("eDHR 工单")
                .productId(productId)
                .batchCode("BATCH-SRC")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(false)
                .build();
        workOrderMapper.insert(workOrder);

        MesProRouteDO route = MesProRouteDO.builder()
                .code("ROUTE-" + randomLongId())
                .name("eDHR 路线")
                .status(0)
                .build();
        routeMapper.insert(route);
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson("{\"code\":\"" + route.getCode() + "\",\"name\":\"" + route.getName() + "\"}")
                .build();
        routeVersionMapper.insert(routeVersion);

        MesProProcessDO process1 = MesProProcessDO.builder().code("P-10").name("第一工序").status(0).build();
        MesProProcessDO process2 = MesProProcessDO.builder().code("P-20").name("第二工序").status(0).build();
        processMapper.insert(process1);
        processMapper.insert(process2);

        String reportId1 = firstReport ? insertReport("RPT-10-" + Math.abs(randomLongId()), "表1") : null;
        String reportId2 = secondReport ? insertReport("RPT-20-" + Math.abs(randomLongId()), "表2") : null;

        MesProRouteProcessDO routeProcess1 = MesProRouteProcessDO.builder()
                .routeId(route.getId())
                .processId(process1.getId())
                .sort(10)
                .keyFlag(true)
                .checkFlag(false)
                .batchRecordReportId(reportId1)
                .build();
        routeProcessMapper.insert(routeProcess1);
        MesProRouteProcessDO routeProcess2 = MesProRouteProcessDO.builder()
                .routeId(route.getId())
                .processId(process2.getId())
                .sort(20)
                .keyFlag(false)
                .checkFlag(false)
                .batchRecordReportId(reportId2)
                .build();
        routeProcessMapper.insert(routeProcess2);
        routeProcessFlowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                .routeId(route.getId())
                .graphVersion(1L)
                .sourceRouteProcessId(routeProcess1.getId())
                .targetRouteProcessId(routeProcess2.getId())
                .relationType("NORMAL")
                .sort(1)
                .build());

        if (reportId1 != null) {
            insertBatchUseConfig(route.getId(), routeProcess1.getId(), "SEQUENTIAL", reportId1);
        }
        if (reportId2 != null) {
            insertBatchUseConfig(route.getId(), routeProcess2.getId(), "SEQUENTIAL", reportId2);
        }
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(route.getId())
                .itemId(productId)
                .quantity(1)
                .build());
        refreshRouteVersionSnapshot(routeVersion.getId(), route);

        return new Fixture(workOrder.getId(), route.getId(), routeVersion.getId(), routeVersion.getVersionNo(),
                productId, reportId1, reportId2);
    }

    private void refreshRouteVersionSnapshot(Long routeVersionId, MesProRouteDO route) {
        routeVersionMapper.updateById(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeSnapshotJson(frozenRouteSnapshotJson(route, routeProcessMapper.selectListByRouteId(route.getId())))
                .build());
    }

    private MesProRouteDO insertExecutableRoute(String routeName, String reportIdPrefix) {
        MesProRouteDO route = MesProRouteDO.builder()
                .code("ROUTE-" + randomLongId())
                .name(routeName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(route);

        MesProProcessDO process = MesProProcessDO.builder()
                .code("P-ALT-" + randomLongId())
                .name(routeName + "工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        processMapper.insert(process);

        String reportId = insertReport(reportIdPrefix + "-" + Math.abs(randomLongId()), routeName + "表");
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(route.getId())
                .processId(process.getId())
                .sort(10)
                .keyFlag(true)
                .checkFlag(false)
                .batchRecordReportId(reportId)
                .build();
        routeProcessMapper.insert(routeProcess);
        insertBatchUseConfig(route.getId(), routeProcess.getId(), "SEQUENTIAL", reportId);
        return route;
    }

    private void replaceEnabledProcessConfigWithoutMovingBinding(Long routeId) {
        MesProRouteFlowProcessConfigDO enabledConfig =
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, "BATCH").stream()
                        .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                        .findFirst()
                        .orElseThrow();
        Long otherRouteProcessId = routeProcessMapper.selectListByRouteId(routeId).stream()
                .map(MesProRouteProcessDO::getId)
                .filter(id -> !Objects.equals(id, enabledConfig.getRouteProcessId()))
                .findFirst()
                .orElseThrow();
        MesProRouteFlowProcessConfigDO disabledConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(enabledConfig.getRouteFlowConfigId())
                .routeId(enabledConfig.getRouteId())
                .routeProcessId(otherRouteProcessId)
                .useType(enabledConfig.getUseType())
                .enabled(Boolean.FALSE)
                .executionMode(enabledConfig.getExecutionMode())
                .build();
        routeFlowProcessConfigMapper.insert(disabledConfig);
        MesProRouteFlowProcessBatchRecordDO binding =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                                List.of(enabledConfig.getRouteProcessId()), "BATCH").stream()
                        .findFirst()
                        .orElseThrow();
        binding.setRouteFlowProcessConfigId(disabledConfig.getId());
        routeFlowProcessBatchRecordMapper.updateById(binding);
        refreshActiveRouteVersionSnapshot(routeId);
    }

    private String insertReport(String reportId, String name) {
        Long definitionId = randomLongId();
        MesProBatchRecordVersionDO version = insertBatchRecordVersion(definitionId, "V1.0", "APPROVED");
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setSampleKey("sample-" + reportId);
        report.setBatchRecordName("压力泵批记录");
        report.setFormSlotType("MAIN");
        report.setRouteKey("A");
        report.setBatchRecordDefinitionId(definitionId);
        report.setBatchRecordVersionId(version.getId());
        report.setSourceFileName("source.xlsx");
        report.setSourceFileSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        report.setSourceTableIndex((int) (Math.abs(randomLongId()) % 10000));
        report.setTableTitle(name);
        report.setReportId(reportId);
        report.setReportCode(reportId);
        report.setReportName(name);
        report.setReportCategoryId("cat");
        report.setLastImportTime(LocalDateTime.now());
        reportMapper.insert(report);
        return reportId;
    }

    private String insertLegacyReportWithoutStableIdentity(String reportId, String name) {
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setSampleKey("sample-" + reportId);
        report.setRouteKey("A");
        report.setSourceFileName("legacy-source.xlsx");
        report.setSourceFileSha256("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        report.setSourceTableIndex((int) (Math.abs(randomLongId()) % 10000));
        report.setReportId(reportId);
        report.setReportCode(reportId);
        report.setReportName(name);
        report.setReportCategoryId("cat");
        report.setLastImportTime(LocalDateTime.now());
        reportMapper.insert(report);
        return reportId;
    }

    private MesProBatchRecordVersionDO insertBatchRecordVersion(Long definitionId, String versionNo, String status) {
        MesProBatchRecordVersionDO version = MesProBatchRecordVersionDO.builder()
                .definitionId(definitionId)
                .versionNo(versionNo)
                .status(status)
                .sourceFileName(versionNo + ".doc")
                .sourceFileSha256("sha-" + definitionId + "-" + versionNo)
                .approvedAt("APPROVED".equals(status) ? LocalDateTime.now() : null)
                .build();
        batchRecordVersionMapper.insert(version);
        return version;
    }

    private String insertVersionedReport(String reportId, String name, Long definitionId, Long versionId,
                                         Integer sourceTableIndex, String formSlotType) {
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setSampleKey("sample-" + reportId);
        report.setBatchRecordName("压力泵批记录");
        report.setFormSlotType(formSlotType);
        report.setRouteKey("A");
        report.setBatchRecordDefinitionId(definitionId);
        report.setBatchRecordVersionId(versionId);
        report.setSourceFileName("source-" + versionId + ".doc");
        report.setSourceFileSha256("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        report.setSourceTableIndex(sourceTableIndex);
        report.setTableTitle(name);
        report.setReportId(reportId);
        report.setReportCode(reportId);
        report.setReportName(name);
        report.setReportCategoryId("cat");
        report.setLastImportTime(LocalDateTime.now());
        reportMapper.insert(report);
        return reportId;
    }

    private MesProTaskDO insertProductionTask(Long workOrderId, Long routeId, Long processId, Long workstationId) {
        MesProTaskDO productionTask = MesProTaskDO.builder()
                .code("TASK-" + randomLongId())
                .name("eDHR 生产任务")
                .workOrderId(workOrderId)
                .routeId(routeId)
                .processId(processId)
                .workstationId(workstationId)
                .status(0)
                .build();
        taskMapper.insert(productionTask);
        return productionTask;
    }

    private MesProScheduleOrderDO insertEffectiveScheduleOrder(Long workOrderId, Long productId, Long routeId) {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .code("SCH-" + randomLongId())
                .workOrderId(workOrderId)
                .productId(productId)
                .routeId(routeId)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .manualFinished(Boolean.FALSE)
                .build();
        scheduleOrderMapper.insert(scheduleOrder);
        return scheduleOrder;
    }

    private void insertBatchUseConfig(Long routeId, Long routeProcessId, String executionMode, String... reportIds) {
        MesProRouteFlowConfigDO useConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, "BATCH");
        if (useConfig == null) {
            useConfig = MesProRouteFlowConfigDO.builder()
                    .routeId(routeId)
                    .useType("BATCH")
                    .enabled(Boolean.TRUE)
                    .configVersion("TEST-BATCH")
                    .build();
            routeFlowConfigMapper.insert(useConfig);
        }
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(useConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .enabled(Boolean.TRUE)
                .executionMode(executionMode)
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        for (int index = 0; index < reportIds.length; index++) {
            FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("表单-" + reportIds[index]);
            routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                    .routeFlowProcessConfigId(processConfig.getId())
                    .routeId(routeId)
                    .routeProcessId(routeProcessId)
                    .useType("BATCH")
                    .batchRecordReportId(reportIds[index])
                    .formSlotType("MAIN")
                    .formBindingKey("FB_" + routeProcessId + "_" + (index + 1))
                    .formTemplateId(templateVersion.getTemplateId())
                    .formTemplateNameSnapshot(templateVersion.getTemplateName())
                    .lastPublishedTemplateVersionId(templateVersion.getId())
                    .lastPublishedTemplateVersionNo(templateVersion.getVersionNo())
                    .recordCategory("BATCH_RECORD")
                    .validationProfile("CONTROLLED_BATCH")
                    .permissionScopeId(5001L)
                    .requiredPolicy("REQUIRED")
                    .ownerRoleKey("PRODUCTION")
                    .archiveVisibility("FINAL_DHR")
                    .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                    .reportSort(index + 1)
                    .build());
        }
        refreshActiveRouteVersionSnapshot(routeId);
    }

    private void insertRouteFlowEdge(Long routeId, Long sourceRouteProcessId, Long targetRouteProcessId, Integer sort) {
        routeProcessFlowEdgeMapper.insert(MesProRouteProcessFlowEdgeDO.builder()
                .routeId(routeId)
                .graphVersion(1L)
                .sourceRouteProcessId(sourceRouteProcessId)
                .targetRouteProcessId(targetRouteProcessId)
                .relationType("NORMAL")
                .sort(sort)
                .build());
    }

    private void insertBatchSharedFormCenterBinding(Long routeId, Long routeProcessId,
                                                    FormTemplateVersionDO templateVersion,
                                                    String formBindingKey, String sharedFormKey,
                                                    String fillableScopeJson) {
        insertBatchFormCenterBinding(routeId, routeProcessId, templateVersion, formBindingKey,
                "BATCH_SHARED", sharedFormKey, fillableScopeJson);
    }

    private void insertBatchProcessFormCenterBinding(Long routeId, Long routeProcessId,
                                                     FormTemplateVersionDO templateVersion,
                                                     String formBindingKey) {
        insertBatchFormCenterBinding(routeId, routeProcessId, templateVersion, formBindingKey,
                "PROCESS", null, null);
    }

    private void insertBatchFormCenterBinding(Long routeId, Long routeProcessId,
                                              FormTemplateVersionDO templateVersion,
                                              String formBindingKey, String instanceScope,
                                              String sharedFormKey, String fillableScopeJson) {
        MesProRouteFlowConfigDO useConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, "BATCH");
        if (useConfig == null) {
            useConfig = MesProRouteFlowConfigDO.builder()
                    .routeId(routeId)
                    .useType("BATCH")
                    .enabled(Boolean.TRUE)
                    .configVersion("TEST-BATCH-FORM-CENTER")
                    .build();
            routeFlowConfigMapper.insert(useConfig);
        }
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(useConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .enabled(Boolean.TRUE)
                .executionMode("SEQUENTIAL")
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                .routeFlowProcessConfigId(processConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .formSlotType("MAIN")
                .formBindingKey(formBindingKey)
                .formTemplateId(templateVersion.getTemplateId())
                .formTemplateNameSnapshot(templateVersion.getTemplateName())
                .lastPublishedTemplateVersionId(templateVersion.getId())
                .lastPublishedTemplateVersionNo(templateVersion.getVersionNo())
                .instanceScope(instanceScope)
                .sharedFormKey(sharedFormKey)
                .fillableScopeJson(fillableScopeJson)
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .permissionScopeId(5001L)
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("4444444444444444444444444444444444444444444444444444444444444444")
                .reportSort(1)
                .build());
        refreshActiveRouteVersionSnapshot(routeId);
    }

    private void stubFormCenterInstanceIds(Long... instanceIds) {
        AtomicInteger index = new AtomicInteger();
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), any()))
                .thenAnswer(invocation -> {
                    int current = index.getAndIncrement();
                    if (current >= instanceIds.length) {
                        throw new AssertionError("unexpected form center instance creation");
                    }
                    FormInstanceRespVO respVO = new FormInstanceRespVO();
                    respVO.setId(instanceIds[current]);
                    return respVO;
                });
    }

    private void insertBatchUseConfigWithSlots(Long routeId, Long routeProcessId, String executionMode,
                                               List<BatchSlot> slots) {
        MesProRouteFlowConfigDO useConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, "BATCH");
        if (useConfig == null) {
            useConfig = MesProRouteFlowConfigDO.builder()
                    .routeId(routeId)
                    .useType("BATCH")
                    .enabled(Boolean.TRUE)
                    .configVersion("TEST-BATCH")
                    .build();
            routeFlowConfigMapper.insert(useConfig);
        }
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(useConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .enabled(Boolean.TRUE)
                .executionMode(executionMode)
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        for (BatchSlot slot : slots) {
            alignReportSlotType(slot.reportId(), slot.formSlotType());
            FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersionForReport(slot.reportId());
            routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                    .routeFlowProcessConfigId(processConfig.getId())
                    .routeId(routeId)
                    .routeProcessId(routeProcessId)
                    .useType("BATCH")
                    .batchRecordReportId(slot.reportId())
                    .formSlotType(slot.formSlotType())
                    .formBindingKey("FB_" + routeProcessId + "_" + slot.reportSort())
                    .formTemplateId(templateVersion.getTemplateId())
                    .formTemplateNameSnapshot(templateVersion.getTemplateName())
                    .lastPublishedTemplateVersionId(templateVersion.getId())
                    .lastPublishedTemplateVersionNo(templateVersion.getVersionNo())
                    .instanceScope(slot.instanceScope())
                    .sharedFormKey(slot.sharedFormKey())
                    .fillableScopeJson(slot.fillableScopeJson())
                    .recordCategory(slot.recordCategory())
                    .validationProfile(slot.validationProfile())
                    .permissionScopeId(slot.permissionScopeId())
                    .recordCategorySnapshotHash(slot.snapshotHash())
                    .requiredPolicy(slot.requiredPolicy())
                    .ownerRoleKey(slot.ownerRoleKey())
                    .archiveVisibility(slot.archiveVisibility())
                    .slotConfigSnapshotHash(slot.snapshotHash())
                    .reportSort(slot.reportSort())
                    .build());
        }
        refreshActiveRouteVersionSnapshot(routeId);
    }

    private void alignReportSlotType(String reportId, String formSlotType) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(reportId);
        if (report == null) {
            throw new IllegalStateException("test report is missing: " + reportId);
        }
        report.setFormSlotType(normalizeTestFormSlotType(formSlotType));
        reportMapper.updateById(report);
    }

    private FormTemplateVersionDO insertPublishedFormTemplateVersionForReport(String reportId) {
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("表单-" + reportId);
        templateVersion.setTemplateId(stableTestTemplateId(reportId));
        formTemplateVersionMapper.updateById(templateVersion);
        return templateVersion;
    }

    private Long stableTestTemplateId(String reportId) {
        return 1_000_000L + Integer.toUnsignedLong(String.valueOf(reportId).hashCode());
    }

    private String normalizeTestFormSlotType(String formSlotType) {
        return formSlotType == null || formSlotType.isBlank() ? "MAIN" : formSlotType;
    }

    private BatchSlot batchSlot(String reportId, String formSlotType, String recordCategory,
                                Long permissionScopeId, String ownerRoleKey, int reportSort) {
        return new BatchSlot(reportId, formSlotType,
                recordCategory == null ? "BATCH_RECORD" : recordCategory,
                recordCategory == null ? "CONTROLLED_BATCH" : "INTERNAL_TRACE",
                permissionScopeId == null ? 5001L : permissionScopeId,
                "REQUIRED",
                ownerRoleKey == null ? "PRODUCTION" : ownerRoleKey,
                "FINAL_DHR",
                "1111111111111111111111111111111111111111111111111111111111111111",
                reportSort,
                "PROCESS",
                null,
                null);
    }

    private BatchSlot sharedBatchSlot(String reportId, String formSlotType, String sharedFormKey,
                                      String fillableScopeJson, int reportSort) {
        return new BatchSlot(reportId, formSlotType, "INTERNAL_RECORD", "INTERNAL_TRACE",
                5012L, "REQUIRED", "QUALITY", "FINAL_DHR",
                "2222222222222222222222222222222222222222222222222222222222222222",
                reportSort, "BATCH_SHARED", sharedFormKey, fillableScopeJson);
    }

    private BatchSlot optionalSharedBatchSlot(String reportId, String formSlotType, String sharedFormKey,
                                              String fillableScopeJson, int reportSort) {
        return new BatchSlot(reportId, formSlotType, "INTERNAL_RECORD", "INTERNAL_TRACE",
                5012L, "OPTIONAL", "QUALITY", "FINAL_DHR",
                "3333333333333333333333333333333333333333333333333333333333333333",
                reportSort, "BATCH_SHARED", sharedFormKey, fillableScopeJson);
    }

    private MesProRouteFlowProcessBatchRecordDO insertBatchUseConfig(Long routeId, Long routeProcessId,
                                                                     String executionMode, String reportId,
                                                                     String recordCategory, String validationProfile,
                                                                     Long permissionScopeId,
                                                                     String recordCategorySnapshotHash) {
        MesProRouteFlowConfigDO useConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, "BATCH");
        if (useConfig == null) {
            useConfig = MesProRouteFlowConfigDO.builder()
                    .routeId(routeId)
                    .useType("BATCH")
                    .enabled(Boolean.TRUE)
                    .configVersion("TEST-BATCH")
                    .build();
            routeFlowConfigMapper.insert(useConfig);
        }
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(useConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .enabled(Boolean.TRUE)
                .executionMode(executionMode)
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        FormTemplateVersionDO templateVersion = insertPublishedFormTemplateVersion("表单-" + reportId);
        MesProRouteFlowProcessBatchRecordDO record = MesProRouteFlowProcessBatchRecordDO.builder()
                .routeFlowProcessConfigId(processConfig.getId())
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .batchRecordReportId(reportId)
                .formSlotType("MAIN")
                .formBindingKey("FB_" + routeProcessId + "_1")
                .formTemplateId(templateVersion.getTemplateId())
                .formTemplateNameSnapshot(templateVersion.getTemplateName())
                .lastPublishedTemplateVersionId(templateVersion.getId())
                .lastPublishedTemplateVersionNo(templateVersion.getVersionNo())
                .recordCategory(recordCategory)
                .validationProfile(validationProfile)
                .permissionScopeId(permissionScopeId)
                .recordCategorySnapshotHash(recordCategorySnapshotHash)
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .slotConfigSnapshotHash("1111111111111111111111111111111111111111111111111111111111111111")
                .reportSort(1)
                .build();
        routeFlowProcessBatchRecordMapper.insert(record);
        refreshActiveRouteVersionSnapshot(routeId);
        return record;
    }

    private void refreshActiveRouteVersionSnapshot(Long routeId) {
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (activeRouteVersion == null || route == null) {
            return;
        }
        refreshRouteVersionSnapshot(activeRouteVersion.getId(), route);
    }

    private FormTemplateVersionDO insertPublishedFormTemplateVersion(String templateName) {
        FormTemplateVersionDO templateVersion = FormTemplateVersionDO.builder()
                .templateId(Math.abs(randomLongId()))
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .templateName(templateName)
                .versionNo("V1.0")
                .status("PUBLISHED")
                .sourceFileName(templateName + ".docx")
                .build();
        formTemplateVersionMapper.insert(templateVersion);
        return templateVersion;
    }

    private String unopenedBatchRecordPreviewReportJson(boolean withAssistRows) {
        JSONObject root = JSON.parseObject("""
                {
                  "name":"preview-assist-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"操作员"},
                        "1":{"text":"","fillForm":{"field":"ebr_preview_r0_c1","component":"Input","componentFlag":"input-text","required":true,"label":"","labelText":"","defaultValue":"OP-001","value":"OP-001"},"edhrCellRule":{"rowIndex":0,"columnIndex":1,"valueType":"STRING","componentFlag":"input-text","required":true,"label":"操作员","helpText":"填写实际执行本表单的操作人员姓名或工号","constraints":{},"source":"MANUAL","confidence":1.0,"reviewed":true}},
                        "2":{"text":"备注"},
                        "3":{"text":"","fillForm":{"field":"ebr_preview_r0_c3","component":"Input","componentFlag":"input-textarea","required":false,"label":"","labelText":""},"edhrCellRule":{"rowIndex":0,"columnIndex":3,"valueType":"STRING","componentFlag":"input-textarea","required":false,"label":"备注","helpText":"记录本次操作相关的补充说明","constraints":{},"source":"MANUAL","confidence":1.0,"reviewed":true}}
                      },
                      "height":24
                    }
                  },
                  "cols":{"0":{"width":100},"1":{"width":120},"2":{"width":100},"3":{"width":180},"len":4},
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":180,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":500
                }
                """);
        if (withAssistRows) {
            root.put("edhrAssistRows", JSON.parseArray("""
                    [
                      {"rowKey":"AR_OPERATOR","description":"操作信息","sort":1,
                        "fields":[{"rowIndex":0,"columnIndex":1}]},
                      {"rowKey":"AR_REMARK","description":"备注信息","sort":2,
                        "fields":[{"rowIndex":0,"columnIndex":3}]}
                    ]
                    """));
            root.put("edhrAssistGridRowCount", 12);
            root.put("edhrAssistGridColumnCount", 9);
        }
        return root.toJSONString();
    }

    private String dynamicFormJimuSchemaJson() {
        JSONObject layout = JSON.parseObject("""
                {
                  "cols": {"0": {"width": 140}, "1": {"width": 220}},
                  "rows": {
                    "3": {
                      "height": 36,
                      "cells": {
                        "0": {"text": "生产批号"},
                        "1": {
                          "text": "",
                          "fillForm": {"label": "旧规则"},
                          "edhrCellRule": {"label": "旧规则"},
                          "edhrSignature": {"enabled": true, "label": "旧签名"}
                        }
                      }
                    },
                    "4": {
                      "height": 36,
                      "cells": {
                        "0": {"text": "复核"},
                        "1": {"text": ""}
                      }
                    }
                  }
                }
                """);
        JSONArray rules = JSON.parseArray("""
                [
                  {
                    "rowIndex": 3,
                    "columnIndex": 1,
                    "label": "生产批号",
                    "valueType": "STRING",
                    "componentFlag": "input-text",
                    "required": true,
                    "reviewed": true,
                    "source": "MANUAL"
                  }
                ]
                """);
        JSONArray markers = JSON.parseArray("""
                [
                  {
                    "rowIndex": 4,
                    "columnIndex": 1,
                    "enabled": true,
                    "actionType": "FORM_REVIEW",
                    "label": "复核签名"
                  }
                ]
                """);
        JSONObject schema = new JSONObject();
        schema.put("sheetLayoutJson", JSON.toJSONString(layout));
        schema.put("cellRules", rules);
        schema.put("signatureCellMarkers", markers);
        return JSON.toJSONString(schema);
    }

    private String dynamicFormJimuSchemaJsonWithAssistRows() {
        JSONObject schema = JSON.parseObject(dynamicFormJimuSchemaJson());
        schema.put("edhrAssistRows", JSON.parseArray("""
                [
                  {"rowKey":"AR_BATCH_CODE","description":"损耗单生产批号","sort":1,
                    "fields":[{"rowIndex":3,"columnIndex":1}]}
                ]
                """));
        return JSON.toJSONString(schema);
    }

    private String dynamicFormRecognizedSchemaJson() {
        return """
                [
                  {"fieldCode":"batchCode","label":"生产批号","fieldType":"text","required":true},
                  {"fieldCode":"reviewDate","label":"复核日期","fieldType":"date","required":false}
                ]
                """;
    }

    private void insertInitialFillAssignmentRule(Long routeId) {
        MesProRouteProcessDO firstRouteProcess = routeProcessMapper.selectListByRouteId(routeId).stream()
                .sorted((left, right) -> Integer.compare(left.getSort(), right.getSort()))
                .findFirst()
                .orElseThrow();
        workTaskAssignmentRuleMapper.insert(MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .routeProcessId(firstRouteProcess.getId())
                .taskType("FILL")
                .assigneeUserId(10001L)
                .candidateSourceType("USER")
                .candidateSourceId(10001L)
                .enabled(Boolean.TRUE)
                .build());
    }

    private EdhrBatchExecutionTaskOpenRespVO openTaskAsFiller(Long batchExecutionId, Long taskId) {
        return openTaskAs(10001L, batchExecutionId, taskId);
    }

    private EdhrBatchExecutionTaskOpenRespVO openTaskAsFiller(Long batchExecutionId, Long taskId, Long workTaskId) {
        return openTaskAs(10001L, batchExecutionId, taskId, workTaskId);
    }

    private EdhrBatchExecutionTaskOpenRespVO openTaskAs(Long userId, Long batchExecutionId, Long taskId) {
        return openTaskAs(userId, batchExecutionId, taskId, null);
    }

    private EdhrBatchExecutionTaskOpenRespVO openTaskAs(Long userId, Long batchExecutionId, Long taskId,
                                                       Long workTaskId) {
        return openTaskAs(userId, batchExecutionId, taskId, workTaskId, null);
    }

    private EdhrBatchExecutionTaskOpenRespVO openTaskAs(Long userId, Long batchExecutionId, Long taskId,
                                                       Long workTaskId, Long assistUserId) {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            return batchExecutionService.openTask(new EdhrBatchExecutionTaskOpenReqVO()
                    .setBatchExecutionId(batchExecutionId)
                    .setTaskId(taskId)
                    .setWorkTaskId(workTaskId)
                    .setAssistUserId(assistUserId));
        }
    }

    private void insertCloseAssignmentRule(Long routeId, Long assigneeUserId) {
        MesProEdhrWorkTaskAssignmentRuleDO existing =
                workTaskAssignmentRuleMapper.selectEnabledByScopeAndType("ROUTE", routeId, "CLOSE");
        MesProEdhrWorkTaskAssignmentRuleDO rule = MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .scopeType("ROUTE")
                .scopeId(routeId)
                .taskType("CLOSE")
                .assigneeUserId(assigneeUserId)
                .candidateSourceType("USER")
                .candidateSourceId(assigneeUserId)
                .enabled(Boolean.TRUE)
                .build();
        if (existing == null) {
            workTaskAssignmentRuleMapper.insert(rule);
            return;
        }
        rule.setId(existing.getId());
        workTaskAssignmentRuleMapper.updateById(rule);
    }

    private void insertRouteReleaseAssignmentRule(Long routeId, String sourceType, Long sourceId) {
        workTaskAssignmentRuleMapper.insert(MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .scopeType("ROUTE")
                .scopeId(routeId)
                .taskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .assigneeUserId("USER".equals(sourceType) ? sourceId : null)
                .candidateSourceType(sourceType)
                .candidateSourceId(sourceId)
                .enabled(Boolean.TRUE)
                .build());
    }

    private void insertCurrentProcessFillRule(Long routeProcessId, String reportId, String ruleType, Long userId) {
        insertCurrentProcessFillRule(routeProcessId, reportId, ruleType, "USER", String.valueOf(userId));
    }

    private void insertCurrentProcessFillRule(Long routeProcessId, String reportId, String ruleType,
                                              String sourceType, String sourceIds) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(reportId);
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordVersionId(report == null ? null : report.getBatchRecordVersionId())
                .setRuleType(ruleType)
                .setSignatureCellKey("")
                .setCandidateSourceType(sourceType)
                .setCandidateSourceIds(sourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(Boolean.TRUE));
    }

    private VisibleBatchFixture openBatchWithSecondProcessCurrentFillers(String batchCode, Long currentFillerUserId) {
        Fixture fixture = insertRouteFixture(true, true);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode(batchCode)
                .setRouteId(fixture.routeId()));
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(batch);
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(routeTasks.get(0).getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(routeTasks.get(1).getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT));
        MesProEdhrBatchExecutionTaskDO currentTask = batchTaskMapper.selectById(routeTasks.get(1).getId());
        if (currentFillerUserId != null) {
            insertCurrentProcessFillRule(currentTask.getRouteProcessId(), fixture.reportId2(), "FILL", currentFillerUserId);
        }
        return new VisibleBatchFixture(batch, currentTask);
    }

    private void stubCurrentFillerUsers() {
        AdminUserRespDTO mine = adminUser(10001L, "王歆", CommonStatusEnum.ENABLE.getStatus());
        AdminUserRespDTO other = adminUser(10002L, "其他填写人", CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUserList(List.of(10001L))).thenReturn(List.of(mine));
        when(adminUserApi.getUserList(List.of(10002L))).thenReturn(List.of(other));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(10001L, mine, 10002L, other));
    }

    private AdminUserRespDTO adminUser(Long userId, String nickname, Integer status) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setNickname(nickname);
        user.setStatus(status);
        return user;
    }

    private void bindApprovedExecution(Long taskId, Long executionId, boolean submit, boolean review, boolean approve) {
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(taskId)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setExecutionCode("BRE-" + executionId)
                .setWorkOrderId(1L)
                .setWorkOrderCode("WO")
                .setBatchCode("BATCH")
                .setStatus(3)
                .setSheetLayoutJson("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"产品信息\"}}}}}")
                .setMetaJson("{\"tableTitle\":\"产品信息\"}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setDomainTraceStatus("VERIFIED"));
        if (submit) {
            insertSignature(executionId, "SUBMIT");
        }
        if (review) {
            insertSignature(executionId, "FORM_REVIEW");
        }
        if (approve) {
            insertSignature(executionId, "APPROVE");
        }
    }

    private void bindFillCompletedExecution(Long taskId, Long executionId) {
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(taskId)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setExecutionCode("BRE-" + executionId)
                .setWorkOrderId(1L)
                .setWorkOrderCode("WO")
                .setBatchCode("BATCH")
                .setStatus(4)
                .setSubmittedAt(LocalDateTime.of(2026, 6, 15, 9, 30))
                .setClosedAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .setSheetLayoutJson("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"产品信息\"}}}}}")
                .setMetaJson("{\"tableTitle\":\"产品信息\"}")
                .setExecutionSnapshotJson("{}")
                .setCellValuesJson("[]")
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setDomainTraceStatus("VERIFIED"));
        insertSignature(executionId, "SUBMIT");
    }

    private EdhrBatchExecutionRespVO prepareClosableBatch(Fixture fixture, String batchCode, Long ownerUserId) {
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(fixture.workOrderId())
                .setBatchCode(batchCode)
                .setRouteId(fixture.routeId()));
        List<EdhrBatchExecutionTaskRespVO> routeTasks = routeTasks(batch);
        for (int i = 0; i < routeTasks.size(); i++) {
            bindFillCompletedExecution(routeTasks.get(i).getId(), randomLongId());
        }
        completeFinalInspectionDossier(batch.getId());
        skipAllSpecialNodes(batch);
        markBatchOwner(batch.getId(), ownerUserId);
        return batchExecutionService.get(batch.getId());
    }

    private void markReleasePrecheckPassed(Long batchExecutionId) {
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-PRECHECK-PASS-" + randomLongId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED)
                .setRequiredCheckCount(6)
                .setFailedCheckCount(0)
                .setBlockingCheckCount(0));
    }

    private void markReleasePrecheckFailed(Long batchExecutionId) {
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-PRECHECK-FAIL-" + randomLongId())
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED)
                .setRequiredCheckCount(6)
                .setFailedCheckCount(1)
                .setBlockingCheckCount(1));
    }

    private void bindApprovedExecutionWithSnapshot(Long taskId, Long executionId, String executionSnapshotJson) {
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(taskId)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setExecutionCode("BRE-" + executionId)
                .setWorkOrderId(1L)
                .setWorkOrderCode("WO")
                .setBatchCode("BATCH")
                .setStatus(3)
                .setSheetLayoutJson("{\"rows\":{\"0\":{\"cells\":{\"0\":{\"text\":\"产品信息\"}}}}}")
                .setMetaJson("{\"tableTitle\":\"产品信息\"}")
                .setExecutionSnapshotJson(executionSnapshotJson)
                .setCellValuesJson("[]")
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setDomainTraceStatus("VERIFIED"));
        insertSignature(executionId, "SUBMIT");
        insertSignature(executionId, "FORM_REVIEW");
        insertSignature(executionId, "APPROVE");
    }

    private void bindApprovedExecutionPrintable(Long taskId, Long executionId, String remark) {
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(taskId)
                .setExecutionId(executionId)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .setApprovedAt(LocalDateTime.of(2026, 6, 15, 10, 15)));
        executionMapper.insert(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setExecutionCode("BRE-" + executionId)
                .setWorkOrderId(1L)
                .setWorkOrderCode("WO")
                .setBatchCode("BATCH")
                .setStatus(3)
                .setSubmittedAt(LocalDateTime.of(2026, 6, 15, 9, 30))
                .setApprovedAt(LocalDateTime.of(2026, 6, 15, 10, 15))
                .setSheetLayoutJson("""
                        {"cols":{"0":{"width":180},"1":{"width":220},"2":{"width":160},"3":{"width":200}},"rows":{"0":{"height":36,"cells":{"0":{"text":"产品信息","merge":[0,3]}}},"1":{"cells":{"0":{"text":"项目"},"1":{"text":"","fillForm":{"field":"item","value":"首件确认"}},"2":{"text":"重量"},"3":{"text":"","fillForm":{"field":"weight"}}}},"2":{"cells":{"0":{"text":"是否通过"},"1":{"text":"","fillForm":{"field":"pass"}},"2":{"text":"审核签名"},"3":{"text":"","edhrSignature":{"enabled":true,"actionType":"FORM_REVIEW","label":"审核签名","displayFormat":"ACTOR_SIGNED_AT"}}}}}}
                        """)
                .setMetaJson("{\"tableTitle\":\"工序一记录表\"}")
                .setExecutionSnapshotJson("""
                        {"layout":{"cols":{"0":{"width":180},"1":{"width":220},"2":{"width":160},"3":{"width":200}},"rows":{"0":{"height":36,"cells":{"0":{"text":"产品信息","merge":[0,3]}}},"1":{"cells":{"0":{"text":"项目"},"1":{"text":"","fillForm":{"field":"item","value":"首件确认"}},"2":{"text":"重量"},"3":{"text":"","fillForm":{"field":"weight"}}}},"2":{"cells":{"0":{"text":"是否通过"},"1":{"text":"","fillForm":{"field":"pass"}},"2":{"text":"审核签名"},"3":{"text":"","edhrSignature":{"enabled":true,"actionType":"FORM_REVIEW","label":"审核签名","displayFormat":"ACTOR_SIGNED_AT"}}}}}},"fields":[{"fieldPath":"rows[2].cells[3].photo","fieldKey":"photo","label":"现场照片","rowIndex":2,"columnIndex":3,"attachmentRule":{"required":true,"minCount":1,"maxCount":3,"attachmentType":"IMAGE","groupKey":"photo-1"}}]}
                        """)
                .setCellValuesJson("""
                        [{"rowIndex":1,"columnIndex":1,"valueType":"STRING","value":"首件确认","valueDisplay":"首件确认"},{"rowIndex":1,"columnIndex":3,"valueType":"NUMBER","value":37.5,"valueDisplay":"37.5","unit":"kg"},{"rowIndex":2,"columnIndex":1,"valueType":"BOOLEAN","value":true,"valueDisplay":"是"}]
                        """)
                .setRemark(remark)
                .setCellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setDomainTraceStatus("VERIFIED"));
        insertSignature(executionId, "SUBMIT", "提交人", LocalDateTime.of(2026, 6, 15, 9, 35));
        insertSignature(executionId, "FORM_REVIEW", "签名人", LocalDateTime.of(2026, 6, 15, 9, 45));
        insertSignature(executionId, "APPROVE", "批准人", LocalDateTime.of(2026, 6, 15, 10, 15));
    }

    private void insertSignature(Long executionId, String actionType) {
        insertSignature(executionId, actionType, "签名人", LocalDateTime.now());
    }

    private void insertSignature(Long executionId, String actionType, String actorName, LocalDateTime signedAt) {
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(executionId)
                .actorId(1L)
                .actorName(actorName)
                .actionType(actionType)
                .signatureMode("PASSWORD")
                .passwordVerified(true)
                .signedAt(signedAt)
                .cellValuesHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .fieldAuditHeadHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .fieldAuditRevision(1L)
                .build());
    }

    private void completeFinalInspectionDossier(Long batchExecutionId) {
        MesProEdhrBatchDossierItemDO item = dossierItemMapper.selectRequiredFinalInspection(batchExecutionId);
        MesProEdhrBatchDossierItemDO completed = new MesProEdhrBatchDossierItemDO()
                .setBatchExecutionId(batchExecutionId)
                .setItemType("FINAL_INSPECTION")
                .setItemKey("FINAL_INSPECTION")
                .setItemName("成品检")
                .setRequiredFlag(Boolean.TRUE)
                .setItemStatus("COMPLETED")
                .setSourceDocType("OQC")
                .setSourceDocId(8801L)
                .setSourceDocCode("OQC-FINAL-" + batchExecutionId)
                .setSourceDocStatus("APPROVED")
                .setSourceDocResult("PASS")
                .setSourceDocHash("abababababababababababababababababababababababababababababababab")
                .setCompletedAt(LocalDateTime.now())
                .setVerifiedAt(LocalDateTime.now());
        if (item == null) {
            dossierItemMapper.insert(completed);
            return;
        }
        completed.setId(item.getId());
        dossierItemMapper.updateById(completed);
    }

    private void markBatchOwner(Long batchExecutionId, Long ownerUserId) {
        markBatchCreatorOnly(batchExecutionId, ownerUserId);
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        insertCloseAssignmentRule(batch.getRouteId(), ownerUserId);
        configureBatchSpecialAttachmentOwners(batchExecutionId, ownerUserId);
    }

    private void markBatchCreatorOnly(Long batchExecutionId, Long ownerUserId) {
        MesProEdhrBatchExecutionDO owner = new MesProEdhrBatchExecutionDO().setId(batchExecutionId);
        owner.setCreator(String.valueOf(ownerUserId));
        batchExecutionMapper.updateById(owner);
    }

    private void insertAttachmentEvent(Long batchExecutionId, Long batchTaskId, Long executionId,
                                       String fileName, String attachmentHash) {
        attachmentMapper.insert(new MesProBatchRecordExecutionAttachmentDO()
                .setExecutionId(executionId)
                .setBatchExecutionId(batchExecutionId)
                .setBatchTaskId(batchTaskId)
                .setWorkTaskId(9101L)
                .setRowIndex(2)
                .setColumnIndex(3)
                .setFieldKey("photo")
                .setFieldPath("rows[2].cells[3]")
                .setFieldLabel("现场照片")
                .setAttachmentType("IMAGE")
                .setAttachmentGroupKey("photo-1")
                .setAttachmentAction("ADD")
                .setVersionNo(1)
                .setFileId(9201L)
                .setFileUrl("http://127.0.0.1:9000/yudao/edhr/photo.jpg")
                .setStorageConfigId(28L)
                .setStoragePath("edhr/photo.jpg")
                .setFileName(fileName)
                .setContentType("image/jpeg")
                .setFileSize(128L)
                .setSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setStorageRetentionHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setAuditBatchId(9301L)
                .setSignatureId(9401L)
                .setPreviousAttachmentHash("0000000000000000000000000000000000000000000000000000000000000000")
                .setAttachmentHash(attachmentHash)
                .setOperatorId(9501L)
                .setOperatorName("操作员")
                .setOperatedAt(LocalDateTime.now())
                .setReasonCategory("FIELD_ATTACHMENT")
                .setReasonText("附件 manifest 测试"));
    }

    private MesProEdhrWorkTaskDO insertFillWorkTask(EdhrBatchExecutionRespVO batch,
                                                    EdhrBatchExecutionTaskRespVO batchTask,
                                                    Fixture fixture) {
        return insertWorkTask(batch, batchTask, fixture, MesProEdhrWorkTaskService.TASK_TYPE_FILL, 10001L);
    }

    private MesProEdhrWorkTaskDO insertWorkTask(EdhrBatchExecutionRespVO batch,
                                                EdhrBatchExecutionTaskRespVO batchTask,
                                                Fixture fixture,
                                                String taskType,
                                                Long assigneeUserId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(fixture.workOrderId());
        MesProEdhrWorkTaskDO workTask = MesProEdhrWorkTaskDO.builder()
                .taskCode("WT-" + randomLongId())
                .taskType(taskType)
                .batchExecutionId(batch.getId())
                .batchTaskId(batchTask.getId())
                .businessScopeType("BATCH_TASK")
                .businessScopeId(batchTask.getId())
                .workOrderId(fixture.workOrderId())
                .workOrderCode(workOrder == null ? null : workOrder.getCode())
                .batchCode("BATCH-WORK-TASK")
                .routeId(fixture.routeId())
                .routeProcessId(batchTask.getRouteProcessId())
                .processId(batchTask.getProcessId())
                .processName(batchTask.getProcessName())
                .assigneeUserId(assigneeUserId)
                .candidateSourceType("USER")
                .candidateSourceId(assigneeUserId)
                .candidateUserSnapshot(String.valueOf(assigneeUserId))
                .status(MesProEdhrWorkTaskStatus.TODO)
                .actionUrl("/mes/pro/feedback/edhr-batch-execution/detail?id=" + batch.getId())
                .remark("eDHR填写任务")
                .build();
        workTaskMapper.insert(workTask);
        return workTask;
    }

    private String readSource(String relativePath) throws Exception {
        return Files.readString(Path.of(System.getProperty("user.dir")).resolve(relativePath), StandardCharsets.UTF_8);
    }

    private record Fixture(Long workOrderId, Long routeId, Long routeVersionId, String routeVersionNo,
                           Long productId, String reportId1, String reportId2) {
    }

    private record VisibleBatchFixture(EdhrBatchExecutionRespVO batch,
                                       MesProEdhrBatchExecutionTaskDO currentTask) {
    }

    private record BatchSlot(String reportId, String formSlotType, String recordCategory,
                              String validationProfile, Long permissionScopeId,
                              String requiredPolicy, String ownerRoleKey, String archiveVisibility,
                              String snapshotHash, Integer reportSort, String instanceScope,
                              String sharedFormKey, String fillableScopeJson) {
    }
}
