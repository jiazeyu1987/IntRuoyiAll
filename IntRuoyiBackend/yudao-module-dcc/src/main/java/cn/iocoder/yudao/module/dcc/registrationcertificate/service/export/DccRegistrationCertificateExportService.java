package cn.iocoder.yudao.module.dcc.registrationcertificate.service.export;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query.vo.DccRegistrationCertificatePageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DccRegistrationCertificateExportService {
    public record Scope(String tabType, String mode, DccRegistrationCertificatePageReqVO query, List<Long> recordIds) {}
    public record Preview(String snapshotId, String requestId, String mode, int count, Map<String,Long> types,
                          int maxRecords, boolean allowed, String reason, Instant snapshotExpiresAt, List<String> structureNotices) {}
    public record Status(String state, String reason, int exported, int skipped, String fileName, Instant expiresAt, List<String> structureNotices) {}
    private static final String PERMISSION = "dcc:registration-certificate:query-current";
    private final DccRegistrationCertificateQueryService queries;
    private final DccRegistrationCertificateAuditMapper audits;
    private final PermissionApi permissions;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final Clock clock;
    private final Map<String,Job> jobs = new HashMap<>();
    private final Set<String> generatingUsers = new HashSet<>();
    private final RegistrationCertificateExportWorkbook workbook = new RegistrationCertificateExportWorkbook();
    @Value("${dcc.registration-certificate.export.max-records:100}") private int maxRecords = 100;
    @Value("${dcc.registration-certificate.export.max-rows:10000}") private int maxRows = 10000;
    @Value("${dcc.registration-certificate.export.max-seconds:20}") private int maxSeconds = 20;
    private static final int MAX_FILE_BYTES = 16 * 1024 * 1024;
    private static final long MAX_RETAINED_BYTES = 64L * 1024 * 1024;
    private long retainedBytes;

    @org.springframework.beans.factory.annotation.Autowired
    public DccRegistrationCertificateExportService(DccRegistrationCertificateQueryService queries,
            DccRegistrationCertificateAuditMapper audits, PermissionApi permissions,
            DccRegistrationCertificateBusinessClock businessClock) {
        this(queries, audits, permissions, businessClock, Clock.systemUTC());
    }

    DccRegistrationCertificateExportService(DccRegistrationCertificateQueryService queries,
            DccRegistrationCertificateAuditMapper audits, PermissionApi permissions,
            DccRegistrationCertificateBusinessClock businessClock, Clock clock) {
        this.queries = queries; this.audits = audits; this.permissions = permissions;
        this.businessClock = businessClock; this.clock = clock;
    }

    public Preview preview(Long tenant, Long actor, Scope scope) {
        checkPermission(actor);
        if (scope == null || !"current".equals(scope.tabType()) || !Set.of("SELECTED", "FILTERED").contains(Objects.toString(scope.mode(), "")))
            throw error("仅支持注册证Tab的已勾选或全部筛选结果导出");
        List<Long> ids = scope.recordIds() == null ? List.of() : scope.recordIds().stream().distinct().toList();
        if (ids.stream().anyMatch(id -> id == null || id <= 0) || ids.size() > maxRecords)
            return denied(scope.mode(), "勾选记录数超限或记录ID无效，请缩小范围");
        if ("SELECTED".equals(scope.mode()) && ids.isEmpty()) throw error("请先勾选注册证");
        var query = scope.query() == null ? new DccRegistrationCertificatePageReqVO() : scope.query();
        // Use the exact same filter and ORDER BY path as the list, including deterministic tie breakers.
        List<cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageItem> rows;
        try {
            rows = queries.getExportRows(tenant, actor, query.toQuery(), "SELECTED".equals(scope.mode()) ? ids : null, maxRecords);
        } catch (DccRegistrationCertificateQueryService.ExportLimitExceeded limit) {
            return denied(scope.mode(), limit.getMessage());
        }
        if (rows.isEmpty()) throw error("暂无可导出数据");
        if ("SELECTED".equals(scope.mode()) && rows.size() != ids.size()) throw error("部分勾选记录已不属于当前列表，请重新查询并确认范围");
        Map<String,Long> types = new LinkedHashMap<>();
        for (var row : rows) types.merge(Objects.toString(DccRegistrationCertificateFacts.parse(row.getFactsJson()).getCertificateType(), "未配置"), 1L, Long::sum);
        Instant now = clock.instant();
        var job = new Job(tenant, actor, scope.mode(), JsonUtils.toJsonString(query), rows.stream().map(r -> r.getVersionId()).toList(), now.plusSeconds(1800));
        synchronized (this) {
            cleanup();
            if (jobs.size() >= 1000 || jobs.values().stream().filter(j -> j.tenant.equals(tenant) && j.actor.equals(actor)).count() >= 20)
                throw error("待确认导出范围过多，请稍后重试");
            jobs.put(job.request, job);
        }
        return new Preview(job.snapshot, job.request, job.mode, job.ids.size(), types, maxRecords, true, "", job.snapshotExpires,
                types.keySet().stream().flatMap(type -> RegistrationCertificateExportWorkbook.structureNotices(type).stream()).distinct().toList());
    }

    private Preview denied(String mode, String reason) {
        return new Preview(null, null, mode, 0, Map.of(), maxRecords, false, reason, null, List.of());
    }

    public void generate(Long tenant, Long actor, String snapshot, String request) throws IOException {
        checkPermission(actor);
        Job job;
        synchronized (this) {
            job = require(tenant, actor, request);
            if (!job.snapshot.equals(snapshot)) throw error("请求与导出快照不匹配");
            if (!job.state.equals("CONFIRMED")) return;
            if (!clock.instant().isBefore(job.snapshotExpires)) { job.state = "EXPIRED"; job.reason = "快照已过期，请重新确认范围"; throw error(job.reason); }
            if (generatingUsers.contains(job.userKey())) throw error("您有一笔Excel正在生成，请稍后再试");
            if (generatingUsers.size() >= 2 || retainedBytes + (generatingUsers.size() + 1L) * MAX_FILE_BYTES > MAX_RETAINED_BYTES)
                throw error("导出并发或临时存储已达上限，请稍后再试");
            generatingUsers.add(job.userKey()); job.state = "GENERATING";
        }
        long deadline = System.nanoTime() + maxSeconds * 1_000_000_000L;
        Runnable check = () -> { if (System.nanoTime() > deadline) throw error("Excel生成超时，请缩小导出范围"); };
        try {
            List<RegistrationCertificateExportWorkbook.Line> lines = new ArrayList<>();
            Map<Long,String> skipped = new LinkedHashMap<>();
            List<Long> exported = new ArrayList<>();
            Set<String> structureNotices = new LinkedHashSet<>();
            Long auditCompany = null, auditCertificate = null;
            for (Long id : job.ids) {
                check.run();
                cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageItem row;
                try { row = queries.getExportRow(tenant, actor, id); }
                catch (ServiceException denied) {
                    if (!isUnavailable(denied)) throw denied;
                    skipped.put(id, "记录已不可用或失去导出权限"); continue;
                }
                if (row == null) { skipped.put(id, "记录已删除或不再属于注册证列表"); continue; }
                try {
                    var line = workbook.map(row);
                    lines.add(line); exported.add(id);
                    structureNotices.addAll(RegistrationCertificateExportWorkbook.structureNotices(line.type()));
                    if (auditCertificate == null) { auditCompany = row.getOwnerCompanyId(); auditCertificate = row.getCertificateId(); }
                }
                catch (RegistrationCertificateExportWorkbook.MappingUnavailable unavailable) { skipped.put(id, unavailable.getMessage()); }
            }
            if (lines.isEmpty()) throw error("全部记录不可导出：" + String.join("；", new LinkedHashSet<>(skipped.values())));
            var bytes = new ByteArrayOutputStream();
            workbook.write(lines, new FilterOutputStream(bytes) {
                @Override public void write(int value) throws IOException { checkSize(1); out.write(value); }
                @Override public void write(byte[] b, int offset, int length) throws IOException { checkSize(length); out.write(b, offset, length); }
                private void checkSize(int addition) throws IOException {
                    check.run(); if ((long) bytes.size() + addition > MAX_FILE_BYTES) throw new IOException("Excel文件超出临时存储上限，请缩小范围");
                }
            }, maxRows, check);
            Instant generatedAt = clock.instant();
            var detail = new LinkedHashMap<String,Object>();
            detail.put("request_id", job.request); detail.put("snapshot_id", job.snapshot); detail.put("mode", job.mode);
            detail.put("filters", JsonUtils.parseObject(job.filters, Map.class)); detail.put("ordered_record_ids", job.ids);
            detail.put("exported_record_ids", exported); detail.put("skipped", skipped);
            detail.put("structure_notices", structureNotices);
            detail.put("record_id_kind", "versionId"); detail.put("generated_millis", (System.nanoTime() - deadline + maxSeconds * 1_000_000_000L) / 1_000_000);
            // Existing audit schema requires a trusted certificate identity even for a batch event.
            var audit = DccRegistrationCertificateAuditDO.builder().tenantId(tenant).actorId(actor)
                    .ownerCompanyId(auditCompany).certificateId(auditCertificate)
                    .eventKey("EXCEL:" + job.request).eventType("EXCEL_GENERATED").result("SUCCESS").resultCode("OK")
                    .requestTraceId(job.request).detailJson(JsonUtils.toJsonString(detail)).occurredAt(businessClock.now()).creator(actor.toString()).build();
            if (audits.insert(audit) != 1) throw error("Excel生成审计持久化失败");
            synchronized (this) {
                job.bytes = bytes.toByteArray(); retainedBytes += job.bytes.length;
                job.exported = List.copyOf(exported); job.skipped = skipped.size();
                job.structureNotices = List.copyOf(structureNotices);
                job.fileName = "医疗器械注册证信息表_" + businessClock.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
                job.expires = generatedAt.plusSeconds(300); job.state = "READY";
            }
        } catch (IOException | RuntimeException failure) {
            synchronized (this) { job.state = "FAILED"; job.reason = Objects.toString(failure.getMessage(), "Excel生成失败"); }
            throw failure;
        } finally {
            synchronized (this) { generatingUsers.remove(job.userKey()); }
        }
    }

    public Status status(Long tenant, Long actor, String request) {
        boolean permitted = permissions.hasAnyPermissions(actor, PERMISSION);
        synchronized (this) {
            cleanup();
            Job job = require(tenant, actor, request);
            if (!permitted) {
                if ("READY".equals(job.state)) invalidate(job);
                throw error("没有注册证台账导出权限");
            }
            return status(job);
        }
    }
    private Status status(Job job) { return new Status(job.state, job.reason, job.exported.size(), job.skipped, job.fileName, job.expires, job.structureNotices); }

    public Download open(Long tenant, Long actor, String request) {
        Job job;
        synchronized (this) {
            cleanup(); job = require(tenant, actor, request);
            if (!"READY".equals(job.state)) throw error(job.state.equals("GENERATING") ? "Excel正在生成，请稍后重试获取" : job.reason.isBlank() ? "请求尚未生成文件" : job.reason);
        }
        try {
            checkPermission(actor);
            for (Long id : job.exported) if (queries.getExportRow(tenant, actor, id) == null) throw error("导出记录已不可用");
        } catch (ServiceException denied) {
            if (!isUnavailable(denied) && !Objects.equals(denied.getCode(), 400)) throw denied;
            synchronized (this) { invalidate(job); }
            throw error(job.reason);
        }
        synchronized (this) {
            cleanup();
            if (!"READY".equals(job.state)) throw error(job.reason);
            job.streams++;
            return new Download(job, status(job));
        }
    }

    private void invalidate(Job job) {
        job.state = "INVALID";
        job.reason = "导出权限或可用范围已变化，请重新确认导出";
        release(job);
    }

    public final class Download implements AutoCloseable {
        private final Job job;
        private final byte[] bytes;
        private final Status status;
        private boolean closed;
        private Download(Job job, Status status) { this.job = job; this.bytes = job.bytes; this.status = status; }
        public Status status() { return status; }
        public int size() { return bytes.length; }
        public void write(OutputStream output) throws IOException { output.write(bytes); output.flush(); }
        @Override public void close() { synchronized (DccRegistrationCertificateExportService.this) {
            if (!closed) { closed = true; job.streams--; cleanup(); release(job); }
        } }
    }

    @Scheduled(fixedDelay = 1000)
    public synchronized void cleanup() {
        Instant now = clock.instant();
        for (Job job : jobs.values()) {
            if (job.expires != null && !now.isBefore(job.expires) && "READY".equals(job.state)) {
                job.state = "EXPIRED"; job.reason = "文件5分钟保留窗口已过期，请重新确认导出";
            }
            release(job);
        }
        jobs.values().removeIf(j -> j.streams == 0 && !"GENERATING".equals(j.state) && !now.isBefore(j.snapshotExpires.plusSeconds(1800)));
    }
    private void release(Job job) {
        if (job.streams == 0 && job.bytes != null && !"READY".equals(job.state)) {
            retainedBytes -= job.bytes.length; job.bytes = null;
        }
    }
    private Job require(Long tenant, Long actor, String request) {
        Job job = jobs.get(request);
        if (job == null || !job.tenant.equals(tenant) || !job.actor.equals(actor)) throw error("导出请求不存在、已回收或服务已重启，请重新确认范围");
        return job;
    }
    private void checkPermission(Long actor) { if (!permissions.hasAnyPermissions(actor, PERMISSION)) throw error("没有注册证台账导出权限"); }
    private boolean isUnavailable(ServiceException error) {
        return Set.of(
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(),
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS.getCode(),
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED.getCode()
        ).contains(error.getCode());
    }
    private ServiceException error(String message) { return new ServiceException(400, message); }
    private static class Job {
        final Long tenant, actor;
        final String request = UUID.randomUUID().toString(), snapshot = UUID.randomUUID().toString(), mode, filters;
        final List<Long> ids;
        final Instant snapshotExpires;
        String state = "CONFIRMED", reason = "", fileName;
        List<Long> exported = List.of();
        List<String> structureNotices = List.of();
        Instant expires;
        int skipped, streams;
        byte[] bytes;
        Job(Long tenant, Long actor, String mode, String filters, List<Long> ids, Instant snapshotExpires) {
            this.tenant=tenant; this.actor=actor; this.mode=mode; this.filters=filters; this.ids=ids; this.snapshotExpires=snapshotExpires;
        }
        String userKey() { return tenant + ":" + actor; }
    }
}
