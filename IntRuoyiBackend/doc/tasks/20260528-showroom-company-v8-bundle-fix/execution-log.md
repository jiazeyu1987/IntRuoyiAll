# 执行日志：修复公司版本 V8 缺少 readable bundle

BDD: 公司版本 V8 应具备可读版本详情 -> Given 公司 `targetType=COMPANY,targetId=1` 的历史版本 V8 已发布且存在双语公开讲解 / When 管理端请求版本中心详情 / Then 后端应返回 V8 的可读快照、双语讲解和重发 readiness，而不是 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。

BDD: 缺少 readable bundle 时必须暴露真实前置条件 -> Given 已发布公司 revision 缺少唯一双语公开讲解或 bundle / When 管理端请求版本中心详情 / Then 系统必须 fail fast 暴露缺失前置条件，不得用当前版本、mock 数据或空音频兜底。

INFO: 最近后端任务 `20260528-showroom-sites-nginx-proxy` 已标记 `completed`，本任务可开始。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 前置编译阻塞：`ShowroomSignatureGovernanceAdapterTest` 依赖 DCC 签名包，单模块命令未带入 `yudao-module-dcc`。

RED: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 预期失败原因：详情请求选中公司历史 bundle 后，解析当前内容 revision 时直接抛出 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`，导致 V8 页面不可读。

INFO: 只读复现 `POST http://172.30.30.58:48081/admin-api/system/auth/login` with `tenant-id=1 / admin / admin123` -> PASS；`GET /admin-api/showroom/company/current` -> 当前 `companyId=1, revisionId=9, revisionNo=9`；`GET /admin-api/showroom/company/history?id=1` -> 历史 V8 为 `revisionId=8`。

INFO: 只读复现 `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8&siteKey=yingtai-showroom&stage=TEST` -> FAIL, 响应 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`；说明选中 V8 可读，但当前内容 V9 辅助快照缺 bundle。

INFO: 只读 SQL 诊断 -> PASS, `showroom_version_bundle` 已有 `COMPANY:1:7` 与 `COMPANY:1:8`，无 `COMPANY:1:9`；`showroom_narration_version` 中 `COMPANY:1:9` 的 ZH/EN 公开已发布候选各 2 条，因此现有 backfill 规则按 no-fallback 原则不会猜测写入 V9 bundle。

GREEN: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

GREEN: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest,ShowroomVersionBundleServiceTest,ShowroomVersionCenterBackfillContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests。

GREEN: `git diff --check` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-showroom-company-v8-bundle-fix --mode preview` -> PASS, keep `task.md` and `execution-log.md`, delete none, blocked none, warnings none。

INFO: 本次修复未修改 `芋道源码` 租户业务数据；V9 自身 readable bundle 仍需后续在业务确认唯一 ZH/EN 音频候选后单独处理。
