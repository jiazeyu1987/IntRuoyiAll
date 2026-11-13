package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QuickFilterUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignaturePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DccControlledFileSignatureMapper extends BaseMapperX<DccControlledFileSignatureDO> {

    default List<DccControlledFileSignatureDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileSignatureDO::getControlledFileId, controlledFileId);
    }

    default DccControlledFileSignatureDO selectActionSignature(Long controlledFileId, String taskId,
                                                               Long actorId, String actionType) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileSignatureDO>()
                .eq(DccControlledFileSignatureDO::getControlledFileId, controlledFileId)
                .eq(DccControlledFileSignatureDO::getTaskId, taskId)
                .eq(DccControlledFileSignatureDO::getActorId, actorId)
                .eq(DccControlledFileSignatureDO::getActionType, actionType));
    }

    default LambdaQueryWrapperX<DccControlledFileSignatureDO> createPageQuery(DccElectronicSignaturePageReqVO reqVO) {
        LambdaQueryWrapperX<DccControlledFileSignatureDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getControlledFileId, reqVO.getControlledFileId());
        queryWrapper.inIfPresent(DccControlledFileSignatureDO::getControlledFileId, reqVO.getControlledFileIds());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getRevisionId, reqVO.getRevisionId());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getVersionNo, reqVO.getVersionNo());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getActorId, reqVO.getSignerUserId());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getActionType, reqVO.getPersistentActionType());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getMeaningCode, reqVO.getMeaningCode());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getControlledCopyHashStatus,
                reqVO.getControlledCopyHashStatus());
        queryWrapper.eqIfPresent(DccControlledFileSignatureDO::getEvidenceStatus, reqVO.getEvidenceStatus());
        queryWrapper.betweenIfPresent(DccControlledFileSignatureDO::getSignedAt, reqVO.getSignedAt());
        queryWrapper.likeRight(StrUtil.isNotBlank(reqVO.getEvidenceHashShort()),
                DccControlledFileSignatureDO::getEvidenceHash, StrUtil.trim(reqVO.getEvidenceHashShort()));
        QuickFilterUtils.filter(queryWrapper, reqVO.getQuickFilter(), Map.of(
                "versionNo", QuickFilterUtils.QuickFilterField.text(DccControlledFileSignatureDO::getVersionNo),
                "signer", QuickFilterUtils.QuickFilterField.text(DccControlledFileSignatureDO::getActorNicknameSnapshot),
                "role", QuickFilterUtils.QuickFilterField.text(DccControlledFileSignatureDO::getActorRoleNamesSnapshot),
                "action", QuickFilterUtils.QuickFilterField.select(DccControlledFileSignatureDO::getActionType),
                "signedAt", QuickFilterUtils.QuickFilterField.localDateTimeRange(DccControlledFileSignatureDO::getSignedAt)
        ));
        cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.apply(reqVO, queryWrapper,
                DccSignatureListFilterFields.fields());
        return queryWrapper;
    }

    default PageResult<DccControlledFileSignatureDO> selectPage(DccElectronicSignaturePageReqVO reqVO) {
        return selectPage(reqVO, createPageQuery(reqVO)
                .orderByDesc(DccControlledFileSignatureDO::getSignedAt)
                .orderByDesc(DccControlledFileSignatureDO::getId));
    }

    /** Stable descending keyset for real-time evidence predicates; never materializes all candidates. */
    default List<DccControlledFileSignatureDO> selectFilterCandidates(DccElectronicSignaturePageReqVO request,
            java.time.LocalDateTime afterSignedAt, Long afterId, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Signature filter batch must be between 1 and 100");
        var query = createPageQuery(request);
        if (afterId != null) {
            if (afterSignedAt == null) {
                query.isNull(DccControlledFileSignatureDO::getSignedAt).lt(DccControlledFileSignatureDO::getId, afterId);
            } else {
                query.and(w -> w.lt(DccControlledFileSignatureDO::getSignedAt, afterSignedAt)
                        .or().isNull(DccControlledFileSignatureDO::getSignedAt)
                        .or(n -> n.eq(DccControlledFileSignatureDO::getSignedAt, afterSignedAt)
                                .lt(DccControlledFileSignatureDO::getId, afterId)));
            }
        }
        query.orderByDesc(DccControlledFileSignatureDO::getSignedAt).orderByDesc(DccControlledFileSignatureDO::getId)
                .last("LIMIT " + limit);
        return selectList(query);
    }
}
