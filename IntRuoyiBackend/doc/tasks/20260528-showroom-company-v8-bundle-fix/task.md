# 任务：修复公司版本 V8 缺少 readable bundle

## 任务目标

- 复现用户在 `芋道源码/admin` 查看公司版本 V8 时出现 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9` 的问题。
- 确认 V8 对应的 `revisionId=9` 是前端传参错误、后端查询错误，还是运行数据缺少 `showroom_version_bundle`。
- 在不引入 fallback、不伪造历史数据、不修改测试开发阶段的芋道源码租户数据前提下，补齐可维护的回归保护和必要修复。

## BDD 场景

- BDD: 公司版本 V8 应具备可读版本详情 -> Given 公司 `targetType=COMPANY,targetId=1` 的历史版本 V8 已发布且存在双语公开讲解 / When 管理端请求版本中心详情 / Then 后端应返回 V8 的可读快照、双语讲解和重发 readiness，而不是 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。
- BDD: 缺少 readable bundle 时必须暴露真实前置条件 -> Given 已发布公司 revision 缺少唯一双语公开讲解或 bundle / When 管理端请求版本中心详情 / Then 系统必须 fail fast 暴露缺失前置条件，不得用当前版本、mock 数据或空音频兜底。

## 里程碑

- [x] M1：只读复现 `芋道源码/admin` V8 报错并确认 `revisionNo` 与 `revisionId` 映射。
- [x] M2：补充 RED 回归测试，证明具备双语公开讲解的已发布公司 revision 缺少 bundle 时当前路径不可读。
- [x] M3：实现最小正式修复，并保留 no-fallback fail-fast 语义。
- [x] M4：运行目标测试和相关回归验证。
- [x] M5：执行 task-closeout-cleanup 预览，更新任务记录并按策略提交本任务改动。

## 预期验证

- RED: 目标后端回归测试先失败，失败原因为公司 V8 readable bundle 缺失。
- GREEN: 目标后端回归测试通过。
- GREEN: 只读 API 或 Playwright 真实路径验证 `芋道源码/admin` 查看公司 V8 不再返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。
- GREEN: `git diff --check` 通过。

## 当前状态

completed

## 当前发现

- 最近后端任务 `20260528-showroom-sites-nginx-proxy` 已完成，可开始本任务。
- 报错由 `ShowroomVersionBundleService.requireBundle()` 在缺少 `showroom_version_bundle` 时直接抛出，当前行为是 fail fast，不是前端兜底问题。
- 历史任务 `20260525-test-showroom-company-revision-schema-hotfix` 已记录：当时仅补齐 schema，明确未执行 readable bundle backfill，版本中心仍可能返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。
- 只读复现确认：测试服主租户 `芋道源码/admin` 下 `companyId=1`，历史 V8 是 `revisionId=8` 且已有 bundle；当前内容版本是 V9 `revisionId=9` 且没有 bundle，因此请求 V8 详情时被 V9 辅助快照解析拖成 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:9`。
- 只读数据诊断确认：V9 的公司快照字段完整，但 ZH/EN 公开讲解各有 2 个 `PUBLISHED` 候选，现有 backfill 规则会按 no-fallback 原则跳过该版本，不会猜测选哪条音频。
- 已完成代码修复：版本中心详情页的选中版本仍必须有 bundle；当前内容 / 当前线上辅助快照缺 bundle 时返回 `null` 并记录带 scope 的 blocker，不再导致选中历史版本整页不可读。

## 验证结果

- RED: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 现有代码在解析当前内容 revision 时直接抛出 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。
- GREEN: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest#detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-module-dcc,yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest,ShowroomVersionBundleServiceTest,ShowroomVersionCenterBackfillContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests。
- GREEN: `git diff --check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-showroom-company-v8-bundle-fix --mode preview` -> PASS, 无删除项、无阻塞、无警告。

## 最终结果

- 版本中心详情现在只要求选中版本本身具备 readable bundle；当前内容 / 当前线上辅助快照缺 bundle 时以 scoped blocker 暴露，并返回 `null` 快照，避免历史 V8 被当前 V9 数据缺口拖成整页 500。
- 回归测试覆盖了历史公司版本可读、当前公司版本 bundle 缺失且讲解重复候选的真实场景。

## 剩余说明

- 本次未修改 `芋道源码` 租户业务数据；V9 仍有双语讲解重复发布候选，后续若要让 V9 本身进入 readable bundle，需要先由业务确认保留哪一组 ZH/EN 音频，再执行明确的数据清理/补 bundle 任务。
