package cn.iocoder.yudao.module.mes.service.md.workstation.importer;

import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.BalloonProcessDeviceMappingImportRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface BalloonProcessDeviceMappingImportService {

    BalloonProcessDeviceMappingImportRespVO importMapping(MultipartFile file, Long workshopId);
}
