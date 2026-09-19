package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_DOSSIER_FILE_BLOCKED;

/**
 * 活跃订单资料文件服务。
 *
 * 资料文件是订单详情和 PQC 生产放行申请的业务资料，不属于 P2 正式批次。
 */
@Service
@RequiredArgsConstructor
public class MesActiveOrderDossierFileService {

    private static final String FILE_DIRECTORY_PREFIX = "mes/active-order-dossier/";
    private static final Set<String> CATEGORY_KEYS = Set.of(
            "INCOMING_INSPECTION_FILE", "STERILIZATION_FILE", "FINISHED_PRODUCT_FILE", "OTHER_FILE");
    private static final List<CategoryDefinition> CATEGORY_DEFINITIONS = List.of(
            new CategoryDefinition("INCOMING_INSPECTION_FILE", "来料检文件"),
            new CategoryDefinition("STERILIZATION_FILE", "灭菌文件"),
            new CategoryDefinition("FINISHED_PRODUCT_FILE", "成品检文件"),
            new CategoryDefinition("OTHER_FILE", "其他文件"));

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderDossierFileMapper dossierFileMapper;
    private final AdminUserApi adminUserApi;
    private final FileService fileService;
    private final MesProEdhrOperationAuditService operationAuditService;

    @Transactional(readOnly = true)
    public Result list(Long actorUserId, Query query) {
        if (query == null) {
            throw ServiceExceptionUtil.invalidParamException("资料文件查询参数为空。");
        }
        ResolvedContext context = resolveContext(actorUserId, query.activeOrderId(), query.applicationId());
        List<MesProcessPoolActiveOrderDossierFileDO> rows = dossierFileMapper
                .selectListByActiveOrderId(context.activeOrder().getId());
        Map<String, List<MesProcessPoolActiveOrderDossierFileDO>> rowsByCategory = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderDossierFileDO row : rows) {
            validateDossierRow(row);
            if (!Objects.equals(row.getActiveOrderId(), context.activeOrder().getId())) {
                throw new IllegalStateException("活跃订单资料文件归属与查询订单不一致：" + row.getId());
            }
            rowsByCategory.computeIfAbsent(row.getCategoryKey(), ignored -> new java.util.ArrayList<>()).add(row);
        }
        List<CategoryFiles> categories = CATEGORY_DEFINITIONS.stream()
                .map(category -> new CategoryFiles(category.key(), category.label(),
                        rowsByCategory.getOrDefault(category.key(), List.of()).stream()
                                .map(MesActiveOrderDossierFileService::toFileItem)
                                .toList()))
                .toList();
        Long applicationId = context.application() == null ? null : context.application().getId();
        return new Result(context.activeOrder().getId(), applicationId, categories);
    }

    @Transactional(readOnly = true)
    public MesProcessPoolActiveOrderDossierFileDO requireReadableFile(Long actorUserId, Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw ServiceExceptionUtil.invalidParamException("缺少资料文件编号，不能预览。");
        }
        List<MesProcessPoolActiveOrderDossierFileDO> rows = dossierFileMapper.selectListByFileId(fileId);
        if (rows == null || rows.size() != 1) {
            throw dossierFileBlocked("资料文件归属不唯一或不存在，不能预览：" + fileId);
        }
        MesProcessPoolActiveOrderDossierFileDO row = rows.get(0);
        validateDossierRow(row);
        ResolvedContext context = resolveContext(actorUserId, row.getActiveOrderId(), row.getApplicationId());
        if (!Objects.equals(context.activeOrder().getId(), row.getActiveOrderId())) {
            throw dossierFileBlocked("资料文件与活跃订单归属不一致，不能预览：" + fileId);
        }
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    public FileItem upload(Long actorUserId, UploadCommand command) {
        if (command == null) {
            throw ServiceExceptionUtil.invalidParamException("资料文件上传参数为空。");
        }
        CategoryDefinition category = requireCategory(command.categoryKey());
        if (command.content() == null || command.content().length == 0) {
            throw ServiceExceptionUtil.invalidParamException("上传文件内容为空，不能写入资料文件。");
        }
        String fileName = StrUtil.trim(command.fileName());
        if (StrUtil.isBlank(fileName)) {
            throw ServiceExceptionUtil.invalidParamException("上传文件缺少文件名，不能写入资料文件。");
        }
        String contentType = StrUtil.trim(command.contentType());
        if (StrUtil.isBlank(contentType)) {
            throw ServiceExceptionUtil.invalidParamException("上传文件缺少文件类型，不能写入资料文件。");
        }
        ResolvedContext context = resolveContext(actorUserId, command.activeOrderId(), command.applicationId());
        String operatorName = resolveOperatorName(actorUserId);
        String directory = FILE_DIRECTORY_PREFIX + context.activeOrder().getId() + "/" + category.key();
        Long fileId = fileService.createFileAndReturnId(command.content(), fileName, directory, contentType);
        if (fileId == null || fileId <= 0) {
            throw new IllegalStateException("文件服务未返回有效文件编号，不能登记资料文件。");
        }
        FileDO file = fileService.getFile(fileId);
        validateStoredFile(fileId, file, command.content());
        LocalDateTime operatedAt = LocalDateTime.now();
        MesProcessPoolActiveOrderDossierFileDO row = MesProcessPoolActiveOrderDossierFileDO.builder()
                .activeOrderId(context.activeOrder().getId())
                .applicationId(context.application() == null ? null : context.application().getId())
                .categoryKey(category.key())
                .fileId(file.getId())
                .fileUrl(file.getUrl())
                .storageConfigId(file.getConfigId())
                .storagePath(file.getPath())
                .fileName(file.getName())
                .contentType(file.getType())
                .fileSize(file.getSize())
                .sha256(DigestUtil.sha256Hex(command.content()))
                .operatorId(actorUserId)
                .operatorName(operatorName)
                .operatedAt(operatedAt)
                .build();
        try {
            int inserted = dossierFileMapper.insert(row);
            if (inserted != 1 || row.getId() == null) {
                IllegalStateException failure = new IllegalStateException("资料文件关系写入失败，不能确认上传成功。");
                cleanupStoredFile(fileId, failure);
                throw failure;
            }
        } catch (RuntimeException ex) {
            cleanupStoredFile(fileId, ex);
            throw ex;
        }
        recordDossierOperation("DOSSIER_UPLOAD", "活跃订单资料上传", actorUserId, operatorName, row, null, row.getSha256(),
                operatedAt);
        return toFileItem(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long actorUserId, DeleteCommand command) {
        if (command == null) {
            throw ServiceExceptionUtil.invalidParamException("资料文件删除参数为空。");
        }
        CategoryDefinition category = requireCategory(command.categoryKey());
        if (command.attachmentId() == null || command.attachmentId() <= 0) {
            throw ServiceExceptionUtil.invalidParamException("缺少资料文件编号，不能删除。");
        }
        ResolvedContext context = resolveContext(actorUserId, command.activeOrderId(), command.applicationId());
        MesProcessPoolActiveOrderDossierFileDO row = dossierFileMapper.selectById(command.attachmentId());
        if (row == null) {
            throw dossierFileBlocked("要删除的资料文件不存在：" + command.attachmentId());
        }
        if (!Objects.equals(row.getActiveOrderId(), context.activeOrder().getId())
                || !Objects.equals(row.getCategoryKey(), category.key())
                || row.getFileId() == null) {
            throw dossierFileBlocked("资料文件不属于当前活跃订单和资料页签，不能删除。");
        }
        String operatorName = resolveOperatorName(actorUserId);
        FileDO file = fileService.getFile(row.getFileId());
        if (file == null || file.getId() == null) {
            throw dossierFileBlocked("资料文件对应的文件实体不存在：" + row.getFileId());
        }
        int deleted = dossierFileMapper.deleteById(row.getId());
        if (deleted != 1) {
            throw new IllegalStateException("删除资料文件关系失败，不能确认删除成功：" + row.getId());
        }
        try {
            fileService.deleteFile(file.getId());
        } catch (Exception ex) {
            throw new IllegalStateException("删除资料文件实体失败：" + file.getId(), ex);
        }
        recordDossierOperation("DOSSIER_DELETE", "活跃订单资料删除", actorUserId, operatorName, row, row.getSha256(), null,
                LocalDateTime.now());
    }

    private ResolvedContext resolveContext(Long actorUserId, Long activeOrderId, Long applicationId) {
        if (actorUserId == null) {
            throw dossierFileBlocked("当前登录用户为空，不能读取资料文件。");
        }
        if (activeOrderId == null || activeOrderId <= 0) {
            throw ServiceExceptionUtil.invalidParamException("缺少活跃订单ID，不能读取资料文件。");
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = null;
        if (applicationId != null) {
            application = applicationMapper.selectById(applicationId);
            if (application == null) {
                throw dossierFileBlocked("PQC生产放行申请不存在：" + applicationId);
            }
            if (!Objects.equals(application.getActiveOrderId(), activeOrderId)) {
                throw dossierFileBlocked("PQC生产放行申请与当前活跃订单不一致，不能读取资料文件。");
            }
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null) {
            throw dossierFileBlocked("活跃订单不存在：" + activeOrderId);
        }
        if (applicationId == null && !Objects.equals(activeOrder.getLeaderUserId(), actorUserId)) {
            throw dossierFileBlocked("当前用户不是该活跃订单生产组长，不能读取资料文件。");
        }
        return new ResolvedContext(activeOrder, application);
    }

    private static void validateDossierRow(MesProcessPoolActiveOrderDossierFileDO row) {
        if (row == null || row.getId() == null || row.getId() <= 0
                || row.getActiveOrderId() == null || row.getActiveOrderId() <= 0
                || row.getFileId() == null || row.getFileId() <= 0
                || row.getTenantId() == null || row.getTenantId() <= 0
                || StrUtil.isBlank(row.getCategoryKey())) {
            throw new IllegalStateException("活跃订单资料文件记录不完整，不能返回资料文件。");
        }
        if (!CATEGORY_KEYS.contains(row.getCategoryKey())) {
            throw new IllegalStateException("活跃订单资料文件类型未配置：" + row.getCategoryKey());
        }
        if (StrUtil.isBlank(row.getFileUrl()) || row.getStorageConfigId() == null
                || StrUtil.isBlank(row.getStoragePath()) || StrUtil.isBlank(row.getFileName())
                || StrUtil.isBlank(row.getContentType()) || row.getFileSize() == null
                || row.getFileSize() < 0 || StrUtil.isBlank(row.getSha256())
                || row.getOperatorId() == null || StrUtil.isBlank(row.getOperatorName())
                || row.getOperatedAt() == null) {
            throw new IllegalStateException("活跃订单资料文件元数据不完整，不能返回资料文件：" + row.getId());
        }
    }

    private String resolveOperatorName(Long actorUserId) {
        AdminUserRespDTO user = adminUserApi.getUser(actorUserId);
        if (user == null || StrUtil.isBlank(user.getNickname())) {
            throw dossierFileBlocked("上传用户不存在有效名称，不能登记资料文件：" + actorUserId);
        }
        return StrUtil.trim(user.getNickname());
    }

    private static void validateStoredFile(Long requestedFileId, FileDO file, byte[] content) {
        if (file == null || file.getId() == null || !Objects.equals(file.getId(), requestedFileId)
                || StrUtil.isBlank(file.getUrl()) || file.getConfigId() == null
                || StrUtil.isBlank(file.getPath()) || StrUtil.isBlank(file.getName())
                || StrUtil.isBlank(file.getType()) || file.getSize() == null
                || file.getSize() < 0 || file.getSize() != content.length) {
            throw new IllegalStateException("文件服务返回的资料文件元数据不完整，不能登记资料文件：" + requestedFileId);
        }
    }

    private void cleanupStoredFile(Long fileId, RuntimeException failure) {
        try {
            fileService.deleteFile(fileId);
        } catch (Exception cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private void recordDossierOperation(String operationType, String actionName, Long actorUserId, String operatorName,
                                        MesProcessPoolActiveOrderDossierFileDO row, String beforeHash,
                                        String afterHash, LocalDateTime operatedAt) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("activeOrderId", row.getActiveOrderId());
        metadata.put("applicationId", row.getApplicationId());
        metadata.put("dossierFileId", row.getId());
        metadata.put("categoryKey", row.getCategoryKey());
        metadata.put("fileId", row.getFileId());
        metadata.put("fileName", row.getFileName());
        metadata.put("sha256", row.getSha256());
        operationAuditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("active-order-dossier-" + operationType.toLowerCase() + "-" + row.getId())
                .setObjectType("ACTIVE_ORDER_DOSSIER_FILE")
                .setObjectId(String.valueOf(row.getId()))
                .setRecordCategory("ACTIVE_ORDER_DOSSIER")
                .setOperationType(operationType)
                .setActionName(actionName)
                .setActorUserId(actorUserId)
                .setActorUsername(operatorName)
                .setPermissionCode("mes:pro:process-pool-team:active-order-dossier")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(beforeHash)
                .setAfterSummaryHash(afterHash)
                .setMetadataJson(JsonUtils.toJsonString(metadata))
                .setOccurredAt(operatedAt));
    }

    private static CategoryDefinition requireCategory(String categoryKey) {
        return CATEGORY_DEFINITIONS.stream()
                .filter(category -> category.key().equals(categoryKey))
                .findFirst()
                .orElseThrow(() -> ServiceExceptionUtil.invalidParamException("未知资料文件类型：" + categoryKey));
    }

    private static ServiceException dossierFileBlocked(String reason) {
        return ServiceExceptionUtil.exception(PRO_PROCESS_POOL_ACTIVE_ORDER_DOSSIER_FILE_BLOCKED, reason);
    }

    private static FileItem toFileItem(MesProcessPoolActiveOrderDossierFileDO row) {
        return new FileItem(row.getId(), row.getFileId(), row.getFileName(), row.getFileUrl(),
                row.getContentType(), row.getFileSize(), row.getSha256(), row.getOperatorId(),
                row.getOperatorName(), row.getOperatedAt());
    }

    public record Query(Long activeOrderId, Long applicationId) {
    }

    public record UploadCommand(Long activeOrderId, Long applicationId, String categoryKey,
                                String fileName, String contentType, byte[] content) {
    }

    public record DeleteCommand(Long activeOrderId, Long applicationId, String categoryKey, Long attachmentId) {
    }

    public record Result(Long activeOrderId, Long applicationId, List<CategoryFiles> categories) {
    }

    public record CategoryFiles(String key, String label, List<FileItem> files) {
    }

    public record FileItem(Long attachmentId, Long fileId, String fileName, String fileUrl,
                           String contentType, Long fileSize, String sha256, Long operatorId,
                           String operatorName, LocalDateTime operatedAt) {
    }

    private record CategoryDefinition(String key, String label) {
    }

    private record ResolvedContext(MesProcessPoolActiveOrderDO activeOrder,
                                   MesProcessPoolActiveOrderReleaseApplicationDO application) {
    }
}
