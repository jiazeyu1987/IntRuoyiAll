package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import java.time.LocalDateTime;

/** A member stores both the viewed complete set and whether this node newly supplemented it. */
public record DccRelationStageMemberDO(@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long tenantId,
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long snapshotId,
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long relatedMasterId,
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long selectedControlledFileId,
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long projectId,
        String fileNumberSnapshot, String fileNameSnapshot,String versionNoSnapshot, String origin, boolean supplemental,
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long assigneeUserId,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime dueAt) {}
