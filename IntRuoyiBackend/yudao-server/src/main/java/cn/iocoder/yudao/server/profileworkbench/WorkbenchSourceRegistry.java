package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.SourceAdapter;
import cn.iocoder.yudao.module.system.api.workbench.SourceDescriptor;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.util.*;

/** The only Spring source discovery/composition root. No business source enum. */
@Component
public class WorkbenchSourceRegistry {
    public record Module(String ownerModule, boolean enabled, List<String> sourceIds) { }
    public record Manifest(int contractVersion, String registryRevision, List<Module> modules) { }
    public record Directory(int contractVersion, String registryRevision, List<SourceDescriptor> sources) { }
    private final Manifest manifest;
    private final Map<String, SourceAdapter> sources;
    private final Set<String> disabled;

    @Autowired
    public WorkbenchSourceRegistry(List<SourceAdapter> beans, ObjectMapper mapper) throws IOException {
        this(beans, mapper.readValue(new ClassPathResource("workbench-manifest.json").getInputStream(), Manifest.class));
    }
    public WorkbenchSourceRegistry(List<SourceAdapter> beans, Manifest manifest) {
        if (manifest == null || manifest.contractVersion() != 1 || manifest.registryRevision() == null
                || manifest.registryRevision().isBlank() || manifest.modules() == null)
            throw new IllegalStateException("CONTRACT_VERSION_UNSUPPORTED: invalid manifest");
        this.manifest = manifest;
        Map<String, String> expected = new HashMap<>();
        Set<String> disabledIds = new HashSet<>(), modules = new HashSet<>();
        for (Module module : manifest.modules()) {
            if (module == null || module.ownerModule() == null || module.sourceIds() == null
                    || module.sourceIds().isEmpty() || !modules.add(module.ownerModule()))
                throw new IllegalStateException("Duplicate or invalid manifest module");
            for (String id : module.sourceIds()) {
                if (id == null || id.isBlank() || expected.put(id, module.ownerModule()) != null)
                    throw new IllegalStateException("Duplicate or invalid manifest source");
                if (!module.enabled()) disabledIds.add(id);
            }
        }
        Map<String, SourceAdapter> registered = new HashMap<>();
        Set<String> discovered = new HashSet<>();
        Set<Integer> ranks = new HashSet<>(); Set<String> actions = new HashSet<>();
        for (SourceAdapter source : beans) {
            if (source.rowOrderContract() != cn.iocoder.yudao.module.system.api.workbench.RowOrderContract.UTF8_BINARY_DESC)
                throw new IllegalStateException("Missing fixed workbench row order contract");
            if (source.identityContract() != cn.iocoder.yudao.module.system.api.workbench.SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY)
                throw new IllegalStateException("Missing unique binary task identity contract");
            SourceDescriptor d = Objects.requireNonNull(source.descriptor(), "Missing source descriptor");
            if (!d.ownerModule().equals(expected.get(d.sourceId()))) throw new IllegalStateException("Source not declared by manifest");
            if (!discovered.add(d.sourceId()) || !ranks.add(d.sortRank()) || !actions.add(d.actionKind()))
                throw new IllegalStateException("Duplicate workbench source/action/rank");
            if (d.contractVersion() != manifest.contractVersion()) throw new IllegalStateException("CONTRACT_VERSION_UNSUPPORTED");
            if (!disabledIds.contains(d.sourceId())) registered.put(d.sourceId(), source);
        }
        Set<String> enabled = new HashSet<>(expected.keySet()); enabled.removeAll(disabledIds);
        if (!enabled.equals(registered.keySet())) throw new IllegalStateException("SOURCE_NOT_REGISTERED: enabled manifest mismatch");
        sources = Map.copyOf(registered); disabled = Set.copyOf(disabledIds);
    }
    public SourceAdapter require(String id) {
        if (id == null) throw INVALID_QUERY.exception();
        if (disabled.contains(id)) throw MODULE_DISABLED.exception();
        SourceAdapter source = sources.get(id);
        if (source == null) throw SOURCE_NOT_REGISTERED.exception();
        return source;
    }
    public void requireRevision(String revision) {
        if (!manifest.registryRevision().equals(revision))
            throw CONTRACT_VERSION_UNSUPPORTED.exception();
    }
    public Set<String> enabledSourceIds() { return sources.keySet(); }
    public String revision() { return manifest.registryRevision(); }
    public List<String> normalize(List<String> ids) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size())
            throw INVALID_QUERY.exception();
        ids.forEach(this::require);
        return ids.stream().sorted(Comparator.comparingInt(id -> require(id).descriptor().sortRank())).toList();
    }
    public Directory directory(long user, long tenant) {
        List<SourceDescriptor> authorized = sources.values().stream()
                .sorted(Comparator.comparingInt(source -> source.descriptor().sortRank()))
                .filter(source -> source.isAuthorized(user, tenant)).map(SourceAdapter::descriptor).toList();
        return new Directory(manifest.contractVersion(), manifest.registryRevision(), authorized);
    }
}
