package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrSourceCreationBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrSourceCreationBindingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Immutable facts, joined writes only; query and mapper failures are never converted to absence. */
@Service
public class MesEdhrSourceCreationBindingStore {
    private static final Map<String, String> PROTOCOLS = Map.of(
            "INDEPENDENT_ISSUE_V1", "INDEPENDENT_ISSUE_REQUEST_V1",
            "INDEPENDENT_REVOKE_V1", "INDEPENDENT_REVOKE_REQUEST_V1",
            "MATERIAL_READY_V1", "MATERIAL_READY_REQUEST_V1");
    private final MesEdhrSourceCreationBindingMapper mapper;

    public MesEdhrSourceCreationBindingStore(MesEdhrSourceCreationBindingMapper mapper) { this.mapper = mapper; }

    public record BindingFact(Long tenantId, String ownerProtocol, Long sourceRowId, String sourceReceiptId,
            String sourceVersion, String sourceHash, String requestHashVersion, String requestHash,
            Long creationEventId, String creationEventHash) {}
    public record StoredBindingFact(Long id, BindingFact fact) {}

    public List<StoredBindingFact> readBySourceRow(Long tenant, String protocol, Long rowId) {
        tenant(tenant); protocol(protocol); positive(rowId);
        return read(mapper.selectBySourceRow(tenant, protocol, rowId), fact ->
                Objects.equals(fact.tenantId(), tenant) && Objects.equals(fact.ownerProtocol(), protocol)
                        && Objects.equals(fact.sourceRowId(), rowId));
    }
    public List<StoredBindingFact> readByReceiptVersion(Long tenant, String protocol, String receiptId, String version) {
        tenant(tenant); protocol(protocol); text(receiptId, 128); version(version);
        return read(mapper.selectByReceiptVersion(tenant, protocol, receiptId, version), fact ->
                Objects.equals(fact.tenantId(), tenant) && Objects.equals(fact.ownerProtocol(), protocol)
                        && Objects.equals(fact.sourceReceiptId(), receiptId) && Objects.equals(fact.sourceVersion(), version));
    }
    public List<StoredBindingFact> readByCreationEvent(Long tenant, Long eventId) {
        tenant(tenant); positive(eventId);
        return read(mapper.selectByCreationEvent(tenant, eventId), fact ->
                Objects.equals(fact.tenantId(), tenant) && Objects.equals(fact.creationEventId(), eventId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public StoredBindingFact insert(BindingFact input) {
        validate(input); tenant(input.tenantId());
        MesEdhrSourceCreationBindingDO row = row(input);
        if (mapper.insert(row) != 1 || row.getId() == null || row.getId() <= 0) {
            throw new IllegalStateException("SOURCE_BINDING_INSERT_FAILED");
        }
        StoredBindingFact actual = stored(row);
        if (!input.equals(actual.fact())
                || !List.of(actual).equals(readBySourceRow(input.tenantId(), input.ownerProtocol(), input.sourceRowId()))
                || !List.of(actual).equals(readByReceiptVersion(input.tenantId(), input.ownerProtocol(),
                        input.sourceReceiptId(), input.sourceVersion()))
                || !List.of(actual).equals(readByCreationEvent(input.tenantId(), input.creationEventId()))) {
            throw new IllegalStateException("SOURCE_BINDING_MISMATCH");
        }
        return actual;
    }

    private List<StoredBindingFact> read(List<MesEdhrSourceCreationBindingDO> rows,
            java.util.function.Predicate<BindingFact> matches) {
        if (rows == null) throw new IllegalStateException("SOURCE_BINDING_QUERY_NULL");
        List<StoredBindingFact> result = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        for (MesEdhrSourceCreationBindingDO row : rows) {
            StoredBindingFact value = stored(row);
            if (!ids.add(value.id()) || !matches.test(value.fact())) {
                throw new IllegalStateException("SOURCE_BINDING_MISMATCH");
            }
            result.add(value);
        }
        return List.copyOf(result);
    }
    private static StoredBindingFact stored(MesEdhrSourceCreationBindingDO row) {
        if (row == null || row.getId() == null || row.getId() <= 0) {
            throw new IllegalStateException("SOURCE_BINDING_MISMATCH");
        }
        BindingFact fact = new BindingFact(row.getTenantId(), row.getOwnerProtocol(), row.getSourceRowId(),
                row.getSourceReceiptId(), row.getSourceVersion(), row.getSourceHash(), row.getRequestHashVersion(),
                row.getRequestHash(), row.getCreationEventId(), row.getCreationEventHash());
        try { validate(fact); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("SOURCE_BINDING_MISMATCH", e); }
        return new StoredBindingFact(row.getId(), fact);
    }
    private static MesEdhrSourceCreationBindingDO row(BindingFact f) {
        return new MesEdhrSourceCreationBindingDO().setTenantId(f.tenantId()).setOwnerProtocol(f.ownerProtocol())
                .setSourceRowId(f.sourceRowId()).setSourceReceiptId(f.sourceReceiptId())
                .setSourceVersion(f.sourceVersion()).setSourceHash(f.sourceHash())
                .setRequestHashVersion(f.requestHashVersion()).setRequestHash(f.requestHash())
                .setCreationEventId(f.creationEventId()).setCreationEventHash(f.creationEventHash());
    }
    private static void validate(BindingFact f) {
        if (f == null) throw new IllegalArgumentException("Binding fact required");
        positive(f.tenantId()); positive(f.sourceRowId()); positive(f.creationEventId());
        protocol(f.ownerProtocol()); text(f.sourceReceiptId(), 128); version(f.sourceVersion());
        hash(f.sourceHash()); hash(f.requestHash()); hash(f.creationEventHash());
        if (!Objects.equals(PROTOCOLS.get(f.ownerProtocol()), f.requestHashVersion())) {
            throw new IllegalArgumentException("SOURCE_BINDING_REQUEST_PROTOCOL");
        }
    }
    static void tenant(Long tenant) {
        positive(tenant);
        if (TenantContextHolder.isIgnore() || !Objects.equals(tenant, TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalArgumentException("SOURCE_TENANT_MISMATCH");
        }
    }
    static void positive(Long value) {
        if (value == null || value <= 0) throw new IllegalArgumentException("Positive source identity required");
    }
    static void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Source text required");
    }
    static void hash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Lowercase SHA256 required");
    }
    private static void protocol(String value) {
        if (!PROTOCOLS.containsKey(value == null ? "" : value)) throw new IllegalArgumentException("Unknown owner protocol");
    }
    private static void version(String value) {
        if (value == null || value.length() > 64 || !value.matches("0|[1-9][0-9]*")) {
            throw new IllegalArgumentException("Canonical unsigned version required");
        }
    }
}
