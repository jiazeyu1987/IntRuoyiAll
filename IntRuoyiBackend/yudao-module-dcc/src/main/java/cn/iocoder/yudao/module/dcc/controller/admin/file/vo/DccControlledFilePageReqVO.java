package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccControlledFilePageReqVO extends PageParam {

    private Long categoryId;
    private Long directoryId;
    private Boolean includeDescendantDirectories;
    private Long requesterId;
    private String status;
    private String processType;
    private String keyword;
    private Boolean latestVersionOnly;
    private Long dccProjectCodeId;
    private Long fileTypeTaxonomyId;
    private List<Long> fileTypeTaxonomyIds;
    private List<FileTypeTaxonomyPathFilter> fileTypeTaxonomyPaths;
    private String recognitionStatus;
    private Long batchRecognitionTaskId;
    private QuickFilter quickFilter;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileTypeTaxonomyPathFilter {

        private String level1;
        private String level2;
        private String level3;
        private String level4;
        private String level5;
    }
}
