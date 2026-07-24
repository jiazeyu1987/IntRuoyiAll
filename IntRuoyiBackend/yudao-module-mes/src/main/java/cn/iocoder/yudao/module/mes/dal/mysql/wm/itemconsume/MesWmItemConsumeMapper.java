package cn.iocoder.yudao.module.mes.dal.mysql.wm.itemconsume;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 物料消耗记录 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesWmItemConsumeMapper extends BaseMapperX<MesWmItemConsumeDO> {

    default MesWmItemConsumeDO selectByFeedbackId(Long feedbackId) {
        return selectOne(MesWmItemConsumeDO::getFeedbackId, feedbackId);
    }

    default Long selectCountByWorkOrderId(Long workOrderId) {
        return selectCount(MesWmItemConsumeDO::getWorkOrderId, workOrderId);
    }

}
