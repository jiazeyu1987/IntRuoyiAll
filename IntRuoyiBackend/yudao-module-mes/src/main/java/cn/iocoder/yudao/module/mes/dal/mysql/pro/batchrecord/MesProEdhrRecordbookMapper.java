package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrRecordbookMapper extends BaseMapperX<MesProEdhrRecordbookDO> {

    default PageResult<MesProEdhrRecordbookDO> selectPage(MesProEdhrRecordbookPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrRecordbookDO>()
                .likeIfPresent(MesProEdhrRecordbookDO::getRecordbookCode, reqVO.getRecordbookCode())
                .likeIfPresent(MesProEdhrRecordbookDO::getRecordbookName, reqVO.getRecordbookName())
                .eqIfPresent(MesProEdhrRecordbookDO::getRecordbookType, reqVO.getRecordbookType())
                .eqIfPresent(MesProEdhrRecordbookDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrRecordbookDO::getOwnerUserId, reqVO.getOwnerUserId())
                .likeIfPresent(MesProEdhrRecordbookDO::getBusinessObjectCode, reqVO.getBusinessObjectCode())
                .betweenIfPresent(MesProEdhrRecordbookDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrRecordbookDO::getId));
    }

    default MesProEdhrRecordbookDO selectByRecordbookCode(String recordbookCode) {
        return selectOne(MesProEdhrRecordbookDO::getRecordbookCode, recordbookCode);
    }
}
