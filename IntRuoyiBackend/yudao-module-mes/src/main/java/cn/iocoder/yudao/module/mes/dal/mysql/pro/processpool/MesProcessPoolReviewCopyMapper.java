package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProcessPoolReviewCopyMapper extends BaseMapperX<MesProcessPoolReviewCopyDO> {

    default MesProcessPoolReviewCopyDO selectByReviewerSignatureId(Long reviewerSignatureId) {
        if (reviewerSignatureId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolReviewCopyDO>()
                .eq(MesProcessPoolReviewCopyDO::getReviewerSignatureId, reviewerSignatureId));
    }
}
