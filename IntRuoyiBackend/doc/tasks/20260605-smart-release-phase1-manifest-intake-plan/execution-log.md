# 执行日志：20260605-smart-release-phase1-manifest-intake-plan

BDD: Manifest v1 缺失字段报告失败 -> Given 一个发布包带有 Manifest v1 但缺少 `packageId` / When 运行 validator / Then validator 输出 failed，并指出缺失字段，不影响 legacy 发布执行。

BDD: 包内文件未声明报告失败 -> Given 发布包存在 manifest 未声明文件 / When 运行 validator / Then validator 输出 failed，并列出未声明文件。

BDD: smart-release 缺 baseline 报告失败 -> Given packageType 为 `smart-release` 且没有 `baselineManifestId` / When 运行 validator / Then validator 输出 failed。

BDD: build-release 本地 schema 手工变化被报告 -> Given 本地 MySQL 新增字段但没有绑定 migration / When 运行 build-release 的 release-intake report-only / Then `schema-change-report.json` 包含该字段 drift，并在 `intake-result.json` 标记 warning/blocker candidate，但不阻断当前 build-release。

BDD: build-release 必要数据变化进入分类报告 -> Given 第一批 registry 覆盖菜单/权限/字典/配置 / When 本地对应数据变化 / Then `data-change-manifest.json` 按 registry 输出 required-data 变化。

BDD: build-release 未归类数据变化不进入发布包 -> Given 本地业务表变化没有 registry / When 运行 release-intake report-only / Then 变化进入 `unclassified-local-change`，不进入 required-data。

BDD: build-release 新增文件引用进入资源引用报告 -> Given 本地数据引用新增 MinIO object / When 运行 release-intake report-only / Then `resource-reference-manifest.json` 记录 file config、bucket、object key、domain 和资源准备状态。

BDD: deploy-release 缺少目标配置被报告 -> Given 发布人员选择部署到 `test` / When 运行 deploy-precheck report-only 但没有服务器侧目标配置 / Then `deploy-precheck-result.json` 输出 failed，code=`DEPLOY_TARGET_CONFIG_MISSING`，不继续假定任何 IP。

BDD: deploy-release 目标环境不兼容被报告 -> Given manifest 的 `targetRequirements.environmentCodes` 不包含 `backup` / When 发布人员选择备份服部署 / Then deploy-precheck 输出 `DEPLOY_TARGET_REQUIREMENT_MISMATCH`。

BDD: deploy-release 大文件增量未准备被报告 -> Given manifest 声明资源引用发生变化但没有 resource delta 或 snapshot 证明 / When 运行 deploy-precheck report-only / Then 输出 `DEPLOY_RESOURCE_DELTA_NOT_VERIFIED`，Phase 1 只报告不阻断 legacy deploy。

BDD: legacy 发布包部署前报告为 warning -> Given 旧发布包没有 Manifest v1 / When 运行 deploy-precheck report-only / Then 输出 warning `LEGACY_DEPLOY_PRECHECK_REPORT_ONLY`，部署行为仍按现有路径执行。

RED: 用户评审 Phase 1 文档 -> FAIL，原文档偏构建行为，缺少 `deploy-release` 的部署前检查命令、输出、错误码和测试矩阵。

GREEN: Phase 1 构建与部署双行为文档落盘 -> PASS，已补充 task、PRD、development-plan、test-plan、task-state、manifest-v1-contract、build-intake spec、deploy-precheck spec。

GREEN: roadmap-node-dev-plan validator -> PASS，`python C:\Users\BJB110\.codex\skills\roadmap-node-dev-plan\scripts\validate_node_dev_plan.py --task-dir D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260605-smart-release-phase1-manifest-intake-plan`。

GREEN: deploy coverage pattern check -> PASS，`rg --no-ignore` 已确认 `deploy-precheck-result.json`、`run-deploy-precheck-report.ps1`、`DEPLOY_TARGET_REQUIREMENT_MISMATCH`、`DEPLOY_RESOURCE_DELTA_NOT_VERIFIED`、`build-release`、`deploy-release`、`report-only` 均在任务包中覆盖。

GREEN: forbidden target IP check -> PASS，任务包未出现测试服、正式服、备份服具体服务器地址；部署目标按逻辑环境和服务器侧配置建模。

GREEN: task-state JSON validation -> PASS，`python -X utf8 -m json.tool task-state.json` 可解析。

GREEN: task-closeout-cleanup preview -> PASS，无阻塞；预览建议删除 PRD、开发计划、测试计划、契约和 task-state，但这些是本任务正式交付物，本次保留不删除。
