package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Component
public class WorkbenchQueryTokens {
    public static final int CONTRACT_VERSION = 3;
    public static final Duration LIFETIME = Duration.ofSeconds(300);
    public record Claim(int contractVersion, String registryRevision, List<String> sources, long tenant, long user,
                        long visibilityVersion, WorkbenchQuerySpec querySpec, String contextEpoch, Instant issuedAt, Instant expiresAt) { }
    public record Issued(String queryVersion, Claim claim) { }
    private final WorkbenchTokenStore store;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    @org.springframework.beans.factory.annotation.Autowired
    public WorkbenchQueryTokens(WorkbenchTokenStore store, ObjectMapper mapper) { this(store, mapper, Clock.systemUTC()); }
    // Explicit clock supports deterministic expiry verification; production has one UTC clock.
    public WorkbenchQueryTokens(WorkbenchTokenStore store, ObjectMapper mapper, Clock clock) {
        this.store = store; this.mapper = mapper; this.clock = clock;
    }
    public Issued issue(long tenant, long user, long version, String revision, List<String> sources,
                        WorkbenchQuerySpec spec, String epoch) {
        epoch(epoch);
        if (tenant < 0 || user <= 0 || version < 1 || revision == null || sources == null || spec == null) throw INVALID_QUERY.exception();
        spec = spec.normalized();
        Instant now = clock.instant();
        Claim claim = new Claim(CONTRACT_VERSION, revision, List.copyOf(sources), tenant, user, version, spec, epoch, now, now.plus(LIFETIME));
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            if (!store.create(token, mapper.writeValueAsString(claim), LIFETIME)) throw QUERY_TOKEN_STORE_FAILED.exception();
        } catch (ServiceException failure) { throw failure; }
        catch (Exception failure) { throw QUERY_TOKEN_STORE_FAILED.exception(failure); }
        return new Issued(token, claim);
    }
    public Claim verify(String token, String epoch, long tenant, long user) {
        epoch(epoch);
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw QUERY_TOKEN_INVALID.exception();
        String encoded;
        try { encoded = store.read(token); }
        catch (RuntimeException failure) { throw QUERY_TOKEN_STORE_FAILED.exception(failure); }
        if (encoded == null) throw QUERY_TOKEN_EXPIRED.exception();
        Claim claim;
        try { claim = mapper.readValue(encoded, Claim.class); }
        catch (Exception failure) { throw QUERY_TOKEN_INVALID.exception(failure); }
        if (claim == null || claim.contractVersion() != CONTRACT_VERSION || claim.registryRevision() == null
                || claim.sources() == null || claim.tenant() < 0 || claim.user() <= 0 || claim.sources().stream().anyMatch(Objects::isNull) || claim.visibilityVersion() < 1 || claim.querySpec() == null
                || claim.contextEpoch() == null || claim.issuedAt() == null || claim.expiresAt() == null
                || !claim.expiresAt().equals(claim.issuedAt().plus(LIFETIME)) || claim.issuedAt().isAfter(clock.instant())) throw QUERY_TOKEN_INVALID.exception();
        requireAlive(claim);
        if (claim.tenant() != tenant || claim.user() != user || !claim.contextEpoch().equals(epoch)) throw QUERY_CONTEXT_CHANGED.exception();
        try { if (!claim.querySpec().equals(claim.querySpec().normalized())) throw QUERY_TOKEN_INVALID.exception(); }
        catch (ServiceException failure) { throw QUERY_TOKEN_INVALID.exception(failure); }
        return claim;
    }
    public void requireAlive(Claim claim) { if (!clock.instant().isBefore(claim.expiresAt())) throw QUERY_TOKEN_EXPIRED.exception(); }
    public static void epoch(String epoch) {
        if (epoch == null || !epoch.matches("[A-Za-z0-9_.:-]{1,128}")) throw INVALID_QUERY.exception();
    }
}
