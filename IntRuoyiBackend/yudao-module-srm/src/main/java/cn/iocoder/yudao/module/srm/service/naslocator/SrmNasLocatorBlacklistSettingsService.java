package cn.iocoder.yudao.module.srm.service.naslocator;

import java.util.List;

public interface SrmNasLocatorBlacklistSettingsService {

    List<String> getPatterns();

    void savePatterns(List<String> patterns);
}
