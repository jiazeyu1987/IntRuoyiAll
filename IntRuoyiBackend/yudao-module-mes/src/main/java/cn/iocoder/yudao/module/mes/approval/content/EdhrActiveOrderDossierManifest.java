package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;
import static cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentProjection.*;

@Component
@RequiredArgsConstructor
public class EdhrActiveOrderDossierManifest {
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesProcessPoolActiveOrderDossierFileMapper dossiers;
    private final FileService files;
    private final cn.iocoder.yudao.module.infra.service.file.FileConfigService configurations;
    public record Snapshot(JsonNode manifest, List<PreviewAttachment> attachments) { }
    public Snapshot read(Long tenantId, Long orderId, Long applicationId, Long batchId, boolean locked) {
        var order = locked ? orders.selectByIdForUpdate(orderId) : orders.selectById(orderId);
        if (order == null || !Objects.equals(tenantId, order.getTenantId()) || order.getDossierRevision() == null) {
            throw new IllegalStateException("EDHR_DOSSIER_ORDER_CONTEXT_INVALID");
        }
        var query = new LambdaQueryWrapperX<MesProcessPoolActiveOrderDossierFileDO>()
                .eq(MesProcessPoolActiveOrderDossierFileDO::getActiveOrderId, orderId)
                .orderByAsc(MesProcessPoolActiveOrderDossierFileDO::getId);
        if (locked) query.last("FOR UPDATE");
        var items = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        var attachments = new ArrayList<PreviewAttachment>();
        for (var row : dossiers.selectList(query)) {
            if (!Objects.equals(row.getTenantId(), tenantId) || !Objects.equals(row.getActiveOrderId(), orderId)
                    || row.getApplicationId() != null && !Objects.equals(row.getApplicationId(), applicationId)
                    || row.getId() == null || row.getFileId() == null || row.getSha256() == null
                    || !row.getSha256().matches("[0-9a-f]{64}") || row.getFileSize() == null) {
                throw new IllegalStateException("EDHR_DOSSIER_FILE_SCOPE_INVALID");
            }
            var file = files.getFile(row.getFileId());
            if (file == null || !Objects.equals(file.getConfigId(), row.getStorageConfigId())
                    || !Objects.equals(file.getPath(), row.getStoragePath()) || !Objects.equals(file.getSize(), row.getFileSize())) {
                throw new IllegalStateException("EDHR_DOSSIER_FILE_MISSING");
            }
            try {
                byte[] bytes = files.getFileContent(file.getConfigId(), file.getPath());
                if (bytes == null || bytes.length != row.getFileSize()
                        || !cn.hutool.crypto.digest.DigestUtil.sha256Hex(bytes).equals(row.getSha256())) {
                    throw new IllegalStateException("EDHR_DOSSIER_FILE_INTEGRITY_FAILED");
                }
            } catch (RuntimeException ex) { throw ex; }
            catch (Exception ex) { throw new IllegalStateException("EDHR_DOSSIER_FILE_READ_FAILED", ex); }
            items.add(object("attachmentId", row.getId(), "activeOrderId", orderId,
                    "applicationId", row.getApplicationId(), "categoryKey", row.getCategoryKey(),
                    "fileId", row.getFileId(), "fileName", row.getFileName(), "fileSize", row.getFileSize(),
                    "contentType", row.getContentType(), "sha256", row.getSha256()));
            var client = configurations.getFileClient(file.getConfigId());
            if (client == null) throw new IllegalStateException("EDHR_DOSSIER_STORAGE_CONFIG_MISSING");
            String previewUrl = client.presignGetUrl(file.getUrl(), 900);
            if (previewUrl == null || previewUrl.isBlank()) throw new IllegalStateException("EDHR_DOSSIER_PREVIEW_URL_MISSING");
            attachments.add(new PreviewAttachment(row.getFileId().toString(), row.getFileName(),
                    row.getId().toString(), row.getSha256(), row.getFileSize().toString(), row.getContentType(),
                    previewUrl));
        }
        var manifest = object("tenantId", tenantId, "activeOrderId", orderId, "applicationId", applicationId,
                "batchExecutionId", batchId, "revision", version(order.getDossierRevision()), "files", items);
        manifest.put("manifestHash", ApprovalContentJson.hash(manifest));
        return new Snapshot(manifest, List.copyOf(attachments));
    }
}
