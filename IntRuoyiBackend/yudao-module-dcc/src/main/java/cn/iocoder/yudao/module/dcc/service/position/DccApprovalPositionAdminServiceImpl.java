package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_AMBIGUOUS;

@Service
@Validated
public class DccApprovalPositionAdminServiceImpl implements DccApprovalPositionAdminService {

    private static final String INTAUTH_SOURCE_PREFIX = "INTAUTH:";
    private static final String INTAUTH_CODE_PREFIX = "INTAUTH-";
    private static final Set<String> VISIBLE_LOCAL_POSITION_CODES = Set.of(
            "LOCAL-ROLE-APPROVER-DEPT",
            "LOCAL-ROLE-AUTH-REP"
    );

    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccPositionAssignmentMapper assignmentMapper;
    @Resource
    private DccIntAuthPositionClient intAuthPositionClient;

    @Override
    public List<DccApprovalPositionDO> getPositionList() {
        return positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> parseIntAuthSourceId(item.getSource()) != null
                        || VISIBLE_LOCAL_POSITION_CODES.contains(item.getCode()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccApprovalPositionDO createPosition(String name, String changeReason) {
        DccIntAuthPositionClient.IntAuthPosition intAuthPosition = intAuthPositionClient.createPosition(name, changeReason);
        return upsertPositionFromIntAuth(intAuthPosition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccApprovalPositionImportResult importPositionsFromIntAuth() {
        return syncPositionsFromIntAuth(intAuthPositionClient.listPositions());
    }

    @Override
    public List<DccPositionAssignmentDO> getAssignments(Long positionId) {
        validatePositionExists(positionId);
        return assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, positionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccPositionAssignmentDO> replaceAssignments(Long positionId, List<DccPositionAssignmentSaveReqVO> reqVOList) {
        DccApprovalPositionDO position = requirePosition(positionId);
        if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position) && !reqVOList.isEmpty()) {
            throw exception(APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED, position.getName());
        }
        assignmentMapper.delete(DccPositionAssignmentDO::getPositionId, positionId);
        if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position)) {
            return List.of();
        }
        List<DccPositionAssignmentDO> assignments = CollectionUtils.convertList(reqVOList, reqVO -> {
            DccPositionAssignmentDO assignment = BeanUtils.toBean(reqVO, DccPositionAssignmentDO.class);
            assignment.setPositionId(positionId);
            return assignment;
        });
        assignments.forEach(assignmentMapper::insert);
        return assignments;
    }

    private void validatePositionExists(Long positionId) {
        requirePosition(positionId);
    }

    private DccApprovalPositionDO requirePosition(Long positionId) {
        DccApprovalPositionDO position = positionMapper.selectById(positionId);
        if (position == null) {
            throw exception(APPROVAL_POSITION_NOT_EXISTS);
        }
        return position;
    }

    private DccApprovalPositionImportResult syncPositionsFromIntAuth(List<DccIntAuthPositionClient.IntAuthPosition> intAuthPositions) {
        List<DccApprovalPositionDO> localPositions = positionMapper.selectList();
        Map<Long, DccApprovalPositionDO> mappedPositions = buildMappedPositions(localPositions);
        Map<String, List<DccApprovalPositionDO>> unmappedPositionsByName = localPositions.stream()
                .filter(item -> parseIntAuthSourceId(item.getSource()) == null)
                .collect(Collectors.groupingBy(item -> normalizeName(item.getName())));
        Set<Long> activeSourceIds = intAuthPositions.stream()
                .map(DccIntAuthPositionClient.IntAuthPosition::id)
                .collect(Collectors.toSet());
        int createdCount = 0;
        int adoptedCount = 0;
        int updatedCount = 0;
        int disabledCount = 0;

        for (DccIntAuthPositionClient.IntAuthPosition intAuthPosition : intAuthPositions) {
            DccApprovalPositionDO mappedPosition = mappedPositions.get(intAuthPosition.id());
            if (mappedPosition != null) {
                updateSyncedPosition(mappedPosition, intAuthPosition);
                updatedCount++;
                continue;
            }

            List<DccApprovalPositionDO> sameNamePositions = unmappedPositionsByName
                    .getOrDefault(normalizeName(intAuthPosition.name()), List.of());
            if (sameNamePositions.size() > 1) {
                throw exception(INTAUTH_POSITION_SYNC_AMBIGUOUS, intAuthPosition.name());
            }
            if (sameNamePositions.size() == 1) {
                adoptExistingLocalPosition(sameNamePositions.get(0), intAuthPosition);
                adoptedCount++;
                continue;
            }

            createSyncedPosition(intAuthPosition);
            createdCount++;
        }

        disabledCount += (int) localPositions.stream()
                .filter(item -> {
                    Long sourceId = parseIntAuthSourceId(item.getSource());
                    return sourceId != null && !activeSourceIds.contains(sourceId) && Boolean.TRUE.equals(item.getActive());
                })
                .peek(item -> positionMapper.updateById(DccApprovalPositionDO.builder()
                        .id(item.getId())
                        .active(Boolean.FALSE)
                        .build()))
                .count();

        return new DccApprovalPositionImportResult(intAuthPositions.size(), createdCount, adoptedCount, updatedCount,
                disabledCount);
    }

    private DccApprovalPositionDO upsertPositionFromIntAuth(DccIntAuthPositionClient.IntAuthPosition intAuthPosition) {
        List<DccApprovalPositionDO> localPositions = positionMapper.selectList();
        Map<Long, DccApprovalPositionDO> mappedPositions = buildMappedPositions(localPositions);
        Map<String, List<DccApprovalPositionDO>> unmappedPositionsByName = localPositions.stream()
                .filter(item -> parseIntAuthSourceId(item.getSource()) == null)
                .collect(Collectors.groupingBy(item -> normalizeName(item.getName())));

        DccApprovalPositionDO mappedPosition = mappedPositions.get(intAuthPosition.id());
        if (mappedPosition != null) {
            updateSyncedPosition(mappedPosition, intAuthPosition);
            return positionMapper.selectById(mappedPosition.getId());
        }

        List<DccApprovalPositionDO> sameNamePositions = unmappedPositionsByName
                .getOrDefault(normalizeName(intAuthPosition.name()), List.of());
        if (sameNamePositions.size() > 1) {
            throw exception(INTAUTH_POSITION_SYNC_AMBIGUOUS, intAuthPosition.name());
        }
        if (sameNamePositions.size() == 1) {
            DccApprovalPositionDO existingPosition = sameNamePositions.get(0);
            adoptExistingLocalPosition(existingPosition, intAuthPosition);
            return positionMapper.selectById(existingPosition.getId());
        }

        createSyncedPosition(intAuthPosition);
        return positionMapper.selectOne(DccApprovalPositionDO::getSource, buildIntAuthSource(intAuthPosition.id()));
    }

    private Map<Long, DccApprovalPositionDO> buildMappedPositions(List<DccApprovalPositionDO> localPositions) {
        Map<Long, List<DccApprovalPositionDO>> groupedBySourceId = localPositions.stream()
                .filter(item -> parseIntAuthSourceId(item.getSource()) != null)
                .collect(Collectors.groupingBy(item -> parseIntAuthSourceId(item.getSource())));
        groupedBySourceId.forEach((sourceId, positions) -> {
            if (positions.size() > 1) {
                throw exception(INTAUTH_POSITION_SYNC_AMBIGUOUS, String.valueOf(sourceId));
            }
        });
        return groupedBySourceId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
    }

    private void updateSyncedPosition(DccApprovalPositionDO existingPosition,
                                      DccIntAuthPositionClient.IntAuthPosition intAuthPosition) {
        positionMapper.updateById(DccApprovalPositionDO.builder()
                .id(existingPosition.getId())
                .name(intAuthPosition.name())
                .active(Boolean.TRUE)
                .source(buildIntAuthSource(intAuthPosition.id()))
                .build());
    }

    private void adoptExistingLocalPosition(DccApprovalPositionDO existingPosition,
                                            DccIntAuthPositionClient.IntAuthPosition intAuthPosition) {
        positionMapper.updateById(DccApprovalPositionDO.builder()
                .id(existingPosition.getId())
                .name(intAuthPosition.name())
                .active(Boolean.TRUE)
                .source(buildIntAuthSource(intAuthPosition.id()))
                .build());
    }

    private void createSyncedPosition(DccIntAuthPositionClient.IntAuthPosition intAuthPosition) {
        DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                .code(INTAUTH_CODE_PREFIX + intAuthPosition.id())
                .name(intAuthPosition.name())
                .active(Boolean.TRUE)
                .source(buildIntAuthSource(intAuthPosition.id()))
                .remark("Synchronized from IntAuth")
                .build();
        positionMapper.insert(position);
    }

    private Long parseIntAuthSourceId(String source) {
        if (source == null || !source.startsWith(INTAUTH_SOURCE_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(source.substring(INTAUTH_SOURCE_PREFIX.length()));
        } catch (NumberFormatException ex) {
            throw exception(INTAUTH_POSITION_SYNC_AMBIGUOUS, source);
        }
    }

    private String buildIntAuthSource(Long sourceId) {
        return INTAUTH_SOURCE_PREFIX + sourceId;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
