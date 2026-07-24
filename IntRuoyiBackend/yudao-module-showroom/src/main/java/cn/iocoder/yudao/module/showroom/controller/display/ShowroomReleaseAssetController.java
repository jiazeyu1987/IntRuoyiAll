package cn.iocoder.yudao.module.showroom.controller.display;

import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseManifestQueryService;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/showroom/assets")
@Validated
public class ShowroomReleaseAssetController {

    private final ShowroomReleaseManifestQueryService manifestQueryService;

    public ShowroomReleaseAssetController(ShowroomReleaseManifestQueryService manifestQueryService) {
        this.manifestQueryService = manifestQueryService;
    }

    @GetMapping("/{assetId}/{contentHash}")
    @PermitAll
    public ResponseEntity<?> getAsset(@PathVariable("assetId") String assetId,
                                      @PathVariable("contentHash") String contentHash,
                                      @RequestHeader HttpHeaders headers) {
        return manifestQueryService.getAssetResponse(assetId, contentHash, headers);
    }
}
