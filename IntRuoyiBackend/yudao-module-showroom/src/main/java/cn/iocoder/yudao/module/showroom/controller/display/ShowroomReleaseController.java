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
@RequestMapping("/showroom/release")
@Validated
public class ShowroomReleaseController {

    private final ShowroomReleaseManifestQueryService manifestQueryService;

    public ShowroomReleaseController(ShowroomReleaseManifestQueryService manifestQueryService) {
        this.manifestQueryService = manifestQueryService;
    }

    @GetMapping("/current")
    @PermitAll
    public ResponseEntity<String> getCurrent(@RequestHeader HttpHeaders headers) {
        return manifestQueryService.getCurrentResponse(headers);
    }

    @GetMapping("/{releaseId}/manifest")
    @PermitAll
    public ResponseEntity<String> getManifest(@PathVariable("releaseId") String releaseId,
                                              @RequestHeader HttpHeaders headers) {
        return manifestQueryService.getManifestResponse(releaseId, headers);
    }

    @GetMapping("/{releaseId}/documents/{documentId}.json")
    @PermitAll
    public ResponseEntity<String> getDocument(@PathVariable("releaseId") String releaseId,
                                              @PathVariable("documentId") String documentId,
                                              @RequestHeader HttpHeaders headers) {
        return manifestQueryService.getDocumentResponse(releaseId, documentId, headers);
    }
}
