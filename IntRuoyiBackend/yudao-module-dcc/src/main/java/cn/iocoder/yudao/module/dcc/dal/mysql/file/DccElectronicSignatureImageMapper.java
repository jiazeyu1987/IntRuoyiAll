package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureImageDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DccElectronicSignatureImageMapper extends BaseMapperX<DccElectronicSignatureImageDO> {

    default DccElectronicSignatureImageDO selectActiveByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<DccElectronicSignatureImageDO>()
                .eq(DccElectronicSignatureImageDO::getUserId, userId)
                .eq(DccElectronicSignatureImageDO::getActive, Boolean.TRUE)
                .eq(DccElectronicSignatureImageDO::getImageStatus, "ACTIVE"));
    }

    default List<DccElectronicSignatureImageDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<DccElectronicSignatureImageDO>()
                .eq(DccElectronicSignatureImageDO::getUserId, userId)
                .orderByDesc(DccElectronicSignatureImageDO::getVersionNo)
                .orderByDesc(DccElectronicSignatureImageDO::getId));
    }

    default Integer selectMaxVersionNoByUserId(Long userId) {
        DccElectronicSignatureImageDO latest = selectOne(new LambdaQueryWrapperX<DccElectronicSignatureImageDO>()
                .eq(DccElectronicSignatureImageDO::getUserId, userId)
                .orderByDesc(DccElectronicSignatureImageDO::getVersionNo)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getVersionNo();
    }

    default int deactivateActiveByUserId(Long userId, Long exceptImageId, LocalDateTime disabledAt) {
        return update(null, new LambdaUpdateWrapper<DccElectronicSignatureImageDO>()
                .eq(DccElectronicSignatureImageDO::getUserId, userId)
                .eq(DccElectronicSignatureImageDO::getActive, Boolean.TRUE)
                .ne(exceptImageId != null, DccElectronicSignatureImageDO::getId, exceptImageId)
                .set(DccElectronicSignatureImageDO::getActive, Boolean.FALSE)
                .set(DccElectronicSignatureImageDO::getImageStatus, "SUPERSEDED")
                .set(DccElectronicSignatureImageDO::getDisabledAt, disabledAt)
                .set(DccElectronicSignatureImageDO::getDisableReason, "启用新签名图片自动停用旧版本"));
    }
}
