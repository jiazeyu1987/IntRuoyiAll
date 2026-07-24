package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccExternalFileReviewMapper extends BaseMapperX<DccExternalFileReviewDO> {

    default DccExternalFileReviewDO selectByControlledFileId(Long controlledFileId) {
        return selectOne(DccExternalFileReviewDO::getControlledFileId, controlledFileId);
    }

    default void updateByControlledFileId(Long controlledFileId, DccExternalFileReviewDO updateObj) {
        update(updateObj, new LambdaUpdateWrapper<DccExternalFileReviewDO>()
                .eq(DccExternalFileReviewDO::getControlledFileId, controlledFileId));
    }
}
