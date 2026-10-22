package cn.iocoder.yudao.server.profileworkbench;
import java.time.Instant;
import java.util.List;
public record WorkbenchContextResponse(int contractVersion, String registryRevision, List<String> sources,
    String contextEpoch, String visibilityVersion, String queryVersion,
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) Instant expiresAt, WorkbenchQuerySpec querySpec) { }
