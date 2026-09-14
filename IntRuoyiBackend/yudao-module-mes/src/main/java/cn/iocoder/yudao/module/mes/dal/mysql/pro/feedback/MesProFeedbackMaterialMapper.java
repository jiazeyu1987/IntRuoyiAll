package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProFeedbackMaterialMapper extends BaseMapperX<MesProFeedbackMaterialDO> {

    default List<MesProFeedbackMaterialDO> selectListByFeedbackIdForUpdate(Long feedbackId) {
        if (feedbackId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackMaterialDO>()
                .eq(MesProFeedbackMaterialDO::getFeedbackId, feedbackId)
                .orderByAsc(MesProFeedbackMaterialDO::getId)
                .last("FOR UPDATE"));
    }

    default int updateCorrectedMaterialFact(Long id,
                                            BigDecimal outputQuantity,
                                            BigDecimal lossQuantity,
                                            String lossDetailsJson,
                                            String selectedDeviceJson,
                                            String deviceParameterReadingsJson) {
        return update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProFeedbackMaterialDO>()
                        .eq(MesProFeedbackMaterialDO::getId, id)
                        .set(MesProFeedbackMaterialDO::getOutputQuantity, outputQuantity)
                        .set(MesProFeedbackMaterialDO::getLossQuantity, lossQuantity)
                        .set(MesProFeedbackMaterialDO::getLossDetailsJson, lossDetailsJson)
                        .set(MesProFeedbackMaterialDO::getSelectedDeviceJson, selectedDeviceJson)
                        .set(MesProFeedbackMaterialDO::getDeviceParameterReadingsJson,
                                deviceParameterReadingsJson));
    }
}
