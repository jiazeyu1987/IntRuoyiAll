package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/** Server-only sealed artifact, never an HTTP request/boolean claiming verification. */
public final class DccLegacyNameVerifiedScope {
    public record Evidence(Long claimId,Long masterId,Long fileId,Long sourceFileId,Long configId,
        String versionNo,String sourceName,String sourcePath,String sourceSha256,long sourceSize,
        int storageType,String storageEndpoint,String storageBucket,String storageRegion,boolean storagePathStyle,
        String claimName,Long claimProjectId,Long claimLeafId,String claimNumber,
        Long masterProjectId,Long masterLeafId,String masterNumber,String processInstanceId,
        String filePreimageSha256,String masterPreimageSha256,String claimPreimageSha256,String storagePreimageSha256) { }
    record RootManifest(int schemaVersion,String database,String serverUuid,Long tenantId,String scopeId,
        String factsSha256,String bytesReceiptSha256,String userDecisionSha256,String scopeIdentitySha256,
        LocalDateTime verifiedAt,String reason,String requestId,List<Evidence> evidence) { }
    final RootManifest manifest;
    final String manifestSha256;
    private static final com.fasterxml.jackson.databind.ObjectMapper STRICT_JSON=new com.fasterxml.jackson.databind.ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
        .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .disable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    private DccLegacyNameVerifiedScope(RootManifest m,String sha) {
        Objects.requireNonNull(m);this.manifest=new RootManifest(m.schemaVersion(),m.database(),m.serverUuid(),m.tenantId(),m.scopeId(),m.factsSha256(),m.bytesReceiptSha256(),m.userDecisionSha256(),m.scopeIdentitySha256(),m.verifiedAt(),m.reason(),m.requestId(),List.copyOf(m.evidence()));
        this.manifestSha256=sha;validate();
    }

    /** Only the reviewed internal local execution entry may supply its explicitly authorized raw artifact SHA. */
    static DccLegacyNameVerifiedScope fromProtectedArtifact(Path artifact,String authorizedRawSha256,Path facts,Path bytesReceipt,Path bytesResults,Path userDecision) throws java.io.IOException {
        hash(authorizedRawSha256);
        if(artifact==null || !Files.isRegularFile(artifact) || Files.isSymbolicLink(artifact))throw new IllegalArgumentException("sealed regular artifact required");
        byte[] body=readBounded(artifact,2*1024*1024);
        if(!DigestUtil.sha256Hex(body).equals(authorizedRawSha256))throw new IllegalArgumentException("authorized artifact bytes changed");
        var sealed=new DccLegacyNameVerifiedScope(STRICT_JSON.readValue(body,RootManifest.class),authorizedRawSha256);
        sealed.verifyArtifacts(facts,bytesReceipt,bytesResults,userDecision);
        return sealed;
    }
    private void verifyArtifacts(Path facts,Path bytesReceipt,Path bytesResults,Path userDecision) throws java.io.IOException {
        byte[] factsBody=readBounded(facts),receiptBody=readBounded(bytesReceipt),resultsBody=readBounded(bytesResults),decisionBody=readBounded(userDecision);
        if(!DigestUtil.sha256Hex(factsBody).equals(manifest.factsSha256()) || !DigestUtil.sha256Hex(receiptBody).equals(manifest.bytesReceiptSha256()) || !DigestUtil.sha256Hex(decisionBody).equals(manifest.userDecisionSha256()))
            throw new IllegalArgumentException("sealed actual evidence artifact bytes changed");
        var receipt=STRICT_JSON.readValue(receiptBody,Map.class);
        var decision=STRICT_JSON.readValue(decisionBody,Map.class);
        if(!(decision.get("historicalNamesDecision") instanceof Map historical)
            || !Boolean.TRUE.equals(historical.get("preserveHistoricalFilesVersionsNamesAndSignatures"))
            || !Boolean.TRUE.equals(historical.get("verifiedHistoricalOriginalNamesRemainOccupied"))
            || !Boolean.TRUE.equals(historical.get("rejectFutureNewExactDuplicateNames"))
            || !Boolean.FALSE.equals(historical.get("autoRenameMergeOrDeleteHistoricalRecords")))
            throw new IllegalArgumentException("actual agreed historical-name policy required");
        var identity=(Map<?,?>)receipt.get("source");
        if(!"SOURCE_BYTES_VERIFIED".equals(receipt.get("status")) || !Boolean.TRUE.equals(receipt.get("sourceBytesVerified"))
            || !Integer.valueOf(0).equals(receipt.get("readerExitCode")) || identity==null || !manifest.database().equals(identity.get("database"))
            || !manifest.serverUuid().equals(identity.get("serverUuid")) || !DigestUtil.sha256Hex(resultsBody).equals(receipt.get("resultsSha256"))
            || !manifest.factsSha256().equals(receipt.get("sourceFactsSha256")))
            throw new IllegalArgumentException("actual complete MATCH receipt required; incomplete sources cannot activate");
        if(!(receipt.get("finishedAtUtc") instanceof String finished)
            || !manifest.verifiedAt().equals(java.time.OffsetDateTime.parse(finished).atZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai")).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MICROS)))
            throw new IllegalArgumentException("verified time must be derived from actual completed MATCH read");
        var bySource=new HashMap<String,Evidence>();manifest.evidence().forEach(e->bySource.put(String.valueOf(e.sourceFileId()),e));
        var seen=new HashSet<String>();
        for(var line:lines(resultsBody)) {
            if(line.isBlank())throw new IllegalArgumentException("empty actual source result");
            var row=STRICT_JSON.readValue(line,Map.class);var id=row.get("id");var e=bySource.get(id);
            if(!row.keySet().equals(Set.of("id","status","actualSha256","actualLength","httpStatus","errorCode"))
                || e==null || !seen.add((String)id) || !"MATCH".equals(row.get("status")) || !e.sourceSha256().equals(row.get("actualSha256"))
                || !(row.get("actualLength") instanceof Number size) || size instanceof Double || size instanceof Float || size.longValue()!=e.sourceSize()
                || !Integer.valueOf(200).equals(row.get("httpStatus")) || row.get("errorCode")!=null)
                throw new IllegalArgumentException("actual source result coverage/hash/length is invalid");
        }
        if(!seen.equals(bySource.keySet()) || !Objects.equals(receipt.get("objectsRead"),seen.size()) || !Objects.equals(receipt.get("matches"),seen.size()))
            throw new IllegalArgumentException("actual source result scope is incomplete");
        var versions=new HashMap<String,Evidence>();manifest.evidence().forEach(e->versions.put(String.valueOf(e.fileId()),e));var sourceVersions=new HashSet<String>();
        var claimFacts=new HashSet<String>();var masterFacts=new HashSet<String>();var storageFacts=new HashSet<String>();var ownershipFacts=new HashSet<String>();
        for(var line:lines(factsBody)) {
            var row=STRICT_JSON.readValue(line,Map.class);
            if("claim".equals(row.get("kind"))) {
                var own=manifest.evidence().stream().filter(e->String.valueOf(e.claimId()).equals(row.get("id"))).toList();
                if(own.isEmpty() || !claimFacts.add(String.valueOf(row.get("id"))))throw new IllegalArgumentException("sealed claim set differs");
                for(var e:own)if(!String.valueOf(e.masterId()).equals(row.get("masterId")) || !e.claimName().equals(row.get("normalizedName")) || !String.valueOf(manifest.tenantId()).equals(row.get("tenantId")))throw new IllegalArgumentException("sealed claim preimage differs");
                continue;
            }
            if("master".equals(row.get("kind"))) {
                var own=manifest.evidence().stream().filter(e->String.valueOf(e.masterId()).equals(row.get("id"))).toList();
                if(own.isEmpty() || !masterFacts.add(String.valueOf(row.get("id"))))throw new IllegalArgumentException("sealed master set differs");
                for(var e:own)if(!Objects.equals(e.masterProjectId()==null?null:String.valueOf(e.masterProjectId()),row.get("projectId")) || !Objects.equals(e.masterLeafId()==null?null:String.valueOf(e.masterLeafId()),row.get("leafId")) || !Objects.equals(e.masterNumber(),row.get("normalizedNumber")) || !String.valueOf(manifest.tenantId()).equals(row.get("tenantId")))throw new IllegalArgumentException("sealed master preimage differs");
                continue;
            }
            if("storage".equals(row.get("kind"))) {
                var e=bySource.get(row.get("id"));
                if(e==null || !storageFacts.add(String.valueOf(row.get("id"))) || !String.valueOf(e.configId()).equals(row.get("configId")) || !e.sourceName().equals(row.get("name")) || !String.valueOf(e.sourceSize()).equals(row.get("size")) || !HexFormat.of().withUpperCase().formatHex(e.sourceName().getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(row.get("nameHex")))throw new IllegalArgumentException("sealed storage preimage differs");
                continue;
            }
            if("ownership".equals(row.get("kind"))) {
                var e=versions.get(row.get("controlledFileId"));
                if(e==null || !ownershipFacts.add(String.valueOf(row.get("controlledFileId"))) || !String.valueOf(e.sourceFileId()).equals(row.get("sourceFileId")) || !e.sourceSha256().equals(row.get("sourceSha256")) || !String.valueOf(manifest.tenantId()).equals(row.get("tenantId")))throw new IllegalArgumentException("sealed source ownership differs");
                continue;
            }
            if(!"version".equals(row.get("kind")))continue;
            var e=versions.get(row.get("id"));
            if(e==null || !sourceVersions.add(String.valueOf(row.get("id"))) || !String.valueOf(e.masterId()).equals(row.get("masterId"))
                || !String.valueOf(e.sourceFileId()).equals(row.get("sourceFileId")) || !String.valueOf(manifest.tenantId()).equals(row.get("tenantId"))
                || !e.sourceSha256().equals(row.get("sourceSha256")) || !e.versionNo().equals(row.get("versionNo")))
                throw new IllegalArgumentException("sealed actual version facts differ");
        }
        if(!sourceVersions.equals(versions.keySet()))throw new IllegalArgumentException("sealed actual version facts incomplete");
        if(!ownershipFacts.equals(versions.keySet()) || !storageFacts.equals(bySource.keySet())
            || !claimFacts.equals(manifest.evidence().stream().map(e->String.valueOf(e.claimId())).collect(java.util.stream.Collectors.toSet()))
            || !masterFacts.equals(manifest.evidence().stream().map(e->String.valueOf(e.masterId())).collect(java.util.stream.Collectors.toSet())))
            throw new IllegalArgumentException("sealed claim/master/storage/ownership coverage incomplete");
        if(!identityHash(manifest.evidence()).equals(manifest.scopeIdentitySha256()))throw new IllegalArgumentException("sealed exact scope identity hash differs");
    }
    static String identityHash(List<Evidence> evidence) {
        var rows=evidence.stream().sorted(Comparator.comparing(Evidence::fileId)).map(e-> {
            var row=new TreeMap<String,Object>();row.put("claimId",String.valueOf(e.claimId()));row.put("masterId",String.valueOf(e.masterId()));
            row.put("fileId",String.valueOf(e.fileId()));row.put("sourceFileId",String.valueOf(e.sourceFileId()));
            row.put("sourceNameHex",HexFormat.of().formatHex(e.sourceName().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            row.put("sourceSha256",e.sourceSha256());row.put("sourceSize",String.valueOf(e.sourceSize()));return row;
        }).toList();return DigestUtil.sha256Hex(JsonUtils.toJsonString(rows));
    }
    private static List<String> lines(byte[] body) throws java.io.IOException {
        return java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(body)).toString().lines().toList();
    }
    private static byte[] readBounded(Path path) throws java.io.IOException {
        if(path==null || !Files.isRegularFile(path) || Files.isSymbolicLink(path))throw new IllegalArgumentException("sealed regular evidence artifact required");
        return readBounded(path,8*1024*1024);
    }
    private static byte[] readBounded(Path path,int limit) throws java.io.IOException {
        if(path==null || !Files.isRegularFile(path) || Files.isSymbolicLink(path))throw new IllegalArgumentException("sealed regular evidence artifact required");
        try(var input=Files.newInputStream(path)) {byte[] body=input.readNBytes(limit+1);if(body.length>limit)throw new IllegalArgumentException("sealed artifact exceeds reviewed byte bound");return body;}
    }
    private void validate() {
        var m=Objects.requireNonNull(manifest);
        if(m.schemaVersion()!=1 || !"ruoyi-vue-pro".equals(m.database()) || m.serverUuid()==null || !m.serverUuid().matches("[0-9a-f-]{36}")
            || !Long.valueOf(1).equals(m.tenantId()) || m.scopeId()==null || !m.scopeId().matches("[A-Za-z0-9_-]{1,64}")
            || m.verifiedAt()==null || m.verifiedAt().getNano()%1000!=0 || m.reason()==null || m.reason().isBlank() || m.reason().length()>500
            || m.requestId()==null || m.requestId().isBlank() || m.requestId().length()>128 || m.evidence()==null || m.evidence().isEmpty())
            throw new IllegalArgumentException("sealed registration identity/reason incomplete");
        for(var sha:List.of(manifestSha256,m.factsSha256(),m.bytesReceiptSha256(),m.userDecisionSha256(),m.scopeIdentitySha256()))hash(sha);
        var versions=new HashSet<Long>();var sources=new HashSet<Long>();var claims=new HashMap<Long,Long>();
        for(var e:m.evidence()) {
            if(e==null)throw new IllegalArgumentException("evidence missing");
            for(var id:List.of(e.claimId(),e.masterId(),e.fileId(),e.sourceFileId(),e.configId()))if(id==null || id<1)throw new IllegalArgumentException("exact positive identity required");
            if(!versions.add(e.fileId()) || !sources.add(e.sourceFileId()) || e.sourceSize()<0 || !Long.valueOf(28).equals(e.configId()) || e.storageType()!=20
                || e.sourcePath()==null || e.sourcePath().isEmpty() || e.versionNo()==null || e.versionNo().isBlank() || e.versionNo().length()>64
                || e.sourceName()==null || e.sourceName().isBlank() || e.sourceName().codePointCount(0,e.sourceName().length())>256
                || e.sourceName().contains("/") || e.sourceName().contains("\\") || e.claimName()==null
                || e.claimProjectId()!=null || e.claimLeafId()!=null || e.claimNumber()!=null
                || e.storageEndpoint()==null || e.storageEndpoint().isBlank() || e.storageBucket()==null || e.storageBucket().isBlank()
                || e.storageRegion()==null || e.storageRegion().isBlank() || !e.storagePathStyle())
                throw new IllegalArgumentException("sealed source/legacy identity is invalid or outside reviewed config28 scope");
            Long old=claims.putIfAbsent(e.claimId(),e.masterId());if(old!=null && !old.equals(e.masterId()))throw new IllegalArgumentException("claim owner is ambiguous");
            var endpoint=java.net.URI.create(e.storageEndpoint());
            if(!Set.of("http","https").contains(endpoint.getScheme()) || !Set.of("127.0.0.1","localhost").contains(endpoint.getHost())
                || endpoint.getPort()!=9000 || endpoint.getUserInfo()!=null || endpoint.getQuery()!=null || endpoint.getFragment()!=null
                || endpoint.getPath()!=null && !Set.of("","/").contains(endpoint.getPath()) || !"us-east-1".equals(e.storageRegion()))
                throw new IllegalArgumentException("source locator outside reviewed local config28 scope");
            for(var sha:List.of(e.sourceSha256(),e.filePreimageSha256(),e.masterPreimageSha256(),e.claimPreimageSha256(),e.storagePreimageSha256()))hash(sha);
        }
    }
    static void hash(String sha){if(sha==null || !sha.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("exact lower hexadecimal SHA-256 required");}
    static String rowHash(Map<String,Object> row) {
        var canonical=new TreeMap<String,Object>();
        row.forEach((key,value)->canonical.put(key.toLowerCase(Locale.ROOT),value==null?null:
            value instanceof byte[] bytes?HexFormat.of().formatHex(bytes):
            value instanceof java.sql.Timestamp date?date.toLocalDateTime().toString():String.valueOf(value)));
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(canonical));
    }
}
