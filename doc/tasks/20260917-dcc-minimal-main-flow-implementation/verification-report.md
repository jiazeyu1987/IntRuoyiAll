# DCC 主流程最小实现验证报告

## Scope

本报告只覆盖当前实施阶段的 M04 上传身份边界，不代表 M01—M30 全部完成。产品/项目目录模板实例化、两阶段审批、轮次历史、小版本、大版本、作废、例外替换、关联引用和 PDF 对比仍待后续阶段实现与验证。

## BDD

- `BDD-M04-free-name-upload`：启用项目中，允许文件类型不再要求名称命中旧模板清单；名称仍必须非空，项目、权限和类型校验仍保留。

## RED / GREEN

- RED：`DccSourceUploadContextTest#newUploadAllowsFreeNameForConfiguredType` 失败，当前实现调用了严格 `validateUploadSelection`。
- GREEN：`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccSourceUploadContextTest,DccProjectFileTemplateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## REGRESSION / STATIC CONTRACTS

- 定向单元测试：`DccSourceUploadContextTest` 7/7、`DccProjectFileTemplateServiceImplTest` 8/8 通过。
- 受影响回归：`DccControlledFileWorkflowServiceImplTest` 135/135、`DccControlledFileUploadApiTest` 34/34 通过。
- 后端静态合同 `dcc-static-003`、`dcc-static-008` 通过；前端上传布局、项目分类新建上传、类别绑定、权限边界、优化和受控保存闭环 6 个静态合同通过。
- 相关已跟踪文件 `git diff --check` 通过；新增源文件和测试文件的尾随空白检查通过。

## Implemented Boundary

- 新增 `DccProjectFileTemplateService.validateUploadLocation`，仅校验项目模板已配置、文件类型存在于模板且分类仍是有效叶子。
- `NEW_UPLOAD` 上下文调用 `validateUploadLocation`，用户自定义名称不再与旧模板文件名清单比较。
- `validateUploadSelection` 保留给仍使用旧严格模板清单的历史入口。

## Evidence Boundary

- 当前不运行 E2E、真实数据库写入、服务启停或远程操作。
- 当前不声称 M01—M30 全部实现。
- 本报告只证明当前 M04 阶段的实现和回归证据，不扩展为 P1—P4 或 M01—M30 全量交付。

## Closeout Boundary

- `task-closeout-cleanup preview/apply` 未执行成功：当前仓库和可用命令路径均无该入口，preview 结果为 `CLEANUP_COMMAND_NOT_FOUND`，退出码 127。
- 当前轮未授权 Git 提交/推送，且工作区保留其它任务改动；因此本任务记录为 `blocked`，不把收尾门禁阻塞写成产品验证失败。

## Continuation Verification — 2026-09-17

本轮补齐的是验证合同与现行架构之间的陈旧断言，并复跑当前工作区的主流程定向回归：

- `IntRuoyiFronted/tests/e2e/dcc-approval-task-load-error-context-static.spec.js` 现在验证旧审批入口跳转统一 DCC 工作台，以及工作台在受控文件读取失败时清空待办列表和统计值。
- `IntRuoyiFronted/tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js` 现在验证带并发序列参数的详情加载顺序，签名证据读取失败仍保持可见且不阻断审批详情。
- `IntRuoyiFronted/tests/e2e/dcc-project-code-recognition-static.spec.js` 现在读取当前 `IntRuoyiBackend` 源码路径，并使用当前 `/mes/md/dcc-project-code` 入口。
- 前端 DCC 定向静态合同：19/19 PASS；`pnpm run ts:check`：PASS。
- 后端 DCC 静态合同：20/20 PASS；后端定向 Maven 回归：PASS；`git diff --check`：PASS。

这组验证仍不代表 M01—M30 的真实页面全量验收；M01/M04/M05 的最小实现已有当前工作区 Java 回归，其他批次继续以 `docs/dcc-minimal-main-flow/acceptance.md` 的 T1—T6 为实施门禁。

## Current Verification Rerun

本轮按开发文档的 T1—T6 定向门禁重新执行了代码、静态合同、SQL 合同和文档结构校验，并修正了两处与现行代码结构不一致的前端合同断言。

### RED / GREEN

- RED：`dcc-browser-version-summary-static.spec.js` 发现类别筛选只做数值检查，没有复用统一的浏览器选项 ID 守卫。
- GREEN：类别选项现在先经过 `isValidBrowserOptionId`，再保留数值类型约束；版本选项、检出、检入和提交路径继续使用同一守卫。
- RED：`dcc-upload-governance-ux-static.spec.js` 直接在模板块查找 `row.businessContextTags`，与当前通过 `resolveVisibleBusinessContextTags` 做过滤的实现不一致。
- GREEN：合同改为检查上下文解析器读取 `row.businessContextTags`，并继续检查模板稳定 `data-testid` 和可见渲染。

### Automated Verification

| Verification | Result |
|---|---|
| DCC focused Maven regression | PASS，退出码 0 |
| Backend static contracts | PASS，20/20 |
| Frontend static contracts | PASS，24/24 |
| Frontend type check | PASS，`pnpm run ts:check` |
| DCC SQL static contracts | PASS，35/35 |
| Name-claim migration static check | PASS，9/9 |
| Development-document structure | PASS，M01—M30、BDD-M01—BDD-M30、I00—I08、UTF-8、链接、代码围栏 |
| `git diff --check` | PASS，无 whitespace error |

后端 DCC 定向 Maven 回归覆盖名称占用、工作流、作废、项目编码、项目模板、上传上下文、查询、终结、审批版本绑定、发布、版本分配和 OnlyOffice 预览等测试类。

### Environment Boundary

- `mvn -pl yudao-module-dcc -am test` 的全量依赖回归被无关 `yudao-module-system` 合同阻断：缺少 `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js`。该错误未出现在 DCC 定向回归中。
- 未执行真实页面 Playwright E2E、真实数据库写入、服务启停、远程操作、Git 提交或推送；本报告不把这些未执行项写成 PASS。
- 当前证据证明代码、单元回归、静态合同、SQL 静态合同和文档结构完成；不扩大为 M01—M30 的真实页面全量验收或部署完成。

### Closeout

- 已先设置 `ready_for_closeout`，再执行 `task-closeout-cleanup --task-id 20260917-dcc-minimal-main-flow-implementation --mode preview`。
- 当前仓库、本机技能目录和可搜索路径均无该命令或 `task_closeout.py`，因此 preview 未执行成功；没有使用其它脚本冒充 cleanup。
- 本轮未授权 Git 提交/推送，任务状态按规则记录为 `blocked`，仅表示正式收尾门禁阻塞，不表示 DCC 定向验证失败。
