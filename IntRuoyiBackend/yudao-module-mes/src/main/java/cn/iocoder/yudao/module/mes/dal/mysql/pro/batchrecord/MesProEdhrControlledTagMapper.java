package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrControlledTagDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrControlledTagMapper extends BaseMapperX<MesProEdhrControlledTagDO> {

    String STATUS_ACTIVE = "ACTIVE";

    default PageResult<MesProEdhrControlledTagDO> selectPage(MesProEdhrControlledTagPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrControlledTagDO>()
                .likeIfPresent(MesProEdhrControlledTagDO::getTagCode, reqVO.getTagCode())
                .likeIfPresent(MesProEdhrControlledTagDO::getTagName, reqVO.getTagName())
                .eqIfPresent(MesProEdhrControlledTagDO::getTagType, reqVO.getTagType())
                .eqIfPresent(MesProEdhrControlledTagDO::getTagStatus, reqVO.getTagStatus())
                .betweenIfPresent(MesProEdhrControlledTagDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrControlledTagDO::getId));
    }

    default MesProEdhrControlledTagDO selectByTagCode(String tagCode) {
        return selectOne(MesProEdhrControlledTagDO::getTagCode, tagCode);
    }

    default MesProEdhrControlledTagDO selectActiveByTagCode(String tagCode) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrControlledTagDO>()
                .eq(MesProEdhrControlledTagDO::getTagCode, tagCode)
                .eq(MesProEdhrControlledTagDO::getTagStatus, STATUS_ACTIVE)
                .orderByDesc(MesProEdhrControlledTagDO::getActiveAt)
                .orderByDesc(MesProEdhrControlledTagDO::getId));
    }
}
