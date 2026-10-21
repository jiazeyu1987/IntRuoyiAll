package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionFence;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Component
@ConditionalOnProperty(name="profile.workbench.query-engine",havingValue="s4")
public class IndexQueryTokens {
    public record Claim(int contractVersion,long tenant,long user,String contextEpoch,WorkbenchQuerySpec querySpec,
                        ProjectionFence.Version version,Instant issuedAt,Instant expiresAt) { }
    public record Issued(String token,Claim claim) { }
    private final WorkbenchTokenStore store;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SecureRandom random=new SecureRandom();
    @org.springframework.beans.factory.annotation.Autowired
    public IndexQueryTokens(WorkbenchTokenStore store,ObjectMapper mapper) { this(store,mapper,Clock.systemUTC()); }
    public IndexQueryTokens(WorkbenchTokenStore store,ObjectMapper mapper,Clock clock) { this.store=store;this.mapper=mapper;this.clock=clock; }
    public Instant now() { return clock.instant(); }
    public Issued issue(long tenant,long user,String epoch,WorkbenchQuerySpec spec,ProjectionFence.Version version) {
        WorkbenchQueryTokens.epoch(epoch); ProjectionFence.alive(version,now());
        Instant now=now(),expires=now.plusSeconds(cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.TOKEN_MAX_SECONDS);
        if(version.authorizationExpiresAt()!=null&&version.authorizationExpiresAt().isBefore(expires)) expires=version.authorizationExpiresAt();
        if(tenant<0||user<=0||!spec.equals(spec.normalized())||!now.isBefore(expires)) throw INVALID_QUERY.exception();
        Claim claim=new Claim(4,tenant,user,epoch,spec,version,now,expires);byte[] bytes=new byte[32];random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try { if(!store.create(token,mapper.writeValueAsString(claim),Duration.between(now,expires))) throw QUERY_TOKEN_STORE_FAILED.exception(); }
        catch(ServiceException failure) { throw failure; }
        catch(Exception failure) { throw QUERY_TOKEN_STORE_FAILED.exception(failure); }
        return new Issued(token,claim);
    }
    public Claim verify(String token,String epoch,long tenant,long user) {
        WorkbenchQueryTokens.epoch(epoch);
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}")) throw QUERY_TOKEN_INVALID.exception();
        String encoded;
        try { encoded=store.read(token); }
        catch(RuntimeException failure) { throw QUERY_TOKEN_STORE_FAILED.exception(failure); }
        if(encoded==null) throw QUERY_TOKEN_EXPIRED.exception();
        Claim claim;
        try { claim=mapper.readValue(encoded,Claim.class); }
        catch(Exception failure) { throw QUERY_TOKEN_INVALID.exception(failure); }
        if(claim==null||claim.contractVersion()!=4||claim.tenant()<0||claim.user()<1||claim.contextEpoch()==null
            ||claim.querySpec()==null||claim.version()==null||claim.issuedAt()==null||claim.expiresAt()==null
            ||claim.issuedAt().isAfter(now())||!claim.expiresAt().isAfter(claim.issuedAt())
            ||claim.expiresAt().isAfter(claim.issuedAt().plusSeconds(cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.TOKEN_MAX_SECONDS))
            ||(claim.version().authorizationExpiresAt()!=null&&claim.expiresAt().isAfter(claim.version().authorizationExpiresAt()))
            ||claim.version().authorizationAt().isAfter(claim.issuedAt())||!claim.querySpec().equals(claim.querySpec().normalized())) throw QUERY_TOKEN_INVALID.exception();
        if(claim.tenant()!=tenant||claim.user()!=user||!claim.contextEpoch().equals(epoch)) throw QUERY_CONTEXT_CHANGED.exception();
        alive(claim); return claim;
    }
    public void alive(Claim claim) {
        ProjectionFence.alive(claim.version(),now());
        if(!now().isBefore(claim.expiresAt())) throw QUERY_TOKEN_EXPIRED.exception();
    }
}
