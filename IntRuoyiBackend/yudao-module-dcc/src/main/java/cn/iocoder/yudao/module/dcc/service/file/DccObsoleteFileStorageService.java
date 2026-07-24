package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DccObsoleteFileStorageService {

    static final String OBSOLETE_FOLDER_NAME = "作废文件";

    @Resource
    private FileService fileService;

    public void moveControlledFileArtifactsToObsoleteFolder(DccControlledFileDO controlledFile) {
        if (controlledFile == null) {
            throw new IllegalStateException("Controlled file is missing for obsolete move");
        }
        List<FileDO> artifactFiles = resolveArtifactFiles(controlledFile);
        for (FileDO artifactFile : artifactFiles) {
            moveArtifactToObsoleteFolder(artifactFile);
        }
    }

    private List<FileDO> resolveArtifactFiles(DccControlledFileDO controlledFile) {
        Set<Long> artifactIds = new LinkedHashSet<>();
        addArtifactId(artifactIds, controlledFile.getSourceFileId());
        addArtifactId(artifactIds, controlledFile.getOriginalFileId());
        addArtifactId(artifactIds, controlledFile.getDrawingPdfFileId());
        addArtifactId(artifactIds, controlledFile.getTrainingRecordFileId());
        addArtifactId(artifactIds, controlledFile.getPublishedFileId());
        addArtifactId(artifactIds, controlledFile.getStampedFileId());

        List<FileDO> artifactFiles = new ArrayList<>();
        for (Long artifactId : artifactIds) {
            FileDO artifactFile = fileService.getFile(artifactId);
            if (artifactFile == null) {
                throw new IllegalStateException("Controlled file artifact " + artifactId
                        + " is missing for obsolete move");
            }
            artifactFiles.add(artifactFile);
        }
        return artifactFiles;
    }

    private void addArtifactId(Set<Long> artifactIds, Long artifactId) {
        if (artifactId != null) {
            artifactIds.add(artifactId);
        }
    }

    private void moveArtifactToObsoleteFolder(FileDO artifactFile) {
        String sourcePath = normalizePath(artifactFile.getPath(), artifactFile.getId());
        if (isAlreadyInObsoleteFolder(sourcePath)) {
            return;
        }
        fileService.moveFile(artifactFile.getId(), buildObsoleteTargetPath(artifactFile, sourcePath));
    }

    private String buildObsoleteTargetPath(FileDO artifactFile, String sourcePath) {
        int slashIndex = sourcePath.lastIndexOf('/');
        String parentPath = slashIndex >= 0 ? sourcePath.substring(0, slashIndex) : "";
        String fileName = slashIndex >= 0 ? sourcePath.substring(slashIndex + 1) : sourcePath;
        fileName = resolveFileName(fileName, artifactFile);
        String obsoleteDirectory = StrUtil.isBlank(parentPath)
                ? OBSOLETE_FOLDER_NAME : parentPath + "/" + OBSOLETE_FOLDER_NAME;
        return obsoleteDirectory + "/" + artifactFile.getId() + "/" + fileName;
    }

    private String resolveFileName(String sourceFileName, FileDO artifactFile) {
        String fileName = StrUtil.blankToDefault(sourceFileName, artifactFile.getName());
        fileName = StrUtil.blankToDefault(fileName, String.valueOf(artifactFile.getId()));
        fileName = fileName.replace('\\', '/');
        int slashIndex = fileName.lastIndexOf('/');
        return slashIndex >= 0 ? fileName.substring(slashIndex + 1) : fileName;
    }

    private String normalizePath(String path, Long artifactId) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalStateException("Controlled file artifact " + artifactId
                    + " path is missing for obsolete move");
        }
        String normalized = StrUtil.trim(path).replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return StrUtil.removePrefix(normalized, "/");
    }

    private boolean isAlreadyInObsoleteFolder(String path) {
        return StrUtil.equals(path, OBSOLETE_FOLDER_NAME)
                || StrUtil.startWith(path, OBSOLETE_FOLDER_NAME + "/")
                || StrUtil.contains(path, "/" + OBSOLETE_FOLDER_NAME + "/");
    }
}
