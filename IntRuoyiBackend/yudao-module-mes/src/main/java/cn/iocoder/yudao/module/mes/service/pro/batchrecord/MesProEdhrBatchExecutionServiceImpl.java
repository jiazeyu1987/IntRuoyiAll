package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveDownloadRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureTimeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordApprovalSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchDossierItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFlowEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchDossierItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFlowEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProEdhrDossierConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowContextMatcher;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPublishProjectionServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_PROCESS_FLOW_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_CLOSE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_CLOSE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ROUTE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_ROUTE_VERSION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_STERILIZATION_BATCH_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;

@Service
public class MesProEdhrBatchExecutionServiceImpl implements MesProEdhrBatchExecutionService {

    public static final int BATCH_STATUS_CREATED = 0;
    public static final int BATCH_STATUS_IN_PROGRESS = 10;
    public static final int BATCH_STATUS_READY_TO_CLOSE = 20;
    public static final int BATCH_STATUS_REWORK_REQUIRED = 25;
    public static final int BATCH_STATUS_CLOSED = 30;
    public static final int BATCH_STATUS_ARCHIVED = 40;
    public static final int BATCH_STATUS_REJECTED = 50;
    public static final int BATCH_STATUS_VOIDED = 60;

    private static final String CHANGE_TYPE_VOID = "VOID";
    private static final String CHANGE_TYPE_REEXECUTE = "REEXECUTE";
    private static final String CHANGE_STATUS_SUBMITTED = "SUBMITTED";
    private static final String CHANGE_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String TARGET_SCOPE_BATCH = "BATCH";
    private static final String PENDING_VOID_ACTION_LOCK_REASON = "作废申请待处理，只能撤回作废申请";
    private static final String PENDING_RELEASE_ACTION_LOCK_REASON = "放行审批待处理，只能处理放行审批或撤回放行";
    private static final String VOIDED_ACTION_LOCK_REASON = "批次已作废，只能追溯审计";

    public static final int TASK_STATUS_WAITING = 0;
    public static final int TASK_STATUS_DRAFT = 10;
    public static final int TASK_STATUS_SUBMITTED = 20;
    public static final int TASK_STATUS_REJECTED = 30;
    public static final int TASK_STATUS_REWORK_REQUIRED = 35;
    public static final int TASK_STATUS_APPROVED = 40;
    public static final int TASK_STATUS_SKIPPED = 45;
    public static final int TASK_STATUS_BLOCKED = 50;
    private static final String FLOW_EVENT_TYPE_FLOW_INTERVENTION = "FLOW_INTERVENTION";

    public static final String NODE_TYPE_ROUTE_FORM = "ROUTE_FORM";
    public static final String NODE_TYPE_INCOMING_INSPECTION_REPORT = "INCOMING_INSPECTION_REPORT";
    public static final String NODE_TYPE_STERILIZATION_REPORT = "STERILIZATION_REPORT";
    public static final String NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT = "FINISHED_PRODUCT_INSPECTION_REPORT";
    public static final String NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD = "FINISHED_PRODUCT_INSPECTION_RECORD";

    private static final Set<String> SKIPPABLE_SPECIAL_NODE_TYPES = Set.of(
            NODE_TYPE_INCOMING_INSPECTION_REPORT,
            NODE_TYPE_STERILIZATION_REPORT,
            NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
            NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD);
    private static final int SPECIAL_SORT_INCOMING_INSPECTION = 0;
    private static final int SPECIAL_SORT_STERILIZATION = 9000;
    private static final int SPECIAL_SORT_FINISHED_PRODUCT_INSPECTION_REPORT = 9010;
    private static final int SPECIAL_SORT_FINISHED_PRODUCT_INSPECTION_RECORD = 9020;

    private static final Set<Integer> COMPLETED_FORM_EXECUTION_STATUSES = Set.of(3, 4);
    private static final Set<String> REQUIRED_FORM_SIGNATURES = Set.of("SUBMIT");
    private static final String DEFAULT_SIGNATURE_DISPLAY_FORMAT = "ACTOR_SIGNED_AT";
    private static final String SIGNATURE_TIME_MODE_SERVER = "SERVER_TIME";
    private static final String SIGNATURE_TIME_MODE_USER_SELECTED = "USER_SELECTED";
    private static final String SIGNATURE_TIME_POLICY_VERSION = "EDHR_SIGNATURE_TIME_V1";
    private static final String ARCHIVE_STATUS_SEALED = "SEALED";
    private static final String ARTIFACT_TYPE_BATCH_FINAL_PDF = "BATCH_FINAL_PDF";
    private static final Set<String> OPEN_RECORD_CHANGE_STATUSES = Set.of("DRAFT", "SUBMITTED", "APPROVED");
    private static final String WORK_TASK_TYPE_FILL = "FILL";
    private static final String WORK_TASK_TYPE_REWORK = "REWORK";
    private static final String WORK_TASK_TYPE_REVIEW = "REVIEW";
    private static final String WORK_TASK_TYPE_APPROVE = "APPROVE";
    private static final String WORK_TASK_TYPE_ARCHIVE = "ARCHIVE";
    private static final String WORK_TASK_TYPE_CLOSE = "CLOSE";
    private static final String PROCESS_RULE_TYPE_FILL = "FILL";
    private static final String PROCESS_RULE_TYPE_EQUIPMENT_FILL = "EQUIPMENT_FILL";
    private static final String PROCESS_RULE_TYPE_QUALITY_FILL = "QUALITY_FILL";
    private static final String RULE_SCOPE_TYPE_ROUTE = "ROUTE";
    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String CANDIDATE_SOURCE_TYPE_USERS = "USERS";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE = "ROLE";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE_GROUP = "ROLE_GROUP";
    private static final String CANDIDATE_SOURCE_TYPE_DEPT = "DEPT";
    private static final String CANDIDATE_SOURCE_TYPE_DEPT_GROUP = "DEPT_GROUP";
    private static final String EXECUTION_MODE_SEQUENTIAL = "SEQUENTIAL";
    private static final String EXECUTION_MODE_PARALLEL = "PARALLEL";
    private static final String FORM_SLOT_MAIN = "MAIN";
    private static final String INSTANCE_SCOPE_PROCESS = "PROCESS";
    private static final String INSTANCE_SCOPE_BATCH_SHARED = "BATCH_SHARED";
    private static final String FORM_POLICY_DATA_DOMAIN = "MES";
    private static final String FORM_POLICY_SYSTEM_CODE = "MES";
    private static final String FORM_POLICY_OBJECT_TYPE = "EDHR_ROUTE_FORM";
    private static final String FORM_POLICY_OBJECT_STATE = "ACTIVE";
    private static final String FORM_POLICY_EFFECT_EXECUTOR_CODE = "MES_EDHR_ROUTE_FORM_FILL";
    private static final String SLOT_TYPE_PROCESS_INSPECTION = "PROCESS_INSPECTION";
    private static final String SLOT_TYPE_PARAMETER_RECORD = "PARAMETER_RECORD";
    private static final String RECORD_CATEGORY_BATCH = "BATCH_RECORD";
    private static final String RECORD_CATEGORY_INTERNAL = "INTERNAL_RECORD";
    private static final String VALIDATION_PROFILE_BATCH = "CONTROLLED_BATCH";
    private static final String VALIDATION_PROFILE_INTERNAL = "INTERNAL_TRACE";
    private static final String REQUIRED_POLICY_REQUIRED = "REQUIRED";
    private static final String ARCHIVE_VISIBILITY_FINAL_DHR = "FINAL_DHR";
    private static final String OWNER_ROLE_PRODUCTION = "PRODUCTION";
    private static final String OWNER_ROLE_QUALITY = "QUALITY";
    private static final String OWNER_ROLE_EQUIPMENT = "EQUIPMENT";
    private static final Set<String> ROUTE_INSTANCE_SCOPES = Set.of(INSTANCE_SCOPE_PROCESS, INSTANCE_SCOPE_BATCH_SHARED);
    private static final Set<String> ROUTE_FORM_SLOT_TYPES = Set.of(
            FORM_SLOT_MAIN, "LOSS_REPORT", "PROCESS_INSPECTION", "PARAMETER_RECORD", "OTHER_INTERNAL");
    private static final Set<String> ROUTE_RECORD_CATEGORIES = Set.of("BATCH_RECORD", "INTERNAL_RECORD");
    private static final Set<String> ROUTE_VALIDATION_PROFILES = Set.of("CONTROLLED_BATCH", "INTERNAL_TRACE");
    private static final Set<String> ROUTE_REQUIRED_POLICIES = Set.of(
            "REQUIRED", "CONDITIONAL_REQUIRED", "OPTIONAL", "SKIPPABLE_CONTROLLED");
    private static final Set<String> ROUTE_ARCHIVE_VISIBILITIES = Set.of(
            "FINAL_DHR", "INTERNAL_REVIEW", "AUDIT_ONLY", "ATTACHMENT_REFERENCE");
    private static final String PRINTABLE_ARCHIVE_SCHEMA_VERSION = "EDHR_BATCH_PRINTABLE_ARCHIVE_V1";
    private static final String BATCH_ARCHIVE_PDF_FONT_PATH = "C:/Windows/Fonts/simhei.ttf";
    private static final String BATCH_ARCHIVE_PDF_SYMBOL_FONT_PATH = "C:/Windows/Fonts/seguisym.ttf";
    private static final float BATCH_ARCHIVE_PDF_MARGIN = 48F;
    private static final float BATCH_ARCHIVE_PDF_FONT_SIZE = 10F;
    private static final float BATCH_ARCHIVE_PDF_TITLE_FONT_SIZE = 16F;
    private static final float BATCH_ARCHIVE_PDF_LEADING = 14F;
    private static final int BATCH_ARCHIVE_PDF_WRAP_CHARS = 96;
    private static final Long SPECIAL_NODE_ATTACHMENT_EXECUTION_ID = 0L;
    private static final String SPECIAL_NODE_ATTACHMENT_UPLOAD_PREFIX = "EDHR_SPECIAL_NODE_ATTACHMENT";
    private static final String SPECIAL_NODE_ATTACHMENT_RETENTION_PREFIX =
            "EDHR_SPECIAL_NODE_ATTACHMENT_V1:RETENTION\n";
    private static final String SPECIAL_NODE_ATTACHMENT_LEDGER_PREFIX =
            "EDHR_SPECIAL_NODE_ATTACHMENT_V1:LEDGER\n";
    private static final String SPECIAL_NODE_ATTACHMENT_REASON_CATEGORY = "SPECIAL_NODE_ATTACHMENT";
    private static final String SPECIAL_NODE_ATTACHMENT_ACTION_ADD = "ADD";
    private static final String SPECIAL_NODE_ATTACHMENT_ACTION_PENDING = "PENDING";
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    public static final String DOSSIER_ITEM_TYPE_FINAL_INSPECTION =
            MesProEdhrDossierConstants.ITEM_TYPE_FINAL_INSPECTION;
    private static final String DOSSIER_ITEM_KEY_FINAL_INSPECTION =
            MesProEdhrDossierConstants.ITEM_KEY_FINAL_INSPECTION;
    private static final String DOSSIER_ITEM_NAME_FINAL_INSPECTION =
            MesProEdhrDossierConstants.ITEM_NAME_FINAL_INSPECTION;
    private static final String DOSSIER_ITEM_STATUS_PENDING =
            MesProEdhrDossierConstants.ITEM_STATUS_PENDING;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionVisibilityService batchExecutionVisibilityService;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrFlowEventMapper flowEventMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;
    @Resource
    private MesProEdhrBatchDossierItemMapper dossierItemMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
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
    private MesProBatchRecordExecutionFieldAuditBatchMapper fieldAuditBatchMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper executionSignatureMapper;
    @Resource
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Resource
    private MesProBatchRecordDomainTraceSnapshotMapper domainTraceSnapshotMapper;
    @Resource
    private MesProBatchRecordExecutionService singleExecutionService;
    @Resource
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private MesProEdhrPermissionGateService permissionGateService;
    @Resource
    private MesProEdhrRecordChangeEventMapper recordChangeEventMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper workTaskAssignmentRuleMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProEdhrCandidateResolver candidateResolver;
    @Resource
    private FileService fileService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private MesProEdhrBatchStageResolver batchStageResolver;
    @Resource
    private MesProEdhrPreReleaseEditabilityService preReleaseEditabilityService;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @Resource
    private FormTemplateVersionMapper formTemplateVersionMapper;
    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<EdhrBatchExecutionRespVO> getPage(EdhrBatchExecutionPageReqVO reqVO) {
        Long currentUserId = currentUserId();
        PageResult<EdhrBatchExecutionRespVO> result;
        if (batchExecutionVisibilityService.hasOverviewPermission(currentUserId)) {
            PageResult<MesProEdhrBatchExecutionDO> page = batchExecutionMapper.selectPage(reqVO);
            result = new PageResult<>(page.getList().stream()
                    .map(this::toPageResp)
                    .toList(), page.getTotal());
        } else {
            List<MesProEdhrBatchExecutionDO> visibleBatches = batchExecutionMapper.selectList(reqVO).stream()
                    .filter(batch -> batchExecutionVisibilityService.canViewBatch(batch, currentUserId))
                    .toList();
            result = new PageResult<>(sliceVisibleBatches(visibleBatches, reqVO).stream()
                    .map(this::toPageResp)
                    .toList(), (long) visibleBatches.size());
        }
        recordOperationAudit("BATCH_EXECUTION_PAGE", "LIST", "QUERY",
                "查询 eDHR 批次列表", null, null, null, null, null,
                null, null, "mes:pro-edhr-batch-execution:query", "ALLOW",
                "SUCCESS", null, null, null);
        return result;
    }

    private List<MesProEdhrBatchExecutionDO> sliceVisibleBatches(List<MesProEdhrBatchExecutionDO> batches,
                                                                 EdhrBatchExecutionPageReqVO reqVO) {
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            return batches;
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int pageSize = reqVO.getPageSize() == null ? 10 : Math.max(reqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, batches.size());
        int toIndex = Math.min(fromIndex + pageSize, batches.size());
        return batches.subList(fromIndex, toIndex);
    }

    private EdhrBatchExecutionRespVO toPageResp(MesProEdhrBatchExecutionDO batch) {
        try {
            return toResp(batch);
        } catch (ServiceException ex) {
            if (!isDefaultReportRequired(ex)) {
                throw ex;
            }
            return toBlockedResp(batch, ex);
        }
    }

    private boolean isDefaultReportRequired(ServiceException ex) {
        return Objects.equals(ex.getCode(), PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED.getCode());
    }

    private EdhrBatchExecutionRespVO toBlockedResp(MesProEdhrBatchExecutionDO batch, ServiceException ex) {
        EdhrBatchExecutionRespVO response = toBlockedPageResp(batch);
        List<String> blockers = new ArrayList<>();
        blockers.add(ex.getMessage());
        if (response.getCloseBlockers() != null) {
            blockers.addAll(response.getCloseBlockers());
        }
        response.setCanClose(Boolean.FALSE)
                .setCanArchive(Boolean.FALSE)
                .setCloseBlockers(blockers)
                .setStageBlockers(blockers);
        return response;
    }

    private EdhrBatchExecutionRespVO toBlockedPageResp(MesProEdhrBatchExecutionDO batch) {
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(batch.getId());
        MesProEdhrReleaseTransactionDO releaseTransaction =
                releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        String mainStage = batchStageResolver.resolveMainStageCode(latest, releaseTransaction);
        boolean pendingReleaseApproval = isReleasePendingApproval(releaseTransaction);
        List<String> blockers = collectCloseBlockers(batch.getId());
        return new EdhrBatchExecutionRespVO()
                .setId(latest.getId())
                .setBatchExecutionCode(latest.getBatchExecutionCode())
                .setWorkOrderId(latest.getWorkOrderId())
                .setWorkOrderCode(latest.getWorkOrderCode())
                .setBatchCode(latest.getBatchCode())
                .setCreateTime(latest.getCreateTime())
                .setUpdateTime(latest.getUpdateTime())
                .setAttemptNo(latest.getAttemptNo())
                .setSourceRejectedBatchExecutionId(latest.getSourceRejectedBatchExecutionId())
                .setSupersededByBatchExecutionId(latest.getSupersededByBatchExecutionId())
                .setReexecutedByChangeEventId(latest.getReexecutedByChangeEventId())
                .setProductId(latest.getProductId())
                .setProductCode(latest.getProductCode())
                .setProductName(latest.getProductName())
                .setRouteId(latest.getRouteId())
                .setRouteVersionId(latest.getRouteVersionId())
                .setRouteVersionNo(latest.getRouteVersionNo())
                .setRouteCode(latest.getRouteCode())
                .setRouteName(latest.getRouteName())
                .setStatus(latest.getStatus())
                .setTaskTotal(latest.getTaskTotal())
                .setTaskApprovedCount(latest.getTaskApprovedCount())
                .setBlockedCount(latest.getBlockedCount())
                .setMainStage(mainStage)
                .setMainStageLabel(batchStageResolver.resolveMainStageDisplayLabel(mainStage))
                .setStageOwnerRole(batchStageResolver.resolveStageOwnerRole(mainStage))
                .setStageBlockers(blockers)
                .setCanClose(Boolean.FALSE)
                .setCanArchive(Boolean.FALSE)
                .setCloseBlockers(blockers)
                .setTasks(List.of())
                .setReleaseActionLocked(pendingReleaseApproval)
                .setReleaseActionLockReason(pendingReleaseApproval ? PENDING_RELEASE_ACTION_LOCK_REASON : null)
                .setCanWithdrawVoidRequest(Boolean.FALSE)
                .setClosedBy(latest.getClosedBy())
                .setClosedAt(latest.getClosedAt())
                .setCloseSignatureId(latest.getCloseSignatureId())
                .setRejectSignatureId(latest.getRejectSignatureId())
                .setRejectedBy(latest.getRejectedBy())
                .setRejectedAt(latest.getRejectedAt())
                .setRejectReason(latest.getRejectReason())
                .setAggregateHash(latest.getAggregateHash());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO get(Long id) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(id);
        EdhrBatchExecutionRespVO result;
        try {
            syncIfActive(batch);
            batch = batchExecutionMapper.selectById(batch.getId());
            batchExecutionVisibilityService.requireVisibleBatch(batch, currentUserId());
            result = toResp(batch);
        } catch (ServiceException ex) {
            if (!isDefaultReportRequired(ex)) {
                throw ex;
            }
            result = toBlockedResp(batch, ex);
        }
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(batch.getId()), "VIEW",
                "查看 eDHR 批次详情", batch.getId(), null, null, batch.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:query", "ALLOW",
                "SUCCESS", null, null, null);
        return result;
    }

    @Override
    public List<EdhrBatchExecutionRouteOptionRespVO> listRouteOptionsByWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = validateSelectableWorkOrder(workOrderId);
        return resolveEnabledProductRoutes(workOrder).stream()
                .map(this::toRouteOption)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO openOrCreate(EdhrBatchExecutionOpenOrCreateReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getBatchCode())) {
            throw exception(BAD_REQUEST);
        }
        MesProWorkOrderDO workOrder = validateSelectableWorkOrder(reqVO.getWorkOrderId());
        String batchCode = reqVO.getBatchCode().trim();
        if (reqVO.getRouteId() == null) {
            List<MesProEdhrBatchExecutionDO> existingBatches =
                    batchExecutionMapper.selectListByWorkOrderIdAndBatchCode(reqVO.getWorkOrderId(), batchCode);
            if (existingBatches.size() == 1) {
                return openExistingBatch(existingBatches.get(0));
            }
            if (existingBatches.size() > 1) {
                long routeCount = existingBatches.stream()
                        .map(MesProEdhrBatchExecutionDO::getRouteId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();
                if (routeCount == 1) {
                    return openExistingBatch(existingBatches.get(0));
                }
                throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED);
            }
        }
        Long routeId = resolveRouteId(reqVO.getRouteId(), workOrder);
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionDO existing = batchExecutionMapper.selectByContext(
                reqVO.getWorkOrderId(), batchCode, routeId);
        if (existing != null) {
            return openExistingBatch(existing);
        }
        if (!CommonStatusEnum.isEnable(route.getStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (activeRouteVersion == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_VERSION_REQUIRED, route.getId());
        }

        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("EDHRB-" + System.currentTimeMillis())
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setBatchCode(batchCode)
                .setActiveContextKey(buildActiveContextKey(workOrder.getId(), batchCode, route.getId()))
                .setAttemptNo(1)
                .setProductId(workOrder.getProductId())
                .setProductCode(String.valueOf(workOrder.getProductId()))
                .setProductName(workOrder.getName())
                .setRouteId(route.getId())
                .setRouteVersionId(activeRouteVersion.getId())
                .setRouteVersionNo(activeRouteVersion.getVersionNo())
                .setRouteSnapshotJson(activeRouteVersion.getRouteSnapshotJson())
                .setRouteCode(route.getCode())
                .setRouteName(route.getName())
                .setStatus(BATCH_STATUS_CREATED)
                .setTaskApprovedCount(0)
                .setBlockedCount(0)
                .setRemark(reqVO.getRemark());
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        List<BatchTaskConfig> taskConfigs = buildBatchTaskConfigs(batch, route, routeProcesses);
        batch.setTaskTotal(taskConfigs.size());
        batchExecutionMapper.insert(batch);
        createDefaultDossierItems(batch);

        List<MesProEdhrBatchExecutionTaskDO> insertedTasks = new ArrayList<>();
        for (BatchTaskConfig taskConfig : taskConfigs) {
            MesProEdhrBatchExecutionTaskDO task = toTaskDO(batch.getId(), taskConfig);
            batchTaskMapper.insert(task);
            createFormCenterInstanceForTask(batch, task);
            insertedTasks.add(task);
        }
        freezeBatchSharedExecutions(batch, insertedTasks);
        batchExecutionMapper.updateById(batch);
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(batch.getId());
        workTaskService.createInitialFillTask(latest);
        EdhrBatchExecutionRespVO result = toResp(latest);
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(latest.getId()), "OPEN",
                "创建并打开 eDHR 批次", latest.getId(), null, null, latest.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:create", "ALLOW",
                "SUCCESS", null, null, null);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO reexecuteRejectedBatch(EdhrBatchExecutionReexecuteReqVO reqVO) {
        if (reqVO.getSourceRejectedBatchExecutionId() == null || StrUtil.isBlank(reqVO.getReason())) {
            throw exception(BAD_REQUEST);
        }
        MesProEdhrBatchExecutionDO source = validateBatchExists(reqVO.getSourceRejectedBatchExecutionId());
        if (!Objects.equals(source.getStatus(), BATCH_STATUS_REJECTED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        requireBatchActionUnlocked(source.getId());
        MesProWorkOrderDO workOrder = validateSelectableWorkOrder(source.getWorkOrderId());
        MesProRouteDO route = routeMapper.selectById(source.getRouteId());
        if (route == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        if (!CommonStatusEnum.isEnable(route.getStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (activeRouteVersion == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_VERSION_REQUIRED, route.getId());
        }
        int attemptNo = nextAttemptNo(source);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String reason = StrUtil.trim(reqVO.getReason());
        Long actorId = currentUserId();
        MesProEdhrRecordChangeEventDO event = MesProEdhrRecordChangeEventDO.builder()
                .changeCode("EDHR-REEXECUTE-" + source.getId() + "-" + System.currentTimeMillis())
                .changeType(CHANGE_TYPE_REEXECUTE)
                .targetScope(TARGET_SCOPE_BATCH)
                .batchExecutionId(source.getId())
                .changeStatus(CHANGE_STATUS_EFFECTIVE)
                .reasonCategory("QUALITY_REJECT_REEXECUTE")
                .reasonText(reason)
                .requestedBy(actorId)
                .requestedAt(now)
                .approvedBy(actorId)
                .approvedAt(now)
                .effectiveAt(now)
                .previousStatus(String.valueOf(source.getStatus()))
                .newStatus(String.valueOf(BATCH_STATUS_CREATED))
                .previousHeadHash(source.getAggregateHash())
                .remark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getRemark()), null))
                .build();
        recordChangeEventMapper.insert(event);

        String activeContextKey = buildActiveContextKey(workOrder.getId(), source.getBatchCode(), route.getId(), attemptNo);
        MesProEdhrBatchExecutionDO newAttempt = new MesProEdhrBatchExecutionDO()
                .setBatchExecutionCode("EDHRB-" + System.currentTimeMillis())
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setBatchCode(source.getBatchCode())
                .setActiveContextKey(activeContextKey)
                .setAttemptNo(attemptNo)
                .setSourceRejectedBatchExecutionId(source.getId())
                .setReexecutedByChangeEventId(event.getId())
                .setProductId(workOrder.getProductId())
                .setProductCode(String.valueOf(workOrder.getProductId()))
                .setProductName(workOrder.getName())
                .setRouteId(route.getId())
                .setRouteVersionId(activeRouteVersion.getId())
                .setRouteVersionNo(activeRouteVersion.getVersionNo())
                .setRouteSnapshotJson(activeRouteVersion.getRouteSnapshotJson())
                .setRouteCode(route.getCode())
                .setRouteName(route.getName())
                .setStatus(BATCH_STATUS_CREATED)
                .setTaskApprovedCount(0)
                .setBlockedCount(0)
                .setRemark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getRemark()), null));
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId());
        List<BatchTaskConfig> taskConfigs = buildBatchTaskConfigs(newAttempt, route, routeProcesses);
        newAttempt.setTaskTotal(taskConfigs.size());
        batchExecutionMapper.insert(newAttempt);
        createDefaultDossierItems(newAttempt);

        List<MesProEdhrBatchExecutionTaskDO> insertedTasks = new ArrayList<>();
        for (BatchTaskConfig taskConfig : taskConfigs) {
            MesProEdhrBatchExecutionTaskDO task = toTaskDO(newAttempt.getId(), taskConfig);
            batchTaskMapper.insert(task);
            createFormCenterInstanceForTask(newAttempt, task);
            insertedTasks.add(task);
        }
        freezeBatchSharedExecutions(newAttempt, insertedTasks);
        batchExecutionMapper.updateById(newAttempt);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(source.getId())
                .setSupersededByBatchExecutionId(newAttempt.getId()));
        recordChangeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setNewExecutionId(newAttempt.getId())
                .setNewHeadHash(newAttempt.getAggregateHash()));

        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(newAttempt.getId());
        workTaskService.createInitialFillTask(latest);
        EdhrBatchExecutionRespVO result = toResp(latest);
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(latest.getId()), CHANGE_TYPE_REEXECUTE,
                "质量拒收后同生产批号新执行尝试", latest.getId(), null, null, latest.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:create", "ALLOW",
                "SUCCESS", null, null, JSON.toJSONString(Map.of(
                        "sourceRejectedBatchExecutionId", source.getId(),
                        "attemptNo", attemptNo,
                        "reason", reason)));
        return result;
    }

    private int nextAttemptNo(MesProEdhrBatchExecutionDO source) {
        int maxAttemptNo = batchExecutionMapper
                .selectListByWorkOrderIdAndBatchCode(source.getWorkOrderId(), source.getBatchCode()).stream()
                .filter(batch -> Objects.equals(batch.getRouteId(), source.getRouteId()))
                .map(MesProEdhrBatchExecutionDO::getAttemptNo)
                .map(attemptNo -> attemptNo == null ? 1 : attemptNo)
                .max(Integer::compareTo)
                .orElse(1);
        return Math.max(2, maxAttemptNo + 1);
    }

    private String buildActiveContextKey(Long workOrderId, String batchCode, Long routeId) {
        return workOrderId + "|" + routeId + "|" + StrUtil.trim(batchCode);
    }

    private String buildActiveContextKey(Long workOrderId, String batchCode, Long routeId, Integer attemptNo) {
        String baseKey = buildActiveContextKey(workOrderId, batchCode, routeId);
        if (attemptNo == null || attemptNo <= 1) {
            return baseKey;
        }
        return baseKey + "|attempt=" + attemptNo;
    }

    private EdhrBatchExecutionRespVO openExistingBatch(MesProEdhrBatchExecutionDO existing) {
        syncIfActive(existing);
        existing = batchExecutionMapper.selectById(existing.getId());
        EdhrBatchExecutionRespVO result = toResp(existing);
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(existing.getId()), "OPEN",
                "打开已有 eDHR 批次", existing.getId(), null, null, existing.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:create", "ALLOW",
                "SUCCESS", null, null, null);
        return result;
    }

    private MesProWorkOrderDO validateSelectableWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_NOT_EXISTS);
        }
        if (Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.CANCELED.getStatus())
                || Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID);
        }
        return workOrder;
    }

    private EdhrBatchExecutionRouteOptionRespVO toRouteOption(MesProRouteDO route) {
        return new EdhrBatchExecutionRouteOptionRespVO()
                .setRouteId(route.getId())
                .setRouteCode(route.getCode())
                .setRouteName(route.getName())
                .setBatchRouteEnabled(isBatchRouteEnabled(route.getId()));
    }

    private boolean isBatchRouteEnabled(Long routeId) {
        MesProRouteFlowConfigDO batchFlowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, "BATCH");
        return batchFlowConfig != null && Boolean.TRUE.equals(batchFlowConfig.getEnabled());
    }

    @Override
    public List<String> getScheduleCompletionMissingItems(EdhrScheduleCompletionCreateCommand command) {
        MesProWorkOrderDO workOrder = command == null || command.getWorkOrderId() == null
                ? null : workOrderMapper.selectById(command.getWorkOrderId());
        MesProRouteDO route = command == null || command.getRouteId() == null
                ? null : routeMapper.selectById(command.getRouteId());
        List<MesProRouteProcessDO> routeProcesses = route == null
                ? Collections.emptyList() : routeProcessMapper.selectListByRouteId(route.getId());
        return collectScheduleCompletionMissingItems(command, workOrder, route, routeProcesses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO openOrCreateFromScheduleCompletion(EdhrScheduleCompletionCreateCommand command) {
        List<String> missingItems = getScheduleCompletionMissingItems(command);
        if (!missingItems.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING,
                    String.join("、", missingItems));
        }

        MesProEdhrBatchExecutionDO existing = batchExecutionMapper.selectByContext(
                command.getWorkOrderId(), command.getBatchCode().trim(), command.getRouteId());
        if (existing != null) {
            validateScheduleCompletionExistingContext(existing, command);
            syncIfActive(existing);
            existing = batchExecutionMapper.selectById(existing.getId());
            return toResp(existing);
        }
        return openOrCreate(new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(command.getWorkOrderId())
                .setBatchCode(command.getBatchCode())
                .setRouteId(command.getRouteId())
                .setRemark(StrUtil.blankToDefault(command.getRemark(), "排产完成自动创建")));
    }

    private Long resolveRouteId(Long expectedRouteId, MesProWorkOrderDO workOrder) {
        List<MesProRouteDO> productRoutes = resolveEnabledProductRoutes(workOrder);
        Map<Long, MesProRouteDO> enabledRouteMap = productRoutes.stream()
                .collect(Collectors.toMap(MesProRouteDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        if (expectedRouteId != null) {
            MesProRouteDO expectedRoute = enabledRouteMap.get(expectedRouteId);
            if (expectedRoute == null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_MISMATCH,
                        formatRouteMismatch(productRoutes, routeMapper.selectById(expectedRouteId)));
            }
            return expectedRoute.getId();
        }
        if (productRoutes.size() > 1) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_DUPLICATE,
                    formatRouteCodes(productRoutes));
        }
        return productRoutes.get(0).getId();
    }

    private List<MesProRouteDO> resolveEnabledProductRoutes(MesProWorkOrderDO workOrder) {
        if (workOrder.getProductId() == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        List<Long> productRouteIds = routeProductMapper.selectListByItemId(workOrder.getProductId()).stream()
                .map(MesProRouteProductDO::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productRouteIds.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        List<MesProRouteDO> productRoutes = routeMapper.selectList(MesProRouteDO::getId, productRouteIds).stream()
                .filter(route -> CommonStatusEnum.isEnable(route.getStatus()))
                .filter(route -> route.getId() != null)
                .sorted(Comparator.comparing(MesProRouteDO::getId))
                .toList();
        if (productRoutes.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        return productRoutes;
    }

    private String formatRouteCodes(List<MesProRouteDO> routes) {
        return routes.stream()
                .map(route -> StrUtil.blankToDefault(route.getCode(), String.valueOf(route.getId())))
                .collect(Collectors.joining("、"));
    }

    private String formatRouteMismatch(List<MesProRouteDO> resolvedRoutes, MesProRouteDO expectedRoute) {
        String resolved = resolvedRoutes.isEmpty()
                ? "无"
                : resolvedRoutes.stream().map(this::routeDisplay).collect(Collectors.joining("、"));
        String expected = expectedRoute == null ? "无" : routeDisplay(expectedRoute);
        return "产品对应路线=" + resolved + "，请求路线=" + expected;
    }

    private String routeDisplay(MesProRouteDO route) {
        return StrUtil.blankToDefault(route.getCode(), String.valueOf(route.getId()));
    }

    private List<String> collectScheduleCompletionMissingItems(EdhrScheduleCompletionCreateCommand command,
                                                              MesProWorkOrderDO workOrder,
                                                              MesProRouteDO route,
                                                              List<MesProRouteProcessDO> routeProcesses) {
        List<String> missingItems = new ArrayList<>();
        if (command == null || command.getWorkOrderId() == null || workOrder == null) {
            missingItems.add("工单");
        }
        if (command == null || StrUtil.isBlank(command.getBatchCode())) {
            missingItems.add("批次号");
        }
        Long commandProductId = command == null ? null : command.getProductId();
        Long workOrderProductId = workOrder == null ? null : workOrder.getProductId();
        if (commandProductId == null && workOrderProductId == null) {
            missingItems.add("产品");
        }
        if (command == null || command.getRouteId() == null || route == null) {
            missingItems.add("工艺路线");
        }
        if (route != null && routeProcesses.isEmpty()) {
            missingItems.add("工序");
        }
        MesProRouteProcessDO firstRouteProcess = route == null || routeProcesses.isEmpty()
                ? null : resolveFirstBatchRecordRouteProcess(route.getId(), routeProcesses, missingItems);
        if (firstRouteProcess != null && !hasInitialFillAssignmentRule(firstRouteProcess.getId())) {
            missingItems.add("首任务责任来源/候选池");
        }
        return missingItems.stream().distinct().toList();
    }

    private MesProRouteProcessDO resolveFirstBatchRecordRouteProcess(Long routeId,
                                                                     List<MesProRouteProcessDO> routeProcesses,
                                                                     List<String> missingItems) {
        String batchUseType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        if (!hasBatchFlowConfigContext(routeId, batchUseType)) {
            missingItems.add("工艺流程批记录配置");
            missingItems.add("工序与批记录绑定");
            return null;
        }
        Map<Long, MesProRouteProcessDO> routeProcessMap = routeProcesses.stream()
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, item -> item, (left, right) -> left));
        List<MesProRouteFlowProcessConfigDO> enabledConfigs =
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(routeId, batchUseType).stream()
                        .toList();
        Map<Long, MesProRouteFlowProcessConfigDO> enabledConfigMap = enabledConfigs.stream()
                .filter(config -> config.getId() != null)
                .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, Long> currentRouteProcessIdBySnapshot =
                resolveCurrentRouteProcessIdBySnapshot(routeId, routeProcessMap, enabledConfigs);
        List<MesProRouteFlowProcessBatchRecordDO> batchRecords =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                                currentRouteProcessIdBySnapshot.keySet(), batchUseType).stream()
                        .filter(record -> isOwnedByEnabledProcessConfig(record, enabledConfigMap, batchUseType))
                        .toList();
        Set<Long> boundRouteProcessIds = batchRecords.stream()
                .filter(record -> StrUtil.isNotBlank(record.getBatchRecordReportId()))
                .filter(record -> record.getReportSort() != null && record.getReportSort() > 0)
                .map(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .map(currentRouteProcessIdBySnapshot::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (enabledConfigs.isEmpty() || batchRecords.isEmpty() || boundRouteProcessIds.isEmpty()) {
            missingItems.add("工序与批记录绑定");
            return null;
        }
        return routeProcessMap.values().stream()
                .filter(process -> boundRouteProcessIds.contains(process.getId()))
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
    }

    private boolean hasInitialFillAssignmentRule(Long routeProcessId) {
        MesProEdhrWorkTaskAssignmentRuleDO rule =
                workTaskAssignmentRuleMapper.selectEnabledByRouteProcessAndType(routeProcessId, WORK_TASK_TYPE_FILL);
        if (rule == null) {
            return false;
        }
        String sourceType = StrUtil.blankToDefault(rule.getCandidateSourceType(), "USER");
        if ("USER".equals(sourceType)) {
            return rule.getCandidateSourceId() != null || rule.getAssigneeUserId() != null;
        }
        return rule.getCandidateSourceId() != null;
    }

    private void validateScheduleCompletionExistingContext(MesProEdhrBatchExecutionDO existing,
                                                           EdhrScheduleCompletionCreateCommand command) {
        List<String> conflicts = new ArrayList<>();
        if (command.getProductId() != null && existing.getProductId() != null
                && !Objects.equals(command.getProductId(), existing.getProductId())) {
            conflicts.add("产品");
        }
        if (!conflicts.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING,
                    "已存在批次上下文冲突：" + String.join("、", conflicts));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO skipSpecialNode(Long taskId, String reason, String password,
                                                    List<MesProEdhrSpecialNodeAttachment> attachments) {
        MesProEdhrBatchExecutionTaskDO task = validateTaskForSpecialAction(taskId);
        boolean specialNodeSkip = SKIPPABLE_SPECIAL_NODE_TYPES.contains(resolveNodeType(task));
        boolean optionalRouteFormSkip = isOptionalRouteFormTask(task);
        if (!specialNodeSkip && !optionalRouteFormSkip) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        if (StrUtil.isBlank(reason) || StrUtil.isBlank(password)) {
            throw exception(BAD_REQUEST, specialNodeSkip
                    ? "特殊节点跳过必须填写原因和签名密码"
                    : "可选表单跳过必须填写原因和签名密码");
        }
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(task.getBatchExecutionId());
        Long actorId = currentUserId();
        MesProEdhrWorkTaskDO optionalWorkTask = null;
        if (specialNodeSkip) {
            validateCurrentUserIsBatchOwner(batch);
        } else {
            optionalWorkTask = validateOptionalRouteFormSkipWorkTask(task, actorId);
        }
        String signatureAction = specialNodeSkip ? "SPECIAL_NODE_SKIP" : "ROUTE_FORM_OPTIONAL_SKIP";
        String actionName = specialNodeSkip ? "跳过 eDHR 特殊工序" : "跳过 eDHR 可选路线表单";
        validateBatchSignaturePassword(batch, actorId, password, signatureAction,
                actionName, "mes:pro-edhr-batch-execution:update");
        LocalDateTime operatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        List<MesProBatchRecordExecutionAttachmentDO> persistedAttachments =
                persistSpecialNodeAttachments(task, attachments, operatedAt);
        String skipReason = StrUtil.trim(reason);
        String aggregateHash = DigestUtil.sha256Hex(task.getBatchExecutionId() + ":" + task.getId() + ":"
                + skipReason + ":" + password);
        MesProEdhrBatchExecutionSignatureDO signature = new MesProEdhrBatchExecutionSignatureDO()
                .setBatchExecutionId(task.getBatchExecutionId())
                .setActorId(actorId)
                .setActorName(String.valueOf(actorId))
                .setActionType(signatureAction)
                .setSignatureMode("PASSWORD")
                .setPasswordVerified(true)
                .setComment(skipReason)
                .setSignedAt(operatedAt)
                .setSignatureChallengeHash(DigestUtil.sha256Hex(task.getBatchExecutionId() + ":" + password))
                .setAggregateHash(aggregateHash);
        batchSignatureMapper.insert(signature);
        JSONObject payload = new JSONObject();
        payload.put("skippedBy", actorId);
        payload.put("skippedAt", operatedAt.toString());
        payload.put("skipReason", skipReason);
        payload.put("skipSignatureId", signature.getId());
        payload.put("skipSignatureHash", signature.getAggregateHash());
        payload.put("skipTaskType", specialNodeSkip ? "SPECIAL_NODE" : "OPTIONAL_ROUTE_FORM");
        if (optionalWorkTask != null) {
            payload.put("workTaskId", optionalWorkTask.getId());
        }
        putSpecialNodePayloadAttachments(payload, task, persistedAttachments);
        task.setStatus(TASK_STATUS_SKIPPED)
                .setSkippedBy(actorId)
                .setSkippedAt(operatedAt)
                .setSpecialPayloadJson(payload.toJSONString());
        batchTaskMapper.updateById(task);
        if (optionalWorkTask != null) {
            workTaskService.completeOptionalFillTaskBySkip(optionalWorkTask.getId(), skipReason);
        }
        if (specialNodeSkip) {
            workTaskService.createNextFillAfterSpecialNodeResolved(task);
        }
        syncBatchStatus(batch);
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(task.getBatchExecutionId());
        recordOperationAudit("BATCH_EXECUTION_TASK", String.valueOf(task.getId()), "SKIP",
                actionName, task.getBatchExecutionId(), task.getExecutionId(), task.getId(),
                batch.getRouteId(), task.getRouteProcessId(), task.getBatchRecordReportId(), task.getRecordCategory(),
                "mes:pro-edhr-batch-execution:update", "ALLOW", "SUCCESS", null, null,
                payload.toJSONString());
        return toResp(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO completeSpecialNode(Long taskId, String sterilizationBatchNo) {
        return completeSpecialNode(taskId, sterilizationBatchNo, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO completeSpecialNode(Long taskId, String sterilizationBatchNo,
                                                        List<MesProEdhrSpecialNodeAttachment> attachments) {
        // 灭菌报告选择填写完成时，灭菌批次必填。
        MesProEdhrBatchExecutionTaskDO task = validateTaskForSpecialAction(taskId);
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(task.getBatchExecutionId());
        validateCurrentUserIsBatchOwner(batch);
        String nodeType = resolveNodeType(task);
        if (!SKIPPABLE_SPECIAL_NODE_TYPES.contains(nodeType)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        if (NODE_TYPE_STERILIZATION_REPORT.equals(nodeType) && StrUtil.isBlank(sterilizationBatchNo)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STERILIZATION_BATCH_REQUIRED);
        }
        LocalDateTime operatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        List<MesProBatchRecordExecutionAttachmentDO> persistedAttachments =
                persistSpecialNodeAttachments(task, attachments, operatedAt);
        JSONObject payload = new JSONObject();
        payload.put("completedBy", currentUserId());
        payload.put("completedAt", operatedAt.toString());
        if (NODE_TYPE_STERILIZATION_REPORT.equals(nodeType)) {
            payload.put("sterilizationBatchNo", sterilizationBatchNo.trim());
        }
        putSpecialNodePayloadAttachments(payload, task, persistedAttachments);
        task.setStatus(TASK_STATUS_APPROVED)
                .setApprovedAt(operatedAt)
                .setSpecialPayloadJson(payload.isEmpty() ? null : payload.toJSONString());
        batchTaskMapper.updateById(task);
        workTaskService.createNextFillAfterSpecialNodeResolved(task);
        syncBatchStatus(batch);
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(task.getBatchExecutionId());
        recordOperationAudit("BATCH_EXECUTION_TASK", String.valueOf(task.getId()), "COMPLETE",
                "完成 eDHR 特殊工序", task.getBatchExecutionId(), task.getExecutionId(), task.getId(),
                batch.getRouteId(), task.getRouteProcessId(), task.getBatchRecordReportId(), task.getRecordCategory(),
                "mes:pro-edhr-batch-execution:update", "ALLOW", "SUCCESS", null, null,
                payload.toJSONString());
        return toResp(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrSpecialNodeAttachmentPrepareUploadResult prepareSpecialNodeAttachmentUpload(
            MesProEdhrSpecialNodeAttachmentPrepareUploadCommand command) {
        requireSpecialNodeAttachmentPrepareUploadCommand(command);
        MesProEdhrBatchExecutionTaskDO task = validateTaskForSpecialAttachmentUpload(command.getTaskId());
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(task.getBatchExecutionId());
        validateCurrentUserIsBatchOwner(batch);
        if (!SKIPPABLE_SPECIAL_NODE_TYPES.contains(resolveNodeType(task))) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        String directory = "edhr/special-nodes/" + task.getBatchExecutionId() + "/" + task.getId() + "/attachments";
        String sha256 = DigestUtil.sha256Hex(command.getContent());
        Long fileId = fileService.createFileAndReturnId(command.getContent(), StrUtil.trim(command.getFileName()),
                directory, StrUtil.trim(command.getContentType()));
        FileDO file = fileService.getFile(fileId);
        requireSpecialNodeFileMetadata(file);
        String storageRetentionJson = "{\"fileId\":" + file.getId()
                + ",\"storageConfigId\":" + file.getConfigId()
                + ",\"storagePath\":\"" + file.getPath()
                + "\",\"sha256\":\"" + sha256 + "\"}";
        String storageRetentionHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                SPECIAL_NODE_ATTACHMENT_RETENTION_PREFIX
                        + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(storageRetentionJson));
        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result =
                MesProEdhrSpecialNodeAttachmentPrepareUploadResult.builder()
                        .uploadToken(SPECIAL_NODE_ATTACHMENT_UPLOAD_PREFIX + ":" + task.getId() + ":" + file.getId() + ":" + sha256)
                        .fileId(file.getId())
                        .fileUrl(file.getUrl())
                        .storageConfigId(file.getConfigId())
                        .storagePath(file.getPath())
                        .fileName(file.getName())
                        .contentType(file.getType())
                        .fileSize(file.getSize())
                        .sha256(sha256)
                        .storageRetentionJson(storageRetentionJson)
                        .storageRetentionHash(storageRetentionHash)
                        .build();
        MesProBatchRecordExecutionAttachmentDO pendingAttachment =
                upsertPendingSpecialNodeAttachment(task, toSpecialNodeAttachment(result),
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        recordAttachmentPrepareUploadAudit(task, pendingAttachment, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePendingSpecialNodeAttachment(Long taskId, MesProEdhrSpecialNodeAttachment attachment,
                                                   String reason) {
        MesProEdhrBatchExecutionTaskDO task = validateTaskForSpecialAttachmentUpload(taskId);
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(task.getBatchExecutionId());
        validateCurrentUserIsBatchOwner(batch);
        validateSpecialNodeAttachment(task, attachment);
        String auditReason = requireSpecialNodeAttachmentAuditReason(reason);
        List<MesProBatchRecordExecutionAttachmentDO> deletedAttachments =
                deletePendingSpecialNodeAttachment(task, attachment, true);
        for (MesProBatchRecordExecutionAttachmentDO deletedAttachment : deletedAttachments) {
            recordPendingAttachmentDeleteAudit(task, deletedAttachment, auditReason);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO savePendingSpecialNodeAttachments(Long batchExecutionId, String reason) {
        MesProEdhrBatchExecutionDO batch = validateBatchForSpecialAttachmentSave(batchExecutionId);
        validateCurrentUserIsBatchOwner(batch);
        String auditReason = requireSpecialNodeAttachmentAuditReason(reason);
        LocalDateTime operatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        List<MesProBatchRecordExecutionAttachmentDO> allPendingAttachments = new ArrayList<>();
        List<MesProBatchRecordExecutionAttachmentDO> allPersistedAttachments = new ArrayList<>();
        List<Map<String, Object>> taskPayloads = new ArrayList<>();
        for (MesProEdhrBatchExecutionTaskDO task : batchTaskMapper.selectListByBatchExecutionId(batch.getId())) {
            if (!SKIPPABLE_SPECIAL_NODE_TYPES.contains(resolveNodeType(task))) {
                continue;
            }
            List<MesProBatchRecordExecutionAttachmentDO> pendingAttachments =
                    resolvePendingSpecialNodeAttachmentRecords(task.getId());
            if (pendingAttachments.isEmpty()) {
                continue;
            }
            String beforePayloadJson = StrUtil.nullToEmpty(task.getSpecialPayloadJson());
            List<MesProBatchRecordExecutionAttachmentDO> persistedAttachments = persistSpecialNodeAttachments(
                    task,
                    pendingAttachments.stream()
                            .map(attachment -> toSpecialNodeAttachment(task, attachment))
                            .toList(),
                    operatedAt);
            if (persistedAttachments.isEmpty()) {
                continue;
            }
            JSONObject payload = parseSpecialNodePayloadObject(task);
            putSpecialNodePayloadAttachments(payload, task, persistedAttachments);
            task.setSpecialPayloadJson(payload.toJSONString());
            batchTaskMapper.updateById(task);
            allPendingAttachments.addAll(pendingAttachments);
            allPersistedAttachments.addAll(persistedAttachments);
            Map<String, Object> taskPayload = new LinkedHashMap<>();
            taskPayload.put("batchTaskId", task.getId());
            taskPayload.put("nodeType", resolveNodeType(task));
            taskPayload.put("beforePayloadHash", hashAuditPayload(beforePayloadJson));
            taskPayload.put("afterPayloadHash", hashAuditPayload(task.getSpecialPayloadJson()));
            taskPayload.put("pendingAttachments", pendingAttachments.stream()
                    .map(this::toAttachmentAuditPayload)
                    .toList());
            taskPayload.put("persistedAttachments", persistedAttachments.stream()
                    .map(this::toAttachmentAuditPayload)
                    .toList());
            taskPayloads.add(taskPayload);
        }
        if (!allPersistedAttachments.isEmpty()) {
            recordAttachmentSavePendingAudit(batch, auditReason, allPendingAttachments, allPersistedAttachments, taskPayloads);
        }
        return get(batch.getId());
    }

    private void requireSpecialNodeAttachmentPrepareUploadCommand(
            MesProEdhrSpecialNodeAttachmentPrepareUploadCommand command) {
        if (command == null
                || command.getTaskId() == null
                || StrUtil.isBlank(command.getFileName())
                || StrUtil.isBlank(command.getContentType())
                || command.getContent() == null
                || command.getContent().length == 0) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
    }

    private List<MesProBatchRecordExecutionAttachmentDO> persistSpecialNodeAttachments(
            MesProEdhrBatchExecutionTaskDO task,
            List<MesProEdhrSpecialNodeAttachment> attachments,
            LocalDateTime operatedAt) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<MesProBatchRecordExecutionAttachmentDO> persisted = new ArrayList<>();
        for (MesProEdhrSpecialNodeAttachment attachment : attachments) {
            deletePendingSpecialNodeAttachment(task, attachment, false);
            deletePendingSpecialNodeAttachmentsByFileName(task.getId(), attachment.getFileName());
            persisted.add(insertSpecialNodeAttachment(task, attachment, operatedAt));
        }
        return persisted;
    }

    private MesProBatchRecordExecutionAttachmentDO insertSpecialNodeAttachment(
            MesProEdhrBatchExecutionTaskDO task,
            MesProEdhrSpecialNodeAttachment attachment,
            LocalDateTime operatedAt) {
        return insertSpecialNodeAttachment(task, attachment, operatedAt,
                SPECIAL_NODE_ATTACHMENT_ACTION_ADD, "SPECIAL_NODE:" + attachment.getFileId());
    }

    private MesProBatchRecordExecutionAttachmentDO insertSpecialNodeAttachment(
            MesProEdhrBatchExecutionTaskDO task,
            MesProEdhrSpecialNodeAttachment attachment,
            LocalDateTime operatedAt,
            String attachmentAction,
            String groupKey) {
        validateSpecialNodeAttachment(task, attachment);
        String nodeType = resolveNodeType(task);
        String fieldPath = "specialNode." + nodeType;
        MesProBatchRecordExecutionAttachmentDO latest = attachmentMapper.selectLatestByExecutionFieldGroup(
                SPECIAL_NODE_ATTACHMENT_EXECUTION_ID, fieldPath, nodeType, groupKey);
        String previousHash = latest == null ? MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH
                : latest.getAttachmentHash();
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        MesProBatchRecordExecutionAttachmentDO record = MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(SPECIAL_NODE_ATTACHMENT_EXECUTION_ID)
                .batchExecutionId(task.getBatchExecutionId())
                .batchTaskId(task.getId())
                .workTaskId(null)
                .rowIndex(0)
                .columnIndex(0)
                .fieldKey(nodeType)
                .fieldPath(fieldPath)
                .fieldLabel(StrUtil.blankToDefault(task.getProcessName(), nodeType))
                .attachmentType(resolveSpecialNodeAttachmentType(attachment.getContentType()))
                .attachmentGroupKey(groupKey)
                .attachmentAction(attachmentAction)
                .versionNo(versionNo)
                .fileId(attachment.getFileId())
                .fileUrl(StrUtil.trim(attachment.getFileUrl()))
                .storageConfigId(attachment.getStorageConfigId())
                .storagePath(StrUtil.trim(attachment.getStoragePath()))
                .fileName(StrUtil.trim(attachment.getFileName()))
                .contentType(StrUtil.trim(attachment.getContentType()))
                .fileSize(attachment.getFileSize())
                .sha256(StrUtil.trim(attachment.getSha256()))
                .storageRetentionJson(StrUtil.trim(attachment.getStorageRetentionJson()))
                .storageRetentionHash(StrUtil.trim(attachment.getStorageRetentionHash()))
                .previousAttachmentHash(previousHash)
                .operatorId(currentUserId())
                .operatorName(String.valueOf(currentUserId()))
                .operatedAt(operatedAt)
                .reasonCategory(SPECIAL_NODE_ATTACHMENT_REASON_CATEGORY)
                .reasonText(StrUtil.blankToDefault(task.getProcessName(), nodeType) + "特殊节点附件")
                .build();
        record.setAttachmentHash(hashSpecialNodeAttachment(record));
        attachmentMapper.insert(record);
        return record;
    }

    private MesProBatchRecordExecutionAttachmentDO upsertPendingSpecialNodeAttachment(
            MesProEdhrBatchExecutionTaskDO task,
            MesProEdhrSpecialNodeAttachment attachment,
            LocalDateTime operatedAt) {
        List<MesProBatchRecordExecutionAttachmentDO> replacedAttachments =
                deletePendingSpecialNodeAttachmentsByFileName(task.getId(), attachment.getFileName());
        for (MesProBatchRecordExecutionAttachmentDO replacedAttachment : replacedAttachments) {
            recordPendingAttachmentDeleteAudit(task, replacedAttachment, "同名待提交特殊节点附件被新预登记替换");
        }
        return insertSpecialNodeAttachment(task, attachment, operatedAt,
                SPECIAL_NODE_ATTACHMENT_ACTION_PENDING, "SPECIAL_NODE_PENDING:" + attachment.getFileId());
    }

    private List<MesProBatchRecordExecutionAttachmentDO> deletePendingSpecialNodeAttachment(
            MesProEdhrBatchExecutionTaskDO task,
            MesProEdhrSpecialNodeAttachment attachment,
            boolean failIfMissing) {
        List<MesProBatchRecordExecutionAttachmentDO> matchedAttachments =
                attachmentMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProBatchRecordExecutionAttachmentDO>()
                .eq(MesProBatchRecordExecutionAttachmentDO::getBatchTaskId, task.getId())
                .eq(MesProBatchRecordExecutionAttachmentDO::getExecutionId, SPECIAL_NODE_ATTACHMENT_EXECUTION_ID)
                .eq(MesProBatchRecordExecutionAttachmentDO::getAttachmentAction, SPECIAL_NODE_ATTACHMENT_ACTION_PENDING)
                .eq(MesProBatchRecordExecutionAttachmentDO::getFileId, attachment.getFileId())
                .eq(MesProBatchRecordExecutionAttachmentDO::getSha256, StrUtil.trim(attachment.getSha256())));
        for (MesProBatchRecordExecutionAttachmentDO matchedAttachment : matchedAttachments) {
            attachmentMapper.deleteById(matchedAttachment.getId());
        }
        if (failIfMissing && matchedAttachments.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        return matchedAttachments;
    }

    private List<MesProBatchRecordExecutionAttachmentDO> deletePendingSpecialNodeAttachmentsByFileName(Long taskId,
                                                                                                      String fileName) {
        String normalizedFileName = normalizeSpecialNodeAttachmentFileName(fileName);
        if (StrUtil.isBlank(normalizedFileName)) {
            return List.of();
        }
        List<MesProBatchRecordExecutionAttachmentDO> matchedAttachments = resolvePendingSpecialNodeAttachmentRecords(taskId).stream()
                .filter(attachment -> Objects.equals(normalizedFileName,
                        normalizeSpecialNodeAttachmentFileName(attachment.getFileName())))
                .toList();
        matchedAttachments.forEach(attachment -> attachmentMapper.deleteById(attachment.getId()));
        return matchedAttachments;
    }

    private List<MesProBatchRecordExecutionAttachmentDO> resolvePendingSpecialNodeAttachmentRecords(Long taskId) {
        Map<String, MesProBatchRecordExecutionAttachmentDO> latestByFileName = new LinkedHashMap<>();
        attachmentMapper.selectListByBatchTaskId(taskId).stream()
                .filter(attachment -> Objects.equals(attachment.getExecutionId(), SPECIAL_NODE_ATTACHMENT_EXECUTION_ID))
                .filter(attachment -> Objects.equals(attachment.getAttachmentAction(), SPECIAL_NODE_ATTACHMENT_ACTION_PENDING))
                .sorted(Comparator
                        .comparing(MesProBatchRecordExecutionAttachmentDO::getOperatedAt,
                                Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(MesProBatchRecordExecutionAttachmentDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .forEach(attachment -> latestByFileName.put(
                        normalizeSpecialNodeAttachmentFileName(attachment.getFileName()), attachment));
        return latestByFileName.values().stream().toList();
    }

    private List<EdhrBatchExecutionSpecialNodeAttachmentVO> resolvePendingSpecialNodeAttachments(
            MesProEdhrBatchExecutionTaskDO task) {
        if (!SKIPPABLE_SPECIAL_NODE_TYPES.contains(resolveNodeType(task))) {
            return List.of();
        }
        return resolvePendingSpecialNodeAttachmentRecords(task.getId()).stream()
                .map(attachment -> new EdhrBatchExecutionSpecialNodeAttachmentVO()
                        .setUploadToken(SPECIAL_NODE_ATTACHMENT_UPLOAD_PREFIX + ":" + task.getId()
                                + ":" + attachment.getFileId() + ":" + attachment.getSha256())
                        .setFileId(attachment.getFileId())
                        .setFileUrl(attachment.getFileUrl())
                        .setStorageConfigId(attachment.getStorageConfigId())
                        .setStoragePath(attachment.getStoragePath())
                        .setFileName(attachment.getFileName())
                        .setContentType(attachment.getContentType())
                        .setFileSize(attachment.getFileSize())
                        .setSha256(attachment.getSha256())
                        .setStorageRetentionJson(attachment.getStorageRetentionJson())
                        .setStorageRetentionHash(attachment.getStorageRetentionHash()))
                .toList();
    }

    private MesProEdhrSpecialNodeAttachment toSpecialNodeAttachment(
            MesProEdhrSpecialNodeAttachmentPrepareUploadResult result) {
        return new MesProEdhrSpecialNodeAttachment()
                .setUploadToken(result.getUploadToken())
                .setFileId(result.getFileId())
                .setFileUrl(result.getFileUrl())
                .setStorageConfigId(result.getStorageConfigId())
                .setStoragePath(result.getStoragePath())
                .setFileName(result.getFileName())
                .setContentType(result.getContentType())
                .setFileSize(result.getFileSize())
                .setSha256(result.getSha256())
                .setStorageRetentionJson(result.getStorageRetentionJson())
                .setStorageRetentionHash(result.getStorageRetentionHash());
    }

    private MesProEdhrSpecialNodeAttachment toSpecialNodeAttachment(
            MesProEdhrBatchExecutionTaskDO task,
            MesProBatchRecordExecutionAttachmentDO attachment) {
        return new MesProEdhrSpecialNodeAttachment()
                .setUploadToken(SPECIAL_NODE_ATTACHMENT_UPLOAD_PREFIX + ":" + task.getId()
                        + ":" + attachment.getFileId() + ":" + attachment.getSha256())
                .setFileId(attachment.getFileId())
                .setFileUrl(attachment.getFileUrl())
                .setStorageConfigId(attachment.getStorageConfigId())
                .setStoragePath(attachment.getStoragePath())
                .setFileName(attachment.getFileName())
                .setContentType(attachment.getContentType())
                .setFileSize(attachment.getFileSize())
                .setSha256(attachment.getSha256())
                .setStorageRetentionJson(attachment.getStorageRetentionJson())
                .setStorageRetentionHash(attachment.getStorageRetentionHash());
    }

    private String normalizeSpecialNodeAttachmentFileName(String fileName) {
        return StrUtil.blankToDefault(fileName, "").trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBookedSpecialNodeAttachment(MesProBatchRecordExecutionAttachmentDO attachment) {
        return Objects.equals(attachment.getExecutionId(), SPECIAL_NODE_ATTACHMENT_EXECUTION_ID)
                && Objects.equals(attachment.getAttachmentAction(), SPECIAL_NODE_ATTACHMENT_ACTION_ADD);
    }

    private void validateSpecialNodeAttachment(MesProEdhrBatchExecutionTaskDO task,
                                               MesProEdhrSpecialNodeAttachment attachment) {
        if (attachment == null
                || attachment.getFileId() == null
                || attachment.getStorageConfigId() == null
                || attachment.getFileSize() == null
                || attachment.getFileSize() <= 0
                || StrUtil.isBlank(attachment.getUploadToken())
                || StrUtil.isBlank(attachment.getFileUrl())
                || StrUtil.isBlank(attachment.getStoragePath())
                || StrUtil.isBlank(attachment.getFileName())
                || StrUtil.isBlank(attachment.getContentType())
                || StrUtil.isBlank(attachment.getSha256())
                || !SHA256_PATTERN.matcher(StrUtil.trim(attachment.getSha256())).matches()
                || StrUtil.isBlank(attachment.getStorageRetentionJson())
                || StrUtil.isBlank(attachment.getStorageRetentionHash())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        String sha256 = StrUtil.trim(attachment.getSha256());
        String expectedToken = SPECIAL_NODE_ATTACHMENT_UPLOAD_PREFIX + ":" + task.getId()
                + ":" + attachment.getFileId() + ":" + sha256;
        if (!Objects.equals(expectedToken, StrUtil.trim(attachment.getUploadToken()))) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        String expectedRetentionHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                SPECIAL_NODE_ATTACHMENT_RETENTION_PREFIX
                        + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(
                        StrUtil.trim(attachment.getStorageRetentionJson())));
        if (!Objects.equals(expectedRetentionHash, StrUtil.trim(attachment.getStorageRetentionHash()))) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        FileDO file = fileService.getFile(attachment.getFileId());
        requireSpecialNodeFileMetadata(file);
        if (!Objects.equals(file.getConfigId(), attachment.getStorageConfigId())
                || !Objects.equals(file.getPath(), StrUtil.trim(attachment.getStoragePath()))
                || !Objects.equals(file.getUrl(), StrUtil.trim(attachment.getFileUrl()))
                || !Objects.equals(file.getName(), StrUtil.trim(attachment.getFileName()))
                || !Objects.equals(file.getType(), StrUtil.trim(attachment.getContentType()))
                || !Objects.equals(file.getSize(), attachment.getFileSize())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
    }

    private void requireSpecialNodeFileMetadata(FileDO file) {
        if (file == null
                || file.getId() == null
                || file.getConfigId() == null
                || StrUtil.isBlank(file.getPath())
                || StrUtil.isBlank(file.getUrl())
                || StrUtil.isBlank(file.getName())
                || StrUtil.isBlank(file.getType())
                || file.getSize() == null
                || file.getSize() <= 0) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
    }

    private String resolveSpecialNodeAttachmentType(String contentType) {
        return StrUtil.startWithIgnoreCase(StrUtil.trim(contentType), "image/") ? "IMAGE" : "FILE";
    }

    private void putSpecialNodePayloadAttachments(JSONObject payload, MesProEdhrBatchExecutionTaskDO task,
                                                   List<MesProBatchRecordExecutionAttachmentDO> persistedAttachments) {
        JSONArray attachments = resolveSpecialNodePayloadAttachmentArray(task);
        persistedAttachments.stream()
                .map(this::toSpecialNodeAttachmentPayload)
                .forEach(attachments::add);
        if (!attachments.isEmpty()) {
            payload.put("attachments", attachments);
        }
    }

    private JSONArray resolveSpecialNodePayloadAttachmentArray(MesProEdhrBatchExecutionTaskDO task) {
        JSONArray attachments = new JSONArray();
        JSONArray existingAttachments = parseSpecialNodePayloadObject(task).getJSONArray("attachments");
        if (existingAttachments != null) {
            attachments.addAll(existingAttachments);
        }
        return attachments;
    }

    private JSONObject parseSpecialNodePayloadObject(MesProEdhrBatchExecutionTaskDO task) {
        if (task == null || StrUtil.isBlank(task.getSpecialPayloadJson())) {
            return new JSONObject();
        }
        JSONObject payload = JSON.parseObject(task.getSpecialPayloadJson());
        return payload == null ? new JSONObject() : payload;
    }

    private Map<String, Object> toSpecialNodeAttachmentPayload(MesProBatchRecordExecutionAttachmentDO attachment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileId", attachment.getFileId());
        payload.put("fileUrl", attachment.getFileUrl());
        payload.put("storageConfigId", attachment.getStorageConfigId());
        payload.put("storagePath", attachment.getStoragePath());
        payload.put("fileName", attachment.getFileName());
        payload.put("contentType", attachment.getContentType());
        payload.put("fileSize", attachment.getFileSize());
        payload.put("sha256", attachment.getSha256());
        payload.put("storageRetentionHash", attachment.getStorageRetentionHash());
        payload.put("attachmentHash", attachment.getAttachmentHash());
        payload.put("operatedAt", attachment.getOperatedAt());
        return payload;
    }

    private String hashSpecialNodeAttachment(MesProBatchRecordExecutionAttachmentDO attachment) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("executionId", attachment.getExecutionId());
        root.put("batchExecutionId", attachment.getBatchExecutionId());
        root.put("batchTaskId", attachment.getBatchTaskId());
        root.put("workTaskId", attachment.getWorkTaskId());
        root.put("rowIndex", attachment.getRowIndex());
        root.put("columnIndex", attachment.getColumnIndex());
        root.put("fieldKey", attachment.getFieldKey());
        root.put("fieldPath", attachment.getFieldPath());
        root.put("attachmentType", attachment.getAttachmentType());
        root.put("attachmentGroupKey", attachment.getAttachmentGroupKey());
        root.put("attachmentAction", attachment.getAttachmentAction());
        root.put("versionNo", attachment.getVersionNo());
        root.put("fileId", attachment.getFileId());
        root.put("fileUrl", attachment.getFileUrl());
        root.put("storageConfigId", attachment.getStorageConfigId());
        root.put("storagePath", attachment.getStoragePath());
        root.put("fileName", attachment.getFileName());
        root.put("contentType", attachment.getContentType());
        root.put("fileSize", attachment.getFileSize());
        root.put("sha256", attachment.getSha256());
        root.put("storageRetentionHash", attachment.getStorageRetentionHash());
        root.put("previousAttachmentHash", attachment.getPreviousAttachmentHash());
        root.put("operatorId", attachment.getOperatorId());
        root.put("operatorName", attachment.getOperatorName());
        root.put("operatedAt", attachment.getOperatedAt().toString());
        root.put("reasonCategory", attachment.getReasonCategory());
        root.put("reasonText", attachment.getReasonText());
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(
                SPECIAL_NODE_ATTACHMENT_LEDGER_PREFIX
                        + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(JSON.toJSONString(root)));
    }

    private void validateCurrentUserIsBatchOwner(MesProEdhrBatchExecutionDO batch) {
        Long currentUserId = currentUserId();
        MesProEdhrWorkTaskAssignmentRuleDO closeRule = batch.getRouteId() == null ? null
                : workTaskAssignmentRuleMapper.selectEnabledByScopeAndType(RULE_SCOPE_TYPE_ROUTE,
                batch.getRouteId(), WORK_TASK_TYPE_CLOSE);
        if (!isCurrentUserCloseOwner(closeRule, currentUserId)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID, currentUserId);
        }
    }

    private boolean isCurrentUserCloseOwner(MesProEdhrWorkTaskAssignmentRuleDO closeRule, Long currentUserId) {
        if (closeRule == null || currentUserId == null) {
            return false;
        }
        if (Objects.equals(closeRule.getAssigneeUserId(), currentUserId)) {
            return true;
        }
        String sourceType = StrUtil.blankToDefault(closeRule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
        return Objects.equals(sourceType, CANDIDATE_SOURCE_TYPE_USER)
                && Objects.equals(closeRule.getCandidateSourceId(), currentUserId);
    }

    private MesProEdhrBatchExecutionTaskDO validateTaskForSpecialAction(Long taskId) {
        MesProEdhrBatchExecutionTaskDO task = validateSpecialNodeTaskBeforeRelease(taskId);
        if (Objects.equals(task.getStatus(), TASK_STATUS_APPROVED)
                || Objects.equals(task.getStatus(), TASK_STATUS_SKIPPED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        return task;
    }

    private MesProEdhrBatchExecutionTaskDO validateTaskForSpecialAttachmentUpload(Long taskId) {
        MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectByIdForUpdate(taskId);
        if (task == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS);
        }
        validateBatchForSpecialAttachmentSave(task.getBatchExecutionId());
        if (Objects.equals(task.getStatus(), TASK_STATUS_BLOCKED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        return task;
    }

    private MesProEdhrBatchExecutionDO validateBatchForSpecialAttachmentSave(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(batchExecutionId);
        if (Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrReleaseTransactionDO releaseTransaction =
                releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        if (releaseTransaction != null
                && MesProEdhrReleaseServiceImpl.STATUS_RELEASED.equals(releaseTransaction.getReleaseStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        return batch;
    }

    private MesProEdhrBatchExecutionTaskDO validateSpecialNodeTaskBeforeRelease(Long taskId) {
        MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectByIdForUpdate(taskId);
        if (task == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionDO batch = validateBatchExists(task.getBatchExecutionId());
        if (Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        requireBatchActionUnlocked(batch.getId());
        return task;
    }

    private MesProEdhrWorkTaskDO validateOptionalRouteFormSkipWorkTask(MesProEdhrBatchExecutionTaskDO task,
                                                                       Long actorId) {
        if (!isOptionalRouteFormTask(task)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectActiveByBatchTaskAndType(task.getId(), WORK_TASK_TYPE_FILL);
        if (workTask == null) {
            workTask = workTaskMapper.selectActiveByBatchTaskAndType(task.getId(), WORK_TASK_TYPE_REWORK);
        }
        if (!isAssignedOrCandidate(workTask, actorId)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE);
        }
        return workTask;
    }

    private MesProEdhrBatchExecutionTaskDO toTaskDO(Long batchExecutionId, BatchTaskConfig taskConfig) {
        if (taskConfig.specialNodeType() != null) {
            return new MesProEdhrBatchExecutionTaskDO()
                    .setBatchExecutionId(batchExecutionId)
                    .setNodeType(taskConfig.specialNodeType())
                    .setRouteProcessSort(taskConfig.specialSort())
                    .setProcessCode(taskConfig.specialNodeType())
                    .setProcessName(taskConfig.specialNodeName())
                    .setBatchRecordSort(0)
                    .setExecutionMode(EXECUTION_MODE_SEQUENTIAL)
                    .setStatus(TASK_STATUS_WAITING)
                    .setRequiredFlag(Boolean.TRUE)
                    .setBlockerCode(null)
                    .setBlockerMessage(null);
        }
        MesProRouteProcessDO routeProcess = taskConfig.routeProcess();
        MesProProcessDO process = taskConfig.process();
        MesProRouteFlowProcessBatchRecordDO batchRecord = taskConfig.batchRecord();
        MesProBatchRecordReportDO report = taskConfig.report();
        FormTemplateVersionDO templateVersion = resolveRuntimeTemplateVersion(batchRecord);
        String slotConfigSnapshotHash = resolveSlotConfigSnapshotHash(batchRecord);
        boolean requiredTask = isRequiredRoutePolicy(batchRecord.getRequiredPolicy());
        return new MesProEdhrBatchExecutionTaskDO()
                .setBatchExecutionId(batchExecutionId)
                .setNodeType(NODE_TYPE_ROUTE_FORM)
                .setRouteProcessId(routeProcess.getId())
                .setPredecessorRouteProcessId(taskConfig.predecessorRouteProcessId())
                .setRootProcessFlag(taskConfig.predecessorRouteProcessId() == null)
                .setRouteProcessSort(routeProcess.getSort())
                .setProcessId(routeProcess.getProcessId())
                .setProcessCode(process == null ? null : process.getCode())
                .setProcessName(process == null ? null : process.getName())
                .setBatchRecordReportId(null)
                .setBatchRecordReportName(batchRecord.getFormTemplateNameSnapshot())
                .setBatchRecordDefinitionId(null)
                .setBatchRecordVersionId(null)
                .setBatchRecordSort(batchRecord.getReportSort())
                .setInstanceScope(resolveInstanceScope(batchRecord.getInstanceScope()))
                .setSharedFormKey(StrUtil.blankToDefault(StrUtil.trim(batchRecord.getSharedFormKey()), null))
                .setFillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(batchRecord.getFillableScopeJson()), null))
                .setExecutionMode(taskConfig.executionMode())
                .setFormSlotType(null)
                .setFormBindingKey(batchRecord.getFormBindingKey())
                .setFormTemplateId(batchRecord.getFormTemplateId())
                .setFormTemplateNameSnapshot(StrUtil.blankToDefault(
                        StrUtil.trim(templateVersion.getTemplateName()), batchRecord.getFormTemplateNameSnapshot()))
                .setFormTemplateVersionId(templateVersion.getId())
                .setFormTemplateVersionNo(StrUtil.trim(templateVersion.getVersionNo()))
                .setRecordCategory(batchRecord.getRecordCategory())
                .setValidationProfile(batchRecord.getValidationProfile())
                .setPermissionScopeId(batchRecord.getPermissionScopeId())
                .setRouteBindingId(batchRecord.getId())
                .setRouteBindingSnapshotHash(batchRecord.getRecordCategorySnapshotHash())
                .setRequiredPolicy(batchRecord.getRequiredPolicy())
                .setRequiredConditionJson(batchRecord.getRequiredConditionJson())
                .setOwnerRoleKey(batchRecord.getOwnerRoleKey())
                .setArchiveVisibility(batchRecord.getArchiveVisibility())
                .setSlotConfigSnapshotHash(slotConfigSnapshotHash)
                .setStatus(TASK_STATUS_WAITING)
                .setRequiredFlag(requiredTask)
                .setBlockerCode(null)
                .setBlockerMessage(null);
    }

    private boolean isRequiredRoutePolicy(String requiredPolicy) {
        return !"OPTIONAL".equals(StrUtil.trim(requiredPolicy));
    }

    private FormTemplateVersionDO resolveRuntimeTemplateVersion(MesProRouteFlowProcessBatchRecordDO batchRecord) {
        if (batchRecord == null || batchRecord.getFormTemplateId() == null) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_REQUIRED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        FormTemplateVersionDO publishedVersion = formTemplateVersionMapper.selectLatestPublishedByTemplateId(
                tenantId, batchRecord.getFormTemplateId());
        if (publishedVersion == null || publishedVersion.getId() == null
                || !Objects.equals(tenantId, publishedVersion.getTenantId())
                || !Objects.equals(batchRecord.getFormTemplateId(), publishedVersion.getTemplateId())
                || !"PUBLISHED".equals(StrUtil.trim(publishedVersion.getStatus()))
                || StrUtil.isBlank(publishedVersion.getVersionNo())) {
            throw exception(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS,
                    batchRecord.getFormTemplateId());
        }
        return publishedVersion;
    }

    private void createFormCenterInstanceForTask(MesProEdhrBatchExecutionDO batch,
                                                 MesProEdhrBatchExecutionTaskDO task) {
        if (task == null || !NODE_TYPE_ROUTE_FORM.equals(task.getNodeType())) {
            return;
        }
        if (batch == null || batch.getRouteVersionId() == null || task.getId() == null
                || StrUtil.isBlank(task.getFormBindingKey())
                || task.getFormTemplateId() == null || task.getFormTemplateVersionId() == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED);
        }
        FormInstanceCreateReqVO reqVO = new FormInstanceCreateReqVO();
        reqVO.setContext(buildRouteFormActionContext(batch, task));
        reqVO.setIdempotencyKey("EDHR_ROUTE_FORM:" + batch.getId() + ":" + task.getId()
                + ":" + task.getFormBindingKey());
        reqVO.setFormData(new LinkedHashMap<>(Map.of(
                "batchExecutionId", batch.getId(),
                "batchTaskId", task.getId(),
                "routeProcessId", task.getRouteProcessId(),
                "formBindingKey", task.getFormBindingKey(),
                "formTemplateId", task.getFormTemplateId(),
                "formTemplateVersionId", task.getFormTemplateVersionId(),
                "formTemplateVersionNo", task.getFormTemplateVersionNo())));
        FormInstanceRespVO instance = formCenterRuntimeService.createInstance(reqVO, currentUserId());
        if (instance == null || instance.getId() == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED);
        }
        task.setFormCenterInstanceId(instance.getId());
        batchTaskMapper.updateById(new MesProEdhrBatchExecutionTaskDO()
                .setId(task.getId())
                .setFormCenterInstanceId(instance.getId()));
    }

    private BusinessActionContextReqVO buildRouteFormActionContext(MesProEdhrBatchExecutionDO batch,
                                                                   MesProEdhrBatchExecutionTaskDO task) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain(FORM_POLICY_DATA_DOMAIN);
        context.setSystemCode(FORM_POLICY_SYSTEM_CODE);
        context.setObjectType(FORM_POLICY_OBJECT_TYPE);
        context.setObjectId(String.valueOf(task.getId()));
        context.setObjectVersion(String.valueOf(batch.getRouteVersionId()));
        context.setActionCode(MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(
                batch.getRouteVersionId(), task.getFormBindingKey()));
        context.setObjectState(FORM_POLICY_OBJECT_STATE);
        context.setProductCode(batch.getProductCode());
        return context;
    }

    private boolean isOptionalRouteFormTask(MesProEdhrBatchExecutionTaskDO task) {
        return isRouteForm(task) && "OPTIONAL".equals(StrUtil.trim(task.getRequiredPolicy()));
    }

    private String resolveSlotConfigSnapshotHash(MesProRouteFlowProcessBatchRecordDO batchRecord) {
        if (batchRecord == null) {
            return null;
        }
        if (StrUtil.isNotBlank(batchRecord.getSlotConfigSnapshotHash())) {
            return batchRecord.getSlotConfigSnapshotHash();
        }
        return DigestUtil.sha256Hex(String.join("|",
                nullToEmpty(batchRecord.getRouteId()),
                nullToEmpty(batchRecord.getRouteProcessId()),
                StrUtil.nullToEmpty(batchRecord.getBatchRecordReportId()),
                StrUtil.nullToEmpty(batchRecord.getFormSlotType()),
                StrUtil.nullToEmpty(batchRecord.getRecordCategory()),
                StrUtil.nullToEmpty(batchRecord.getValidationProfile()),
                nullToEmpty(batchRecord.getPermissionScopeId()),
                StrUtil.nullToEmpty(batchRecord.getRequiredPolicy()),
                StrUtil.nullToEmpty(batchRecord.getRequiredConditionJson()),
                StrUtil.nullToEmpty(batchRecord.getOwnerRoleKey()),
                StrUtil.nullToEmpty(batchRecord.getArchiveVisibility()),
                nullToEmpty(batchRecord.getReportSort()),
                StrUtil.nullToEmpty(resolveInstanceScope(batchRecord.getInstanceScope())),
                StrUtil.nullToEmpty(batchRecord.getSharedFormKey()),
                StrUtil.nullToEmpty(batchRecord.getFillableScopeJson())));
    }

    private void freezeBatchSharedExecutions(MesProEdhrBatchExecutionDO batch,
                                             List<MesProEdhrBatchExecutionTaskDO> insertedTasks) {
        Map<String, List<MesProEdhrBatchExecutionTaskDO>> sharedTaskMap = insertedTasks.stream()
                .filter(this::isBatchSharedTask)
                .collect(Collectors.groupingBy(task -> StrUtil.trim(task.getSharedFormKey()),
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<MesProEdhrBatchExecutionTaskDO>> entry : sharedTaskMap.entrySet()) {
            validateSameSharedFormIdentity(entry.getKey(), entry.getValue());
        }
    }

    private void validateSameSharedFormIdentity(String sharedFormKey, List<MesProEdhrBatchExecutionTaskDO> tasks) {
        if (StrUtil.isBlank(sharedFormKey) || tasks.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Set<Long> templateIds = tasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFormTemplateId)
                .collect(Collectors.toSet());
        Set<Long> instanceIds = tasks.stream().map(MesProEdhrBatchExecutionTaskDO::getFormCenterInstanceId)
                .collect(Collectors.toSet());
        boolean bindingKeyMissing = tasks.stream()
                .anyMatch(task -> StrUtil.isBlank(task.getFormBindingKey()));
        if (templateIds.size() != 1 || templateIds.contains(null) || bindingKeyMissing || instanceIds.contains(null)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionTaskOpenRespVO openTask(EdhrBatchExecutionTaskOpenReqVO reqVO) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(reqVO.getBatchExecutionId());
        syncBatchStatus(batch);
        batch = batchExecutionMapper.selectById(batch.getId());
        if (Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        requireBatchActionUnlocked(batch.getId());
        MesProEdhrBatchExecutionTaskDO task = batchTaskMapper.selectByIdForUpdate(reqVO.getTaskId());
        if (task == null || !Objects.equals(task.getBatchExecutionId(), batch.getId())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS);
        }
        if (Objects.equals(task.getStatus(), TASK_STATUS_APPROVED)) {
            EdhrBatchExecutionTaskOpenRespVO preReleaseOpenResp =
                    openPreReleaseSubmittedOrdinaryTaskIfAllowed(batch, task, reqVO);
            if (preReleaseOpenResp != null) {
                return preReleaseOpenResp;
            }
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        if (Objects.equals(task.getStatus(), TASK_STATUS_SKIPPED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        if (isSpecialNode(task)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED);
        }
        if (Objects.equals(task.getStatus(), TASK_STATUS_BLOCKED)
                || Boolean.FALSE.equals(task.getRequiredFlag())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED);
        }
        List<MesProEdhrBatchExecutionTaskDO> allTasks = batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        TaskGate taskGate = resolveTaskGate(task, allTasks);
        if (!taskGate.available()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED);
        }
        MesProEdhrWorkTaskDO openWorkTask = resolveOpenWorkTask(task.getId());
        requireOpenWorkTaskContext(reqVO, openWorkTask);
        Long permissionScopeId = resolveTaskPermissionScopeId(task);
        requireTaskFillAbility(batch, task, openWorkTask, permissionScopeId);
        if (task.getFormCenterInstanceId() == null
                || task.getFormTemplateId() == null
                || task.getFormTemplateVersionId() == null
                || StrUtil.isBlank(task.getFormBindingKey())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED);
        }
        if (Objects.equals(task.getStatus(), TASK_STATUS_WAITING)) {
            task.setStatus(TASK_STATUS_DRAFT)
                    .setOpenedBy(currentUserId())
                    .setOpenedAt(LocalDateTime.now());
            batchTaskMapper.updateById(task);
        }
        return buildTaskOpenResp(batch, task, openWorkTask, permissionScopeId);
    }

    private EdhrBatchExecutionTaskOpenRespVO openPreReleaseSubmittedOrdinaryTaskIfAllowed(
            MesProEdhrBatchExecutionDO batch,
            MesProEdhrBatchExecutionTaskDO task,
            EdhrBatchExecutionTaskOpenReqVO reqVO) {
        if (task.getExecutionId() == null || isSpecialNode(task)) {
            return null;
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        if (execution == null
                || !Objects.equals(execution.getStatus(),
                MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED)) {
            return null;
        }
        MesProEdhrPreReleaseEditabilityService.MesProEdhrPreReleaseEditability editability =
                hasGoldenFingerActionBypass()
                        ? preReleaseEditabilityService.requireSubmittedOrdinaryGoldenFingerEditable(
                        execution, reqVO.getWorkTaskId())
                        : preReleaseEditabilityService.requireSubmittedOrdinaryEditable(
                        execution, reqVO.getWorkTaskId());
        Long permissionScopeId = resolveTaskPermissionScopeId(task);
        requireTaskFillAbility(batch, task, editability.workTask(), permissionScopeId);
        return buildTaskOpenResp(batch, task, editability.workTask(), permissionScopeId);
    }

    private EdhrBatchExecutionTaskOpenRespVO buildTaskOpenResp(MesProEdhrBatchExecutionDO batch,
                                                               MesProEdhrBatchExecutionTaskDO task,
                                                               MesProEdhrWorkTaskDO openWorkTask,
                                                               Long permissionScopeId) {
        Map<String, Object> executionPageQuery = new LinkedHashMap<>();
        executionPageQuery.put("id", task.getExecutionId());
        executionPageQuery.put("batchExecutionId", batch.getId());
        executionPageQuery.put("batchTaskId", task.getId());
        executionPageQuery.put("batchCode", batch.getBatchCode());
        executionPageQuery.put("batchRecordDefinitionId", task.getBatchRecordDefinitionId());
        executionPageQuery.put("batchRecordVersionId", task.getBatchRecordVersionId());
        executionPageQuery.put("recordCategory", task.getRecordCategory());
        executionPageQuery.put("validationProfile", task.getValidationProfile());
        executionPageQuery.put("permissionScopeId", permissionScopeId);
        executionPageQuery.put("routeBindingId", task.getRouteBindingId());
        executionPageQuery.put("routeBindingSnapshotHash", task.getRouteBindingSnapshotHash());
        executionPageQuery.put("formSlotType", task.getFormSlotType());
        executionPageQuery.put("formBindingKey", task.getFormBindingKey());
        executionPageQuery.put("formTemplateId", task.getFormTemplateId());
        executionPageQuery.put("formTemplateName", task.getFormTemplateNameSnapshot());
        executionPageQuery.put("formTemplateVersionId", task.getFormTemplateVersionId());
        executionPageQuery.put("formTemplateVersionNo", task.getFormTemplateVersionNo());
        executionPageQuery.put("formCenterInstanceId", task.getFormCenterInstanceId());
        executionPageQuery.put("instanceScope", resolveInstanceScope(task.getInstanceScope()));
        executionPageQuery.put("sharedFormKey", task.getSharedFormKey());
        executionPageQuery.put("fillableScopeJson", task.getFillableScopeJson());
        executionPageQuery.put("requiredPolicy", task.getRequiredPolicy());
        executionPageQuery.put("requiredConditionJson", task.getRequiredConditionJson());
        executionPageQuery.put("ownerRoleKey", task.getOwnerRoleKey());
        executionPageQuery.put("archiveVisibility", task.getArchiveVisibility());
        executionPageQuery.put("slotConfigSnapshotHash", task.getSlotConfigSnapshotHash());
        Long workTaskId = openWorkTask == null ? null : openWorkTask.getId();
        if (workTaskId != null) {
            executionPageQuery.put("workTaskId", workTaskId);
        }
        EdhrBatchExecutionTaskOpenRespVO result = new EdhrBatchExecutionTaskOpenRespVO()
                .setTaskId(task.getId())
                .setExecutionId(task.getExecutionId())
                .setWorkTaskId(workTaskId)
                .setRouteProcessId(task.getRouteProcessId())
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordDefinitionId(task.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(task.getBatchRecordVersionId())
                .setBatchRecordSort(task.getBatchRecordSort())
                .setInstanceScope(resolveInstanceScope(task.getInstanceScope()))
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
                .setPermissionScopeId(permissionScopeId)
                .setRouteBindingId(task.getRouteBindingId())
                .setRouteBindingSnapshotHash(task.getRouteBindingSnapshotHash())
                .setRequiredPolicy(task.getRequiredPolicy())
                .setRequiredConditionJson(task.getRequiredConditionJson())
                .setOwnerRoleKey(task.getOwnerRoleKey())
                .setArchiveVisibility(task.getArchiveVisibility())
                .setSlotConfigSnapshotHash(task.getSlotConfigSnapshotHash())
                .setStatus(task.getStatus())
                .setExecutionPageQuery(executionPageQuery);
        recordOperationAudit("BATCH_EXECUTION_TASK", String.valueOf(task.getId()), "OPEN",
                "打开 eDHR 工序任务", batch.getId(), task.getExecutionId(), workTaskId, batch.getRouteId(),
                task.getRouteProcessId(), task.getBatchRecordReportId(), task.getRecordCategory(),
                "mes:pro-edhr-batch-execution:update", "ALLOW", "SUCCESS", null, null,
                JSON.toJSONString(executionPageQuery));
        return result;
    }

    private Long resolveTaskPermissionScopeId(MesProEdhrBatchExecutionTaskDO task) {
        return task.getPermissionScopeId();
    }

    private MesProEdhrWorkTaskDO resolveOpenWorkTask(Long batchTaskId) {
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectActiveByBatchTaskAndType(batchTaskId, WORK_TASK_TYPE_FILL);
        if (workTask == null) {
            workTask = workTaskMapper.selectActiveByBatchTaskAndType(batchTaskId, WORK_TASK_TYPE_REWORK);
        }
        return workTask;
    }

    private void requireOpenWorkTaskContext(EdhrBatchExecutionTaskOpenReqVO reqVO, MesProEdhrWorkTaskDO openWorkTask) {
        if (!isFillOrReworkWorkTask(openWorkTask)) {
            return;
        }
        if (reqVO.getWorkTaskId() == null || !Objects.equals(reqVO.getWorkTaskId(), openWorkTask.getId())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE);
        }
    }

    private void requireTaskFillAbility(MesProEdhrBatchExecutionDO batch, MesProEdhrBatchExecutionTaskDO task,
                                        MesProEdhrWorkTaskDO workTask, Long permissionScopeId) {
        if (isFillOrReworkWorkTask(workTask)
                && !hasGoldenFingerActionBypass()
                && !isAssignedOrCandidate(workTask, currentUserId())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE);
        }
        if (permissionScopeId == null) {
            return;
        }
        permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                .setScopeId(permissionScopeId)
                .setObjectType("BATCH_EXECUTION_TASK")
                .setObjectId(String.valueOf(task.getId()))
                .setAbility("FILL")
                .setBatchExecutionId(batch.getId())
                .setWorkTaskId(workTask == null ? null : workTask.getId())
                .setRouteId(batch.getRouteId())
                .setRouteProcessId(task.getRouteProcessId())
                .setReportId(task.getBatchRecordReportId())
                .setRecordCategory(task.getRecordCategory())
                .setPermissionCode("mes:pro-edhr-batch-execution:update")
                .setActionName("打开 eDHR 工序任务"));
    }

    private boolean isFillOrReworkWorkTask(MesProEdhrWorkTaskDO workTask) {
        return workTask != null
                && (WORK_TASK_TYPE_FILL.equals(workTask.getTaskType())
                || WORK_TASK_TYPE_REWORK.equals(workTask.getTaskType()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO syncStatus(Long id) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(id);
        syncBatchStatus(batch);
        reconcileResolvedSpecialNodeAdvance(id);
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(id);
        EdhrBatchExecutionRespVO result = toResp(latest);
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(id), "SYNC",
                "同步 eDHR 批次状态", id, null, null, latest.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:update", "ALLOW",
                "SUCCESS", null, null, null);
        return result;
    }

    private void reconcileResolvedSpecialNodeAdvance(Long batchExecutionId) {
        batchTaskMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                .filter(this::isSpecialNode)
                .filter(task -> SKIPPABLE_SPECIAL_NODE_TYPES.contains(resolveNodeType(task)))
                .filter(this::isTaskApproved)
                .sorted(Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort,
                                Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(MesProEdhrBatchExecutionTaskDO::getId,
                                Comparator.nullsFirst(Long::compareTo)))
                .forEach(workTaskService::createNextFillAfterSpecialNodeResolved);
    }

    @Override
    public EdhrBatchExecutionReviewTimelineRespVO getReviewTimeline(Long id) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(id);
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        batchExecutionVisibilityService.requireVisibleBatch(batch, tasks, currentUserId());
        List<MesProEdhrBatchExecutionSignatureDO> signatures =
                batchSignatureMapper.selectListByBatchExecutionId(batch.getId());
        List<MesProEdhrBatchExecutionArchiveDO> archives =
                batchArchiveMapper.selectListByBatchExecutionId(batch.getId());
        List<MesProEdhrBatchDossierItemDO> dossierItems =
                dossierItemMapper.selectListByBatchExecutionId(batch.getId());
        Map<Long, TaskGate> taskGateMap = buildTaskGateMap(tasks);
        List<String> taskIds = tasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
        List<MesProEdhrFlowEventDO> flowEvents =
                flowEventMapper.selectListByTaskIds(taskIds, FLOW_EVENT_TYPE_FLOW_INTERVENTION);
        List<EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview> executionReviews = tasks.stream()
                .map(this::toExecutionReview)
                .filter(Objects::nonNull)
                .toList();
        List<EdhrBatchExecutionReviewTimelineRespVO.ApprovalRecord> approvalRecords = executionReviews.stream()
                .map(EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview::getApprovalSummary)
                .filter(Objects::nonNull)
                .filter(summary -> summary.getApprovedRecord() != null)
                .map(summary -> summary.getApprovedRecord())
                .toList();
        return new EdhrBatchExecutionReviewTimelineRespVO()
                .setBatchExecutionId(batch.getId())
                .setBatchEvents(List.of(new EdhrBatchExecutionReviewTimelineRespVO.BatchEvent()
                        .setBatchExecutionId(batch.getId())
                        .setBatchExecutionCode(batch.getBatchExecutionCode())
                        .setStatus(batch.getStatus())
                        .setAggregateHash(batch.getAggregateHash())
                        .setClosedBy(batch.getClosedBy())
                        .setClosedAt(batch.getClosedAt())
                        .setCloseSignatureId(batch.getCloseSignatureId())
                        .setRejectSignatureId(batch.getRejectSignatureId())
                        .setRejectedBy(batch.getRejectedBy())
                        .setRejectedAt(batch.getRejectedAt())
                        .setRejectReason(batch.getRejectReason())
                        .setCreateTime(batch.getCreateTime())))
                .setTaskEvents(tasks.stream().map(task -> toTaskEvent(task, taskGateMap.get(task.getId()))).toList())
                .setSignatureRecords(signatures.stream().map(this::toSignatureRecord).toList())
                .setApprovalRecords(approvalRecords)
                .setFlowEvents(flowEvents.stream().map(this::toFlowEvent).toList())
                .setArchiveVersions(archives.stream().map(this::toArchiveResp).toList())
                .setDossierItems(dossierItems.stream().map(this::toTimelineDossierItem).toList())
                .setExecutionReviews(executionReviews);
    }

    @Override
    public EdhrBatchExecutionTaskPreviewRespVO previewTask(Long batchExecutionId, Long taskId) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(batchExecutionId);
        List<MesProEdhrBatchExecutionTaskDO> tasks =
                batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        MesProEdhrBatchExecutionTaskDO task = tasks.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), taskId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS));
        batchExecutionVisibilityService.requireVisibleBatch(batch, tasks, currentUserId());
        if (task.getExecutionId() != null) {
            EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview review = toExecutionReview(task);
            if (review == null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS);
            }
            return new EdhrBatchExecutionTaskPreviewRespVO()
                    .setBatchExecutionId(batchExecutionId)
                    .setTaskId(taskId)
                    .setExecutionId(task.getExecutionId())
                    .setTaskStatus(task.getStatus())
                    .setExecutionCreated(true)
                    .setFormViewModel(review.getFormViewModel());
        }
        if (StrUtil.isBlank(task.getBatchRecordReportId())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(task.getBatchRecordReportId());
        if (report == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NOT_EXISTS);
        }
        String reportJson = jimuReportGateway.getReportJson(report.getReportId());
        if (StrUtil.isBlank(reportJson)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    report.getReportId());
        }
        String tableTitle = StrUtil.blankToDefault(report.getTableTitle(),
                StrUtil.blankToDefault(report.getReportName(), task.getBatchRecordReportName()));
        return new EdhrBatchExecutionTaskPreviewRespVO()
                .setBatchExecutionId(batchExecutionId)
                .setTaskId(taskId)
                .setTaskStatus(task.getStatus())
                .setExecutionCreated(false)
                .setFormViewModel(new EdhrBatchExecutionReviewTimelineRespVO.FormViewModel()
                        .setSheetLayoutJson(reportJson)
                        .setMetaJson(JSON.toJSONString(Map.of("tableTitle", tableTitle)))
                        .setCellValuesJson("[]")
                        .setSignatureCellMarkers(extractSignatureCellMarkers(reportJson)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO close(EdhrBatchExecutionCloseReqVO reqVO) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(reqVO.getId());
        if (Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        boolean goldenFingerActionBypass = hasGoldenFingerActionBypass();
        requireBatchActionUnlocked(batch.getId());
        List<String> blockers = goldenFingerActionBypass ? List.of() : collectCloseBlockers(batch.getId());
        if (!blockers.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_CLOSE_BLOCKED, String.join("；", blockers));
        }
        if (!goldenFingerActionBypass) {
            requireReleasePrecheckPassedBeforeClose(batch.getId());
        }
        if (!goldenFingerActionBypass) {
            validateCurrentUserIsBatchOwner(batch);
        }
        String aggregateHash = DigestUtil.sha256Hex(batch.getId() + ":" + reqVO.getComment());
        Long actorId = currentUserId();
        validateBatchSignaturePassword(batch, actorId, reqVO.getPassword(), "BATCH_CLOSE",
                "关闭 eDHR 批次", "mes:pro-edhr-batch-execution:close");
        LocalDateTime signedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        BatchSignatureTimeEvidence signatureTimeEvidence =
                buildBatchSignatureTimeEvidence(batch.getId(), "BATCH_CLOSE", actorId, signedAt,
                        reqVO.getSignatureTime());
        MesProEdhrBatchExecutionSignatureDO signature = new MesProEdhrBatchExecutionSignatureDO()
                .setBatchExecutionId(batch.getId())
                .setActorId(actorId)
                .setActorName(String.valueOf(actorId))
                .setActionType("BATCH_CLOSE")
                .setSignatureMode("PASSWORD")
                .setPasswordVerified(true)
                .setComment(reqVO.getComment())
                .setSignedAt(signedAt)
                .setSelectedSignedAt(signatureTimeEvidence.selectedSignedAt())
                .setSignatureDisplayAt(signatureTimeEvidence.signatureDisplayAt())
                .setSignatureTimeMode(signatureTimeEvidence.signatureTimeMode())
                .setSelectedTimeZone(signatureTimeEvidence.selectedTimeZone())
                .setSelectedTimeReason(signatureTimeEvidence.selectedTimeReason())
                .setSelectedTimePolicyVersion(signatureTimeEvidence.selectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signatureTimeEvidence.selectedTimeAuditHash())
                .setSignatureChallengeHash(DigestUtil.sha256Hex(batch.getId() + ":" + reqVO.getPassword()))
                .setAggregateHash(aggregateHash);
        batchSignatureMapper.insert(signature);

        batch.setStatus(BATCH_STATUS_CLOSED)
                .setClosedAt(signedAt)
                .setClosedBy(actorId)
                .setCloseSignatureId(signature.getId())
                .setAggregateHash(aggregateHash);
        batchExecutionMapper.updateById(batch);
        workTaskService.createArchiveTaskAfterBatchClose(batch);
        EdhrBatchExecutionRespVO result = toResp(batch);
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(batch.getId()), "CLOSE",
                "关闭 eDHR 批次", batch.getId(), null, null, batch.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:close", "ALLOW",
                "SUCCESS", null, aggregateHash, JSON.toJSONString(Map.of("comment", value(reqVO.getComment()))));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionRespVO qualityReject(EdhrBatchExecutionQualityRejectReqVO reqVO) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(reqVO.getId());
        if (Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        requireBatchActionUnlocked(batch.getId());
        requireQualityRejectPrecheckStage(batch);
        if (StrUtil.isBlank(reqVO.getReason()) || StrUtil.isBlank(reqVO.getPassword())) {
            throw exception(BAD_REQUEST, "质量终态拒收必须填写原因和签名密码");
        }
        Long actorId = currentUserId();
        validateBatchSignaturePassword(batch, actorId, reqVO.getPassword(), "QUALITY_REJECT",
                "质量终态拒收 eDHR 批次", "mes:pro-edhr-batch-execution:quality-reject");
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String reason = StrUtil.trim(reqVO.getReason());
        String aggregateHash = DigestUtil.sha256Hex(batch.getId() + ":QUALITY_REJECT:" + reason);
        BatchSignatureTimeEvidence signatureTimeEvidence =
                buildBatchSignatureTimeEvidence(batch.getId(), "QUALITY_REJECT", actorId, now,
                        reqVO.getSignatureTime());
        MesProEdhrBatchExecutionSignatureDO signature = new MesProEdhrBatchExecutionSignatureDO()
                .setBatchExecutionId(batch.getId())
                .setActorId(actorId)
                .setActorName(String.valueOf(actorId))
                .setActionType("QUALITY_REJECT")
                .setSignatureMode("PASSWORD")
                .setPasswordVerified(true)
                .setComment(reason)
                .setSignedAt(now)
                .setSelectedSignedAt(signatureTimeEvidence.selectedSignedAt())
                .setSignatureDisplayAt(signatureTimeEvidence.signatureDisplayAt())
                .setSignatureTimeMode(signatureTimeEvidence.signatureTimeMode())
                .setSelectedTimeZone(signatureTimeEvidence.selectedTimeZone())
                .setSelectedTimeReason(signatureTimeEvidence.selectedTimeReason())
                .setSelectedTimePolicyVersion(signatureTimeEvidence.selectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signatureTimeEvidence.selectedTimeAuditHash())
                .setSignatureChallengeHash(DigestUtil.sha256Hex(batch.getId() + ":" + reqVO.getPassword()))
                .setAggregateHash(aggregateHash);
        batchSignatureMapper.insert(signature);

        batch.setStatus(BATCH_STATUS_REJECTED)
                .setRejectSignatureId(signature.getId())
                .setRejectedBy(actorId)
                .setRejectedAt(now)
                .setRejectReason(reason)
                .setAggregateHash(aggregateHash);
        batchExecutionMapper.updateById(batch);
        workTaskService.cancelActiveTasksByBatch(batch.getId(), "质量终态拒收：" + reason);
        EdhrBatchExecutionRespVO result = toResp(batch);
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(batch.getId()), "QUALITY_REJECT",
                "质量终态拒收 eDHR 批次", batch.getId(), null, null, batch.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution:quality-reject", "ALLOW",
                "SUCCESS", null, aggregateHash, JSON.toJSONString(Map.of("reason", reason)));
        return result;
    }

    private void requireQualityRejectPrecheckStage(MesProEdhrBatchExecutionDO batch) {
        if (!Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrReleaseTransactionDO releaseTransaction =
                releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        if (releaseTransaction == null) {
            return;
        }
        String releaseStatus = releaseTransaction.getReleaseStatus();
        if (MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED.equals(releaseStatus)
                || MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(releaseStatus)
                || MesProEdhrReleaseServiceImpl.STATUS_RELEASED.equals(releaseStatus)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
    }

    private void validateBatchSignaturePassword(MesProEdhrBatchExecutionDO batch, Long actorId, String password,
                                                String operationType, String actionName, String permissionCode) {
        if (StrUtil.isBlank(password)) {
            recordSignaturePasswordFailure(batch, operationType, actionName, permissionCode,
                    "签名密码不能为空");
            throw exception(BAD_REQUEST, actionName + "必须填写签名密码");
        }
        try {
            adminUserApi.validatePassword(actorId, password);
        } catch (RuntimeException ex) {
            recordSignaturePasswordFailure(batch, operationType, actionName, permissionCode,
                    StrUtil.blankToDefault(ex.getMessage(), "签名密码校验失败"));
            throw ex;
        }
    }

    private void recordSignaturePasswordFailure(MesProEdhrBatchExecutionDO batch, String operationType,
                                                String actionName, String permissionCode, String failureReason) {
        recordOperationAudit("BATCH_EXECUTION", String.valueOf(batch.getId()), operationType,
                actionName, batch.getId(), null, null, batch.getRouteId(), null,
                null, null, permissionCode, "DENY", "FAILED",
                batch.getAggregateHash(), null, JSON.toJSONString(Map.of("failureReason", value(failureReason))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrBatchExecutionArchiveRespVO generateArchive(EdhrBatchExecutionArchiveGenerateReqVO reqVO) {
        MesProEdhrBatchExecutionDO batch = validateBatchExists(reqVO.getBatchExecutionId());
        requireBatchActionUnlocked(batch.getId());
        MesProEdhrWorkTaskDO archiveTask = workTaskService.validateArchiveTask(reqVO.getWorkTaskId(), batch.getId());
        if (!Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)
                && !Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_CLOSED);
        }
        List<MesProEdhrBatchExecutionArchiveDO> existing = batchArchiveMapper.selectListByBatchExecutionId(batch.getId());
        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getArchiveVersion() + 1;
        LocalDateTime generatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String manifest = buildArchiveManifest(batch, generatedAt);
        byte[] pdfBytes = buildPdfBytes(manifest);
        MesProEdhrBatchExecutionArchiveDO archive = new MesProEdhrBatchExecutionArchiveDO()
                .setBatchExecutionId(batch.getId())
                .setArtifactType(StrUtil.blankToDefault(reqVO.getArtifactType(), ARTIFACT_TYPE_BATCH_FINAL_PDF))
                .setArchiveVersion(nextVersion)
                .setArchiveStatus(ARCHIVE_STATUS_SEALED)
                .setFileName(batch.getBatchCode() + "-edhr-final.pdf")
                .setContentType("application/pdf")
                .setFileSize((long) pdfBytes.length)
                .setContentHash(DigestUtil.sha256Hex(pdfBytes))
                .setSourceManifestJson(manifest)
                .setGeneratedBy(currentUserId())
                .setGeneratedAt(generatedAt);
        batchArchiveMapper.insert(archive);
        if (!Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)) {
            batch.setStatus(BATCH_STATUS_ARCHIVED);
            batchExecutionMapper.updateById(batch);
        }
        workTaskService.completeArchiveTask(archiveTask.getId(), batch.getId());
        EdhrBatchExecutionArchiveRespVO result = toArchiveResp(archive);
        recordOperationAudit("BATCH_ARCHIVE", String.valueOf(archive.getId()), "ARCHIVE",
                "生成 eDHR 批次最终归档", batch.getId(), null, archiveTask.getId(), batch.getRouteId(), null,
                null, null, "mes:pro-edhr-batch-execution-archive:create", "ALLOW",
                "SUCCESS", null, archive.getContentHash(), null);
        return result;
    }

    @Override
    public EdhrBatchExecutionArchiveRespVO getLatestArchive(Long batchExecutionId) {
        List<MesProEdhrBatchExecutionArchiveDO> archives = batchArchiveMapper.selectListByBatchExecutionId(batchExecutionId);
        if (archives.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_EXISTS);
        }
        return toArchiveResp(archives.get(0));
    }

    @Override
    public EdhrBatchExecutionArchiveDownloadRespVO downloadArchive(Long id) {
        MesProEdhrBatchExecutionArchiveDO archive = batchArchiveMapper.selectById(id);
        if (archive == null || Boolean.TRUE.equals(archive.getDeleted())
                || !ARCHIVE_STATUS_SEALED.equals(archive.getArchiveStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_EXISTS);
        }
        byte[] content = buildPdfBytes(archive.getSourceManifestJson());
        EdhrBatchExecutionArchiveDownloadRespVO result = new EdhrBatchExecutionArchiveDownloadRespVO()
                .setFileName(archive.getFileName())
                .setContentType(archive.getContentType())
                .setFileSize((long) content.length)
                .setContent(content);
        recordOperationAudit("BATCH_ARCHIVE", String.valueOf(archive.getId()), "DOWNLOAD",
                "下载 eDHR 批次最终归档", archive.getBatchExecutionId(), null, null, null, null,
                null, null, "mes:pro-edhr-batch-execution-archive:download", "ALLOW",
                "SUCCESS", archive.getContentHash(), archive.getContentHash(), null);
        return result;
    }

    private MesProEdhrBatchExecutionDO validateBatchExists(Long id) {
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(id);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private void createDefaultDossierItems(MesProEdhrBatchExecutionDO batch) {
        dossierItemMapper.insert(new MesProEdhrBatchDossierItemDO()
                .setBatchExecutionId(batch.getId())
                .setItemType(DOSSIER_ITEM_TYPE_FINAL_INSPECTION)
                .setItemKey(DOSSIER_ITEM_KEY_FINAL_INSPECTION)
                .setItemName(DOSSIER_ITEM_NAME_FINAL_INSPECTION)
                .setRequiredFlag(Boolean.TRUE)
                .setItemStatus(DOSSIER_ITEM_STATUS_PENDING));
    }

    private List<BatchTaskConfig> buildBatchTaskConfigs(MesProRouteDO route, List<MesProRouteProcessDO> routeProcesses) {
        return addSpecialBatchTaskConfigs(resolveBatchTaskConfigs(route, routeProcesses));
    }

    private List<BatchTaskConfig> buildBatchTaskConfigs(MesProEdhrBatchExecutionDO batch,
                                                        MesProRouteDO route,
                                                        List<MesProRouteProcessDO> routeProcesses) {
        return addSpecialBatchTaskConfigs(resolveBatchTaskConfigs(batch, route, routeProcesses));
    }

    private List<BatchTaskConfig> addSpecialBatchTaskConfigs(List<BatchTaskConfig> routeConfigs) {
        List<BatchTaskConfig> taskConfigs = new ArrayList<>();
        taskConfigs.add(BatchTaskConfig.special(
                NODE_TYPE_INCOMING_INSPECTION_REPORT, "来料检报告", SPECIAL_SORT_INCOMING_INSPECTION));
        taskConfigs.addAll(routeConfigs);
        taskConfigs.add(BatchTaskConfig.special(
                NODE_TYPE_STERILIZATION_REPORT, "灭菌报告", SPECIAL_SORT_STERILIZATION));
        taskConfigs.add(BatchTaskConfig.special(
                NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT, "成品检报告",
                SPECIAL_SORT_FINISHED_PRODUCT_INSPECTION_REPORT));
        taskConfigs.add(BatchTaskConfig.special(
                NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD, "成品检记录",
                SPECIAL_SORT_FINISHED_PRODUCT_INSPECTION_RECORD));
        return taskConfigs;
    }

    private List<BatchTaskConfig> resolveBatchTaskConfigs(MesProRouteDO route,
                                                          List<MesProRouteProcessDO> routeProcesses) {
        if (routeProcesses.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        String batchUseType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        if (!hasBatchFlowConfigContext(route.getId(), batchUseType)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Map<Long, MesProRouteProcessDO> routeProcessMap = routeProcesses.stream()
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        List<MesProRouteFlowProcessConfigDO> enabledConfigs =
                routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(route.getId(), batchUseType).stream()
                        .toList();
        if (enabledConfigs.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Map<Long, Long> currentRouteProcessIdBySnapshot =
                resolveCurrentRouteProcessIdBySnapshot(route.getId(), routeProcessMap, enabledConfigs);
        Map<Long, MesProRouteFlowProcessConfigDO> enabledConfigMap = enabledConfigs.stream()
                .filter(config -> config.getId() != null)
                .collect(Collectors.toMap(MesProRouteFlowProcessConfigDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, MesProRouteFlowProcessConfigDO> configMap =
                buildCurrentRouteProcessConfigMap(enabledConfigs, currentRouteProcessIdBySnapshot);
        List<MesProRouteFlowProcessBatchRecordDO> batchRecords =
                routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(
                                currentRouteProcessIdBySnapshot.keySet(), batchUseType).stream()
                        .filter(record -> isOwnedByEnabledProcessConfig(record, enabledConfigMap, batchUseType))
                        .filter(record -> record.getFormTemplateId() != null)
                        .toList();
        if (batchRecords.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Map<Long, List<MesProRouteFlowProcessBatchRecordDO>> recordMap = batchRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> currentRouteProcessIdBySnapshot.get(record.getRouteProcessId()),
                        LinkedHashMap::new, Collectors.toList()));
        List<MesProRouteProcessDO> resolvedRouteProcesses = currentRouteProcessIdBySnapshot.values().stream()
                .distinct()
                .map(routeProcessMap::get)
                .toList();
        if (resolvedRouteProcesses.stream().anyMatch(Objects::isNull)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Set<Long> processIds = resolvedRouteProcesses.stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MesProProcessDO> processMap = processIds.isEmpty() ? Collections.emptyMap()
                : processMapper.selectBatchIds(processIds).stream()
                        .collect(Collectors.toMap(MesProProcessDO::getId, item -> item, (left, right) -> left));
        Map<Long, Long> predecessorMap = buildRouteProcessPredecessorMap(route.getId(), resolvedRouteProcesses);
        List<BatchTaskConfig> taskConfigs = new ArrayList<>();
        List<MesProRouteProcessDO> orderedRouteProcesses = resolvedRouteProcesses.stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        for (MesProRouteProcessDO routeProcess : orderedRouteProcesses) {
            MesProRouteFlowProcessConfigDO config = configMap.get(routeProcess.getId());
            if (config == null) {
                continue;
            }
            String executionMode = resolveBatchExecutionMode(config.getExecutionMode());
            List<MesProRouteFlowProcessBatchRecordDO> records =
                    recordMap.getOrDefault(routeProcess.getId(), Collections.emptyList()).stream()
                            .sorted(Comparator.comparing(MesProRouteFlowProcessBatchRecordDO::getReportSort,
                                            Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(MesProRouteFlowProcessBatchRecordDO::getId,
                                            Comparator.nullsLast(Long::compareTo)))
                            .toList();
            if (records.isEmpty()) {
                continue;
            }
            for (MesProRouteFlowProcessBatchRecordDO record : records) {
                validateDynamicRouteFormBinding(record);
                taskConfigs.add(new BatchTaskConfig(routeProcess, processMap.get(routeProcess.getProcessId()),
                        record, null, executionMode, predecessorMap.get(routeProcess.getId()),
                        null, null, null));
            }
        }
        if (taskConfigs.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return taskConfigs;
    }

    private List<BatchTaskConfig> resolveBatchTaskConfigs(MesProEdhrBatchExecutionDO batch,
                                                          MesProRouteDO route,
                                                          List<MesProRouteProcessDO> routeProcesses) {
        if (batch.getRouteVersionId() != null) {
            return resolveFrozenBatchTaskConfigs(batch);
        }
        return resolveBatchTaskConfigs(route, routeProcesses);
    }

    private List<BatchTaskConfig> resolveFrozenBatchTaskConfigs(MesProEdhrBatchExecutionDO batch) {
        JSONObject snapshot = parseFrozenRouteSnapshot(batch.getRouteSnapshotJson());
        JSONObject configSnapshots = requireFrozenObject(snapshot, "configSnapshots");
        JSONObject flowGraph = requireFrozenObject(configSnapshots, "flowGraph");
        List<JSONObject> flowNodes = requireFrozenObjectArray(flowGraph, "nodes");
        List<JSONObject> batchUseConfigs = requireFrozenObjectArray(configSnapshots, "batchUseConfigs");
        if (flowNodes.isEmpty() || batchUseConfigs.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Map<Long, JSONObject> nodeByRouteProcessId = new LinkedHashMap<>();
        Map<Integer, JSONObject> nodeBySort = new LinkedHashMap<>();
        for (JSONObject node : flowNodes) {
            Long routeProcessId = node.getLong("routeProcessId");
            Integer sort = node.getInteger("sort");
            if (routeProcessId != null) {
                nodeByRouteProcessId.put(routeProcessId, node);
            }
            if (sort != null) {
                nodeBySort.put(sort, node);
            }
        }
        Map<Long, Long> predecessorMap = buildFrozenRouteProcessPredecessorMap(flowGraph, nodeByRouteProcessId, nodeBySort);
        Set<Long> processIds = flowNodes.stream()
                .map(node -> node.getLong("processId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processMap = processIds.isEmpty() ? Collections.emptyMap()
                : processMapper.selectBatchIds(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, item -> item, (left, right) -> left));
        List<FrozenBatchUseConfig> flattenedConfigs = new ArrayList<>();
        for (JSONObject processConfig : batchUseConfigs) {
            JSONObject node = resolveFrozenRouteProcessNode(processConfig, nodeByRouteProcessId, nodeBySort);
            for (JSONObject bindingConfig : resolveFrozenFormBindings(processConfig)) {
                flattenedConfigs.add(new FrozenBatchUseConfig(processConfig, node, bindingConfig,
                        bindingConfig.getString("reportId")));
            }
        }
        if (flattenedConfigs.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        List<BatchTaskConfig> taskConfigs = flattenedConfigs.stream()
                .sorted(Comparator.comparing(FrozenBatchUseConfig::routeProcessSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(FrozenBatchUseConfig::reportSort, Comparator.nullsLast(Integer::compareTo)))
                .map(config -> toFrozenBatchTaskConfig(batch, config, processMap, predecessorMap))
                .toList();
        if (taskConfigs.isEmpty()) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return taskConfigs;
    }

    private JSONObject parseFrozenRouteSnapshot(String routeSnapshotJson) {
        if (StrUtil.isBlank(routeSnapshotJson)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        try {
            return JSON.parseObject(routeSnapshotJson);
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
    }

    private JSONObject requireFrozenObject(JSONObject parent, String key) {
        JSONObject object = parent == null ? null : parent.getJSONObject(key);
        if (object == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return object;
    }

    private List<JSONObject> requireFrozenObjectArray(JSONObject parent, String key) {
        JSONArray array = parent == null ? null : parent.getJSONArray(key);
        if (array == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object value : array) {
            if (!(value instanceof JSONObject object)) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            result.add(object);
        }
        return result;
    }

    private Map<Long, Long> buildFrozenRouteProcessPredecessorMap(
            JSONObject flowGraph,
            Map<Long, JSONObject> nodeByRouteProcessId,
            Map<Integer, JSONObject> nodeBySort) {
        JSONArray edges = flowGraph.getJSONArray("edges");
        if (edges == null || edges.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> predecessorMap = new LinkedHashMap<>();
        for (Object value : edges) {
            if (!(value instanceof JSONObject edge)) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            Long sourceRouteProcessId = resolveFrozenEdgeRouteProcessId(edge,
                    "sourceRouteProcessId", "sourceSort", nodeByRouteProcessId, nodeBySort);
            Long targetRouteProcessId = resolveFrozenEdgeRouteProcessId(edge,
                    "targetRouteProcessId", "targetSort", nodeByRouteProcessId, nodeBySort);
            if (sourceRouteProcessId == null || targetRouteProcessId == null
                    || Objects.equals(sourceRouteProcessId, targetRouteProcessId)
                    || predecessorMap.putIfAbsent(targetRouteProcessId, sourceRouteProcessId) != null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
        }
        return predecessorMap;
    }

    private Long resolveFrozenEdgeRouteProcessId(JSONObject edge,
                                                String idKey,
                                                String sortKey,
                                                Map<Long, JSONObject> nodeByRouteProcessId,
                                                Map<Integer, JSONObject> nodeBySort) {
        Long routeProcessId = edge.getLong(idKey);
        if (routeProcessId != null && nodeByRouteProcessId.containsKey(routeProcessId)) {
            return routeProcessId;
        }
        Integer sort = edge.getInteger(sortKey);
        JSONObject node = sort == null ? null : nodeBySort.get(sort);
        return node == null ? null : node.getLong("routeProcessId");
    }

    private JSONObject resolveFrozenRouteProcessNode(JSONObject processConfig,
                                                     Map<Long, JSONObject> nodeByRouteProcessId,
                                                     Map<Integer, JSONObject> nodeBySort) {
        Long routeProcessId = processConfig.getLong("routeProcessId");
        if (routeProcessId != null && nodeByRouteProcessId.containsKey(routeProcessId)) {
            return nodeByRouteProcessId.get(routeProcessId);
        }
        Integer sort = processConfig.getInteger("sort");
        JSONObject node = sort == null ? null : nodeBySort.get(sort);
        if (node == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return node;
    }

    private List<JSONObject> resolveFrozenFormBindings(JSONObject processConfig) {
        JSONArray bindings = processConfig.getJSONArray("formBindings");
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object value : bindings) {
            if (!(value instanceof JSONObject binding)) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            if (binding.getLong("formTemplateId") != null) {
                result.add(binding);
            }
        }
        return result;
    }

    private BatchTaskConfig toFrozenBatchTaskConfig(
            MesProEdhrBatchExecutionDO batch,
            FrozenBatchUseConfig config,
            Map<Long, MesProProcessDO> processMap,
            Map<Long, Long> predecessorMap) {
        JSONObject processConfig = config.processConfig();
        JSONObject node = config.node();
        Long routeProcessId = node.getLong("routeProcessId");
        Integer sort = node.getInteger("sort");
        Long processId = node.getLong("processId");
        if (routeProcessId == null || sort == null || processId == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId)
                .routeId(batch.getRouteId())
                .processId(processId)
                .sort(sort)
                .keyFlag(node.getBoolean("keyFlag"))
                .checkFlag(node.getBoolean("checkFlag"))
                .build();
        MesProProcessDO process = processMap.get(processId);
        if (process == null) {
            process = MesProProcessDO.builder()
                    .id(processId)
                    .code(StrUtil.blankToDefault(node.getString("processCode"), processConfig.getString("processCode")))
                    .name(StrUtil.blankToDefault(node.getString("processName"), processConfig.getString("processName")))
                    .build();
        }
        MesProRouteFlowProcessBatchRecordDO batchRecord = toFrozenBatchRecordDO(
                batch, routeProcess, processConfig, config.reportConfig());
        validateDynamicRouteFormBinding(batchRecord);
        return new BatchTaskConfig(routeProcess, process, batchRecord, null,
                resolveBatchExecutionMode(processConfig.getString("executionMode")),
                predecessorMap.get(routeProcessId), null, null, null);
    }

    private MesProRouteFlowProcessBatchRecordDO toFrozenBatchRecordDO(
            MesProEdhrBatchExecutionDO batch,
            MesProRouteProcessDO routeProcess,
            JSONObject processConfig,
            JSONObject bindingConfig) {
        String ownerRoleKey = normalizeRouteOwnerRoleKey(bindingConfig.getString("ownerRoleKey"), FORM_SLOT_MAIN);
        String recordCategory = normalizeRouteRecordCategory(bindingConfig.getString("recordCategory"), FORM_SLOT_MAIN,
                bindingConfig.getString("ownerRoleKey"));
        String validationProfile = normalizeRouteValidationProfile(bindingConfig.getString("validationProfile"),
                recordCategory);
        String requiredPolicy = normalizeRouteRequiredPolicy(bindingConfig.getString("requiredPolicy"));
        String archiveVisibility = normalizeRouteArchiveVisibility(bindingConfig.getString("archiveVisibility"));
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(bindingConfig.getLong("routeBindingId"))
                .routeId(batch.getRouteId())
                .routeProcessId(routeProcess.getId())
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formBindingKey(bindingConfig.getString("formBindingKey"))
                .formTemplateId(bindingConfig.getLong("formTemplateId"))
                .formTemplateNameSnapshot(bindingConfig.getString("formTemplateName"))
                .lastPublishedTemplateVersionId(bindingConfig.getLong("lastPublishedTemplateVersionId"))
                .lastPublishedTemplateVersionNo(bindingConfig.getString("lastPublishedTemplateVersionNo"))
                .instanceScope(resolveInstanceScope(bindingConfig.getString("instanceScope")))
                .sharedFormKey(StrUtil.blankToDefault(StrUtil.trim(bindingConfig.getString("sharedFormKey")), null))
                .fillableScopeJson(StrUtil.blankToDefault(StrUtil.trim(bindingConfig.getString("fillableScopeJson")), null))
                .recordCategory(recordCategory)
                .validationProfile(validationProfile)
                .permissionScopeId(bindingConfig.getLong("permissionScopeId"))
                .recordCategorySnapshotHash(bindingConfig.getString("recordCategorySnapshotHash"))
                .requiredPolicy(requiredPolicy)
                .requiredConditionJson(bindingConfig.getString("requiredConditionJson"))
                .ownerRoleKey(ownerRoleKey)
                .archiveVisibility(archiveVisibility)
                .slotConfigSnapshotHash(bindingConfig.getString("slotConfigSnapshotHash"))
                .candidateSourceType(bindingConfig.getString("candidateSourceType"))
                .candidateSourceIds(toFrozenCandidateSourceIds(bindingConfig.get("candidateSourceIds")))
                .candidateSourceNames(toFrozenCandidateSourceNames(bindingConfig.get("candidateSourceNames")))
                .reportSort(bindingConfig.getInteger("reportSort"))
                .remark(StrUtil.blankToDefault(bindingConfig.getString("remark"), processConfig.getString("remark")))
                .build();
    }

    private String toFrozenCandidateSourceIds(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        List<?> values;
        if (rawValue instanceof JSONArray array) {
            values = array;
        } else if (rawValue instanceof List<?> list) {
            values = list;
        } else {
            String text = StrUtil.trim(String.valueOf(rawValue));
            if (StrUtil.isBlank(text)) {
                return null;
            }
            if (!text.startsWith("[")) {
                return text;
            }
            values = JSON.parseArray(text);
        }
        String joined = values.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(","));
        return StrUtil.blankToDefault(joined, null);
    }

    private String toFrozenCandidateSourceNames(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof JSONArray || rawValue instanceof List<?>) {
            return JSON.toJSONString(rawValue);
        }
        String text = StrUtil.trim(String.valueOf(rawValue));
        return StrUtil.blankToDefault(text, null);
    }

    private Map<Long, Long> resolveCurrentRouteProcessIdBySnapshot(
            Long routeId,
            Map<Long, MesProRouteProcessDO> currentRouteProcessMap,
            List<MesProRouteFlowProcessConfigDO> processConfigs) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (MesProRouteFlowProcessConfigDO config : processConfigs) {
            Long snapshotRouteProcessId = config.getRouteProcessId();
            if (snapshotRouteProcessId == null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            MesProRouteProcessDO frozenRouteProcess = currentRouteProcessMap.get(snapshotRouteProcessId);
            if (frozenRouteProcess == null) {
                frozenRouteProcess =
                        routeProcessService.resolveFrozenRouteProcess(snapshotRouteProcessId, routeId, null);
                if (frozenRouteProcess != null && frozenRouteProcess.getId() != null) {
                    currentRouteProcessMap.putIfAbsent(frozenRouteProcess.getId(), frozenRouteProcess);
                }
            }
            if (frozenRouteProcess == null
                    || frozenRouteProcess.getId() == null
                    || !Objects.equals(routeId, frozenRouteProcess.getRouteId())) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            Long previous = result.putIfAbsent(snapshotRouteProcessId, frozenRouteProcess.getId());
            if (previous != null && !Objects.equals(previous, frozenRouteProcess.getId())) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
        }
        return result;
    }

    private Map<Long, MesProRouteFlowProcessConfigDO> buildCurrentRouteProcessConfigMap(
            List<MesProRouteFlowProcessConfigDO> processConfigs,
            Map<Long, Long> currentRouteProcessIdBySnapshot) {
        Map<Long, MesProRouteFlowProcessConfigDO> result = new LinkedHashMap<>();
        for (MesProRouteFlowProcessConfigDO config : processConfigs) {
            Long currentRouteProcessId = currentRouteProcessIdBySnapshot.get(config.getRouteProcessId());
            if (currentRouteProcessId == null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            MesProRouteFlowProcessConfigDO previous = result.putIfAbsent(currentRouteProcessId, config);
            if (previous != null && !Objects.equals(previous.getId(), config.getId())) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
        }
        return result;
    }

    private void validateDynamicRouteFormBinding(MesProRouteFlowProcessBatchRecordDO record) {
        if (record == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        String ownerRoleKey = normalizeRouteOwnerRoleKey(record.getOwnerRoleKey(), FORM_SLOT_MAIN);
        String recordCategory = normalizeRouteRecordCategory(record.getRecordCategory(), FORM_SLOT_MAIN,
                record.getOwnerRoleKey());
        String validationProfile = normalizeRouteValidationProfile(record.getValidationProfile(), recordCategory);
        String requiredPolicy = normalizeRouteRequiredPolicy(record.getRequiredPolicy());
        String archiveVisibility = normalizeRouteArchiveVisibility(record.getArchiveVisibility());
        if (record.getFormTemplateId() == null
                || StrUtil.isBlank(record.getFormBindingKey())
                || StrUtil.isBlank(record.getFormTemplateNameSnapshot())
                || record.getReportSort() == null || record.getReportSort() <= 0
                || StrUtil.isBlank(recordCategory) || !ROUTE_RECORD_CATEGORIES.contains(recordCategory)
                || StrUtil.isBlank(validationProfile) || !ROUTE_VALIDATION_PROFILES.contains(validationProfile)
                || StrUtil.isBlank(requiredPolicy) || !ROUTE_REQUIRED_POLICIES.contains(requiredPolicy)
                || StrUtil.isBlank(ownerRoleKey)
                || StrUtil.isBlank(archiveVisibility) || !ROUTE_ARCHIVE_VISIBILITIES.contains(archiveVisibility)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        String instanceScope = resolveInstanceScope(record.getInstanceScope());
        if (INSTANCE_SCOPE_BATCH_SHARED.equals(instanceScope)) {
            if (StrUtil.isBlank(record.getSharedFormKey()) || StrUtil.isBlank(record.getFillableScopeJson())) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
            validateFillableScopeJson(record.getFillableScopeJson());
        }
    }

    private String normalizeRouteRecordCategory(String recordCategory, String formSlotType, String ownerRoleKey) {
        String category = StrUtil.trim(recordCategory);
        if (StrUtil.isBlank(category)
                || isExtraRouteFormSlot(formSlotType)
                && RECORD_CATEGORY_BATCH.equals(category)
                && (StrUtil.isBlank(ownerRoleKey) || OWNER_ROLE_PRODUCTION.equals(ownerRoleKey))) {
            return defaultRouteRecordCategory(formSlotType);
        }
        return category;
    }

    private String normalizeRouteValidationProfile(String validationProfile, String recordCategory) {
        String profile = StrUtil.trim(validationProfile);
        String expectedProfile = defaultRouteValidationProfile(recordCategory);
        if (StrUtil.isBlank(profile)
                || RECORD_CATEGORY_INTERNAL.equals(recordCategory) && VALIDATION_PROFILE_BATCH.equals(profile)) {
            return expectedProfile;
        }
        return profile;
    }

    private String normalizeRouteRequiredPolicy(String requiredPolicy) {
        return StrUtil.blankToDefault(StrUtil.trim(requiredPolicy), REQUIRED_POLICY_REQUIRED);
    }

    private String normalizeRouteOwnerRoleKey(String ownerRoleKey, String formSlotType) {
        String roleKey = StrUtil.trim(ownerRoleKey);
        if (StrUtil.isBlank(roleKey)
                || isExtraRouteFormSlot(formSlotType) && OWNER_ROLE_PRODUCTION.equals(roleKey)) {
            return defaultRouteOwnerRoleKey(formSlotType);
        }
        return roleKey;
    }

    private String normalizeRouteArchiveVisibility(String archiveVisibility) {
        return StrUtil.blankToDefault(StrUtil.trim(archiveVisibility), ARCHIVE_VISIBILITY_FINAL_DHR);
    }

    private String defaultRouteRecordCategory(String formSlotType) {
        return isExtraRouteFormSlot(formSlotType) ? RECORD_CATEGORY_INTERNAL : RECORD_CATEGORY_BATCH;
    }

    private String defaultRouteValidationProfile(String recordCategory) {
        return RECORD_CATEGORY_INTERNAL.equals(recordCategory) ? VALIDATION_PROFILE_INTERNAL : VALIDATION_PROFILE_BATCH;
    }

    private String defaultRouteOwnerRoleKey(String formSlotType) {
        if (SLOT_TYPE_PROCESS_INSPECTION.equals(formSlotType)) {
            return OWNER_ROLE_QUALITY;
        }
        if (SLOT_TYPE_PARAMETER_RECORD.equals(formSlotType)) {
            return OWNER_ROLE_EQUIPMENT;
        }
        return OWNER_ROLE_PRODUCTION;
    }

    private boolean isExtraRouteFormSlot(String formSlotType) {
        return !FORM_SLOT_MAIN.equals(formSlotType);
    }

    private String resolveInstanceScope(String instanceScope) {
        String scope = StrUtil.blankToDefault(StrUtil.trim(instanceScope), INSTANCE_SCOPE_PROCESS);
        if (!ROUTE_INSTANCE_SCOPES.contains(scope)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return scope;
    }

    private boolean isBatchSharedTask(MesProEdhrBatchExecutionTaskDO task) {
        return task != null && INSTANCE_SCOPE_BATCH_SHARED.equals(resolveInstanceScope(task.getInstanceScope()));
    }

    private void validateFillableScopeJson(String fillableScopeJson) {
        try {
            Object parsed = JSON.parse(fillableScopeJson);
            if (!JSON.toJSONString(parsed).contains("\"sourceTableIndex\"")) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
            }
        } catch (RuntimeException ex) {
            if (ex instanceof cn.iocoder.yudao.framework.common.exception.ServiceException) {
                throw ex;
            }
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
    }

    private Map<Long, Long> buildRouteProcessPredecessorMap(
            Long routeId, List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> routeProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Set<Long>> outgoingMap = new LinkedHashMap<>();
        Map<Long, Set<Long>> incomingMap = new LinkedHashMap<>();
        routeProcessIds.forEach(id -> {
            outgoingMap.put(id, new LinkedHashSet<>());
            incomingMap.put(id, new LinkedHashSet<>());
        });
        Set<String> seenEdges = new LinkedHashSet<>();
        for (MesProRouteProcessFlowEdgeDO edge : routeProcessFlowEdgeMapper.selectListByRouteId(routeId)) {
            Long sourceRouteProcessId = edge.getSourceRouteProcessId();
            Long targetRouteProcessId = edge.getTargetRouteProcessId();
            boolean sourceIncluded = routeProcessIds.contains(sourceRouteProcessId);
            boolean targetIncluded = routeProcessIds.contains(targetRouteProcessId);
            if (!sourceIncluded || !targetIncluded) {
                continue;
            }
            if (Objects.equals(sourceRouteProcessId, targetRouteProcessId)
                    || !seenEdges.add(sourceRouteProcessId + "->" + targetRouteProcessId)) {
                throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
            }
            outgoingMap.get(sourceRouteProcessId).add(targetRouteProcessId);
            incomingMap.get(targetRouteProcessId).add(sourceRouteProcessId);
        }
        boolean hasIsolatedRouteProcess = routeProcessIds.size() > 1 && routeProcessIds.stream()
                .anyMatch(id -> incomingMap.get(id).isEmpty() && outgoingMap.get(id).isEmpty());
        if (hasIsolatedRouteProcess || hasRouteProcessCycle(routeProcessIds, outgoingMap)) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        Set<Long> rootRouteProcessIds = routeProcessIds.stream()
                .filter(id -> incomingMap.get(id).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (rootRouteProcessIds.isEmpty()) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        Set<Long> reachableRouteProcessIds = new LinkedHashSet<>();
        rootRouteProcessIds.forEach(rootRouteProcessId ->
                reachableRouteProcessIds.addAll(reachableRouteProcessIds(rootRouteProcessId, outgoingMap)));
        if (reachableRouteProcessIds.size() != routeProcessIds.size()) {
            throw exception(PRO_ROUTE_PROCESS_FLOW_INVALID);
        }
        Map<Long, Long> predecessorMap = new LinkedHashMap<>();
        incomingMap.forEach((routeProcessId, predecessorIds) -> {
            if (predecessorIds.size() == 1) {
                predecessorMap.put(routeProcessId, predecessorIds.iterator().next());
            }
        });
        return predecessorMap;
    }

    private boolean hasRouteProcessCycle(Set<Long> routeProcessIds, Map<Long, Set<Long>> outgoingMap) {
        Set<Long> visiting = new LinkedHashSet<>();
        Set<Long> visited = new LinkedHashSet<>();
        for (Long routeProcessId : routeProcessIds) {
            if (hasRouteProcessCycle(routeProcessId, outgoingMap, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRouteProcessCycle(Long routeProcessId, Map<Long, Set<Long>> outgoingMap,
                                         Set<Long> visiting, Set<Long> visited) {
        if (visited.contains(routeProcessId)) {
            return false;
        }
        if (!visiting.add(routeProcessId)) {
            return true;
        }
        for (Long targetRouteProcessId : outgoingMap.getOrDefault(routeProcessId, Set.of())) {
            if (hasRouteProcessCycle(targetRouteProcessId, outgoingMap, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(routeProcessId);
        visited.add(routeProcessId);
        return false;
    }

    private Set<Long> reachableRouteProcessIds(Long rootRouteProcessId, Map<Long, Set<Long>> outgoingMap) {
        Set<Long> visited = new LinkedHashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        visited.add(rootRouteProcessId);
        queue.add(rootRouteProcessId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long target : outgoingMap.getOrDefault(current, Set.of())) {
                if (visited.add(target)) {
                    queue.add(target);
                }
            }
        }
        return visited;
    }

    private boolean hasBatchFlowConfigContext(Long routeId, String batchUseType) {
        MesProRouteFlowConfigDO flowConfig = routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, batchUseType);
        return flowConfig == null || MesProRouteFlowContextMatcher.isFlowContext(flowConfig, routeId, batchUseType);
    }

    private boolean isOwnedByEnabledProcessConfig(
            MesProRouteFlowProcessBatchRecordDO record,
            Map<Long, MesProRouteFlowProcessConfigDO> enabledConfigMap,
            String batchUseType) {
        return record != null && isOwnedByProcessConfig(
                record, enabledConfigMap.get(record.getRouteFlowProcessConfigId()), batchUseType);
    }

    private boolean isOwnedByProcessConfig(MesProRouteFlowProcessBatchRecordDO record,
                                           MesProRouteFlowProcessConfigDO processConfig,
                                           String batchUseType) {
        return record != null && processConfig != null
                && Objects.equals(batchUseType, record.getUseType())
                && Objects.equals(batchUseType, processConfig.getUseType())
                && Objects.equals(record.getRouteFlowProcessConfigId(), processConfig.getId())
                && Objects.equals(record.getRouteId(), processConfig.getRouteId())
                && Objects.equals(record.getRouteProcessId(), processConfig.getRouteProcessId());
    }

    private String resolveBatchExecutionMode(String executionMode) {
        String normalized = StrUtil.trim(executionMode);
        if (Objects.equals(EXECUTION_MODE_SEQUENTIAL, normalized)) {
            return EXECUTION_MODE_SEQUENTIAL;
        }
        if (Objects.equals(EXECUTION_MODE_PARALLEL, normalized)) {
            return EXECUTION_MODE_PARALLEL;
        }
        throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
    }

    private void syncBatchStatus(MesProEdhrBatchExecutionDO batch) {
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        int approved = 0;
        int blocked = 0;
        int requiredTotal = 0;
        boolean anyStarted = false;
        boolean anyReworkRequired = false;
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            boolean requiredTask = !Boolean.FALSE.equals(task.getRequiredFlag());
            if (requiredTask) {
                requiredTotal++;
            }
            if (!Objects.equals(task.getStatus(), TASK_STATUS_SKIPPED) && task.getExecutionId() != null) {
                MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
                if (execution != null) {
                    task.setStatus(mapExecutionStatus(execution));
                    if (Objects.equals(task.getStatus(), TASK_STATUS_SUBMITTED)) {
                        task.setSubmittedAt(execution.getSubmittedAt());
                    }
                    if (Objects.equals(task.getStatus(), TASK_STATUS_APPROVED)) {
                        task.setApprovedAt(execution.getApprovedAt());
                    }
                    batchTaskMapper.updateById(task);
                }
            }
            if (requiredTask && isTaskApproved(task)) {
                approved++;
            }
            if (requiredTask && Objects.equals(task.getStatus(), TASK_STATUS_BLOCKED)) {
                blocked++;
            }
            if (requiredTask && !Objects.equals(task.getStatus(), TASK_STATUS_WAITING)) {
                anyStarted = true;
            }
            if (requiredTask && Objects.equals(task.getStatus(), TASK_STATUS_REWORK_REQUIRED)) {
                anyReworkRequired = true;
            }
        }
        int status = blocked > 0 ? BATCH_STATUS_IN_PROGRESS
                : anyReworkRequired ? BATCH_STATUS_REWORK_REQUIRED
                : approved == requiredTotal && requiredTotal > 0 ? BATCH_STATUS_READY_TO_CLOSE
                : anyStarted ? BATCH_STATUS_IN_PROGRESS : BATCH_STATUS_CREATED;
        if (Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)) {
            status = batch.getStatus();
        }
        batch.setTaskTotal(tasks.size())
                .setTaskApprovedCount(approved)
                .setBlockedCount(blocked)
                .setStatus(status);
        batchExecutionMapper.updateById(batch);
    }

    private Map<Long, TaskGate> buildTaskGateMap(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        Map<Long, TaskGate> result = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            result.put(task.getId(), resolveTaskGate(task, tasks));
        }
        return result;
    }

    private TaskGate resolveTaskGate(MesProEdhrBatchExecutionTaskDO task,
                                     List<MesProEdhrBatchExecutionTaskDO> allTasks) {
        if (Objects.equals(task.getStatus(), TASK_STATUS_BLOCKED)
                || (Boolean.FALSE.equals(task.getRequiredFlag()) && !isOptionalRouteFormTask(task))
                || (isRouteForm(task) && StrUtil.isBlank(task.getBatchRecordReportId()))) {
            return new TaskGate(false, StrUtil.blankToDefault(task.getBlockerMessage(), "批记录任务被阻塞"));
        }
        boolean previousProcessesApproved;
        if (isRouteForm(task)) {
            if (task.getPredecessorRouteProcessId() == null && !Boolean.TRUE.equals(task.getRootProcessFlag())) {
                return new TaskGate(false, "工序缺少直接前置关系快照");
            }
            if (Objects.equals(EXECUTION_MODE_SEQUENTIAL, task.getExecutionMode())
                    && !isOptionalRouteFormTask(task)) {
                List<MesProEdhrBatchExecutionTaskDO> previousSameProcessTasks = allTasks.stream()
                        .filter(candidate -> Objects.equals(candidate.getRouteProcessId(), task.getRouteProcessId()))
                        .filter(candidate -> !Objects.equals(candidate.getId(), task.getId()))
                        .filter(candidate -> !Boolean.FALSE.equals(candidate.getRequiredFlag()))
                        .filter(this::isRouteForm)
                        .filter(candidate -> compareBatchRecordOrder(candidate, task) < 0)
                        .toList();
                if (previousSameProcessTasks.stream().anyMatch(candidate -> !isTaskApproved(candidate))) {
                    return new TaskGate(false, "前一张批记录未填写完成");
                }
            }
            if (task.getPredecessorRouteProcessId() == null) {
                previousProcessesApproved = true;
            } else {
                List<MesProEdhrBatchExecutionTaskDO> predecessorTasks = allTasks.stream()
                        .filter(candidate -> Objects.equals(
                                candidate.getRouteProcessId(), task.getPredecessorRouteProcessId()))
                        .filter(candidate -> !Boolean.FALSE.equals(candidate.getRequiredFlag()))
                        .filter(this::isRouteForm)
                        .toList();
                previousProcessesApproved = !predecessorTasks.isEmpty()
                        && predecessorTasks.stream().allMatch(this::isTaskApproved);
            }
        } else {
            previousProcessesApproved = allTasks.stream()
                    .filter(this::isRouteForm)
                    .filter(candidate -> !Boolean.FALSE.equals(candidate.getRequiredFlag()))
                    .filter(candidate -> isRouteFormBeforeOrAtSpecialNode(candidate, task))
                    .allMatch(this::isTaskApproved);
        }
        if (!previousProcessesApproved) {
            return new TaskGate(false, isRouteForm(task)
                    ? "直接前置工序批记录未全部填写完成"
                    : "请先完成全部必需工序记录");
        }
        return new TaskGate(true, null);
    }

    private boolean isRouteFormBeforeOrAtSpecialNode(MesProEdhrBatchExecutionTaskDO routeFormTask,
                                                     MesProEdhrBatchExecutionTaskDO specialTask) {
        Integer specialSort = specialTask.getRouteProcessSort();
        if (specialSort == null) {
            return true;
        }
        Integer routeSort = routeFormTask.getRouteProcessSort();
        return routeSort == null || routeSort <= specialSort;
    }

    private boolean isTaskApproved(MesProEdhrBatchExecutionTaskDO task) {
        if (Objects.equals(task.getStatus(), TASK_STATUS_APPROVED)) {
            return true;
        }
        if (!Objects.equals(task.getStatus(), TASK_STATUS_SKIPPED)) {
            return false;
        }
        return !"SKIPPABLE_CONTROLLED".equals(task.getRequiredPolicy())
                || (task.getSkippedBy() != null && task.getSkippedAt() != null);
    }

    private boolean isRouteForm(MesProEdhrBatchExecutionTaskDO task) {
        return NODE_TYPE_ROUTE_FORM.equals(resolveNodeType(task));
    }

    private boolean isSpecialNode(MesProEdhrBatchExecutionTaskDO task) {
        return !isRouteForm(task);
    }

    private String resolveNodeType(MesProEdhrBatchExecutionTaskDO task) {
        if (StrUtil.isNotBlank(task.getNodeType())) {
            return task.getNodeType();
        }
        return StrUtil.isBlank(task.getBatchRecordReportId()) ? task.getProcessCode() : NODE_TYPE_ROUTE_FORM;
    }

    private int compareRouteProcessOrder(MesProEdhrBatchExecutionTaskDO left,
                                         MesProEdhrBatchExecutionTaskDO right) {
        int sortCompare = Integer.compare(safeSort(left.getRouteProcessSort()), safeSort(right.getRouteProcessSort()));
        if (sortCompare != 0) {
            return sortCompare;
        }
        return Long.compare(left.getRouteProcessId() == null ? Long.MAX_VALUE : left.getRouteProcessId(),
                right.getRouteProcessId() == null ? Long.MAX_VALUE : right.getRouteProcessId());
    }

    private int compareBatchRecordOrder(MesProEdhrBatchExecutionTaskDO left,
                                        MesProEdhrBatchExecutionTaskDO right) {
        int sortCompare = Integer.compare(safeSort(left.getBatchRecordSort()), safeSort(right.getBatchRecordSort()));
        if (sortCompare != 0) {
            return sortCompare;
        }
        return Long.compare(left.getId() == null ? Long.MAX_VALUE : left.getId(),
                right.getId() == null ? Long.MAX_VALUE : right.getId());
    }

    private int safeSort(Integer sort) {
        return sort == null ? Integer.MAX_VALUE : sort;
    }

    private List<String> collectCloseBlockers(Long batchExecutionId) {
        List<String> blockers = new ArrayList<>();
        List<MesProEdhrRecordChangeEventDO> openChangeEvents = recordChangeEventMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                        .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batchExecutionId)
                        .in(MesProEdhrRecordChangeEventDO::getChangeStatus, OPEN_RECORD_CHANGE_STATUSES)
                        .orderByAsc(MesProEdhrRecordChangeEventDO::getId));
        for (MesProEdhrRecordChangeEventDO changeEvent : openChangeEvents) {
            blockers.add("存在待处理EDHR变更事件: " + changeEvent.getChangeType()
                    + "/" + changeEvent.getChangeStatus()
                    + "/" + changeEvent.getChangeCode());
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        if (tasks.isEmpty()) {
            blockers.add("批次没有工序任务");
            return blockers;
        }
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (Boolean.FALSE.equals(task.getRequiredFlag())) {
                continue;
            }
            if (Objects.equals(task.getStatus(), TASK_STATUS_BLOCKED)) {
                blockers.add(task.getProcessName() + ": " + task.getBlockerMessage());
                continue;
            }
            if (Objects.equals(task.getStatus(), TASK_STATUS_REWORK_REQUIRED)) {
                blockers.add(task.getProcessName() + ": 需返工修订");
                continue;
            }
            if (isSpecialNode(task)) {
                if (!isTaskApproved(task)) {
                    blockers.add(task.getProcessName() + ": 未完成或未跳过");
                }
                continue;
            }
            if (task.getExecutionId() == null) {
                blockers.add(task.getProcessName() + ": 未打开电子批记录");
                continue;
            }
            MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
            if (execution == null || !COMPLETED_FORM_EXECUTION_STATUSES.contains(execution.getStatus())
                    || !Objects.equals(task.getStatus(), TASK_STATUS_APPROVED)) {
                blockers.add(task.getProcessName() + ": 未填写完成");
                continue;
            }
            if (StrUtil.isBlank(execution.getCellValuesHash()) || StrUtil.isBlank(execution.getFieldAuditHeadHash())
                    || !"VERIFIED".equals(execution.getDomainTraceStatus())) {
                blockers.add(task.getProcessName() + ": 审计链或主数据追溯未验证");
                continue;
            }
            Set<String> actions = executionSignatureMapper.selectListByExecutionId(task.getExecutionId()).stream()
                    .map(MesProBatchRecordExecutionSignatureDO::getActionType)
                    .collect(Collectors.toSet());
            if (!actions.containsAll(REQUIRED_FORM_SIGNATURES)) {
                blockers.add(task.getProcessName() + ": 缺少提交签名");
            }
            List<MesProBatchRecordExecutionAttachmentDO> attachments =
                    attachmentMapper.selectListByExecutionId(task.getExecutionId());
            MesProBatchRecordAttachmentRuleSupport.collectMissingRequiredAttachments(
                            execution.getExecutionSnapshotJson(), attachments)
                    .forEach(blocker -> blockers.add(task.getProcessName() + ": " + blocker));
        }
        return blockers;
    }

    private int mapExecutionStatus(MesProBatchRecordExecutionDO execution) {
        if (execution == null || execution.getStatus() == null) {
            return TASK_STATUS_DRAFT;
        }
        if (Objects.equals(execution.getStatus(), 2)
                || (Objects.equals(execution.getStatus(), 0) && execution.getSourceRejectedExecutionId() != null)) {
            return TASK_STATUS_REWORK_REQUIRED;
        }
        return switch (execution.getStatus()) {
            case 1 -> TASK_STATUS_SUBMITTED;
            case 3 -> TASK_STATUS_APPROVED;
            case 4 -> TASK_STATUS_APPROVED;
            default -> TASK_STATUS_DRAFT;
        };
    }

    private EdhrBatchExecutionRespVO toResp(MesProEdhrBatchExecutionDO batch) {
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(batch.getId());
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        Map<Long, TaskGate> taskGateMap = buildTaskGateMap(tasks);
        Map<Long, MesProEdhrWorkTaskDO> fillableWorkTaskMap = buildFillableWorkTaskMap(batch.getId());
        Map<Long, MesProEdhrProcessFormPermissionRuleDO> fillableProcessFormRuleMap =
                buildFillableProcessFormRuleMap(tasks, fillableWorkTaskMap);
        Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> fillableRuleMap =
                buildFillableRuleMap(tasks, fillableWorkTaskMap, fillableProcessFormRuleMap);
        Map<Long, List<Long>> routeBindingFillableUserIdsMap = buildRouteBindingFillableUserIdsMap(
                tasks, fillableWorkTaskMap, fillableProcessFormRuleMap, fillableRuleMap);
        Map<Long, AdminUserRespDTO> fillableUserMap = buildFillableUserMap(
                fillableWorkTaskMap.values(), fillableProcessFormRuleMap.values(), fillableRuleMap.values(),
                routeBindingFillableUserIdsMap.values());
        Long currentUserId = currentUserId();
        Map<Long, String> batchRecordVersionNoMap = buildBatchRecordVersionNoMap(tasks);
        Map<Long, List<MesProEdhrWorkTaskDO>> activeWorkTasksByBatchTask =
                workTaskMapper.selectActiveListByBatchExecutionId(batch.getId()).stream()
                        .filter(workTask -> workTask.getBatchTaskId() != null)
                        .collect(Collectors.groupingBy(MesProEdhrWorkTaskDO::getBatchTaskId));
        MesProEdhrWorkTaskAssignmentRuleDO closeRule = latest.getRouteId() == null ? null
                : workTaskAssignmentRuleMapper.selectEnabledByScopeAndType(RULE_SCOPE_TYPE_ROUTE,
                latest.getRouteId(), WORK_TASK_TYPE_CLOSE);
        boolean goldenFingerActionBypass = hasGoldenFingerActionBypass(currentUserId);
        List<String> blockers = goldenFingerActionBypass
                || Objects.equals(latest.getStatus(), BATCH_STATUS_CLOSED)
                || Objects.equals(latest.getStatus(), BATCH_STATUS_ARCHIVED) ? List.of() : collectCloseBlockers(batch.getId());
        var releaseTransaction = releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        if (!goldenFingerActionBypass
                && blockers.isEmpty()
                && !Objects.equals(latest.getStatus(), BATCH_STATUS_CLOSED)
                && !Objects.equals(latest.getStatus(), BATCH_STATUS_ARCHIVED)
                && !isReleasePrecheckPassed(releaseTransaction)) {
            blockers = List.of("放行预检未通过，需先执行并通过放行预检");
        }
        String mainStage = batchStageResolver.resolveMainStageCode(latest, releaseTransaction);
        MesProEdhrBatchExecutionTaskDO currentProcessTask =
                batchExecutionVisibilityService.resolveCurrentProcessTask(tasks);
        Map<String, List<EdhrBatchExecutionRespVO.CurrentProcessFiller>> currentProcessFillerMap =
                batchExecutionVisibilityService.resolveCurrentProcessFillerMap(currentProcessTask);
        MesProEdhrRecordChangeEventDO pendingVoidChange = selectPendingBatchVoidChange(latest.getId());
        boolean pendingVoid = pendingVoidChange != null;
        boolean voidedTerminal = Objects.equals(latest.getStatus(), BATCH_STATUS_VOIDED);
        boolean pendingReleaseApproval = isReleasePendingApproval(releaseTransaction);
        String actionLockReason = goldenFingerActionBypass ? null : pendingVoid ? PENDING_VOID_ACTION_LOCK_REASON
                : pendingReleaseApproval ? PENDING_RELEASE_ACTION_LOCK_REASON
                : voidedTerminal ? VOIDED_ACTION_LOCK_REASON : null;
        return new EdhrBatchExecutionRespVO()
                .setId(latest.getId())
                .setBatchExecutionCode(latest.getBatchExecutionCode())
                .setWorkOrderId(latest.getWorkOrderId())
                .setWorkOrderCode(latest.getWorkOrderCode())
                .setBatchCode(latest.getBatchCode())
                .setCreateTime(latest.getCreateTime())
                .setUpdateTime(latest.getUpdateTime())
                .setAttemptNo(latest.getAttemptNo())
                .setSourceRejectedBatchExecutionId(latest.getSourceRejectedBatchExecutionId())
                .setSupersededByBatchExecutionId(latest.getSupersededByBatchExecutionId())
                .setReexecutedByChangeEventId(latest.getReexecutedByChangeEventId())
                .setProductId(latest.getProductId())
                .setProductCode(latest.getProductCode())
                .setProductName(latest.getProductName())
                .setRouteId(latest.getRouteId())
                .setRouteVersionId(latest.getRouteVersionId())
                .setRouteVersionNo(latest.getRouteVersionNo())
                .setRouteCode(latest.getRouteCode())
                .setRouteName(latest.getRouteName())
                .setCurrentProcessRouteProcessId(currentProcessTask == null ? null : currentProcessTask.getRouteProcessId())
                .setCurrentProcessCode(currentProcessTask == null ? null : currentProcessTask.getProcessCode())
                .setCurrentProcessName(currentProcessTask == null ? null : currentProcessTask.getProcessName())
                .setCurrentProcessProductionFillers(
                        currentProcessFillerMap.getOrDefault(PROCESS_RULE_TYPE_FILL, List.of()))
                .setCurrentProcessEquipmentFillers(
                        currentProcessFillerMap.getOrDefault(PROCESS_RULE_TYPE_EQUIPMENT_FILL, List.of()))
                .setCurrentProcessQualityFillers(
                        currentProcessFillerMap.getOrDefault(PROCESS_RULE_TYPE_QUALITY_FILL, List.of()))
                .setStatus(latest.getStatus())
                .setTaskTotal(latest.getTaskTotal())
                .setTaskApprovedCount(latest.getTaskApprovedCount())
                .setBlockedCount(latest.getBlockedCount())
                .setMainStage(mainStage)
                .setMainStageLabel(batchStageResolver.resolveMainStageDisplayLabel(mainStage))
                .setStageOwnerRole(batchStageResolver.resolveStageOwnerRole(mainStage))
                .setStageBlockers(batchStageResolver.resolveStageBlockers(latest, releaseTransaction))
                .setCanClose(actionLockReason == null
                        && (goldenFingerActionBypass || isCurrentUserCloseOwner(closeRule, currentUserId))
                        && blockers.isEmpty() && !tasks.isEmpty()
                        && !Objects.equals(latest.getStatus(), BATCH_STATUS_CLOSED)
                        && !Objects.equals(latest.getStatus(), BATCH_STATUS_ARCHIVED))
                .setCanArchive(actionLockReason == null && (Objects.equals(latest.getStatus(), BATCH_STATUS_CLOSED)
                        || Objects.equals(latest.getStatus(), BATCH_STATUS_ARCHIVED))
                )
                .setCloseBlockers(blockers)
                .setReleaseActionLocked(!goldenFingerActionBypass && pendingReleaseApproval)
                .setReleaseActionLockReason(!goldenFingerActionBypass && pendingReleaseApproval
                        ? PENDING_RELEASE_ACTION_LOCK_REASON : null)
                .setTasks(tasks.stream()
                        .map(task -> toTaskResp(task, taskGateMap.get(task.getId()),
                                fillableWorkTaskMap.get(task.getId()),
                                fillableProcessFormRuleMap.get(task.getId()), fillableRuleMap.get(task.getId()),
                                routeBindingFillableUserIdsMap.get(task.getId()), fillableUserMap,
                                batchRecordVersionNoMap,
                                activeWorkTasksByBatchTask.getOrDefault(task.getId(), List.of()), currentUserId,
                                actionLockReason,
                                closeRule))
                        .toList())
                .setPendingVoidChangeEventId(pendingVoidChange == null ? null : pendingVoidChange.getId())
                .setPendingVoidChangeCode(pendingVoidChange == null ? null : pendingVoidChange.getChangeCode())
                .setPendingVoidChangeStatus(pendingVoidChange == null ? null : pendingVoidChange.getChangeStatus())
                .setPendingVoidProcessInstanceId(pendingVoidChange == null ? null : pendingVoidChange.getBpmProcessInstanceId())
                .setPendingVoidRequestedBy(pendingVoidChange == null ? null : pendingVoidChange.getRequestedBy())
                .setPendingVoidRequestedAt(pendingVoidChange == null ? null : pendingVoidChange.getRequestedAt())
                .setCanWithdrawVoidRequest(pendingVoid
                        && Objects.equals(pendingVoidChange.getRequestedBy(), currentUserId)
                        && StrUtil.isNotBlank(pendingVoidChange.getBpmProcessInstanceId()))
                .setClosedBy(latest.getClosedBy())
                .setClosedAt(latest.getClosedAt())
                .setCloseSignatureId(latest.getCloseSignatureId())
                .setRejectSignatureId(latest.getRejectSignatureId())
                .setRejectedBy(latest.getRejectedBy())
                .setRejectedAt(latest.getRejectedAt())
                .setRejectReason(latest.getRejectReason())
                .setAggregateHash(latest.getAggregateHash());
    }

    private void requireReleaseActionUnlocked(Long batchExecutionId) {
        if (!hasGoldenFingerActionBypass()
                && isReleasePendingApproval(releaseTransactionMapper.selectByBatchExecutionId(batchExecutionId))) {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }
    }

    private void requireBatchActionUnlocked(Long batchExecutionId) {
        if (hasGoldenFingerActionBypass()) {
            return;
        }
        if (selectPendingBatchVoidChange(batchExecutionId) != null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED);
        }
        requireReleaseActionUnlocked(batchExecutionId);
    }

    private void requireReleasePrecheckPassedBeforeClose(Long batchExecutionId) {
        MesProEdhrReleaseTransactionDO releaseTransaction =
                releaseTransactionMapper.selectByBatchExecutionId(batchExecutionId);
        if (isReleasePrecheckPassed(releaseTransaction)) {
            return;
        }
        throw exception(PRO_EDHR_BATCH_EXECUTION_CLOSE_PRECHECK_REQUIRED);
    }

    private boolean isReleasePrecheckPassed(MesProEdhrReleaseTransactionDO releaseTransaction) {
        return releaseTransaction != null
                && MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED.equals(releaseTransaction.getReleaseStatus())
                && zero(releaseTransaction.getFailedCheckCount())
                && zero(releaseTransaction.getBlockingCheckCount());
    }

    private boolean zero(Integer value) {
        return value == null || value == 0;
    }

    private boolean hasGoldenFingerActionBypass() {
        return hasGoldenFingerActionBypass(currentUserId());
    }

    private boolean hasGoldenFingerActionBypass(Long userId) {
        return goldenFingerPermissionService != null
                && goldenFingerPermissionService.hasGoldenFingerPermission(userId);
    }

    private boolean isReleasePendingApproval(MesProEdhrReleaseTransactionDO releaseTransaction) {
        return releaseTransaction != null
                && MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(releaseTransaction.getReleaseStatus());
    }

    private MesProEdhrRecordChangeEventDO selectPendingBatchVoidChange(Long batchExecutionId) {
        if (batchExecutionId == null) {
            return null;
        }
        return recordChangeEventMapper.selectOne(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                        .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batchExecutionId)
                        .eq(MesProEdhrRecordChangeEventDO::getTargetScope, TARGET_SCOPE_BATCH)
                        .eq(MesProEdhrRecordChangeEventDO::getChangeType, CHANGE_TYPE_VOID)
                        .eq(MesProEdhrRecordChangeEventDO::getChangeStatus, CHANGE_STATUS_SUBMITTED)
                        .last("LIMIT 1"));
    }

    private Map<Long, String> buildBatchRecordVersionNoMap(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        Set<Long> versionIds = tasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getBatchRecordVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (versionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchRecordVersionMapper.selectBatchIds(versionIds).stream()
                .filter(version -> version.getId() != null)
                .filter(version -> StrUtil.isNotBlank(version.getVersionNo()))
                .collect(Collectors.toMap(
                        MesProBatchRecordVersionDO::getId,
                        version -> StrUtil.trim(version.getVersionNo()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private MesProEdhrBatchExecutionDO recoverMissingRouteFormTasksBeforeRead(MesProEdhrBatchExecutionDO batch) {
        if (!isActiveBatch(batch)) {
            return batch;
        }
        List<MesProEdhrBatchExecutionTaskDO> existingTasks =
                batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        if (existingTasks.stream().anyMatch(this::isRouteForm)) {
            return batch;
        }
        ensureRouteFormTasksPresent(batch);
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(batch.getId());
        syncBatchStatus(latest);
        return batchExecutionMapper.selectById(batch.getId());
    }

    private void syncIfActive(MesProEdhrBatchExecutionDO batch) {
        if (isActiveBatch(batch)) {
            ensureRouteFormTasksPresent(batch);
            syncBatchStatus(batch);
        }
    }

    private boolean isActiveBatch(MesProEdhrBatchExecutionDO batch) {
        return batch != null
                && !Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)
                && !Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)
                && !Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)
                && !Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED);
    }

    private void ensureRouteFormTasksPresent(MesProEdhrBatchExecutionDO batch) {
        List<MesProEdhrBatchExecutionTaskDO> existingTasks =
                batchTaskMapper.selectListByBatchExecutionId(batch.getId());
        if (existingTasks.stream().anyMatch(this::isRouteForm)) {
            return;
        }
        if (batch.getRouteId() == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        MesProRouteDO route = routeMapper.selectById(batch.getRouteId());
        boolean historicalDeletedRoute = false;
        if (route == null) {
            route = routeMapper.selectByIdIgnoreDeleted(batch.getRouteId());
            historicalDeletedRoute = route != null;
        }
        if (route == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS);
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(route.getId());
        if (routeProcesses.isEmpty() && historicalDeletedRoute) {
            routeProcesses = routeProcessMapper.selectListByRouteIdsIgnoreDeleted(List.of(route.getId()));
        }
        List<BatchTaskConfig> routeConfigs = resolveBatchTaskConfigs(batch, route, routeProcesses);
        List<MesProEdhrBatchExecutionTaskDO> insertedTasks = new ArrayList<>();
        for (BatchTaskConfig routeConfig : routeConfigs) {
            MesProEdhrBatchExecutionTaskDO task = toTaskDO(batch.getId(), routeConfig);
            batchTaskMapper.insert(task);
            createFormCenterInstanceForTask(batch, task);
            insertedTasks.add(task);
        }
        freezeBatchSharedExecutions(batch, insertedTasks);
        MesProEdhrBatchExecutionDO latest = batchExecutionMapper.selectById(batch.getId());
        workTaskService.createInitialFillTask(latest);
    }

    private EdhrBatchExecutionTaskRespVO toTaskResp(MesProEdhrBatchExecutionTaskDO task, TaskGate taskGate,
                                                    MesProEdhrWorkTaskDO fillableWorkTask,
                                                    MesProEdhrProcessFormPermissionRuleDO fillableProcessFormRule,
                                                    MesProEdhrWorkTaskAssignmentRuleDO fillableRule,
                                                    List<Long> routeBindingFillableUserIds,
                                                    Map<Long, AdminUserRespDTO> fillableUserMap,
                                                    Map<Long, String> batchRecordVersionNoMap,
                                                    List<MesProEdhrWorkTaskDO> activeWorkTasks,
                                                    Long currentUserId,
                                                    String actionLockReason,
                                                    MesProEdhrWorkTaskAssignmentRuleDO closeRule) {
        TaskGate resolvedGate = taskGate == null ? new TaskGate(false, "任务门禁状态缺失") : taskGate;
        TaskActionContext actionContext = resolveTaskActionContext(task, resolvedGate, activeWorkTasks,
                currentUserId, closeRule);
        if (StrUtil.isNotBlank(actionLockReason)) {
            actionContext = new TaskActionContext(actionContext.currentUserRole(), List.of(),
                    actionLockReason, actionContext.activeWorkTaskId(),
                    actionContext.activeWorkTaskType(), actionContext.actionUrl());
        }
        return new EdhrBatchExecutionTaskRespVO()
                .setId(task.getId())
                .setNodeType(resolveNodeType(task))
                .setRouteProcessId(task.getRouteProcessId())
                .setRouteProcessSort(task.getRouteProcessSort())
                .setProcessId(task.getProcessId())
                .setProcessCode(task.getProcessCode())
                .setProcessName(task.getProcessName())
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordReportName(task.getBatchRecordReportName())
                .setBatchRecordDefinitionId(task.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(task.getBatchRecordVersionId())
                .setBatchRecordVersionNo(batchRecordVersionNoMap.get(task.getBatchRecordVersionId()))
                .setBatchRecordSort(task.getBatchRecordSort())
                .setInstanceScope(resolveInstanceScope(task.getInstanceScope()))
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
                .setPermissionScopeId(task.getPermissionScopeId())
                .setRouteBindingId(task.getRouteBindingId())
                .setRouteBindingSnapshotHash(task.getRouteBindingSnapshotHash())
                .setRequiredPolicy(task.getRequiredPolicy())
                .setRequiredConditionJson(task.getRequiredConditionJson())
                .setOwnerRoleKey(task.getOwnerRoleKey())
                .setArchiveVisibility(task.getArchiveVisibility())
                .setSlotConfigSnapshotHash(task.getSlotConfigSnapshotHash())
                .setAvailable(resolvedGate.available())
                .setGateMessage(resolvedGate.message())
                .setCurrentUserRole(actionContext.currentUserRole())
                .setAllowedActions(actionContext.allowedActions())
                .setDisabledReason(actionContext.disabledReason())
                .setActiveWorkTaskId(actionContext.activeWorkTaskId())
                .setActiveWorkTaskType(actionContext.activeWorkTaskType())
                .setActiveWorkTaskActionUrl(actionContext.actionUrl())
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
                .setPendingSpecialNodeAttachments(resolvePendingSpecialNodeAttachments(task))
                .setFillableUsers(resolveFillableUsers(fillableWorkTask, fillableProcessFormRule,
                        fillableRule, routeBindingFillableUserIds, fillableUserMap));
    }

    private Map<Long, MesProEdhrWorkTaskDO> buildFillableWorkTaskMap(Long batchExecutionId) {
        return workTaskMapper.selectActiveListByBatchExecutionId(batchExecutionId).stream()
                .filter(task -> WORK_TASK_TYPE_FILL.equals(task.getTaskType())
                        || WORK_TASK_TYPE_REWORK.equals(task.getTaskType()))
                .filter(task -> task.getBatchTaskId() != null)
                .collect(Collectors.toMap(
                        MesProEdhrWorkTaskDO::getBatchTaskId,
                        task -> task,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    private Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> buildFillableRuleMap(
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            Map<Long, MesProEdhrWorkTaskDO> fillableWorkTaskMap,
            Map<Long, MesProEdhrProcessFormPermissionRuleDO> fillableProcessFormRuleMap) {
        Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> result = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getId() == null || task.getRouteProcessId() == null
                    || fillableWorkTaskMap.containsKey(task.getId())
                    || fillableProcessFormRuleMap.containsKey(task.getId()) || !isRouteForm(task)) {
                continue;
            }
            MesProEdhrWorkTaskAssignmentRuleDO rule =
                    workTaskAssignmentRuleMapper.selectEnabledByRouteProcessAndType(
                            task.getRouteProcessId(), WORK_TASK_TYPE_FILL);
            if (rule != null) {
                result.put(task.getId(), rule);
            }
        }
        return result;
    }

    private Map<Long, MesProEdhrProcessFormPermissionRuleDO> buildFillableProcessFormRuleMap(
            List<MesProEdhrBatchExecutionTaskDO> tasks, Map<Long, MesProEdhrWorkTaskDO> fillableWorkTaskMap) {
        Map<Long, MesProEdhrProcessFormPermissionRuleDO> result = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getId() == null || task.getRouteProcessId() == null
                    || fillableWorkTaskMap.containsKey(task.getId()) || !isRouteForm(task)) {
                continue;
            }
            String bindingKey = resolveProcessFormRuleBindingKey(task);
            if (StrUtil.isBlank(bindingKey)) {
                continue;
            }
            MesProEdhrProcessFormPermissionRuleDO rule =
                    processFormPermissionRuleMapper.selectEnabledFillRuleForRouteOrReport(
                            task.getRouteProcessId(), bindingKey, task.getBatchRecordVersionId());
            if (rule != null) {
                result.put(task.getId(), rule);
            }
        }
        return result;
    }

    private Map<Long, List<Long>> buildRouteBindingFillableUserIdsMap(
            List<MesProEdhrBatchExecutionTaskDO> tasks,
            Map<Long, MesProEdhrWorkTaskDO> fillableWorkTaskMap,
            Map<Long, MesProEdhrProcessFormPermissionRuleDO> fillableProcessFormRuleMap,
            Map<Long, MesProEdhrWorkTaskAssignmentRuleDO> fillableRuleMap) {
        Map<Long, Long> taskBindingIdMap = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getId() == null || task.getRouteBindingId() == null
                    || fillableWorkTaskMap.containsKey(task.getId())
                    || fillableProcessFormRuleMap.containsKey(task.getId())
                    || fillableRuleMap.containsKey(task.getId()) || !isRouteForm(task)) {
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
            List<Long> userIds = resolveRouteBindingFillableUserIds(bindingMap.get(bindingId));
            if (!userIds.isEmpty()) {
                result.put(taskId, userIds);
            }
        });
        return result;
    }

    private Map<Long, AdminUserRespDTO> buildFillableUserMap(Iterable<MesProEdhrWorkTaskDO> workTasks,
                                                             Iterable<MesProEdhrProcessFormPermissionRuleDO> processFormRules,
                                                             Iterable<MesProEdhrWorkTaskAssignmentRuleDO> rules,
                                                             Iterable<List<Long>> routeBindingUserIds) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (MesProEdhrWorkTaskDO workTask : workTasks) {
            userIds.addAll(resolveFillableUserIds(workTask));
        }
        for (MesProEdhrProcessFormPermissionRuleDO rule : processFormRules) {
            userIds.addAll(resolveFillableUserIds(rule));
        }
        for (MesProEdhrWorkTaskAssignmentRuleDO rule : rules) {
            userIds.addAll(resolveFillableUserIds(rule));
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
                "EDHR_FILLABLE_USER_MAP_REQUIRED: admin user map is required");
    }

    private List<EdhrBatchExecutionTaskRespVO.FillableUser> resolveFillableUsers(
            MesProEdhrWorkTaskDO workTask, MesProEdhrProcessFormPermissionRuleDO processFormRule,
            MesProEdhrWorkTaskAssignmentRuleDO rule, List<Long> routeBindingFillableUserIds,
            Map<Long, AdminUserRespDTO> userMap) {
        List<Long> userIds = workTask != null ? resolveFillableUserIds(workTask)
                : processFormRule != null ? resolveFillableUserIds(processFormRule)
                : rule != null ? resolveFillableUserIds(rule)
                : routeBindingFillableUserIds == null ? List.of() : routeBindingFillableUserIds;
        return userIds.stream()
                .map(userId -> new EdhrBatchExecutionTaskRespVO.FillableUser()
                        .setUserId(userId)
                        .setDisplayName(resolveFillableUserDisplayName(userMap, userId)))
                .toList();
    }

    private List<Long> resolveFillableUserIds(MesProEdhrWorkTaskDO workTask) {
        if (workTask == null) {
            return List.of();
        }
        Set<Long> userIds = new LinkedHashSet<>(
                MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(workTask.getCandidateUserSnapshot()));
        if (userIds.isEmpty() && workTask.getAssigneeUserId() != null) {
            userIds.add(workTask.getAssigneeUserId());
        }
        return List.copyOf(userIds);
    }

    private List<Long> resolveFillableUserIds(MesProEdhrProcessFormPermissionRuleDO rule) {
        if (rule == null) {
            return List.of();
        }
        MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate =
                Objects.requireNonNull(candidateResolver.resolveProcessFormRule(rule),
                        "EDHR_PROCESS_FORM_FILLABLE_CANDIDATE_REQUIRED: process form candidate is required");
        return MesProEdhrWorkTaskAuthorization.parseCandidateSnapshotUserIds(candidate.userSnapshot());
    }

    private String resolveProcessFormRuleBindingKey(MesProEdhrBatchExecutionTaskDO task) {
        if (task == null) {
            return null;
        }
        return StrUtil.blankToDefault(StrUtil.trim(task.getBatchRecordReportId()),
                StrUtil.trim(task.getFormBindingKey()));
    }

    private List<Long> resolveRouteBindingFillableUserIds(MesProRouteFlowProcessBatchRecordDO binding) {
        if (binding == null) {
            return List.of();
        }
        String sourceType = StrUtil.trim(binding.getCandidateSourceType());
        List<Long> sourceIds = parseRouteBindingCandidateSourceIds(binding.getCandidateSourceIds());
        if (StrUtil.isBlank(sourceType) && sourceIds.isEmpty()) {
            return List.of();
        }
        if (StrUtil.isBlank(sourceType) || sourceIds.isEmpty()) {
            throw new IllegalStateException("EDHR_ROUTE_FORM_FILLER_SOURCE_REQUIRED: routeBindingId="
                    + binding.getId());
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType) || CANDIDATE_SOURCE_TYPE_USERS.equals(sourceType)) {
            return sourceIds;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(sourceType) || CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(sourceType)) {
            Set<Long> userIds = Objects.requireNonNull(
                    permissionApi.getUserRoleIdListByRoleIds(new LinkedHashSet<>(sourceIds)),
                    "EDHR_ROUTE_FORM_FILLER_ROLE_USER_IDS_REQUIRED: routeBindingId=" + binding.getId());
            return userIds.stream().filter(Objects::nonNull).sorted().toList();
        }
        if (CANDIDATE_SOURCE_TYPE_DEPT.equals(sourceType) || CANDIDATE_SOURCE_TYPE_DEPT_GROUP.equals(sourceType)) {
            List<AdminUserRespDTO> users = Objects.requireNonNull(
                    adminUserApi.getUserListByDeptIds(new LinkedHashSet<>(sourceIds)),
                    "EDHR_ROUTE_FORM_FILLER_DEPT_USERS_REQUIRED: routeBindingId=" + binding.getId());
            return users.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getId() != null && CommonStatusEnum.isEnable(user.getStatus()))
                    .map(AdminUserRespDTO::getId)
                    .distinct()
                    .sorted()
                    .toList();
        }
        throw new IllegalStateException("EDHR_ROUTE_FORM_FILLER_SOURCE_INVALID: routeBindingId="
                + binding.getId() + ", sourceType=" + sourceType);
    }

    private List<Long> parseRouteBindingCandidateSourceIds(String rawIds) {
        String normalized = toFrozenCandidateSourceIds(rawIds);
        if (StrUtil.isBlank(normalized)) {
            return List.of();
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

    private List<Long> resolveFillableUserIds(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        if (rule == null) {
            return List.of();
        }
        String sourceType = StrUtil.blankToDefault(rule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
        Long sourceId = rule.getCandidateSourceId() == null ? rule.getAssigneeUserId() : rule.getCandidateSourceId();
        if (sourceId == null) {
            return List.of();
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType)) {
            return List.of(sourceId);
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(sourceType)) {
            Set<Long> userIds = Objects.requireNonNull(permissionApi.getUserRoleIdListByRoleIds(Set.of(sourceId)),
                    "EDHR_FILLABLE_ROLE_USER_IDS_REQUIRED: role user ids are required");
            return userIds.stream().filter(Objects::nonNull).sorted().toList();
        }
        if (CANDIDATE_SOURCE_TYPE_DEPT_GROUP.equals(sourceType)) {
            List<AdminUserRespDTO> users = Objects.requireNonNull(adminUserApi.getUserListByDeptIds(Set.of(sourceId)),
                    "EDHR_FILLABLE_DEPT_USERS_REQUIRED: dept users are required");
            return users.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getId() != null && CommonStatusEnum.isEnable(user.getStatus()))
                    .map(AdminUserRespDTO::getId)
                    .distinct()
                    .sorted()
                    .toList();
        }
        throw new IllegalStateException("EDHR_FILLABLE_RULE_SOURCE_INVALID: ruleId=" + rule.getId()
                + ", sourceType=" + sourceType);
    }

    private String resolveFillableUserDisplayName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        AdminUserRespDTO user = userMap.get(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return StrUtil.blankToDefault(user.getNickname(), String.valueOf(userId));
    }

    private TaskActionContext resolveTaskActionContext(MesProEdhrBatchExecutionTaskDO task, TaskGate taskGate,
                                                       List<MesProEdhrWorkTaskDO> activeWorkTasks,
                                                       Long currentUserId,
                                                       MesProEdhrWorkTaskAssignmentRuleDO closeRule) {
        MesProEdhrWorkTaskDO matchedTask = activeWorkTasks.stream()
                .filter(workTask -> isAssignedOrCandidate(workTask, currentUserId))
                .findFirst()
                .orElse(null);
        MesProEdhrWorkTaskDO visibleTask = matchedTask != null ? matchedTask
                : activeWorkTasks.stream().findFirst().orElse(null);
        if (visibleTask == null) {
            TaskActionContext specialNodeContext = resolveSpecialNodeCloseActionContext(task, taskGate,
                    closeRule, currentUserId);
            if (specialNodeContext != null) {
                return specialNodeContext;
            }
            TaskActionContext preReleaseContext = resolvePreReleaseSubmittedOrdinaryActionContext(task);
            if (preReleaseContext != null) {
                return preReleaseContext;
            }
            String disabledReason = taskGate.available() ? null : taskGate.message();
            return new TaskActionContext("UNRELATED", List.of(), disabledReason, null, null, null);
        }
        if (matchedTask == null) {
            return new TaskActionContext("UNRELATED", List.of(),
                    "当前用户不是该节点的" + roleLabelForTaskType(visibleTask.getTaskType()),
                    visibleTask.getId(), visibleTask.getTaskType(), visibleTask.getActionUrl());
        }
        if ((WORK_TASK_TYPE_FILL.equals(matchedTask.getTaskType()) || WORK_TASK_TYPE_REWORK.equals(matchedTask.getTaskType()))
                && !taskGate.available()) {
            return new TaskActionContext(roleForTaskType(matchedTask.getTaskType()), List.of(), taskGate.message(),
                    matchedTask.getId(), matchedTask.getTaskType(), matchedTask.getActionUrl());
        }
        return new TaskActionContext(roleForTaskType(matchedTask.getTaskType()),
                allowedActionsForTask(task, matchedTask.getTaskType()), null,
                matchedTask.getId(), matchedTask.getTaskType(), matchedTask.getActionUrl());
    }

    private TaskActionContext resolvePreReleaseSubmittedOrdinaryActionContext(MesProEdhrBatchExecutionTaskDO task) {
        if (task == null || task.getExecutionId() == null || !isRouteForm(task)) {
            return null;
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        MesProEdhrPreReleaseEditabilityService.MesProEdhrPreReleaseEditability editability =
                preReleaseEditabilityService.resolveSubmittedOrdinaryEditableForCurrentUser(execution);
        MesProEdhrWorkTaskDO workTask = editability.workTask();
        if (!isFillOrReworkWorkTask(workTask)) {
            return null;
        }
        if (!editability.editable()) {
            return new TaskActionContext(roleForTaskType(workTask.getTaskType()), List.of(), editability.reason(),
                    workTask.getId(), workTask.getTaskType(), workTask.getActionUrl());
        }
        return new TaskActionContext(roleForTaskType(workTask.getTaskType()),
                allowedActionsForTask(task, workTask.getTaskType()), null,
                workTask.getId(), workTask.getTaskType(), workTask.getActionUrl());
    }

    private TaskActionContext resolveSpecialNodeCloseActionContext(MesProEdhrBatchExecutionTaskDO task,
                                                                   TaskGate taskGate,
                                                                   MesProEdhrWorkTaskAssignmentRuleDO closeRule,
                                                                   Long currentUserId) {
        if (!isSpecialBatchExecutionNode(task) || !isSpecialNodePending(task)) {
            return null;
        }
        if (!isCurrentUserCloseOwner(closeRule, currentUserId)) {
            return new TaskActionContext("UNRELATED", List.of(),
                    "当前用户不是该节点的生产负责人", null, WORK_TASK_TYPE_CLOSE, null);
        }
        if (!taskGate.available()) {
            return new TaskActionContext("PRODUCTION_OWNER", List.of(), taskGate.message(),
                    null, WORK_TASK_TYPE_CLOSE, null);
        }
        return new TaskActionContext("PRODUCTION_OWNER", allowedActionsForTaskType(WORK_TASK_TYPE_CLOSE), null,
                null, WORK_TASK_TYPE_CLOSE, null);
    }

    private boolean isSpecialBatchExecutionNode(MesProEdhrBatchExecutionTaskDO task) {
        return task != null && StrUtil.isNotBlank(task.getNodeType())
                && !NODE_TYPE_ROUTE_FORM.equals(task.getNodeType());
    }

    private boolean isSpecialNodePending(MesProEdhrBatchExecutionTaskDO task) {
        return task != null
                && !Objects.equals(task.getStatus(), TASK_STATUS_APPROVED)
                && !Objects.equals(task.getStatus(), TASK_STATUS_SKIPPED);
    }

    private boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long currentUserId) {
        return MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, currentUserId);
    }

    private String roleForTaskType(String taskType) {
        if (WORK_TASK_TYPE_FILL.equals(taskType) || WORK_TASK_TYPE_REWORK.equals(taskType)) {
            return "FILLER";
        }
        if (WORK_TASK_TYPE_REVIEW.equals(taskType)) {
            return "REVIEWER";
        }
        if (WORK_TASK_TYPE_APPROVE.equals(taskType)) {
            return "APPROVER";
        }
        if (WORK_TASK_TYPE_CLOSE.equals(taskType)) {
            return "PRODUCTION_OWNER";
        }
        if (WORK_TASK_TYPE_ARCHIVE.equals(taskType)) {
            return "ARCHIVER";
        }
        return "UNRELATED";
    }

    private String roleLabelForTaskType(String taskType) {
        return switch (roleForTaskType(taskType)) {
            case "FILLER" -> "填写人";
            case "REVIEWER" -> "审核人";
            case "APPROVER" -> "批准人";
            case "PRODUCTION_OWNER" -> "生产负责人";
            case "ARCHIVER" -> "归档人";
            default -> "处理人";
        };
    }

    private List<String> allowedActionsForTaskType(String taskType) {
        if (WORK_TASK_TYPE_FILL.equals(taskType) || WORK_TASK_TYPE_REWORK.equals(taskType)) {
            return List.of("OPEN_FORM", "SAVE_FORM", "SUBMIT");
        }
        if (WORK_TASK_TYPE_REVIEW.equals(taskType)) {
            return List.of("REVIEW_APPROVE", "REVIEW_REJECT");
        }
        if (WORK_TASK_TYPE_APPROVE.equals(taskType)) {
            return List.of("APPROVE", "REJECT");
        }
        if (WORK_TASK_TYPE_CLOSE.equals(taskType)) {
            return List.of("CLOSE");
        }
        if (WORK_TASK_TYPE_ARCHIVE.equals(taskType)) {
            return List.of("ARCHIVE");
        }
        return List.of();
    }

    private List<String> allowedActionsForTask(MesProEdhrBatchExecutionTaskDO task, String taskType) {
        List<String> actions = allowedActionsForTaskType(taskType);
        if (!isOptionalRouteFormTask(task)
                || !(WORK_TASK_TYPE_FILL.equals(taskType) || WORK_TASK_TYPE_REWORK.equals(taskType))) {
            return actions;
        }
        List<String> optionalActions = new ArrayList<>(actions);
        optionalActions.add("SKIP");
        return optionalActions;
    }

    private EdhrBatchExecutionArchiveRespVO toArchiveResp(MesProEdhrBatchExecutionArchiveDO archive) {
        return new EdhrBatchExecutionArchiveRespVO()
                .setId(archive.getId())
                .setBatchExecutionId(archive.getBatchExecutionId())
                .setArtifactType(archive.getArtifactType())
                .setArchiveVersion(archive.getArchiveVersion())
                .setArchiveStatus(archive.getArchiveStatus())
                .setFileName(archive.getFileName())
                .setFileSize(archive.getFileSize())
                .setContentHash(archive.getContentHash())
                .setSourceManifestJson(archive.getSourceManifestJson())
                .setGeneratedAt(archive.getGeneratedAt());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.TaskEvent toTaskEvent(MesProEdhrBatchExecutionTaskDO task,
                                                                          TaskGate taskGate) {
        TaskGate resolvedGate = taskGate == null ? new TaskGate(false, "任务门禁状态缺失") : taskGate;
        return new EdhrBatchExecutionReviewTimelineRespVO.TaskEvent()
                .setTaskId(task.getId())
                .setRouteProcessSort(task.getRouteProcessSort())
                .setProcessCode(task.getProcessCode())
                .setProcessName(task.getProcessName())
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordReportName(task.getBatchRecordReportName())
                .setBatchRecordSort(task.getBatchRecordSort())
                .setExecutionMode(task.getExecutionMode())
                .setAvailable(resolvedGate.available())
                .setGateMessage(resolvedGate.message())
                .setExecutionId(task.getExecutionId())
                .setStatus(task.getStatus())
                .setBlockerCode(task.getBlockerCode())
                .setBlockerMessage(task.getBlockerMessage())
                .setSkippedBy(task.getSkippedBy())
                .setSkippedAt(task.getSkippedAt())
                .setSpecialPayloadJson(task.getSpecialPayloadJson())
                .setOpenedAt(task.getOpenedAt())
                .setSubmittedAt(task.getSubmittedAt())
                .setApprovedAt(task.getApprovedAt());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.DossierItem toTimelineDossierItem(
            MesProEdhrBatchDossierItemDO item) {
        return new EdhrBatchExecutionReviewTimelineRespVO.DossierItem()
                .setId(item.getId())
                .setItemType(item.getItemType())
                .setItemKey(item.getItemKey())
                .setItemName(item.getItemName())
                .setRequiredFlag(item.getRequiredFlag())
                .setItemStatus(item.getItemStatus())
                .setSourceDocType(item.getSourceDocType())
                .setSourceDocId(item.getSourceDocId())
                .setSourceDocCode(item.getSourceDocCode())
                .setSourceDocStatus(item.getSourceDocStatus())
                .setSourceDocResult(item.getSourceDocResult())
                .setSourceDocHash(item.getSourceDocHash())
                .setCompletedAt(item.getCompletedAt())
                .setVerifiedAt(item.getVerifiedAt());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord toSignatureRecord(
            MesProEdhrBatchExecutionSignatureDO signature) {
        return new EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord()
                .setId(signature.getId())
                .setActorId(signature.getActorId())
                .setActorName(signature.getActorName())
                .setActionType(signature.getActionType())
                .setSignatureMode(signature.getSignatureMode())
                .setPasswordVerified(signature.getPasswordVerified())
                .setComment(signature.getComment())
                .setAggregateHash(signature.getAggregateHash())
                .setSignedAt(signature.getSignedAt())
                .setSelectedSignedAt(signature.getSelectedSignedAt())
                .setSignatureDisplayAt(signature.getSignatureDisplayAt())
                .setSignatureTimeMode(signature.getSignatureTimeMode())
                .setSelectedTimeZone(signature.getSelectedTimeZone())
                .setSelectedTimeReason(signature.getSelectedTimeReason())
                .setSelectedTimePolicyVersion(signature.getSelectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signature.getSelectedTimeAuditHash());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.FlowEvent toFlowEvent(MesProEdhrFlowEventDO event) {
        return new EdhrBatchExecutionReviewTimelineRespVO.FlowEvent()
                .setId(event.getId())
                .setInterventionId(event.getInterventionId())
                .setTaskId(event.getTaskId())
                .setNodeKey(event.getNodeKey())
                .setEventType(event.getEventType())
                .setAction(resolveFlowEventAction(event))
                .setFromStatus(event.getFromStatus())
                .setToStatus(event.getToStatus())
                .setActorUserId(event.getActorUserId())
                .setTargetUserId(event.getTargetUserId())
                .setPermissionCode(event.getPermissionCode())
                .setPermissionDecision(event.getPermissionDecision())
                .setReason(event.getReason())
                .setSignoffEvidenceHash(event.getSignoffEvidenceHash())
                .setIntegrityCheckResult(event.getIntegrityCheckResult())
                .setEventSnapshotJson(event.getEventSnapshotJson())
                .setEvidenceHash(event.getEvidenceHash())
                .setOccurredAt(event.getOccurredAt());
    }

    private String resolveFlowEventAction(MesProEdhrFlowEventDO event) {
        if (StrUtil.isBlank(event.getEventSnapshotJson())) {
            return null;
        }
        try {
            return JSON.parseObject(event.getEventSnapshotJson()).getString("action");
        } catch (Exception ignored) {
            return null;
        }
    }

    private EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord toExecutionSignatureRecord(
            MesProBatchRecordExecutionSignatureDO signature, MesProBatchRecordExecutionDO execution) {
        return new EdhrBatchExecutionReviewTimelineRespVO.SignatureRecord()
                .setId(signature.getId())
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setActorId(signature.getActorId())
                .setActorName(signature.getActorName())
                .setActionType(signature.getActionType())
                .setSignatureMode(signature.getSignatureMode())
                .setPasswordVerified(signature.getPasswordVerified())
                .setComment(StrUtil.blankToDefault(signature.getComment(), signature.getReason()))
                .setSignedAt(signature.getSignedAt())
                .setSelectedSignedAt(signature.getSelectedSignedAt())
                .setSignatureDisplayAt(signature.getSignatureDisplayAt())
                .setSignatureTimeMode(signature.getSignatureTimeMode())
                .setSelectedTimeZone(signature.getSelectedTimeZone())
                .setSelectedTimeReason(signature.getSelectedTimeReason())
                .setSelectedTimePolicyVersion(signature.getSelectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signature.getSelectedTimeAuditHash());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview toExecutionReview(
            MesProEdhrBatchExecutionTaskDO task) {
        if (task.getExecutionId() == null) {
            return null;
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        if (execution == null) {
            return null;
        }
        List<MesProBatchRecordExecutionSignatureDO> executionSignatures =
                executionSignatureMapper.selectTimelineListByExecutionId(execution.getId());
        List<MesProBatchRecordExecutionFieldAuditBatchDO> auditBatches =
                fieldAuditBatchMapper.selectListByExecutionId(execution.getId());
        MesProBatchRecordApprovalSnapshotDO approvalSnapshot =
                approvalSnapshotMapper.selectByExecutionId(execution.getId());
        MesProBatchRecordDomainTraceSnapshotDO domainTraceSnapshot =
                domainTraceSnapshotMapper.selectLatestByExecutionId(execution.getId());
        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(execution.getId());
        return new EdhrBatchExecutionReviewTimelineRespVO.ExecutionReview()
                .setTaskId(task.getId())
                .setRouteProcessSort(task.getRouteProcessSort())
                .setProcessCode(task.getProcessCode())
                .setProcessName(task.getProcessName())
                .setBatchRecordReportId(task.getBatchRecordReportId())
                .setBatchRecordReportName(task.getBatchRecordReportName())
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setStatus(execution.getStatus())
                .setSubmittedAt(execution.getSubmittedAt())
                .setApprovedAt(execution.getApprovedAt())
                .setFormViewModel(new EdhrBatchExecutionReviewTimelineRespVO.FormViewModel()
                        .setSheetLayoutJson(execution.getSheetLayoutJson())
                        .setMetaJson(execution.getMetaJson())
                        .setExecutionSnapshotJson(execution.getExecutionSnapshotJson())
                        .setCellValuesJson(execution.getCellValuesJson())
                        .setRemark(execution.getRemark())
                        .setSignatureCellMarkers(resolveSignatureCellMarkers(execution, task)))
                .setFieldAuditSummary(toFieldAuditSummary(execution, auditBatches))
                .setSignatureSummary(toSignatureSummary(executionSignatures))
                .setSignatureRecords(executionSignatures.stream()
                        .map(signature -> toExecutionSignatureRecord(signature, execution))
                        .toList())
                .setApprovalSummary(toApprovalSummary(task, execution, approvalSnapshot, executionSignatures))
                .setDomainTraceSummary(toDomainTraceSummary(execution, domainTraceSnapshot))
                .setAttachmentCount(attachments.size())
                .setAttachmentSummaries(attachments.stream().map(this::toTimelineAttachmentSummary).toList());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.AttachmentSummary toTimelineAttachmentSummary(
            MesProBatchRecordExecutionAttachmentDO attachment) {
        return new EdhrBatchExecutionReviewTimelineRespVO.AttachmentSummary()
                .setId(attachment.getId())
                .setExecutionId(attachment.getExecutionId())
                .setBatchTaskId(attachment.getBatchTaskId())
                .setWorkTaskId(attachment.getWorkTaskId())
                .setRowIndex(attachment.getRowIndex())
                .setColumnIndex(attachment.getColumnIndex())
                .setFieldKey(attachment.getFieldKey())
                .setFieldPath(attachment.getFieldPath())
                .setFieldLabel(attachment.getFieldLabel())
                .setAttachmentType(attachment.getAttachmentType())
                .setAttachmentGroupKey(attachment.getAttachmentGroupKey())
                .setAttachmentAction(attachment.getAttachmentAction())
                .setVersionNo(attachment.getVersionNo())
                .setFileId(attachment.getFileId())
                .setFileUrl(attachment.getFileUrl())
                .setStorageConfigId(attachment.getStorageConfigId())
                .setStoragePath(attachment.getStoragePath())
                .setFileName(attachment.getFileName())
                .setContentType(attachment.getContentType())
                .setFileSize(attachment.getFileSize())
                .setSha256(attachment.getSha256())
                .setStorageRetentionHash(attachment.getStorageRetentionHash())
                .setAuditBatchId(attachment.getAuditBatchId())
                .setSignatureId(attachment.getSignatureId())
                .setAttachmentHash(attachment.getAttachmentHash())
                .setOperatorId(attachment.getOperatorId())
                .setOperatorName(attachment.getOperatorName())
                .setOperatedAt(attachment.getOperatedAt());
    }

    private List<EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker> resolveSignatureCellMarkers(
            MesProBatchRecordExecutionDO execution,
            MesProEdhrBatchExecutionTaskDO task) {
        List<EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker> snapshotMarkers =
                extractSignatureCellMarkers(execution.getExecutionSnapshotJson());
        if (!snapshotMarkers.isEmpty()) {
            return snapshotMarkers;
        }
        if (StrUtil.isBlank(task.getBatchRecordReportId())) {
            return List.of();
        }
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(task.getBatchRecordReportId());
        if (report == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NOT_EXISTS);
        }
        String reportJson = jimuReportGateway.getReportJson(report.getReportId());
        if (StrUtil.isBlank(reportJson)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    report.getReportId());
        }
        return extractSignatureCellMarkers(reportJson);
    }

    private List<EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker> extractSignatureCellMarkers(String rawJson) {
        if (StrUtil.isBlank(rawJson)) {
            return List.of();
        }
        JSONObject root = JSON.parseObject(rawJson);
        JSONObject layout = root.getJSONObject("layout");
        JSONObject rows = layout == null ? root.getJSONObject("rows") : layout.getJSONObject("rows");
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker> markers = new ArrayList<>();
        for (String rowKey : rows.keySet()) {
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(columnKey);
                JSONObject signature = cell == null ? null : cell.getJSONObject("edhrSignature");
                if (signature == null || !Boolean.TRUE.equals(signature.getBoolean("enabled"))) {
                    continue;
                }
                markers.add(new EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker()
                        .setRowIndex(Integer.valueOf(rowKey))
                        .setColumnIndex(Integer.valueOf(columnKey))
                        .setEnabled(true)
                        .setActionType(signature.getString("actionType"))
                        .setLabel(signature.getString("label"))
                        .setDisplayFormat(StrUtil.blankToDefault(signature.getString("displayFormat"),
                                DEFAULT_SIGNATURE_DISPLAY_FORMAT)));
            }
        }
        markers.sort((first, second) -> {
            int rowCompare = Integer.compare(first.getRowIndex(), second.getRowIndex());
            return rowCompare != 0 ? rowCompare : Integer.compare(first.getColumnIndex(), second.getColumnIndex());
        });
        return markers;
    }

    private EdhrBatchExecutionReviewTimelineRespVO.FieldAuditSummary toFieldAuditSummary(
            MesProBatchRecordExecutionDO execution,
            List<MesProBatchRecordExecutionFieldAuditBatchDO> auditBatches) {
        return new EdhrBatchExecutionReviewTimelineRespVO.FieldAuditSummary()
                .setBatchCount(auditBatches.size())
                .setRevision(execution.getFieldAuditRevision())
                .setLastBatchId(execution.getFieldAuditLastBatchId())
                .setHeadHash(execution.getFieldAuditHeadHash());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.SignatureSummary toSignatureSummary(
            List<MesProBatchRecordExecutionSignatureDO> signatures) {
        return new EdhrBatchExecutionReviewTimelineRespVO.SignatureSummary()
                .setTotalCount(signatures.size())
                .setFieldChangeCount((int) signatures.stream().filter(signature ->
                        "FIELD_CHANGE".equals(signature.getActionType())).count())
                .setFormReviewCount((int) signatures.stream().filter(signature ->
                        "FORM_REVIEW".equals(signature.getActionType())).count())
                .setSubmitCount((int) signatures.stream().filter(signature ->
                        "SUBMIT".equals(signature.getActionType())).count())
                .setApproveCount((int) signatures.stream().filter(signature ->
                        "APPROVE".equals(signature.getActionType())).count())
                .setLastSignedAt(signatures.isEmpty() ? null : signatures.get(signatures.size() - 1).getSignedAt());
    }

    private EdhrBatchExecutionReviewTimelineRespVO.ApprovalSummary toApprovalSummary(
            MesProEdhrBatchExecutionTaskDO task,
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordApprovalSnapshotDO approvalSnapshot,
            List<MesProBatchRecordExecutionSignatureDO> signatures) {
        EdhrBatchExecutionReviewTimelineRespVO.ApprovalRecord approvedRecord = signatures.stream()
                .filter(signature -> "APPROVE".equals(signature.getActionType()))
                .reduce((first, second) -> second)
                .map(signature -> new EdhrBatchExecutionReviewTimelineRespVO.ApprovalRecord()
                        .setExecutionId(execution.getId())
                        .setExecutionCode(execution.getExecutionCode())
                        .setProcessCode(task.getProcessCode())
                        .setProcessName(task.getProcessName())
                        .setActorName(signature.getActorName())
                        .setComment(signature.getComment())
                        .setBpmTaskId(signature.getBpmTaskId())
                        .setBpmTaskName(signature.getBpmTaskName())
                        .setApprovalResult(signature.getApprovalResult())
                        .setSignedAt(signature.getSignedAt()))
                .orElse(null);
        return new EdhrBatchExecutionReviewTimelineRespVO.ApprovalSummary()
                .setProcessInstanceId(execution.getProcessInstanceId())
                .setApprovalSnapshotStatus(approvalSnapshot == null ? null : approvalSnapshot.getApprovalStatus())
                .setCurrentBpmTaskId(approvalSnapshot == null ? null : approvalSnapshot.getCurrentBpmTaskId())
                .setApprovedRecord(approvedRecord);
    }

    private EdhrBatchExecutionReviewTimelineRespVO.DomainTraceSummary toDomainTraceSummary(
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordDomainTraceSnapshotDO snapshot) {
        return new EdhrBatchExecutionReviewTimelineRespVO.DomainTraceSummary()
                .setSnapshotId(execution.getDomainTraceSnapshotId())
                .setStatus(execution.getDomainTraceStatus())
                .setSnapshotHash(snapshot == null ? null : snapshot.getSnapshotHash())
                .setVerifiedAt(execution.getDomainTraceVerifiedAt());
    }

    private String buildArchiveManifest(MesProEdhrBatchExecutionDO batch, LocalDateTime generatedAt) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        MesProRouteDO route = batch.getRouteId() == null ? null : routeMapper.selectById(batch.getRouteId());
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batch.getId()).stream()
                .sorted(this::compareRouteProcessOrder)
                .toList();
        manifest.put("schemaVersion", PRINTABLE_ARCHIVE_SCHEMA_VERSION);
        manifest.put("batchExecutionId", batch.getId());
        manifest.put("batchCode", batch.getBatchCode());
        manifest.put("routeId", batch.getRouteId());
        manifest.put("routeCode", route == null ? null : route.getCode());
        manifest.put("routeName", route == null ? null : route.getName());
        manifest.put("aggregateHash", batch.getAggregateHash());
        manifest.put("generatedAt", generatedAt);
        manifest.put("tasks", buildArchiveTaskManifests(tasks));
        manifest.put("bodyForms", tasks.stream()
                .filter(this::isRouteForm)
                .filter(task -> task.getExecutionId() != null)
                .map(this::toPrintableBodyFormSnapshot)
                .toList());
        manifest.put("appendixSpecialNodes", tasks.stream()
                .filter(this::isSpecialNode)
                .map(this::toPrintableSpecialNodeSnapshot)
                .toList());
        manifest.put("dossierItems", dossierItemMapper.selectListByBatchExecutionId(batch.getId()).stream()
                .map(this::toArchiveDossierItemManifest)
                .toList());
        manifest.put("changeEvents", recordChangeEventMapper.selectList(
                        new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batch.getId())
                                .orderByAsc(MesProEdhrRecordChangeEventDO::getId))
                .stream()
                .map(event -> {
                    Map<String, Object> eventManifest = new LinkedHashMap<>();
                    eventManifest.put("id", event.getId());
                    eventManifest.put("changeCode", event.getChangeCode());
                    eventManifest.put("changeType", event.getChangeType());
                    eventManifest.put("changeStatus", event.getChangeStatus());
                    eventManifest.put("targetScope", event.getTargetScope());
                    eventManifest.put("executionId", event.getExecutionId());
                    eventManifest.put("previousStatus", event.getPreviousStatus());
                    eventManifest.put("newStatus", event.getNewStatus());
                    eventManifest.put("reasonCategory", event.getReasonCategory());
                    eventManifest.put("requestSignatureId", event.getRequestSignatureId());
                    eventManifest.put("approvalSignatureId", event.getApprovalSignatureId());
                    eventManifest.put("effectiveAt", event.getEffectiveAt());
                    return eventManifest;
                })
                .toList());
        return JSON.toJSONString(manifest);
    }

    private List<Map<String, Object>> buildArchiveTaskManifests(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        return tasks.stream()
                .map(task -> {
                    Map<String, Object> taskManifest = new LinkedHashMap<>();
                    taskManifest.put("taskId", task.getId());
                    taskManifest.put("nodeType", resolveNodeType(task));
                    taskManifest.put("routeProcessId", task.getRouteProcessId());
                    taskManifest.put("executionId", task.getExecutionId());
                    taskManifest.put("formSlotType", task.getFormSlotType());
                    taskManifest.put("requiredPolicy", task.getRequiredPolicy());
                    taskManifest.put("requiredConditionJson", task.getRequiredConditionJson());
                    taskManifest.put("ownerRoleKey", task.getOwnerRoleKey());
                    taskManifest.put("archiveVisibility", task.getArchiveVisibility());
                    taskManifest.put("slotConfigSnapshotHash", task.getSlotConfigSnapshotHash());
                    taskManifest.put("processCode", task.getProcessCode());
                    taskManifest.put("processName", task.getProcessName());
                    taskManifest.put("batchRecordReportName", task.getBatchRecordReportName());
                    taskManifest.put("status", task.getStatus());
                    taskManifest.put("skippedBy", task.getSkippedBy());
                    taskManifest.put("skippedAt", task.getSkippedAt());
                    taskManifest.put("specialPayloadJson", task.getSpecialPayloadJson());
                    MesProBatchRecordExecutionDO execution = task.getExecutionId() == null
                            ? null : executionMapper.selectById(task.getExecutionId());
                    taskManifest.put("executionCode", execution == null ? null : execution.getExecutionCode());
                    taskManifest.put("domainTraceStatus", execution == null ? null : execution.getDomainTraceStatus());
                    taskManifest.put("domainTraceHash", execution == null ? null : execution.getDomainTraceHash());
                    taskManifest.put("fieldAuditRevision", execution == null ? null : execution.getFieldAuditRevision());
                    taskManifest.put("fieldAuditHeadHash", execution == null ? null : execution.getFieldAuditHeadHash());
                    taskManifest.put("signatures", task.getExecutionId() == null ? List.of()
                            : executionSignatureMapper.selectListByExecutionId(task.getExecutionId()).stream()
                            .map(signature -> {
                                Map<String, Object> signatureManifest = new LinkedHashMap<>();
                                signatureManifest.put("actionType", signature.getActionType());
                                signatureManifest.put("actorName", signature.getActorName());
                                signatureManifest.put("signedAt", signature.getSignedAt());
                                signatureManifest.put("approvalResult", signature.getApprovalResult());
                                return signatureManifest;
                            })
                            .toList());
                    MesProBatchRecordApprovalSnapshotDO approvalSnapshot = task.getExecutionId() == null
                            ? null : approvalSnapshotMapper.selectByExecutionId(task.getExecutionId());
                    taskManifest.put("approvalStatus", approvalSnapshot == null ? null : approvalSnapshot.getApprovalStatus());
                    taskManifest.put("approvalSnapshotHash", approvalSnapshot == null ? null : approvalSnapshot.getSnapshotHash());
                    List<MesProBatchRecordExecutionAttachmentDO> formAttachments = task.getExecutionId() == null
                            ? List.of() : attachmentMapper.selectListByExecutionId(task.getExecutionId());
                    List<MesProBatchRecordExecutionAttachmentDO> specialAttachments =
                            task.getExecutionId() == null ? attachmentMapper.selectListByBatchTaskId(task.getId()).stream()
                                    .filter(this::isBookedSpecialNodeAttachment)
                                    .toList() : List.of();
                    List<MesProBatchRecordExecutionAttachmentDO> attachments = new ArrayList<>(formAttachments);
                    attachments.addAll(specialAttachments);
                    taskManifest.put("attachmentCount", attachments.size());
                    taskManifest.put("attachmentManifests", attachments.stream()
                            .map(this::toArchiveAttachmentManifest)
                            .toList());
                    taskManifest.put("specialAttachmentCount", specialAttachments.size());
                    taskManifest.put("specialAttachments", specialAttachments.stream()
                            .map(this::toArchiveAttachmentManifest)
                            .toList());
                    taskManifest.put("attachmentRuleSummaries", buildArchiveAttachmentRuleSummaries(task));
                    return taskManifest;
                })
                .toList();
    }

    private Map<String, Object> toPrintableBodyFormSnapshot(MesProEdhrBatchExecutionTaskDO task) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        if (execution == null) {
            throw new IllegalStateException("eDHR batch archive printable snapshot missing execution: taskId=" + task.getId());
        }
        List<MesProBatchRecordExecutionSignatureDO> executionSignatures =
                executionSignatureMapper.selectTimelineListByExecutionId(execution.getId());
        List<MesProBatchRecordExecutionFieldAuditBatchDO> auditBatches =
                fieldAuditBatchMapper.selectListByExecutionId(execution.getId());
        MesProBatchRecordApprovalSnapshotDO approvalSnapshot =
                approvalSnapshotMapper.selectByExecutionId(execution.getId());
        MesProBatchRecordDomainTraceSnapshotDO domainTraceSnapshot =
                domainTraceSnapshotMapper.selectLatestByExecutionId(execution.getId());
        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(execution.getId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("taskId", task.getId());
        snapshot.put("nodeType", resolveNodeType(task));
        snapshot.put("routeProcessId", task.getRouteProcessId());
        snapshot.put("routeProcessSort", task.getRouteProcessSort());
        snapshot.put("predecessorRouteProcessId", task.getPredecessorRouteProcessId());
        snapshot.put("rootProcessFlag", task.getRootProcessFlag());
        snapshot.put("processCode", task.getProcessCode());
        snapshot.put("processName", task.getProcessName());
        snapshot.put("batchRecordReportId", task.getBatchRecordReportId());
        snapshot.put("batchRecordReportName", task.getBatchRecordReportName());
        snapshot.put("executionId", execution.getId());
        snapshot.put("executionCode", execution.getExecutionCode());
        snapshot.put("status", execution.getStatus());
        snapshot.put("submittedAt", execution.getSubmittedAt());
        snapshot.put("approvedAt", execution.getApprovedAt());
        snapshot.put("sheetLayoutJson", execution.getSheetLayoutJson());
        snapshot.put("metaJson", execution.getMetaJson());
        snapshot.put("executionSnapshotJson", execution.getExecutionSnapshotJson());
        snapshot.put("cellValuesJson", execution.getCellValuesJson());
        snapshot.put("remark", execution.getRemark());
        snapshot.put("signatureCellMarkers", resolveArchiveSignatureCellMarkers(execution));
        snapshot.put("signatureSummary", toSignatureSummary(executionSignatures));
        snapshot.put("signatureRecords", executionSignatures.stream()
                .map(signature -> toExecutionSignatureRecord(signature, execution))
                .toList());
        snapshot.put("fieldAuditSummary", toFieldAuditSummary(execution, auditBatches));
        snapshot.put("approvalSummary", toApprovalSummary(task, execution, approvalSnapshot, executionSignatures));
        snapshot.put("domainTraceSummary", toDomainTraceSummary(execution, domainTraceSnapshot));
        snapshot.put("attachmentCount", attachments.size());
        snapshot.put("attachmentSummaries", attachments.stream()
                .map(this::toTimelineAttachmentSummary)
                .toList());
        snapshot.put("attachmentRuleSummaries", buildArchiveAttachmentRuleSummaries(task));
        return snapshot;
    }

    private List<EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker> resolveArchiveSignatureCellMarkers(
            MesProBatchRecordExecutionDO execution) {
        List<EdhrBatchExecutionReviewTimelineRespVO.SignatureCellMarker> snapshotMarkers =
                extractSignatureCellMarkers(execution.getExecutionSnapshotJson());
        if (!snapshotMarkers.isEmpty()) {
            return snapshotMarkers;
        }
        return extractSignatureCellMarkers(execution.getSheetLayoutJson());
    }

    private Map<String, Object> toPrintableSpecialNodeSnapshot(MesProEdhrBatchExecutionTaskDO task) {
        JSONObject payload = StrUtil.isBlank(task.getSpecialPayloadJson()) ? new JSONObject()
                : JSON.parseObject(task.getSpecialPayloadJson());
        List<MesProBatchRecordExecutionAttachmentDO> specialAttachments =
                attachmentMapper.selectListByBatchTaskId(task.getId()).stream()
                        .filter(this::isBookedSpecialNodeAttachment)
                        .toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("taskId", task.getId());
        snapshot.put("nodeType", resolveNodeType(task));
        snapshot.put("routeProcessId", task.getRouteProcessId());
        snapshot.put("routeProcessSort", task.getRouteProcessSort());
        snapshot.put("predecessorRouteProcessId", task.getPredecessorRouteProcessId());
        snapshot.put("rootProcessFlag", task.getRootProcessFlag());
        snapshot.put("processCode", task.getProcessCode());
        snapshot.put("processName", task.getProcessName());
        snapshot.put("status", task.getStatus());
        snapshot.put("skippedBy", task.getSkippedBy());
        snapshot.put("skippedAt", task.getSkippedAt());
        snapshot.put("approvedAt", task.getApprovedAt());
        snapshot.put("operatorId", firstNonBlank(
                payload.get("skippedBy"),
                firstNonBlank(payload.get("completedBy"), task.getSkippedBy())));
        snapshot.put("operatedAt", firstNonBlank(
                payload.get("skippedAt"),
                firstNonBlank(payload.get("completedAt"),
                        firstNonBlank(task.getSkippedAt(), task.getApprovedAt()))));
        snapshot.put("specialPayloadJson", task.getSpecialPayloadJson());
        snapshot.put("specialAttachments", specialAttachments.stream()
                .map(this::toArchiveAttachmentManifest)
                .toList());
        snapshot.put("attachmentCount", specialAttachments.size());
        return snapshot;
    }

    private List<Map<String, Object>> buildArchiveAttachmentRuleSummaries(MesProEdhrBatchExecutionTaskDO task) {
        if (task.getExecutionId() == null) {
            return List.of();
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        if (execution == null || StrUtil.isBlank(execution.getExecutionSnapshotJson())) {
            return List.of();
        }
        JSONObject snapshot = JSON.parseObject(execution.getExecutionSnapshotJson());
        List<JSONObject> fields = snapshot.getJSONArray("fields") == null
                ? List.of() : snapshot.getJSONArray("fields").toJavaList(JSONObject.class);
        return fields.stream()
                .filter(field -> field.getJSONObject("attachmentRule") != null
                        && !field.getJSONObject("attachmentRule").isEmpty())
                .map(field -> toArchiveAttachmentRuleSummary(task, field))
                .toList();
    }

    private Map<String, Object> toArchiveAttachmentRuleSummary(MesProEdhrBatchExecutionTaskDO task, JSONObject field) {
        JSONObject attachmentRule = field.getJSONObject("attachmentRule");
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("executionId", task.getExecutionId());
        summary.put("batchTaskId", task.getId());
        summary.put("fieldPath", field.getString("fieldPath"));
        summary.put("fieldKey", field.getString("fieldKey"));
        summary.put("label", field.getString("label"));
        summary.put("required", attachmentRule.getBoolean("required"));
        summary.put("minCount", attachmentRule.getInteger("minCount"));
        summary.put("maxCount", attachmentRule.getInteger("maxCount"));
        summary.put("attachmentType", attachmentRule.getString("attachmentType"));
        summary.put("groupKey", attachmentRule.getString("groupKey"));
        return summary;
    }

    private Map<String, Object> toArchiveDossierItemManifest(MesProEdhrBatchDossierItemDO item) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("id", item.getId());
        manifest.put("itemType", item.getItemType());
        manifest.put("itemKey", item.getItemKey());
        manifest.put("itemName", item.getItemName());
        manifest.put("requiredFlag", item.getRequiredFlag());
        manifest.put("itemStatus", item.getItemStatus());
        manifest.put("sourceDocType", item.getSourceDocType());
        manifest.put("sourceDocId", item.getSourceDocId());
        manifest.put("sourceDocCode", item.getSourceDocCode());
        manifest.put("sourceDocStatus", item.getSourceDocStatus());
        manifest.put("sourceDocResult", item.getSourceDocResult());
        manifest.put("sourceDocHash", item.getSourceDocHash());
        manifest.put("completedAt", item.getCompletedAt());
        manifest.put("verifiedAt", item.getVerifiedAt());
        return manifest;
    }

    private Map<String, Object> toArchiveAttachmentManifest(MesProBatchRecordExecutionAttachmentDO attachment) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("id", attachment.getId());
        manifest.put("executionId", attachment.getExecutionId());
        manifest.put("batchTaskId", attachment.getBatchTaskId());
        manifest.put("workTaskId", attachment.getWorkTaskId());
        manifest.put("rowIndex", attachment.getRowIndex());
        manifest.put("columnIndex", attachment.getColumnIndex());
        manifest.put("fieldKey", attachment.getFieldKey());
        manifest.put("fieldPath", attachment.getFieldPath());
        manifest.put("fieldLabel", attachment.getFieldLabel());
        manifest.put("attachmentType", attachment.getAttachmentType());
        manifest.put("attachmentGroupKey", attachment.getAttachmentGroupKey());
        manifest.put("attachmentAction", attachment.getAttachmentAction());
        manifest.put("versionNo", attachment.getVersionNo());
        manifest.put("fileId", attachment.getFileId());
        manifest.put("fileName", attachment.getFileName());
        manifest.put("contentType", attachment.getContentType());
        manifest.put("fileSize", attachment.getFileSize());
        manifest.put("sha256", attachment.getSha256());
        manifest.put("storageConfigId", attachment.getStorageConfigId());
        manifest.put("storagePath", attachment.getStoragePath());
        manifest.put("storageRetentionHash", attachment.getStorageRetentionHash());
        manifest.put("auditBatchId", attachment.getAuditBatchId());
        manifest.put("signatureId", attachment.getSignatureId());
        manifest.put("previousAttachmentHash", attachment.getPreviousAttachmentHash());
        manifest.put("attachmentHash", attachment.getAttachmentHash());
        manifest.put("operatorId", attachment.getOperatorId());
        manifest.put("operatorName", attachment.getOperatorName());
        manifest.put("operatedAt", attachment.getOperatedAt());
        return manifest;
    }

    private byte[] buildPdfBytes(String manifest) {
        ensurePrintableArchiveSnapshot(manifest);
        return MesProEdhrBatchArchivePrintablePdfRenderer.render(
                manifest, BATCH_ARCHIVE_PDF_FONT_PATH, BATCH_ARCHIVE_PDF_SYMBOL_FONT_PATH);
    }

    private void ensurePrintableArchiveSnapshot(String manifest) {
        if (StrUtil.isBlank(manifest)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED);
        }
        try {
            JSONObject snapshot = JSON.parseObject(manifest);
            if (snapshot == null || !PRINTABLE_ARCHIVE_SCHEMA_VERSION.equals(snapshot.getString("schemaVersion"))) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED);
            }
            if (snapshot.getJSONArray("bodyForms") == null || snapshot.getJSONArray("appendixSpecialNodes") == null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED);
            }
            for (JSONObject form : jsonArrayObjects(snapshot, "bodyForms")) {
                if (StrUtil.isBlank(form.getString("sheetLayoutJson"))
                        || StrUtil.isBlank(form.getString("executionSnapshotJson"))
                        || StrUtil.isBlank(form.getString("cellValuesJson"))) {
                    throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED);
                }
            }
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED);
        }
    }

    private List<String> buildPdfLines(String manifest) {
        String safeManifest = StrUtil.nullToEmpty(manifest);
        String hash = DigestUtil.sha256Hex(safeManifest);
        List<String> lines = new ArrayList<>();
        lines.add("eDHR Batch Final Archive");
        lines.add("eDHR 批次最终归档 PDF");
        lines.add("");
        lines.add("批次摘要");
        appendJsonScalar(lines, safeManifest, "batchExecutionId", "批次ID");
        appendJsonScalar(lines, safeManifest, "batchCode", "批号");
        appendJsonScalar(lines, safeManifest, "routeId", "路线ID");
        appendJsonScalar(lines, safeManifest, "aggregateHash", "归档聚合哈希");
        lines.add("");
        lines.add("普通表单与特殊节点");
        appendTaskLines(lines, safeManifest);
        lines.add("");
        lines.add("签名记录");
        lines.add("审核/批准记录");
        appendSignatureLines(lines, safeManifest);
        lines.add("");
        lines.add("附件清单");
        appendAttachmentLines(lines, safeManifest);
        lines.add("");
        lines.add("卷宗项");
        appendDossierLines(lines, safeManifest);
        lines.add("");
        lines.add("返工/驳回/变更事件");
        appendChangeEventLines(lines, safeManifest);
        lines.add("");
        lines.add("审计追踪");
        lines.add("审计追踪来源: manifest json, aggregate hash, signature hash, approval snapshot hash, domain trace hash");
        lines.add("");
        lines.add("归档哈希");
        lines.add("manifest hash: " + hash);
        lines.add("manifest json: " + safeManifest);
        return lines;
    }

    private void appendJsonScalar(List<String> lines, String manifest, String key, String label) {
        Object value = firstJsonValue(manifest, key);
        if (value != null) {
            lines.add(label + ": " + value(value));
        }
    }

    private void appendTaskLines(List<String> lines, String manifest) {
        List<JSONObject> tasks = jsonArrayObjects(manifest, "tasks");
        if (tasks.isEmpty()) {
            lines.add("无任务");
            return;
        }
        for (JSONObject task : tasks) {
            lines.add("任务: taskId=" + value(task.get("taskId"))
                    + ", nodeType=" + value(task.get("nodeType"))
                    + ", processName=" + value(task.get("processName"))
                    + ", batchRecordReportName=" + value(task.get("batchRecordReportName"))
                    + ", executionId=" + value(task.get("executionId"))
                    + ", status=" + value(task.get("status")));
            String specialPayloadJson = task.getString("specialPayloadJson");
            if (StrUtil.isNotBlank(specialPayloadJson)) {
                lines.add("  specialPayloadJson: " + shortText(specialPayloadJson, 240));
            }
            if (Integer.valueOf(45).equals(task.getInteger("status")) || StrUtil.isNotBlank(specialPayloadJson)) {
                JSONObject skippedPayload = StrUtil.isBlank(specialPayloadJson) ? null : JSON.parseObject(specialPayloadJson);
                lines.add("  跳过: 操作人=" + value(firstNonBlank(task.get("skippedBy"), skippedPayload == null ? null : skippedPayload.get("skippedBy")))
                        + ", 操作时间=" + value(firstNonBlank(task.get("skippedAt"), skippedPayload == null ? null : skippedPayload.get("skippedAt"))));
            }
            lines.add("  approvalStatus: " + value(task.get("approvalStatus")));
            lines.add("  approvalSnapshotHash: " + value(task.get("approvalSnapshotHash")));
            lines.add("  attachmentCount: " + value(task.get("attachmentCount")));
            List<JSONObject> attachmentRules = jsonArrayObjects(task, "attachmentRuleSummaries");
            for (JSONObject rule : attachmentRules) {
                lines.add("  附件规则: fieldPath=" + value(rule.get("fieldPath"))
                        + ", fieldKey=" + value(rule.get("fieldKey"))
                        + ", label=" + value(rule.get("label"))
                        + ", required=" + value(rule.get("required"))
                        + ", minCount=" + value(rule.get("minCount"))
                        + ", maxCount=" + value(rule.get("maxCount"))
                        + ", attachmentType=" + value(rule.get("attachmentType"))
                        + ", groupKey=" + value(rule.get("groupKey")));
            }
            List<JSONObject> attachments = jsonArrayObjects(task, "attachmentManifests");
            for (JSONObject attachment : attachments) {
                lines.add("  附件: fileName=" + value(attachment.get("fileName"))
                        + ", fieldKey=" + value(attachment.get("fieldKey"))
                        + ", fieldLabel=" + value(attachment.get("fieldLabel"))
                        + ", attachmentType=" + value(attachment.get("attachmentType"))
                        + ", sha256=" + value(attachment.get("sha256"))
                        + ", attachmentHash=" + value(attachment.get("attachmentHash"))
                        + ", operatorName=" + value(attachment.get("operatorName")));
            }
        }
    }

    private void appendSignatureLines(List<String> lines, String manifest) {
        List<JSONObject> tasks = jsonArrayObjects(manifest, "tasks");
        if (tasks.isEmpty()) {
            lines.add("无签名");
            return;
        }
        for (JSONObject task : tasks) {
            List<JSONObject> signatures = jsonArrayObjects(task, "signatures");
            if (signatures.isEmpty()) {
                continue;
            }
            lines.add("任务 " + value(task.get("taskId")) + " 签名:");
            for (JSONObject signature : signatures) {
                lines.add("  actionType=" + value(signature.get("actionType"))
                        + ", actorName=" + value(signature.get("actorName"))
                        + ", signedAt=" + value(signature.get("signedAt"))
                        + ", approvalResult=" + value(signature.get("approvalResult")));
            }
        }
    }

    private void appendAttachmentLines(List<String> lines, String manifest) {
        List<JSONObject> tasks = jsonArrayObjects(manifest, "tasks");
        boolean hasAny = false;
        for (JSONObject task : tasks) {
            List<JSONObject> attachments = jsonArrayObjects(task, "attachmentManifests");
            if (attachments.isEmpty()) {
                continue;
            }
            hasAny = true;
            lines.add("任务 " + value(task.get("taskId")) + " 附件数: " + attachments.size());
            for (JSONObject attachment : attachments) {
                lines.add("  fileName=" + value(attachment.get("fileName"))
                        + ", fieldPath=" + value(attachment.get("fieldPath"))
                        + ", contentType=" + value(attachment.get("contentType"))
                        + ", fileSize=" + value(attachment.get("fileSize"))
                        + ", sha256=" + value(attachment.get("sha256"))
                        + ", attachmentHash=" + value(attachment.get("attachmentHash")));
            }
        }
        if (!hasAny) {
            lines.add("无附件");
        }
    }

    private void appendDossierLines(List<String> lines, String manifest) {
        List<JSONObject> dossierItems = jsonArrayObjects(manifest, "dossierItems");
        if (dossierItems.isEmpty()) {
            lines.add("无卷宗项");
            return;
        }
        for (JSONObject item : dossierItems) {
            lines.add("卷宗项: itemType=" + value(item.get("itemType"))
                    + ", itemKey=" + value(item.get("itemKey"))
                    + ", itemName=" + value(item.get("itemName"))
                    + ", itemStatus=" + value(item.get("itemStatus"))
                    + ", sourceDocCode=" + value(item.get("sourceDocCode"))
                    + ", sourceDocResult=" + value(item.get("sourceDocResult"))
                    + ", sourceDocHash=" + value(item.get("sourceDocHash"))
                    + ", completedAt=" + value(item.get("completedAt"))
                    + ", verifiedAt=" + value(item.get("verifiedAt")));
        }
    }

    private void appendChangeEventLines(List<String> lines, String manifest) {
        List<JSONObject> changeEvents = jsonArrayObjects(manifest, "changeEvents");
        if (changeEvents.isEmpty()) {
            lines.add("无变更事件");
            return;
        }
        for (JSONObject event : changeEvents) {
            lines.add("变更事件: changeCode=" + value(event.get("changeCode"))
                    + ", changeType=" + value(event.get("changeType"))
                    + ", changeStatus=" + value(event.get("changeStatus"))
                    + ", reasonCategory=" + value(event.get("reasonCategory"))
                    + ", effectiveAt=" + value(event.get("effectiveAt")));
        }
    }

    private List<JSONObject> jsonArrayObjects(String manifest, String key) {
        if (StrUtil.isBlank(manifest)) {
            return List.of();
        }
        try {
            JSONObject root = JSON.parseObject(manifest);
            return jsonArrayObjects(root, key);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private List<JSONObject> jsonArrayObjects(JSONObject root, String key) {
        if (root == null) {
            return List.of();
        }
        return root.getJSONArray(key) == null ? List.of() : root.getJSONArray(key).toJavaList(JSONObject.class);
    }

    private Object firstNonBlank(Object first, Object second) {
        if (!StrUtil.isBlankIfStr(first)) {
            return first;
        }
        return second;
    }

    private Object firstJsonValue(String manifest, String key) {
        if (StrUtil.isBlank(manifest)) {
            return null;
        }
        try {
            return JSON.parseObject(manifest).get(key);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String shortText(String value, int maxLength) {
        String source = StrUtil.nullToEmpty(value);
        if (source.length() <= maxLength) {
            return source;
        }
        return source.substring(0, maxLength) + "...";
    }

    private BatchSignatureTimeEvidence buildBatchSignatureTimeEvidence(
            Long batchExecutionId,
            String actionType,
            Long actorId,
            LocalDateTime signedAt,
            MesProBatchRecordExecutionSignatureTimeReqVO signatureTime) {
        LocalDateTime selectedSignedAt = signatureTime == null ? null : signatureTime.getSelectedSignedAt();
        String signatureTimeMode = selectedSignedAt == null
                ? SIGNATURE_TIME_MODE_SERVER : SIGNATURE_TIME_MODE_USER_SELECTED;
        LocalDateTime displayAt = selectedSignedAt == null
                ? signedAt : selectedSignedAt.truncatedTo(ChronoUnit.SECONDS);
        String selectedTimeZone = null;
        String selectedTimeReason = null;
        if (selectedSignedAt != null) {
            selectedTimeZone = StrUtil.trim(signatureTime.getSelectedTimeZone());
            selectedTimeReason = StrUtil.trim(signatureTime.getSelectedTimeReason());
            if (StrUtil.isBlank(selectedTimeZone) || StrUtil.isBlank(selectedTimeReason)) {
                throw exception(BAD_REQUEST, "选择签名时间时必须填写时区和原因");
            }
        } else if (signatureTime != null && (StrUtil.isNotBlank(signatureTime.getSelectedTimeZone())
                || StrUtil.isNotBlank(signatureTime.getSelectedTimeReason()))) {
            throw exception(BAD_REQUEST, "选择签名时间时必须同时填写时间、时区和原因");
        }
        String auditHash = DigestUtil.sha256Hex(String.join("|",
                SIGNATURE_TIME_POLICY_VERSION,
                value(batchExecutionId),
                value(actionType),
                value(actorId),
                value(signedAt),
                signatureTimeMode,
                value(displayAt),
                value(selectedSignedAt == null ? null : selectedSignedAt.truncatedTo(ChronoUnit.SECONDS)),
                value(selectedTimeZone),
                value(selectedTimeReason)));
        return new BatchSignatureTimeEvidence(
                selectedSignedAt == null ? null : selectedSignedAt.truncatedTo(ChronoUnit.SECONDS),
                displayAt,
                signatureTimeMode,
                selectedTimeZone,
                selectedTimeReason,
                SIGNATURE_TIME_POLICY_VERSION,
                auditHash);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long currentUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? 0L : loginUserId;
    }

    private String requireSpecialNodeAttachmentAuditReason(String rawReason) {
        String reason = StrUtil.trim(rawReason);
        if (StrUtil.isBlank(reason)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID);
        }
        return reason;
    }

    private void recordAttachmentPrepareUploadAudit(MesProEdhrBatchExecutionTaskDO task,
                                                    MesProBatchRecordExecutionAttachmentDO pendingAttachment,
                                                    MesProEdhrSpecialNodeAttachmentPrepareUploadResult result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestSource", "BATCH_EXECUTION_DETAIL");
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("reason", "特殊节点附件上传预登记");
        metadata.put("batchTaskId", task.getId());
        metadata.put("nodeType", resolveNodeType(task));
        metadata.put("uploadToken", result.getUploadToken());
        metadata.put("fileId", result.getFileId());
        metadata.put("fileName", result.getFileName());
        metadata.put("fileSize", result.getFileSize());
        metadata.put("sha256", result.getSha256());
        metadata.put("storageConfigId", result.getStorageConfigId());
        metadata.put("storagePath", result.getStoragePath());
        metadata.put("storageRetentionHash", result.getStorageRetentionHash());
        metadata.put("previousAttachmentHash", pendingAttachment.getPreviousAttachmentHash());
        metadata.put("attachmentChainHeadHash", pendingAttachment.getAttachmentHash());
        metadata.put("pendingAttachment", toAttachmentAuditPayload(pendingAttachment));
        recordRegulatedOperationAudit("SPECIAL_NODE_ATTACHMENT", String.valueOf(pendingAttachment.getId()),
                "ATTACHMENT_PREPARE_UPLOAD", "特殊节点附件上传预登记",
                task.getBatchExecutionId(), SPECIAL_NODE_ATTACHMENT_EXECUTION_ID, null,
                null, task.getRouteProcessId(), task.getBatchRecordReportId(), task.getRecordCategory(),
                "mes:pro-edhr-batch-execution:update", "ALLOW", "SUCCESS",
                pendingAttachment.getPreviousAttachmentHash(), pendingAttachment.getAttachmentHash(),
                JSON.toJSONString(metadata));
    }

    private void recordPendingAttachmentDeleteAudit(MesProEdhrBatchExecutionTaskDO task,
                                                    MesProBatchRecordExecutionAttachmentDO deletedAttachment,
                                                    String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestSource", "BATCH_EXECUTION_DETAIL");
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("reason", reason);
        metadata.put("batchTaskId", task.getId());
        metadata.put("nodeType", resolveNodeType(task));
        metadata.put("fileId", deletedAttachment.getFileId());
        metadata.put("fileName", deletedAttachment.getFileName());
        metadata.put("fileSize", deletedAttachment.getFileSize());
        metadata.put("sha256", deletedAttachment.getSha256());
        metadata.put("storageConfigId", deletedAttachment.getStorageConfigId());
        metadata.put("storagePath", deletedAttachment.getStoragePath());
        metadata.put("previousAttachmentHash", deletedAttachment.getPreviousAttachmentHash());
        metadata.put("attachmentChainHeadHash", deletedAttachment.getAttachmentHash());
        metadata.put("deletedAttachment", toAttachmentAuditPayload(deletedAttachment));
        recordRegulatedOperationAudit("SPECIAL_NODE_ATTACHMENT", String.valueOf(deletedAttachment.getId()),
                "ATTACHMENT_PENDING_DELETE", "删除待提交特殊节点附件",
                task.getBatchExecutionId(), deletedAttachment.getExecutionId(), null,
                null, task.getRouteProcessId(), task.getBatchRecordReportId(), task.getRecordCategory(),
                "mes:pro-edhr-batch-execution:update", "ALLOW", "SUCCESS",
                deletedAttachment.getAttachmentHash(), hashAuditPayload(metadata), JSON.toJSONString(metadata));
    }

    private void recordAttachmentSavePendingAudit(MesProEdhrBatchExecutionDO batch,
                                                  String reason,
                                                  List<MesProBatchRecordExecutionAttachmentDO> pendingAttachments,
                                                  List<MesProBatchRecordExecutionAttachmentDO> persistedAttachments,
                                                  List<Map<String, Object>> taskPayloads) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestSource", "BATCH_EXECUTION_DETAIL");
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("reason", reason);
        metadata.put("batchExecutionId", batch.getId());
        metadata.put("pendingAttachmentCount", pendingAttachments.size());
        metadata.put("persistedAttachmentCount", persistedAttachments.size());
        metadata.put("pendingAttachments", pendingAttachments.stream()
                .map(this::toAttachmentAuditPayload)
                .toList());
        metadata.put("persistedAttachments", persistedAttachments.stream()
                .map(this::toAttachmentAuditPayload)
                .toList());
        metadata.put("taskPayloads", taskPayloads);
        metadata.put("beforeAttachmentHeadHash", pendingAttachments.isEmpty() ? null
                : pendingAttachments.get(pendingAttachments.size() - 1).getAttachmentHash());
        metadata.put("attachmentChainHeadHash", persistedAttachments.isEmpty() ? null
                : persistedAttachments.get(persistedAttachments.size() - 1).getAttachmentHash());
        recordRegulatedOperationAudit("BATCH_EXECUTION", String.valueOf(batch.getId()),
                "ATTACHMENT_SAVE_PENDING", "保存待提交特殊节点附件",
                batch.getId(), SPECIAL_NODE_ATTACHMENT_EXECUTION_ID, null,
                batch.getRouteId(), null, null, null,
                "mes:pro-edhr-batch-execution:update", "ALLOW", "SUCCESS",
                hashAuditPayload(pendingAttachments.stream().map(this::toAttachmentAuditPayload).toList()),
                hashAuditPayload(metadata), JSON.toJSONString(metadata));
    }

    private Map<String, Object> toAttachmentAuditPayload(MesProBatchRecordExecutionAttachmentDO attachment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", attachment.getId());
        payload.put("batchExecutionId", attachment.getBatchExecutionId());
        payload.put("batchTaskId", attachment.getBatchTaskId());
        payload.put("executionId", attachment.getExecutionId());
        payload.put("workTaskId", attachment.getWorkTaskId());
        payload.put("fieldKey", attachment.getFieldKey());
        payload.put("fieldPath", attachment.getFieldPath());
        payload.put("fieldLabel", attachment.getFieldLabel());
        payload.put("attachmentType", attachment.getAttachmentType());
        payload.put("attachmentGroupKey", attachment.getAttachmentGroupKey());
        payload.put("attachmentAction", attachment.getAttachmentAction());
        payload.put("versionNo", attachment.getVersionNo());
        payload.put("fileId", attachment.getFileId());
        payload.put("fileName", attachment.getFileName());
        payload.put("fileSize", attachment.getFileSize());
        payload.put("sha256", attachment.getSha256());
        payload.put("storageConfigId", attachment.getStorageConfigId());
        payload.put("storagePath", attachment.getStoragePath());
        payload.put("storageRetentionHash", attachment.getStorageRetentionHash());
        payload.put("previousAttachmentHash", attachment.getPreviousAttachmentHash());
        payload.put("attachmentHash", attachment.getAttachmentHash());
        payload.put("operatorId", attachment.getOperatorId());
        payload.put("operatedAt", attachment.getOperatedAt());
        payload.put("reasonCategory", attachment.getReasonCategory());
        payload.put("reasonText", attachment.getReasonText());
        return payload;
    }

    private String hashAuditPayload(Object payload) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(JSON.toJSONString(payload));
    }

    private void recordRegulatedOperationAudit(String objectType, String objectId, String operationType,
                                               String actionName, Long batchExecutionId, Long executionId,
                                               Long workTaskId, Long routeId, Long routeProcessId, String reportId,
                                               String recordCategory, String permissionCode, String permissionDecision,
                                               String resultStatus, String beforeSummaryHash, String afterSummaryHash,
                                               String metadataJson) {
        Long actorUserId = requireAuditActorUserId();
        String requestId = "EDHR-AUD-" + java.util.UUID.randomUUID();
        JSONObject metadata = JSON.parseObject(metadataJson);
        if (metadata == null) {
            throw new IllegalStateException("eDHR regulated audit metadata is required");
        }
        metadata.putIfAbsent("auditRequestId", requestId);
        metadata.putIfAbsent("idempotencyKey", requestId);
        metadata.putIfAbsent("associatedSignatureId", "NOT_APPLICABLE");
        metadata.putIfAbsent("permissionDecision", permissionDecision);
        metadata.putIfAbsent("resultStatus", resultStatus);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId(requestId)
                .setObjectType(objectType)
                .setObjectId(objectId)
                .setBatchExecutionId(batchExecutionId)
                .setExecutionId(executionId)
                .setWorkTaskId(workTaskId)
                .setRouteId(routeId)
                .setRouteProcessId(routeProcessId)
                .setReportId(reportId)
                .setRecordCategory(recordCategory)
                .setOperationType(operationType)
                .setActionName(actionName)
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(permissionCode)
                .setPermissionDecision(permissionDecision)
                .setResultStatus(resultStatus)
                .setBeforeSummaryHash(beforeSummaryHash)
                .setAfterSummaryHash(afterSummaryHash)
                .setMetadataJson(metadata.toJSONString()));
    }

    private Long requireAuditActorUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(UNAUTHORIZED);
        }
        return loginUserId;
    }

    private void recordOperationAudit(String objectType, String objectId, String operationType, String actionName,
                                      Long batchExecutionId, Long executionId, Long workTaskId, Long routeId,
                                      Long routeProcessId, String reportId, String recordCategory,
                                      String permissionCode, String permissionDecision, String resultStatus,
                                      String beforeSummaryHash, String afterSummaryHash, String metadataJson) {
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + java.util.UUID.randomUUID())
                .setObjectType(objectType)
                .setObjectId(objectId)
                .setBatchExecutionId(batchExecutionId)
                .setExecutionId(executionId)
                .setWorkTaskId(workTaskId)
                .setRouteId(routeId)
                .setRouteProcessId(routeProcessId)
                .setReportId(reportId)
                .setRecordCategory(recordCategory)
                .setOperationType(operationType)
                .setActionName(actionName)
                .setActorUserId(currentUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(permissionCode)
                .setPermissionDecision(permissionDecision)
                .setResultStatus(resultStatus)
                .setBeforeSummaryHash(beforeSummaryHash)
                .setAfterSummaryHash(afterSummaryHash)
                .setMetadataJson(metadataJson));
    }

    private record FrozenBatchUseConfig(JSONObject processConfig,
                                        JSONObject node,
                                        JSONObject reportConfig,
                                        String reportId) {

        private Integer routeProcessSort() {
            return node == null ? null : node.getInteger("sort");
        }

        private Integer reportSort() {
            return reportConfig == null ? null : reportConfig.getInteger("reportSort");
        }
    }

    private record BatchTaskConfig(MesProRouteProcessDO routeProcess,
                                   MesProProcessDO process,
                                   MesProRouteFlowProcessBatchRecordDO batchRecord,
                                   MesProBatchRecordReportDO report,
                                   String executionMode,
                                   Long predecessorRouteProcessId,
                                   String specialNodeType,
                                   String specialNodeName,
                                   Integer specialSort) {

        private static BatchTaskConfig special(String nodeType, String nodeName, Integer sort) {
            return new BatchTaskConfig(null, null, null, null, EXECUTION_MODE_SEQUENTIAL,
                    null, nodeType, nodeName, sort);
        }
    }

    private record TaskGate(boolean available, String message) {
    }

    private record TaskActionContext(String currentUserRole,
                                     List<String> allowedActions,
                                     String disabledReason,
                                     Long activeWorkTaskId,
                                     String activeWorkTaskType,
                                     String actionUrl) {
    }

    private record BatchSignatureTimeEvidence(LocalDateTime selectedSignedAt,
                                              LocalDateTime signatureDisplayAt,
                                              String signatureTimeMode,
                                              String selectedTimeZone,
                                              String selectedTimeReason,
                                              String selectedTimePolicyVersion,
                                              String selectedTimeAuditHash) {
    }

    private static final class BatchArchivePdfWriter {

        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream contentStream;
        private float cursorY;

        private BatchArchivePdfWriter(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            addPage();
        }

        private void writeTitle(String text) throws IOException {
            ensureSpace(BATCH_ARCHIVE_PDF_TITLE_FONT_SIZE + BATCH_ARCHIVE_PDF_LEADING);
            contentStream.beginText();
            contentStream.setFont(font, BATCH_ARCHIVE_PDF_TITLE_FONT_SIZE);
            contentStream.newLineAtOffset(BATCH_ARCHIVE_PDF_MARGIN, cursorY);
            contentStream.showText(sanitize(text));
            contentStream.endText();
            cursorY -= BATCH_ARCHIVE_PDF_TITLE_FONT_SIZE + BATCH_ARCHIVE_PDF_LEADING;
        }

        private void writeLine(String text) throws IOException {
            for (String wrappedLine : wrap(text)) {
                ensureSpace(BATCH_ARCHIVE_PDF_LEADING);
                contentStream.beginText();
                contentStream.setFont(font, BATCH_ARCHIVE_PDF_FONT_SIZE);
                contentStream.newLineAtOffset(BATCH_ARCHIVE_PDF_MARGIN, cursorY);
                contentStream.showText(sanitize(wrappedLine));
                contentStream.endText();
                cursorY -= BATCH_ARCHIVE_PDF_LEADING;
            }
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (cursorY - requiredHeight >= BATCH_ARCHIVE_PDF_MARGIN) {
                return;
            }
            contentStream.close();
            addPage();
        }

        private void addPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - BATCH_ARCHIVE_PDF_MARGIN;
        }

        private void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private List<String> wrap(String text) {
            String source = sanitize(text);
            if (source.isEmpty()) {
                return List.of("");
            }
            List<String> wrapped = new ArrayList<>();
            int index = 0;
            while (index < source.length()) {
                int end = Math.min(index + BATCH_ARCHIVE_PDF_WRAP_CHARS, source.length());
                wrapped.add(source.substring(index, end));
                index = end;
            }
            return wrapped;
        }

        private String sanitize(String value) {
            return StrUtil.nullToEmpty(value).replace('\r', ' ').replace('\n', ' ');
        }
    }
}
