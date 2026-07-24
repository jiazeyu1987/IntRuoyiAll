# 执行日志

- BDD: 正式站点安装目标 release -> Given 正式站点已发布目标 release；When 用户访问 `http://172.30.30.57:8083/`；Then 浏览器端应安装并运行目标 release，不应提示 `SHOWROOM_RELEASE_INSTALL_FAILED`。
- GREEN: experience-preflight -> PASS, 已读取正式服排障、发布边界和 PowerShell 编码门禁；当前仅执行只读 HTTP/静态资源排障。

- READ: docs/experience-index.md -> PASS, 命中正式服排障、发布验证、PowerShell 编码、真实浏览器验证门禁。
- WRITE: docs/request-command-log.md -> PASS, 已记录用户反馈与本轮排障命令。
- GREEN: prod-release-server-resource-probe -> PASS, release current/manifest 可读，194 documents 与 612 assets 均可通过 Website origin 下载。

- GREEN: prod-website-dist-deploy-url-attachments -> PASS, packaged `website-dist-url-attachments-20260708T005836Z.tar.gz`, sha256 `d9bc270d9fcb163a10fc926396db55f4255aa4ef3b3a79dbb82bf201fa7d1ea1`, backup `/opt/intruoyi/runtime/website.dist-backup-20260708T005836Z-before-url-attachments`, active HTML serves `index-C0FU2vqj.js`.

- GREEN: prod-post-deploy-http-health -> PASS, 后端健康、8083 根页面、/showroom、release/current 均可访问。
- GREEN: prod-browser-post-deploy-probe -> PASS, 真实浏览器打开正式 8083 不再出现 `SHOWROOM_RELEASE_INSTALL_FAILED`、`更新失败` 或 `assetId is required`。
