package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePrintRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DCC controlled file print record mapper.
 */
@Mapper
public interface DccControlledFilePrintRecordMapper extends BaseMapperX<DccControlledFilePrintRecordDO> {

    default List<DccControlledFilePrintRecordDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFilePrintRecordDO>()
                .eq(DccControlledFilePrintRecordDO::getControlledFileId, controlledFileId)
                .orderByDesc(DccControlledFilePrintRecordDO::getPrintTime)
                .orderByDesc(DccControlledFilePrintRecordDO::getId));
    }

    default DccControlledFilePrintRecordDO selectByIdAndControlledFileId(Long id, Long controlledFileId) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFilePrintRecordDO>()
                .eq(DccControlledFilePrintRecordDO::getId, id)
                .eq(DccControlledFilePrintRecordDO::getControlledFileId, controlledFileId));
    }
}
