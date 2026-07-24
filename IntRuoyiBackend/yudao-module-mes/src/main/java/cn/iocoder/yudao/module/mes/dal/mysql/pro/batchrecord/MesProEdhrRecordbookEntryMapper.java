package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookEntryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrRecordbookEntryMapper extends BaseMapperX<MesProEdhrRecordbookEntryDO> {

    default PageResult<MesProEdhrRecordbookEntryDO> selectPage(MesProEdhrRecordbookEntryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrRecordbookEntryDO>()
                .eqIfPresent(MesProEdhrRecordbookEntryDO::getRecordbookId, reqVO.getRecordbookId())
                .likeIfPresent(MesProEdhrRecordbookEntryDO::getEntryCode, reqVO.getEntryCode())
                .eqIfPresent(MesProEdhrRecordbookEntryDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrRecordbookEntryDO::getSubmittedBy, reqVO.getSubmittedBy())
                .betweenIfPresent(MesProEdhrRecordbookEntryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrRecordbookEntryDO::getId));
    }

    default MesProEdhrRecordbookEntryDO selectByEntryCode(String entryCode) {
        return selectOne(MesProEdhrRecordbookEntryDO::getEntryCode, entryCode);
    }

    default MesProEdhrRecordbookEntryDO selectByRecordbookIdAndIdempotencyKey(Long recordbookId,
                                                                              String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrRecordbookEntryDO>()
                .eq(MesProEdhrRecordbookEntryDO::getRecordbookId, recordbookId)
                .eq(MesProEdhrRecordbookEntryDO::getIdempotencyKey, idempotencyKey));
    }
}
