package cn.iocoder.yudao.server.profileworkbench;
public record WorkbenchVisibilityRequest(String queryVersion, String contextEpoch, String taskKey, String operation) { }
