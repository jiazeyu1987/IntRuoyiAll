package cn.iocoder.yudao.server.profileworkbench;
import java.util.List;
public record WorkbenchContextRequest(String registryRevision, List<String> sources, String contextEpoch, WorkbenchQuerySpec querySpec) { }
