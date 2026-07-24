package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCellValueVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCreateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignaturePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordApprovalSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.batch.MesWmBatchDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.batch.MesWmBatchMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_BATCH_RECORD_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_BATCH_CODE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_COUNT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REQUIRED_FIELD_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProBatchRecordExecutionServiceImpl.class, MesProEdhrCandidateResolver.class,
        MesProEdhrPreReleaseEditabilityService.class, MesProEdhrGoldenFingerPermissionService.class})
class MesProBatchRecordExecutionServiceImplTest extends BaseDbUnitTest {

    private static final String DOMAIN_TRACE_HASH =
            "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";

    @Resource
    private MesProBatchRecordExecutionService executionService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordTemplateMapper templateMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper edhrBatchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper edhrBatchExecutionTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesWmBatchMapper batchMapper;

    @MockitoBean
    private MesProBatchRecordExecutionSignatureService executionSignatureService;
    @MockitoBean
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    @MockitoBean
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;
    @MockitoBean
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @MockitoBean
    private BpmTaskService bpmTaskService;
    @MockitoBean
    private MesProBatchRecordDomainTraceService domainTraceService;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;
    @MockitoBean
    private MesProEdhrPermissionGateService permissionGateService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private RoleApi roleApi;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;

    @BeforeEach
    void setUpFieldAuditVerification() {
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> {
                    Long routeProcessId = invocation.getArgument(0);
                    MesProRouteProcessDO direct = routeProcessMapper.selectById(routeProcessId);
                    return direct != null ? direct : MesProRouteProcessDO.builder()
                            .id(routeProcessId)
                            .routeId(invocation.getArgument(1))
                            .processId(invocation.getArgument(2))
                            .build();
                });
        lenient().when(routeProcessService.resolveFrozenRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> {
                    Long routeProcessId = invocation.getArgument(0);
                    MesProRouteProcessDO direct = routeProcessMapper.selectById(routeProcessId);
                    return direct != null ? direct : MesProRouteProcessDO.builder()
                            .id(routeProcessId)
                            .routeId(invocation.getArgument(1))
                            .processId(invocation.getArgument(2))
                            .build();
                });
        lenient().when(fieldAuditService.verifyChain(anyLong()))
                .thenReturn(MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                        MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                        MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                        0L, 0L));
        lenient().when(domainTraceService.verifyForSubmit(anyLong()))
                .thenReturn(new MesProBatchRecordDomainTraceDetailRespVO()
                        .setDomainTraceSnapshotId(7001L)
                        .setDomainTraceHash(DOMAIN_TRACE_HASH)
                        .setStatus("VERIFIED"));
        lenient().when(workTaskService.requireReworkAssigneeUserId(any(), any())).thenReturn(77L);
        lenient().when(workTaskService.completeReviewAndCreateRework(any(), any(), any(), any()))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(6001L));
        lenient().when(workTaskService.createApproveTaskAfterReview(any()))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(6201L));
        lenient().when(adminUserApi.getUserListByPostIds(any())).thenReturn(List.of(enabledReviewUser()));
        lenient().when(businessApprovalOrchestrator.submit(any(BusinessApprovalContext.class)))
                .thenAnswer(invocation -> submitThroughPlatformApprovalHarness(invocation.getArgument(0)));
    }

    private BusinessApprovalRequest submitThroughPlatformApprovalHarness(BusinessApprovalContext context) {
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO();
        reqDTO.setProcessDefinitionKey(String.valueOf(context.getVariables().get("processDefinitionKey")));
        reqDTO.setBusinessKey(String.valueOf(context.getVariables().get("executionBusinessKey")));
        reqDTO.setVariables(buildBusinessApprovalBpmVariables(context, 91001L));
        reqDTO.setStartUserSelectAssignees(context.getStartUserSelectAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(context.getApplicantUserId(), reqDTO);
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new ServiceException(PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS);
        }
        BusinessApprovalRequest request = BusinessApprovalRequest.builder()
                .requestId(91001L)
                .context(context)
                .status(BusinessApprovalRequestStatus.PENDING_BPM)
                .processInstanceId(processInstanceId)
                .build();
        MesProBatchRecordExecutionBusinessApprovalEffectExecutor effectExecutor =
                new MesProBatchRecordExecutionBusinessApprovalEffectExecutor();
        ReflectionTestUtils.setField(effectExecutor, "executionMapper", executionMapper);
        ReflectionTestUtils.setField(effectExecutor, "approvalSnapshotMapper", approvalSnapshotMapper);
        ReflectionTestUtils.setField(effectExecutor, "executionSignatureService", executionSignatureService);
        ReflectionTestUtils.setField(effectExecutor, "workTaskService", workTaskService);
        ReflectionTestUtils.setField(effectExecutor, "bpmTaskService", bpmTaskService);
        try {
            effectExecutor.markPending(context, request);
        } catch (RuntimeException ex) {
            processInstanceApi.cancelProcessInstance(context.getApplicantUserId(), processInstanceId,
                    "eDHR submit compensation: executionId=" + context.getObjectId());
            throw ex;
        }
        return request;
    }

    private Map<String, Object> buildBusinessApprovalBpmVariables(BusinessApprovalContext context, Long requestId) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("tenantId", context.getTenantId());
        variables.put("businessType", context.getSystemCode() + "_" + context.getObjectType() + "_" + context.getActionCode());
        variables.put("approvalRequestId", requestId);
        variables.put("dataDomain", context.getDataDomain());
        variables.put("systemCode", context.getSystemCode());
        variables.put("objectType", context.getObjectType());
        variables.put("objectId", context.getObjectId());
        variables.put("objectVersion", context.getObjectVersion());
        variables.put("actionCode", context.getActionCode());
        variables.put("objectState", context.getObjectState());
        variables.put("businessKey", context.getObjectType() + ":" + context.getObjectId() + ":" + context.getActionCode());
        variables.put("reason", context.getReason());
        if (context.getVariables() != null && !context.getVariables().isEmpty()) {
            context.getVariables().forEach(variables::putIfAbsent);
        }
        if (context.getStartUserSelectAssignees() != null && !context.getStartUserSelectAssignees().isEmpty()) {
            variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                    context.getStartUserSelectAssignees());
        }
        return variables;
    }

    @Test
    void requireRouteProcess_deletedSnapshot_resolvesFrozenRouteProcess() throws Exception {
        MesProRouteProcessDO frozenRouteProcess = MesProRouteProcessDO.builder()
                .id(101L)
                .routeId(201L)
                .processId(301L)
                .build();
        when(routeProcessService.resolveFrozenRouteProcess(100L, 200L, 300L))
                .thenReturn(frozenRouteProcess);
        Method method = MesProBatchRecordExecutionServiceImpl.class.getDeclaredMethod(
                "requireRouteProcess", Long.class, Long.class, Long.class);
        method.setAccessible(true);
        MesProBatchRecordExecutionServiceImpl target = AopTestUtils.getTargetObject(executionService);

        Object result = method.invoke(target, 100L, 200L, 300L);

        assertSame(frozenRouteProcess, result);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(100L, 200L, 300L);
    }

    @Test
    void resolveReviewSignatureAssignments_shouldUseFrozenBatchRecordVersionSignatureRule() throws Exception {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-VERSIONED-SIGN", "BATCH-VERSIONED-SIGN");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setBatchRecordVersionId(7701L)
                .setExecutionSnapshotJson(reviewExecutionSnapshotJson()));
        execution = executionMapper.selectById(execution.getId());
        processFormPermissionRuleMapper.insert(processFormPermissionRule(execution.getRouteProcessId(),
                execution.getBatchRecordReportId(), 7701L, "801", "V1 reviewer"));
        processFormPermissionRuleMapper.insert(processFormPermissionRule(execution.getRouteProcessId(),
                execution.getBatchRecordReportId(), 7702L, "802", "V2 reviewer"));
        when(adminUserApi.getUserList(Set.of(801L))).thenReturn(List.of(enabledReviewUser(801L, "V1审核人")));

        Method method = MesProBatchRecordExecutionServiceImpl.class.getDeclaredMethod(
                "resolveReviewSignatureAssignments", MesProBatchRecordExecutionDO.class);
        method.setAccessible(true);
        MesProBatchRecordExecutionServiceImpl target = AopTestUtils.getTargetObject(executionService);

        List<?> assignments = (List<?>) method.invoke(target, execution);

        assertEquals(1, assignments.size());
        assertEquals(801L, ReflectionTestUtils.getField(assignments.get(0), "reviewSourceId"));
    }

    @Test
    void buildResp_deletedSnapshot_usesFrozenRouteProcessIdentity() throws Exception {
        MesProRouteDO route = MesProRouteDO.builder()
                .code("ROUTE-LEGACY-RESP")
                .name("历史快照响应路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(route);
        MesProProcessDO process = MesProProcessDO.builder()
                .code("PROCESS-LEGACY-RESP")
                .name("当前工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        processMapper.insert(process);
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(101L)
                .routeId(route.getId())
                .processId(process.getId())
                .build();
        when(routeProcessService.resolveFrozenRouteProcess(100L, route.getId(), null))
                .thenReturn(currentRouteProcess);
        MesProBatchRecordExecutionDO execution = new MesProBatchRecordExecutionDO()
                .setRouteId(route.getId())
                .setRouteProcessId(100L);
        Method method = MesProBatchRecordExecutionServiceImpl.class.getDeclaredMethod(
                "buildResp", MesProBatchRecordExecutionDO.class);
        method.setAccessible(true);
        MesProBatchRecordExecutionServiceImpl target = AopTestUtils.getTargetObject(executionService);

        MesProBatchRecordExecutionRespVO result =
                (MesProBatchRecordExecutionRespVO) method.invoke(target, execution);

        assertEquals(route.getId(), result.getRouteId());
        assertEquals(route.getName(), result.getRouteName());
        assertEquals(process.getId(), result.getProcessId());
        assertEquals(process.getName(), result.getProcessName());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(100L, route.getId(), null);
    }

    @Test
    void createExecution_snapshotsTemplateAndStartsDraft() {
        MesProBatchRecordTemplateDO template = insertTemplate();
        MesProWorkOrderDO workOrder = insertWorkOrder();

        MesProBatchRecordExecutionCreateReqVO reqVO = new MesProBatchRecordExecutionCreateReqVO();
        reqVO.setTemplateId(template.getId());
        reqVO.setWorkOrderId(workOrder.getId());
        reqVO.setBatchCode("BATCH-20260513-01");

        MesProBatchRecordExecutionCreateRespVO response = executionService.createBatchRecordExecution(reqVO);

        assertNotNull(response.getId());
        assertNotNull(response.getExecutionCode());
        assertEquals(0, response.getStatus());

        MesProBatchRecordExecutionDO execution = executionMapper.selectById(response.getId());
        assertEquals(template.getId(), execution.getTemplateId());
        assertEquals(template.getTemplateCode(), execution.getTemplateCode());
        assertEquals(template.getTemplateName(), execution.getTemplateName());
        assertEquals(template.getSheetLayoutJson(), execution.getSheetLayoutJson());
        assertEquals(template.getMetaJson(), execution.getMetaJson());
        assertNotNull(execution.getExecutionSnapshotJson());
        assertEquals(workOrder.getId(), execution.getWorkOrderId());
        assertEquals(workOrder.getCode(), execution.getWorkOrderCode());
        assertEquals("BATCH-20260513-01", execution.getBatchCode());
        assertEquals(0, execution.getStatus());
    }

    @Test
    void createExecution_validatesTemplateWorkOrderAndExplicitBatchCode() {
        MesProWorkOrderDO workOrder = insertWorkOrder();

        MesProBatchRecordExecutionCreateReqVO missingTemplate = new MesProBatchRecordExecutionCreateReqVO();
        missingTemplate.setTemplateId(randomLongId());
        missingTemplate.setWorkOrderId(workOrder.getId());
        missingTemplate.setBatchCode("BATCH-20260513-01");
        assertServiceException(() -> executionService.createBatchRecordExecution(missingTemplate),
                PRO_BATCH_RECORD_TEMPLATE_NOT_EXISTS);

        MesProBatchRecordTemplateDO template = insertTemplate();
        MesProBatchRecordExecutionCreateReqVO missingWorkOrder = new MesProBatchRecordExecutionCreateReqVO();
        missingWorkOrder.setTemplateId(template.getId());
        missingWorkOrder.setWorkOrderId(randomLongId());
        missingWorkOrder.setBatchCode("BATCH-20260513-01");
        assertServiceException(() -> executionService.createBatchRecordExecution(missingWorkOrder),
                PRO_WORK_ORDER_NOT_EXISTS);

        MesProBatchRecordExecutionCreateReqVO blankBatchCode = new MesProBatchRecordExecutionCreateReqVO();
        blankBatchCode.setTemplateId(template.getId());
        blankBatchCode.setWorkOrderId(workOrder.getId());
        blankBatchCode.setBatchCode(" ");
        assertServiceException(() -> executionService.createBatchRecordExecution(blankBatchCode),
                PRO_BATCH_RECORD_EXECUTION_BATCH_CODE_REQUIRED);
    }

    @Test
    void pageGetLegacyDraftGateAndSubmit_followDraftOnlyRules() {
        MesProBatchRecordTemplateDO template = insertTemplate();
        MesProWorkOrderDO workOrder = insertWorkOrder();

        MesProBatchRecordExecutionCreateReqVO createReqVO = new MesProBatchRecordExecutionCreateReqVO();
        createReqVO.setTemplateId(template.getId());
        createReqVO.setWorkOrderId(workOrder.getId());
        createReqVO.setBatchCode("BATCH-20260513-02");
        Long executionId = executionService.createBatchRecordExecution(createReqVO).getId();

        MesProBatchRecordExecutionSaveDraftReqVO saveDraftReqVO = new MesProBatchRecordExecutionSaveDraftReqVO();
        saveDraftReqVO.setId(executionId);
        saveDraftReqVO.setCellValues(List.of(
                new MesProBatchRecordExecutionCellValueVO().setRowIndex(2).setColumnIndex(1).setValue("BATCH-20260513-02"),
                new MesProBatchRecordExecutionCellValueVO().setRowIndex(2).setColumnIndex(3).setValue("120")));
        saveDraftReqVO.setRemark("draft note");
        assertServiceException(() -> executionService.saveBatchRecordExecutionDraft(saveDraftReqVO),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);

        MesProBatchRecordExecutionSaveDraftReqVO signatureDraftReqVO = new MesProBatchRecordExecutionSaveDraftReqVO();
        signatureDraftReqVO.setId(executionId);
        signatureDraftReqVO.setCellValues(List.of(new MesProBatchRecordExecutionCellValueVO()
                .setRowIndex(4)
                .setColumnIndex(2)
                .setValue("张三")
                .setValueType("SIGNATURE")));
        assertServiceException(() -> executionService.saveBatchRecordExecutionDraft(signatureDraftReqVO),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN);

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(executionId);
        assertEquals(0, detail.getCellValues().size());
        assertNull(detail.getRemark());

        MesProBatchRecordExecutionPageReqVO pageReqVO = new MesProBatchRecordExecutionPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setTemplateId(template.getId());
        PageResult<MesProBatchRecordExecutionRespVO> page = executionService.getBatchRecordExecutionPage(pageReqVO);
        assertEquals(1, page.getTotal());
        assertEquals(workOrder.getCode(), page.getList().get(0).getWorkOrderCode());

        MesProBatchRecordExecutionSubmitReqVO submitReqVO = new MesProBatchRecordExecutionSubmitReqVO()
                .setId(executionId)
                .setWorkTaskId(8001L)
                .setPassword("secret")
                .setComment("submit note")
                .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                        .setSignatureCellKey("R1C1")
                        .setSelectedUserId(88L)));
        attachDefaultApprovalContext(executionId);
        attachReviewSignatureSnapshot(executionId);
        Task task = mockTask("task-submit", "process-submit", "approveNode", "审批");
        when(executionSignatureService.recordSubmitSignature(executionId, "secret", "submit note")).thenReturn(1001L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit", null, "approveNode")).thenReturn(List.of(task));
        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(submitReqVO);
        } finally {
            TenantContextHolder.clear();
        }
        verify(executionSignatureService).recordSubmitSignature(executionId, "secret", "submit note");
        verify(executionSignatureService)
                .bindSignatureFieldAuditEvidence(1001L, executionId,
                        0L, MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                        MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"));

        MesProBatchRecordExecutionRespVO submitted = executionService.getBatchRecordExecution(executionId);
        assertEquals(1, submitted.getStatus());
        assertEquals("process-submit", submitted.getProcessInstanceId());

        assertServiceException(() -> executionService.saveBatchRecordExecutionDraft(saveDraftReqVO),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
        assertServiceException(() -> executionService.submitBatchRecordExecution(submitReqVO),
                PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        assertServiceException(() -> executionService.getBatchRecordExecution(randomLongId()),
                PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void submitBatchRecordExecution_startsBpmAndPersistsApprovalSnapshot() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-BPM", "BATCH-SUBMIT-BPM"));
        Task task = mockTask("task-submit-bpm", "process-submit-bpm", "approveNode", "审批");
        when(executionSignatureService.recordSubmitSignature(eq(execution.getId()), eq("secret"), eq("提交审批"),
                any(MesProBatchRecordExecutionSignatureTimeCommand.class))).thenReturn(1101L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-bpm");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-bpm", null, "approveNode"))
                .thenReturn(List.of(task));
        LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 10, 30, 0);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                            .setSelectedSignedAt(selectedSignedAt)
                            .setSelectedTimeZone("Asia/Shanghai")
                            .setSelectedTimeReason("纸质记录补签"))
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(88L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<MesProBatchRecordExecutionSignatureTimeCommand> timeCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureTimeCommand.class);
        verify(executionSignatureService).recordSubmitSignature(eq(execution.getId()), eq("secret"),
                eq("提交审批"), timeCaptor.capture());
        assertEquals(selectedSignedAt, timeCaptor.getValue().getSelectedSignedAt());
        assertEquals("Asia/Shanghai", timeCaptor.getValue().getSelectedTimeZone());
        assertEquals("纸质记录补签", timeCaptor.getValue().getSelectedTimeReason());

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(eq(99L), bpmCaptor.capture());
        assertEquals("mes-edhr-approval-v1", bpmCaptor.getValue().getProcessDefinitionKey());
        assertEquals("EDHR_EXECUTION:" + execution.getId(), bpmCaptor.getValue().getBusinessKey());
        Map<String, Object> variables = bpmCaptor.getValue().getVariables();
        assertEquals(execution.getId(), variables.get("edhrExecutionId"));
        assertEquals(execution.getExecutionCode(), variables.get("edhrExecutionCode"));
        assertEquals(execution.getWorkOrderId(), variables.get("workOrderId"));
        assertEquals(execution.getWorkOrderCode(), variables.get("workOrderCode"));
        assertEquals(execution.getTaskId(), variables.get("taskId"));
        assertEquals(execution.getRouteProcessId(), variables.get("routeProcessId"));
        assertEquals(2002L, variables.get("processId"));
        assertEquals("焊接", variables.get("processName"));
        assertEquals(execution.getWorkstationId(), variables.get("workstationId"));
        assertEquals("焊接工位", variables.get("workstationName"));
        assertEquals(execution.getBatchCode(), variables.get("batchCode"));
        assertEquals(DigestUtil.sha256Hex(execution.getExecutionSnapshotJson().trim()), variables.get("executionSnapshotHash"));
        assertEquals(execution.getCellValuesHash(), variables.get("cellValuesHash"));
        assertEquals(execution.getFieldAuditRevision(), variables.get("fieldAuditRevision"));
        assertEquals(execution.getFieldAuditHeadHash(), variables.get("fieldAuditHeadHash"));
        assertEquals(122L, variables.get("tenantId"));
        assertTrue(String.valueOf(variables.get("edhrReviewSignatureCells")).contains("R1C1"));
        assertEquals(List.of(88L), bpmCaptor.getValue().getStartUserSelectAssignees().get("approveNode"));

        MesProBatchRecordExecutionDO submitted = executionMapper.selectById(execution.getId());
        assertEquals(1, submitted.getStatus());
        assertEquals("process-submit-bpm", submitted.getProcessInstanceId());
        assertEquals(99L, submitted.getSubmittedBy());
        assertNotNull(submitted.getSubmittedAt());

        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertNotNull(snapshot);
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        assertEquals("process-submit-bpm", snapshot.getProcessInstanceId());
        assertNull(snapshot.getCurrentBpmTaskId());
        assertEquals("approveNode", snapshot.getCurrentTaskDefinitionKey());
        assertEquals(1101L, snapshot.getSubmitSignatureId());
        JSONObject snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        assertEquals(execution.getCellValuesHash(), snapshotJson.getString("cellValuesHash"));
        assertEquals(execution.getFieldAuditRevision(), snapshotJson.getLong("fieldAuditRevision"));
        assertEquals(execution.getFieldAuditHeadHash(), snapshotJson.getString("fieldAuditHeadHash"));
        assertEquals(1, snapshotJson.getJSONArray("reviewAssignments").size());
        verify(executionSignatureService)
                .attachSubmitSignatureProcessInstance(1101L, execution.getId(), "process-submit-bpm");
        verify(executionSignatureService)
                .bindSignatureFieldAuditEvidence(1101L, execution.getId(),
                        execution.getFieldAuditRevision(), execution.getFieldAuditHeadHash(), execution.getCellValuesHash());
        ArgumentCaptor<List> reviewTaskCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(execution.getId()), reviewTaskCaptor.capture());
        MesProEdhrReviewTaskCreateCommand reviewTaskCommand =
                (MesProEdhrReviewTaskCreateCommand) reviewTaskCaptor.getValue().get(0);
        assertEquals("R1C1", reviewTaskCommand.getSignatureCellKey());
        assertEquals(88L, reviewTaskCommand.getAssigneeUserId());
        assertEquals("task-submit-bpm", reviewTaskCommand.getBpmTaskId());
    }

    @Test
    void submitBatchRecordExecution_submitsThroughBusinessApprovalOrchestratorWithDomainVariables() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-PLATFORM", "BATCH-SUBMIT-PLATFORM"));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批"))
                .thenReturn(1301L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("legacy-direct-process");
        when(businessApprovalOrchestrator.submit(any(BusinessApprovalContext.class)))
                .thenReturn(BusinessApprovalRequest.builder()
                        .requestId(91001L)
                        .status(BusinessApprovalRequestStatus.PENDING_BPM)
                        .processInstanceId("process-submit-platform")
                        .build());

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(88L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<BusinessApprovalContext> contextCaptor = ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals(122L, context.getTenantId());
        assertEquals("MES", context.getDataDomain());
        assertEquals("MES", context.getSystemCode());
        assertEquals("EDHR_BATCH_EXECUTION", context.getObjectType());
        assertEquals(String.valueOf(execution.getId()), context.getObjectId());
        assertEquals("SUBMIT_REVIEW", context.getActionCode());
        assertEquals("DRAFT", context.getObjectState());
        assertEquals(99L, context.getApplicantUserId());
        assertEquals(List.of(88L), context.getStartUserSelectAssignees().get("approveNode"));
        assertEquals(execution.getId(), context.getVariables().get("edhrExecutionId"));
        assertEquals(execution.getExecutionCode(), context.getVariables().get("edhrExecutionCode"));
        assertEquals(DOMAIN_TRACE_HASH, context.getVariables().get("domainTraceHash"));
        assertTrue(String.valueOf(context.getVariables().get("edhrReviewSignatureCells")).contains("R1C1"));
        assertNotNull(context.getVariables().get("approvalSnapshotHash"));
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
    }

    @Test
    void submitBatchRecordExecution_singleCandidateStillRequiresExplicitSelection() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-T7-SINGLE-MISSING", "BATCH-T7-SINGLE-MISSING"));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                            .setId(execution.getId())
                            .setWorkTaskId(8001L)
                            .setPassword("secret")
                            .setComment("提交审批")));
            assertEquals(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("R1C1"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void submitBatchRecordExecution_usesMultiUserReviewSourceAsBpmCandidatePoolAndCreatesAssignedReviewTask() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-USERS", "BATCH-SUBMIT-USERS");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(multiUserReviewExecutionSnapshotJson())
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        Task task = mockTask("task-submit-users", "process-submit-users", "approveNode", "审批", "89");
        when(adminUserApi.getUserList(any())).thenReturn(List.of(
                enabledReviewUser(88L, "审核人A"),
                enabledReviewUser(89L, "审核人B")));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1201L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-users");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-users", null, "approveNode"))
                .thenReturn(List.of(task));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(89L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(eq(99L), bpmCaptor.capture());
        assertEquals(List.of(89L), bpmCaptor.getValue().getStartUserSelectAssignees().get("approveNode"));
        assertTrue(String.valueOf(bpmCaptor.getValue().getVariables().get("edhrReviewSignatureCells"))
                .contains("\"reviewSourceIds\":[88,89]"));

        ArgumentCaptor<List> reviewTasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(execution.getId()), reviewTasksCaptor.capture());
        List<MesProEdhrReviewTaskCreateCommand> reviewTaskCommands = reviewTasksCaptor.getValue();
        assertEquals(1, reviewTaskCommands.size());
        assertEquals(89L, reviewTaskCommands.get(0).getAssigneeUserId());
        assertEquals("USERS", reviewTaskCommands.get(0).getReviewSourceType());
        assertEquals("USER", reviewTaskCommands.get(0).getCandidateSourceType());
        assertNull(reviewTaskCommands.get(0).getCandidateSourceId());
        assertEquals("88,89", reviewTaskCommands.get(0).getCandidateUserSnapshot());
        assertEquals("task-submit-users", reviewTaskCommands.get(0).getBpmTaskId());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        JSONObject snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        JSONObject assignment = snapshotJson.getJSONArray("reviewAssignments").getJSONObject(0);
        assertEquals(89L, assignment.getLong("assigneeUserId"));
        assertEquals("审核人B", assignment.getString("assigneeUserName"));
    }

    @Test
    void submitBatchRecordExecution_multiCandidateMissingSelectionFailsBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-T7-MISSING", "BATCH-T7-MISSING");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(multiUserReviewExecutionSnapshotJson())
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        when(adminUserApi.getUserList(any())).thenReturn(List.of(
                enabledReviewUser(88L, "审核人A"),
                enabledReviewUser(89L, "审核人B")));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                            .setId(execution.getId())
                            .setWorkTaskId(8001L)
                            .setPassword("secret")
                            .setComment("提交审批")));
            assertEquals(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("R1C1"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    void submitBatchRecordExecution_selectedAssigneeOutsideCandidatePoolFailsBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-T7-INVALID", "BATCH-T7-INVALID");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(multiUserReviewExecutionSnapshotJson())
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        when(adminUserApi.getUserList(any())).thenReturn(List.of(
                enabledReviewUser(88L, "审核人A"),
                enabledReviewUser(89L, "审核人B")));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                            .setId(execution.getId())
                            .setWorkTaskId(8001L)
                            .setPassword("secret")
                            .setComment("提交审批")
                            .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                                    .setSignatureCellKey("R1C1")
                                    .setSelectedUserId(188L)))));
            assertEquals(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("R1C1:188"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    void submitBatchRecordExecution_unknownSelectionCellFailsBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-T7-UNKNOWN", "BATCH-T7-UNKNOWN");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(multiUserReviewExecutionSnapshotJson())
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        when(adminUserApi.getUserList(any())).thenReturn(List.of(
                enabledReviewUser(88L, "审核人A"),
                enabledReviewUser(89L, "审核人B")));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                            .setId(execution.getId())
                            .setWorkTaskId(8001L)
                            .setPassword("secret")
                            .setComment("提交审批")
                            .setReviewAssigneeSelections(List.of(
                                    new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                                            .setSignatureCellKey("R1C1")
                                            .setSelectedUserId(89L),
                                    new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                                            .setSignatureCellKey("R9C9")
                                            .setSelectedUserId(89L)))));
            assertEquals(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("R9C9"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void submitBatchRecordExecution_roleReviewSourceCreatesRoleCandidateSnapshotCommand() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-ROLE", "BATCH-SUBMIT-ROLE");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(reviewSourceExecutionSnapshotJson("ROLE", 7001L, "QA 角色"))
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        when(permissionApi.getUserRoleIdListByRoleIds(Set.of(7001L))).thenReturn(Set.of(88L, 89L));
        when(adminUserApi.getUserList(Set.of(88L, 89L))).thenReturn(List.of(
                enabledReviewUser(88L, "审核人A"),
                enabledReviewUser(89L, "审核人B")));
        Task firstTask = mockTask("task-submit-role-t5-a", "process-submit-role-t5", "approveNode", "审批", "88");
        Task secondTask = mockTask("task-submit-role-t5-b", "process-submit-role-t5", "approveNode", "审批", null);
        when(secondTask.getTaskLocalVariables()).thenReturn(Map.of("approveNode_assignee", 89L));
        Task flowableCandidateTask = mockTask("task-submit-role-t5-candidate", "process-submit-role-t5",
                "approveNode", "审批", null);
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1301L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-role-t5");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-role-t5", null, "approveNode"))
                .thenReturn(List.of(firstTask, secondTask, flowableCandidateTask));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(89L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(eq(99L), bpmCaptor.capture());
        assertEquals(List.of(89L), bpmCaptor.getValue().getStartUserSelectAssignees().get("approveNode"));

        ArgumentCaptor<List> reviewTasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(execution.getId()), reviewTasksCaptor.capture());
        List<MesProEdhrReviewTaskCreateCommand> commands =
                (List<MesProEdhrReviewTaskCreateCommand>) reviewTasksCaptor.getValue();
        assertEquals(1, commands.size());
        assertEquals(List.of(89L), commands.stream()
                .map(MesProEdhrReviewTaskCreateCommand::getAssigneeUserId)
                .toList());
        assertEquals(List.of("task-submit-role-t5-b"), commands.stream()
                .map(MesProEdhrReviewTaskCreateCommand::getBpmTaskId)
                .toList());
        assertTrue(commands.stream().allMatch(command -> "ROLE".equals(command.getReviewSourceType())));
        assertTrue(commands.stream().allMatch(command -> Long.valueOf(7001L).equals(command.getReviewSourceId())));
        assertTrue(commands.stream().allMatch(command -> "ROLE_GROUP".equals(command.getCandidateSourceType())));
        assertTrue(commands.stream().allMatch(command -> Long.valueOf(7001L).equals(command.getCandidateSourceId())));
        assertTrue(commands.stream().allMatch(command -> "88,89".equals(command.getCandidateUserSnapshot())));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void submitBatchRecordExecution_processFormSignatureRuleOverridesSnapshotReviewSource() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-PERMISSION-RULE", "BATCH-SUBMIT-PERMISSION-RULE");
        attachDefaultApprovalContext(execution.getId());
        execution = executionMapper.selectById(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(reviewSourceExecutionSnapshotJson("ROLE", 7001L, "旧快照角色"))
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(execution.getRouteProcessId())
                .setBatchRecordReportId(execution.getBatchRecordReportId())
                .setRuleType("SIGNATURE")
                .setSignatureCellKey("R1C1")
                .setSignatureRole("APPROVAL")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds("188,189")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(120)
                .setEnabled(true)
                .setRemark("测试签名位规则覆盖快照来源"));
        when(adminUserApi.getUserList(Set.of(188L, 189L))).thenReturn(List.of(
                enabledReviewUser(188L, "配置审批人A"),
                enabledReviewUser(189L, "配置审批人B")));
        Task task = mockTask("task-submit-permission-rule", "process-submit-permission-rule",
                "approveNode", "审批", "189");
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1351L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-permission-rule");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-permission-rule", null, "approveNode"))
                .thenReturn(List.of(task));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(189L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(eq(99L), bpmCaptor.capture());
        assertEquals(List.of(189L), bpmCaptor.getValue().getStartUserSelectAssignees().get("approveNode"));

        ArgumentCaptor<List> reviewTasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(execution.getId()), reviewTasksCaptor.capture());
        List<MesProEdhrReviewTaskCreateCommand> commands =
                (List<MesProEdhrReviewTaskCreateCommand>) reviewTasksCaptor.getValue();
        assertEquals(1, commands.size());
        assertEquals("USERS", commands.get(0).getReviewSourceType());
        assertEquals(null, commands.get(0).getReviewSourceId());
        assertEquals("USER", commands.get(0).getCandidateSourceType());
        assertEquals(null, commands.get(0).getCandidateSourceId());
        assertEquals("188,189", commands.get(0).getCandidateUserSnapshot());
        assertEquals(189L, commands.get(0).getAssigneeUserId());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void submitBatchRecordExecution_deptReviewSourceCreatesDeptCandidateSnapshotCommand() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-DEPT", "BATCH-SUBMIT-DEPT");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(reviewSourceExecutionSnapshotJson("DEPT", 8001L, "QA 部门"))
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        when(adminUserApi.getUserListByDeptIds(Set.of(8001L))).thenReturn(List.of(
                enabledReviewUser(88L, "审核人A"),
                enabledReviewUser(89L, "审核人B")));
        Task task = mockTask("task-submit-dept-t5", "process-submit-dept-t5", "approveNode", "审批", "89");
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1302L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-dept-t5");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-dept-t5", null, "approveNode"))
                .thenReturn(List.of(task));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(89L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<List> reviewTasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(execution.getId()), reviewTasksCaptor.capture());
        MesProEdhrReviewTaskCreateCommand command =
                (MesProEdhrReviewTaskCreateCommand) reviewTasksCaptor.getValue().get(0);
        assertEquals("DEPT", command.getReviewSourceType());
        assertEquals(8001L, command.getReviewSourceId());
        assertEquals("DEPT_GROUP", command.getCandidateSourceType());
        assertEquals(8001L, command.getCandidateSourceId());
        assertEquals("88,89", command.getCandidateUserSnapshot());
        assertEquals(89L, command.getAssigneeUserId());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void submitBatchRecordExecution_usesDeptReviewSourceAsCandidatePoolSnapshot() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-SUBMIT-DEPT", "BATCH-SUBMIT-DEPT");
        attachDefaultApprovalContext(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(deptReviewExecutionSnapshotJson())
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        Task task = mockTask("task-submit-dept", "process-submit-dept", "approveNode", "审批", "188");
        when(adminUserApi.getUserListByDeptIds(any())).thenReturn(List.of(
                enabledReviewUser(188L, "部门审核人A"),
                enabledReviewUser(189L, "部门审核人B")));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1301L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-dept");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-dept", null, "approveNode"))
                .thenReturn(List.of(task));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(188L))));
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> bpmCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processInstanceApi).createProcessInstance(eq(99L), bpmCaptor.capture());
        assertEquals(List.of(188L), bpmCaptor.getValue().getStartUserSelectAssignees().get("approveNode"));

        ArgumentCaptor<List> reviewTasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(execution.getId()), reviewTasksCaptor.capture());
        List<MesProEdhrReviewTaskCreateCommand> reviewTaskCommands = reviewTasksCaptor.getValue();
        assertEquals(1, reviewTaskCommands.size());
        assertEquals("DEPT", reviewTaskCommands.get(0).getReviewSourceType());
        assertEquals("DEPT_GROUP", reviewTaskCommands.get(0).getCandidateSourceType());
        assertEquals(8001L, reviewTaskCommands.get(0).getCandidateSourceId());
        assertEquals("188,189", reviewTaskCommands.get(0).getCandidateUserSnapshot());
        assertEquals(188L, reviewTaskCommands.get(0).getAssigneeUserId());
    }

    @Test
    void submitBatchRecordExecution_missingRequiredField_failsBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-REQUIRED-MISSING", "BATCH-REQUIRED-MISSING"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldExecutionSnapshotJson())
                .setCellValuesJson("[]")
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]")));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> executionService.submitBatchRecordExecution(
                            new MesProBatchRecordExecutionSubmitReqVO()
                                    .setId(execution.getId())
                                    .setWorkTaskId(8001L)
                                    .setPassword("secret")
                                    .setComment("提交审批")));
            assertEquals(PRO_BATCH_RECORD_EXECUTION_REQUIRED_FIELD_MISSING.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("操作温度"));
            assertTrue(exception.getMessage().contains("第 5 行第 6 列"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        MesProBatchRecordExecutionDO draft = executionMapper.selectById(execution.getId());
        assertEquals(0, draft.getStatus());
        assertNull(draft.getProcessInstanceId());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    void submitBatchRecordExecution_goldenFingerBypassesAssigneeRequiredFieldsAndRecordsAudit() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-GF-SUBMIT", "BATCH-GF-SUBMIT"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldExecutionSnapshotJson())
                .setCellValuesJson("[]")
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]")));
        grantGoldenFingerPermission(99L);
        when(workTaskService.validateGoldenFingerFillTaskForExecution(8001L, execution.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(8001L)
                        .setExecutionId(execution.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                        .setStatus(MesProEdhrWorkTaskStatus.TODO)
                        .setAssigneeUserId(188L));
        when(workTaskService.completeFillAndCreateNextFillAfterGoldenFingerSubmit(8001L, execution.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(8001L));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "金手指提交"))
                .thenReturn(9701L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("admin");
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("金手指提交"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(workTaskService).validateGoldenFingerFillTaskForExecution(8001L, execution.getId());
        verify(workTaskService).completeFillAndCreateNextFillAfterGoldenFingerSubmit(8001L, execution.getId());
        verify(workTaskService, never()).validateWritableFillTaskForExecution(8001L, execution.getId());
        verify(domainTraceService, never()).verifyForSubmit(anyLong());
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        verify(operationAuditService).record(argThat(audit ->
                "GOLDEN_FINGER_SUBMIT".equals(audit.getOperationType())
                        && MesProEdhrGoldenFingerPermissionService.PERMISSION.equals(audit.getPermissionCode())
                        && "ALLOW_GOLDEN_FINGER".equals(audit.getPermissionDecision())
                        && audit.getMetadataJson() != null
                        && audit.getMetadataJson().contains("ASSIGNEE")
                        && audit.getMetadataJson().contains("REQUIRED_FIELDS")));
        MesProBatchRecordExecutionDO completed = executionMapper.selectById(execution.getId());
        assertEquals(4, completed.getStatus());
        assertEquals(99L, completed.getSubmittedBy());
        assertNull(completed.getProcessInstanceId());
    }

    @Test
    void submitBatchRecordExecution_goldenFingerStillRejectsTerminalExecution() {
        MesProBatchRecordExecutionDO execution = insertExecution(2, "BRE-GF-LOCKED", "BATCH-GF-LOCKED");
        grantGoldenFingerPermission(99L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertServiceException(() -> executionService.submitBatchRecordExecution(
                            new MesProBatchRecordExecutionSubmitReqVO()
                                    .setId(execution.getId())
                                    .setWorkTaskId(8001L)
                                    .setPassword("secret")
                                    .setComment("金手指提交")),
                    PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(operationAuditService, never()).record(any());
    }

    @Test
    void submitOrdinaryProcessCompletesFillWithoutCreatingReviewOrBpm() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(0, "BRE-SUBMIT-ORDINARY", "BATCH-SUBMIT-ORDINARY");
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":37.5,\"valueDisplay\":\"37.5\"}]";
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldOrdinaryExecutionSnapshotJson())
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson)));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交填写"))
                .thenReturn(1401L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交填写"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "提交填写");
        verify(executionSignatureService)
                .bindSignatureFieldAuditEvidence(1401L, execution.getId(),
                        0L, MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                        MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson));
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        verify(workTaskService, never()).createReviewTasks(anyLong(), anyLong(), any());
        verify(workTaskService).completeFillAndCreateNextFillAfterOrdinarySubmit(8001L, execution.getId());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));

        MesProBatchRecordExecutionDO completed = executionMapper.selectById(execution.getId());
        assertEquals(4, completed.getStatus());
        assertEquals(99L, completed.getSubmittedBy());
        assertNotNull(completed.getSubmittedAt());
        assertNull(completed.getApprovedBy());
        assertNull(completed.getApprovedAt());
        assertNotNull(completed.getClosedAt());
        assertNull(completed.getProcessInstanceId());
    }

    @Test
    void submitOrdinaryProcessResubmitBeforeReleaseUpdatesSignatureWithoutCreatingNextTaskAgain() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(4, "BRE-SUBMIT-ORDINARY-REDO", "BATCH-SUBMIT-ORDINARY-REDO");
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":38.1,\"valueDisplay\":\"38.1\"}]";
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldOrdinaryExecutionSnapshotJson())
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson))
                .setSubmittedBy(77L)
                .setSubmittedAt(LocalDateTime.of(2026, 7, 20, 8, 30))
                .setClosedAt(LocalDateTime.of(2026, 7, 20, 8, 35)));
        insertPreReleaseBatchTask(execution, 91001L, 92001L,
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "重新提交填写"))
                .thenReturn(1501L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("重新提交填写"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "重新提交填写");
        verify(executionSignatureService)
                .bindSignatureFieldAuditEvidence(1501L, execution.getId(),
                        0L, MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                        MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson));
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        verify(workTaskService, never()).createReviewTasks(anyLong(), anyLong(), any());
        verify(workTaskService, never()).completeFillAndCreateNextFillAfterOrdinarySubmit(anyLong(), anyLong());

        MesProBatchRecordExecutionDO updated = executionMapper.selectById(execution.getId());
        assertEquals(4, updated.getStatus());
        assertEquals(99L, updated.getSubmittedBy());
        assertNotNull(updated.getSubmittedAt());
        assertNotEquals(LocalDateTime.of(2026, 7, 20, 8, 30), updated.getSubmittedAt());
        assertNull(updated.getProcessInstanceId());
    }

    @Test
    void submitOrdinaryProcessResubmitRejectsWhenReleasePendingApproval() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(4, "BRE-SUBMIT-ORDINARY-RELEASE-LOCK", "BATCH-SUBMIT-ORDINARY-RELEASE-LOCK");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldOrdinaryExecutionSnapshotJson()));
        Long batchExecutionId = 93001L;
        insertPreReleaseBatchTask(execution, batchExecutionId, 94001L,
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-LOCK")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertServiceException(() -> executionService.submitBatchRecordExecution(
                            new MesProBatchRecordExecutionSubmitReqVO()
                                    .setId(execution.getId())
                                    .setWorkTaskId(8001L)
                                    .setPassword("secret")
                                    .setComment("审批中修改")),
                    PRO_EDHR_RELEASE_STATUS_INVALID);
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService, never()).recordSubmitSignature(anyLong(), any(), any());
        verify(workTaskService, never()).completeFillAndCreateNextFillAfterOrdinarySubmit(anyLong(), anyLong());
    }

    @Test
    void submitBatchRecordExecution_goldenFingerBypassesPendingReleaseLockOnResubmit() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(4, "BRE-GF-RELEASE-LOCK-BYPASS", "BATCH-GF-RELEASE-LOCK-BYPASS");
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":38.1,\"valueDisplay\":\"38.1\"}]";
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldOrdinaryExecutionSnapshotJson())
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson))
                .setSubmittedBy(77L)
                .setSubmittedAt(LocalDateTime.of(2026, 7, 20, 8, 30))
                .setClosedAt(LocalDateTime.of(2026, 7, 20, 8, 35)));
        Long batchExecutionId = 95001L;
        insertPreReleaseBatchTask(execution, batchExecutionId, 96001L,
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-GF-LOCK")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));
        grantGoldenFingerPermission(99L);
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "金手指审批中修改"))
                .thenReturn(9501L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("admin");
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("金手指审批中修改"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "金手指审批中修改");
        verify(workTaskService, never()).completeFillAndCreateNextFillAfterGoldenFingerSubmit(anyLong(), anyLong());
        MesProBatchRecordExecutionDO updated = executionMapper.selectById(execution.getId());
        assertEquals(4, updated.getStatus());
        assertEquals(99L, updated.getSubmittedBy());
    }

    @Test
    void submitWithValidatedFillWorkTaskDoesNotRequireExecutionScopeSign() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(0, "BRE-SUBMIT-WORK-TASK-SIGN", "BATCH-SUBMIT-WORK-TASK-SIGN");
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":37.5,\"valueDisplay\":\"37.5\"}]";
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldOrdinaryExecutionSnapshotJson())
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson)));
        when(workTaskService.validateWritableFillTaskForExecution(8001L, execution.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(8001L)
                        .setExecutionId(execution.getId())
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                        .setStatus("TODO"));
        doThrow(new IllegalStateException("execution scope SIGN should not be required for an assigned fill task"))
                .when(permissionGateService)
                .requireAbility(argThat(command ->
                        "BATCH_RECORD_EXECUTION".equals(command.getObjectType())
                                && String.valueOf(execution.getId()).equals(command.getObjectId())
                                && "SIGN".equals(command.getAbility())));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交填写"))
                .thenReturn(1404L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交填写"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(workTaskService).validateWritableFillTaskForExecution(8001L, execution.getId());
        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "提交填写");
        verify(workTaskService).completeFillAndCreateNextFillAfterOrdinarySubmit(8001L, execution.getId());
        MesProBatchRecordExecutionDO completed = executionMapper.selectById(execution.getId());
        assertEquals(4, completed.getStatus());
        assertEquals(99L, completed.getSubmittedBy());
        assertNull(completed.getProcessInstanceId());
    }

    @Test
    void submitBatchExecutionSlotFormWithReviewSignatureCompletesFillWithoutCreatingReviewOrBpm() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-SLOT-DIRECT", "BATCH-SUBMIT-SLOT-DIRECT"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setFormSlotType("MAIN")
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRouteBindingId(91001L)
                .setRouteBindingSnapshotHash("slot-direct-route-binding-hash"));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交填写"))
                .thenReturn(1403L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交填写"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "提交填写");
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        verify(workTaskService, never()).createReviewTasks(anyLong(), anyLong(), any());
        verify(workTaskService).completeFillAndCreateNextFillAfterOrdinarySubmit(8001L, execution.getId());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));

        MesProBatchRecordExecutionDO completed = executionMapper.selectById(execution.getId());
        assertEquals(4, completed.getStatus());
        assertEquals(99L, completed.getSubmittedBy());
        assertNotNull(completed.getSubmittedAt());
        assertNull(completed.getProcessInstanceId());
    }

    @Test
    void submitOrdinaryProcessIgnoresReleaseApprovalSignatureCell() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(0, "BRE-SUBMIT-RELEASE-CELL", "BATCH-SUBMIT-RELEASE-CELL");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(releaseApprovalSignatureExecutionSnapshotJson())
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE"));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交填写"))
                .thenReturn(1402L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交填写"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "提交填写");
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        verify(workTaskService, never()).createReviewTasks(anyLong(), anyLong(), any());
        verify(workTaskService).completeFillAndCreateNextFillAfterOrdinarySubmit(8001L, execution.getId());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));

        MesProBatchRecordExecutionDO completed = executionMapper.selectById(execution.getId());
        assertEquals(4, completed.getStatus());
        assertEquals(99L, completed.getSubmittedBy());
        assertNotNull(completed.getSubmittedAt());
        assertNull(completed.getApprovedBy());
        assertNull(completed.getApprovedAt());
        assertNotNull(completed.getClosedAt());
        assertNull(completed.getProcessInstanceId());
    }

    @Test
    void submitControlledBatchRecordIgnoresLegacyProcessReviewSignatureCell() {
        MesProBatchRecordExecutionDO execution =
                insertExecution(0, "BRE-SUBMIT-M4-CONTROLLED", "BATCH-M4-CONTROLLED");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(reviewExecutionSnapshotJson())
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH"));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交填写"))
                .thenReturn(1403L);

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交填写"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "提交填写");
        verify(processInstanceApi, never()).createProcessInstance(anyLong(), any(BpmProcessInstanceCreateReqDTO.class));
        verify(workTaskService, never()).createReviewTasks(anyLong(), anyLong(), any());
        verify(workTaskService).completeFillAndCreateNextFillAfterOrdinarySubmit(8001L, execution.getId());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));

        MesProBatchRecordExecutionDO completed = executionMapper.selectById(execution.getId());
        assertEquals(4, completed.getStatus());
        assertEquals(99L, completed.getSubmittedBy());
        assertNotNull(completed.getSubmittedAt());
        assertNull(completed.getApprovedBy());
        assertNull(completed.getApprovedAt());
        assertNotNull(completed.getClosedAt());
        assertNull(completed.getProcessInstanceId());
    }

    @Test
    void submitBatchRecordExecution_requiredFieldsCompleted_startsBpm() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-REQUIRED-OK", "BATCH-REQUIRED-OK"));
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":37.5,\"valueDisplay\":\"37.5\"}]";
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredFieldExecutionSnapshotJson())
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson)));
        Task task = mockTask("task-submit-required-ok", "process-submit-required-ok", "approveNode", "审批");
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1301L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-required-ok");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-required-ok", null, "approveNode"))
                .thenReturn(List.of(task));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(88L))));
        } finally {
            TenantContextHolder.clear();
        }

        verify(executionSignatureService).recordSubmitSignature(execution.getId(), "secret", "提交审批");
        verify(processInstanceApi).createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class));
        MesProBatchRecordExecutionDO submitted = executionMapper.selectById(execution.getId());
        assertEquals(1, submitted.getStatus());
        assertEquals("process-submit-required-ok", submitted.getProcessInstanceId());
    }

    @Test
    void submitBatchRecordExecution_requiredBooleanFalseAndNumberZeroAreValid() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-REQUIRED-ZERO", "BATCH-REQUIRED-ZERO"));
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":0,\"valueDisplay\":\"0\"},{\"rowIndex\":6,\"columnIndex\":2,"
                + "\"valueType\":\"BOOLEAN\",\"value\":false,\"valueDisplay\":\"否\"}]";
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(requiredNumberAndBooleanFieldExecutionSnapshotJson())
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson)));
        Task task = mockTask("task-submit-required-zero", "process-submit-required-zero", "approveNode", "审批");
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1302L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-required-zero");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-required-zero", null, "approveNode"))
                .thenReturn(List.of(task));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                    .setId(execution.getId())
                    .setWorkTaskId(8001L)
                    .setPassword("secret")
                    .setComment("提交审批")
                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                            .setSignatureCellKey("R1C1")
                            .setSelectedUserId(88L))));
        } finally {
            TenantContextHolder.clear();
        }

        MesProBatchRecordExecutionDO submitted = executionMapper.selectById(execution.getId());
        assertEquals(1, submitted.getStatus());
        assertEquals("process-submit-required-zero", submitted.getProcessInstanceId());
    }

    @Test
    void submitBatchRecordExecution_afterBpmCreatedAttachSignatureFailure_cancelsBpmAndKeepsLocalDraft() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-COMPENSATE", "BATCH-SUBMIT-COMPENSATE"));
        Task task = mockTask("task-submit-compensate", "process-submit-compensate", "approveNode", "审批");
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1201L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-compensate");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-compensate", null, "approveNode"))
                .thenReturn(List.of(task));
        doThrow(new IllegalStateException("attach submit signature failed"))
                .when(executionSignatureService)
                .attachSubmitSignatureProcessInstance(1201L, execution.getId(), "process-submit-compensate");

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
        assertThrows(RuntimeException.class, () -> executionService.submitBatchRecordExecution(
                new MesProBatchRecordExecutionSubmitReqVO()
                        .setId(execution.getId())
                        .setWorkTaskId(8001L)
                        .setPassword("secret")
                        .setComment("提交审批")
                        .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                                .setSignatureCellKey("R1C1")
                                .setSelectedUserId(88L)))));
        } finally {
            TenantContextHolder.clear();
        }

        verify(processInstanceApi).cancelProcessInstance(99L, "process-submit-compensate",
                "eDHR submit compensation: executionId=" + execution.getId());
        MesProBatchRecordExecutionDO draft = executionMapper.selectById(execution.getId());
        assertEquals(0, draft.getStatus());
        assertNull(draft.getProcessInstanceId());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    void submitBatchRecordExecution_afterBpmCreatedCurrentTaskMissing_cancelsBpmAndKeepsLocalDraft() {
        MesProBatchRecordExecutionDO execution = attachReviewSignatureSnapshot(
                insertExecution(0, "BRE-SUBMIT-NO-TASK", "BATCH-SUBMIT-NO-TASK"));
        when(executionSignatureService.recordSubmitSignature(execution.getId(), "secret", "提交审批")).thenReturn(1202L);
        when(processInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-submit-no-task");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-submit-no-task", null, "approveNode"))
                .thenReturn(List.of());

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> executionService.submitBatchRecordExecution(
                            new MesProBatchRecordExecutionSubmitReqVO()
                                    .setId(execution.getId())
                                    .setWorkTaskId(8001L)
                                    .setPassword("secret")
                                    .setComment("提交审批")
                                    .setReviewAssigneeSelections(List.of(new MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection()
                                            .setSignatureCellKey("R1C1")
                                            .setSelectedUserId(88L)))));
            assertEquals(PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("processInstanceId=process-submit-no-task"));
            assertTrue(exception.getMessage().contains("tasks=empty"));
        } finally {
            TenantContextHolder.clear();
        }

        verify(processInstanceApi).cancelProcessInstance(99L, "process-submit-no-task",
                "eDHR submit compensation: executionId=" + execution.getId());
        assertEquals(0, executionMapper.selectById(execution.getId()).getStatus());
        assertNull(approvalSnapshotMapper.selectByExecutionId(execution.getId()));
    }

    @Test
    void submitBatchRecordExecution_missingExecutionSnapshot_failsFastWithoutSubmitSignatureOrBpm() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-MISSING-SNAPSHOT", "BATCH-MISSING-SNAPSHOT");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionSnapshotJson(" "));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
        assertServiceException(() -> executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                        .setId(execution.getId())
                        .setWorkTaskId(8001L)
                        .setPassword("secret")
                        .setComment("提交审批")),
                PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }

        verify(executionSignatureService, never()).recordSubmitSignature(any(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(any(), any());
    }

    @Test
    void submitBatchRecordExecution_missingCellValues_failsFastWithoutSubmitSignatureOrBpm() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-MISSING-CELLS", "BATCH-MISSING-CELLS");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setCellValuesJson(" "));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            assertServiceException(() -> executionService.submitBatchRecordExecution(new MesProBatchRecordExecutionSubmitReqVO()
                            .setId(execution.getId())
                            .setWorkTaskId(8001L)
                            .setPassword("secret")
                            .setComment("提交审批")),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }

        verify(executionSignatureService, never()).recordSubmitSignature(any(), any(), any());
        verify(processInstanceApi, never()).createProcessInstance(any(), any());
    }

    @Test
    void cosignBatchRecordExecution_activeExecutionRecordsFormReviewSignatureWithCurrentAuditEvidence() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-COSIGN-001", "BATCH-COSIGN-001");
        when(workTaskService.validateWritableFillTaskForExecution(7001L, execution.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(7001L));
        when(executionSignatureService.recordFormReviewSignature(eq(execution.getId()), eq("review-secret"), eq("复核无异常"),
                eq(execution.getFieldAuditRevision()), eq(execution.getFieldAuditHeadHash()), eq(execution.getCellValuesHash()),
                any(MesProBatchRecordExecutionSignatureTimeCommand.class)))
                .thenReturn(3101L);
        LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 11, 20);

        MesProBatchRecordExecutionFormReviewSignRespVO response =
                executionService.cosignBatchRecordExecution(new MesProBatchRecordExecutionFormReviewSignReqVO()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(7001L)
                        .setPassword("review-secret")
                        .setComment("复核无异常")
                        .setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                                .setSelectedSignedAt(selectedSignedAt)
                                .setSelectedTimeZone("Asia/Shanghai")
                                .setSelectedTimeReason("复核人确认纸面时间")));

        assertEquals(execution.getId(), response.getExecutionId());
        assertEquals(0, response.getStatus());
        assertEquals(3101L, response.getSignatureId());
        assertEquals("FORM_REVIEW", response.getActionType());
        assertEquals("表单复核", response.getMeaningText());
        assertEquals(execution.getCellValuesHash(), response.getCellValuesHash());
        assertEquals(execution.getFieldAuditRevision(), response.getFieldAuditRevision());
        assertEquals(execution.getFieldAuditHeadHash(), response.getFieldAuditHeadHash());
        ArgumentCaptor<MesProBatchRecordExecutionSignatureTimeCommand> timeCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureTimeCommand.class);
        InOrder inOrder = inOrder(workTaskService, executionSignatureService);
        inOrder.verify(workTaskService).validateWritableFillTaskForExecution(7001L, execution.getId());
        inOrder.verify(executionSignatureService).recordFormReviewSignature(eq(execution.getId()), eq("review-secret"), eq("复核无异常"),
                eq(execution.getFieldAuditRevision()), eq(execution.getFieldAuditHeadHash()), eq(execution.getCellValuesHash()),
                timeCaptor.capture());
        assertEquals(selectedSignedAt, timeCaptor.getValue().getSelectedSignedAt());
        assertEquals("Asia/Shanghai", timeCaptor.getValue().getSelectedTimeZone());
        assertEquals("复核人确认纸面时间", timeCaptor.getValue().getSelectedTimeReason());
        verify(permissionGateService).requireAbility(argThat(command ->
                "BATCH_RECORD_EXECUTION".equals(command.getObjectType())
                        && String.valueOf(execution.getId()).equals(command.getObjectId())
                        && "SIGN".equals(command.getAbility())
                        && execution.getId().equals(command.getExecutionId())
                        && "mes:pro-batch-record-execution:cosign"
                        .equals(command.getPermissionCode())));
        verify(processInstanceApi, never()).createProcessInstance(any(), any());
    }

    @Test
    void cosignBatchRecordExecution_missingWorkTaskIdFailsFastWithoutSignature() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-COSIGN-MISSING-TASK", "BATCH-COSIGN-MISSING-TASK");
        when(workTaskService.validateWritableFillTaskForExecution(isNull(), eq(execution.getId())))
                .thenThrow(new ServiceException(PRO_EDHR_WORK_TASK_NOT_EXISTS));

        assertServiceException(() -> executionService.cosignBatchRecordExecution(
                        new MesProBatchRecordExecutionFormReviewSignReqVO()
                                .setExecutionId(execution.getId())
                                .setPassword("review-secret")
                                .setComment("missing task")),
                PRO_EDHR_WORK_TASK_NOT_EXISTS);

        verify(executionSignatureService, never()).recordFormReviewSignature(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cosignBatchRecordExecution_wrongWorkTaskIdFailsFastWithoutSignature() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-COSIGN-WRONG-TASK", "BATCH-COSIGN-WRONG-TASK");
        when(workTaskService.validateWritableFillTaskForExecution(7002L, execution.getId()))
                .thenThrow(new ServiceException(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID));

        assertServiceException(() -> executionService.cosignBatchRecordExecution(
                        new MesProBatchRecordExecutionFormReviewSignReqVO()
                                .setExecutionId(execution.getId())
                                .setWorkTaskId(7002L)
                                .setPassword("review-secret")
                                .setComment("wrong task")),
                PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID);

        verify(executionSignatureService, never()).recordFormReviewSignature(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cosignBatchRecordExecution_closedExecutionFailsFastWithoutSignature() {
        MesProBatchRecordExecutionDO execution = insertExecution(3, "BRE-COSIGN-CLOSED", "BATCH-COSIGN-CLOSED");

        assertServiceException(() -> executionService.cosignBatchRecordExecution(
                        new MesProBatchRecordExecutionFormReviewSignReqVO()
                                .setExecutionId(execution.getId())
                                .setPassword("review-secret")
                                .setComment("closed review")),
                PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        verify(executionSignatureService, never()).recordFormReviewSignature(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveBatchRecordExecution_matchingTask_closesExecutionAndReleasesActiveContext() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-approve", "task-approve");
        Task task = mockTask("task-approve", "process-approve", "approveNode", "审批");
        MesProBatchRecordExecutionApproveReqVO reqVO = approveReqForApproveTask(execution, "task-approve");
        when(bpmTaskService.validateTask(88L, "task-approve")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(2101L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            executionService.approveBatchRecordExecution(reqVO
                    .setPassword("secret")
                    .setComment("同意")
                    .setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                            .setSelectedSignedAt(LocalDateTime.of(2026, 6, 15, 12, 10))
                            .setSelectedTimeZone("Asia/Shanghai")
                            .setSelectedTimeReason("审批人确认签名显示时间")));
        }

        ArgumentCaptor<MesProBatchRecordExecutionApprovalSignatureCommand> signatureCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionApprovalSignatureCommand.class);
        InOrder inOrder = inOrder(bpmTaskService, domainTraceService, executionSignatureService);
        inOrder.verify(bpmTaskService).validateTask(88L, "task-approve");
        inOrder.verify(domainTraceService).verifyForApproval(execution.getId(), DOMAIN_TRACE_HASH);
        inOrder.verify(executionSignatureService).recordApprovalSignature(signatureCaptor.capture());
        inOrder.verify(bpmTaskService).approveTask(eq(88L), any(BpmTaskApproveReqVO.class));
        assertEquals("APPROVE", signatureCaptor.getValue().getApprovalResult());
        assertEquals("task-approve", signatureCaptor.getValue().getBpmTaskId());
        assertEquals("review-cell", signatureCaptor.getValue().getSignatureCellKey());
        assertEquals("POST", signatureCaptor.getValue().getReviewSourceType());
        assertEquals(7001L, signatureCaptor.getValue().getReviewSourceId());
        assertEquals(execution.getCellValuesHash(), signatureCaptor.getValue().getCellValuesHash());
        assertEquals(execution.getFieldAuditRevision(), signatureCaptor.getValue().getFieldAuditRevision());
        assertEquals(execution.getFieldAuditHeadHash(), signatureCaptor.getValue().getFieldAuditHeadHash());
        assertEquals(LocalDateTime.of(2026, 6, 15, 12, 10),
                signatureCaptor.getValue().getSignatureTimeCommand().getSelectedSignedAt());
        assertEquals("Asia/Shanghai", signatureCaptor.getValue().getSignatureTimeCommand().getSelectedTimeZone());
        assertEquals("审批人确认签名显示时间",
                signatureCaptor.getValue().getSignatureTimeCommand().getSelectedTimeReason());
        verify(permissionGateService).requireAbility(argThat(command ->
                "BATCH_RECORD_EXECUTION".equals(command.getObjectType())
                        && String.valueOf(execution.getId()).equals(command.getObjectId())
                        && "APPROVE".equals(command.getAbility())
                        && execution.getId().equals(command.getExecutionId())
                        && "mes:pro-batch-record-execution:approve"
                        .equals(command.getPermissionCode())));

        MesProBatchRecordExecutionDO approved = executionMapper.selectById(execution.getId());
        assertEquals(3, approved.getStatus());
        assertEquals(88L, approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());
        assertNotNull(approved.getClosedAt());
        assertNull(approved.getActiveContextKey());

        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("APPROVED", snapshot.getApprovalStatus());
        assertEquals(2101L, snapshot.getApproveSignatureId());
        verify(workTaskService).createNextFillAfterReview(any(MesProEdhrWorkTaskDO.class));
        verify(workTaskService).completeApproveTask(reqVO.getWorkTaskId(), execution.getId());
    }

    @Test
    void approveBatchRecordExecution_waitsForOtherReviewTasksBeforeClosingExecution() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-approve-partial", "task-approve-a");
        Task task = mockTask("task-approve-a", "process-approve-partial", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-approve-a")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(2151L);
        when(workTaskService.hasActiveReviewTasks(execution.getId())).thenReturn(true);

        MesProBatchRecordExecutionApprovalActionRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            response = executionService.approveBatchRecordExecution(approveReq(execution, "task-approve-a")
                    .setPassword("secret")
                    .setComment("同意"));
        }

        assertEquals(1, response.getStatus());
        assertEquals(MesProEdhrApprovalStatusMapping.ACTION_RESULT_REVIEW_INTERMEDIATE, response.getResultType());
        assertEquals(2151L, response.getSignatureId());
        ArgumentCaptor<MesProBatchRecordExecutionApprovalSignatureCommand> signatureCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionApprovalSignatureCommand.class);
        verify(executionSignatureService).recordApprovalSignature(signatureCaptor.capture());
        assertEquals("REVIEW_APPROVE", signatureCaptor.getValue().getApprovalResult());
        verify(bpmTaskService, never()).approveTask(eq(88L), any(BpmTaskApproveReqVO.class));
        verify(workTaskService, never()).createApproveTaskAfterReview(any(MesProEdhrWorkTaskDO.class));
        verify(workTaskService, never()).createNextFillAfterReview(any(MesProEdhrWorkTaskDO.class));
        MesProBatchRecordExecutionDO stillSubmitted = executionMapper.selectById(execution.getId());
        assertEquals(1, stillSubmitted.getStatus());
        assertNull(stillSubmitted.getApprovedAt());
        assertNull(stillSubmitted.getClosedAt());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        assertNull(snapshot.getApproveSignatureId());
    }

    @Test
    void approveBatchRecordExecution_reviewTaskCreatesApproveTaskAndKeepsExecutionSubmitted() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-review-to-approve", "task-review-to-approve");
        Task task = mockTask("task-review-to-approve", "process-review-to-approve", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-review-to-approve")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(2161L);
        when(workTaskService.hasActiveReviewTasks(execution.getId())).thenReturn(false);
        MesProEdhrWorkTaskDO approveTask = new MesProEdhrWorkTaskDO().setId(6202L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE);
        when(workTaskService.createApproveTaskAfterReview(any(MesProEdhrWorkTaskDO.class))).thenReturn(approveTask);

        MesProBatchRecordExecutionApprovalActionRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            response = executionService.approveBatchRecordExecution(approveReq(execution, "task-review-to-approve")
                    .setPassword("secret")
                    .setComment("审核通过"));
        }

        assertEquals(1, response.getStatus());
        assertEquals(MesProEdhrApprovalStatusMapping.ACTION_RESULT_REVIEW_TO_APPROVE, response.getResultType());
        assertEquals(2161L, response.getSignatureId());
        assertEquals(6202L, response.getApproveTaskId());
        ArgumentCaptor<MesProBatchRecordExecutionApprovalSignatureCommand> signatureCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionApprovalSignatureCommand.class);
        verify(executionSignatureService).recordApprovalSignature(signatureCaptor.capture());
        assertEquals("REVIEW_APPROVE", signatureCaptor.getValue().getApprovalResult());
        verify(permissionGateService).requireAbility(argThat(command ->
                "REVIEW".equals(command.getAbility())
                        && execution.getId().equals(command.getExecutionId())
                        && "mes:pro-batch-record-execution:approve".equals(command.getPermissionCode())));
        verify(bpmTaskService, never()).approveTask(eq(88L), any(BpmTaskApproveReqVO.class));
        verify(workTaskService).createApproveTaskAfterReview(any(MesProEdhrWorkTaskDO.class));
        verify(workTaskService, never()).createNextFillAfterReview(any(MesProEdhrWorkTaskDO.class));
        MesProBatchRecordExecutionDO stillSubmitted = executionMapper.selectById(execution.getId());
        assertEquals(1, stillSubmitted.getStatus());
        assertNull(stillSubmitted.getApprovedAt());
        assertNull(stillSubmitted.getClosedAt());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        assertNull(snapshot.getApproveSignatureId());
    }

    @Test
    void approveBatchRecordExecution_bpmApproveFailure_doesNotPersistLocalClosureFields() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-approve-fail", "task-approve-fail");
        Task task = mockTask("task-approve-fail", "process-approve-fail", "approveNode", "审批");
        MesProBatchRecordExecutionApproveReqVO reqVO = approveReqForApproveTask(execution, "task-approve-fail");
        when(bpmTaskService.validateTask(88L, "task-approve-fail")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(2201L);
        doThrow(new IllegalStateException("bpm approve failed"))
                .when(bpmTaskService).approveTask(eq(88L), any(BpmTaskApproveReqVO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertThrows(RuntimeException.class, () -> executionService.approveBatchRecordExecution(
                    reqVO
                            .setPassword("secret")
                            .setComment("同意")));
        }

        InOrder inOrder = inOrder(bpmTaskService, domainTraceService, executionSignatureService);
        inOrder.verify(bpmTaskService).validateTask(88L, "task-approve-fail");
        inOrder.verify(domainTraceService).verifyForApproval(execution.getId(), DOMAIN_TRACE_HASH);
        inOrder.verify(executionSignatureService)
                .recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        inOrder.verify(bpmTaskService).approveTask(eq(88L), any(BpmTaskApproveReqVO.class));
        MesProBatchRecordExecutionDO stillSubmitted = executionMapper.selectById(execution.getId());
        assertEquals(1, stillSubmitted.getStatus());
        assertNull(stillSubmitted.getApprovedAt());
        assertNull(stillSubmitted.getClosedAt());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        assertNull(snapshot.getApproveSignatureId());
        assertNull(snapshot.getClosedAt());
        verify(workTaskService).completeApproveTask(reqVO.getWorkTaskId(), execution.getId());
    }

    @Test
    void approveBatchRecordExecution_rejectsWhenFieldAuditEvidenceChangedAfterSubmitSnapshot() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-field-audit-stale", "task-field-audit-stale");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setFieldAuditHeadHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        Task task = mockTask("task-field-audit-stale", "process-field-audit-stale", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-field-audit-stale")).thenReturn(task);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.approveBatchRecordExecution(
                            approveReq(execution, "task-field-audit-stale")
                                    .setPassword("secret")
                                    .setComment("同意")),
                    PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
        }

        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(bpmTaskService, never()).approveTask(any(Long.class), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveBatchRecordExecution_missingDomainTraceHashInSnapshot_failsFastBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-domain-trace-missing", "task-domain-trace-missing");
        removeDomainTraceHashFromApprovalSnapshot(execution);
        Task task = mockTask("task-domain-trace-missing", "process-domain-trace-missing", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-domain-trace-missing")).thenReturn(task);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.approveBatchRecordExecution(
                            approveReq(execution, "task-domain-trace-missing")
                                    .setPassword("secret")
                                    .setComment("同意")),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }

        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(domainTraceService, never()).verifyForApproval(anyLong(), any());
        verify(bpmTaskService, never()).validateTask(any(Long.class), eq("task-domain-trace-missing"));
        verify(bpmTaskService, never()).approveTask(any(Long.class), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveBatchRecordExecution_changedDomainTraceAfterSnapshot_failsFastBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-domain-trace-changed", "task-domain-trace-changed");
        Task task = mockTask("task-domain-trace-changed", "process-domain-trace-changed", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-domain-trace-changed")).thenReturn(task);
        doThrow(new IllegalStateException("domain trace changed"))
                .when(domainTraceService).verifyForApproval(execution.getId(), DOMAIN_TRACE_HASH);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertThrows(RuntimeException.class, () -> executionService.approveBatchRecordExecution(
                    approveReq(execution, "task-domain-trace-changed")
                            .setPassword("secret")
                            .setComment("同意")));
        }

        MesProBatchRecordExecutionDO stillSubmitted = executionMapper.selectById(execution.getId());
        assertEquals(1, stillSubmitted.getStatus());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(bpmTaskService, never()).approveTask(any(Long.class), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void rejectBatchRecordExecution_matchingTask_keepsRejectReason() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-reject", "task-reject");
        Task task = mockTask("task-reject", "process-reject", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-reject")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(3101L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            executionService.rejectBatchRecordExecution(rejectReq(execution, "task-reject")
                    .setPassword("secret")
                    .setReason("批记录填写不完整")
                    .setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                            .setSelectedSignedAt(LocalDateTime.of(2026, 6, 15, 12, 30))
                            .setSelectedTimeZone("Asia/Shanghai")
                            .setSelectedTimeReason("驳回签名按纸面确认时间显示")));
        }

        ArgumentCaptor<MesProBatchRecordExecutionApprovalSignatureCommand> signatureCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionApprovalSignatureCommand.class);
        InOrder inOrder = inOrder(bpmTaskService, domainTraceService, executionSignatureService);
        inOrder.verify(bpmTaskService).validateTask(88L, "task-reject");
        inOrder.verify(domainTraceService).verifyForApproval(execution.getId(), DOMAIN_TRACE_HASH);
        inOrder.verify(executionSignatureService).recordApprovalSignature(signatureCaptor.capture());
        inOrder.verify(bpmTaskService).rejectTask(eq(88L), any(BpmTaskRejectReqVO.class));
        assertEquals("REJECT", signatureCaptor.getValue().getApprovalResult());
        assertEquals("批记录填写不完整", signatureCaptor.getValue().getReason());
        assertEquals(LocalDateTime.of(2026, 6, 15, 12, 30),
                signatureCaptor.getValue().getSignatureTimeCommand().getSelectedSignedAt());
        assertEquals("Asia/Shanghai", signatureCaptor.getValue().getSignatureTimeCommand().getSelectedTimeZone());
        assertEquals("驳回签名按纸面确认时间显示",
                signatureCaptor.getValue().getSignatureTimeCommand().getSelectedTimeReason());

        MesProBatchRecordExecutionDO rejected = executionMapper.selectById(execution.getId());
        assertEquals(2, rejected.getStatus());
        assertEquals(88L, rejected.getRejectedBy());
        assertNotNull(rejected.getRejectedAt());
        assertEquals("批记录填写不完整", rejected.getRejectReason());

        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("REJECTED", snapshot.getApprovalStatus());
        assertEquals(3101L, snapshot.getRejectSignatureId());
    }

    @Test
    void rejectBatchRecordExecution_matchingTask_createsControlledRevisionAndReworkTask() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-reject-revision", "task-reject-revision");
        MesProBatchRecordApprovalSnapshotDO beforeSnapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        Task task = mockTask("task-reject-revision", "process-reject-revision", "approveNode", "审批");
        stubReviewWorkTask(901L, execution, "task-reject-revision");
        when(bpmTaskService.validateTask(88L, "task-reject-revision")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(3151L);
        when(workTaskService.completeReviewAndCreateRework(eq(901L), eq(execution.getId()), anyLong(), eq("批记录填写不完整")))
                .thenReturn(new MesProEdhrWorkTaskDO().setId(6101L));

        MesProBatchRecordExecutionApprovalActionRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            response = executionService.rejectBatchRecordExecution(rejectReq(execution, "task-reject-revision")
                    .setWorkTaskId(901L)
                    .setPassword("secret")
                    .setReason("批记录填写不完整"));
        }

        assertEquals(execution.getId(), response.getExecutionId());
        assertEquals(2, response.getStatus());
        assertNotNull(response.getRevisionExecutionId());
        assertEquals(6101L, response.getReworkTaskId());

        MesProBatchRecordExecutionDO rejected = executionMapper.selectById(execution.getId());
        MesProBatchRecordExecutionDO revision = executionMapper.selectById(response.getRevisionExecutionId());
        assertEquals(2, rejected.getStatus());
        assertEquals(revision.getId(), rejected.getSupersededByExecutionId());
        assertEquals(false, rejected.getActiveRevisionFlag());
        assertNull(rejected.getActiveContextKey());

        assertEquals(0, revision.getStatus());
        assertEquals(execution.getId(), revision.getRevisionRootExecutionId());
        assertEquals(2, revision.getRevisionNo());
        assertEquals(execution.getId(), revision.getSourceRejectedExecutionId());
        assertEquals("批记录填写不完整", revision.getRevisionReason());
        assertEquals(beforeSnapshot.getSnapshotHash(), revision.getRevisionParentHash());
        assertEquals(true, revision.getActiveRevisionFlag());
        assertEquals(execution.getWorkOrderId(), revision.getWorkOrderId());
        assertEquals(execution.getRouteProcessId(), revision.getRouteProcessId());
        assertNull(revision.getTaskId());
        assertNull(revision.getWorkstationId());
        assertEquals(execution.getBatchRecordReportId(), revision.getBatchRecordReportId());
        assertEquals(execution.getBatchCode(), revision.getBatchCode());
        assertEquals(execution.getExecutionSnapshotJson(), revision.getExecutionSnapshotJson());
        assertEquals(execution.getCellValuesJson(), revision.getCellValuesJson());
        assertEquals(execution.getCellValuesHash(), revision.getCellValuesHash());
        assertEquals(0L, revision.getFieldAuditRevision());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH, revision.getFieldAuditHeadHash());
        assertNull(revision.getFieldAuditLastBatchId());
        assertEquals(existingContextKey(revision.getWorkOrderId(), revision.getTaskId(), revision.getRouteProcessId(),
                revision.getWorkstationId(), revision.getBatchRecordReportId(), revision.getBatchCode()), revision.getActiveContextKey());
        verify(workTaskService).requireReworkAssigneeUserId(901L, execution.getId());
        verify(workTaskService).completeReviewAndCreateRework(901L, execution.getId(), revision.getId(), "批记录填写不完整");
    }

    @Test
    void rejectBatchRecordExecution_bpmRejectFailure_doesNotPersistLocalRejectFields() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-reject-fail", "task-reject-fail");
        Task task = mockTask("task-reject-fail", "process-reject-fail", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-reject-fail")).thenReturn(task);
        when(executionSignatureService.recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class)))
                .thenReturn(3201L);
        doThrow(new IllegalStateException("bpm reject failed"))
                .when(bpmTaskService).rejectTask(eq(88L), any(BpmTaskRejectReqVO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertThrows(RuntimeException.class, () -> executionService.rejectBatchRecordExecution(
                    rejectReq(execution, "task-reject-fail")
                            .setPassword("secret")
                            .setReason("批记录填写不完整")));
        }

        InOrder inOrder = inOrder(bpmTaskService, domainTraceService, executionSignatureService);
        inOrder.verify(bpmTaskService).validateTask(88L, "task-reject-fail");
        inOrder.verify(domainTraceService).verifyForApproval(execution.getId(), DOMAIN_TRACE_HASH);
        inOrder.verify(executionSignatureService)
                .recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        inOrder.verify(bpmTaskService).rejectTask(eq(88L), any(BpmTaskRejectReqVO.class));
        MesProBatchRecordExecutionDO stillSubmitted = executionMapper.selectById(execution.getId());
        assertEquals(1, stillSubmitted.getStatus());
        assertNull(stillSubmitted.getRejectedAt());
        assertNull(stillSubmitted.getRejectReason());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        assertNull(snapshot.getRejectSignatureId());
        assertNull(snapshot.getRejectedAt());
        assertNull(snapshot.getRejectReason());
    }

    @Test
    void rejectBatchRecordExecution_missingDomainTraceHashInSnapshot_failsFastBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-domain-trace-reject-missing", "task-domain-trace-reject-missing");
        removeDomainTraceHashFromApprovalSnapshot(execution);
        Task task = mockTask("task-domain-trace-reject-missing", "process-domain-trace-reject-missing",
                "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-domain-trace-reject-missing")).thenReturn(task);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.rejectBatchRecordExecution(
                            rejectReq(execution, "task-domain-trace-reject-missing")
                                    .setPassword("secret")
                                    .setReason("批记录填写不完整")),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }

        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(domainTraceService, never()).verifyForApproval(anyLong(), any());
        verify(bpmTaskService, never()).validateTask(any(Long.class), eq("task-domain-trace-reject-missing"));
        verify(bpmTaskService, never()).rejectTask(any(Long.class), any(BpmTaskRejectReqVO.class));
    }

    @Test
    void rejectBatchRecordExecution_changedDomainTraceAfterSnapshot_failsFastBeforeSignatureAndBpm() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-domain-trace-reject-changed", "task-domain-trace-reject-changed");
        Task task = mockTask("task-domain-trace-reject-changed", "process-domain-trace-reject-changed",
                "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-domain-trace-reject-changed")).thenReturn(task);
        doThrow(new IllegalStateException("domain trace changed"))
                .when(domainTraceService).verifyForApproval(execution.getId(), DOMAIN_TRACE_HASH);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertThrows(RuntimeException.class, () -> executionService.rejectBatchRecordExecution(
                    rejectReq(execution, "task-domain-trace-reject-changed")
                            .setPassword("secret")
                            .setReason("批记录填写不完整")));
        }

        MesProBatchRecordExecutionDO stillSubmitted = executionMapper.selectById(execution.getId());
        assertEquals(1, stillSubmitted.getStatus());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals("SUBMITTED", snapshot.getApprovalStatus());
        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(bpmTaskService, never()).rejectTask(any(Long.class), any(BpmTaskRejectReqVO.class));
    }

    @Test
    void approveBatchRecordExecution_taskProcessInstanceMismatch_failsFastWithoutSideEffects() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-right", "task-right");
        Task task = mockTask("task-wrong", "process-wrong", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-wrong")).thenReturn(task);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.approveBatchRecordExecution(approveReq(execution, "task-wrong")
                            .setPassword("secret")
                            .setComment("同意")),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
        }

        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(bpmTaskService, never()).approveTask(any(Long.class), any(BpmTaskApproveReqVO.class));
        assertEquals(1, executionMapper.selectById(execution.getId()).getStatus());
    }

    @Test
    void approveBatchRecordExecution_sameProcessButNotSnapshotCurrentTask_failsFastWithoutSideEffects() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-current", "task-current");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.approveBatchRecordExecution(approveReq(execution, "task-old")
                            .setPassword("secret")
                            .setComment("同意")),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
        }

        verify(bpmTaskService, never()).validateTask(88L, "task-old");
        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(bpmTaskService, never()).approveTask(any(Long.class), any(BpmTaskApproveReqVO.class));
        assertEquals(1, executionMapper.selectById(execution.getId()).getStatus());
    }

    @Test
    void approvalPendingPage_missingSnapshot_failsFastInsteadOfFiltering() {
        Task task = mockTask("task-pending-missing", "process-missing-snapshot", "approveNode", "审批");
        when(bpmTaskService.getTaskTodoPage(eq(88L), any())).thenReturn(new PageResult<>(List.of(task), 1L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.getApprovalPendingPage(pageReq()),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
    }

    @Test
    void approvalPendingPage_missingExecution_failsFastInsteadOfFiltering() {
        approvalSnapshotMapper.insert(MesProBatchRecordApprovalSnapshotDO.builder()
                .executionId(999999L)
                .processDefinitionKey("mes-edhr-approval-v1")
                .processInstanceId("process-missing-execution")
                .approvalStatus("SUBMITTED")
                .snapshotJson("{}")
                .snapshotHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .currentBpmTaskId("task-pending")
                .currentTaskDefinitionKey("approveNode")
                .build());
        Task task = mockTask("task-pending", "process-missing-execution", "approveNode", "审批");
        when(bpmTaskService.getTaskTodoPage(eq(88L), any())).thenReturn(new PageResult<>(List.of(task), 1L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.getApprovalPendingPage(pageReq()),
                    PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
    }

    @Test
    void approvalDonePage_missingSnapshot_failsFastInsteadOfFiltering() {
        HistoricTaskInstance task = mockHistoricTask("task-done-missing", "process-done-missing-snapshot", "approveNode", "审批");
        when(bpmTaskService.getTaskDonePage(eq(88L), any())).thenReturn(new PageResult<>(List.of(task), 1L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.getApprovalDonePage(pageReq()),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
    }

    @Test
    void approvalDetail_submittedExecutionReturnsContractAndValidatesCurrentTask() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-detail", "task-detail");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"value\":\"OK\"}]"));
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(execution.getId())
                .actorId(77L)
                .actorName("提交人")
                .actionType("SUBMIT")
                .signatureMode("PASSWORD")
                .passwordVerified(true)
                .comment("提交")
                .signedAt(LocalDateTime.of(2026, 5, 26, 9, 30))
                .processInstanceId("process-detail")
                .bpmTaskId("task-detail")
                .bpmTaskDefinitionKey("approveNode")
                .bpmTaskName("审批")
                .build());
        MesProEdhrWorkTaskDO reviewTask = new MesProEdhrWorkTaskDO()
                .setId(9001L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setExecutionId(execution.getId())
                .setBpmTaskId("task-detail")
                .setSignatureCellKey("R19C18-REVIEW-A")
                .setSignatureRowIndex(19)
                .setSignatureColumnIndex(18)
                .setReviewSourceType("POST")
                .setReviewSourceId(7001L)
                .setReviewSourceName("QA 岗");
        when(workTaskService.getAssignedReviewOrApproveTaskForDetail(9001L, execution.getId())).thenReturn(reviewTask);
        Task task = mockTask("task-detail", "process-detail", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-detail")).thenReturn(task);

        MesProBatchRecordExecutionApprovalRespVO detail;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            detail = executionService.getApprovalDetail(execution.getId(), null, 9001L);
        }

        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        assertEquals(execution.getId(), detail.getExecutionId());
        assertEquals("BRE-process-detail", detail.getExecutionCode());
        assertEquals("process-detail", detail.getProcessInstanceId());
        assertEquals("task-detail", detail.getBpmTaskId());
        assertEquals("审批", detail.getBpmTaskName());
        assertEquals("approveNode", detail.getBpmTaskDefinitionKey());
        assertEquals("焊接", detail.getProcessName());
        assertEquals("焊接工位", detail.getWorkstationName());
        assertEquals("{\"from\":\"test\"}", detail.getExecutionSnapshotJson());
        assertEquals(1, detail.getCellValues().size());
        assertEquals("OK", detail.getCellValues().get(0).getValue());
        assertEquals(snapshot.getId(), detail.getApprovalSnapshotId());
        assertEquals(snapshot.getSnapshotHash(), detail.getApprovalSnapshotHash());
        assertEquals("SUBMITTED", detail.getApprovalSnapshotStatus());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, detail.getTaskType());
        assertEquals(9001L, detail.getWorkTaskId());
        assertEquals(Boolean.TRUE, detail.getCanApprove());
        assertEquals(Boolean.TRUE, detail.getCanReject());
        assertEquals(1, detail.getSignatureSummaries().size());
    }

    @Test
    void approvalDetail_mismatchedTask_failsFastWithoutApproving() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-detail-mismatch", "task-current");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.getApprovalDetail(execution.getId(), "task-other"),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
        }

        verify(bpmTaskService, never()).validateTask(anyLong(), any());
        verify(executionSignatureService, never()).recordApprovalSignature(any(MesProBatchRecordExecutionApprovalSignatureCommand.class));
        verify(bpmTaskService, never()).approveTask(any(Long.class), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approvalDetail_completedReviewWorkTaskOpensReadonlyAfterApproval() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-detail-done", "task-done");
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        approvalSnapshotMapper.updateById(new MesProBatchRecordApprovalSnapshotDO()
                .setId(snapshot.getId())
                .setApprovalStatus("APPROVED")
                .setApprovedBy(88L)
                .setApprovedAt(LocalDateTime.of(2026, 5, 26, 12, 0))
                .setClosedAt(LocalDateTime.of(2026, 5, 26, 12, 0)));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(3)
                .setClosedAt(LocalDateTime.of(2026, 5, 26, 12, 0)));
        MesProEdhrWorkTaskDO reviewTask = new MesProEdhrWorkTaskDO()
                .setId(9100L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setExecutionId(execution.getId())
                .setBpmTaskId("task-done")
                .setSignatureCellKey("R19C18-REVIEW-A")
                .setSignatureRowIndex(19)
                .setSignatureColumnIndex(18)
                .setReviewSourceType("ROLE")
                .setReviewSourceId(910212L)
                .setReviewSourceName("eDHR矩阵-审批人");
        when(workTaskService.getAssignedReviewOrApproveTaskForDetail(9100L, execution.getId())).thenReturn(reviewTask);

        MesProBatchRecordExecutionApprovalRespVO detail;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            detail = executionService.getApprovalDetail(execution.getId(), null, 9100L);
        }

        assertEquals(Boolean.FALSE, detail.getCanApprove());
        assertEquals(Boolean.FALSE, detail.getCanReject());
        assertEquals("APPROVED", detail.getApprovalSnapshotStatus());
        assertEquals("R19C18-REVIEW-A", detail.getSignatureCellKey());
        assertEquals(19, detail.getSignatureRowIndex());
        assertEquals(18, detail.getSignatureColumnIndex());
        verify(workTaskService, never()).validateWritableTask(anyLong(), anyLong(), any());
        verify(bpmTaskService, never()).validateTask(anyLong(), any());
    }

    @Test
    void approvalDetail_approveWorkTaskOpensWritableApprovalDetail() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-detail-approve", "task-approve");
        MesProEdhrWorkTaskDO approveTask = new MesProEdhrWorkTaskDO()
                .setId(9200L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE)
                .setExecutionId(execution.getId())
                .setBpmTaskId("task-approve")
                .setSignatureCellKey("R19C18-APPROVE-A")
                .setSignatureRowIndex(19)
                .setSignatureColumnIndex(18)
                .setReviewSourceType("ROLE_GROUP")
                .setReviewSourceId(910212L)
                .setReviewSourceName("eDHR矩阵-批准人");
        when(workTaskService.getAssignedReviewOrApproveTaskForDetail(9200L, execution.getId())).thenReturn(approveTask);
        Task task = mockTask("task-approve", "process-detail-approve", "approveNode", "审批");
        when(bpmTaskService.validateTask(88L, "task-approve")).thenReturn(task);

        MesProBatchRecordExecutionApprovalRespVO detail;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            detail = executionService.getApprovalDetail(execution.getId(), null, 9200L);
        }

        assertEquals(Boolean.TRUE, detail.getCanApprove());
        assertEquals(Boolean.FALSE, detail.getCanReject());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE, detail.getTaskType());
        assertEquals(9200L, detail.getWorkTaskId());
        assertEquals("R19C18-APPROVE-A", detail.getSignatureCellKey());
        assertEquals("task-approve", detail.getBpmTaskId());
        verify(workTaskService, never()).getAssignedTaskForDetail(anyLong(), anyLong(), eq(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW));
    }

    @Test
    void approvalDetail_reviewWorkTaskKeepsRejectCapabilityAlignedWithActionEntry() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-detail-review", "task-review");
        MesProEdhrWorkTaskDO reviewTask = new MesProEdhrWorkTaskDO()
                .setId(9300L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setExecutionId(execution.getId())
                .setBpmTaskId("task-review")
                .setSignatureCellKey("R19C18-REVIEW-B")
                .setSignatureRowIndex(19)
                .setSignatureColumnIndex(18)
                .setReviewSourceType("ROLE")
                .setReviewSourceId(910213L)
                .setReviewSourceName("eDHR矩阵-审核人");
        when(workTaskService.getAssignedReviewOrApproveTaskForDetail(9300L, execution.getId())).thenReturn(reviewTask);
        Task task = mockTask("task-review", "process-detail-review", "approveNode", "审核");
        when(bpmTaskService.validateTask(88L, "task-review")).thenReturn(task);

        MesProBatchRecordExecutionApprovalRespVO detail;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            detail = executionService.getApprovalDetail(execution.getId(), null, 9300L);
        }

        assertEquals(Boolean.TRUE, detail.getCanApprove());
        assertEquals(Boolean.TRUE, detail.getCanReject());
        assertEquals(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW, detail.getTaskType());
        assertEquals(9300L, detail.getWorkTaskId());
        assertEquals("R19C18-REVIEW-B", detail.getSignatureCellKey());
        assertEquals("task-review", detail.getBpmTaskId());
    }

    @Test
    void approvalDetail_missingTaskId_failsFastWithoutDefaultingToSnapshotCurrentTask() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot("process-detail-required", "task-required");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            assertServiceException(() -> executionService.getApprovalDetail(execution.getId(), null),
                    PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
        }

        verify(bpmTaskService, never()).validateTask(88L, "task-required");
    }

    @Test
    void approvalPendingPage_appliesExecutionSubmittedByAndSubmittedAtFilters() {
        MesProBatchRecordExecutionDO matched = insertSubmittedExecutionWithSnapshot("process-filter-match", "task-filter-match");
        MesProBatchRecordExecutionDO ignoredByUser = insertSubmittedExecutionWithSnapshot("process-filter-user", "task-filter-user");
        MesProBatchRecordExecutionDO ignoredByTime = insertSubmittedExecutionWithSnapshot("process-filter-time", "task-filter-time");
        LocalDateTime matchedSubmittedAt = LocalDateTime.of(2026, 5, 26, 10, 0);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(matched.getId())
                .setSubmittedBy(77L)
                .setSubmittedAt(matchedSubmittedAt));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(ignoredByUser.getId())
                .setSubmittedBy(88L)
                .setSubmittedAt(matchedSubmittedAt));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(ignoredByTime.getId())
                .setSubmittedBy(77L)
                .setSubmittedAt(LocalDateTime.of(2026, 5, 20, 10, 0)));
        Task matchedTask = mockTask("task-filter-match", "process-filter-match", "approveNode", "审批");
        Task ignoredByUserTask = mockTask("task-filter-user", "process-filter-user", "approveNode", "审批");
        Task ignoredByTimeTask = mockTask("task-filter-time", "process-filter-time", "approveNode", "审批");
        when(bpmTaskService.getTaskTodoPage(eq(66L), any()))
                .thenReturn(new PageResult<>(List.of(matchedTask, ignoredByUserTask, ignoredByTimeTask), 3L));

        MesProBatchRecordExecutionApprovalPageReqVO reqVO = pageReq();
        reqVO.setExecutionCode("match");
        reqVO.setWorkOrderCode(matched.getWorkOrderCode());
        reqVO.setBatchCode("BATCH-process-filter-match");
        reqVO.setSubmittedBy(77L);
        reqVO.setSubmittedAtStart(LocalDateTime.of(2026, 5, 26, 0, 0));
        reqVO.setSubmittedAtEnd(LocalDateTime.of(2026, 5, 26, 23, 59));

        PageResult<MesProBatchRecordExecutionApprovalRespVO> page;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            page = executionService.getApprovalPendingPage(reqVO);
        }

        assertEquals(1L, page.getTotal());
        assertEquals(matched.getId(), page.getList().get(0).getExecutionId());
        assertEquals("task-filter-match", page.getList().get(0).getBpmTaskId());
    }

    @Test
    void approvalPendingPage_filtersFullBpmResultBeforePaginatingAndKeepsTotalStable() {
        MesProBatchRecordExecutionDO ignored = insertSubmittedExecutionWithSnapshot(
                "process-filter-page-ignored", "task-filter-page-ignored");
        MesProBatchRecordExecutionDO firstMatched = insertSubmittedExecutionWithSnapshot(
                "process-filter-page-match-1", "task-filter-page-match-1");
        MesProBatchRecordExecutionDO secondMatched = insertSubmittedExecutionWithSnapshot(
                "process-filter-page-match-2", "task-filter-page-match-2");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(ignored.getId())
                .setBatchCode("BATCH-OTHER"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(firstMatched.getId())
                .setBatchCode("BATCH-MATCH-PAGE"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(secondMatched.getId())
                .setBatchCode("BATCH-MATCH-PAGE"));
        Task ignoredTask = mockTask("task-filter-page-ignored", "process-filter-page-ignored", "approveNode", "Approval");
        Task firstMatchedTask = mockTask("task-filter-page-match-1", "process-filter-page-match-1", "approveNode", "Approval");
        Task secondMatchedTask = mockTask("task-filter-page-match-2", "process-filter-page-match-2", "approveNode", "Approval");
        when(bpmTaskService.getTaskTodoPage(eq(66L), any(BpmTaskPageReqVO.class)))
                .thenAnswer(invocation -> {
                    BpmTaskPageReqVO bpmReqVO = invocation.getArgument(1);
                    if (bpmReqVO.getPageNo() == 1 && bpmReqVO.getPageSize() >= 3) {
                        return new PageResult<>(List.of(ignoredTask, firstMatchedTask, secondMatchedTask), 3L);
                    }
                    if (bpmReqVO.getPageNo() == 2 && bpmReqVO.getPageSize() == 1) {
                        return new PageResult<>(List.of(firstMatchedTask), 3L);
                    }
                    return new PageResult<>(List.of(), 3L);
                });

        MesProBatchRecordExecutionApprovalPageReqVO reqVO = pageReq();
        reqVO.setPageNo(2);
        reqVO.setPageSize(1);
        reqVO.setBatchCode("MATCH-PAGE");

        PageResult<MesProBatchRecordExecutionApprovalRespVO> page;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            page = executionService.getApprovalPendingPage(reqVO);
        }

        assertEquals(2L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(secondMatched.getId(), page.getList().get(0).getExecutionId());
        assertEquals("task-filter-page-match-2", page.getList().get(0).getBpmTaskId());
    }

    @Test
    void approvalDonePage_filtersFullBpmResultBeforePaginatingAndKeepsTotalStable() {
        MesProBatchRecordExecutionDO ignored = insertSubmittedExecutionWithSnapshot(
                "process-done-filter-page-ignored", "task-done-filter-page-ignored");
        MesProBatchRecordExecutionDO firstMatched = insertSubmittedExecutionWithSnapshot(
                "process-done-filter-page-match-1", "task-done-filter-page-match-1");
        MesProBatchRecordExecutionDO secondMatched = insertSubmittedExecutionWithSnapshot(
                "process-done-filter-page-match-2", "task-done-filter-page-match-2");
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(ignored.getId())
                .setBatchCode("BATCH-DONE-OTHER"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(firstMatched.getId())
                .setBatchCode("BATCH-DONE-MATCH-PAGE"));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(secondMatched.getId())
                .setBatchCode("BATCH-DONE-MATCH-PAGE"));
        HistoricTaskInstance ignoredTask = mockHistoricTask("task-done-filter-page-ignored",
                "process-done-filter-page-ignored", "approveNode", "Approval");
        HistoricTaskInstance firstMatchedTask = mockHistoricTask("task-done-filter-page-match-1",
                "process-done-filter-page-match-1", "approveNode", "Approval");
        HistoricTaskInstance secondMatchedTask = mockHistoricTask("task-done-filter-page-match-2",
                "process-done-filter-page-match-2", "approveNode", "Approval");
        when(bpmTaskService.getTaskDonePage(eq(66L), any(BpmTaskPageReqVO.class)))
                .thenAnswer(invocation -> {
                    BpmTaskPageReqVO bpmReqVO = invocation.getArgument(1);
                    if (bpmReqVO.getPageNo() == 1 && bpmReqVO.getPageSize() >= 3) {
                        return new PageResult<>(List.of(ignoredTask, firstMatchedTask, secondMatchedTask), 3L);
                    }
                    if (bpmReqVO.getPageNo() == 2 && bpmReqVO.getPageSize() == 1) {
                        return new PageResult<>(List.of(firstMatchedTask), 3L);
                    }
                    return new PageResult<>(List.of(), 3L);
                });

        MesProBatchRecordExecutionApprovalPageReqVO reqVO = pageReq();
        reqVO.setPageNo(2);
        reqVO.setPageSize(1);
        reqVO.setBatchCode("DONE-MATCH-PAGE");

        PageResult<MesProBatchRecordExecutionApprovalRespVO> page;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            page = executionService.getApprovalDonePage(reqVO);
        }

        assertEquals(2L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(secondMatched.getId(), page.getList().get(0).getExecutionId());
        assertEquals("task-done-filter-page-match-2", page.getList().get(0).getBpmTaskId());
    }

    @Test
    void approvalPendingPage_fillsReviewWorkTaskContextForCurrentBpmTask() {
        MesProBatchRecordExecutionDO execution = insertSubmittedExecutionWithSnapshot(
                "process-pending-context", "task-pending-context");
        Task task = mockTask("task-pending-context", "process-pending-context", "approveNode", "审批");
        MesProEdhrWorkTaskDO reviewTask = new MesProEdhrWorkTaskDO()
                .setId(91001L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setExecutionId(execution.getId())
                .setBpmTaskId("task-pending-context")
                .setSignatureCellKey("R19C18-REVIEW-A")
                .setSignatureRowIndex(19)
                .setSignatureColumnIndex(18)
                .setReviewSourceType("ROLE")
                .setReviewSourceId(910212L)
                .setReviewSourceName("eDHR审核-审批人");
        when(bpmTaskService.getTaskTodoPage(eq(88L), any()))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(workTaskService.getActiveReviewTaskByBpmTaskId(execution.getId(), "task-pending-context"))
                .thenReturn(reviewTask);

        PageResult<MesProBatchRecordExecutionApprovalRespVO> page;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            page = executionService.getApprovalPendingPage(pageReq());
        }

        MesProBatchRecordExecutionApprovalRespVO row = page.getList().get(0);
        assertEquals(91001L, row.getWorkTaskId());
        assertEquals("R19C18-REVIEW-A", row.getSignatureCellKey());
        assertEquals(19, row.getSignatureRowIndex());
        assertEquals(18, row.getSignatureColumnIndex());
        assertEquals("ROLE", row.getReviewSourceType());
        assertEquals(910212L, row.getReviewSourceId());
        assertEquals("eDHR审核-审批人", row.getReviewSourceName());
    }

    @Test
    void trackingAndSignatureQuery_returnMesOwnedExecutionAndSignatureData() {
        MesProBatchRecordExecutionDO fillExecution = insertExecution(0, "BRE-TRACK-FILL-001", "BATCH-TRACK-FILL");
        MesProBatchRecordExecutionDO execution = insertExecution(3, "BRE-TRACK-001", "BATCH-TRACK-001");
        MesProBatchRecordExecutionDO otherExecution = insertExecution(3, "BRE-OTHER-001", "BATCH-OTHER-001");
        MesProBatchRecordExecutionDO rejectedExecution = insertExecution(2, "BRE-TRACK-REJECT-001", "BATCH-TRACK-REJECT");
        MesWmBatchDO fillBatch = insertBatchForExecution(fillExecution);
        MesWmBatchDO approvedBatch = insertBatchForExecution(execution);
        insertBatchForExecution(otherExecution);
        MesWmBatchDO rejectedBatch = insertBatchForExecution(rejectedExecution);
        LocalDateTime submittedAt = LocalDateTime.of(2026, 5, 26, 9, 0);
        LocalDateTime filledAt = LocalDateTime.of(2026, 5, 26, 9, 30);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 26, 10, 0);
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 5, 26, 11, 0);
        LocalDateTime selectedApprovedAt = LocalDateTime.of(2026, 5, 26, 9, 45);
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(fillExecution.getId())
                .actorId(55L)
                .actorName("填写人")
                .actionType("FORM_REVIEW")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .comment("填写复核")
                .signedAt(filledAt)
                .build());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setProcessInstanceId("process-track")
                .setApprovedBy(88L)
                .setApprovedAt(approvedAt)
                .setClosedAt(approvedAt));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(otherExecution.getId())
                .setProcessInstanceId("process-other")
                .setApprovedBy(99L)
                .setApprovedAt(approvedAt)
                .setClosedAt(approvedAt));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(rejectedExecution.getId())
                .setProcessInstanceId("process-reject-track")
                .setRejectedBy(66L)
                .setRejectedAt(rejectedAt)
                .setRejectReason("E2E-reject-material mismatch"));
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(execution.getId())
                .actorId(77L)
                .actorName("提交人")
                .actionType("SUBMIT")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .processInstanceId("process-track")
                .comment("提交")
                .signedAt(submittedAt)
                .build());
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(execution.getId())
                .actorId(88L)
                .actorName("审批人")
                .actionType("APPROVE")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .processInstanceId("process-track")
                .bpmTaskId("task-track")
                .approvalResult("APPROVE")
                .comment("同意")
                .signedAt(approvedAt)
                .selectedSignedAt(selectedApprovedAt)
                .signatureDisplayAt(selectedApprovedAt)
                .signatureTimeMode("USER_SELECTED")
                .selectedTimeZone("Asia/Shanghai")
                .selectedTimeReason("审批人确认线下复核时间")
                .selectedTimePolicyVersion("EDHR_SIGNATURE_TIME_V1")
                .selectedTimeAuditHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .build());
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(otherExecution.getId())
                .actorId(99L)
                .actorName("其他审批人")
                .actionType("APPROVE")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .processInstanceId("process-other")
                .bpmTaskId("task-other")
                .approvalResult("APPROVE")
                .comment("其他")
                .signedAt(approvedAt.plusMinutes(10))
                .build());
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(rejectedExecution.getId())
                .actorId(77L)
                .actorName("提交人")
                .actionType("SUBMIT")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .processInstanceId("process-reject-track")
                .comment("提交驳回路径")
                .signedAt(submittedAt)
                .build());
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(rejectedExecution.getId())
                .actorId(66L)
                .actorName("驳回审批人")
                .actionType("REJECT")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .processInstanceId("process-reject-track")
                .bpmTaskId("task-reject-track")
                .approvalResult("REJECT")
                .reason("E2E-reject-material mismatch")
                .comment("驳回意见备注")
                .signedAt(rejectedAt)
                .build());

        MesProBatchRecordExecutionTrackingPageReqVO trackingReqVO = new MesProBatchRecordExecutionTrackingPageReqVO();
        trackingReqVO.setPageNo(1);
        trackingReqVO.setPageSize(10);
        trackingReqVO.setExecutionCode("BRE-TRACK");
        trackingReqVO.setBatchCode("BATCH-TRACK");
        trackingReqVO.setProcessId(2002L);
        trackingReqVO.setProcessInstanceId("process-track");
        trackingReqVO.setActorName("审批人");
        PageResult<MesProBatchRecordExecutionTrackingRespVO> trackingPage = executionService.getTrackingPage(trackingReqVO);
        assertEquals(1L, trackingPage.getTotal());
        assertEquals(execution.getId(), trackingPage.getList().get(0).getExecutionId());
        assertEquals("BRE-TRACK-001", trackingPage.getList().get(0).getExecutionCode());
        assertEquals(execution.getWorkOrderId(), trackingPage.getList().get(0).getWorkOrderId());
        assertEquals(approvedBatch.getId(), trackingPage.getList().get(0).getBatchId());
        assertEquals("焊接", trackingPage.getList().get(0).getCurrentNodeName());
        assertEquals(List.of("审批人"), trackingPage.getList().get(0).getCurrentAssigneeNames());
        assertEquals("APPROVE", trackingPage.getList().get(0).getLastEventType());
        assertEquals("同意", trackingPage.getList().get(0).getLastEventReason());

        MesProBatchRecordExecutionTrackingPageReqVO fillTrackingReqVO = new MesProBatchRecordExecutionTrackingPageReqVO();
        fillTrackingReqVO.setPageNo(1);
        fillTrackingReqVO.setPageSize(10);
        fillTrackingReqVO.setExecutionCode("BRE-TRACK-FILL-001");
        PageResult<MesProBatchRecordExecutionTrackingRespVO> fillTrackingPage =
                executionService.getTrackingPage(fillTrackingReqVO);
        assertEquals(1L, fillTrackingPage.getTotal());
        assertEquals(fillExecution.getId(), fillTrackingPage.getList().get(0).getExecutionId());
        assertEquals(fillExecution.getWorkOrderId(), fillTrackingPage.getList().get(0).getWorkOrderId());
        assertEquals(fillBatch.getId(), fillTrackingPage.getList().get(0).getBatchId());
        assertEquals("焊接", fillTrackingPage.getList().get(0).getCurrentNodeName());
        assertEquals(List.of("填写人"), fillTrackingPage.getList().get(0).getCurrentAssigneeNames());
        assertEquals("FORM_REVIEW", fillTrackingPage.getList().get(0).getLastEventType());
        assertEquals("填写复核", fillTrackingPage.getList().get(0).getLastEventReason());

        MesProBatchRecordExecutionTrackingPageReqVO rejectTrackingReqVO = new MesProBatchRecordExecutionTrackingPageReqVO();
        rejectTrackingReqVO.setPageNo(1);
        rejectTrackingReqVO.setPageSize(10);
        rejectTrackingReqVO.setExecutionCode("BRE-TRACK-REJECT-001");
        rejectTrackingReqVO.setProcessInstanceId("process-reject-track");
        PageResult<MesProBatchRecordExecutionTrackingRespVO> rejectTrackingPage =
                executionService.getTrackingPage(rejectTrackingReqVO);
        assertEquals(1L, rejectTrackingPage.getTotal());
        assertEquals(rejectedExecution.getId(), rejectTrackingPage.getList().get(0).getExecutionId());
        assertEquals(rejectedExecution.getWorkOrderId(), rejectTrackingPage.getList().get(0).getWorkOrderId());
        assertEquals(rejectedBatch.getId(), rejectTrackingPage.getList().get(0).getBatchId());
        assertEquals("焊接", rejectTrackingPage.getList().get(0).getCurrentNodeName());
        assertEquals(List.of("驳回审批人"), rejectTrackingPage.getList().get(0).getCurrentAssigneeNames());
        assertEquals("REJECT", rejectTrackingPage.getList().get(0).getLastEventType());
        assertEquals("E2E-reject-material mismatch", rejectTrackingPage.getList().get(0).getLastEventReason());

        MesProBatchRecordExecutionSignaturePageReqVO signatureReqVO = new MesProBatchRecordExecutionSignaturePageReqVO();
        signatureReqVO.setPageNo(1);
        signatureReqVO.setPageSize(10);
        signatureReqVO.setExecutionCode("BRE-TRACK");
        signatureReqVO.setActionType("APPROVE");
        signatureReqVO.setActorName("审批人");
        PageResult<MesProBatchRecordExecutionSignatureRespVO> signaturePage = executionService.getSignaturePage(signatureReqVO);
        assertEquals(1L, signaturePage.getTotal());
        assertEquals("APPROVE", signaturePage.getList().get(0).getActionType());
        assertEquals("task-track", signaturePage.getList().get(0).getBpmTaskId());
        assertEquals(execution.getId(), signaturePage.getList().get(0).getExecutionId());
        assertEquals("BRE-TRACK-001", signaturePage.getList().get(0).getExecutionCode());
        assertEquals(selectedApprovedAt, signaturePage.getList().get(0).getSelectedSignedAt());
        assertEquals(selectedApprovedAt, signaturePage.getList().get(0).getSignatureDisplayAt());
        assertEquals("USER_SELECTED", signaturePage.getList().get(0).getSignatureTimeMode());
        assertEquals("Asia/Shanghai", signaturePage.getList().get(0).getSelectedTimeZone());
        assertEquals("审批人确认线下复核时间", signaturePage.getList().get(0).getSelectedTimeReason());
        assertEquals("EDHR_SIGNATURE_TIME_V1", signaturePage.getList().get(0).getSelectedTimePolicyVersion());
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                signaturePage.getList().get(0).getSelectedTimeAuditHash());

        MesProBatchRecordExecutionSignaturePageReqVO submitSignatureReqVO = new MesProBatchRecordExecutionSignaturePageReqVO();
        submitSignatureReqVO.setPageNo(1);
        submitSignatureReqVO.setPageSize(10);
        submitSignatureReqVO.setExecutionCode("BRE-TRACK");
        submitSignatureReqVO.setActionType("SUBMIT");
        submitSignatureReqVO.setProcessInstanceId("process-track");
        PageResult<MesProBatchRecordExecutionSignatureRespVO> submitSignaturePage =
                executionService.getSignaturePage(submitSignatureReqVO);
        assertEquals(1L, submitSignaturePage.getTotal());
        assertEquals("SUBMIT", submitSignaturePage.getList().get(0).getActionType());
        assertEquals("process-track", submitSignaturePage.getList().get(0).getProcessInstanceId());

        List<MesProBatchRecordExecutionTrackingEventRespVO> timeline = executionService.getTrackingTimeline(execution.getId());
        assertEquals(2, timeline.size());
        assertEquals("SUBMIT", timeline.get(0).getEventType());
        assertEquals("ORDINARY_FILL_SIGNATURE", timeline.get(0).getEvidenceCategory());
        assertEquals("普通工序填写提交证据", timeline.get(0).getEvidenceCategoryName());
        assertEquals("process-track", timeline.get(0).getProcessInstanceId());
        assertEquals(submittedAt, timeline.get(0).getOccurredAt());
        assertEquals("APPROVE", timeline.get(1).getEventType());
        assertEquals("RELEASE_REVIEW_APPROVAL", timeline.get(1).getEvidenceCategory());
        assertEquals("放行阶段审核/批准证据", timeline.get(1).getEvidenceCategoryName());
        assertEquals("task-track", timeline.get(1).getBpmTaskId());
        assertEquals(approvedAt, timeline.get(1).getOccurredAt());
    }

    @Test
    void trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing() {
        MesProBatchRecordExecutionDO execution = insertExecution(3,
                "BRE-TRACK-ARCHIVE-LEGACY-001", "BATCH-TRACK-ARCHIVE-LEGACY");
        MesWmBatchDO batch = insertBatchForExecution(execution);
        routeProcessMapper.deleteById(execution.getRouteProcessId());
        LocalDateTime archivedAt = LocalDateTime.of(2026, 5, 26, 12, 0);
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(execution.getId())
                .actorId(88L)
                .actorName("归档人")
                .actionType("ARCHIVE_SEAL")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .comment("归档封存")
                .signedAt(archivedAt)
                .build());

        MesProBatchRecordExecutionTrackingPageReqVO trackingReqVO = new MesProBatchRecordExecutionTrackingPageReqVO();
        trackingReqVO.setPageNo(1);
        trackingReqVO.setPageSize(10);
        trackingReqVO.setExecutionCode("BRE-TRACK-ARCHIVE-LEGACY-001");
        PageResult<MesProBatchRecordExecutionTrackingRespVO> trackingPage =
                executionService.getTrackingPage(trackingReqVO);

        assertEquals(1L, trackingPage.getTotal());
        MesProBatchRecordExecutionTrackingRespVO row = trackingPage.getList().get(0);
        assertEquals(execution.getId(), row.getExecutionId());
        assertEquals(batch.getId(), row.getBatchId());
        assertNull(row.getProcessName());
        assertEquals("归档封存", row.getCurrentNodeName());
        assertEquals(List.of("归档人"), row.getCurrentAssigneeNames());
        assertEquals("ARCHIVE_SEAL", row.getLastEventType());
        assertEquals("归档封存", row.getLastEventReason());
        assertEquals(archivedAt, row.getLastEventAt());
    }

    @Test
    void trackingPage_shouldNotResolveHistoricalRouteProcessThroughCurrentIdentity() {
        MesProBatchRecordExecutionDO execution = insertExecution(3,
                "BRE-TRACK-HISTORICAL-ROUTE-001", "BATCH-TRACK-HISTORICAL-ROUTE");
        insertBatchForExecution(execution);
        signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(execution.getId())
                .actorId(88L)
                .actorName("审计人")
                .actionType("FORM_REVIEW")
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .comment("历史复核")
                .signedAt(LocalDateTime.of(2026, 7, 14, 9, 30))
                .build());
        when(routeProcessService.resolveCurrentRouteProcess(execution.getRouteProcessId(), execution.getRouteId(), null))
                .thenThrow(new IllegalStateException("无法解析当前工艺路线工序，routeId=900022，sourceProcessId=900333，routeProcessId=900055，processCode=B030"));

        MesProBatchRecordExecutionTrackingPageReqVO trackingReqVO = new MesProBatchRecordExecutionTrackingPageReqVO();
        trackingReqVO.setPageNo(1);
        trackingReqVO.setPageSize(10);
        trackingReqVO.setExecutionCode("BRE-TRACK-HISTORICAL-ROUTE-001");
        PageResult<MesProBatchRecordExecutionTrackingRespVO> trackingPage =
                executionService.getTrackingPage(trackingReqVO);

        assertEquals(1L, trackingPage.getTotal());
        MesProBatchRecordExecutionTrackingRespVO row = trackingPage.getList().get(0);
        assertEquals(execution.getId(), row.getExecutionId());
        assertEquals("焊接", row.getProcessName());
        assertEquals("焊接", row.getCurrentNodeName());
        assertEquals(List.of("审计人"), row.getCurrentAssigneeNames());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(
                execution.getRouteProcessId(), execution.getRouteId(), null);
    }

    @Test
    void executionAggregate_contractRequiresRouteProcessAndDefaultReportContext() {
        requireGetter(MesProBatchRecordExecutionDO.class, "getRouteProcessId");
        requireGetter(MesProBatchRecordExecutionDO.class, "getTaskId");
        requireGetter(MesProBatchRecordExecutionDO.class, "getWorkstationId");
        requireGetter(MesProBatchRecordExecutionDO.class, "getBatchRecordReportId");
        requireGetter(MesProBatchRecordExecutionDO.class, "getExecutionSnapshotJson");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getRouteProcessId");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getTaskId");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getWorkstationId");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getBatchRecordReportId");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getExecutionSnapshotJson");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getProcessCode");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getProcessName");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getWorkstationCode");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getWorkstationName");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getBatchRecordReportCode");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getBatchRecordReportName");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getActiveContextKey");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getBindingResolved");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getCanOpen");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getRouteCode");
        requireGetter(MesProBatchRecordExecutionRespVO.class, "getRouteName");
    }

    @Test
    void serviceContract_requiresContextOpenAndSignatureAwareSubmit() {
        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        requireGetter(openReqClass, "getWorkOrderId");
        requireGetter(openReqClass, "getRouteProcessId");
        requireGetter(openReqClass, "getTaskId");
        requireGetter(openReqClass, "getWorkstationId");
        requireGetter(openReqClass, "getBatchRecordReportId");
        requireGetter(openReqClass, "getBatchCode");
        requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", openReqClass);

        requireGetter(MesProBatchRecordExecutionSubmitReqVO.class, "getPassword");
        requireGetter(MesProBatchRecordExecutionSubmitReqVO.class, "getComment");
        requireMethod(MesProBatchRecordExecutionService.class, "submitBatchRecordExecution",
                MesProBatchRecordExecutionSubmitReqVO.class);

        Class<?> formReviewReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignReqVO");
        Class<?> formReviewRespClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignRespVO");
        requireGetter(formReviewReqClass, "getExecutionId");
        requireGetter(formReviewReqClass, "getPassword");
        requireGetter(formReviewReqClass, "getComment");
        requireGetter(formReviewRespClass, "getSignatureId");
        requireGetter(formReviewRespClass, "getActionType");
        requireGetter(formReviewRespClass, "getMeaningText");
        requireMethod(MesProBatchRecordExecutionService.class, "cosignBatchRecordExecution", formReviewReqClass);
    }

    @Test
    void entryContextContract_requiresResolvedDefaultBatchRecordBinding() {
        Class<?> reqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO");
        Class<?> respClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO");

        requireGetter(reqClass, "getWorkOrderId");
        requireGetter(reqClass, "getRouteProcessId");
        requireGetter(reqClass, "getBatchRecordReportId");
        requireGetter(respClass, "getRouteProcessId");
        requireGetter(respClass, "getTaskId");
        requireGetter(respClass, "getWorkstationId");
        requireGetter(respClass, "getBatchRecordReportId");
        requireGetter(respClass, "getBatchCode");
        requireGetter(respClass, "getCanOpen");
        requireGetter(respClass, "getBindingResolved");
        requireGetter(respClass, "getActiveExecutionId");
        requireGetter(respClass, "getActiveExecutionStatus");
        requireGetter(respClass, "getActiveContextKey");
        requireGetter(respClass, "getProcessCode");
        requireGetter(respClass, "getProcessName");
        requireGetter(respClass, "getWorkstationCode");
        requireGetter(respClass, "getWorkstationName");
        requireGetter(respClass, "getBatchRecordReportCode");
        requireGetter(respClass, "getBatchRecordReportName");
        requireGetter(respClass, "getRouteCode");
        requireGetter(respClass, "getRouteName");
        requireMethod(MesProBatchRecordExecutionService.class, "getEntryContext", reqClass);
    }

    @Test
    void openOrCreateByContext_missingDefaultBatchRecordBinding_mustFailFast() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId(" ")
                .remark("missing default batch record binding")
                .build();
        routeProcessMapper.insert(routeProcess);

        Class<?> reqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", reqClass);
        Object reqVO = reqClass.getDeclaredConstructor().newInstance();
        setValue(reqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(reqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(reqVO, "setBatchCode", String.class, "BATCH-20260523-RED");

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(executionService, reqVO));
        assertNotNull(thrown.getCause(), "openOrCreateByContext 缺少默认批记录绑定时必须抛出明确业务异常");
    }

    @Test
    void openOrCreateByContext_missingReportRuntimeSnapshot_mustFailFast() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-2002")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        reportMapper.insert(report("report-2002"));
        when(jimuReportGateway.getReportJson("report-2002")).thenReturn(" ");

        Class<?> reqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", reqClass);
        Object reqVO = reqClass.getDeclaredConstructor().newInstance();
        setValue(reqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(reqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(reqVO, "setBatchRecordReportId", String.class, "report-2002");
        setValue(reqVO, "setBatchCode", String.class, "BATCH-CTX-FAIL");

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(executionService, reqVO));
        assertServiceException(() -> {
            throw (RuntimeException) thrown.getCause();
        }, PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE);
    }

    @Test
    void openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-unreviewed-cell-rule")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        reportMapper.insert(report("report-unreviewed-cell-rule"));
        when(jimuReportGateway.getReportJson("report-unreviewed-cell-rule"))
                .thenReturn(sampleEditableReportJsonWithoutRules());

        Class<?> reqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", reqClass);
        Object reqVO = reqClass.getDeclaredConstructor().newInstance();
        setValue(reqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(reqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(reqVO, "setBatchRecordReportId", String.class, "report-unreviewed-cell-rule");
        setValue(reqVO, "setBatchCode", String.class, "BATCH-RULE-MISSING");

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> openMethod.invoke(executionService, reqVO));
        assertTrue(thrown.getCause() instanceof ServiceException);
        ServiceException serviceException = (ServiceException) thrown.getCause();
        assertEquals(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED.getCode(), serviceException.getCode());
        assertTrue(serviceException.getMessage().contains("第 1 行第 2 列"));
        assertTrue(serviceException.getMessage().contains("第 1 行第 4 列"));
        assertEquals(0L, executionMapper.selectCount());
    }

    @Test
    void openOrCreateByContext_shouldNormalizeExecutionSnapshotJsonStructure() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-snapshot")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO report = report("report-snapshot");
        report.setReportCode("EBR-SNAPSHOT");
        report.setReportName("标准化快照报表");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-snapshot")).thenReturn(sampleEditableReportJson());

        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", openReqClass);
        Object openReqVO = openReqClass.getDeclaredConstructor().newInstance();
        setValue(openReqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(openReqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(openReqVO, "setBatchRecordReportId", String.class, "report-snapshot");
        setValue(openReqVO, "setBatchCode", String.class, "BATCH-SNAPSHOT-01");

        Object openResp = openMethod.invoke(executionService, openReqVO);
        Long executionId = (Long) invokeGetter(openResp, "getId");
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        JSONObject snapshot = JSON.parseObject(execution.getExecutionSnapshotJson());

        assertEquals("EDHR_EXECUTION_V1", snapshot.getString("snapshotVersion"));
        assertEquals("JMREPORT", snapshot.getJSONObject("source").getString("type"));
        assertEquals("report-snapshot", snapshot.getJSONObject("source").getString("reportId"));
        assertNotNull(snapshot.getJSONObject("layout"));
        assertNotNull(snapshot.getJSONObject("meta"));
        JSONArray fields = snapshot.getJSONArray("fields");
        assertEquals(2, fields.size());
        assertEquals("ebr_snapshot_r0_c1", fields.getJSONObject(0).getString("fieldKey"));
        assertEquals("操作员", fields.getJSONObject(0).getString("label"));
        assertEquals(0, fields.getJSONObject(0).getIntValue("rowIndex"));
        assertEquals(1, fields.getJSONObject(0).getIntValue("columnIndex"));
        assertEquals("input-text", fields.getJSONObject(0).getString("component"));
        assertEquals(true, fields.getJSONObject(0).getBooleanValue("required"));
        assertEquals("OP-001", fields.getJSONObject(0).getString("defaultValue"));
        assertEquals("OP-001", fields.getJSONObject(0).getString("value"));
        assertEquals("STRING", fields.getJSONObject(0).getString("valueType"));
        assertEquals("填写实际执行本表单的操作人员姓名或工号", fields.getJSONObject(0).getString("helpText"));
        assertEquals("操作员", fields.getJSONObject(0).getJSONObject("edhrCellRule").getString("label"));
        assertEquals("备注", fields.getJSONObject(1).getString("label"));
        assertEquals("input-textarea", fields.getJSONObject(1).getString("component"));
        assertEquals("STRING", fields.getJSONObject(1).getString("valueType"));
        assertEquals("记录本次操作相关的补充说明", fields.getJSONObject(1).getString("helpText"));

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(executionId);
        JSONObject detailLayout = JSON.parseObject(detail.getSheetLayoutJson());
        assertTrue(hasRenderableRows(detailLayout));
        assertEquals("操作员", detailLayout.getJSONObject("rows")
                .getJSONObject("0").getJSONObject("cells").getJSONObject("0").getString("text"));
    }

    @Test
    void openOrCreateByContext_legacyStaticCheckboxCellsRequireRuleReviewBeforeExecution() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-legacy-static-checkbox")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO report = report("report-legacy-static-checkbox");
        report.setReportCode("EBR-CHECKBOX");
        report.setReportName("历史静态 checkbox 报表");
        MesProBatchRecordDefinitionDO definition = batchRecordDefinition("历史静态 checkbox 表单");
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO version = batchRecordVersion(definition.getId(), "V1.0", "APPROVED", null);
        versionMapper.insert(version);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(version.getId());
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-legacy-static-checkbox"))
                .thenReturn(sampleLegacyStaticCheckboxReportJson());

        ServiceException serviceException = assertThrows(ServiceException.class,
                () -> executionService.openOrCreateByContext(new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteProcessId(routeProcess.getId())
                        .setBatchRecordReportId("report-legacy-static-checkbox")
                        .setBatchCode("BATCH-CHECKBOX-01")));

        assertEquals(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED.getCode(), serviceException.getCode());
        assertTrue(serviceException.getMessage().contains("第 1 行第 2 列"));
        assertTrue(serviceException.getMessage().contains("第 2 行第 2 列"));
        assertEquals(0L, executionMapper.selectCount());
    }

    @Test
    void openOrCreateByContext_persistsInternalRecordMetadataToExecutionAndResponse() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-internal-trace")
                .remark("internal record binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO report = report("report-internal-trace");
        report.setReportCode("EBR-INTERNAL");
        report.setReportName("内部追溯记录");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-internal-trace")).thenReturn(sampleEditableReportJson());

        MesProBatchRecordExecutionOpenOrCreateByContextRespVO resp = executionService.openOrCreateByContext(
                new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteProcessId(routeProcess.getId())
                        .setBatchRecordReportId("report-internal-trace")
                        .setRecordCategory("INTERNAL_RECORD")
                        .setValidationProfile("INTERNAL_TRACE")
                        .setPermissionScopeId(5001L)
                        .setRouteBindingId(6001L)
                        .setRouteBindingSnapshotHash("2222222222222222222222222222222222222222222222222222222222222222")
                        .setBatchCode("BATCH-INTERNAL-EXECUTION"));

        assertEquals("INTERNAL_RECORD", resp.getRecordCategory());
        assertEquals("INTERNAL_TRACE", resp.getValidationProfile());
        assertEquals(5001L, resp.getPermissionScopeId());
        assertEquals(6001L, resp.getRouteBindingId());
        assertEquals("2222222222222222222222222222222222222222222222222222222222222222",
                resp.getRouteBindingSnapshotHash());

        MesProBatchRecordExecutionDO execution = executionMapper.selectById(resp.getId());
        assertEquals("INTERNAL_RECORD", execution.getRecordCategory());
        assertEquals("INTERNAL_TRACE", execution.getValidationProfile());
        assertEquals(5001L, execution.getPermissionScopeId());
        assertEquals(6001L, execution.getRouteBindingId());
        assertEquals("2222222222222222222222222222222222222222222222222222222222222222",
                execution.getRouteBindingSnapshotHash());

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(resp.getId());
        assertEquals("INTERNAL_RECORD", detail.getRecordCategory());
        assertEquals("INTERNAL_TRACE", detail.getValidationProfile());
        assertEquals(5001L, detail.getPermissionScopeId());
        assertEquals(6001L, detail.getRouteBindingId());
        assertEquals("2222222222222222222222222222222222222222222222222222222222222222",
                detail.getRouteBindingSnapshotHash());
    }

    @Test
    void openOrCreateByContext_persistsBatchRecordVersionSnapshotToExecution() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-version-snapshot")
                .remark("versioned binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO report = report("report-version-snapshot");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-version-snapshot")).thenReturn(sampleEditableReportJson());

        MesProBatchRecordExecutionOpenOrCreateByContextRespVO resp = executionService.openOrCreateByContext(
                new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteId(routeProcess.getRouteId())
                        .setRouteProcessId(routeProcess.getId())
                        .setBatchRecordReportId("report-version-snapshot")
                        .setBatchCode("BATCH-VERSION-SNAPSHOT"));

        MesProBatchRecordExecutionDO execution = executionMapper.selectById(resp.getId());
        assertEquals(report.getBatchRecordDefinitionId(), execution.getBatchRecordDefinitionId());
        assertEquals(report.getBatchRecordVersionId(), execution.getBatchRecordVersionId());
        assertEquals(routeProcess.getRouteId(), execution.getRouteId());
        assertEquals(report.getBatchRecordDefinitionId(), resp.getBatchRecordDefinitionId());
        assertEquals(report.getBatchRecordVersionId(), resp.getBatchRecordVersionId());
        assertEquals(routeProcess.getRouteId(), resp.getRouteId());
    }

    @Test
    void openOrCreateByContext_rejectsObsoleteVersionForNewBusiness() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-obsolete-version")
                .remark("obsolete version binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordDefinitionDO definition = batchRecordDefinition("受控表单");
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO obsoleteVersion = batchRecordVersion(definition.getId(), "V1.0", "OBSOLETE", null);
        versionMapper.insert(obsoleteVersion);
        MesProBatchRecordVersionDO currentVersion = batchRecordVersion(definition.getId(), "V2.0", "APPROVED",
                obsoleteVersion.getId());
        versionMapper.insert(currentVersion);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO report = report("report-obsolete-version");
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(obsoleteVersion.getId());
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-obsolete-version")).thenReturn(sampleEditableReportJson());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> executionService.openOrCreateByContext(new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteProcessId(routeProcess.getId())
                        .setBatchRecordReportId("report-obsolete-version")
                        .setBatchCode("BATCH-OBSOLETE-NEW")));

        assertTrue(exception.getMessage().contains("最新已发布"));
        assertEquals(0L, executionMapper.selectCount());
    }

    @Test
    void openOrCreateByContext_rejectsPendingApprovalVersionForNewBusiness() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-pending-version")
                .remark("pending version binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordDefinitionDO definition = batchRecordDefinition("审批中表单");
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO pendingVersion = batchRecordVersion(definition.getId(), "V2.0", "PENDING_APPROVAL", null);
        versionMapper.insert(pendingVersion);
        MesProBatchRecordReportDO report = report("report-pending-version");
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(pendingVersion.getId());
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-pending-version")).thenReturn(sampleEditableReportJson());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> executionService.openOrCreateByContext(new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteProcessId(routeProcess.getId())
                        .setBatchRecordReportId("report-pending-version")
                        .setBatchCode("BATCH-PENDING-NEW")));

        assertTrue(exception.getMessage().contains("最新已发布"));
        assertEquals(0L, executionMapper.selectCount());
    }

    @Test
    void openOrCreateByContext_allowsExistingHistoricalExecutionOnObsoleteVersion() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-obsolete-history")
                .remark("historical obsolete version binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordDefinitionDO definition = batchRecordDefinition("历史追溯表单");
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO obsoleteVersion = batchRecordVersion(definition.getId(), "V1.0", "OBSOLETE", null);
        versionMapper.insert(obsoleteVersion);
        MesProBatchRecordVersionDO currentVersion = batchRecordVersion(definition.getId(), "V2.0", "APPROVED",
                obsoleteVersion.getId());
        versionMapper.insert(currentVersion);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO report = report("report-obsolete-history");
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(obsoleteVersion.getId());
        reportMapper.insert(report);
        MesProBatchRecordExecutionDO existing = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-HISTORY-OBSOLETE")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess.getId())
                .batchRecordReportId("report-obsolete-history")
                .batchRecordDefinitionId(definition.getId())
                .batchRecordVersionId(obsoleteVersion.getId())
                .batchCode("BATCH-OBSOLETE-HISTORY")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson("{\"from\":\"historical\"}")
                .cellValuesJson("[]")
                .activeContextKey(existingContextKey(workOrder.getId(), null, routeProcess.getId(),
                        null, "report-obsolete-history", "BATCH-OBSOLETE-HISTORY"))
                .remark(null)
                .build();
        executionMapper.insert(existing);

        MesProBatchRecordExecutionOpenOrCreateByContextRespVO resp = executionService.openOrCreateByContext(
                new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteProcessId(routeProcess.getId())
                        .setBatchRecordReportId("report-obsolete-history")
                        .setBatchCode("BATCH-OBSOLETE-HISTORY"));

        assertEquals(existing.getId(), resp.getId());
        assertEquals(Boolean.FALSE, resp.getCreated());
        assertEquals(obsoleteVersion.getId(), resp.getBatchRecordVersionId());
    }

    @Test
    void openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-reviewed-cell-rule")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO report = report("report-reviewed-cell-rule");
        report.setReportCode("EBR-RULE-FREEZE");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-reviewed-cell-rule"))
                .thenReturn(sampleEditableReportJsonWithReviewedNumberAndDateRules());

        Class<?> reqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", reqClass);
        Object reqVO = reqClass.getDeclaredConstructor().newInstance();
        setValue(reqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(reqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(reqVO, "setBatchRecordReportId", String.class, "report-reviewed-cell-rule");
        setValue(reqVO, "setBatchCode", String.class, "BATCH-RULE-FROZEN");

        Object openResp = openMethod.invoke(executionService, reqVO);

        Long executionId = (Long) invokeGetter(openResp, "getId");
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        JSONObject snapshot = JSON.parseObject(execution.getExecutionSnapshotJson());
        JSONArray fields = snapshot.getJSONArray("fields");
        assertEquals("NUMBER", fields.getJSONObject(0).getString("valueType"));
        assertEquals("input-number", fields.getJSONObject(0).getString("component"));
        assertEquals("g", fields.getJSONObject(0).getString("unit"));
        assertEquals(100, fields.getJSONObject(0).getJSONObject("constraints").getIntValue("max"));
        assertEquals("DATE", fields.getJSONObject(1).getString("valueType"));
        assertEquals("date", fields.getJSONObject(1).getString("component"));
        assertTrue(execution.getExecutionSnapshotJson().contains("\"edhrCellRule\""));
    }

    @Test
    void openOrCreateByContext_shouldIgnoreNonNumericLenKeysInReportRowsAndCells() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1002L)
                .processId(2003L)
                .sort(1)
                .batchRecordReportId("report-with-len")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO report = report("report-with-len");
        report.setReportCode("EBR-LEN");
        report.setReportName("包含 len 键的报表");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("report-with-len")).thenReturn(sampleEditableReportJsonWithLenKeys());

        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", openReqClass);
        Object openReqVO = openReqClass.getDeclaredConstructor().newInstance();
        setValue(openReqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(openReqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(openReqVO, "setBatchRecordReportId", String.class, "report-with-len");
        setValue(openReqVO, "setBatchCode", String.class, "BATCH-LEN-01");

        Object openResp = openMethod.invoke(executionService, openReqVO);
        Long executionId = (Long) invokeGetter(openResp, "getId");
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        JSONObject snapshot = JSON.parseObject(execution.getExecutionSnapshotJson());
        JSONArray fields = snapshot.getJSONArray("fields");

        assertEquals(2, fields.size());
        assertEquals("ebr_len_r0_c1", fields.getJSONObject(0).getString("fieldKey"));
        assertEquals("操作员", fields.getJSONObject(0).getString("label"));
        assertEquals("ebr_len_r1_c1", fields.getJSONObject(1).getString("fieldKey"));
        assertEquals("备注", fields.getJSONObject(1).getString("label"));
    }

    @Test
    void entryContextAndOpenOrCreateByContext_ignoreScheduleTaskFieldsForFutureExecutionContext() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteDO route = MesProRouteDO.builder().code("ROUTE-CTX").name("执行路线").status(0).remark("").build();
        routeMapper.insert(route);
        MesProProcessDO process = MesProProcessDO.builder().code("PROC-CTX").name("焊接").status(0).remark("").build();
        processMapper.insert(process);
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .code("WS-CTX").name("焊接工位").processId(process.getId()).status(0).remark("").build();
        workstationMapper.insert(workstation);
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(route.getId())
                .processId(process.getId())
                .sort(1)
                .batchRecordReportId("report-2002")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        reportMapper.insert(report("report-2002"));

        Class<?> entryReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO");
        Method entryMethod = requireMethod(MesProBatchRecordExecutionService.class, "getEntryContext", entryReqClass);
        Object entryReqVO = entryReqClass.getDeclaredConstructor().newInstance();
        setValue(entryReqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(entryReqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(entryReqVO, "setTaskId", Long.class, 3003L);
        setValue(entryReqVO, "setWorkstationId", Long.class, workstation.getId());
        setValue(entryReqVO, "setBatchRecordReportId", String.class, "report-2002");
        setValue(entryReqVO, "setBatchCode", String.class, "BATCH-CTX-01");

        Object entryResp = entryMethod.invoke(executionService, entryReqVO);
        assertEquals(routeProcess.getId(), invokeGetter(entryResp, "getRouteProcessId"));
        assertNull(invokeGetter(entryResp, "getTaskId"));
        assertNull(invokeGetter(entryResp, "getWorkstationId"));
        assertEquals("report-2002", invokeGetter(entryResp, "getBatchRecordReportId"));
        assertEquals("PROC-CTX", invokeGetter(entryResp, "getProcessCode"));
        assertEquals("焊接", invokeGetter(entryResp, "getProcessName"));
        assertNull(invokeGetter(entryResp, "getWorkstationCode"));
        assertNull(invokeGetter(entryResp, "getWorkstationName"));
        assertEquals("EBR-001", invokeGetter(entryResp, "getBatchRecordReportCode"));
        assertEquals("电子批记录", invokeGetter(entryResp, "getBatchRecordReportName"));
        assertEquals("ROUTE-CTX", invokeGetter(entryResp, "getRouteCode"));
        assertEquals("执行路线", invokeGetter(entryResp, "getRouteName"));
        assertEquals("BATCH-CTX-01", invokeGetter(entryResp, "getBatchCode"));
        assertEquals(Boolean.TRUE, invokeGetter(entryResp, "getCanOpen"));
        assertEquals(Boolean.TRUE, invokeGetter(entryResp, "getBindingResolved"));
        assertEquals(existingContextKey(workOrder.getId(), null, routeProcess.getId(), null, "report-2002", "BATCH-CTX-01"),
                invokeGetter(entryResp, "getActiveContextKey"));

        MesProBatchRecordExecutionDO existing = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-CONTEXT-001")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess.getId())
                .taskId(null)
                .workstationId(null)
                .batchRecordReportId("report-2002")
                .batchCode("BATCH-CTX-01")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson("{\"from\":\"existing\"}")
                .cellValuesJson("[]")
                .remark(null)
                .build();
        executionMapper.insert(existing);

        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", openReqClass);
        Object openReqVO = openReqClass.getDeclaredConstructor().newInstance();
        setValue(openReqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(openReqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(openReqVO, "setTaskId", Long.class, 3003L);
        setValue(openReqVO, "setWorkstationId", Long.class, workstation.getId());
        setValue(openReqVO, "setBatchRecordReportId", String.class, "report-2002");
        setValue(openReqVO, "setBatchCode", String.class, "BATCH-CTX-01");

        Object openResp = openMethod.invoke(executionService, openReqVO);
        assertEquals(existing.getId(), invokeGetter(openResp, "getId"));
        assertEquals(existing.getExecutionCode(), invokeGetter(openResp, "getExecutionCode"));
        assertEquals(Boolean.FALSE, invokeGetter(openResp, "getCreated"));
        assertEquals(existingContextKey(workOrder.getId(), null, routeProcess.getId(), null, "report-2002", "BATCH-CTX-01"),
                invokeGetter(openResp, "getActiveContextKey"));
        assertEquals(1L, executionMapper.selectCount());
    }

    @Test
    void openOrCreateByContext_doesNotPersistScheduleTaskFieldsForNewExecution() {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-no-schedule-ref")
                .remark("future execution context")
                .build();
        routeProcessMapper.insert(routeProcess);
        reportMapper.insert(report("report-no-schedule-ref"));
        when(jimuReportGateway.getReportJson("report-no-schedule-ref")).thenReturn(sampleEditableReportJson());

        MesProBatchRecordExecutionOpenOrCreateByContextRespVO resp = executionService.openOrCreateByContext(
                new MesProBatchRecordExecutionOpenOrCreateByContextReqVO()
                        .setWorkOrderId(workOrder.getId())
                        .setRouteProcessId(routeProcess.getId())
                        .setTaskId(3003L)
                        .setWorkstationId(4004L)
                        .setBatchRecordReportId("report-no-schedule-ref")
                        .setBatchCode("BATCH-NO-SCHEDULE-REF"));

        MesProBatchRecordExecutionDO execution = executionMapper.selectById(resp.getId());
        assertNull(execution.getTaskId());
        assertNull(execution.getWorkstationId());
        assertEquals(existingContextKey(workOrder.getId(), null, routeProcess.getId(), null,
                "report-no-schedule-ref", "BATCH-NO-SCHEDULE-REF"), execution.getActiveContextKey());
        assertEquals(execution.getActiveContextKey(), resp.getActiveContextKey());
    }

    @Test
    void openOrCreateByContext_reusesSubmittedExecutionForActiveStatus() throws Exception {
        assertReuseForActiveStatus(1);
    }

    @Test
    void openOrCreateByContext_rejectedExecutionCreatesNewDraft() throws Exception {
        assertCreatesNewExecutionForInactiveRejectedStatus();
    }

    @Test
    void getExecutionAndPage_shouldExposeProcessWorkstationReportAndContextFields() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteDO route = MesProRouteDO.builder().code("ROUTE-01").name("焊接路线").status(0).remark("").build();
        routeMapper.insert(route);
        MesProProcessDO process = MesProProcessDO.builder().code("PROC-01").name("焊接").status(0).remark("").build();
        processMapper.insert(process);
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .code("WS-01").name("焊接工位").processId(process.getId()).status(0).remark("").build();
        workstationMapper.insert(workstation);
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(route.getId()).processId(process.getId()).sort(1).batchRecordReportId("report-detail").build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordReportDO detailReport = report("report-detail");
        detailReport.setReportCode("EBR-DET");
        detailReport.setReportName("执行报表");
        reportMapper.insert(detailReport);

        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-DETAIL-01")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess.getId())
                .taskId(3003L)
                .workstationId(workstation.getId())
                .batchRecordReportId("report-detail")
                .batchCode("BATCH-DETAIL-01")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson("{\"from\":\"detail\"}")
                .cellValuesJson("[]")
                .remark(null)
                .build();
        executionMapper.insert(execution);

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(execution.getId());
        assertEquals("PROC-01", detail.getProcessCode());
        assertEquals("焊接", detail.getProcessName());
        assertEquals("ROUTE-01", detail.getRouteCode());
        assertEquals("焊接路线", detail.getRouteName());
        assertEquals("WS-01", detail.getWorkstationCode());
        assertEquals("焊接工位", detail.getWorkstationName());
        assertEquals("EBR-DET", detail.getBatchRecordReportCode());
        assertEquals("执行报表", detail.getBatchRecordReportName());
        assertEquals(Boolean.TRUE, detail.getBindingResolved());
        assertEquals(Boolean.TRUE, detail.getCanOpen());
        assertEquals(existingContextKey(workOrder.getId(), 3003L, routeProcess.getId(), workstation.getId(), "report-detail", "BATCH-DETAIL-01"),
                detail.getActiveContextKey());
        verify(permissionGateService).requireAbility(argThat(command ->
                "BATCH_RECORD_EXECUTION".equals(command.getObjectType())
                        && String.valueOf(execution.getId()).equals(command.getObjectId())
                        && "VIEW".equals(command.getAbility())
                        && execution.getId().equals(command.getExecutionId())
                        && routeProcess.getId().equals(command.getRouteProcessId())
                        && "report-detail".equals(command.getReportId())
                        && "mes:pro-batch-record-execution:query".equals(command.getPermissionCode())));

        MesProBatchRecordExecutionPageReqVO pageReqVO = new MesProBatchRecordExecutionPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setActiveContextKey(existingContextKey(workOrder.getId(), 3003L, routeProcess.getId(),
                workstation.getId(), "report-detail", "BATCH-DETAIL-01"));
        PageResult<MesProBatchRecordExecutionRespVO> page = executionService.getBatchRecordExecutionPage(pageReqVO);
        assertEquals(1L, page.getTotal());
        assertEquals(execution.getId(), page.getList().get(0).getId());
        assertEquals("PROC-01", page.getList().get(0).getProcessCode());
        assertEquals("ROUTE-01", page.getList().get(0).getRouteCode());
        assertEquals(Boolean.TRUE, page.getList().get(0).getBindingResolved());
        assertEquals(Boolean.TRUE, page.getList().get(0).getCanOpen());
    }

    @Test
    void getExecution_withValidatedFillWorkTaskUsesWorkTaskAccessWithoutExecutionScopeView() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-FILL-WORK-TASK-VIEW", "BATCH-FILL-WORK-TASK-VIEW");
        Long workTaskId = 7001L;
        when(workTaskService.validateWritableFillTaskForExecution(workTaskId, execution.getId()))
                .thenReturn(new MesProEdhrWorkTaskDO()
                        .setId(workTaskId)
                        .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                        .setExecutionId(execution.getId()));
        doThrow(new IllegalStateException("execution scope VIEW should not be required for an assigned fill task"))
                .when(permissionGateService).requireAbility(any());

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(execution.getId(), workTaskId);

        assertEquals(execution.getId(), detail.getId());
        verify(workTaskService).validateWritableFillTaskForExecution(workTaskId, execution.getId());
        verify(permissionGateService, never()).requireAbility(any());
    }

    @Test
    void getExecution_shouldExposeTypedCellValueProjection() {
        String cellValuesJson = "[{\"rowIndex\":4,\"columnIndex\":5,\"valueType\":\"NUMBER\","
                + "\"value\":37.5,\"valueDisplay\":\"37.5\",\"valueHash\":\"hash-number\",\"unit\":\"kg\"}]";
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-TYPED-CELL-001", "BATCH-TYPED-CELL");
        executionMapper.updateById(MesProBatchRecordExecutionDO.builder()
                .id(execution.getId())
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson))
                .build());

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(execution.getId());

        assertEquals(1, detail.getCellValues().size());
        MesProBatchRecordExecutionCellValueVO cellValue = detail.getCellValues().get(0);
        assertEquals(4, cellValue.getRowIndex());
        assertEquals(5, cellValue.getColumnIndex());
        assertEquals("NUMBER", cellValue.getValueType());
        assertEquals("37.5", cellValue.getValue().toString());
        assertEquals("37.5", cellValue.getValueDisplay());
        assertEquals("hash-number", cellValue.getValueHash());
        assertEquals("kg", cellValue.getUnit());
    }

    @Test
    void getBatchRecordExecution_includesAttachmentSummaries() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, "BRE-DETAIL-ATT-001", "BATCH-DETAIL-ATT");
        attachmentMapper.insert(MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(execution.getId())
                .batchExecutionId(601L)
                .batchTaskId(execution.getTaskId())
                .workTaskId(701L)
                .rowIndex(1)
                .columnIndex(2)
                .fieldKey("visualEvidence")
                .fieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                .fieldLabel("现场图片")
                .attachmentType("IMAGE")
                .attachmentGroupKey("R1C2-IMG-1")
                .attachmentAction("ADD")
                .versionNo(1)
                .fileId(901L)
                .fileUrl("http://127.0.0.1:9000/yudao/edhr/501/evidence.png")
                .storageConfigId(28L)
                .storagePath("edhr/501/evidence.png")
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize(2048L)
                .sha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .storageRetentionJson("{\"fileId\":901,\"retention\":\"batch-record\"}")
                .storageRetentionHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .auditBatchId(7001L)
                .signatureId(8001L)
                .previousAttachmentHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .attachmentHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .operatorId(99L)
                .operatorName("QA")
                .operatedAt(LocalDateTime.of(2026, 6, 12, 10, 30))
                .reasonCategory("CORRECTION")
                .reasonText("现场上传")
                .build());

        MesProBatchRecordExecutionRespVO detail = executionService.getBatchRecordExecution(execution.getId());

        assertEquals(1, detail.getAttachmentSummaries().size());
        MesProBatchRecordExecutionRespVO.AttachmentSummary attachment = detail.getAttachmentSummaries().get(0);
        assertEquals("ADD", attachment.getAttachmentAction());
        assertEquals("visualEvidence", attachment.getFieldKey());
        assertEquals("现场图片", attachment.getFieldLabel());
        assertEquals("evidence.png", attachment.getFileName());
        assertEquals("image/png", attachment.getContentType());
        assertEquals(2048L, attachment.getFileSize());
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                attachment.getSha256());
        assertEquals(7001L, attachment.getAuditBatchId());
        assertEquals(8001L, attachment.getSignatureId());
    }

    private MesProBatchRecordExecutionDO insertExecution(int status, String executionCode, String batchCode) {
        ensureDefaultApprovalContext();
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-" + executionCode)
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode(executionCode)
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess.getId())
                .taskId(3003L)
                .workstationId(4004L)
                .batchRecordReportId("report-" + executionCode)
                .batchCode(batchCode)
                .status(status)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(defaultExecutionSnapshotJson())
                .cellValuesJson("[]")
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .remark(null)
                .activeContextKey(existingContextKey(workOrder.getId(), 3003L, routeProcess.getId(), 4004L,
                        "report-" + executionCode, batchCode))
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private void insertPreReleaseBatchTask(MesProBatchRecordExecutionDO execution, Long batchExecutionId,
                                           Long batchTaskId, Integer batchStatus) {
        edhrBatchExecutionMapper.insert(new MesProEdhrBatchExecutionDO()
                .setId(batchExecutionId)
                .setBatchExecutionCode("EDHR-" + batchExecutionId)
                .setWorkOrderId(execution.getWorkOrderId())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchCode(execution.getBatchCode())
                .setRouteId(1001L)
                .setStatus(batchStatus)
                .setTaskTotal(1)
                .setTaskApprovedCount(1)
                .setBlockedCount(0));
        edhrBatchExecutionTaskMapper.insert(new MesProEdhrBatchExecutionTaskDO()
                .setId(batchTaskId)
                .setBatchExecutionId(batchExecutionId)
                .setNodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .setRouteProcessId(execution.getRouteProcessId())
                .setRouteProcessSort(1)
                .setProcessId(2002L)
                .setProcessCode("P-REDO")
                .setProcessName("重新提交工序")
                .setBatchRecordReportId(execution.getBatchRecordReportId())
                .setBatchRecordReportName("重新提交表单")
                .setExecutionId(execution.getId())
                .setRequiredFlag(Boolean.TRUE)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        workTaskMapper.insert(new MesProEdhrWorkTaskDO()
                .setId(8001L)
                .setTaskCode("EDHRT-REDO-" + execution.getId())
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchExecutionId(batchExecutionId)
                .setBatchTaskId(batchTaskId)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(batchTaskId)
                .setExecutionId(execution.getId())
                .setWorkOrderId(execution.getWorkOrderId())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchCode(execution.getBatchCode())
                .setRouteId(1001L)
                .setRouteProcessId(execution.getRouteProcessId())
                .setProcessId(2002L)
                .setProcessName("重新提交工序")
                .setAssigneeUserId(99L)
                .setCandidateUserSnapshot("99")
                .setStatus(MesProEdhrWorkTaskStatus.DONE)
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=" + execution.getId()));
    }

    private MesProEdhrProcessFormPermissionRuleDO processFormPermissionRule(Long routeProcessId,
                                                                            String reportId,
                                                                            Long batchRecordVersionId,
                                                                            String candidateSourceIds,
                                                                            String remark) {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordVersionId(batchRecordVersionId)
                .setRuleType("SIGNATURE")
                .setSignatureCellKey("R1C1")
                .setSignatureRole("REVIEW")
                .setCandidateSourceType("USER")
                .setCandidateSourceIds(candidateSourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(30)
                .setEnabled(true)
                .setRemark(remark);
    }

    private MesWmBatchDO insertBatchForExecution(MesProBatchRecordExecutionDO execution) {
        MesWmBatchDO batch = MesWmBatchDO.builder()
                .code(execution.getBatchCode())
                .itemId(1001L)
                .workOrderId(execution.getWorkOrderId())
                .build();
        batchMapper.insert(batch);
        return batch;
    }

    private String defaultExecutionSnapshotJson() {
        return "{\"from\":\"test\"}";
    }

    private String reviewExecutionSnapshotJson() {
        return """
                {
                  "from": "test",
                  "rows": {
                    "1": {
                      "cells": {
                        "1": {
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R1C1",
                            "reviewSourceType": "POST",
                            "reviewSourceId": 7001,
                            "reviewSourceName": "QA 岗"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String multiUserReviewExecutionSnapshotJson() {
        return """
                {
                  "from": "test",
                  "rows": {
                    "1": {
                      "cells": {
                        "1": {
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R1C1",
                            "reviewSourceType": "USERS",
                            "reviewSourceIds": [88, 89],
                            "reviewSourceName": "审核人A、审核人B"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String deptReviewExecutionSnapshotJson() {
        return """
                {
                  "from": "test",
                  "rows": {
                    "1": {
                      "cells": {
                        "1": {
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R1C1",
                            "reviewSourceType": "DEPT",
                            "reviewSourceId": 8001,
                            "reviewSourceName": "质量部"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String reviewSourceExecutionSnapshotJson(String reviewSourceType, Long reviewSourceId,
                                                     String reviewSourceName) {
        return """
                {
                  "from": "test",
                  "rows": {
                    "1": {
                      "cells": {
                        "1": {
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R1C1",
                            "reviewSourceType": "%s",
                            "reviewSourceId": %d,
                            "reviewSourceName": "%s"
                          }
                        }
                      }
                    }
                  }
                }
                """.formatted(reviewSourceType, reviewSourceId, reviewSourceName);
    }

    private String requiredFieldExecutionSnapshotJson() {
        return """
                {
                  "snapshotVersion": "EDHR_EXECUTION_V1",
                  "from": "test",
                  "fields": [
                    {
                      "fieldPath": "rows.4.cells.5.required_temperature",
                      "fieldKey": "required_temperature",
                      "label": "操作温度",
                      "rowIndex": 4,
                      "columnIndex": 5,
                      "valueType": "NUMBER",
                      "component": "input-number",
                      "required": true,
                      "readonly": false
                    }
                  ],
                  "rows": {
                    "1": {
                      "cells": {
                        "1": {
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R1C1",
                            "reviewSourceType": "POST",
                            "reviewSourceId": 7001,
                            "reviewSourceName": "QA 岗"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String requiredFieldOrdinaryExecutionSnapshotJson() {
        return """
                {
                  "snapshotVersion": "EDHR_EXECUTION_V1",
                  "from": "test",
                  "fields": [
                    {
                      "fieldPath": "rows.4.cells.5.required_temperature",
                      "fieldKey": "required_temperature",
                      "label": "操作温度",
                      "rowIndex": 4,
                      "columnIndex": 5,
                      "valueType": "NUMBER",
                      "component": "input-number",
                      "required": true,
                      "readonly": false
                    }
                  ],
                  "rows": {}
                }
                """;
    }

    private String releaseApprovalSignatureExecutionSnapshotJson() {
        return """
                {
                  "snapshotVersion": "EDHR_EXECUTION_V1",
                  "from": "test",
                  "fields": [],
                  "rows": {
                    "50": {
                      "cells": {
                        "0": {
                          "text": "过程放行人/放行日期：",
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R50C0",
                            "label": "审批签名",
                            "reviewSourceType": "ROLE",
                            "reviewSourceId": 910289,
                            "reviewSourceName": "eDHR矩阵-审批人"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String requiredNumberAndBooleanFieldExecutionSnapshotJson() {
        return """
                {
                  "snapshotVersion": "EDHR_EXECUTION_V1",
                  "from": "test",
                  "fields": [
                    {
                      "fieldPath": "rows.4.cells.5.required_temperature",
                      "fieldKey": "required_temperature",
                      "label": "操作温度",
                      "rowIndex": 4,
                      "columnIndex": 5,
                      "valueType": "NUMBER",
                      "component": "input-number",
                      "required": true,
                      "readonly": false
                    },
                    {
                      "fieldPath": "rows.6.cells.2.required_confirmed",
                      "fieldKey": "required_confirmed",
                      "label": "是否确认",
                      "rowIndex": 6,
                      "columnIndex": 2,
                      "valueType": "BOOLEAN",
                      "component": "checkbox",
                      "required": true,
                      "readonly": false
                    }
                  ],
                  "rows": {
                    "1": {
                      "cells": {
                        "1": {
                          "edhrSignature": {
                            "enabled": true,
                            "actionType": "APPROVE",
                            "signatureCellKey": "R1C1",
                            "reviewSourceType": "POST",
                            "reviewSourceId": 7001,
                            "reviewSourceName": "QA 岗"
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private MesProBatchRecordExecutionDO attachReviewSignatureSnapshot(MesProBatchRecordExecutionDO execution) {
        attachReviewSignatureSnapshot(execution.getId());
        return executionMapper.selectById(execution.getId());
    }

    private void attachReviewSignatureSnapshot(Long executionId) {
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE")
                .setExecutionSnapshotJson(reviewExecutionSnapshotJson()));
    }

    private void attachDefaultApprovalContext(Long executionId) {
        ensureDefaultApprovalContext();
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-" + execution.getExecutionCode())
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setRouteProcessId(routeProcess.getId())
                .setTaskId(3003L)
                .setWorkstationId(4004L)
                .setBatchRecordReportId("report-" + execution.getExecutionCode()));
    }

    private void grantGoldenFingerPermission(Long userId) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(910399L);
        role.setCode(MesProEdhrGoldenFingerPermissionService.ROLE_CODE);
        role.setName("批记录金手指管理员");
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(permissionApi.getUserRoleIdListByUserId(userId)).thenReturn(Set.of(role.getId()));
        when(roleApi.getRoleList(any())).thenReturn(List.of(role));
        when(permissionApi.hasAnyPermissionsInRoles(Set.of(role.getId()),
                MesProEdhrGoldenFingerPermissionService.PERMISSION)).thenReturn(true);
    }

    private void ensureDefaultApprovalContext() {
        if (processMapper.selectById(2002L) == null) {
            processMapper.insert(MesProProcessDO.builder()
                    .id(2002L)
                    .code("PROC-DEFAULT")
                    .name("焊接")
                    .status(0)
                    .remark("")
                    .build());
        }
        if (workstationMapper.selectById(4004L) == null) {
            workstationMapper.insert(MesMdWorkstationDO.builder()
                    .id(4004L)
                    .code("WS-DEFAULT")
                    .name("焊接工位")
                    .processId(2002L)
                    .status(0)
                    .remark("")
                    .build());
        }
    }

    private MesProBatchRecordExecutionDO insertSubmittedExecutionWithSnapshot(String processInstanceId, String bpmTaskId) {
        MesProBatchRecordExecutionDO execution = insertExecution(1, "BRE-" + processInstanceId, "BATCH-" + processInstanceId);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setProcessDefinitionKey("mes-edhr-approval-v1")
                .setProcessInstanceId(processInstanceId)
                .setSubmittedBy(77L)
                .setSubmittedAt(LocalDateTime.now()));
        approvalSnapshotMapper.insert(MesProBatchRecordApprovalSnapshotDO.builder()
                .executionId(execution.getId())
                .processDefinitionKey("mes-edhr-approval-v1")
                .processInstanceId(processInstanceId)
                .approvalStatus("SUBMITTED")
                .snapshotJson("{\"businessKey\":\"EDHR_EXECUTION:" + execution.getId()
                        + "\",\"cellValuesHash\":\"" + execution.getCellValuesHash()
                        + "\",\"fieldAuditRevision\":" + execution.getFieldAuditRevision()
                        + ",\"fieldAuditHeadHash\":\"" + execution.getFieldAuditHeadHash()
                        + "\",\"domainTraceSnapshotId\":7001"
                        + ",\"domainTraceHash\":\"" + DOMAIN_TRACE_HASH + "\""
                        + ",\"domainTraceStatus\":\"VERIFIED\"}")
                .snapshotHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .currentBpmTaskId(bpmTaskId)
                .currentTaskDefinitionKey("approveNode")
                .submitSignatureId(1001L)
                .submittedBy(77L)
                .submittedAt(LocalDateTime.now())
                .build());
        return executionMapper.selectById(execution.getId());
    }

    private MesProBatchRecordExecutionApproveReqVO approveReq(MesProBatchRecordExecutionDO execution, String bpmTaskId) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        Long workTaskId = 9001L;
        stubReviewWorkTask(workTaskId, execution, snapshot.getCurrentBpmTaskId());
        return new MesProBatchRecordExecutionApproveReqVO()
                .setExecutionId(execution.getId())
                .setWorkTaskId(workTaskId)
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setApprovalSnapshotId(snapshot.getId())
                .setApprovalSnapshotHash(snapshot.getSnapshotHash())
                .setBpmTaskId(bpmTaskId);
    }

    private MesProBatchRecordExecutionApproveReqVO approveReqForApproveTask(MesProBatchRecordExecutionDO execution,
                                                                            String bpmTaskId) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        Long workTaskId = 9002L;
        stubApproveWorkTask(workTaskId, execution, snapshot.getCurrentBpmTaskId());
        return new MesProBatchRecordExecutionApproveReqVO()
                .setExecutionId(execution.getId())
                .setWorkTaskId(workTaskId)
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setApprovalSnapshotId(snapshot.getId())
                .setApprovalSnapshotHash(snapshot.getSnapshotHash())
                .setBpmTaskId(bpmTaskId);
    }

    private void removeDomainTraceHashFromApprovalSnapshot(MesProBatchRecordExecutionDO execution) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        JSONObject snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        snapshotJson.remove("domainTraceHash");
        approvalSnapshotMapper.updateById(new MesProBatchRecordApprovalSnapshotDO()
                .setId(snapshot.getId())
                .setSnapshotJson(snapshotJson.toJSONString()));
    }

    private MesProBatchRecordExecutionRejectReqVO rejectReq(MesProBatchRecordExecutionDO execution, String bpmTaskId) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        Long workTaskId = 9001L;
        stubReviewWorkTask(workTaskId, execution, snapshot.getCurrentBpmTaskId());
        return new MesProBatchRecordExecutionRejectReqVO()
                .setExecutionId(execution.getId())
                .setWorkTaskId(workTaskId)
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setApprovalSnapshotId(snapshot.getId())
                .setApprovalSnapshotHash(snapshot.getSnapshotHash())
                .setBpmTaskId(bpmTaskId);
    }

    private MesProEdhrWorkTaskDO stubReviewWorkTask(Long workTaskId, MesProBatchRecordExecutionDO execution, String bpmTaskId) {
        MesProEdhrWorkTaskDO reviewTask = new MesProEdhrWorkTaskDO()
                .setId(workTaskId)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setExecutionId(execution.getId())
                .setBpmTaskId(bpmTaskId)
                .setSignatureCellKey("review-cell")
                .setSignatureRowIndex(1)
                .setSignatureColumnIndex(1)
                .setReviewSourceType("POST")
                .setReviewSourceId(7001L)
                .setReviewSourceName("QA 岗");
        lenient().when(workTaskService.validateWritableTask(eq(workTaskId), eq(execution.getId()),
                eq(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW))).thenReturn(reviewTask);
        lenient().when(workTaskService.validateWritableReviewOrApproveTask(workTaskId, execution.getId()))
                .thenReturn(reviewTask);
        lenient().when(workTaskService.completeOneReviewTask(eq(workTaskId), eq(execution.getId())))
                .thenReturn(reviewTask);
        return reviewTask;
    }

    private MesProEdhrWorkTaskDO stubApproveWorkTask(Long workTaskId, MesProBatchRecordExecutionDO execution, String bpmTaskId) {
        MesProEdhrWorkTaskDO approveTask = new MesProEdhrWorkTaskDO()
                .setId(workTaskId)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_APPROVE)
                .setExecutionId(execution.getId())
                .setBpmTaskId(bpmTaskId)
                .setSignatureCellKey("review-cell")
                .setSignatureRowIndex(1)
                .setSignatureColumnIndex(1)
                .setReviewSourceType("POST")
                .setReviewSourceId(7001L)
                .setReviewSourceName("QA 岗");
        lenient().when(workTaskService.validateWritableReviewOrApproveTask(workTaskId, execution.getId()))
                .thenReturn(approveTask);
        lenient().when(workTaskService.validateWritableApproveTask(workTaskId, execution.getId()))
                .thenReturn(approveTask);
        lenient().when(workTaskService.completeApproveTask(workTaskId, execution.getId()))
                .thenReturn(approveTask);
        return approveTask;
    }

    private AdminUserRespDTO enabledReviewUser() {
        return enabledReviewUser(88L, "审核人");
    }

    private AdminUserRespDTO enabledReviewUser(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

    private Task mockTask(String taskId, String processInstanceId, String taskDefinitionKey, String name) {
        return mockTask(taskId, processInstanceId, taskDefinitionKey, name, "88");
    }

    private Task mockTask(String taskId, String processInstanceId, String taskDefinitionKey, String name, String assignee) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(taskId);
        when(task.getProcessInstanceId()).thenReturn(processInstanceId);
        when(task.getTaskDefinitionKey()).thenReturn(taskDefinitionKey);
        when(task.getName()).thenReturn(name);
        when(task.getAssignee()).thenReturn(assignee);
        return task;
    }

    private HistoricTaskInstance mockHistoricTask(String taskId, String processInstanceId, String taskDefinitionKey, String name) {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn(taskId);
        when(task.getProcessInstanceId()).thenReturn(processInstanceId);
        when(task.getTaskDefinitionKey()).thenReturn(taskDefinitionKey);
        when(task.getName()).thenReturn(name);
        return task;
    }

    private MesProBatchRecordExecutionApprovalPageReqVO pageReq() {
        MesProBatchRecordExecutionApprovalPageReqVO reqVO = new MesProBatchRecordExecutionApprovalPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        return reqVO;
    }

    private MesProBatchRecordTemplateDO insertTemplate() {
        return insertTemplate(null);
    }

    private MesProBatchRecordTemplateDO insertTemplate(Long processId) {
        MesProBatchRecordTemplateDO template = MesProBatchRecordTemplateDO.builder()
                .templateCode("BR-TPL-001")
                .templateName("Pressure Pump Template")
                .importId(1L)
                .sort(1)
                .status(0)
                .processId(processId)
                .productName("Pressure Pump")
                .sourceTableIndex(1)
                .tableTitle("Product Information")
                .sheetLayoutJson("{\"rows\":[{\"rowIndex\":0,\"cells\":[{\"columnIndex\":0,\"text\":\"Header\",\"rowSpan\":1,\"colSpan\":2}]}]}")
                .metaJson("{\"source\":\"fixture\"}")
                .remark("")
                .build();
        templateMapper.insert(template);
        return template;
    }

    private MesProWorkOrderDO insertWorkOrder() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .code("MO-20260513-001")
                .name("Pressure Pump WO")
                .batchCode("WO-BATCH-01")
                .status(0)
                .remark("")
                .build();
        workOrderMapper.insert(workOrder);
        return workOrder;
    }

    private MesProBatchRecordReportDO report(String reportId) {
        MesProBatchRecordDefinitionDO definition = batchRecordDefinition("默认电子批记录");
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO version = batchRecordVersion(definition.getId(), "V1.0", "APPROVED", null);
        versionMapper.insert(version);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setSampleKey("sample-key");
        report.setRouteKey("route-key");
        report.setSourceFileName("fixture.docx");
        report.setSourceFileSha256("sha256");
        report.setSourceTableIndex(1);
        report.setTableTitle("Product Information");
        report.setReportId(reportId);
        report.setReportCode("EBR-001");
        report.setReportName("电子批记录");
        report.setReportCategoryId("cat-1");
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(version.getId());
        report.setLastImportTime(LocalDateTime.now());
        return report;
    }

    private MesProBatchRecordDefinitionDO batchRecordDefinition(String batchRecordName) {
        return MesProBatchRecordDefinitionDO.builder()
                .batchRecordName(batchRecordName)
                .routeKey("route-key")
                .build();
    }

    private MesProBatchRecordVersionDO batchRecordVersion(Long definitionId, String versionNo, String status,
                                                          Long sourceVersionId) {
        return MesProBatchRecordVersionDO.builder()
                .definitionId(definitionId)
                .versionNo(versionNo)
                .status(status)
                .sourceVersionId(sourceVersionId)
                .sourceFileName(versionNo + ".doc")
                .sourceFileSha256("sha-" + definitionId + "-" + versionNo)
                .build();
    }

    private void assertReuseForActiveStatus(int status) throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-active-" + status)
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        reportMapper.insert(report("report-active-" + status));

        MesProBatchRecordExecutionDO existing = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-ACTIVE-" + status)
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess.getId())
                .taskId(null)
                .workstationId(null)
                .batchRecordReportId("report-active-" + status)
                .batchCode("BATCH-ACTIVE-" + status)
                .status(status)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson("{\"from\":\"existing\"}")
                .cellValuesJson("[]")
                .remark(null)
                .build();
        executionMapper.insert(existing);

        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", openReqClass);
        Object openReqVO = openReqClass.getDeclaredConstructor().newInstance();
        setValue(openReqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(openReqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(openReqVO, "setTaskId", Long.class, 3003L);
        setValue(openReqVO, "setWorkstationId", Long.class, 4004L);
        setValue(openReqVO, "setBatchRecordReportId", String.class, "report-active-" + status);
        setValue(openReqVO, "setBatchCode", String.class, "BATCH-ACTIVE-" + status);

        Object openResp = openMethod.invoke(executionService, openReqVO);
        assertEquals(existing.getId(), invokeGetter(openResp, "getId"));
        assertEquals(existing.getExecutionCode(), invokeGetter(openResp, "getExecutionCode"));
        assertEquals((long) status, ((Integer) invokeGetter(openResp, "getStatus")).longValue());
        assertEquals(Boolean.FALSE, invokeGetter(openResp, "getCreated"));
        assertEquals(1L, executionMapper.selectCount());
    }

    private void assertCreatesNewExecutionForInactiveRejectedStatus() throws Exception {
        MesProWorkOrderDO workOrder = insertWorkOrder();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .routeId(1001L)
                .processId(2002L)
                .sort(1)
                .batchRecordReportId("report-inactive-rejected")
                .remark("default binding")
                .build();
        routeProcessMapper.insert(routeProcess);
        reportMapper.insert(report("report-inactive-rejected"));
        when(jimuReportGateway.getReportJson("report-inactive-rejected")).thenReturn(sampleEditableReportJson());

        MesProBatchRecordExecutionDO rejected = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-ACTIVE-2")
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess.getId())
                .taskId(3003L)
                .workstationId(4004L)
                .batchRecordReportId("report-inactive-rejected")
                .batchCode("BATCH-ACTIVE-2")
                .status(2)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson("{\"from\":\"existing\"}")
                .cellValuesJson("[]")
                .remark(null)
                .build();
        executionMapper.insert(rejected);

        Class<?> openReqClass = requireClass(
                "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO");
        Method openMethod = requireMethod(MesProBatchRecordExecutionService.class, "openOrCreateByContext", openReqClass);
        Object openReqVO = openReqClass.getDeclaredConstructor().newInstance();
        setValue(openReqVO, "setWorkOrderId", Long.class, workOrder.getId());
        setValue(openReqVO, "setRouteProcessId", Long.class, routeProcess.getId());
        setValue(openReqVO, "setTaskId", Long.class, 3003L);
        setValue(openReqVO, "setWorkstationId", Long.class, 4004L);
        setValue(openReqVO, "setBatchRecordReportId", String.class, "report-inactive-rejected");
        setValue(openReqVO, "setBatchCode", String.class, "BATCH-ACTIVE-2");

        Object openResp = openMethod.invoke(executionService, openReqVO);
        assertNotEquals(rejected.getId(), invokeGetter(openResp, "getId"));
        assertEquals(Boolean.TRUE, invokeGetter(openResp, "getCreated"));
        assertEquals(2L, executionMapper.selectCount());
    }

    private String existingContextKey(Long workOrderId, Long taskId, Long routeProcessId, Long workstationId,
                                      String batchRecordReportId, String batchCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        return (tenantId == null ? "0" : tenantId) + ":" + nullToEmpty(workOrderId) + ":"
                + nullToEmpty(taskId) + ":" + nullToEmpty(routeProcessId) + ":" + nullToEmpty(workstationId)
                + ":" + nullToEmpty(batchRecordReportId) + ":" + nullToEmpty(batchCode);
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sampleEditableReportJson() {
        return sampleEditableReportJsonWithReviewedStringRules();
    }

    private String sampleEditableReportJsonWithoutRules() {
        return """
                {
                  "name":"snapshot-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"操作员"},
                        "1":{"text":"","fillForm":{"field":"ebr_snapshot_r0_c1","component":"Input","componentFlag":"input-text","required":true,"label":"","labelText":"","defaultValue":"OP-001","value":"OP-001"}},
                        "2":{"text":"备注"},
                        "3":{"text":"","fillForm":{"field":"ebr_snapshot_r0_c3","component":"Input","componentFlag":"input-textarea","required":false,"label":"","labelText":""}}
                      },
                      "height":24
                    }
                  },
                  "cols":{
                    "0":{"width":100},
                    "1":{"width":120},
                    "2":{"width":100},
                    "3":{"width":180},
                    "len":4
                  },
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":180,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":500
                }
                """;
    }

    private String sampleEditableReportJsonWithReviewedStringRules() {
        return """
                {
                  "name":"snapshot-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"操作员"},
                        "1":{"text":"","fillForm":{"field":"ebr_snapshot_r0_c1","component":"Input","componentFlag":"input-text","required":true,"label":"","labelText":"","defaultValue":"OP-001","value":"OP-001"},"edhrCellRule":{"rowIndex":0,"columnIndex":1,"valueType":"STRING","componentFlag":"input-text","required":true,"label":"操作员","helpText":"填写实际执行本表单的操作人员姓名或工号","constraints":{},"source":"MANUAL","confidence":1.0,"reviewed":true}},
                        "2":{"text":"备注"},
                        "3":{"text":"","fillForm":{"field":"ebr_snapshot_r0_c3","component":"Input","componentFlag":"input-textarea","required":false,"label":"","labelText":""},"edhrCellRule":{"rowIndex":0,"columnIndex":3,"valueType":"STRING","componentFlag":"input-textarea","required":false,"label":"备注","helpText":"记录本次操作相关的补充说明","constraints":{},"source":"MANUAL","confidence":1.0,"reviewed":true}}
                      },
                      "height":24
                    }
                  },
                  "cols":{
                    "0":{"width":100},
                    "1":{"width":120},
                    "2":{"width":100},
                    "3":{"width":180},
                    "len":4
                  },
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":180,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":500
                }
                """;
    }

    private String sampleLegacyStaticCheckboxReportJson() {
        return """
                {
                  "name":"legacy-static-checkbox-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"物料名称"},
                        "1":{"text":"□30atm压力表"},
                        "2":{"text":"封口热合机：□A05199"},
                        "3":{"text":"☑关键/特殊工序"}
                      },
                      "height":24
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"结果"},
                        "1":{"text":"□是 □否"}
                      },
                      "height":24
                    }
                  },
                  "cols":{
                    "0":{"width":100},
                    "1":{"width":140},
                    "2":{"width":180},
                    "3":{"width":140},
                    "len":4
                  },
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":180,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":560
                }
                """;
    }

    private String sampleEditableReportJsonWithReviewedNumberAndDateRules() {
        return """
                {
                  "name":"snapshot-rule-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"重量（g）"},
                        "1":{"text":"","fillForm":{"field":"ebr_rule_r0_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""},"edhrCellRule":{"rowIndex":0,"columnIndex":1,"valueType":"NUMBER","componentFlag":"input-number","required":true,"label":"重量","constraints":{"min":0,"max":100,"scale":2},"unit":"g","source":"MANUAL","confidence":1.0,"reviewed":true}}
                      },
                      "height":24
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"生产日期"},
                        "1":{"text":"","fillForm":{"field":"ebr_rule_r1_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""},"edhrCellRule":{"rowIndex":1,"columnIndex":1,"valueType":"DATE","componentFlag":"date","required":true,"label":"生产日期","constraints":{"format":"yyyy-MM-dd"},"source":"MANUAL","confidence":1.0,"reviewed":true}}
                      },
                      "height":24
                    }
                  },
                  "cols":{"0":{"width":100},"1":{"width":160},"len":2},
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":260
                }
                """;
    }

    private String sampleEditableReportJsonWithLenKeys() {
        return """
                {
                  "name":"snapshot-len-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"操作员"},
                        "1":{"text":"","fillForm":{"field":"ebr_len_r0_c1","component":"Input","componentFlag":"input-text","required":true,"label":"","labelText":""},"edhrCellRule":{"rowIndex":0,"columnIndex":1,"valueType":"STRING","componentFlag":"input-text","required":true,"label":"操作员","constraints":{},"source":"MANUAL","confidence":1.0,"reviewed":true}},
                        "len":2
                      },
                      "height":24
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"备注"},
                        "1":{"text":"","fillForm":{"field":"ebr_len_r1_c1","component":"Input","componentFlag":"input-textarea","required":false,"label":"","labelText":""},"edhrCellRule":{"rowIndex":1,"columnIndex":1,"valueType":"STRING","componentFlag":"input-textarea","required":false,"label":"备注","constraints":{},"source":"MANUAL","confidence":1.0,"reviewed":true}},
                        "len":2
                      },
                      "height":24
                    },
                    "len":2
                  },
                  "cols":{
                    "0":{"width":100},
                    "1":{"width":180},
                    "len":2
                  },
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":180,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":500
                }
                """;
    }

    private Class<?> requireClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return fail("Expected eDHR execution contract class to exist: " + className, ex);
        }
    }

    private Method requireMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return fail("Expected method to exist: " + type.getName() + "#" + methodName, ex);
        }
    }

    private Method requireGetter(Class<?> type, String methodName) {
        try {
            return type.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            return fail("Expected getter to exist: " + type.getName() + "#" + methodName, ex);
        }
    }

    private void setValue(Object target, String methodName, Class<?> parameterType, Object value) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        } catch (ReflectiveOperationException ex) {
            fail("Expected setter to exist: " + target.getClass().getName() + "#" + methodName, ex);
        }
    }

    private Object invokeGetter(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ex) {
            return fail("Expected getter to exist: " + target.getClass().getName() + "#" + methodName, ex);
        }
    }

    private boolean hasRenderableRows(JSONObject layout) {
        JSONObject rows = layout == null ? null : layout.getJSONObject("rows");
        if (rows == null) {
            return false;
        }
        return rows.keySet().stream()
                .filter(cn.hutool.core.util.StrUtil::isNumeric)
                .map(rows::getJSONObject)
                .anyMatch(row -> row != null && row.getJSONObject("cells") != null
                        && !row.getJSONObject("cells").isEmpty());
    }

    private JSONObject findSnapshotField(JSONArray fields, int rowIndex, int columnIndex) {
        for (int index = 0; index < fields.size(); index++) {
            JSONObject field = fields.getJSONObject(index);
            if (field.getIntValue("rowIndex") == rowIndex
                    && field.getIntValue("columnIndex") == columnIndex) {
                return field;
            }
        }
        throw new AssertionError("missing snapshot field row " + rowIndex + " column " + columnIndex);
    }
}
