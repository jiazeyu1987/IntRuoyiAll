# 任务：Smart Release Phase 1 构建与部署报告任务包

## Goal

实现前的开发任务包目标：`Manifest v1 校验器 + build-release intake report-only + deploy-release precheck report-only`。

## 任务目标

把长期 Smart Release 架构的第一阶段拆成可执行开发任务包：先让构建行为和部署行为都具备可审计、结构化、只读的报告能力。本阶段只建立发布契约、构建前后差异报告、部署前目标兼容性报告和测试门禁，不改变当前 `build-release` / `deploy-release` 的实际成功失败结果。

## Scope

本阶段范围为构建侧契约校验、构建侧 intake report-only、部署侧 precheck report-only，不改变当前发布执行路径。

## 范围

- 定义 Manifest v1 精确契约和 validator 行为。
- 定义 `build-release` 阶段 `release-intake report-only` 输入、输出、错误码和报告文件。
- 定义 `deploy-release` 阶段 `deploy-precheck report-only` 输入、输出、错误码和报告文件。
- 定义 MySQL schema fingerprint 采集字段。
- 定义第一批 data ownership registry 范围。
- 定义第一版 resource reference manifest 字段。
- 定义部署目标按逻辑环境和服务器侧目标配置校验，不把测试服、正式服、备份服 IP 写入发布包。
- 定义测试计划、验收命令和任务状态。

## 非范围

- 不实现 artifact cache。
- 不实现 smart-release 真实缓存复用。
- 不阻断当前 `build-release`。
- 不阻断当前 `deploy-release`。
- 不执行数据库 schema/data migration。
- 不搬运 MinIO 大文件。
- 不做远程服务器写操作或远程修复操作。
- 不改 Runtime Control UI，只定义第一阶段展示需求。
- 不修改发布脚本、后端代码、数据库或远程服务器。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；Phase 1 是 report-only，不改变执行路径，也不允许把 validator/intake/precheck 失败伪装为通过。
- `是否从根因和长期维护角度解决`：是；同时建立构建契约和部署契约，为后续 blocking、smart-release 缓存复用、目标环境兼容性校验打基础。
- `是否存在临时补丁或绕过`：否；本任务只输出开发包文档，不做临时脚本补丁。

## BDD 场景

- BDD: Manifest v1 缺失字段报告失败 -> Given 一个发布包带有 Manifest v1 但缺少 `packageId` / When 运行 validator / Then validator 输出 failed，并指出缺失字段，不影响 legacy 发布执行。
- BDD: 包内文件未声明报告失败 -> Given 发布包存在 manifest 未声明文件 / When 运行 validator / Then validator 输出 failed，并列出未声明文件。
- BDD: smart-release 缺 baseline 报告失败 -> Given packageType 为 `smart-release` 且没有 `baselineManifestId` / When 运行 validator / Then validator 输出 failed。
- BDD: build-release 本地 schema 手工变化被报告 -> Given 本地 MySQL 新增字段但没有绑定 migration / When 运行 build-release 的 release-intake report-only / Then `schema-change-report.json` 包含该字段 drift，并在 `intake-result.json` 标记 warning/blocker candidate，但不阻断当前 build-release。
- BDD: build-release 必要数据变化进入分类报告 -> Given 第一批 registry 覆盖菜单/权限/字典/配置 / When 本地对应数据变化 / Then `data-change-manifest.json` 按 registry 输出 required-data 变化。
- BDD: build-release 未归类数据变化不进入发布包 -> Given 本地业务表变化没有 registry / When 运行 release-intake report-only / Then 变化进入 `unclassified-local-change`，不进入 required-data。
- BDD: build-release 新增文件引用进入资源引用报告 -> Given 本地数据引用新增 MinIO object / When 运行 release-intake report-only / Then `resource-reference-manifest.json` 记录 file config、bucket、object key、domain 和资源准备状态。
- BDD: deploy-release 缺少目标配置被报告 -> Given 发布人员选择部署到 `test` / When 运行 deploy-precheck report-only 但没有服务器侧目标配置 / Then `deploy-precheck-result.json` 输出 failed，code=`DEPLOY_TARGET_CONFIG_MISSING`，不继续假定任何 IP。
- BDD: deploy-release 目标环境不兼容被报告 -> Given manifest 的 `targetRequirements.environmentCodes` 不包含 `backup` / When 发布人员选择备份服部署 / Then deploy-precheck 输出 `DEPLOY_TARGET_REQUIREMENT_MISMATCH`。
- BDD: deploy-release 大文件增量未准备被报告 -> Given manifest 声明资源引用发生变化但没有 resource delta 或 snapshot 证明 / When 运行 deploy-precheck report-only / Then 输出 `DEPLOY_RESOURCE_DELTA_NOT_VERIFIED`，Phase 1 只报告不阻断 legacy deploy。
- BDD: legacy 发布包部署前报告为 warning -> Given 旧发布包没有 Manifest v1 / When 运行 deploy-precheck report-only / Then 输出 warning `LEGACY_DEPLOY_PRECHECK_REPORT_ONLY`，部署行为仍按现有路径执行。

## 里程碑

## Milestones

- [x] M1：确认 Phase 1 构建与部署双行为范围和非范围。
- [x] M2：定义 Manifest v1 schema 和 validator 规则。
- [x] M3：定义 build-release release-intake report-only 契约。
- [x] M4：定义 schema fingerprint、data registry、resource reference 第一版字段。
- [x] M5：定义 deploy-release deploy-precheck report-only 契约。
- [x] M6：定义测试计划、验收命令和任务状态。
- [x] M7：完成文档门禁、收尾预览和提交。

## 预期验证

## Expected Verification

- `prd.md`、`development-plan.md`、`test-plan.md`、`task-state.json` 存在。
- 文档明确 Phase 1 不改变现有 `build-release` / `deploy-release` 执行路径。
- 文档明确 validator/intake/deploy-precheck 的 RED/GREEN 测试。
- 文档明确部署目标不写死 IP，只通过逻辑环境和服务器侧目标配置选择。
- 文档明确后续实现首批文件、命令和错误码。

## Blockers

- 后续实现前需要确认本地只读数据库连接配置文件路径和凭据管理方式。
- 后续实现前需要确认 baseline manifest 选择规则。
- data ownership registry 首批表字段 ownership 需要逐表确认。
- deploy target config 的 schema、存放路径和运行控制台读取方式需要确认。
- 部署侧只读远程探测如果需要连接服务器，必须先确认授权边界和凭据管理方式。

## 完成证据

- PRD：`prd.md`。
- 开发计划：`development-plan.md`。
- 测试计划：`test-plan.md`。
- 任务状态：`task-state.json`。
- Manifest v1 契约：`manifest-v1-contract.md`。
- Build release intake report-only 规格：`release-intake-report-only-spec.md`。
- Deploy release precheck report-only 规格：`deploy-precheck-report-only-spec.md`。
- Roadmap node dev plan 校验通过。

## 当前状态

completed

## Current Status

completed
