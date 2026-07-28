package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCellValueVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCreateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO;
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
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.batch.MesWmBatchDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.batch.MesWmBatchMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordCellRuleSupport;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.BatchRecordCellLinkAutoPersistCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.BatchRecordCellLinkAutoPersistResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProBatchRecordCellLinkAutoPersistService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver.MesProEdhrCandidateUser;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_BATCH_RECORD_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_BATCH_CODE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_DEFAULT_REPORT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_LATEST_PUBLISHED_VERSION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_NOT_UNIQUE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_COUNT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_SIGNATURE_CELL_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REQUIRED_FIELD_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_ROUTE_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SHARED_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;

@Service
public class MesProBatchRecordExecutionServiceImpl implements MesProBatchRecordExecutionService {

    private static final int EXECUTION_STATUS_DRAFT = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT;
    private static final int EXECUTION_STATUS_SUBMITTED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED;
    private static final int EXECUTION_STATUS_REJECTED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_REJECTED;
    private static final int EXECUTION_STATUS_APPROVED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED;
    private static final int EXECUTION_STATUS_FILL_COMPLETED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED;
    private static final List<Integer> ACTIVE_EXECUTION_STATUSES = List.of(
            EXECUTION_STATUS_DRAFT, EXECUTION_STATUS_SUBMITTED);
    private static final String EDHR_PROCESS_DEFINITION_KEY = "mes-edhr-approval-v1";
    private static final String EDHR_APPROVAL_TASK_DEFINITION_KEY = "approveNode";
    private static final String EDHR_APPROVAL_TASK_ASSIGNEE_VARIABLE = "approveNode_assignee";
    private static final String EDHR_BUSINESS_KEY_PREFIX = "EDHR_EXECUTION:";
    private static final String APPROVAL_STATUS_SUBMITTED = MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_SUBMITTED;
    private static final String APPROVAL_STATUS_APPROVED = MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_APPROVED;
    private static final String APPROVAL_STATUS_REJECTED = MesProEdhrApprovalStatusMapping.APPROVAL_STATUS_REJECTED;
    private static final String DOMAIN_TRACE_STATUS_VERIFIED = "VERIFIED";
    private static final String SNAPSHOT_VERSION = "EDHR_EXECUTION_V1";
    private static final DateTimeFormatter EXECUTION_CODE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String LEGACY_SNAPSHOT_PLACEHOLDER = "{}";
    private static final String RECORD_CATEGORY_BATCH = "BATCH_RECORD";
    private static final String RECORD_CATEGORY_INTERNAL = "INTERNAL_RECORD";
    private static final String VALIDATION_PROFILE_BATCH = "CONTROLLED_BATCH";
    private static final String VALIDATION_PROFILE_INTERNAL = "INTERNAL_TRACE";
    private static final String BATCH_RECORD_VERSION_STATUS_APPROVED = "APPROVED";
    private static final String INSTANCE_SCOPE_PROCESS = "PROCESS";
    private static final String INSTANCE_SCOPE_BATCH_SHARED = "BATCH_SHARED";
    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String CANDIDATE_SOURCE_TYPE_USERS = "USERS";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE = "ROLE";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE_GROUP = "ROLE_GROUP";
    private static final String CANDIDATE_SOURCE_TYPE_DEPT = "DEPT";
    private static final String CANDIDATE_SOURCE_TYPE_DEPT_GROUP = "DEPT_GROUP";
    private static final int APPROVAL_FILTER_SCAN_PAGE_SIZE = 100;

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
    private MesProBatchRecordVersionMigrationItemMapper versionMigrationItemMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesWmBatchMapper batchMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureService executionSignatureService;
    @Resource
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @Resource
    private MesProBatchRecordRuntimeSnapshotSupport runtimeSnapshotSupport;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper edhrBatchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper workTaskAssignmentRuleMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    @Resource
    private MesProBatchRecordCellLinkAutoPersistService cellLinkAutoPersistService;
    @Resource
    private MesProBatchRecordDomainTraceService domainTraceService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrPermissionGateService permissionGateService;
    @Resource
    private MesProEdhrCandidateResolver candidateResolver;
    @Resource
    private MesProEdhrPreReleaseEditabilityService preReleaseEditabilityService;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private MesProEdhrRecordbookGlobalSettingService recordbookGlobalSettingService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionCreateRespVO createBatchRecordExecution(MesProBatchRecordExecutionCreateReqVO reqVO) {
        MesProBatchRecordTemplateDO template = templateMapper.selectById(reqVO.getTemplateId());
        if (template == null) {
            throw exception(PRO_BATCH_RECORD_TEMPLATE_NOT_EXISTS);
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(reqVO.getWorkOrderId());
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        String batchCode = StrUtil.trim(reqVO.getBatchCode());
        if (StrUtil.isBlank(batchCode)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_BATCH_CODE_REQUIRED);
        }

        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-PENDING-" + System.nanoTime())
                .templateId(template.getId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .batchCode(batchCode)
                .status(EXECUTION_STATUS_DRAFT)
                .recordCategory(RECORD_CATEGORY_BATCH)
                .validationProfile(VALIDATION_PROFILE_BATCH)
                .recordbookEnabled(Boolean.TRUE)
                .sheetLayoutJson(template.getSheetLayoutJson())
                .metaJson(template.getMetaJson())
                .executionSnapshotJson(buildExecutionSnapshot(template))
                .cellValuesJson("[]")
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .revisionNo(1)
                .activeRevisionFlag(true)
                .remark(null)
                .build();
        executionMapper.insert(execution);

        String executionCode = buildExecutionCode(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionCode(executionCode)
                .setRevisionRootExecutionId(execution.getId()));

        return new MesProBatchRecordExecutionCreateRespVO()
                .setId(execution.getId())
                .setExecutionCode(executionCode)
                .setStatus(EXECUTION_STATUS_DRAFT);
    }

    @Override
    public PageResult<MesProBatchRecordExecutionRespVO> getBatchRecordExecutionPage(MesProBatchRecordExecutionPageReqVO pageReqVO) {
        ActiveContextFilter activeContextFilter = parseActiveContextFilter(pageReqVO.getActiveContextKey());
        if (activeContextFilter.invalid() || activeContextFilter.tenantMismatch()) {
            return PageResult.empty();
        }
        applyActiveContextFilter(pageReqVO, activeContextFilter);
        PageResult<MesProBatchRecordExecutionDO> pageResult = executionMapper.selectPage(pageReqVO);
        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public MesProBatchRecordExecutionRespVO getBatchRecordExecution(Long id) {
        return getBatchRecordExecution(id, null);
    }

    @Override
    public MesProBatchRecordExecutionRespVO getBatchRecordExecution(Long id, Long workTaskId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (workTaskId != null) {
            if (Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED)) {
                preReleaseEditabilityService.requireSubmittedOrdinaryEditable(execution, workTaskId);
            } else {
                workTaskService.validateWritableFillTaskForExecution(workTaskId, execution.getId());
            }
            return buildResp(execution);
        }
        requireExecutionAbility(execution, workTaskId, "VIEW",
                "mes:pro-batch-record-execution:query", "查看 eDHR 执行详情");
        return buildResp(execution);
    }

    @Override
    public void saveBatchRecordExecutionDraft(MesProBatchRecordExecutionSaveDraftReqVO reqVO) {
        if (executionMapper.selectById(reqVO.getId()) == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        validateNoSignatureCellValues(reqVO.getCellValues());
        throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
    }

    private void validateNoSignatureCellValues(List<MesProBatchRecordExecutionCellValueVO> cellValues) {
        if (cellValues == null || cellValues.isEmpty()) {
            return;
        }
        boolean hasSignatureCellValue = cellValues.stream()
                .filter(Objects::nonNull)
                .anyMatch(cellValue -> "SIGNATURE".equalsIgnoreCase(StrUtil.trim(cellValue.getValueType())));
        if (hasSignatureCellValue) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN);
        }
    }

    @Override
    public MesProBatchRecordExecutionEntryContextRespVO getEntryContext(MesProBatchRecordExecutionEntryContextReqVO reqVO) {
        MesProWorkOrderDO workOrder = requireWorkOrder(reqVO.getWorkOrderId());
        MesProRouteProcessDO routeProcess = requireRouteProcess(reqVO.getRouteProcessId(), reqVO.getRouteId(), reqVO.getProcessId());
        String batchRecordReportId = requireRequestedBatchRecordReportId(reqVO.getBatchRecordReportId());
        String batchCode = resolveBatchCode(reqVO.getBatchCode(), workOrder);
        MesProBatchRecordReportDO report = requireBatchRecordReport(batchRecordReportId);
        MesProRouteDO route = routeProcess.getRouteId() == null ? null : routeMapper.selectById(routeProcess.getRouteId());
        MesProProcessDO process = routeProcess.getProcessId() == null ? null : processMapper.selectById(routeProcess.getProcessId());
        MesProBatchRecordExecutionDO activeExecution = executionMapper.selectActiveByContext(
                workOrder.getId(), routeProcess.getId(), batchRecordReportId, batchCode, ACTIVE_EXECUTION_STATUSES);
        String activeContextKey = buildActiveContextKey(workOrder.getId(), null, routeProcess.getId(),
                null, batchRecordReportId, batchCode);
        return new MesProBatchRecordExecutionEntryContextRespVO()
                .setWorkOrderId(workOrder.getId())
                .setRouteId(routeProcess.getRouteId())
                .setRouteCode(route == null ? null : route.getCode())
                .setRouteName(route == null ? null : route.getName())
                .setProcessId(routeProcess.getProcessId())
                .setProcessCode(process == null ? null : process.getCode())
                .setProcessName(process == null ? null : process.getName())
                .setRouteProcessId(routeProcess.getId())
                .setTaskId(null)
                .setWorkstationId(null)
                .setWorkstationCode(null)
                .setWorkstationName(null)
                .setBatchRecordReportId(batchRecordReportId)
                .setBatchRecordReportCode(report.getReportCode())
                .setBatchRecordReportName(report.getReportName())
                .setBatchCode(batchCode)
                .setCanOpen(Boolean.TRUE)
                .setBindingResolved(Boolean.TRUE)
                .setActiveExecutionId(activeExecution == null ? null : activeExecution.getId())
                .setActiveExecutionStatus(activeExecution == null ? null : activeExecution.getStatus())
                .setActiveContextKey(activeContextKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionOpenOrCreateByContextRespVO openOrCreateByContext(
            MesProBatchRecordExecutionOpenOrCreateByContextReqVO reqVO) {
        MesProWorkOrderDO workOrder = requireWorkOrder(reqVO.getWorkOrderId());
        String instanceScope = resolveExecutionInstanceScope(reqVO.getInstanceScope());
        String sharedFormKey = StrUtil.blankToDefault(StrUtil.trim(reqVO.getSharedFormKey()), null);
        MesProRouteProcessDO routeProcess = INSTANCE_SCOPE_BATCH_SHARED.equals(instanceScope)
                ? resolveBatchSharedTraceRouteProcess(reqVO)
                : requireRouteProcess(reqVO.getRouteProcessId(), reqVO.getRouteId(), reqVO.getProcessId());
        if (INSTANCE_SCOPE_BATCH_SHARED.equals(instanceScope)
                && (reqVO.getBatchExecutionId() == null || StrUtil.isBlank(sharedFormKey))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SHARED_CONTEXT_REQUIRED);
        }
        String batchRecordReportId = requireRequestedBatchRecordReportId(reqVO.getBatchRecordReportId());
        MesProBatchRecordReportDO report = requireBatchRecordReport(batchRecordReportId);
        String batchCode = resolveBatchCode(reqVO.getBatchCode(), workOrder);
        MesProBatchRecordExecutionDO existing = INSTANCE_SCOPE_BATCH_SHARED.equals(instanceScope)
                ? executionMapper.selectActiveByBatchShared(reqVO.getBatchExecutionId(), sharedFormKey, batchCode,
                        ACTIVE_EXECUTION_STATUSES)
                : executionMapper.selectActiveByContext(
                        reqVO.getBatchExecutionId(), reqVO.getTaskId(),
                        reqVO.getWorkstationId(),
                        workOrder.getId(), routeProcess.getId(), batchRecordReportId, batchCode,
                        ACTIVE_EXECUTION_STATUSES);
        if (existing != null) {
            BatchRecordCellLinkAutoPersistResult autoPersist = cellLinkAutoPersistService.autoPersist(
                    new BatchRecordCellLinkAutoPersistCommand()
                            .setExecutionId(existing.getId())
                            .setTrigger("EXECUTION_OPEN_OR_CREATE_EXISTING"));
            return buildOpenOrCreateResp(existing, false).setCellLinkAutoPersist(autoPersist);
        }
        validateLatestPublishedBatchRecordReport(report);
        RuntimeSnapshot runtimeSnapshot = buildRuntimeSnapshotFromReport(report);
        Long routeId = routeProcess == null ? reqVO.getRouteId() : routeProcess.getRouteId();
        String activeContextKey = INSTANCE_SCOPE_BATCH_SHARED.equals(instanceScope)
                ? buildBatchSharedActiveContextKey(workOrder.getId(), reqVO.getBatchExecutionId(),
                        batchRecordReportId, sharedFormKey, batchCode)
                : buildActiveContextKey(workOrder.getId(), reqVO.getTaskId(), routeProcess.getId(),
                        reqVO.getWorkstationId(), batchRecordReportId, batchCode);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-PENDING-" + System.nanoTime())
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .routeProcessId(routeProcess == null ? null : routeProcess.getId())
                .taskId(reqVO.getTaskId())
                .workstationId(reqVO.getWorkstationId())
                .batchRecordReportId(batchRecordReportId)
                .batchRecordDefinitionId(report.getBatchRecordDefinitionId())
                .batchRecordVersionId(report.getBatchRecordVersionId())
                .batchExecutionId(reqVO.getBatchExecutionId())
                .routeId(routeId)
                .instanceScope(instanceScope)
                .sharedFormKey(sharedFormKey)
                .formSlotType(reqVO.getFormSlotType())
                .recordCategory(resolveRecordCategory(reqVO.getRecordCategory()))
                .validationProfile(resolveValidationProfile(reqVO.getRecordCategory(), reqVO.getValidationProfile()))
                .recordbookEnabled(resolveRecordbookEnabled(reqVO.getRecordbookEnabled(), reqVO.getRecordCategory()))
                .permissionScopeId(reqVO.getPermissionScopeId())
                .routeBindingId(reqVO.getRouteBindingId())
                .routeBindingSnapshotHash(reqVO.getRouteBindingSnapshotHash())
                .archiveVisibility(reqVO.getArchiveVisibility())
                .slotConfigSnapshotHash(reqVO.getSlotConfigSnapshotHash())
                .batchCode(batchCode)
                .status(EXECUTION_STATUS_DRAFT)
                .sheetLayoutJson(runtimeSnapshot.sheetLayoutJson())
                .metaJson(runtimeSnapshot.metaJson())
                .executionSnapshotJson(runtimeSnapshot.executionSnapshotJson())
                .cellValuesJson("[]")
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .revisionNo(1)
                .activeContextKey(activeContextKey)
                .activeRevisionFlag(true)
                .remark(null)
                .build();
        executionMapper.insert(execution);
        String executionCode = buildExecutionCode(execution.getId());
        execution.setExecutionCode(executionCode);
        execution.setRevisionRootExecutionId(execution.getId());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setExecutionCode(executionCode)
                .setRevisionRootExecutionId(execution.getId()));
        BatchRecordCellLinkAutoPersistResult autoPersist = cellLinkAutoPersistService.autoPersist(
                new BatchRecordCellLinkAutoPersistCommand()
                        .setExecutionId(execution.getId())
                        .setTrigger("EXECUTION_CREATE"));
        return buildOpenOrCreateResp(execution, true).setCellLinkAutoPersist(autoPersist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBatchRecordExecution(MesProBatchRecordExecutionSubmitReqVO reqVO) {
        MesProBatchRecordExecutionDO execution = getSubmittableExecution(reqVO.getId());
        boolean preReleaseResubmit = Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED);
        Long userId = requireLoginUserId();
        if (goldenFingerPermissionService.hasGoldenFingerPermission(userId)) {
            if (preReleaseResubmit) {
                preReleaseEditabilityService.requireSubmittedOrdinaryGoldenFingerEditable(execution, reqVO.getWorkTaskId());
            } else {
                workTaskService.validateGoldenFingerFillTaskForExecution(reqVO.getWorkTaskId(), execution.getId());
            }
            submitGoldenFingerFill(reqVO, execution, userId, !preReleaseResubmit);
            return;
        }
        if (preReleaseResubmit) {
            preReleaseEditabilityService.requireSubmittedOrdinaryEditable(execution, reqVO.getWorkTaskId());
        } else {
            requireSubmitAbilityOrFillTask(execution, reqVO.getWorkTaskId());
        }
        MesProBatchRecordDomainTraceDetailRespVO domainTrace = domainTraceService.verifyForSubmit(execution.getId());
        ApprovalExecutionContext approvalContext = validateApprovalSubmissionPrerequisites(execution);
        boolean ordinaryProcessSubmit = isRouteBoundBatchExecutionForm(execution) || !hasReviewSignatureAssignments(execution);
        if (ordinaryProcessSubmit) {
            submitOrdinaryProcessFill(reqVO, execution, approvalContext, domainTrace, !preReleaseResubmit);
            return;
        }
        if (preReleaseResubmit) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        List<ReviewSignatureAssignment> reviewAssignments = resolveReviewSignatureAssignments(execution);
        reviewAssignments = applyReviewAssigneeSelections(reviewAssignments, reqVO.getReviewAssigneeSelections());
        LocalDateTime now = LocalDateTime.now();
        MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand =
                buildSignatureTimeCommand(reqVO.getSignatureTime());
        Long submitSignatureId = signatureTimeCommand == null
                ? executionSignatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment())
                : executionSignatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment(),
                        signatureTimeCommand);
        String snapshotJson = buildApprovalSnapshotJson(execution, approvalContext, domainTrace, now, userId,
                reviewAssignments);
        BusinessApprovalRequest approvalRequest = businessApprovalOrchestrator.submit(BusinessApprovalContext.builder()
                .tenantId(approvalContext.tenantId())
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .objectId(String.valueOf(execution.getId()))
                .objectVersion(String.valueOf(execution.getRevisionNo()))
                .actionCode("SUBMIT_REVIEW")
                .objectState("DRAFT")
                .applicantUserId(userId)
                .reason(reqVO.getComment())
                .startUserSelectAssignees(Map.of(EDHR_APPROVAL_TASK_DEFINITION_KEY,
                        distinctCandidateUserIds(reviewAssignments)))
                .variables(buildBusinessApprovalVariables(execution, approvalContext, domainTrace, snapshotJson,
                        now, userId, submitSignatureId, reqVO.getWorkTaskId(), reviewAssignments))
                .build());
        if (approvalRequest == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS);
        }
    }

    private void submitGoldenFingerFill(MesProBatchRecordExecutionSubmitReqVO reqVO,
                                        MesProBatchRecordExecutionDO execution,
                                        Long userId,
                                        boolean completeFillTask) {
        FieldAuditEvidence fieldAuditEvidence = validateFieldAuditEvidence(execution);
        LocalDateTime now = LocalDateTime.now();
        MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand =
                buildSignatureTimeCommand(reqVO.getSignatureTime());
        Long submitSignatureId = signatureTimeCommand == null
                ? executionSignatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment())
                : executionSignatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment(),
                signatureTimeCommand);
        executionSignatureService.bindSignatureFieldAuditEvidence(submitSignatureId, execution.getId(),
                fieldAuditEvidence.fieldAuditRevision(), fieldAuditEvidence.fieldAuditHeadHash(),
                fieldAuditEvidence.cellValuesHash());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(EXECUTION_STATUS_FILL_COMPLETED)
                .setSubmittedBy(userId)
                .setSubmittedAt(now)
                .setClosedAt(now)
                .setActiveContextKey(null));
        executionMapper.clearActiveContextKey(execution.getId());
        if (completeFillTask) {
            workTaskService.completeFillAndCreateNextFillAfterGoldenFingerSubmit(reqVO.getWorkTaskId(), execution.getId());
        }
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-GF-" + IdWorker.getId())
                .setObjectType("BATCH_RECORD_EXECUTION")
                .setObjectId(String.valueOf(execution.getId()))
                .setBatchExecutionId(execution.getId())
                .setExecutionId(execution.getId())
                .setWorkTaskId(reqVO.getWorkTaskId())
                .setRouteProcessId(execution.getRouteProcessId())
                .setReportId(execution.getBatchRecordReportId())
                .setRecordCategory(execution.getRecordCategory())
                .setOperationType("GOLDEN_FINGER_SUBMIT")
                .setActionName("批记录金手指提交执行")
                .setActorUserId(userId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(MesProEdhrGoldenFingerPermissionService.PERMISSION)
                .setPermissionDecision("ALLOW_GOLDEN_FINGER")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(execution.getFieldAuditHeadHash())
                .setAfterSummaryHash(fieldAuditEvidence.fieldAuditHeadHash())
                .setMetadataJson(buildGoldenFingerSubmitMetadata(execution, fieldAuditEvidence, submitSignatureId,
                        completeFillTask)));
    }

    private void submitOrdinaryProcessFill(MesProBatchRecordExecutionSubmitReqVO reqVO,
                                           MesProBatchRecordExecutionDO execution,
                                           ApprovalExecutionContext approvalContext,
                                           MesProBatchRecordDomainTraceDetailRespVO domainTrace,
                                           boolean completeFillTask) {
        Long userId = requireLoginUserId();
        LocalDateTime now = LocalDateTime.now();
        MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand =
                buildSignatureTimeCommand(reqVO.getSignatureTime());
        Long submitSignatureId = signatureTimeCommand == null
                ? executionSignatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment())
                : executionSignatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment(),
                        signatureTimeCommand);
        executionSignatureService.bindSignatureFieldAuditEvidence(submitSignatureId, execution.getId(),
                approvalContext.fieldAuditRevision(), approvalContext.fieldAuditHeadHash(), approvalContext.cellValuesHash());
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(EXECUTION_STATUS_FILL_COMPLETED)
                .setSubmittedBy(userId)
                .setSubmittedAt(now)
                .setClosedAt(now)
                .setDomainTraceSnapshotId(domainTrace.getDomainTraceSnapshotId())
                .setDomainTraceHash(domainTrace.getDomainTraceHash())
                .setDomainTraceStatus(domainTrace.getStatus())
                .setDomainTraceVerifiedAt(now)
                .setActiveContextKey(null));
        executionMapper.clearActiveContextKey(execution.getId());
        if (completeFillTask) {
            workTaskService.completeFillAndCreateNextFillAfterOrdinarySubmit(reqVO.getWorkTaskId(), execution.getId());
        }
    }

    private String buildGoldenFingerSubmitMetadata(MesProBatchRecordExecutionDO execution,
                                                   FieldAuditEvidence fieldAuditEvidence,
                                                   Long submitSignatureId,
                                                   boolean completedFillTask) {
        JSONObject metadata = new JSONObject(true);
        metadata.put("mode", "GOLDEN_FINGER_TEST");
        metadata.put("scope", "PRE_RELEASE_FORM_FILL_SUBMIT");
        metadata.put("executionStatusBefore", execution.getStatus());
        metadata.put("submitSignatureId", submitSignatureId);
        metadata.put("cellValuesHash", fieldAuditEvidence.cellValuesHash());
        metadata.put("fieldAuditRevision", fieldAuditEvidence.fieldAuditRevision());
        metadata.put("fieldAuditHeadHash", fieldAuditEvidence.fieldAuditHeadHash());
        metadata.put("completedFillTask", completedFillTask);
        JSONArray bypassedChecks = new JSONArray();
        bypassedChecks.add("ASSIGNEE");
        bypassedChecks.add("REQUIRED_FIELDS");
        bypassedChecks.add("SIGNATURE_COMPLETENESS");
        bypassedChecks.add("ATTACHMENT_REQUIREMENTS");
        bypassedChecks.add("DOMAIN_TRACE");
        bypassedChecks.add("REVIEW_ASSIGNMENT");
        bypassedChecks.add("ACTION_LOCKS");
        metadata.put("bypassedChecks", bypassedChecks);
        return metadata.toJSONString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionFormReviewSignRespVO cosignBatchRecordExecution(
            MesProBatchRecordExecutionFormReviewSignReqVO reqVO) {
        MesProBatchRecordExecutionDO execution = getActiveExecution(reqVO.getExecutionId());
        requireExecutionAbility(execution, reqVO.getWorkTaskId(), "SIGN",
                "mes:pro-batch-record-execution:cosign", "电子批记录表单复核签名");
        workTaskService.validateWritableFillTaskForExecution(reqVO.getWorkTaskId(), execution.getId());
        FieldAuditEvidence fieldAuditEvidence = validateFieldAuditEvidence(execution);
        MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand =
                buildSignatureTimeCommand(reqVO.getSignatureTime());
        Long signatureId = executionSignatureService.recordFormReviewSignature(execution.getId(), reqVO.getPassword(),
                reqVO.getComment(), fieldAuditEvidence.fieldAuditRevision(), fieldAuditEvidence.fieldAuditHeadHash(),
                fieldAuditEvidence.cellValuesHash(), signatureTimeCommand);
        return new MesProBatchRecordExecutionFormReviewSignRespVO()
                .setExecutionId(execution.getId())
                .setStatus(execution.getStatus())
                .setSignatureId(signatureId)
                .setActionType(MesProBatchRecordExecutionSignatureService.ACTION_FORM_REVIEW)
                .setMeaningText(resolveSignatureMeaning(MesProBatchRecordExecutionSignatureService.ACTION_FORM_REVIEW))
                .setCellValuesHash(fieldAuditEvidence.cellValuesHash())
                .setFieldAuditRevision(fieldAuditEvidence.fieldAuditRevision())
                .setFieldAuditHeadHash(fieldAuditEvidence.fieldAuditHeadHash());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionApprovalActionRespVO approveBatchRecordExecution(MesProBatchRecordExecutionApproveReqVO reqVO) {
        MesProBatchRecordExecutionDO execution = getSubmittedExecution(reqVO.getExecutionId());
        MesProEdhrWorkTaskDO approvalActionTask = workTaskService.validateWritableReviewOrApproveTask(
                reqVO.getWorkTaskId(), execution.getId());
        String approvalAbility = resolveApprovalActionAbility(approvalActionTask);
        requireExecutionAbility(execution, reqVO.getWorkTaskId(), approvalAbility,
                "mes:pro-batch-record-execution:approve", resolveApprovalActionName(approvalAbility));
        MesProBatchRecordApprovalSnapshotDO snapshot = getApprovalSnapshot(execution.getId());
        validateActionRequestContract(reqVO.getProcessInstanceId(), reqVO.getApprovalSnapshotId(),
                reqVO.getApprovalSnapshotHash(), execution, snapshot);
        FieldAuditEvidence fieldAuditEvidence = validateSubmittedSnapshotFieldAuditEvidence(execution, snapshot);
        String domainTraceHash = requireLockedDomainTraceHash(snapshot);
        Task task = validateApprovalTask(execution, snapshot, approvalActionTask.getBpmTaskId(), reqVO.getBpmTaskId());
        Long userId = requireLoginUserId();
        domainTraceService.verifyForApproval(execution.getId(), domainTraceHash);
        Long signatureId = executionSignatureService.recordApprovalSignature(new MesProBatchRecordExecutionApprovalSignatureCommand()
                .setExecutionId(execution.getId())
                .setPassword(reqVO.getPassword())
                .setComment(reqVO.getComment())
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setBpmTaskId(task.getId())
                .setBpmTaskDefinitionKey(task.getTaskDefinitionKey())
                .setBpmTaskName(task.getName())
                .setSignatureCellKey(approvalActionTask.getSignatureCellKey())
                .setSignatureRowIndex(approvalActionTask.getSignatureRowIndex())
                .setSignatureColumnIndex(approvalActionTask.getSignatureColumnIndex())
                .setReviewSourceType(approvalActionTask.getReviewSourceType())
                .setReviewSourceId(approvalActionTask.getReviewSourceId())
                .setReviewSourceName(approvalActionTask.getReviewSourceName())
                .setApprovalResult(resolveApprovalSignatureAction(approvalActionTask))
                .setFieldAuditRevision(fieldAuditEvidence.fieldAuditRevision())
                .setFieldAuditHeadHash(fieldAuditEvidence.fieldAuditHeadHash())
                .setCellValuesHash(fieldAuditEvidence.cellValuesHash())
                .setSignatureTimeCommand(buildSignatureTimeCommand(reqVO.getSignatureTime())));
        if (Objects.equals(approvalActionTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)) {
            MesProEdhrWorkTaskDO completedReviewTask = workTaskService.completeOneReviewTask(reqVO.getWorkTaskId(), execution.getId());
            if (workTaskService.hasActiveReviewTasks(execution.getId())) {
                return new MesProBatchRecordExecutionApprovalActionRespVO()
                        .setExecutionId(execution.getId())
                        .setStatus(MesProEdhrApprovalStatusMapping.executionStatusAfterReviewApproval(
                                MesProEdhrWorkTaskStatus.DONE))
                        .setResultType(MesProEdhrApprovalStatusMapping.resolveReviewApprovalResultType(true))
                        .setProcessInstanceId(execution.getProcessInstanceId())
                        .setBpmTaskId(task.getId())
                        .setSignatureId(signatureId)
                        .setTrackingEventId(signatureId);
            }
            MesProEdhrWorkTaskDO approveTask = workTaskService.createApproveTaskAfterReview(completedReviewTask);
            return new MesProBatchRecordExecutionApprovalActionRespVO()
                    .setExecutionId(execution.getId())
                    .setStatus(MesProEdhrApprovalStatusMapping.executionStatusAfterReviewApproval(
                            MesProEdhrWorkTaskStatus.DONE))
                    .setResultType(MesProEdhrApprovalStatusMapping.resolveReviewApprovalResultType(false))
                    .setProcessInstanceId(execution.getProcessInstanceId())
                    .setBpmTaskId(task.getId())
                    .setSignatureId(signatureId)
                    .setTrackingEventId(signatureId)
                    .setApproveTaskId(approveTask == null ? null : approveTask.getId());
        }
        MesProEdhrWorkTaskDO completedApproveTask = workTaskService.completeApproveTask(reqVO.getWorkTaskId(), execution.getId());
        BpmTaskApproveReqVO approveReqVO = new BpmTaskApproveReqVO();
        approveReqVO.setId(task.getId());
        approveReqVO.setReason(reqVO.getComment());
        bpmTaskService.approveTask(userId, approveReqVO);
        LocalDateTime now = LocalDateTime.now();
        approvalSnapshotMapper.approveAndClearCurrentBpmTask(new MesProBatchRecordApprovalSnapshotDO()
                .setId(snapshot.getId())
                .setApprovalStatus(APPROVAL_STATUS_APPROVED)
                .setApproveSignatureId(signatureId)
                .setApprovedBy(userId)
                .setApprovedAt(now)
                .setClosedAt(now));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(EXECUTION_STATUS_APPROVED)
                .setApprovedBy(userId)
                .setApprovedAt(now)
                .setClosedAt(now));
        executionMapper.clearActiveContextKey(execution.getId());
        workTaskService.createNextFillAfterReview(completedApproveTask);
        return new MesProBatchRecordExecutionApprovalActionRespVO()
                .setExecutionId(execution.getId())
                .setStatus(MesProEdhrApprovalStatusMapping.executionStatusAfterFinalApproval(
                        MesProEdhrWorkTaskStatus.DONE))
                .setResultType(MesProEdhrApprovalStatusMapping.resolveApproveResultType())
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setBpmTaskId(task.getId())
                .setSignatureId(signatureId)
                .setTrackingEventId(signatureId)
                .setClosedAt(now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionApprovalActionRespVO rejectBatchRecordExecution(MesProBatchRecordExecutionRejectReqVO reqVO) {
        String rejectReason = StrUtil.trim(reqVO.getReason());
        if (StrUtil.isBlank(rejectReason)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_REASON_REQUIRED);
        }
        MesProBatchRecordExecutionDO execution = getSubmittedExecution(reqVO.getExecutionId());
        MesProEdhrWorkTaskDO reviewTask = workTaskService.validateWritableTask(reqVO.getWorkTaskId(), execution.getId(),
                MesProEdhrWorkTaskService.TASK_TYPE_REVIEW);
        requireExecutionAbility(execution, reqVO.getWorkTaskId(), "REVIEW",
                "mes:pro-batch-record-execution:approve", "电子批记录审核驳回");
        workTaskService.requireReworkAssigneeUserId(reqVO.getWorkTaskId(), execution.getId());
        MesProBatchRecordApprovalSnapshotDO snapshot = getApprovalSnapshot(execution.getId());
        validateActionRequestContract(reqVO.getProcessInstanceId(), reqVO.getApprovalSnapshotId(),
                reqVO.getApprovalSnapshotHash(), execution, snapshot);
        FieldAuditEvidence fieldAuditEvidence = validateSubmittedSnapshotFieldAuditEvidence(execution, snapshot);
        String domainTraceHash = requireLockedDomainTraceHash(snapshot);
        Task task = validateApprovalTask(execution, snapshot, reviewTask.getBpmTaskId(), reqVO.getBpmTaskId());
        Long userId = requireLoginUserId();
        domainTraceService.verifyForApproval(execution.getId(), domainTraceHash);
        Long signatureId = executionSignatureService.recordApprovalSignature(new MesProBatchRecordExecutionApprovalSignatureCommand()
                .setExecutionId(execution.getId())
                .setPassword(reqVO.getPassword())
                .setComment(rejectReason)
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setBpmTaskId(task.getId())
                .setBpmTaskDefinitionKey(task.getTaskDefinitionKey())
                .setBpmTaskName(task.getName())
                .setSignatureCellKey(reviewTask.getSignatureCellKey())
                .setSignatureRowIndex(reviewTask.getSignatureRowIndex())
                .setSignatureColumnIndex(reviewTask.getSignatureColumnIndex())
                .setReviewSourceType(reviewTask.getReviewSourceType())
                .setReviewSourceId(reviewTask.getReviewSourceId())
                .setReviewSourceName(reviewTask.getReviewSourceName())
                .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_REJECT)
                .setReason(rejectReason)
                .setFieldAuditRevision(fieldAuditEvidence.fieldAuditRevision())
                .setFieldAuditHeadHash(fieldAuditEvidence.fieldAuditHeadHash())
                .setCellValuesHash(fieldAuditEvidence.cellValuesHash())
                .setSignatureTimeCommand(buildSignatureTimeCommand(reqVO.getSignatureTime())));
        BpmTaskRejectReqVO rejectReqVO = new BpmTaskRejectReqVO();
        rejectReqVO.setId(task.getId());
        rejectReqVO.setReason(rejectReason);
        bpmTaskService.rejectTask(userId, rejectReqVO);
        LocalDateTime now = LocalDateTime.now();
        approvalSnapshotMapper.updateById(new MesProBatchRecordApprovalSnapshotDO()
                .setId(snapshot.getId())
                .setApprovalStatus(APPROVAL_STATUS_REJECTED)
                .setRejectSignatureId(signatureId)
                .setRejectedBy(userId)
                .setRejectedAt(now)
                .setRejectReason(rejectReason)
                .setCurrentBpmTaskId(null)
                .setCurrentTaskDefinitionKey(null));
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(EXECUTION_STATUS_REJECTED)
                .setRejectedBy(userId)
                .setRejectedAt(now)
                .setRejectReason(rejectReason)
                .setActiveRevisionFlag(false));
        executionMapper.clearActiveContextKey(execution.getId());
        MesProBatchRecordExecutionDO revision = createReworkRevision(execution, snapshot, rejectReason);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setSupersededByExecutionId(revision.getId()));
        rebindBatchTaskToRevision(execution.getId(), revision.getId());
        MesProEdhrWorkTaskDO reworkTask = workTaskService.completeReviewAndCreateRework(
                reqVO.getWorkTaskId(), execution.getId(), revision.getId(), rejectReason);
        return new MesProBatchRecordExecutionApprovalActionRespVO()
                .setExecutionId(execution.getId())
                .setRevisionExecutionId(revision.getId())
                .setReworkTaskId(reworkTask.getId())
                .setStatus(MesProEdhrApprovalStatusMapping.executionStatusAfterReviewReject(
                        MesProEdhrWorkTaskStatus.DONE))
                .setResultType(MesProEdhrApprovalStatusMapping.resolveReviewRejectResultType())
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setBpmTaskId(task.getId())
                .setSignatureId(signatureId)
                .setTrackingEventId(signatureId)
                .setRejectedAt(now);
    }

    private MesProBatchRecordExecutionDO createReworkRevision(MesProBatchRecordExecutionDO rejected,
                                                             MesProBatchRecordApprovalSnapshotDO snapshot,
                                                             String rejectReason) {
        Long revisionRootExecutionId = rejected.getRevisionRootExecutionId() == null
                ? rejected.getId() : rejected.getRevisionRootExecutionId();
        int nextRevisionNo = rejected.getRevisionNo() == null ? 2 : rejected.getRevisionNo() + 1;
        String activeContextKey = buildActiveContextKey(rejected.getWorkOrderId(), null,
                rejected.getRouteProcessId(), null,
                rejected.getBatchRecordReportId(), rejected.getBatchCode());
        MesProBatchRecordExecutionDO revision = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-PENDING-" + System.nanoTime())
                .templateId(rejected.getTemplateId())
                .templateCode(rejected.getTemplateCode())
                .templateName(rejected.getTemplateName())
                .workOrderId(rejected.getWorkOrderId())
                .workOrderCode(rejected.getWorkOrderCode())
                .routeProcessId(rejected.getRouteProcessId())
                .taskId(null)
                .workstationId(null)
                .batchRecordReportId(rejected.getBatchRecordReportId())
                .formSlotType(rejected.getFormSlotType())
                .recordCategory(rejected.getRecordCategory())
                .validationProfile(rejected.getValidationProfile())
                .permissionScopeId(rejected.getPermissionScopeId())
                .routeBindingId(rejected.getRouteBindingId())
                .routeBindingSnapshotHash(rejected.getRouteBindingSnapshotHash())
                .archiveVisibility(rejected.getArchiveVisibility())
                .slotConfigSnapshotHash(rejected.getSlotConfigSnapshotHash())
                .batchCode(rejected.getBatchCode())
                .status(EXECUTION_STATUS_DRAFT)
                .sheetLayoutJson(rejected.getSheetLayoutJson())
                .metaJson(rejected.getMetaJson())
                .executionSnapshotJson(rejected.getExecutionSnapshotJson())
                .cellValuesJson(rejected.getCellValuesJson())
                .cellValuesHash(rejected.getCellValuesHash())
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .fieldAuditLastBatchId(null)
                .activeContextKey(activeContextKey)
                .revisionRootExecutionId(revisionRootExecutionId)
                .revisionNo(nextRevisionNo)
                .sourceRejectedExecutionId(rejected.getId())
                .revisionReason(rejectReason)
                .revisionParentHash(resolveRevisionParentHash(rejected, snapshot))
                .activeRevisionFlag(true)
                .domainTraceSnapshotId(rejected.getDomainTraceSnapshotId())
                .domainTraceHash(rejected.getDomainTraceHash())
                .domainTraceStatus(rejected.getDomainTraceStatus())
                .domainTraceVerifiedAt(rejected.getDomainTraceVerifiedAt())
                .remark(rejected.getRemark())
                .build();
        executionMapper.insert(revision);
        String executionCode = buildExecutionCode(revision.getId());
        revision.setExecutionCode(executionCode);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(revision.getId())
                .setExecutionCode(executionCode));
        return revision;
    }

    private String resolveRevisionParentHash(MesProBatchRecordExecutionDO rejected,
                                             MesProBatchRecordApprovalSnapshotDO snapshot) {
        if (snapshot != null && StrUtil.isNotBlank(snapshot.getSnapshotHash())) {
            return snapshot.getSnapshotHash();
        }
        String evidence = rejected.getId() + ":" + StrUtil.nullToEmpty(rejected.getCellValuesHash())
                + ":" + rejected.getFieldAuditRevision()
                + ":" + StrUtil.nullToEmpty(rejected.getFieldAuditHeadHash());
        return DigestUtil.sha256Hex(evidence);
    }

    private void rebindBatchTaskToRevision(Long rejectedExecutionId, Long revisionExecutionId) {
        MesProEdhrBatchExecutionTaskDO batchTask = batchExecutionTaskMapper.selectByExecutionId(rejectedExecutionId);
        if (batchTask == null) {
            return;
        }
        batchExecutionTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(batchTask.getId())
                .setExecutionId(revisionExecutionId)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REWORK_REQUIRED));
    }

    @Override
    public PageResult<MesProBatchRecordExecutionApprovalRespVO> getApprovalPendingPage(MesProBatchRecordExecutionApprovalPageReqVO pageReqVO) {
        Long userId = requireLoginUserId();
        if (hasApprovalPageFilter(pageReqVO)) {
            return getFilteredApprovalPage(pageReqVO,
                    bpmReqVO -> bpmTaskService.getTaskTodoPage(userId, bpmReqVO),
                    this::buildPendingApprovalResp);
        }
        PageResult<Task> taskPage = bpmTaskService.getTaskTodoPage(userId,
                buildApprovalBpmPageReqVO(pageReqVO.getPageNo(), pageReqVO.getPageSize()));
        List<MesProBatchRecordExecutionApprovalRespVO> list = taskPage.getList().stream()
                .map(this::buildPendingApprovalResp)
                .toList();
        return new PageResult<>(list, taskPage.getTotal());
    }

    @Override
    public PageResult<MesProBatchRecordExecutionApprovalRespVO> getApprovalDonePage(MesProBatchRecordExecutionApprovalPageReqVO pageReqVO) {
        Long userId = requireLoginUserId();
        if (hasApprovalPageFilter(pageReqVO)) {
            return getFilteredApprovalPage(pageReqVO,
                    bpmReqVO -> bpmTaskService.getTaskDonePage(userId, bpmReqVO),
                    this::buildDoneApprovalResp);
        }
        PageResult<HistoricTaskInstance> taskPage = bpmTaskService.getTaskDonePage(userId,
                buildApprovalBpmPageReqVO(pageReqVO.getPageNo(), pageReqVO.getPageSize()));
        List<MesProBatchRecordExecutionApprovalRespVO> list = taskPage.getList().stream()
                .map(this::buildDoneApprovalResp)
                .toList();
        return new PageResult<>(list, taskPage.getTotal());
    }

    @Override
    public MesProBatchRecordExecutionApprovalRespVO getApprovalDetail(Long id, String bpmTaskId, Long workTaskId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        validateApprovalEvidenceJson(execution.getExecutionSnapshotJson());
        validateApprovalEvidenceJson(execution.getCellValuesJson());
        MesProBatchRecordApprovalSnapshotDO snapshot = getApprovalSnapshot(execution.getId());
        MesProBatchRecordExecutionApprovalRespVO respVO = buildApprovalResp(execution, snapshot);
        MesProEdhrWorkTaskDO approvalActionTask = null;
        if (workTaskId != null) {
            approvalActionTask = workTaskService.getAssignedReviewOrApproveTaskForDetail(workTaskId, execution.getId());
            fillWorkTaskFields(respVO, approvalActionTask);
        }
        if (Integer.valueOf(EXECUTION_STATUS_SUBMITTED).equals(execution.getStatus())) {
            if (approvalActionTask == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
            }
            String expectedBpmTaskId = approvalActionTask.getBpmTaskId();
            Task task = validateApprovalTask(execution, snapshot, expectedBpmTaskId, bpmTaskId);
            fillTaskFields(respVO, task);
            respVO.setCanApprove(Boolean.TRUE);
            respVO.setCanReject(Objects.equals(approvalActionTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW));
        } else {
            respVO.setCanApprove(Boolean.FALSE);
            respVO.setCanReject(Boolean.FALSE);
        }
        return respVO;
    }

    private void cancelCreatedProcessInstance(Long userId, String processInstanceId, Long executionId,
                                              RuntimeException submitFailure) {
        try {
            processInstanceApi.cancelProcessInstance(userId, processInstanceId,
                    "eDHR submit compensation: executionId=" + executionId);
        } catch (RuntimeException compensationFailure) {
            compensationFailure.addSuppressed(submitFailure);
            throw compensationFailure;
        }
    }

    @Override
    public PageResult<MesProBatchRecordExecutionTrackingRespVO> getTrackingPage(MesProBatchRecordExecutionTrackingPageReqVO pageReqVO) {
        List<Long> routeProcessIds = resolveRouteProcessIds(pageReqVO.getProcessId());
        if (routeProcessIds != null && routeProcessIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<Long> actorMatchedExecutionIds = resolveExecutionIdsByActorName(pageReqVO.getActorName());
        if (actorMatchedExecutionIds != null && actorMatchedExecutionIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<MesProBatchRecordExecutionDO> pageResult =
                executionMapper.selectTrackingPage(pageReqVO, routeProcessIds, actorMatchedExecutionIds);
        return new PageResult<>(pageResult.getList().stream().map(this::buildTrackingResp).toList(), pageResult.getTotal());
    }

    @Override
    public PageResult<MesProBatchRecordExecutionSignatureRespVO> getSignaturePage(MesProBatchRecordExecutionSignaturePageReqVO pageReqVO) {
        List<Long> executionIds = resolveExecutionIdsByExecutionCode(pageReqVO.getExecutionCode());
        if (executionIds != null && executionIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        PageResult<MesProBatchRecordExecutionSignatureDO> pageResult = signatureMapper.selectPage(pageReqVO, executionIds);
        return new PageResult<>(pageResult.getList().stream().map(this::buildSignatureResp).toList(), pageResult.getTotal());
    }

    @Override
    public List<MesProBatchRecordExecutionTrackingEventRespVO> getTrackingTimeline(Long executionId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        return signatureMapper.selectTimelineListByExecutionId(executionId).stream()
                .map(signature -> new MesProBatchRecordExecutionTrackingEventRespVO()
                        .setEventId(signature.getId())
                        .setExecutionId(signature.getExecutionId())
                        .setEventType(signature.getActionType())
                        .setActionType(signature.getActionType())
                        .setEvidenceCategory(resolveTrackingEvidenceCategory(signature.getActionType()))
                        .setEvidenceCategoryName(resolveTrackingEvidenceCategoryName(signature.getActionType()))
                        .setProcessInstanceId(signature.getProcessInstanceId())
                        .setBpmTaskId(signature.getBpmTaskId())
                        .setTaskDefinitionKey(signature.getBpmTaskDefinitionKey())
                        .setNodeName(signature.getBpmTaskName())
                        .setActorId(signature.getActorId())
                        .setActorName(signature.getActorName())
                        .setResult("REJECT".equals(signature.getActionType()) ? "REJECT" : "PASS")
                        .setComment(signature.getComment())
                        .setRejectReason(signature.getReason())
                        .setSignatureId(signature.getId())
                        .setOccurredAt(signature.getSignedAt()))
                .toList();
    }

    private List<Long> resolveRouteProcessIds(Long processId) {
        if (processId == null) {
            return null;
        }
        return routeProcessMapper.selectListByProcessId(processId).stream()
                .map(MesProRouteProcessDO::getId)
                .toList();
    }

    private List<Long> resolveExecutionIdsByActorName(String actorName) {
        if (StrUtil.isBlank(actorName)) {
            return null;
        }
        return signatureMapper.selectExecutionIdsByActorName(actorName);
    }

    private List<Long> resolveExecutionIdsByExecutionCode(String executionCode) {
        if (StrUtil.isBlank(executionCode)) {
            return null;
        }
        return executionMapper.selectIdsByExecutionCode(executionCode);
    }

    private <T> PageResult<MesProBatchRecordExecutionApprovalRespVO> getFilteredApprovalPage(
            MesProBatchRecordExecutionApprovalPageReqVO pageReqVO,
            Function<BpmTaskPageReqVO, PageResult<T>> pageLoader,
            Function<T, MesProBatchRecordExecutionApprovalRespVO> rowMapper) {
        int scanPageSize = Math.max(APPROVAL_FILTER_SCAN_PAGE_SIZE, safePageSize(pageReqVO.getPageSize()));
        int scanPageNo = 1;
        Long expectedTotal = null;
        List<MesProBatchRecordExecutionApprovalRespVO> filteredRows = new ArrayList<>();
        while (true) {
            PageResult<T> taskPage = pageLoader.apply(buildApprovalBpmPageReqVO(scanPageNo, scanPageSize));
            Objects.requireNonNull(taskPage, "EDHR_APPROVAL_BPM_PAGE_REQUIRED");
            Objects.requireNonNull(taskPage.getList(), "EDHR_APPROVAL_BPM_PAGE_LIST_REQUIRED");
            if (taskPage.getTotal() == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
            }
            if (expectedTotal == null) {
                expectedTotal = taskPage.getTotal();
            } else if (!Objects.equals(expectedTotal, taskPage.getTotal())) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
            }
            taskPage.getList().stream()
                    .map(rowMapper)
                    .filter(respVO -> matchesApprovalPageFilter(respVO, pageReqVO))
                    .forEach(filteredRows::add);
            if (taskPage.getList().isEmpty() || (long) scanPageNo * scanPageSize >= expectedTotal) {
                break;
            }
            scanPageNo++;
        }
        return new PageResult<>(sliceApprovalPageRows(filteredRows, pageReqVO.getPageNo(), pageReqVO.getPageSize()),
                (long) filteredRows.size());
    }

    private BpmTaskPageReqVO buildApprovalBpmPageReqVO(Integer pageNo, Integer pageSize) {
        BpmTaskPageReqVO bpmReqVO = new BpmTaskPageReqVO();
        bpmReqVO.setPageNo(safePageNo(pageNo));
        bpmReqVO.setPageSize(safePageSize(pageSize));
        bpmReqVO.setProcessDefinitionKey(EDHR_PROCESS_DEFINITION_KEY);
        return bpmReqVO;
    }

    private List<MesProBatchRecordExecutionApprovalRespVO> sliceApprovalPageRows(
            List<MesProBatchRecordExecutionApprovalRespVO> rows, Integer pageNo, Integer pageSize) {
        int safePageNo = safePageNo(pageNo);
        int safePageSize = safePageSize(pageSize);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private int safePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    private boolean hasApprovalPageFilter(MesProBatchRecordExecutionApprovalPageReqVO pageReqVO) {
        return StrUtil.isNotBlank(pageReqVO.getExecutionCode())
                || StrUtil.isNotBlank(pageReqVO.getWorkOrderCode())
                || StrUtil.isNotBlank(pageReqVO.getBatchCode())
                || pageReqVO.getSubmittedBy() != null
                || pageReqVO.getSubmittedAtStart() != null
                || pageReqVO.getSubmittedAtEnd() != null;
    }

    private boolean matchesApprovalPageFilter(MesProBatchRecordExecutionApprovalRespVO respVO,
                                              MesProBatchRecordExecutionApprovalPageReqVO pageReqVO) {
        if (!containsIgnoreCase(respVO.getExecutionCode(), pageReqVO.getExecutionCode())) {
            return false;
        }
        if (!containsIgnoreCase(respVO.getWorkOrderCode(), pageReqVO.getWorkOrderCode())) {
            return false;
        }
        if (!containsIgnoreCase(respVO.getBatchCode(), pageReqVO.getBatchCode())) {
            return false;
        }
        if (pageReqVO.getSubmittedBy() != null && !Objects.equals(pageReqVO.getSubmittedBy(), respVO.getSubmittedBy())) {
            return false;
        }
        if (pageReqVO.getSubmittedAtStart() != null
                && (respVO.getSubmittedAt() == null || respVO.getSubmittedAt().isBefore(pageReqVO.getSubmittedAtStart()))) {
            return false;
        }
        return pageReqVO.getSubmittedAtEnd() == null
                || (respVO.getSubmittedAt() != null && !respVO.getSubmittedAt().isAfter(pageReqVO.getSubmittedAtEnd()));
    }

    private boolean containsIgnoreCase(String value, String filter) {
        return StrUtil.isBlank(filter) || StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(value), StrUtil.trim(filter));
    }

    private MesProBatchRecordExecutionDO getDraftExecution(Long id) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!Integer.valueOf(EXECUTION_STATUS_DRAFT).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        return execution;
    }

    private MesProBatchRecordExecutionDO getSubmittableExecution(Long id) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!Objects.equals(execution.getStatus(), EXECUTION_STATUS_DRAFT)
                && !Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        return execution;
    }

    private void requireExecutionAbility(MesProBatchRecordExecutionDO execution, Long workTaskId, String ability,
                                         String permissionCode, String actionName) {
        permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                .setScopeId(execution.getPermissionScopeId())
                .setObjectType("BATCH_RECORD_EXECUTION")
                .setObjectId(String.valueOf(execution.getId()))
                .setAbility(ability)
                .setExecutionId(execution.getId())
                .setWorkTaskId(workTaskId)
                .setRouteProcessId(execution.getRouteProcessId())
                .setReportId(execution.getBatchRecordReportId())
                .setRecordCategory(execution.getRecordCategory())
                .setPermissionCode(permissionCode)
                .setActionName(actionName));
    }

    private void requireSubmitAbilityOrFillTask(MesProBatchRecordExecutionDO execution, Long workTaskId) {
        if (workTaskId != null) {
            workTaskService.validateWritableFillTaskForExecution(workTaskId, execution.getId());
            return;
        }
        requireExecutionAbility(execution, null, "SIGN",
                "mes:pro-batch-record-execution:submit", "提交电子批记录审批");
    }

    private String resolveApprovalActionAbility(MesProEdhrWorkTaskDO workTask) {
        if (workTask != null
                && Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)) {
            return "REVIEW";
        }
        if (workTask != null
                && Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_APPROVE)) {
            return "APPROVE";
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
    }

    private String resolveApprovalSignatureAction(MesProEdhrWorkTaskDO workTask) {
        if (workTask != null
                && Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)) {
            return MesProBatchRecordExecutionSignatureService.ACTION_REVIEW_APPROVE;
        }
        if (workTask != null
                && Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_APPROVE)) {
            return MesProBatchRecordExecutionSignatureService.ACTION_APPROVE;
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
    }

    private String resolveApprovalActionName(String ability) {
        return "REVIEW".equals(ability) ? "电子批记录审核签名" : "电子批记录最终批准";
    }

    private MesProBatchRecordExecutionDO getActiveExecution(Long id) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!ACTIVE_EXECUTION_STATUSES.contains(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        return execution;
    }

    private MesProBatchRecordExecutionDO getSubmittedExecution(Long id) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!Integer.valueOf(EXECUTION_STATUS_SUBMITTED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        if (StrUtil.isBlank(execution.getProcessInstanceId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
        return execution;
    }

    private MesProBatchRecordApprovalSnapshotDO getApprovalSnapshot(Long executionId) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(executionId);
        if (snapshot == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
        return snapshot;
    }

    private void validateActionRequestContract(String processInstanceId,
                                               Long approvalSnapshotId,
                                               String approvalSnapshotHash,
                                               MesProBatchRecordExecutionDO execution,
                                               MesProBatchRecordApprovalSnapshotDO snapshot) {
        if (!StrUtil.equals(processInstanceId, execution.getProcessInstanceId())
                || !Objects.equals(approvalSnapshotId, snapshot.getId())
                || !StrUtil.equals(approvalSnapshotHash, snapshot.getSnapshotHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
    }

    private Task validateApprovalTask(MesProBatchRecordExecutionDO execution,
                                      MesProBatchRecordApprovalSnapshotDO snapshot,
                                      String expectedBpmTaskId,
                                      String requestBpmTaskId) {
        if (StrUtil.isBlank(expectedBpmTaskId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
        }
        if (StrUtil.isNotBlank(requestBpmTaskId) && !StrUtil.equals(expectedBpmTaskId, requestBpmTaskId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
        }
        Task task = bpmTaskService.validateTask(requireLoginUserId(), expectedBpmTaskId);
        if (task == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(execution.getProcessInstanceId(), task.getProcessInstanceId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
        }
        if (StrUtil.isNotBlank(snapshot.getCurrentTaskDefinitionKey())
                && !StrUtil.equals(snapshot.getCurrentTaskDefinitionKey(), task.getTaskDefinitionKey())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID);
        }
        return task;
    }

    private Task getSingleRunningTask(String processInstanceId) {
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(processInstanceId, true, null);
        if (tasks == null || tasks.size() != 1) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
        }
        return tasks.get(0);
    }

    private ApprovalExecutionContext validateApprovalSubmissionPrerequisites(MesProBatchRecordExecutionDO execution) {
        String executionSnapshotJson = validateApprovalEvidenceJson(execution.getExecutionSnapshotJson());
        String cellValuesJson = validateApprovalEvidenceJson(execution.getCellValuesJson());
        validateRequiredFieldsCompleted(executionSnapshotJson, cellValuesJson);
        FieldAuditEvidence fieldAuditEvidence = validateFieldAuditEvidence(execution);
        if (execution.getId() == null || StrUtil.isBlank(execution.getExecutionCode())
                || execution.getWorkOrderId() == null || StrUtil.isBlank(execution.getWorkOrderCode())
                || execution.getRouteProcessId() == null || StrUtil.isBlank(execution.getBatchCode())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING);
        }
        MesProRouteProcessDO routeProcess = resolveExecutionRouteProcess(execution);
        if (routeProcess == null || routeProcess.getProcessId() == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING);
        }
        MesProProcessDO process = processMapper.selectById(routeProcess.getProcessId());
        if (process == null || StrUtil.isBlank(process.getName())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING);
        }
        MesMdWorkstationDO workstation = execution.getWorkstationId() == null
                ? null : workstationMapper.selectById(execution.getWorkstationId());
        if (execution.getWorkstationId() != null && (workstation == null || StrUtil.isBlank(workstation.getName()))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING);
        }
        return new ApprovalExecutionContext(routeProcess.getProcessId(), process.getName(),
                execution.getWorkstationId(), workstation == null ? null : workstation.getName(),
                executionSnapshotJson, cellValuesJson,
                fieldAuditEvidence.cellValuesHash(), fieldAuditEvidence.fieldAuditRevision(),
                fieldAuditEvidence.fieldAuditHeadHash(), tenantId);
    }

    private FieldAuditEvidence validateFieldAuditEvidence(MesProBatchRecordExecutionDO execution) {
        if (StrUtil.isBlank(execution.getCellValuesHash()) || execution.getFieldAuditRevision() == null
                || StrUtil.isBlank(execution.getFieldAuditHeadHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
        }
        String actualCellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(execution.getCellValuesJson());
        if (!Objects.equals(actualCellValuesHash, execution.getCellValuesHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
        }
        MesProBatchRecordExecutionFieldAuditHashVerification verification = fieldAuditService.verifyChain(execution.getId());
        if (verification == null
                || verification.getStatus() != MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        return new FieldAuditEvidence(execution.getCellValuesHash(), execution.getFieldAuditRevision(),
                execution.getFieldAuditHeadHash());
    }

    private FieldAuditEvidence validateSubmittedSnapshotFieldAuditEvidence(MesProBatchRecordExecutionDO execution,
                                                                           MesProBatchRecordApprovalSnapshotDO snapshot) {
        FieldAuditEvidence current = validateFieldAuditEvidence(execution);
        JSONObject snapshotJson;
        try {
            snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        if (snapshotJson == null
                || !Objects.equals(current.cellValuesHash(), snapshotJson.getString("cellValuesHash"))
                || !Objects.equals(current.fieldAuditRevision(), snapshotJson.getLong("fieldAuditRevision"))
                || !Objects.equals(current.fieldAuditHeadHash(), snapshotJson.getString("fieldAuditHeadHash"))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
        }
        return current;
    }

    private String requireLockedDomainTraceHash(MesProBatchRecordApprovalSnapshotDO snapshot) {
        JSONObject snapshotJson;
        try {
            snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        if (snapshotJson == null || snapshotJson.getLong("domainTraceSnapshotId") == null
                || StrUtil.isBlank(snapshotJson.getString("domainTraceHash"))
                || !DOMAIN_TRACE_STATUS_VERIFIED.equals(snapshotJson.getString("domainTraceStatus"))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        return snapshotJson.getString("domainTraceHash");
    }

    private String validateApprovalEvidenceJson(String json) {
        String trimmed = StrUtil.trim(json);
        if (StrUtil.isBlank(trimmed)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
        try {
            JSON.parse(trimmed);
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        return trimmed;
    }

    private void validateRequiredFieldsCompleted(String executionSnapshotJson, String cellValuesJson) {
        JSONArray requiredFields = requiredEditableFields(executionSnapshotJson);
        if (requiredFields.isEmpty()) {
            return;
        }
        Map<String, JSONObject> valuesByCell = cellValuesByCell(cellValuesJson);
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < requiredFields.size(); i++) {
            JSONObject field = requiredFields.getJSONObject(i);
            JSONObject cellValue = valuesByCell.get(requiredCellKey(field));
            if (cellValue == null || isRequiredCellValueMissing(cellValue.get("value"))) {
                missing.add(requiredFieldLabel(field));
            }
        }
        if (!missing.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REQUIRED_FIELD_MISSING,
                    String.join("、", missing.stream().limit(5).toList()));
        }
    }

    private JSONArray requiredEditableFields(String executionSnapshotJson) {
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(executionSnapshotJson);
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        JSONArray fields = snapshot == null ? null : snapshot.getJSONArray("fields");
        JSONArray requiredFields = new JSONArray();
        if (fields == null || fields.isEmpty()) {
            return requiredFields;
        }
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field == null || !Boolean.TRUE.equals(field.getBoolean("required"))) {
                continue;
            }
            if (Boolean.TRUE.equals(field.getBoolean("readonly"))
                    || Boolean.TRUE.equals(field.getBoolean("disabled"))
                    || "SIGNATURE".equalsIgnoreCase(field.getString("valueType"))
                    || "signature".equalsIgnoreCase(field.getString("component"))) {
                continue;
            }
            if (field.getInteger("rowIndex") == null || field.getInteger("columnIndex") == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
            }
            requiredFields.add(field);
        }
        return requiredFields;
    }

    private Map<String, JSONObject> cellValuesByCell(String cellValuesJson) {
        JSONArray cellValues;
        try {
            cellValues = JSON.parseArray(cellValuesJson);
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        Map<String, JSONObject> values = new HashMap<>();
        if (cellValues == null || cellValues.isEmpty()) {
            return values;
        }
        for (int i = 0; i < cellValues.size(); i++) {
            JSONObject cellValue = cellValues.getJSONObject(i);
            if (cellValue == null || cellValue.getInteger("rowIndex") == null
                    || cellValue.getInteger("columnIndex") == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
            }
            values.put(requiredCellKey(cellValue), cellValue);
        }
        return values;
    }

    private String requiredCellKey(JSONObject value) {
        return value.getInteger("rowIndex") + ":" + value.getInteger("columnIndex");
    }

    private boolean isRequiredCellValueMissing(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence text) {
            return StrUtil.isBlank(text);
        }
        if (value instanceof JSONArray array) {
            return array.isEmpty();
        }
        if (value instanceof JSONObject object) {
            return object.isEmpty();
        }
        return false;
    }

    private String requiredFieldLabel(JSONObject field) {
        String label = StrUtil.blankToDefault(field.getString("label"), field.getString("fieldKey"));
        if (StrUtil.isBlank(label)) {
            label = "R" + (field.getIntValue("rowIndex") + 1) + "C" + (field.getIntValue("columnIndex") + 1);
        }
        return label + "（第 " + (field.getIntValue("rowIndex") + 1)
                + " 行第 " + (field.getIntValue("columnIndex") + 1) + " 列）";
    }

    private List<ReviewSignatureAssignment> resolveReviewSignatureAssignments(MesProBatchRecordExecutionDO execution) {
        JSONObject rows = resolveExecutionSnapshotRows(execution.getExecutionSnapshotJson());
        List<ReviewSignatureAssignment> assignments = new ArrayList<>();
        List<Integer> rowIndexes = rows.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .toList();
        for (Integer rowIndex : rowIndexes) {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            List<Integer> columnIndexes = cells.keySet().stream()
                    .filter(StrUtil::isNumeric)
                    .map(Integer::valueOf)
                    .sorted()
                    .toList();
            for (Integer columnIndex : columnIndexes) {
                JSONObject cell = cells.getJSONObject(String.valueOf(columnIndex));
                JSONObject signature = cell == null ? null : cell.getJSONObject("edhrSignature");
                if (!isCurrentProcessReviewSignature(cell, signature)) {
                    continue;
                }
                String signatureCellKey = StrUtil.blankToDefault(signature.getString("signatureCellKey"),
                        "R" + rowIndex + "C" + columnIndex);
                String reviewSourceType = signature.getString("reviewSourceType");
                Long reviewSourceId = signature.getLong("reviewSourceId");
                List<Long> reviewSourceIds = readReviewSourceIds(signatureCellKey, reviewSourceType, signature);
                String reviewSourceName = signature.getString("reviewSourceName");
                MesProEdhrProcessFormPermissionRuleDO configuredRule = processFormPermissionRuleMapper
                        .selectEnabledSignatureRule(execution.getRouteProcessId(),
                                execution.getBatchRecordReportId(), signatureCellKey,
                                execution.getBatchRecordVersionId());
                if (configuredRule != null) {
                    reviewSourceType = configuredRule.getCandidateSourceType();
                    List<Long> configuredSourceIds = parseRuleSourceIds(signatureCellKey,
                            configuredRule.getCandidateSourceIds());
                    reviewSourceId = configuredSourceIds.size() == 1 ? configuredSourceIds.get(0) : null;
                    reviewSourceIds = configuredSourceIds;
                    reviewSourceName = StrUtil.blankToDefault(configuredRule.getRemark(), reviewSourceName);
                }
                List<MesProEdhrCandidateUser> candidateUsers = candidateResolver.resolveReviewCandidates(signatureCellKey,
                        reviewSourceType, reviewSourceId, reviewSourceIds);
                assignments.add(new ReviewSignatureAssignment(signatureCellKey, rowIndex, columnIndex,
                        reviewSourceType, reviewSourceId, reviewSourceIds, reviewSourceName, candidateUsers,
                        null, null));
            }
        }
        if (assignments.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SIGNATURE_CELL_MISSING);
        }
        return assignments;
    }

    private boolean hasReviewSignatureAssignments(MesProBatchRecordExecutionDO execution) {
        if (!shouldUseLegacyProcessReviewApproval(execution)) {
            return false;
        }
        JSONObject rows = resolveOptionalExecutionSnapshotRows(execution.getExecutionSnapshotJson());
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (String rowKey : rows.keySet()) {
            if (!StrUtil.isNumeric(rowKey)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null || cells.isEmpty()) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                if (!StrUtil.isNumeric(columnKey)) {
                    continue;
                }
                JSONObject cell = cells.getJSONObject(columnKey);
                JSONObject signature = cell == null ? null : cell.getJSONObject("edhrSignature");
                if (isCurrentProcessReviewSignature(cell, signature)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRouteBoundBatchExecutionForm(MesProBatchRecordExecutionDO execution) {
        return execution != null
                && (execution.getRouteBindingId() != null
                || StrUtil.isNotBlank(execution.getRouteBindingSnapshotHash()));
    }

    private boolean shouldUseLegacyProcessReviewApproval(MesProBatchRecordExecutionDO execution) {
        return execution != null
                && !(RECORD_CATEGORY_BATCH.equals(execution.getRecordCategory())
                && VALIDATION_PROFILE_BATCH.equals(execution.getValidationProfile()));
    }

    private boolean isCurrentProcessReviewSignature(JSONObject cell, JSONObject signature) {
        if (signature == null || !Boolean.TRUE.equals(signature.getBoolean("enabled"))
                || !Objects.equals("APPROVE", signature.getString("actionType"))) {
            return false;
        }
        if (isReleaseApprovalSignature(cell, signature)) {
            return false;
        }
        return true;
    }

    private boolean isReleaseApprovalSignature(JSONObject cell, JSONObject signature) {
        String signatureText = StrUtil.nullToEmpty(cell != null ? cell.getString("text") : null);
        String signatureLabel = StrUtil.nullToEmpty(signature.getString("label"));
        String reviewSourceName = StrUtil.nullToEmpty(signature.getString("reviewSourceName"));
        String combinedText = signatureText + "|" + signatureLabel + "|" + reviewSourceName;
        return StrUtil.containsAny(combinedText, "放行", "过程放行", "最终放行");
    }

    private JSONObject resolveExecutionSnapshotRows(String executionSnapshotJson) {
        try {
            JSONObject snapshot = JSON.parseObject(executionSnapshotJson);
            if (snapshot == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
            }
            JSONObject layout = snapshot.getJSONObject("layout");
            if (layout != null && layout.getJSONObject("rows") != null) {
                return layout.getJSONObject("rows");
            }
            String sheetLayoutJson = snapshot.getString("sheetLayoutJson");
            if (StrUtil.isNotBlank(sheetLayoutJson)) {
                JSONObject sheetLayout = JSON.parseObject(sheetLayoutJson);
                if (sheetLayout != null && sheetLayout.getJSONObject("rows") != null) {
                    return sheetLayout.getJSONObject("rows");
                }
            }
            if (snapshot.getJSONObject("rows") != null) {
                return snapshot.getJSONObject("rows");
            }
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SIGNATURE_CELL_MISSING);
    }

    private JSONObject resolveOptionalExecutionSnapshotRows(String executionSnapshotJson) {
        try {
            JSONObject snapshot = JSON.parseObject(executionSnapshotJson);
            if (snapshot == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
            }
            JSONObject layout = snapshot.getJSONObject("layout");
            if (layout != null && layout.getJSONObject("rows") != null) {
                return layout.getJSONObject("rows");
            }
            String sheetLayoutJson = snapshot.getString("sheetLayoutJson");
            if (StrUtil.isNotBlank(sheetLayoutJson)) {
                JSONObject sheetLayout = JSON.parseObject(sheetLayoutJson);
                if (sheetLayout != null && sheetLayout.getJSONObject("rows") != null) {
                    return sheetLayout.getJSONObject("rows");
                }
            }
            return snapshot.getJSONObject("rows");
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID);
        }
    }

    private List<Long> readReviewSourceIds(String signatureCellKey, String reviewSourceType, JSONObject signature) {
        JSONArray sourceIds = signature.getJSONArray("reviewSourceIds");
        if (sourceIds == null) {
            return null;
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (int i = 0; i < sourceIds.size(); i++) {
            Long sourceId = sourceIds.getLong(i);
            if (sourceId == null || !uniqueIds.add(sourceId)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID,
                        signatureCellKey + ":" + reviewSourceType);
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private List<Long> parseRuleSourceIds(String signatureCellKey, String rawSourceIds) {
        if (StrUtil.isBlank(rawSourceIds)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String item : rawSourceIds.split(",")) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            Long id = parseNullableLong(item.trim());
            if (id == null || !ids.add(id)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey + ":" + rawSourceIds);
            }
        }
        if (ids.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
        }
        return new ArrayList<>(ids);
    }

    private List<MesProEdhrReviewTaskCreateCommand> buildReviewTaskCreateCommands(
            String processInstanceId, List<ReviewSignatureAssignment> reviewAssignments) {
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(processInstanceId, null,
                EDHR_APPROVAL_TASK_DEFINITION_KEY);
        if (tasks == null || tasks.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH,
                    "processInstanceId=" + processInstanceId + ", taskDefinitionKey="
                            + EDHR_APPROVAL_TASK_DEFINITION_KEY + ", tasks=empty");
        }
        List<MesProEdhrReviewTaskCreateCommand> commands = new ArrayList<>();
        for (ReviewSignatureAssignment assignment : reviewAssignments) {
            List<Task> matchedTasks = tasks.stream()
                    .filter(task -> {
                        Long bpmAssigneeUserId = resolveBpmTaskAssigneeUserId(task);
                        return bpmAssigneeUserId != null && assignment.reviewTaskUserIds().contains(bpmAssigneeUserId);
                    })
                    .toList();
            if (matchedTasks.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH,
                        buildBpmTaskMismatchDetail(assignment, tasks));
            }
            for (Task task : matchedTasks) {
                Long assigneeUserId = resolveBpmTaskAssigneeUserId(task);
                ReviewSignatureAssignment assigned = assignment.withAssignee(assigneeUserId);
                commands.add(new MesProEdhrReviewTaskCreateCommand()
                        .setSignatureCellKey(assigned.signatureCellKey())
                        .setSignatureRowIndex(assigned.signatureRowIndex())
                        .setSignatureColumnIndex(assigned.signatureColumnIndex())
                        .setReviewSourceType(assigned.reviewSourceType())
                        .setReviewSourceId(assigned.reviewSourceId())
                        .setReviewSourceName(assigned.reviewSourceName())
                        .setCandidateSourceType(resolveCandidateSourceType(assigned.reviewSourceType()))
                        .setCandidateSourceId(assigned.reviewSourceId())
                        .setCandidateUserSnapshot(assigned.candidateUserIds().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")))
                        .setAssigneeUserId(assigned.assigneeUserId())
                        .setBpmTaskId(task.getId()));
            }
        }
        return commands;
    }

    private List<ReviewSignatureAssignment> applyReviewAssigneeSelections(
            List<ReviewSignatureAssignment> reviewAssignments,
            List<MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            String missingCells = reviewAssignments.stream()
                    .map(ReviewSignatureAssignment::signatureCellKey)
                    .collect(Collectors.joining(","));
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED, missingCells);
        }
        Map<String, Long> selectedUserIdByCell = new LinkedHashMap<>();
        for (MesProBatchRecordExecutionSubmitReqVO.ReviewAssigneeSelection selection : selections) {
            if (selection == null || StrUtil.isBlank(selection.getSignatureCellKey())
                    || selection.getSelectedUserId() == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED, "empty selection");
            }
            String signatureCellKey = StrUtil.trim(selection.getSignatureCellKey());
            if (selectedUserIdByCell.putIfAbsent(signatureCellKey, selection.getSelectedUserId()) != null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID, signatureCellKey);
            }
        }
        Set<String> expectedCells = reviewAssignments.stream()
                .map(ReviewSignatureAssignment::signatureCellKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String selectedCell : selectedUserIdByCell.keySet()) {
            if (!expectedCells.contains(selectedCell)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID, selectedCell);
            }
        }
        List<ReviewSignatureAssignment> selectedAssignments = new ArrayList<>();
        for (ReviewSignatureAssignment assignment : reviewAssignments) {
            Long selectedUserId = selectedUserIdByCell.get(assignment.signatureCellKey());
            if (selectedUserId == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED,
                        assignment.signatureCellKey());
            }
            if (!assignment.candidateUserIds().contains(selectedUserId)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID,
                        assignment.signatureCellKey() + ":" + selectedUserId);
            }
            selectedAssignments.add(assignment.withAssignee(selectedUserId));
        }
        return selectedAssignments;
    }

    private String buildBpmTaskMismatchDetail(ReviewSignatureAssignment assignment, List<Task> tasks) {
        String taskSummary = tasks.stream()
                .map(task -> task.getId() + "/" + task.getTaskDefinitionKey() + "/assignee=" + task.getAssignee()
                        + "/local=" + (task.getTaskLocalVariables() != null
                        ? task.getTaskLocalVariables().get(EDHR_APPROVAL_TASK_ASSIGNEE_VARIABLE)
                        : null))
                .collect(Collectors.joining(";"));
        return "signatureCellKey=" + assignment.signatureCellKey()
                + ", candidateUserIds=" + assignment.candidateUserIds()
                + ", tasks=" + taskSummary;
    }

    private Long resolveBpmTaskAssigneeUserId(Task task) {
        Long assigneeUserId = parseNullableLong(task.getAssignee());
        if (assigneeUserId != null) {
            return assigneeUserId;
        }
        Object localAssignee = task.getTaskLocalVariables() != null
                ? task.getTaskLocalVariables().get(EDHR_APPROVAL_TASK_ASSIGNEE_VARIABLE)
                : null;
        if (localAssignee instanceof Number number) {
            return number.longValue();
        }
        return parseNullableLong(localAssignee != null ? String.valueOf(localAssignee) : null);
    }

    private String resolveCandidateSourceType(String reviewSourceType) {
        return switch (reviewSourceType) {
            case "USER", "USERS" -> "USER";
            case "ROLE", "ROLES" -> "ROLE_GROUP";
            case "DEPT", "DEPTS" -> "DEPT_GROUP";
            case "DEPT_LEADER" -> "DEPT_LEADER";
            default -> reviewSourceType;
        };
    }

    private List<Long> distinctCandidateUserIds(List<ReviewSignatureAssignment> reviewAssignments) {
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        reviewAssignments.forEach(assignment -> userIds.addAll(assignment.reviewTaskUserIds()));
        return new ArrayList<>(userIds);
    }

    private JSONArray toReviewAssignmentJsonArray(List<ReviewSignatureAssignment> reviewAssignments) {
        JSONArray array = new JSONArray();
        for (ReviewSignatureAssignment assignment : reviewAssignments) {
            JSONObject item = new JSONObject(true);
            item.put("signatureCellKey", assignment.signatureCellKey());
            item.put("signatureRowIndex", assignment.signatureRowIndex());
            item.put("signatureColumnIndex", assignment.signatureColumnIndex());
            item.put("reviewSourceType", assignment.reviewSourceType());
            item.put("reviewSourceId", assignment.reviewSourceId());
            if (assignment.reviewSourceIds() != null && !assignment.reviewSourceIds().isEmpty()) {
                item.put("reviewSourceIds", assignment.reviewSourceIds());
            }
            item.put("reviewSourceName", assignment.reviewSourceName());
            item.put("candidateUserIds", assignment.candidateUserIds());
            if (assignment.assigneeUserId() != null) {
                item.put("assigneeUserId", assignment.assigneeUserId());
                item.put("assigneeUserName", assignment.assigneeUserName());
            }
            array.add(item);
        }
        return array;
    }

    private BpmProcessInstanceCreateReqDTO buildProcessCreateReq(MesProBatchRecordExecutionDO execution,
                                                                 ApprovalExecutionContext approvalContext,
                                                                 MesProBatchRecordDomainTraceDetailRespVO domainTrace,
                                                                 String snapshotJson,
                                                                 LocalDateTime submittedAt,
                                                                 Long submittedBy,
                                                                 List<ReviewSignatureAssignment> reviewAssignments) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("edhrExecutionId", execution.getId());
        variables.put("edhrExecutionCode", execution.getExecutionCode());
        variables.put("workOrderId", execution.getWorkOrderId());
        variables.put("workOrderCode", execution.getWorkOrderCode());
        variables.put("taskId", execution.getTaskId());
        variables.put("routeProcessId", execution.getRouteProcessId());
        variables.put("processId", approvalContext.processId());
        variables.put("processName", approvalContext.processName());
        variables.put("workstationId", approvalContext.workstationId());
        variables.put("workstationName", approvalContext.workstationName());
        variables.put("batchCode", execution.getBatchCode());
        variables.put("submittedBy", submittedBy);
        variables.put("submittedAt", submittedAt.toString());
        variables.put("executionSnapshotHash", DigestUtil.sha256Hex(approvalContext.executionSnapshotJson()));
        variables.put("cellValuesHash", approvalContext.cellValuesHash());
        variables.put("fieldAuditRevision", approvalContext.fieldAuditRevision());
        variables.put("fieldAuditHeadHash", approvalContext.fieldAuditHeadHash());
        variables.put("domainTraceSnapshotId", domainTrace.getDomainTraceSnapshotId());
        variables.put("domainTraceHash", domainTrace.getDomainTraceHash());
        variables.put("domainTraceStatus", domainTrace.getStatus());
        variables.put("approvalSnapshotHash", DigestUtil.sha256Hex(snapshotJson));
        variables.put("tenantId", approvalContext.tenantId());
        variables.put("edhrReviewSignatureCells", toReviewAssignmentJsonArray(reviewAssignments).toJSONString());
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO();
        reqDTO.setProcessDefinitionKey(EDHR_PROCESS_DEFINITION_KEY);
        reqDTO.setBusinessKey(buildBusinessKey(execution.getId()));
        reqDTO.setVariables(variables);
        reqDTO.setStartUserSelectAssignees(Map.of(EDHR_APPROVAL_TASK_DEFINITION_KEY,
                distinctCandidateUserIds(reviewAssignments)));
        return reqDTO;
    }

    private Map<String, Object> buildBusinessApprovalVariables(MesProBatchRecordExecutionDO execution,
                                                               ApprovalExecutionContext approvalContext,
                                                               MesProBatchRecordDomainTraceDetailRespVO domainTrace,
                                                               String snapshotJson,
                                                               LocalDateTime submittedAt,
                                                               Long submittedBy,
                                                               Long submitSignatureId,
                                                               Long workTaskId,
                                                               List<ReviewSignatureAssignment> reviewAssignments) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("edhrExecutionId", execution.getId());
        variables.put("edhrExecutionCode", execution.getExecutionCode());
        variables.put("workOrderId", execution.getWorkOrderId());
        variables.put("workOrderCode", execution.getWorkOrderCode());
        variables.put("taskId", execution.getTaskId());
        variables.put("workTaskId", workTaskId);
        variables.put("routeProcessId", execution.getRouteProcessId());
        variables.put("processId", approvalContext.processId());
        variables.put("processName", approvalContext.processName());
        variables.put("workstationId", approvalContext.workstationId());
        variables.put("workstationName", approvalContext.workstationName());
        variables.put("batchCode", execution.getBatchCode());
        variables.put("submittedBy", submittedBy);
        variables.put("submittedAt", submittedAt.toString());
        variables.put("submitSignatureId", submitSignatureId);
        variables.put("processDefinitionKey", EDHR_PROCESS_DEFINITION_KEY);
        variables.put("executionBusinessKey", buildBusinessKey(execution.getId()));
        variables.put("approvalTaskDefinitionKey", EDHR_APPROVAL_TASK_DEFINITION_KEY);
        variables.put("executionSnapshotHash", DigestUtil.sha256Hex(approvalContext.executionSnapshotJson()));
        variables.put("cellValuesHash", approvalContext.cellValuesHash());
        variables.put("fieldAuditRevision", approvalContext.fieldAuditRevision());
        variables.put("fieldAuditHeadHash", approvalContext.fieldAuditHeadHash());
        variables.put("domainTraceSnapshotId", domainTrace.getDomainTraceSnapshotId());
        variables.put("domainTraceHash", domainTrace.getDomainTraceHash());
        variables.put("domainTraceStatus", domainTrace.getStatus());
        variables.put("approvalSnapshotJson", snapshotJson);
        variables.put("approvalSnapshotHash", DigestUtil.sha256Hex(snapshotJson));
        variables.put("edhrReviewSignatureCells", toReviewAssignmentJsonArray(reviewAssignments).toJSONString());
        return variables;
    }

    private String buildApprovalSnapshotJson(MesProBatchRecordExecutionDO execution,
                                             ApprovalExecutionContext approvalContext,
                                             MesProBatchRecordDomainTraceDetailRespVO domainTrace,
                                             LocalDateTime submittedAt,
                                             Long submittedBy,
                                             List<ReviewSignatureAssignment> reviewAssignments) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("processDefinitionKey", EDHR_PROCESS_DEFINITION_KEY);
        snapshot.put("businessKey", buildBusinessKey(execution.getId()));
        snapshot.put("executionId", execution.getId());
        snapshot.put("executionCode", execution.getExecutionCode());
        snapshot.put("workOrderId", execution.getWorkOrderId());
        snapshot.put("workOrderCode", execution.getWorkOrderCode());
        snapshot.put("taskId", execution.getTaskId());
        snapshot.put("routeProcessId", execution.getRouteProcessId());
        snapshot.put("processId", approvalContext.processId());
        snapshot.put("processName", approvalContext.processName());
        snapshot.put("workstationId", approvalContext.workstationId());
        snapshot.put("workstationName", approvalContext.workstationName());
        snapshot.put("batchCode", execution.getBatchCode());
        snapshot.put("submittedBy", submittedBy);
        snapshot.put("submittedAt", submittedAt.toString());
        snapshot.put("executionSnapshotHash", DigestUtil.sha256Hex(approvalContext.executionSnapshotJson()));
        snapshot.put("cellValuesHash", approvalContext.cellValuesHash());
        snapshot.put("fieldAuditRevision", approvalContext.fieldAuditRevision());
        snapshot.put("fieldAuditHeadHash", approvalContext.fieldAuditHeadHash());
        snapshot.put("domainTraceSnapshotId", domainTrace.getDomainTraceSnapshotId());
        snapshot.put("domainTraceHash", domainTrace.getDomainTraceHash());
        snapshot.put("domainTraceStatus", domainTrace.getStatus());
        snapshot.put("tenantId", approvalContext.tenantId());
        snapshot.put("reviewAssignments", toReviewAssignmentJsonArray(reviewAssignments));
        return snapshot.toJSONString();
    }

    private String buildBusinessKey(Long executionId) {
        return EDHR_BUSINESS_KEY_PREFIX + executionId;
    }

    private Long requireLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS);
        }
        return userId;
    }

    private MesProBatchRecordExecutionSignatureTimeCommand buildSignatureTimeCommand(
            MesProBatchRecordExecutionSignatureTimeReqVO signatureTime) {
        if (signatureTime == null) {
            return null;
        }
        return buildSignatureTimeCommand(signatureTime.getSelectedSignedAt(), signatureTime.getSelectedTimeZone(),
                signatureTime.getSelectedTimeReason());
    }

    private MesProBatchRecordExecutionSignatureTimeCommand buildSignatureTimeCommand(
            LocalDateTime selectedSignedAt,
            String selectedTimeZone,
            String selectedTimeReason) {
        if (selectedSignedAt == null && StrUtil.isBlank(selectedTimeZone) && StrUtil.isBlank(selectedTimeReason)) {
            return null;
        }
        if (selectedSignedAt == null || StrUtil.isBlank(selectedTimeZone) || StrUtil.isBlank(selectedTimeReason)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        return new MesProBatchRecordExecutionSignatureTimeCommand()
                .setSelectedSignedAt(selectedSignedAt)
                .setSelectedTimeZone(StrUtil.trim(selectedTimeZone))
                .setSelectedTimeReason(StrUtil.trim(selectedTimeReason));
    }

    private MesProBatchRecordExecutionApprovalRespVO buildPendingApprovalResp(Task task) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByProcessInstanceId(task.getProcessInstanceId());
        if (snapshot == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(snapshot.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        MesProBatchRecordExecutionApprovalRespVO respVO = buildApprovalResp(execution, snapshot);
        fillTaskFields(respVO, task);
        MesProEdhrWorkTaskDO reviewTask = workTaskService.getActiveReviewTaskByBpmTaskId(execution.getId(), task.getId());
        if (reviewTask != null) {
            fillWorkTaskFields(respVO, reviewTask);
        }
        return respVO;
    }

    private MesProBatchRecordExecutionApprovalRespVO buildDoneApprovalResp(HistoricTaskInstance task) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByProcessInstanceId(task.getProcessInstanceId());
        if (snapshot == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(snapshot.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        MesProBatchRecordExecutionApprovalRespVO respVO = buildApprovalResp(execution, snapshot);
        fillTaskFields(respVO, task);
        respVO.setDecision(snapshot.getApprovalStatus());
        respVO.setHandledAt(snapshot.getApprovedAt() == null ? snapshot.getRejectedAt() : snapshot.getApprovedAt());
        respVO.setActorId(snapshot.getApprovedBy() == null ? snapshot.getRejectedBy() : snapshot.getApprovedBy());
        return respVO;
    }

    private MesProBatchRecordExecutionApprovalRespVO buildApprovalResp(MesProBatchRecordExecutionDO execution,
                                                                       MesProBatchRecordApprovalSnapshotDO snapshot) {
        MesProBatchRecordExecutionRespVO detail = buildResp(execution);
        MesProBatchRecordExecutionApprovalRespVO respVO = BeanUtils.toBean(detail, MesProBatchRecordExecutionApprovalRespVO.class);
        fillApprovalContractFields(respVO, execution, snapshot);
        respVO.setSubmittedBy(execution.getSubmittedBy());
        respVO.setSubmittedAt(execution.getSubmittedAt());
        return respVO;
    }

    private void fillTaskFields(MesProBatchRecordExecutionApprovalRespVO respVO, Task task) {
        respVO.setProcessInstanceId(task.getProcessInstanceId());
        respVO.setBpmTaskId(task.getId());
        respVO.setBpmTaskName(task.getName());
        respVO.setTaskName(task.getName());
        respVO.setBpmTaskDefinitionKey(task.getTaskDefinitionKey());
        respVO.setTaskDefinitionKey(task.getTaskDefinitionKey());
    }

    private void fillTaskFields(MesProBatchRecordExecutionApprovalRespVO respVO, HistoricTaskInstance task) {
        respVO.setProcessInstanceId(task.getProcessInstanceId());
        respVO.setBpmTaskId(task.getId());
        respVO.setBpmTaskName(task.getName());
        respVO.setTaskName(task.getName());
        respVO.setBpmTaskDefinitionKey(task.getTaskDefinitionKey());
        respVO.setTaskDefinitionKey(task.getTaskDefinitionKey());
    }

    private void fillWorkTaskFields(MesProBatchRecordExecutionApprovalRespVO respVO, MesProEdhrWorkTaskDO workTask) {
        respVO.setWorkTaskId(workTask.getId());
        respVO.setTaskType(workTask.getTaskType());
        respVO.setSignatureCellKey(workTask.getSignatureCellKey());
        respVO.setSignatureRowIndex(workTask.getSignatureRowIndex());
        respVO.setSignatureColumnIndex(workTask.getSignatureColumnIndex());
        respVO.setReviewSourceType(workTask.getReviewSourceType());
        respVO.setReviewSourceId(workTask.getReviewSourceId());
        respVO.setReviewSourceName(workTask.getReviewSourceName());
    }

    private void fillApprovalContractFields(MesProBatchRecordExecutionApprovalRespVO respVO,
                                            MesProBatchRecordExecutionDO execution,
                                            MesProBatchRecordApprovalSnapshotDO snapshot) {
        respVO.setExecutionId(execution.getId());
        respVO.setApprovalSnapshotId(snapshot.getId());
        respVO.setApprovalSnapshotHash(snapshot.getSnapshotHash());
        respVO.setApprovalSnapshotStatus(snapshot.getApprovalStatus());
        boolean submitted = Integer.valueOf(EXECUTION_STATUS_SUBMITTED).equals(execution.getStatus());
        boolean approved = Integer.valueOf(EXECUTION_STATUS_APPROVED).equals(execution.getStatus())
                && execution.getClosedAt() != null && StrUtil.isNotBlank(execution.getProcessInstanceId());
        respVO.setCanApprove(submitted);
        respVO.setCanReject(Boolean.FALSE);
        respVO.setCanViewTracking(Boolean.TRUE);
        respVO.setCanViewSignatures(Boolean.TRUE);
        respVO.setCanGenerateArchive(approved);
        respVO.setCanDownloadArchive(archiveMapper.selectLatestByExecutionId(execution.getId()) != null);
    }

    private MesProBatchRecordExecutionTrackingRespVO buildTrackingResp(MesProBatchRecordExecutionDO execution) {
        String processName = resolveTrackingProcessName(execution);
        String workstationName = resolveTrackingWorkstationName(execution);
        MesProBatchRecordExecutionTrackingRespVO respVO = new MesProBatchRecordExecutionTrackingRespVO();
        respVO.setExecutionId(execution.getId());
        respVO.setExecutionCode(execution.getExecutionCode());
        respVO.setWorkOrderId(execution.getWorkOrderId());
        respVO.setWorkOrderCode(execution.getWorkOrderCode());
        respVO.setBatchId(resolveTrackingBatchId(execution));
        respVO.setBatchCode(execution.getBatchCode());
        respVO.setProcessName(processName);
        respVO.setWorkstationName(workstationName);
        respVO.setStatus(execution.getStatus());
        respVO.setProcessInstanceId(execution.getProcessInstanceId());
        respVO.setClosedAt(execution.getClosedAt());
        MesProBatchRecordExecutionSignatureDO lastSignature = signatureMapper.selectListByExecutionId(execution.getId())
                .stream().findFirst().orElse(null);
        fillTrackingLastOperationFields(respVO, processName, execution, lastSignature);
        respVO.setLastEventType(lastSignature == null ? null : lastSignature.getActionType());
        respVO.setLastEvidenceCategory(lastSignature == null ? null : resolveTrackingEvidenceCategory(lastSignature.getActionType()));
        respVO.setLastEvidenceCategoryName(lastSignature == null ? null : resolveTrackingEvidenceCategoryName(lastSignature.getActionType()));
        respVO.setLastEventReason(resolveLastEventReason(lastSignature));
        respVO.setLastEventAt(lastSignature == null ? execution.getUpdateTime() : lastSignature.getSignedAt());
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        respVO.setArchiveStatus(archive == null ? null : archive.getArchiveStatus());
        return respVO;
    }

    private Long resolveTrackingBatchId(MesProBatchRecordExecutionDO execution) {
        if (StrUtil.isBlank(execution.getBatchCode())) {
            return null;
        }
        MesWmBatchDO batch = batchMapper.selectByCode(execution.getBatchCode());
        return batch == null ? null : batch.getId();
    }

    private String resolveTrackingProcessName(MesProBatchRecordExecutionDO execution) {
        if (execution.getRouteProcessId() == null) {
            return null;
        }
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(execution.getRouteProcessId());
        if (routeProcess == null || routeProcess.getProcessId() == null) {
            return null;
        }
        MesProProcessDO process = processMapper.selectById(routeProcess.getProcessId());
        return process == null ? null : process.getName();
    }

    private String resolveTrackingWorkstationName(MesProBatchRecordExecutionDO execution) {
        if (execution.getWorkstationId() == null) {
            return null;
        }
        MesMdWorkstationDO workstation = workstationMapper.selectById(execution.getWorkstationId());
        return workstation == null ? null : workstation.getName();
    }

    private void fillTrackingLastOperationFields(MesProBatchRecordExecutionTrackingRespVO respVO,
                                                 String processName,
                                                 MesProBatchRecordExecutionDO execution,
                                                 MesProBatchRecordExecutionSignatureDO lastSignature) {
        if (lastSignature == null) {
            respVO.setCurrentNodeName(null);
            respVO.setCurrentAssigneeNames(List.of());
            return;
        }
        String currentNodeName = resolveTrackingCurrentNodeName(processName, lastSignature);
        if (StrUtil.isBlank(currentNodeName)) {
            throw new IllegalStateException("eDHR 追踪最后操作缺少工序名称: executionId=" + execution.getId());
        }
        if (StrUtil.isBlank(lastSignature.getActorName())) {
            throw new IllegalStateException("eDHR 追踪最后操作缺少处理人: executionId=" + execution.getId()
                    + ", signatureId=" + lastSignature.getId());
        }
        respVO.setCurrentNodeName(currentNodeName);
        respVO.setCurrentAssigneeNames(List.of(lastSignature.getActorName()));
    }

    private String resolveTrackingCurrentNodeName(String processName,
                                                   MesProBatchRecordExecutionSignatureDO lastSignature) {
        String normalizedProcessName = StrUtil.trim(processName);
        if (StrUtil.isNotBlank(normalizedProcessName)) {
            return normalizedProcessName;
        }
        String bpmTaskName = StrUtil.trim(lastSignature.getBpmTaskName());
        if (StrUtil.isNotBlank(bpmTaskName)) {
            return bpmTaskName;
        }
        String eventReason = resolveLastEventReason(lastSignature);
        if (StrUtil.isNotBlank(eventReason)) {
            return eventReason;
        }
        return resolveSignatureMeaning(lastSignature.getActionType());
    }

    private String resolveTrackingEvidenceCategory(String actionType) {
        return switch (String.valueOf(actionType)) {
            case "SUBMIT" -> "ORDINARY_FILL_SIGNATURE";
            case "APPROVE", "REVIEW_APPROVE", "REJECT" -> "RELEASE_REVIEW_APPROVAL";
            case "FORM_REVIEW" -> "HISTORICAL_FORM_REVIEW_APPROVAL";
            case "ARCHIVE_SEAL" -> "ARCHIVE_SEAL";
            default -> "TECHNICAL_TRACE";
        };
    }

    private String resolveTrackingEvidenceCategoryName(String actionType) {
        return switch (resolveTrackingEvidenceCategory(actionType)) {
            case "ORDINARY_FILL_SIGNATURE" -> "普通工序填写提交证据";
            case "RELEASE_REVIEW_APPROVAL" -> "放行阶段审核/批准证据";
            case "HISTORICAL_FORM_REVIEW_APPROVAL" -> "历史工序审核/批准证据（只读）";
            case "ARCHIVE_SEAL" -> "归档封存证据";
            default -> "技术追踪证据";
        };
    }

    private String resolveLastEventReason(MesProBatchRecordExecutionSignatureDO signature) {
        if (signature == null) {
            return null;
        }
        String reason = StrUtil.trim(signature.getReason());
        return StrUtil.isNotBlank(reason) ? reason : StrUtil.blankToDefault(StrUtil.trim(signature.getComment()), null);
    }

    private MesProBatchRecordExecutionSignatureRespVO buildSignatureResp(MesProBatchRecordExecutionSignatureDO signature) {
        MesProBatchRecordExecutionSignatureRespVO respVO = BeanUtils.toBean(signature, MesProBatchRecordExecutionSignatureRespVO.class);
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(signature.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        respVO.setExecutionCode(execution.getExecutionCode());
        respVO.setActorNickname(signature.getActorName());
        respVO.setTaskDefinitionKey(signature.getBpmTaskDefinitionKey());
        respVO.setMeaningText(resolveSignatureMeaning(signature.getActionType()));
        return respVO;
    }

    private String resolveSignatureMeaning(String actionType) {
        return switch (StrUtil.nullToEmpty(actionType)) {
            case MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT -> "提交审批";
            case MesProBatchRecordExecutionSignatureService.ACTION_APPROVE -> "最终批准";
            case MesProBatchRecordExecutionSignatureService.ACTION_REVIEW_APPROVE -> "审核签名";
            case MesProBatchRecordExecutionSignatureService.ACTION_REJECT -> "审批驳回";
            case MesProBatchRecordExecutionSignatureService.ACTION_ARCHIVE_SEAL -> "归档封存";
            case MesProBatchRecordExecutionSignatureService.ACTION_FIELD_CHANGE -> "字段变更";
            case MesProBatchRecordExecutionSignatureService.ACTION_FORM_REVIEW -> "表单复核";
            default -> actionType;
        };
    }

    private List<MesProBatchRecordExecutionRespVO> buildRespList(List<MesProBatchRecordExecutionDO> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, MesProRouteProcessDO> routeProcessMap = getRouteProcessMap(list);
        Map<Long, MesProRouteDO> routeMap = getRouteMap(routeProcessMap.values());
        Map<Long, MesProProcessDO> processMap = getProcessMap(routeProcessMap.values());
        Map<Long, MesMdWorkstationDO> workstationMap = getWorkstationMap(list);
        Map<String, MesProBatchRecordReportDO> reportMap = getReportMap(list);
        List<MesProBatchRecordExecutionRespVO> result = new ArrayList<>(list.size());
        for (MesProBatchRecordExecutionDO execution : list) {
            result.add(buildResp(execution,
                    routeMap.get(routeProcessMap.get(execution.getRouteProcessId()) == null
                            ? null : routeProcessMap.get(execution.getRouteProcessId()).getRouteId()),
                    routeProcessMap.get(execution.getRouteProcessId()),
                    processMap.get(routeProcessMap.get(execution.getRouteProcessId()) == null
                            ? null : routeProcessMap.get(execution.getRouteProcessId()).getProcessId()),
                    workstationMap.get(execution.getWorkstationId()),
                    reportMap.get(execution.getBatchRecordReportId()),
                    false));
        }
        return result;
    }

    private MesProBatchRecordExecutionRespVO buildResp(MesProBatchRecordExecutionDO execution) {
        MesProRouteProcessDO routeProcess = resolveExecutionRouteProcess(execution);
        MesProRouteDO route = routeProcess == null || routeProcess.getRouteId() == null
                ? null : routeMapper.selectById(routeProcess.getRouteId());
        MesProProcessDO process = routeProcess == null || routeProcess.getProcessId() == null
                ? null : processMapper.selectById(routeProcess.getProcessId());
        MesMdWorkstationDO workstation = execution.getWorkstationId() == null ? null
                : workstationMapper.selectById(execution.getWorkstationId());
        MesProBatchRecordReportDO report = StrUtil.isBlank(execution.getBatchRecordReportId()) ? null
                : reportMapper.selectByReportId(execution.getBatchRecordReportId());
        return buildResp(execution, route, routeProcess, process, workstation, report, true);
    }

    private MesProBatchRecordExecutionRespVO buildResp(MesProBatchRecordExecutionDO execution,
                                                       MesProRouteDO route,
                                                       MesProRouteProcessDO routeProcess,
                                                       MesProProcessDO process,
                                                       MesMdWorkstationDO workstation,
                                                       MesProBatchRecordReportDO report,
                                                       boolean includeCellValues) {
        MesProBatchRecordExecutionRespVO respVO = BeanUtils.toBean(execution, MesProBatchRecordExecutionRespVO.class);
        if (route != null) {
            respVO.setRouteCode(route.getCode());
            respVO.setRouteName(route.getName());
        }
        if (routeProcess != null) {
            respVO.setRouteId(routeProcess.getRouteId());
            respVO.setProcessId(routeProcess.getProcessId());
        }
        if (process != null) {
            respVO.setProcessCode(process.getCode());
            respVO.setProcessName(process.getName());
        }
        if (workstation != null) {
            respVO.setWorkstationCode(workstation.getCode());
            respVO.setWorkstationName(workstation.getName());
        }
        if (report != null) {
            respVO.setBatchRecordReportCode(report.getReportCode());
            respVO.setBatchRecordReportName(report.getReportName());
        }
        Boolean effectiveRecordbookEnabled = recordbookGlobalSettingService.resolveEffectiveRecordbookEnabled(execution.getRecordbookEnabled(), execution.getRecordCategory());
        respVO.setRecordbookEnabled(effectiveRecordbookEnabled);
        respVO.setInstanceScope(resolveExecutionInstanceScope(execution.getInstanceScope()));
        respVO.setActiveContextKey(buildExecutionActiveContextKey(execution));
        boolean bindingResolved = StrUtil.isNotBlank(execution.getBatchRecordReportId()) && report != null;
        boolean canOpen = bindingResolved && StrUtil.isNotBlank(execution.getExecutionSnapshotJson());
        respVO.setBindingResolved(bindingResolved);
        respVO.setCanOpen(canOpen);
        respVO.setExecutionId(execution.getId());
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        if (snapshot != null) {
            respVO.setApprovalSnapshotId(snapshot.getId());
            respVO.setApprovalSnapshotHash(snapshot.getSnapshotHash());
            respVO.setApprovalSnapshotStatus(snapshot.getApprovalStatus());
        }
        boolean submitted = Objects.equals(execution.getStatus(), EXECUTION_STATUS_SUBMITTED)
                && snapshot != null && Objects.equals(snapshot.getApprovalStatus(), APPROVAL_STATUS_SUBMITTED);
        boolean closureEvidenceReady = Objects.equals(execution.getStatus(), EXECUTION_STATUS_APPROVED)
                && execution.getClosedAt() != null
                && snapshot != null && Objects.equals(snapshot.getApprovalStatus(), APPROVAL_STATUS_APPROVED)
                && StrUtil.isNotBlank(execution.getProcessInstanceId());
        respVO.setCanApprove(submitted);
        respVO.setCanReject(Boolean.FALSE);
        respVO.setCanViewTracking(true);
        respVO.setCanViewSignatures(true);
        respVO.setCanGenerateArchive(closureEvidenceReady);
        respVO.setCanDownloadArchive(archiveMapper.selectLatestByExecutionId(execution.getId()) != null);
        MesProEdhrPreReleaseEditabilityService.MesProEdhrPreReleaseEditability preReleaseEditability =
                preReleaseEditabilityService.resolveSubmittedOrdinaryEditableForCurrentUser(execution);
        respVO.setPreReleaseEditable(preReleaseEditability.editable());
        respVO.setPreReleaseEditReason(preReleaseEditability.reason());
        if (includeCellValues) {
            respVO.setAssistSwitchTasks(buildAssistSwitchTasksSnapshot(execution));
            respVO.setCellValues(JsonUtils.parseArray(execution.getCellValuesJson(), MesProBatchRecordExecutionCellValueVO.class));
            respVO.setSignatureSummaries(signatureMapper.selectListByExecutionId(execution.getId()).stream()
                    .map(this::buildSignatureResp)
                    .toList());
            if (Objects.equals(execution.getStatus(), EXECUTION_STATUS_DRAFT)
                    && hasReviewSignatureAssignments(execution)) {
                try {
                    respVO.setReviewAssigneeOptions(toReviewAssigneeOptions(resolveReviewSignatureAssignments(execution)));
                } catch (ServiceException ex) {
                    respVO.setReviewAssigneeOptions(List.of());
                    respVO.setReviewAssigneeOptionError(ex.getMessage());
                }
            }
            respVO.setAttachmentSummaries(attachmentMapper.selectListByExecutionId(execution.getId()).stream()
                    .map(this::buildAttachmentSummary)
                    .toList());
        }
        return respVO;
    }

    private List<EdhrBatchExecutionTaskRespVO> buildAssistSwitchTasksSnapshot(MesProBatchRecordExecutionDO execution) {
        if (execution.getBatchExecutionId() == null) {
            return List.of();
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchExecutionTaskMapper
                .selectListByBatchExecutionId(execution.getBatchExecutionId()).stream()
                .filter(task -> execution.getRouteProcessId() == null
                        || Objects.equals(task.getRouteProcessId(), execution.getRouteProcessId()))
                .toList();
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<MesProEdhrWorkTaskDO> activeWorkTasks =
                workTaskMapper.selectActiveListByBatchExecutionId(execution.getBatchExecutionId());
        if (shouldEnsureAssistSwitchCompanionFillTasks(execution.getBatchExecutionId(), tasks, activeWorkTasks)) {
            workTaskService.createInitialFillTask(edhrBatchExecutionMapper.selectById(execution.getBatchExecutionId()));
            activeWorkTasks = workTaskMapper.selectActiveListByBatchExecutionId(execution.getBatchExecutionId());
        }
        Map<Long, MesProEdhrWorkTaskDO> workTaskMap = activeWorkTasks.stream()
                .filter(this::isAssistSwitchFillWorkTask)
                .collect(Collectors.toMap(
                        MesProEdhrWorkTaskDO::getBatchTaskId,
                        task -> task,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        Map<Long, List<MesProEdhrProcessFormPermissionRuleDO>> processFormRuleMap =
                buildAssistSwitchProcessFormRuleMap(tasks, workTaskMap);
        Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> fillableRuleMap =
                buildAssistSwitchFillableRuleMap(tasks, workTaskMap, processFormRuleMap);
        Map<Long, List<Long>> routeBindingFillableUserIdsMap = buildAssistSwitchRouteBindingFillableUserIdsMap(
                tasks, workTaskMap, processFormRuleMap, fillableRuleMap);
        Map<Long, AdminUserRespDTO> userMap = buildAssistSwitchUserMap(
                workTaskMap.values(), processFormRuleMap.values(), fillableRuleMap.values(),
                routeBindingFillableUserIdsMap.values());
        return tasks.stream()
                .map(task -> toAssistSwitchTaskSnapshot(task, workTaskMap.get(task.getId()),
                        processFormRuleMap.get(task.getId()), fillableRuleMap.get(task.getId()),
                        routeBindingFillableUserIdsMap.get(task.getId()), userMap))
                .toList();
    }

    private boolean shouldEnsureAssistSwitchCompanionFillTasks(Long batchExecutionId,
                                                               List<MesProEdhrBatchExecutionTaskDO> tasks,
                                                               List<MesProEdhrWorkTaskDO> activeWorkTasks) {
        if (batchExecutionId == null || tasks == null || tasks.isEmpty()
                || activeWorkTasks == null || activeWorkTasks.isEmpty()) {
            return false;
        }
        MesProEdhrBatchExecutionDO batch = edhrBatchExecutionMapper.selectById(batchExecutionId);
        if (!isAssistSwitchActiveBatch(batch)) {
            return false;
        }
        Map<Long, MesProEdhrBatchExecutionTaskDO> taskMap = tasks.stream()
                .filter(task -> task.getId() != null)
                .collect(Collectors.toMap(
                        MesProEdhrBatchExecutionTaskDO::getId,
                        task -> task,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        Set<Long> activeBatchTaskIds = activeWorkTasks.stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .map(MesProEdhrWorkTaskDO::getBatchTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> activeRouteProcessIds = activeWorkTasks.stream()
                .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(task.getTaskType()))
                .map(task -> taskMap.get(task.getBatchTaskId()))
                .filter(Objects::nonNull)
                .filter(this::isAssistSwitchRouteForm)
                .map(MesProEdhrBatchExecutionTaskDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (activeRouteProcessIds.isEmpty()) {
            return false;
        }
        return tasks.stream()
                .filter(task -> task.getId() != null)
                .filter(this::isAssistSwitchRouteForm)
                .filter(task -> Objects.equals(task.getStatus(),
                        MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING))
                .filter(task -> activeRouteProcessIds.contains(task.getRouteProcessId()))
                .anyMatch(task -> !activeBatchTaskIds.contains(task.getId()));
    }

    private boolean isAssistSwitchActiveBatch(MesProEdhrBatchExecutionDO batch) {
        return batch != null
                && !Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                && !Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                && !Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED)
                && !Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED);
    }

    private boolean isAssistSwitchFillWorkTask(MesProEdhrWorkTaskDO workTask) {
        return workTask.getBatchTaskId() != null
                && (MesProEdhrWorkTaskService.TASK_TYPE_FILL.equals(workTask.getTaskType())
                || MesProEdhrWorkTaskService.TASK_TYPE_REWORK.equals(workTask.getTaskType()));
    }

    private EdhrBatchExecutionTaskRespVO toAssistSwitchTaskSnapshot(MesProEdhrBatchExecutionTaskDO task,
                                                                    MesProEdhrWorkTaskDO workTask,
                                                                    List<MesProEdhrProcessFormPermissionRuleDO> processFormRules,
                                                                    MesProEdhrWorkTaskAssignmentRuleDO fillableRule,
                                                                    List<Long> routeBindingFillableUserIds,
                                                                    Map<Long, AdminUserRespDTO> userMap) {
        boolean openable = isAssistSwitchTaskOpenable(task, workTask);
        return new EdhrBatchExecutionTaskRespVO()
                .setId(task.getId())
                .setNodeType(task.getNodeType())
                .setRouteProcessId(task.getRouteProcessId())
                .setRouteProcessSort(task.getRouteProcessSort())
                .setProcessId(task.getProcessId())
                .setProcessCode(task.getProcessCode())
                .setProcessName(task.getProcessName())
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordReportName(task.getBatchRecordReportName())
                .setBatchRecordDefinitionId(task.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(task.getBatchRecordVersionId())
                .setBatchRecordSort(task.getBatchRecordSort())
                .setInstanceScope(resolveExecutionInstanceScope(task.getInstanceScope()))
                .setSharedFormKey(task.getSharedFormKey())
                .setFillableScopeJson(task.getFillableScopeJson())
                .setExecutionMode(task.getExecutionMode())
                .setFormSlotType(task.getFormSlotType())
                .setFormBindingKey(task.getFormBindingKey())
                .setFormTemplateId(task.getFormTemplateId())
                .setFormTemplateName(task.getFormTemplateNameSnapshot())
                .setFormTemplateVersionId(task.getFormTemplateVersionId())
                .setFormTemplateVersionNo(task.getFormTemplateVersionNo())
                .setFormCenterInstanceId(task.getFormCenterInstanceId())
                .setRecordCategory(task.getRecordCategory())
                .setValidationProfile(task.getValidationProfile())
                .setRecordbookEnabled(recordbookGlobalSettingService.resolveEffectiveRecordbookEnabled(
                        task.getRecordbookEnabled(), task.getRecordCategory()))
                .setPermissionScopeId(task.getPermissionScopeId())
                .setRouteBindingId(task.getRouteBindingId())
                .setRouteBindingSnapshotHash(task.getRouteBindingSnapshotHash())
                .setRequiredPolicy(task.getRequiredPolicy())
                .setRequiredConditionJson(task.getRequiredConditionJson())
                .setOwnerRoleKey(task.getOwnerRoleKey())
                .setArchiveVisibility(task.getArchiveVisibility())
                .setSlotConfigSnapshotHash(task.getSlotConfigSnapshotHash())
                .setAvailable(openable)
                .setAllowedActions(openable ? List.of("OPEN_FORM") : List.of())
                .setActiveWorkTaskId(workTask == null ? null : workTask.getId())
                .setActiveWorkTaskType(workTask == null ? null : workTask.getTaskType())
                .setActiveWorkTaskActionUrl(workTask == null ? null : workTask.getActionUrl())
                .setExecutionId(task.getExecutionId())
                .setStatus(task.getStatus())
                .setRequiredFlag(task.getRequiredFlag())
                .setBlockerCode(task.getBlockerCode())
                .setBlockerMessage(task.getBlockerMessage())
                .setOpenedAt(task.getOpenedAt())
                .setSubmittedAt(task.getSubmittedAt())
                .setApprovedAt(task.getApprovedAt())
                .setSkippedBy(task.getSkippedBy())
                .setSkippedAt(task.getSkippedAt())
                .setSpecialPayloadJson(task.getSpecialPayloadJson())
                .setFillableUsers(resolveAssistSwitchFillableUsers(workTask, processFormRules,
                        fillableRule, routeBindingFillableUserIds, userMap));
    }

    private boolean isAssistSwitchTaskOpenable(MesProEdhrBatchExecutionTaskDO task,
                                               MesProEdhrWorkTaskDO workTask) {
        return workTask != null
                && !Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_BLOCKED)
                && !Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                && !Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED);
    }

    private List<EdhrBatchExecutionTaskRespVO.FillableUser> resolveAssistSwitchFillableUsers(
            MesProEdhrWorkTaskDO workTask,
            List<MesProEdhrProcessFormPermissionRuleDO> processFormRules,
            MesProEdhrWorkTaskAssignmentRuleDO fillableRule,
            List<Long> routeBindingFillableUserIds,
            Map<Long, AdminUserRespDTO> userMap) {
        List<Long> userIds = workTask != null ? resolveAssistSwitchFillableUserIds(workTask)
                : processFormRules != null && !processFormRules.isEmpty()
                ? resolveAssistSwitchFillableUserIds(processFormRules)
                : fillableRule != null ? resolveAssistSwitchFillableUserIds(fillableRule)
                : routeBindingFillableUserIds == null ? List.of() : routeBindingFillableUserIds;
        return userIds.stream()
                .map(userId -> new EdhrBatchExecutionTaskRespVO.FillableUser()
                        .setUserId(userId)
                        .setDisplayName(resolveAssistSwitchUserDisplayName(userMap, userId)))
                .toList();
    }

    private Map<Long, List<MesProEdhrProcessFormPermissionRuleDO>> buildAssistSwitchProcessFormRuleMap(
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            Map<Long, MesProEdhrWorkTaskDO> workTaskMap) {
        Map<Long, List<MesProEdhrProcessFormPermissionRuleDO>> result = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getId() == null || task.getRouteProcessId() == null
                    || workTaskMap.containsKey(task.getId()) || !isAssistSwitchRouteForm(task)) {
                continue;
            }
            String bindingKey = resolveAssistSwitchProcessFormRuleBindingKey(task);
            if (StrUtil.isBlank(bindingKey)) {
                continue;
            }
            List<MesProEdhrProcessFormPermissionRuleDO> rules =
                    processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                            task.getRouteProcessId(), bindingKey, task.getBatchRecordVersionId());
            if (!rules.isEmpty()) {
                result.put(task.getId(), rules);
            }
        }
        return result;
    }

    private Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> buildAssistSwitchFillableRuleMap(
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            Map<Long, MesProEdhrWorkTaskDO> workTaskMap,
            Map<Long, List<MesProEdhrProcessFormPermissionRuleDO>> processFormRuleMap) {
        Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> result = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getId() == null || task.getRouteProcessId() == null
                    || workTaskMap.containsKey(task.getId())
                    || processFormRuleMap.containsKey(task.getId()) || !isAssistSwitchRouteForm(task)) {
                continue;
            }
            MesProEdhrWorkTaskAssignmentRuleDO rule =
                    workTaskAssignmentRuleMapper.selectEnabledByRouteProcessAndType(
                            task.getRouteProcessId(), MesProEdhrWorkTaskService.TASK_TYPE_FILL);
            if (rule != null) {
                result.put(task.getId(), rule);
            }
        }
        return result;
    }

    private Map<Long, List<Long>> buildAssistSwitchRouteBindingFillableUserIdsMap(
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            Map<Long, MesProEdhrWorkTaskDO> workTaskMap,
            Map<Long, List<MesProEdhrProcessFormPermissionRuleDO>> processFormRuleMap,
            Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> fillableRuleMap) {
        Map<Long, Long> taskBindingIdMap = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getId() == null || task.getRouteBindingId() == null
                    || workTaskMap.containsKey(task.getId())
                    || processFormRuleMap.containsKey(task.getId())
                    || fillableRuleMap.containsKey(task.getId()) || !isAssistSwitchRouteForm(task)) {
                continue;
            }
            taskBindingIdMap.put(task.getId(), task.getRouteBindingId());
        }
        if (taskBindingIdMap.isEmpty()) {
            return Map.of();
        }
        Map<Long, MesProRouteFlowProcessBatchRecordDO> bindingMap =
                routeFlowProcessBatchRecordMapper.selectBatchIds(new LinkedHashSet<>(taskBindingIdMap.values()))
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(binding -> binding.getId() != null)
                        .collect(Collectors.toMap(
                                MesProRouteFlowProcessBatchRecordDO::getId,
                                binding -> binding,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new));
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        taskBindingIdMap.forEach((taskId, bindingId) -> {
            List<Long> userIds = resolveAssistSwitchRouteBindingFillableUserIds(bindingMap.get(bindingId));
            if (!userIds.isEmpty()) {
                result.put(taskId, userIds);
            }
        });
        return result;
    }

    private Map<Long, AdminUserRespDTO> buildAssistSwitchUserMap(
            Iterable<MesProEdhrWorkTaskDO> workTasks,
            Iterable<List<MesProEdhrProcessFormPermissionRuleDO>> processFormRuleGroups,
            Iterable<MesProEdhrWorkTaskAssignmentRuleDO> rules,
            Iterable<List<Long>> routeBindingUserIds) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (MesProEdhrWorkTaskDO workTask : workTasks) {
            userIds.addAll(resolveAssistSwitchFillableUserIds(workTask));
        }
        for (List<MesProEdhrProcessFormPermissionRuleDO> processFormRules : processFormRuleGroups) {
            userIds.addAll(resolveAssistSwitchFillableUserIds(processFormRules));
        }
        for (MesProEdhrWorkTaskAssignmentRuleDO rule : rules) {
            userIds.addAll(resolveAssistSwitchFillableUserIds(rule));
        }
        for (List<Long> bindingUserIds : routeBindingUserIds) {
            if (bindingUserIds != null) {
                userIds.addAll(bindingUserIds);
            }
        }
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return Objects.requireNonNull(adminUserApi.getUserMap(userIds),
                "EDHR_ASSIST_SWITCH_USER_MAP_REQUIRED: admin user map is required");
    }

    private List<Long> resolveAssistSwitchFillableUserIds(MesProEdhrWorkTaskDO workTask) {
        if (workTask == null) {
            return List.of();
        }
        List<Long> userIds = MesProEdhrWorkTaskAuthorization
                .parseCandidateSnapshotUserIds(workTask.getCandidateUserSnapshot());
        if (userIds.isEmpty()) {
            throw new IllegalStateException("EDHR_ASSIST_SWITCH_CANDIDATE_SNAPSHOT_REQUIRED: workTaskId="
                    + workTask.getId());
        }
        return userIds;
    }

    private List<Long> resolveAssistSwitchFillableUserIds(List<MesProEdhrProcessFormPermissionRuleDO> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (MesProEdhrProcessFormPermissionRuleDO rule : rules) {
            userIds.addAll(resolveAssistSwitchFillableUserIds(rule));
        }
        return List.copyOf(userIds);
    }

    private List<Long> resolveAssistSwitchFillableUserIds(MesProEdhrProcessFormPermissionRuleDO rule) {
        if (rule == null) {
            return List.of();
        }
        MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate =
                Objects.requireNonNull(candidateResolver.resolveProcessFormRule(rule),
                        "EDHR_ASSIST_SWITCH_PROCESS_FORM_CANDIDATE_REQUIRED: ruleId=" + rule.getId());
        List<Long> userIds = MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(candidate.userSnapshot());
        if (userIds.isEmpty()) {
            throw new IllegalStateException("EDHR_ASSIST_SWITCH_PROCESS_FORM_CANDIDATE_EMPTY: ruleId="
                    + rule.getId());
        }
        return userIds;
    }

    private List<Long> resolveAssistSwitchFillableUserIds(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        if (rule == null) {
            return List.of();
        }
        String sourceType = StrUtil.blankToDefault(rule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
        Long sourceId = rule.getCandidateSourceId() == null ? rule.getAssigneeUserId() : rule.getCandidateSourceId();
        if (sourceId == null) {
            throw new IllegalStateException("EDHR_ASSIST_SWITCH_FILL_RULE_SOURCE_REQUIRED: ruleId=" + rule.getId());
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType)) {
            return List.of(sourceId);
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(sourceType) || CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(sourceType)) {
            Set<Long> userIds = Objects.requireNonNull(permissionApi.getUserRoleIdListByRoleIds(Set.of(sourceId)),
                    "EDHR_ASSIST_SWITCH_ROLE_USER_IDS_REQUIRED: ruleId=" + rule.getId());
            return userIds.stream().filter(Objects::nonNull).sorted().toList();
        }
        if (CANDIDATE_SOURCE_TYPE_DEPT.equals(sourceType) || CANDIDATE_SOURCE_TYPE_DEPT_GROUP.equals(sourceType)) {
            List<AdminUserRespDTO> users = Objects.requireNonNull(adminUserApi.getUserListByDeptIds(Set.of(sourceId)),
                    "EDHR_ASSIST_SWITCH_DEPT_USERS_REQUIRED: ruleId=" + rule.getId());
            return users.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getId() != null && CommonStatusEnum.isEnable(user.getStatus()))
                    .map(AdminUserRespDTO::getId)
                    .distinct()
                    .sorted()
                    .toList();
        }
        throw new IllegalStateException("EDHR_ASSIST_SWITCH_FILL_RULE_SOURCE_INVALID: ruleId="
                + rule.getId() + ", sourceType=" + sourceType);
    }

    private List<Long> resolveAssistSwitchRouteBindingFillableUserIds(MesProRouteFlowProcessBatchRecordDO binding) {
        if (binding == null) {
            return List.of();
        }
        String sourceType = StrUtil.trim(binding.getCandidateSourceType());
        List<Long> sourceIds = parseAssistSwitchRouteBindingCandidateSourceIds(binding.getCandidateSourceIds());
        if (StrUtil.isBlank(sourceType) && sourceIds.isEmpty()) {
            return List.of();
        }
        if (StrUtil.isBlank(sourceType) || sourceIds.isEmpty()) {
            throw new IllegalStateException("EDHR_ASSIST_SWITCH_ROUTE_BINDING_SOURCE_REQUIRED: routeBindingId="
                    + binding.getId());
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType) || CANDIDATE_SOURCE_TYPE_USERS.equals(sourceType)) {
            return sourceIds;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(sourceType) || CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(sourceType)) {
            Set<Long> userIds = Objects.requireNonNull(
                    permissionApi.getUserRoleIdListByRoleIds(new LinkedHashSet<>(sourceIds)),
                    "EDHR_ASSIST_SWITCH_ROUTE_BINDING_ROLE_USERS_REQUIRED: routeBindingId=" + binding.getId());
            return userIds.stream().filter(Objects::nonNull).sorted().toList();
        }
        if (CANDIDATE_SOURCE_TYPE_DEPT.equals(sourceType) || CANDIDATE_SOURCE_TYPE_DEPT_GROUP.equals(sourceType)) {
            List<AdminUserRespDTO> users = Objects.requireNonNull(
                    adminUserApi.getUserListByDeptIds(new LinkedHashSet<>(sourceIds)),
                    "EDHR_ASSIST_SWITCH_ROUTE_BINDING_DEPT_USERS_REQUIRED: routeBindingId=" + binding.getId());
            return users.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getId() != null && CommonStatusEnum.isEnable(user.getStatus()))
                    .map(AdminUserRespDTO::getId)
                    .distinct()
                    .sorted()
                    .toList();
        }
        throw new IllegalStateException("EDHR_ASSIST_SWITCH_ROUTE_BINDING_SOURCE_INVALID: routeBindingId="
                + binding.getId() + ", sourceType=" + sourceType);
    }

    private List<Long> parseAssistSwitchRouteBindingCandidateSourceIds(String rawIds) {
        if (StrUtil.isBlank(rawIds)) {
            return List.of();
        }
        String normalized = StrUtil.trim(rawIds);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            JSONArray array = JSON.parseArray(normalized);
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                Long id = array.getLong(i);
                if (id != null && id > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            }
            return List.copyOf(ids);
        }
        List<Long> ids = new ArrayList<>();
        for (String item : normalized.split(",")) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            Long id = Long.valueOf(StrUtil.trim(item));
            if (id > 0 && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private String resolveAssistSwitchProcessFormRuleBindingKey(MesProEdhrBatchExecutionTaskDO task) {
        if (task == null) {
            return null;
        }
        return StrUtil.blankToDefault(StrUtil.trim(task.getBatchRecordReportId()),
                StrUtil.trim(task.getFormBindingKey()));
    }

    private boolean isAssistSwitchRouteForm(MesProEdhrBatchExecutionTaskDO task) {
        return task != null
                && MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(task.getNodeType());
    }

    private String resolveAssistSwitchUserDisplayName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        AdminUserRespDTO user = Objects.requireNonNull(userMap.get(userId),
                "EDHR_ASSIST_SWITCH_USER_REQUIRED: userId=" + userId);
        return StrUtil.blankToDefault(user.getNickname(), String.valueOf(userId));
    }

    private List<MesProBatchRecordExecutionRespVO.ReviewAssigneeOption> toReviewAssigneeOptions(
            List<ReviewSignatureAssignment> assignments) {
        return assignments.stream().map(assignment -> {
            MesProBatchRecordExecutionRespVO.ReviewAssigneeOption option =
                    new MesProBatchRecordExecutionRespVO.ReviewAssigneeOption();
            option.setSignatureCellKey(assignment.signatureCellKey());
            option.setSignatureRowIndex(assignment.signatureRowIndex());
            option.setSignatureColumnIndex(assignment.signatureColumnIndex());
            option.setReviewSourceType(assignment.reviewSourceType());
            option.setReviewSourceId(assignment.reviewSourceId());
            option.setReviewSourceIds(assignment.reviewSourceIds());
            option.setReviewSourceName(assignment.reviewSourceName());
            option.setCandidates(assignment.candidateUsers().stream().map(candidate -> {
                MesProBatchRecordExecutionRespVO.CandidateUser user =
                        new MesProBatchRecordExecutionRespVO.CandidateUser();
                user.setUserId(candidate.userId());
                user.setUserName(candidate.userName());
                return user;
            }).toList());
            return option;
        }).toList();
    }

    private MesProBatchRecordExecutionRespVO.AttachmentSummary buildAttachmentSummary(
            MesProBatchRecordExecutionAttachmentDO attachment) {
        MesProBatchRecordExecutionRespVO.AttachmentSummary summary =
                new MesProBatchRecordExecutionRespVO.AttachmentSummary();
        summary.setId(attachment.getId());
        summary.setAuditBatchId(attachment.getAuditBatchId());
        summary.setSignatureId(attachment.getSignatureId());
        summary.setExecutionId(attachment.getExecutionId());
        summary.setWorkTaskId(attachment.getWorkTaskId());
        summary.setRowIndex(attachment.getRowIndex());
        summary.setColumnIndex(attachment.getColumnIndex());
        summary.setFieldKey(attachment.getFieldKey());
        summary.setFieldPath(attachment.getFieldPath());
        summary.setFieldLabel(attachment.getFieldLabel());
        summary.setAttachmentType(attachment.getAttachmentType());
        summary.setAttachmentGroupKey(attachment.getAttachmentGroupKey());
        summary.setAttachmentAction(attachment.getAttachmentAction());
        summary.setVersionNo(attachment.getVersionNo());
        summary.setFileId(attachment.getFileId());
        summary.setFileUrl(attachment.getFileUrl());
        summary.setStorageConfigId(attachment.getStorageConfigId());
        summary.setStoragePath(attachment.getStoragePath());
        summary.setFileName(attachment.getFileName());
        summary.setContentType(attachment.getContentType());
        summary.setFileSize(attachment.getFileSize());
        summary.setSha256(attachment.getSha256());
        summary.setStorageRetentionHash(attachment.getStorageRetentionHash());
        summary.setPreviousAttachmentHash(attachment.getPreviousAttachmentHash());
        summary.setAttachmentHash(attachment.getAttachmentHash());
        summary.setOperatorId(attachment.getOperatorId());
        summary.setOperatorName(attachment.getOperatorName());
        summary.setOperatedAt(attachment.getOperatedAt());
        summary.setReasonCategory(attachment.getReasonCategory());
        summary.setReasonText(attachment.getReasonText());
        return summary;
    }

    private MesProBatchRecordExecutionOpenOrCreateByContextRespVO buildOpenOrCreateResp(MesProBatchRecordExecutionDO execution,
                                                                                         boolean created) {
        return new MesProBatchRecordExecutionOpenOrCreateByContextRespVO()
                .setId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setTemplateId(execution.getTemplateId())
                .setRouteProcessId(execution.getRouteProcessId())
                .setRouteId(execution.getRouteId())
                .setTaskId(execution.getTaskId())
                .setWorkstationId(execution.getWorkstationId())
                .setBatchRecordReportId(execution.getBatchRecordReportId())
                .setBatchRecordDefinitionId(execution.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(execution.getBatchRecordVersionId())
                .setBatchExecutionId(execution.getBatchExecutionId())
                .setInstanceScope(resolveExecutionInstanceScope(execution.getInstanceScope()))
                .setSharedFormKey(execution.getSharedFormKey())
                .setFormSlotType(execution.getFormSlotType())
                .setRecordCategory(execution.getRecordCategory())
                .setValidationProfile(execution.getValidationProfile())
                .setRecordbookEnabled(recordbookGlobalSettingService.resolveEffectiveRecordbookEnabled(
                        execution.getRecordbookEnabled(), execution.getRecordCategory()))
                .setPermissionScopeId(execution.getPermissionScopeId())
                .setRouteBindingId(execution.getRouteBindingId())
                .setRouteBindingSnapshotHash(execution.getRouteBindingSnapshotHash())
                .setArchiveVisibility(execution.getArchiveVisibility())
                .setSlotConfigSnapshotHash(execution.getSlotConfigSnapshotHash())
                .setBatchCode(execution.getBatchCode())
                .setStatus(execution.getStatus())
                .setActiveContextKey(buildExecutionActiveContextKey(execution))
                .setCreated(created);
    }

    private String resolveRecordCategory(String recordCategory) {
        String category = StrUtil.blankToDefault(StrUtil.trim(recordCategory), RECORD_CATEGORY_BATCH);
        if (!RECORD_CATEGORY_BATCH.equals(category) && !RECORD_CATEGORY_INTERNAL.equals(category)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return category;
    }

    private String resolveValidationProfile(String recordCategory, String validationProfile) {
        String category = resolveRecordCategory(recordCategory);
        String expectedProfile = RECORD_CATEGORY_INTERNAL.equals(category)
                ? VALIDATION_PROFILE_INTERNAL : VALIDATION_PROFILE_BATCH;
        String profile = StrUtil.blankToDefault(StrUtil.trim(validationProfile), expectedProfile);
        if (!expectedProfile.equals(profile)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return profile;
    }

    private Boolean resolveRecordbookEnabled(Boolean recordbookEnabled, String recordCategory) {
        String category = resolveRecordCategory(recordCategory);
        if (RECORD_CATEGORY_INTERNAL.equals(category)) {
            return Boolean.FALSE;
        }
        return recordbookEnabled == null ? Boolean.TRUE : recordbookEnabled;
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        return workOrder;
    }

    private MesProRouteProcessDO requireRouteProcess(Long routeProcessId, Long routeId, Long processId) {
        return routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, processId);
    }

    private MesProRouteProcessDO resolveBatchSharedTraceRouteProcess(
            MesProBatchRecordExecutionOpenOrCreateByContextReqVO reqVO) {
        if (reqVO.getRouteProcessId() == null && reqVO.getProcessId() == null) {
            return null;
        }
        return requireRouteProcess(reqVO.getRouteProcessId(), reqVO.getRouteId(), reqVO.getProcessId());
    }

    private String requireRequestedBatchRecordReportId(String requestedBatchRecordReportId) {
        String batchRecordReportId = StrUtil.trim(requestedBatchRecordReportId);
        if (StrUtil.isBlank(batchRecordReportId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return batchRecordReportId;
    }

    private MesProBatchRecordReportDO requireBatchRecordReport(String batchRecordReportId) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(batchRecordReportId);
        if (report == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return report;
    }

    private void validateLatestPublishedBatchRecordReport(MesProBatchRecordReportDO report) {
        if (report.getBatchRecordDefinitionId() == null || report.getBatchRecordVersionId() == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_LATEST_PUBLISHED_VERSION_REQUIRED);
        }
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectById(report.getBatchRecordDefinitionId());
        MesProBatchRecordVersionDO version = versionMapper.selectById(report.getBatchRecordVersionId());
        if (definition == null || version == null
                || !Objects.equals(definition.getId(), version.getDefinitionId())
                || !Objects.equals(definition.getCurrentVersionId(), version.getId())
                || !Objects.equals(BATCH_RECORD_VERSION_STATUS_APPROVED, version.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_LATEST_PUBLISHED_VERSION_REQUIRED);
        }
    }

    private String resolveBatchCode(String batchCode, MesProWorkOrderDO workOrder) {
        String resolved = StrUtil.trim(batchCode);
        if (StrUtil.isBlank(resolved)) {
            resolved = StrUtil.trim(workOrder.getBatchCode());
        }
        if (StrUtil.isBlank(resolved)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_BATCH_CODE_REQUIRED);
        }
        return resolved;
    }

    private String buildExecutionSnapshot(MesProBatchRecordTemplateDO template) {
        return JsonUtils.toJsonString(new ExecutionSnapshotPayload(
                template.getSheetLayoutJson(), template.getMetaJson()));
    }

    private RuntimeSnapshot buildRuntimeSnapshotFromReport(MesProBatchRecordReportDO report) {
        String reportJson = StrUtil.trim(jimuReportGateway.getReportJson(report.getReportId()));
        if (StrUtil.isBlank(reportJson)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE);
        }
        MesProBatchRecordRuntimeSnapshotSupport.RuntimeSnapshot runtimeSnapshot =
                runtimeSnapshotSupport.buildRuntimeSnapshot(report, reportJson);
        return new RuntimeSnapshot(runtimeSnapshot.sheetLayoutJson(), runtimeSnapshot.metaJson(),
                runtimeSnapshot.executionSnapshotJson());
    }

    private void materializeApprovedVersionCellRuleSnapshot(MesProBatchRecordReportDO report, JSONObject root) {
        if (report == null || report.getBatchRecordVersionId() == null) {
            return;
        }
        MesProBatchRecordVersionDO version = versionMapper.selectById(report.getBatchRecordVersionId());
        if (version == null || !"APPROVED".equals(version.getStatus())) {
            return;
        }
        if (versionMigrationItemMapper.countBlockingItems(version.getId()) > 0
                || !versionMigrationItemMapper.existsCellRuleReconciledEvidence(version.getId())) {
            return;
        }
        MesProBatchRecordCellRuleSupport.materializeVersionApprovedCellRules(root, report.getReportCode());
    }

    private Map<Long, MesProRouteProcessDO> getRouteProcessMap(List<MesProBatchRecordExecutionDO> executions) {
        Map<Long, MesProRouteProcessDO> result = new LinkedHashMap<>();
        for (MesProBatchRecordExecutionDO execution : executions) {
            if (execution.getRouteProcessId() == null) {
                continue;
            }
            result.put(execution.getRouteProcessId(), resolveExecutionRouteProcess(execution));
        }
        return result;
    }

    private MesProRouteProcessDO resolveExecutionRouteProcess(MesProBatchRecordExecutionDO execution) {
        if (execution == null || execution.getRouteProcessId() == null) {
            return null;
        }
        return routeProcessService.resolveFrozenRouteProcess(
                execution.getRouteProcessId(), execution.getRouteId(), null);
    }

    private Map<Long, MesProRouteDO> getRouteMap(Collection<MesProRouteProcessDO> routeProcesses) {
        Set<Long> routeIds = new LinkedHashSet<>();
        routeProcesses.stream().filter(item -> item != null && item.getRouteId() != null)
                .forEach(item -> routeIds.add(item.getRouteId()));
        if (routeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                routeMapper.selectBatchIds(routeIds), MesProRouteDO::getId);
    }

    private Map<Long, MesProProcessDO> getProcessMap(Collection<MesProRouteProcessDO> routeProcesses) {
        Set<Long> processIds = new LinkedHashSet<>();
        routeProcesses.stream().filter(item -> item != null && item.getProcessId() != null)
                .forEach(item -> processIds.add(item.getProcessId()));
        return processIds.isEmpty() ? Collections.emptyMap() : cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                processMapper.selectBatchIds(processIds), MesProProcessDO::getId);
    }

    private Map<Long, MesMdWorkstationDO> getWorkstationMap(List<MesProBatchRecordExecutionDO> executions) {
        Set<Long> workstationIds = new LinkedHashSet<>();
        executions.stream().map(MesProBatchRecordExecutionDO::getWorkstationId)
                .filter(id -> id != null).forEach(workstationIds::add);
        return workstationIds.isEmpty() ? Collections.emptyMap() : cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                workstationMapper.selectBatchIds(workstationIds), MesMdWorkstationDO::getId);
    }

    private Map<String, MesProBatchRecordReportDO> getReportMap(List<MesProBatchRecordExecutionDO> executions) {
        Set<String> reportIds = new LinkedHashSet<>();
        executions.stream().map(MesProBatchRecordExecutionDO::getBatchRecordReportId)
                .filter(StrUtil::isNotBlank).forEach(reportIds::add);
        if (reportIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                reportMapper.selectListByReportIds(reportIds), MesProBatchRecordReportDO::getReportId);
    }

    private String buildActiveContextKey(Long workOrderId, Long taskId, Long routeProcessId,
                                         Long workstationId, String batchRecordReportId, String batchCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        return String.format(Locale.ROOT, "%s:%s:%s:%s:%s:%s:%s",
                tenantId == null ? "0" : String.valueOf(tenantId),
                workOrderId == null ? "" : String.valueOf(workOrderId),
                taskId == null ? "" : String.valueOf(taskId),
                routeProcessId == null ? "" : String.valueOf(routeProcessId),
                workstationId == null ? "" : String.valueOf(workstationId),
                StrUtil.nullToEmpty(batchRecordReportId),
                StrUtil.nullToEmpty(batchCode));
    }

    private String buildExecutionActiveContextKey(MesProBatchRecordExecutionDO execution) {
        if (execution != null && INSTANCE_SCOPE_BATCH_SHARED.equals(resolveExecutionInstanceScope(execution.getInstanceScope()))) {
            return buildBatchSharedActiveContextKey(execution.getWorkOrderId(), execution.getBatchExecutionId(),
                    execution.getBatchRecordReportId(), execution.getSharedFormKey(), execution.getBatchCode());
        }
        return buildActiveContextKey(execution.getWorkOrderId(), execution.getTaskId(),
                execution.getRouteProcessId(), execution.getWorkstationId(),
                execution.getBatchRecordReportId(), execution.getBatchCode());
    }

    private String buildBatchSharedActiveContextKey(Long workOrderId, Long batchExecutionId,
                                                    String batchRecordReportId, String sharedFormKey,
                                                    String batchCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        return String.format(Locale.ROOT, "%s:%s:%s:%s:%s:%s:%s",
                INSTANCE_SCOPE_BATCH_SHARED,
                tenantId == null ? "0" : String.valueOf(tenantId),
                workOrderId == null ? "" : String.valueOf(workOrderId),
                batchExecutionId == null ? "" : String.valueOf(batchExecutionId),
                StrUtil.nullToEmpty(batchRecordReportId),
                StrUtil.nullToEmpty(sharedFormKey),
                StrUtil.nullToEmpty(batchCode));
    }

    private void applyActiveContextFilter(MesProBatchRecordExecutionPageReqVO pageReqVO, ActiveContextFilter filter) {
        if (!filter.present()) {
            return;
        }
        pageReqVO.setWorkOrderId(filter.workOrderId());
        pageReqVO.setBatchExecutionId(filter.batchExecutionId());
        pageReqVO.setInstanceScope(filter.instanceScope());
        pageReqVO.setSharedFormKey(filter.sharedFormKey());
        pageReqVO.setTaskId(filter.taskId());
        pageReqVO.setRouteProcessId(filter.routeProcessId());
        pageReqVO.setWorkstationId(filter.workstationId());
        pageReqVO.setBatchRecordReportId(filter.batchRecordReportId());
        pageReqVO.setBatchCode(filter.batchCode());
    }

    private ActiveContextFilter parseActiveContextFilter(String activeContextKey) {
        if (StrUtil.isBlank(activeContextKey)) {
            return ActiveContextFilter.absentFilter();
        }
        String[] parts = StrUtil.splitToArray(activeContextKey, ':');
        if (parts.length == 7 && INSTANCE_SCOPE_BATCH_SHARED.equals(parts[0])) {
            Long tenantId = parseNullableLong(parts[1]);
            Long currentTenantId = TenantContextHolder.getTenantId();
            boolean tenantMismatch = tenantId != null && currentTenantId != null && !tenantId.equals(currentTenantId);
            return new ActiveContextFilter(true, false, tenantMismatch,
                    parseNullableLong(parts[2]),
                    parseNullableLong(parts[3]),
                    INSTANCE_SCOPE_BATCH_SHARED,
                    StrUtil.emptyToNull(parts[5]),
                    null,
                    null,
                    null,
                    StrUtil.emptyToNull(parts[4]),
                    StrUtil.emptyToNull(parts[6]));
        }
        if (parts.length != 7) {
            return ActiveContextFilter.invalidFilter();
        }
        Long tenantId = parseNullableLong(parts[0]);
        Long currentTenantId = TenantContextHolder.getTenantId();
        boolean tenantMismatch = tenantId != null && currentTenantId != null && !tenantId.equals(currentTenantId);
        return new ActiveContextFilter(true, false, tenantMismatch,
                parseNullableLong(parts[1]),
                null,
                null,
                null,
                parseNullableLong(parts[2]),
                parseNullableLong(parts[3]),
                parseNullableLong(parts[4]),
                StrUtil.emptyToNull(parts[5]),
                StrUtil.emptyToNull(parts[6]));
    }

    private Long parseNullableLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return Long.valueOf(value);
    }

    private String resolveExecutionInstanceScope(String instanceScope) {
        String scope = StrUtil.blankToDefault(StrUtil.trim(instanceScope), INSTANCE_SCOPE_PROCESS);
        if (!INSTANCE_SCOPE_PROCESS.equals(scope) && !INSTANCE_SCOPE_BATCH_SHARED.equals(scope)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SHARED_CONTEXT_REQUIRED);
        }
        return scope;
    }

    private JSONObject buildSnapshotSource(MesProBatchRecordReportDO report) {
        JSONObject source = new JSONObject(true);
        source.put("type", "JMREPORT");
        source.put("reportId", report.getReportId());
        source.put("reportCode", report.getReportCode());
        source.put("reportName", report.getReportName());
        return source;
    }

    private JSONObject buildSnapshotLayout(JSONObject root) {
        JSONObject layout = new JSONObject(true);
        layout.put("rows", root.getJSONObject("rows"));
        layout.put("cols", root.getJSONObject("cols"));
        layout.put("merges", root.getJSONArray("merges"));
        return layout;
    }

    private JSONObject buildSnapshotMeta(JSONObject root, MesProBatchRecordReportDO report) {
        JSONObject meta = new JSONObject(true);
        meta.put("name", root.getString("name"));
        meta.put("tableTitle", report.getTableTitle());
        meta.put("sourceTableIndex", report.getSourceTableIndex());
        meta.put("fillFormInfo", root.getJSONObject("fillFormInfo"));
        meta.put("printConfig", root.getJSONObject("printConfig"));
        meta.put("dataRectWidth", root.get("dataRectWidth"));
        return meta;
    }

    private JSONArray extractSnapshotFields(JSONObject root) {
        JSONArray fields = new JSONArray();
        JSONObject rows = root.getJSONObject("rows");
        if (rows == null || rows.isEmpty()) {
            return fields;
        }
        List<Integer> rowIndexes = rows.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .toList();
        for (Integer rowIndex : rowIndexes) {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            if (row == null) {
                continue;
            }
            JSONObject cells = row.getJSONObject("cells");
            if (cells == null || cells.isEmpty()) {
                continue;
            }
            List<Integer> columnIndexes = cells.keySet().stream()
                    .filter(StrUtil::isNumeric)
                    .map(Integer::valueOf)
                    .sorted()
                    .toList();
            for (Integer columnIndex : columnIndexes) {
                JSONObject cell = cells.getJSONObject(String.valueOf(columnIndex));
                if (cell == null) {
                    continue;
                }
                JSONObject fillForm = cell.getJSONObject("fillForm");
                if (fillForm == null || StrUtil.isBlank(fillForm.getString("field"))) {
                    continue;
                }
                JSONObject cellRule = cell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
                if (cellRule == null && MesProBatchRecordCellRuleSupport.hasValidSignatureMarker(cell)) {
                    continue;
                }
                JSONObject field = new JSONObject(true);
                field.put("fieldPath", buildSnapshotFieldPath(rowIndex, columnIndex, fillForm.getString("field")));
                field.put("fieldKey", fillForm.getString("field"));
                field.put("label", resolveFieldLabel(rows, rowIndex, columnIndex, cell, fillForm, cellRule));
                field.put("rowIndex", rowIndex);
                field.put("columnIndex", columnIndex);
                String valueType = cellRule.getString("valueType");
                field.put("valueType", valueType);
                field.put("component", MesProBatchRecordCellRuleSupport.defaultComponentFlag(valueType,
                        StrUtil.blankToDefault(cellRule.getString("componentFlag"),
                                StrUtil.blankToDefault(fillForm.getString("componentFlag"),
                                        StrUtil.blankToDefault(fillForm.getString("component"), "input-text")))));
                field.put("required", Boolean.TRUE.equals(cellRule.getBoolean("required")));
                putIfPresent(field, "placeholder", firstNonBlank(
                        cellRule.getString("placeholder"),
                        fillForm.getString("placeholder")));
                putIfPresent(field, "helpText", firstNonBlank(
                        cellRule.getString("helpText"),
                        fillForm.getString("helpText")));
                JSONObject snapshotCellRule = copySnapshotJsonObject(cellRule);
                field.put("constraints", copySnapshotJsonObject(snapshotCellRule.getJSONObject("constraints")));
                putIfPresent(field, "options", resolveSnapshotFieldOptions(snapshotCellRule, fillForm));
                JSONObject attachmentRule = snapshotCellRule.getJSONObject("attachmentRule");
                if (attachmentRule != null && !attachmentRule.isEmpty()) {
                    field.put("attachmentRule", copySnapshotJsonObject(attachmentRule));
                }
                putIfPresent(field, "unit", snapshotCellRule.getString("unit"));
                field.put(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY, snapshotCellRule);
                putIfPresent(field, "defaultValue", copySnapshotJsonValue(fillForm.get("defaultValue")));
                putIfPresent(field, "value", copySnapshotJsonValue(fillForm.get("value")));
                fields.add(field);
            }
        }
        return fields;
    }

    private JSONArray extractSnapshotAssistRows(JSONObject root) {
        JSONArray assistRows = root == null
                ? null : root.getJSONArray(MesProBatchRecordCellRuleSupport.ASSIST_ROWS_KEY);
        if (assistRows == null) {
            return new JSONArray();
        }
        MesProBatchRecordCellRuleSupport.validateAssistRows(
                root, MesProBatchRecordCellRuleSupport.extractAssistRows(root));
        return JSON.parseArray(assistRows.toJSONString());
    }

    private void validateConfirmedCellRules(JSONObject root) {
        List<String> unreviewedCoordinates = MesProBatchRecordCellRuleSupport.unreviewedFillableCoordinates(root);
        if (!unreviewedCoordinates.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED,
                    String.join("、", unreviewedCoordinates));
        }
        try {
            MesProBatchRecordCellRuleSupport.forEachCell(root, (rowIndex, columnIndex, cell) -> {
                JSONObject rule = cell.getJSONObject(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
                if (rule == null) {
                    return;
                }
                MesProBatchRecordCellRuleSupport.validateRule(
                        MesProBatchRecordCellRuleSupport.toRuleVO(rowIndex, columnIndex, rule), cell);
            });
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID, ex.getMessage());
        }
    }

    private void putIfPresent(JSONObject target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private JSONObject copySnapshotJsonObject(JSONObject source) {
        return source == null ? new JSONObject(true) : JSON.parseObject(source.toJSONString());
    }

    private Object copySnapshotJsonValue(Object value) {
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return JSON.parse(JSON.toJSONString(value));
        }
        return value;
    }

    private Object resolveSnapshotFieldOptions(JSONObject cellRule, JSONObject fillForm) {
        JSONObject constraints = cellRule == null ? null : cellRule.getJSONObject("constraints");
        if (constraints != null && constraints.get("options") != null) {
            return constraints.get("options");
        }
        if (cellRule != null && cellRule.get("options") != null) {
            return cellRule.get("options");
        }
        return fillForm == null ? null : fillForm.get("options");
    }

    private String buildSnapshotFieldPath(Integer rowIndex, Integer columnIndex, String fieldKey) {
        return String.format(Locale.ROOT, "sheet[0].rows[%d].cells[%d].%s", rowIndex, columnIndex, fieldKey);
    }

    private String resolveFieldLabel(JSONObject rows, Integer rowIndex, Integer columnIndex,
                                     JSONObject cell, JSONObject fillForm, JSONObject cellRule) {
        String direct = firstNonBlank(
                cellRule.getString("label"),
                fillForm.getString("label"),
                fillForm.getString("labelText"),
                cell.getString("text"));
        if (StrUtil.isNotBlank(direct)) {
            return direct.trim();
        }
        JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
        if (row == null) {
            return fillForm.getString("field");
        }
        JSONObject cells = row.getJSONObject("cells");
        if (cells == null) {
            return fillForm.getString("field");
        }
        for (int cursor = columnIndex - 1; cursor >= 0; cursor--) {
            JSONObject leftCell = cells.getJSONObject(String.valueOf(cursor));
            if (leftCell == null) {
                continue;
            }
            String text = StrUtil.trim(leftCell.getString("text"));
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return fillForm.getString("field");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String buildExecutionCode(Long id) {
        return "BRE" + LocalDateTime.now().format(EXECUTION_CODE_TIME_FORMATTER)
                + String.format(Locale.ROOT, "%04d", id % 10000);
    }

    private record ExecutionSnapshotPayload(String sheetLayoutJson, String metaJson) {
    }

    private record RuntimeSnapshot(String sheetLayoutJson, String metaJson, String executionSnapshotJson) {
    }

    private record ApprovalExecutionContext(Long processId,
                                            String processName,
                                            Long workstationId,
                                            String workstationName,
                                            String executionSnapshotJson,
                                            String cellValuesJson,
                                            String cellValuesHash,
                                            Long fieldAuditRevision,
                                            String fieldAuditHeadHash,
                                            Long tenantId) {
    }

    private record FieldAuditEvidence(String cellValuesHash,
                                      Long fieldAuditRevision,
                                      String fieldAuditHeadHash) {
    }

    private record ReviewSignatureAssignment(String signatureCellKey,
                                             Integer signatureRowIndex,
                                             Integer signatureColumnIndex,
                                             String reviewSourceType,
                                             Long reviewSourceId,
                                             List<Long> reviewSourceIds,
                                             String reviewSourceName,
                                             List<MesProEdhrCandidateUser> candidateUsers,
                                             Long assigneeUserId,
                                             String assigneeUserName) {

        private List<Long> candidateUserIds() {
            return candidateUsers.stream()
                    .map(MesProEdhrCandidateUser::userId)
                    .toList();
        }

        private List<Long> reviewTaskUserIds() {
            if (assigneeUserId != null) {
                return List.of(assigneeUserId);
            }
            return candidateUserIds();
        }

        private ReviewSignatureAssignment withAssignee(Long assigneeUserId) {
            String assigneeUserName = candidateUsers.stream()
                    .filter(user -> Objects.equals(user.userId(), assigneeUserId))
                    .map(MesProEdhrCandidateUser::userName)
                    .findFirst()
                    .orElse(String.valueOf(assigneeUserId));
            return new ReviewSignatureAssignment(signatureCellKey, signatureRowIndex, signatureColumnIndex,
                    reviewSourceType, reviewSourceId, reviewSourceIds, reviewSourceName, candidateUsers,
                    assigneeUserId, assigneeUserName);
        }
    }

    private record ActiveContextFilter(boolean present, boolean invalid, boolean tenantMismatch,
                                       Long workOrderId, Long batchExecutionId, String instanceScope,
                                       String sharedFormKey, Long taskId, Long routeProcessId,
                                       Long workstationId, String batchRecordReportId, String batchCode) {

        private static ActiveContextFilter absentFilter() {
            return new ActiveContextFilter(false, false, false, null, null, null, null, null, null, null, null, null);
        }

        private static ActiveContextFilter invalidFilter() {
            return new ActiveContextFilter(true, true, false, null, null, null, null, null, null, null, null, null);
        }
    }
}
