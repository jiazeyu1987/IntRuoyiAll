package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryFinalSyncRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface MesDvMachineryFinalSheetSyncService {

    MesDvMachineryFinalSyncRespVO syncFinalSheet(MultipartFile file);
}
