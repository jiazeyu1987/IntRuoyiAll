package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class DccRegistrationCertificateChangeApplyReqVO {

    @NotNull
    @Positive
    private Integer expectedRowVersion;
    @NotNull
    private LocalDate approvalDate;
    private List<@Size(max = 64) String> changeTypes;
    private Map<@Size(max = 64) String, @Size(max = 4096) String> structuredValues;
    @Size(max = 4096)
    private String otherDescription;
    private Boolean entrustedProduction;
    private Boolean selfProduction;
    private String entrustedEnterprisesJson;
    @NotNull
    private MultipartFile file;
}
