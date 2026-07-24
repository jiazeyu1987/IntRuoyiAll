# 执行日志：本机导出正式服导入并手动发布

BDD: 本机导出正式服导入并发布 -> Given 本机 `芋道源码/admin` 有当前展厅产品资料 / When 导出 zip 并在正式服导入后点击“手动发布展厅” / Then 正式服发布成功并 Website 运行新 release。

GREEN: freeze-info rollback -> PASS，已提交反向提交 `b45fbb87af`，导入导出契约不包含冻结信息，`ShowroomProductExcelImportExportIntegrationTest` 54 tests PASS。

GREEN: experience-preflight -> PASS，已读取 PowerShell、server-access、login-access、release-backup-restore；正式服导入与手动发布为用户当前目标明确授权。

GREEN: local-admin-export -> PASS，芋道源码/admin 单请求导出 zip，文件 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260707-local-export-prod-import-publish\evidence\showroom-products-local-admin-20260707T021235Z.zip，大小 599585042 bytes；包含 manifest/product-data.xlsx，未发现冻结信息。

## 2026-07-07 11:05 恢复冻结功能
- BDD: 正式服无冻结字段仍可加载产品列表 -> Given 正式服 showroom_product 表没有 frozen_flag/frozen_hall_count, When 打开展厅产品页和导入 zip, Then 后端不得查询冻结字段且前端不得显示冻结/解冻入口。
- RED: mvn -pl yudao-module-showroom '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomApiRuntimeProductPageTest' test -> FAIL, ProductDetailRespVO/ProductPageRespVO record 构造器因移除 frozen 后递归。
- GREEN: mvn -pl yudao-module-showroom '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomApiRuntimeProductPageTest' test -> PASS, Tests run: 57, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: showroom-admin freeze residue scan -> PASS, 展厅前后端产品冻结 API、按钮、frozen 响应字段、frozen_flag/frozen_hall_count 查询残留均已清除。
- GREEN: pnpm exec vue-tsc --noEmit with NODE_OPTIONS=--max-old-space-size=8192 -> PASS for showroom-admin scoped check; 全量仍有其它模块既有 TS 错误，本任务展厅路径无新增错误。
- GREEN: ShowroomApiRuntimeProductPageTest.productPagePayloadShouldNotExposeFreezeState -> PASS, 产品页 ProductPageRespVO/ProductDetailRespVO 不再暴露 frozen 字段。
BLOCKER: publish-test-freeze-restore-v2 -> FAIL, deploy-release 未传 BackendRepoRoot，脚本回退到维护仓并找不到 sql/mysql；修正为显式使用 clean release backend/frontend worktree 后重试。
BLOCKER: publish-test-freeze-restore-v2-retry -> FAIL, release 模式缺少 NasConfigPath；已确认维护仓配置文件并显式补参重试。

BLOCKER: publish-test-freeze-restore-v2-with-nas -> FAIL, NasConfigPath 误传 YAML；已切换为 runtime-control 生成的 NAS JSON 配置后重试。

BLOCKER: publish-test-freeze-restore-v2-json-nas -> FAIL, NAS 上缺少 release 包；本地包已生成，开始按 Backup/ReleasePackage 上传。

BLOCKER: upload-freeze-restore-v2-to-nas -> FAIL, net use 参数顺序错误触发语法提示；NAS 445 连通，改为 share password /user:username 顺序重试。

BLOCKER: upload-freeze-restore-v2-to-nas-retry -> FAIL, Start-Process 参数数组遇中文共享名仍触发 net use 语法提示；已用等价命令字符串验证 NAS 连接成功，改用该方式上传。

GREEN: upload-freeze-restore-v2-to-nas-string -> PASS, release 包已上传到 Backup/ReleasePackage/20260707_showroom_freeze_restore_v2，文件数和字节数一致。

BLOCKER: publish-test-freeze-restore-v2-after-upload -> FAIL, 本机 Docker CLI 在 docker inspect 时出现 0xc0000005；随后 docker version/ps/inspect 均恢复正常，按瞬时 Docker CLI 异常重试测试服发布。

GREEN: publish-test-freeze-restore-v2-docker-retry -> PASS, 测试服 172.30.30.58 已部署恢复版 20260707_showroom_freeze_restore_v2。

BLOCKER: mark-tested-freeze-restore-v2 -> FAIL, 缺少 SelectedRecoverySetCandidateId；已复用最近成功发布门禁恢复集 restore:20260621-063218 后重试。

BLOCKER: mark-tested-freeze-restore-v2-retry -> FAIL, TestConclusion 含空格和斜杠导致 PowerShell 参数绑定错位；改用无空格短结论重试。

GREEN: mark-tested-freeze-restore-v2-short-conclusion -> PASS, release 20260707_showroom_freeze_restore_v2 已在 NAS 标记为测试通过。

BLOCKER: prod-dry-run-freeze-restore-v2 -> FAIL, run-deploy-precheck-report.ps1 report-only exitCode=2。

BLOCKER: prod-dry-run-freeze-restore-v2 -> FAIL, 直接使用测试服发布后被写入 .env/post-import/preflight 的本地包导致正式预检失败；改用 manifest 声明文件生成隔离干净预检工作区。

BLOCKER: prod-dry-run-freeze-restore-v2-clean -> FAIL, 手写隔离包复制时目标父目录未稳定创建；改为先创建全部父目录再复制。

BLOCKER: prod-dry-run-freeze-restore-v2-clean2 -> FAIL, 长任务路径下 Copy-Item 复制长文件名时触发目标路径创建失败；改用短路径缓存工作区重试。

GREEN: prod-dry-run-freeze-restore-v2-shortpath -> PASS, 正式服短路径干净隔离包 report-only 预检通过，dry-run 证据已生成：D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\prod-preflight-release-evidence\20260707_showroom_freeze_restore_v2-preflight-release-dry-run.json。

GREEN: experience-preflight -> PASS, 正式服发布前已完成测试服部署、mark-tested、正式服 report-only dry-run 预检，且本轮不加冻结字段。

GREEN: promote-prod-freeze-restore-v2 -> PASS, 正式服 172.30.30.57 已部署恢复版 20260707_showroom_freeze_restore_v2。

GREEN: prod-post-deploy-freeze-restore-verification -> PASS, 正式服服务健康，showroom_product 无冻结列，近 30 分钟无冻结字段/导入 SQL 错误，release lock 已 APPLIED。证据: evidence/prod-freeze-restore-post-deploy-verification.txt

GREEN: local-admin-export-rerun -> PASS，本机芋道源码/admin 重新导出 zip：D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260707-local-export-prod-import-publish\evidence\showroom-products-local-admin-20260707T080819Z.zip，大小 599585060 bytes，浏览器：C:\Program Files\Google\Chrome\Application\chrome.exe。

GREEN: local-admin-export-rerun-contract -> PASS，最新 zip 可读，包含 manifest/product-data.xlsx，未发现冻结字段/冻结文案。证据: evidence/local-admin-export-rerun-contract.json。

GREEN: local-import-button-layout -> PASS，本地展厅产品导入按钮中心点不再被搜索框覆盖，点击后产品资源包导入弹窗和 file input 正常出现。证据: evidence/verify-local-import-button-layout.json。

GREEN: prod-import-dom-click -> PASS，正式服芋道源码/admin 已通过真实页面导入最新本机 zip；因旧前端布局遮挡，使用 DOM click 触发同一导入按钮事件。响应证据：evidence/prod-import-response-1783413341552.json。

GREEN: prod-fill-missing-hall-canvas-layout -> PASS，正式服通过 calculate-bu-canvas-layout 与 update-item-canvas-layout 补齐 8 个缺布局展柜，剩余缺布局 0。证据: evidence/prod-fill-missing-hall-canvas-layout-summary.json。

GREEN: prod-release-scope-layout-verify -> PASS，确认正式租户 tenant_id=1 的发布范围展柜布局完整；测试租户 id=10..17 的旧展柜不是正式发布范围。

GREEN: prod-manual-publish-after-layout-repair -> PASS，正式服手动发布成功 releaseId=20260707T084816Z-be276b74dfa8-485abb12668a。

GREEN: prod-website-release-current -> PASS，Website `/release/current` 返回 releaseId=20260707T084816Z-be276b74dfa8-485abb12668a，manifest/document 均可读取，无 `更新失败` 或 `SHOWROOM_RELEASE_INSTALL_FAILED`。

GREEN: prod-narration-reprobe -> PASS，正式库 `showroom_narration_version` 使用语言枚举 `ZH/EN`；按当前产品版本核验，正式租户 `INT-*` 产品均有已发布中英文语音。证据: evidence/prod-narration-reprobe.txt。

BLOCKER: prod-int-count-diff -> FOUND，正式库活跃 `INT-*` 产品 149，但导入 zip 与发布产品文档均为 140；差异为 9 个无旧编号、无展柜引用的孤儿产品：INT-64、INT-69、INT-70、INT-71、INT-72、INT-73、INT-74、INT-75、INT-83。

GREEN: prod-delete-orphan-int-products -> PASS，已通过正式服现有 `/showroom/product/delete` 业务接口清理 9 个孤儿产品；复核活跃 `INT-*` 产品 140、展柜引用 `INT-*` 产品 140、活跃旧编号产品 0。

GREEN: prod-final-manual-publish -> PASS，清理孤儿产品后正式服再次手动发布成功 releaseId=20260707T085711Z-be276b74dfa8-485abb12668a。

GREEN: prod-final-publish-verify -> PASS，Website 当前 release 为 20260707T085711Z-be276b74dfa8-485abb12668a；产品文档 140、产品音频资产 280、音频成对 140、缺音频产品 0；manifest 无精确旧产品编号 `product_数字`；根页面 200 且无更新失败。证据: evidence/prod-final-publish-verify.txt。