package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

/** Optional exact source expectations on the existing authorized read resource. */
@Data
public class DccControlledFileObsoleteReworkSourceReqVO {
    @Positive private Long reworkSourceFormInstanceId;
    private String reworkSourceFormInstanceCode;
    private String reworkSourceBpmProcessInstanceId;
    @Positive private Integer reworkSourceAttributeRound;
    @JsonIgnore public boolean hasSource(){return reworkSourceFormInstanceId!=null||reworkSourceFormInstanceCode!=null||reworkSourceBpmProcessInstanceId!=null||reworkSourceAttributeRound!=null;}
    @AssertTrue(message="rework read source identity must be complete and exact")
    @JsonIgnore public boolean isSourceIdentityComplete(){
        return !hasSource()||(reworkSourceFormInstanceId!=null&&reworkSourceFormInstanceId>0
            &&reworkSourceAttributeRound!=null&&reworkSourceAttributeRound>0
            &&exact(reworkSourceFormInstanceCode)&&exact(reworkSourceBpmProcessInstanceId));
    }
    private boolean exact(String value){return value!=null&&!value.isBlank()&&value.equals(value.strip());}
}
