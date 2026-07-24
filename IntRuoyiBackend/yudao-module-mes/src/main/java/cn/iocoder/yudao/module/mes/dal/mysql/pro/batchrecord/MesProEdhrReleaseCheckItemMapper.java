package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseCheckItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReleaseCheckItemMapper extends BaseMapperX<MesProEdhrReleaseCheckItemDO> {

    default PageResult<MesProEdhrReleaseCheckItemDO> selectPage(MesProEdhrReleaseCheckItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrReleaseCheckItemDO>()
                .eqIfPresent(MesProEdhrReleaseCheckItemDO::getReleaseTransactionId, reqVO.getReleaseTransactionId())
                .eqIfPresent(MesProEdhrReleaseCheckItemDO::getCheckCategory, reqVO.getCheckCategory())
                .eqIfPresent(MesProEdhrReleaseCheckItemDO::getCheckResult, reqVO.getCheckResult())
                .eqIfPresent(MesProEdhrReleaseCheckItemDO::getItemStatus, reqVO.getItemStatus())
                .likeIfPresent(MesProEdhrReleaseCheckItemDO::getSourceObjectCode, reqVO.getSourceObjectCode())
                .orderByDesc(MesProEdhrReleaseCheckItemDO::getCheckedAt)
                .orderByDesc(MesProEdhrReleaseCheckItemDO::getId));
    }

    default void closeOpenByReleaseTransactionId(Long releaseTransactionId) {
        update(null, new LambdaUpdateWrapper<MesProEdhrReleaseCheckItemDO>()
                .set(MesProEdhrReleaseCheckItemDO::getItemStatus, "SUPERSEDED")
                .eq(MesProEdhrReleaseCheckItemDO::getReleaseTransactionId, releaseTransactionId)
                .eq(MesProEdhrReleaseCheckItemDO::getItemStatus, "OPEN"));
    }
}
