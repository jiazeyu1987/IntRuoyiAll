package cn.iocoder.yudao.module.showroom.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 展厅客户端下载")
@RestController
@RequestMapping("/showroom/client-downloads")
@Validated
public class ShowroomClientDownloadController {

    private final ShowroomClientDownloadService downloadService;

    public ShowroomClientDownloadController(ShowroomClientDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/android")
    @Operation(summary = "下载展厅 Android 客户端")
    public ResponseEntity<Resource> downloadAndroidClient() {
        return downloadService.download(ShowroomClientDownloadFile.ANDROID);
    }

    @GetMapping("/desktop-win7")
    @Operation(summary = "下载展厅 Win7 桌面客户端")
    public ResponseEntity<Resource> downloadDesktopClient() {
        return downloadService.download(ShowroomClientDownloadFile.DESKTOP_WIN7);
    }
}
