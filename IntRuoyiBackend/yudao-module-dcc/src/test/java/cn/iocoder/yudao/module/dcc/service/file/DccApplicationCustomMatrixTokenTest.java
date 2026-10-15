package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixService.*;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix.ApprovalSourceMode;
import static org.junit.jupiter.api.Assertions.*;

/** Actual TokenService, existing signing properties/mac and current JsonUtils. No mock signer or fake authorization. */
class DccApplicationCustomMatrixTokenTest {
    private static final String KEY="g03-explicit-isolated-test-key-32-bytes-long";
    private DccApprovalMatrixTokenService service()throws Exception {
        var properties=new DccApprovalMatrixTokenProperties();properties.setSigningSecret(KEY);
        var service=new DccApprovalMatrixTokenService();Field field=service.getClass().getDeclaredField("properties");field.setAccessible(true);field.set(service,properties);return service;
    }
    private Path repositoryRoot(){Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();if(root==null)throw new IllegalStateException("Repository token fixture resources are unavailable");return root;}
    private Path task(){return repositoryRoot().resolve("doc/tasks/20261014-dcc-gap-other-custom-matrix");}
    private final DccApplicationCustomMatrixService matrices=new DccApplicationCustomMatrixService();
    private final DccApplicationCustomMatrix matrix=new DccApplicationCustomMatrix(88L,89L);
    private final Instant issued=Instant.parse("2026-10-15T00:00:00Z");
    private PreviewSourceContext preview(){return new PreviewSourceContext(ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,"UPLOAD","custom-scope","exact-v2-definition","a".repeat(64));}
    private SourceContext reserved(){return new SourceContext(ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,20L,3,"UPLOAD","custom-scope","exact-v2-definition","a".repeat(64));}
    private CanonicalSelection p(){return matrices.canonicalizePreview(preview(),matrix,List.of(51L,52L));}
    private CanonicalSelection r(){return matrices.canonicalize(reserved(),matrix,List.of(51L,52L));}
    private DccDirectSupervisorSource.Evidence evidence(){return new DccDirectSupervisorSource.Evidence(1L,99L,100L,"USER_MANAGEMENT","relation-99","v3");}
    private void clock(DccApprovalMatrixTokenService service,Instant value)throws Exception{var f=service.getClass().getDeclaredField("customClock");f.setAccessible(true);f.set(service,Clock.fixed(value,ZoneOffset.UTC));}
    @Test void oldPublicBaselineConfirmationAndNativeSupervisorRemainExactSignedVectors()throws Exception {
        var service=service();var fixture=repositoryRoot().resolve("doc/tasks/20261011-dcc-w02-types-matrix/immutable-public-matrix-v2.fixture.json");
        String canonical=DccApprovalMatrixCanonicalizer.canonicalize(JsonUtils.parseObject(Files.readString(fixture),Map.class));
        var c=new DccApprovalMatrixTokenService.Context(1L,7L,8L,9L,10L,"NEW",null,"public-vector-scope");
        String baseline=service.issueBaseline(c,canonical);String confirmed=service.issueConfirmation(c,canonical,11L,3,"b".repeat(64));
        var evidence=new DccDirectSupervisorSource.Evidence(1L,7L,99L,"USER_MANAGEMENT","RELATION-7","v3");
        var source=service.issueNativeSupervisor(c,"c".repeat(64),"public-exact-definition","d".repeat(64),List.of(51L,52L),evidence);
        var actual=new TreeMap<String,String>();actual.put("baseline",baseline);actual.put("confirmation",confirmed);actual.put("nativeToken",source.sourceToken());actual.put("nativeDigest",source.sourceDigest());
        Path frozen=task().resolve("a22-public-token-vectors.json");
        if(Boolean.getBoolean("g03.token.capturePublic")) {
            assertFalse(Files.exists(frozen),"Never overwrite the original public vector");
            Files.writeString(frozen,JsonUtils.toJsonString(actual));
        }
        assertEquals(JsonUtils.parseObject(Files.readString(frozen),Map.class),actual);
        assertEquals(canonical,service.verifyBaseline(c,baseline));service.requireConfirmation(c,confirmed,canonical,11L,3,"b".repeat(64));
        service.requireNativeSupervisor(source,source.sourceDigest(),source.sourceToken());
        assertThrows(IllegalArgumentException.class,()->service.verifyBaseline(c,confirmed));
    }
    @Test void realPreviewAndReservedHmacBindExactCanonicalAndTheirOwnPurposesAndPhases()throws Exception{
        var service=service();clock(service,issued);String pt=service.issueCustomPreview(preview(),p()),rt=service.issueCustomReserved(reserved(),r());
        service.requireCustomPreview(preview(),p(),pt);service.requireCustomReserved(reserved(),r(),rt);
        assertNotEquals(pt,rt);
        assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),p(),rt));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomReserved(reserved(),r(),pt));
        var changed=matrices.canonicalizePreview(preview(),new DccApplicationCustomMatrix(101L,89L),List.of(51L,52L));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),changed,pt));
        var dept=matrices.canonicalizePreview(preview(),matrix,List.of(51L));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),dept,pt));
        var control=matrices.canonicalizePreview(preview(),new DccApplicationCustomMatrix(88L,101L),List.of(51L,52L));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),control,pt));
    }
    @Test void tenantProjectCategoryTaxonomyRequesterFileRoundContextAndDefinitionCannotReplayProof()throws Exception{
        var service=service();clock(service,issued);String token=service.issueCustomReserved(reserved(),r());var c=reserved();
        for(int i=0;i<6;i++){
            var ids=new Long[]{c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),c.requesterId(),c.controlledFileId()};ids[i]++;
            var changed=new SourceContext(c.approvalSourceMode(),ids[0],ids[1],ids[2],ids[3],ids[4],ids[5],3,c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
            var selection=matrices.canonicalize(changed,matrix,List.of(51L,52L));assertThrows(IllegalArgumentException.class,()->service.requireCustomReserved(changed,selection,token));
        }
        for(var changed:List.of(
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,4,"UPLOAD",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"UPLOAD","different-scope",c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"UPLOAD",c.applicationContextKey(),"different-definition","b".repeat(64)))){
            var selection=matrices.canonicalize(changed,matrix,List.of(51L,52L));assertThrows(IllegalArgumentException.class,()->service.requireCustomReserved(changed,selection,token));
        }
    }
    @Test void sameContextReadReplayIsValidUntilExpiryAndFutureExpiredOrMissingSecretReject()throws Exception{
        var service=service();clock(service,issued);String token=service.issueCustomPreview(preview(),p());
        service.requireCustomPreview(preview(),p(),token);service.requireCustomPreview(preview(),p(),token);
        clock(service,issued.plusSeconds(899));service.requireCustomPreview(preview(),p(),token);
        clock(service,issued.plusSeconds(900));assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),p(),token));
        clock(service,issued.minusSeconds(1));assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),p(),token));
        var missing=new DccApprovalMatrixTokenService();var props=new DccApprovalMatrixTokenProperties();var f=missing.getClass().getDeclaredField("properties");f.setAccessible(true);f.set(missing,props);
        assertThrows(IllegalStateException.class,()->missing.issueCustomPreview(preview(),p()));
    }
    @Test void taggedSupervisorHmacBindsRealSourceAndDoesNotAuthorizeConfigurationPurpose()throws Exception{
        var service=service();clock(service,issued);var previewProof=service.issueCustomSupervisorPreview(preview(),p(),evidence());
        var reservedProof=service.issueCustomSupervisorReserved(reserved(),r(),evidence());
        service.requireCustomSupervisorPreview(preview(),p(),evidence(),previewProof.sourceDigest(),previewProof.sourceToken());
        service.requireCustomSupervisorReserved(reserved(),r(),evidence(),reservedProof.sourceDigest(),reservedProof.sourceToken());
        assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),p(),previewProof.sourceToken()));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomSupervisorPreview(preview(),p(),evidence(),reservedProof.sourceDigest(),reservedProof.sourceToken()));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomSupervisorReserved(reserved(),r(),evidence(),previewProof.sourceDigest(),previewProof.sourceToken()));
        for(var bad:List.of(new DccDirectSupervisorSource.Evidence(2L,99L,100L,"USER_MANAGEMENT","relation-99","v3"),
                new DccDirectSupervisorSource.Evidence(1L,98L,100L,"USER_MANAGEMENT","relation-99","v3"),
                new DccDirectSupervisorSource.Evidence(1L,99L,101L,"USER_MANAGEMENT","relation-99","v3"),
                new DccDirectSupervisorSource.Evidence(1L,99L,100L,"USER_MANAGEMENT","other-relation","v3"),
                new DccDirectSupervisorSource.Evidence(1L,99L,100L,"USER_MANAGEMENT","relation-99","v4")))
            assertThrows(IllegalArgumentException.class,()->service.requireCustomSupervisorPreview(preview(),p(),bad,previewProof.sourceDigest(),previewProof.sourceToken()));
        assertEquals("APPLICATION_CUSTOM",previewProof.payload().get("sourceKind"));
        assertFalse(previewProof.payload().containsKey("routeId"));assertFalse(previewProof.payload().containsKey("matrixDigest"));
    }
    @Test void tamperingPayloadMacCanonicalHashOrPresentingOnlyShaCannotPassActualVerification()throws Exception{
        var service=service();clock(service,issued);String token=service.issueCustomReserved(reserved(),r());String[] parts=token.split("\\.");
        String raw=new String(Base64.getUrlDecoder().decode(parts[0]),java.nio.charset.StandardCharsets.UTF_8);
        String corrupted=Base64.getUrlEncoder().withoutPadding().encodeToString(raw.replace("APPLICATION_CUSTOM","PUBLIC_MATRIX").getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+parts[1];
        assertThrows(IllegalArgumentException.class,()->service.requireCustomReserved(reserved(),r(),corrupted));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomReserved(reserved(),r(),parts[0]+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32])));
        assertThrows(IllegalArgumentException.class,()->service.requireCustomReserved(reserved(),r(),r().digest()));
        assertThrows(IllegalArgumentException.class,()->service.issueCustomReserved(reserved(),new CanonicalSelection(r().canonical(),"b".repeat(64))));
    }
    private String signTestPayload(String raw)throws Exception{
        String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
        return encoded+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Test void evenAuthenticatedDuplicateUnknownOrModifiedLifetimeEnvelopesAreRejected()throws Exception{
        var service=service();clock(service,issued);String token=service.issueCustomPreview(preview(),p());
        String raw=new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]),java.nio.charset.StandardCharsets.UTF_8);
        for(String bad:List.of(raw.replace("\"phase\":","\"unknown\":true,\"phase\":"),
                raw.replace("\"phase\":","\"phase\":\"PREVIEW\",\"phase\":"),raw+" {}",
                raw.replace("2026-10-15T00:15:00Z","2026-10-15T00:30:00Z"),
                raw.replace("\"phase\":\"PREVIEW\"","\"phase\":\"RESERVED\"")))
            assertThrows(IllegalArgumentException.class,()->service.requireCustomPreview(preview(),p(),signTestPayload(bad)));
    }
    @Test void supervisorIssuanceRefusesMissingAccountsDepartmentsOrUntrustedEvidenceShape()throws Exception{
        var service=service();clock(service,issued);
        for(var selection:List.of(matrices.canonicalizePreview(preview(),null,List.of(51L)),matrices.canonicalizePreview(preview(),matrix,List.of()),
                matrices.canonicalizePreview(preview(),new DccApplicationCustomMatrix(88L,null),List.of(51L))))
            assertThrows(IllegalArgumentException.class,()->service.issueCustomSupervisorPreview(preview(),selection,evidence()));
        for(var bad:Arrays.asList(null,new DccDirectSupervisorSource.Evidence(1L,99L,0L,"SYSTEM","identity","version"),
                new DccDirectSupervisorSource.Evidence(1L,99L,100L," SYSTEM","identity","version"),
                new DccDirectSupervisorSource.Evidence(1L,99L,100L,"SYSTEM","identity\nother","version")))
            assertThrows(IllegalArgumentException.class,()->service.issueCustomSupervisorPreview(preview(),p(),bad));
    }
}
