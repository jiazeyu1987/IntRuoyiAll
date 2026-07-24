package cn.iocoder.yudao.module.dcc.service.position;

import java.util.List;

public interface DccIntAuthPositionClient {

    List<IntAuthPosition> listPositions();
    IntAuthPosition createPosition(String name, String changeReason);

    record IntAuthPosition(Long id, String name) {
    }

}
