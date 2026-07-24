package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRecognitionMigrationImportPreviewRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DccControlledFileMetadataImportExportService {

    List<DccControlledFileMetadataExportExcelVO> getExportList(Long userId, DccControlledFilePageReqVO reqVO);

    byte[] buildImportTemplate();

    byte[] buildExportExcel(Long userId, DccControlledFilePageReqVO reqVO);

    byte[] buildRecognitionRecordExportExcel(Long userId, DccControlledFilePageReqVO reqVO);

    byte[] buildRecognitionMigrationExportExcel(Long userId, DccControlledFilePageReqVO reqVO);

    DccControlledFileMetadataImportPreviewRespVO previewImport(Long userId, MultipartFile file);

    DccControlledFileMetadataImportPreviewRespVO confirmImport(Long userId, MultipartFile file);

    DccControlledFileRecognitionMigrationImportPreviewRespVO previewRecognitionMigrationImport(Long userId,
                                                                                              MultipartFile file);

    DccControlledFileRecognitionMigrationImportPreviewRespVO confirmRecognitionMigrationImport(Long userId,
                                                                                              MultipartFile file);
}
