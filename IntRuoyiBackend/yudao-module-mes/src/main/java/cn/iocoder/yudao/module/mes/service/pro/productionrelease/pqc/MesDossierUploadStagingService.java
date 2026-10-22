package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesDossierUploadIntentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesDossierUploadIntentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Durable storage intent precedes I/O. Only unbound, expired objects may be reclaimed. */
@Service @RequiredArgsConstructor @Slf4j
public class MesDossierUploadStagingService {
    private final MesDossierUploadIntentMapper intents;
    private final FileConfigService configurations;
    private final FileMapper files;
    private final PlatformTransactionManager transactions;
    public record Staged(Long intentId, FileDO file) {}

    public Staged upload(Long actor, Long order, String category, String name, String type, byte[] content) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var client = configurations.getMasterFileClient();
        if (client == null) throw new IllegalStateException("资料存储未配置");
        String extension = cn.hutool.core.io.FileUtil.extName(name);
        // Preserve the existing file service's extension semantics: local download MIME detection uses the path.
        String suffix = extension == null || extension.isBlank()
                ? cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils.getExtension(type) : "." + extension;
        if (suffix == null) suffix = "";
        if (!suffix.isEmpty() && !suffix.matches("\\.[A-Za-z0-9]+")) throw new IllegalArgumentException("资料文件扩展名无效");
        var intent = MesDossierUploadIntentDO.builder().activeOrderId(order).actorId(actor)
                .configId(client.getId()).storagePath("mes/active-order-dossier/" + tenant + "/" + order + "/" + category + "/" + UUID.randomUUID() + suffix)
                .state("UPLOADING").expiresAt(LocalDateTime.now().plusHours(24)).build();
        intent.setTenantId(tenant);
        independent(() -> { if (intents.insert(intent) != 1) throw new IllegalStateException("上传意图登记失败"); return intent; });
        final String url;
        try { url = client.upload(content, intent.getStoragePath(), type); }
        catch (Exception failure) { throw new IllegalStateException("资料存储写入失败；已登记待清理操作 " + intent.getId(), failure); }
        if (url == null || url.isBlank()) throw new IllegalStateException("存储未返回有效地址；待清理操作 " + intent.getId());
        return independent(() -> {
            var current = intents.lock(intent.getId());
            requireOwner(current, actor, order);
            if (!"UPLOADING".equals(current.getState()) || !current.getExpiresAt().isAfter(LocalDateTime.now()))
                throw new IllegalStateException("上传操作已过期或已被清理认领，请重新上传");
            var file = FileDO.builder().configId(intent.getConfigId()).path(intent.getStoragePath())
                    .name(name).type(type).size((long) content.length).url(url).build();
            if (files.insert(file) != 1 || file.getId() == null) throw new IllegalStateException("资料文件登记失败");
            current.setFileId(file.getId()); current.setState("READY");
            if (intents.updateById(current) != 1) throw new IllegalStateException("上传意图更新失败");
            return new Staged(current.getId(), file);
        });
    }

    /** Called in the business transaction, along with the association and audits. */
    public void bind(Staged staged, Long actor, Long order) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("资料绑定必须在业务事务内执行");
        var current = intents.lock(staged.intentId());
        requireOwner(current, actor, order);
        if (!"READY".equals(current.getState()) || !Objects.equals(current.getFileId(), staged.file().getId())
                || !current.getExpiresAt().isAfter(LocalDateTime.now())) throw new IllegalStateException("资料上传状态已改变，请重新上传");
        current.setState("BOUND");
        if (intents.updateById(current) != 1) throw new IllegalStateException("资料绑定状态写入失败");
    }

    private static void requireOwner(MesDossierUploadIntentDO row, Long actor, Long order) {
        if (row == null || !Objects.equals(row.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(row.getActorId(), actor) || !Objects.equals(row.getActiveOrderId(), order))
            throw new IllegalStateException("资料上传操作归属不一致");
    }
    private <T> T independent(Supplier<T> work) {
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> work.get());
    }

    @Scheduled(fixedDelay=60000, initialDelay=60000)
    public void cleanExpired() {
        for (var candidate : intents.expired()) {
            TenantUtils.execute(candidate.getTenantId(), () -> clean(candidate.getId()));
        }
    }
    private void clean(Long id) {
        var claimed = independent(() -> {
            var row = intents.lock(id);
            if (row == null || !java.util.Set.of("UPLOADING","READY","CLEANING").contains(row.getState())
                    || row.getExpiresAt().isAfter(LocalDateTime.now())) return null;
            row.setState("CLEANING"); row.setExpiresAt(LocalDateTime.now().plusMinutes(10));
            if (intents.updateById(row) != 1) throw new IllegalStateException("资料清理认领失败");
            return row;
        });
        if (claimed == null) return;
        try {
            var client = configurations.getFileClient(claimed.getConfigId());
            if (client == null) throw new IllegalStateException("资料存储配置不存在");
            client.delete(claimed.getStoragePath());
            independent(() -> {
                var row = intents.lock(id);
                if (!"CLEANING".equals(row.getState())) throw new IllegalStateException("资料清理状态异常");
                if (row.getFileId() != null) files.deleteById(row.getFileId());
                row.setState("CLEANED"); row.setLastError(null);
                if (intents.updateById(row) != 1) throw new IllegalStateException("资料清理确认失败");
                return null;
            });
        } catch (Exception failure) {
            // Failure remains a durable retryable state; never report cleanup success.
            log.error("资料清理失败 tenant={} intent={}", claimed.getTenantId(), id, failure);
            independent(() -> {
                var row = intents.lock(id);
                row.setLastError(failure.getClass().getSimpleName());
                if (intents.updateById(row) != 1) throw new IllegalStateException("资料清理错误登记失败", failure);
                return null;
            });
        }
    }
}
