package cn.iocoder.yudao.module.dcc.service.projectcode.access;

public interface DccProjectAccessService {

    void assertProjectOwner(Long userId, Long projectCodeId);

    void assertProjectEditorOrOwner(Long userId, Long projectCodeId);

}
