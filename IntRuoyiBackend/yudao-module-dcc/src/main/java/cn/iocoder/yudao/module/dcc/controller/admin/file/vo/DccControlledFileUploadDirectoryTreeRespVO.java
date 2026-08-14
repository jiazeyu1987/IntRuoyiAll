package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileUploadDirectoryTreeRespVO {

    private Long bindingDirectoryId;
    private String bindingDirectoryPath;
    private Boolean leafBinding;
    private Boolean defaultUnclassified;
    private List<DirectoryNode> children;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectoryNode {
        private Long id;
        private String name;
        private Boolean leaf;
        private List<DirectoryNode> children;
    }
}
