package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.permission.DccNasPrincipalMappingService;
import lombok.Data;

import java.util.List;

@Data
public class DccNasUnmappedPrincipalRespVO {

    private List<UnmappedPrincipal> list;

    public static DccNasUnmappedPrincipalRespVO of(
            List<DccNasPrincipalMappingService.UnmappedPrincipal> principals) {
        DccNasUnmappedPrincipalRespVO respVO = new DccNasUnmappedPrincipalRespVO();
        respVO.setList(principals.stream().map(UnmappedPrincipal::of).toList());
        return respVO;
    }

    @Data
    public static class UnmappedPrincipal {

        private String sourceAuthority;
        private String sourceSid;
        private String sourceName;
        private Integer aceCount;
        private String firstNasPath;

        private static UnmappedPrincipal of(DccNasPrincipalMappingService.UnmappedPrincipal principal) {
            UnmappedPrincipal respVO = new UnmappedPrincipal();
            respVO.setSourceAuthority(principal.sourceAuthority());
            respVO.setSourceSid(principal.sourceSid());
            respVO.setSourceName(principal.sourceName());
            respVO.setAceCount(principal.aceCount());
            respVO.setFirstNasPath(principal.firstNasPath());
            return respVO;
        }
    }
}
