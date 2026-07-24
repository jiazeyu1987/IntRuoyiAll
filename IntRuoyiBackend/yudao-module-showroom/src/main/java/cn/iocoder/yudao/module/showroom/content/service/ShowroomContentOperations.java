package cn.iocoder.yudao.module.showroom.content.service;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemOption;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductOption;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomVersionAudit;

import java.util.List;
import java.util.Optional;

public interface ShowroomContentOperations {

    ShowroomCompanyRevision saveCompanyDraft(ShowroomCompanyDraft draft);

    ShowroomCompanySnapshot getCompany(Long companyId);

    List<ShowroomCompanySnapshot> listCompanies();

    ShowroomCompanyRevision getCompanyRevision(Long revisionId);

    ShowroomCompanyRevision requireCurrentCompanyRevision();

    Optional<ShowroomCompanyRevision> findCurrentOrLatestCompanyRevision();

    ShowroomCompanyRevision publishCompanyRevision(Long revisionId, Long operatorId);

    ShowroomProductRevision saveProductDraft(ShowroomProductDraft draft);

    ShowroomProductSnapshot getProduct(Long productId);

    ShowroomProductRevision getProductRevision(Long revisionId);

    ShowroomProductRevision getLatestProductRevision(Long productId);

    ShowroomProductRevision getCurrentOrLatestProductRevision(Long productId);

    ShowroomProductRevision requireCurrentProductRevision(Long productId);

    List<ShowroomProductSnapshot> listProducts();

    List<ShowroomProductSnapshot> listProducts(String keyword, Integer pageNo, Integer pageSize);

    List<ShowroomHallProductOption> listHallProductOptions();

    void deleteProduct(Long productId);

    ShowroomProductRevision publishProductRevision(Long revisionId, Long operatorId);

    ShowroomAwardRevision saveAwardDraft(ShowroomAwardDraft draft);

    ShowroomAwardSnapshot getAward(Long awardId);

    ShowroomAwardRevision getAwardRevision(Long revisionId);

    ShowroomAwardRevision getLatestAwardRevision(Long awardId);

    ShowroomAwardRevision getCurrentOrLatestAwardRevision(Long awardId);

    ShowroomAwardRevision requireCurrentAwardRevision(Long awardId);

    List<ShowroomAwardSnapshot> listAwards();

    void deleteAward(Long awardId);

    ShowroomAwardRevision publishAwardRevision(Long revisionId, Long operatorId);

    ShowroomHall createHall(String hallCode, String name, String nameEn, String description, String descriptionEn);

    ShowroomHall updateHall(Long hallId, String name, String nameEn, String description, String descriptionEn);

    ShowroomHall updateHallCanvasBackground(Long hallId, String canvasBackgroundImageUrl);

    ShowroomHall replaceHallProductMappings(Long hallId, List<ShowroomHallProductMapping> mappings);

    ShowroomHall replaceHallCanvasLayout(Long hallId, List<ShowroomHallProductMapping> mappings);

    ShowroomHall replaceHallItemMappings(Long hallId, List<ShowroomHallItemMapping> mappings);

    ShowroomHall replaceHallItemCanvasLayout(Long hallId, List<ShowroomHallItemMapping> mappings);

    ShowroomHall getHall(Long hallId);

    List<ShowroomHall> listHalls();

    List<ShowroomHall> listHalls(String keyword, Integer pageNo, Integer pageSize);

    List<ShowroomHallItemOption> listHallItemOptions();

    void deleteHall(Long hallId);

    List<ShowroomVersionAudit> versionAudits(String targetType, Long targetId);

}
