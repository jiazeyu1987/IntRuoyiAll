package cn.iocoder.yudao.module.dcc.service.directory;

import java.util.List;

public interface DccIntAuthDirectoryClient {

    List<IntAuthDirectoryNode> listBaselineDirectories();

    record IntAuthDirectoryNode(String nodeId, String parentNodeId, String name) {
    }
}
