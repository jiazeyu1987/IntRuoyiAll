package cn.iocoder.yudao.module.mes.service.md.item.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee.MesKingdeeBomListRespVO;

import java.time.LocalDateTime;

public interface MesKingdeeBomListService {

    PageResult<MesKingdeeBomListRespVO> getPage(MesKingdeeBomListPageReqVO pageReqVO);

    int syncAll();

    int syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
