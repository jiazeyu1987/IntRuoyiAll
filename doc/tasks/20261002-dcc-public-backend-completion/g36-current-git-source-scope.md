# G36 当前三批交付的 Git 纳入范围

2026-10-04；只读结果：G33/G34/G35 已接收的三份 manifest 与当前文件指纹一致，未发现源码漂移。**没有暂存、提交或合并；本报告不表示 ready to merge。** 仅核对以下有限范围，不重查整套旧成果、不修改 G21/G28 候选、不生成新的通用候选规则。

已读 AGENTS、task-closeout、worktree 规则；实际 `git branch --show-current` 为 `codex/20261001-dcc-integration`。Git 核对使用限定路径的 `git --no-optional-locks status --porcelain=v1 -z --untracked-files=all --ignored=matching`、`ls-files` 与 `check-ignore -v`，没有运行 stage/commit/merge/cleanup。此前查找猜测的 `docs/git-rules.md` 文件不存在；随后实际文件目录核对显示本仓库 Git/合入规则归于已读取 task-closeout 与 worktree 文档，不据不存在的猜测路径新增门禁。

## 指纹来源

| 当前 manifest | 对应 Root review | 结果 |
|---|---|---|
| backend task `g33-maintenance-entry-delivery-fingerprints.json` | 主任务 `g33-delivery-root-review.json` | manifest SHA 一致；source10、taskAsset4、compiled16 全部对应当前 bytes/SHA |
| detail task `g34-matrix-delivery-fingerprints.json` | 主任务 `g34-matrix-root-review.json` | manifest SHA 一致；3 个源码/测试资产一致 |
| backend task `g35-navigation-delivery-fingerprints.json` | 主任务 `g35-frontend-root-review.json` | manifest SHA 一致；3 个源码/测试及1份说明一致；raw log3指纹一致但不纳入Git |
| detail task `g35-browser-entry-fixture-receipt.json` | Root G35 前端组合核验 | 1 个测试文件的当前 SHA 与收据一致 |

## 正式源码和测试：17条精确路径

状态 `??` 表示尚未跟踪，` M` 表示已跟踪但工作区有修改；当前这些源码和测试没有 ignored 项。**对这些精确路径执行普通 git add 即可纳入，无需 force。** `git add -u` 或只提交 tracked diff 会漏掉13个新增文件；不能通过整目录 git add 来顺带纳入未审资产。

| 批次 | 当前 Git 状态 | 路径 |
|---|---|---|
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceAuthAdapter.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceGate.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceExecutor.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyNameRegistrationMaintenanceCommand.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyNameRegistrationMaintenanceRunner.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceEntryTest.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceGateTest.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceCommandTest.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceKernelTest.java |
| G33 | ?? | IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceLoggingTest.java |
| G34 | M | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalMatrixAdminServiceImpl.java |
| G34 | M | IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/category/DccFileCategoryMapper.java |
| G34 | M | IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalMatrixAdminServiceImplTest.java |
| G35 | M | IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue |
| G35 | ?? | IntRuoyiFronted/src/views/dcc/controlled-file/shared/submitted-application-navigation.ts |
| G35 | ?? | IntRuoyiFronted/scripts/dcc-submitted-application-navigation.test.mjs |
| G35 fixture | ?? | IntRuoyiFronted/tests/unit/dcc-browser-application-entry.test.cjs |

上述17项中4项 tracked modified、13项 untracked。G35 测试位于正式 `scripts/` 或 `tests/unit/`，没有命中 `doc/tasks/**/*.cjs` 的忽略规则，不能因扩展名相同而误用 force。

## 随交付保留的说明和测试资源

三份 manifest、本报告所核 G33 BDD/contract/verification、G35 `g35-positive-ui-architecture-review.md` 当前均 untracked、未 ignored；若 Root 已确认纳入永久交付说明，对精确文件普通 add 即可。G33 `g33-format-owned.java` 是 manifest 所列任务 formatter helper，当前同样 untracked、已在 Cleanup Keep；它不是生产 Java 类，不应移入 src/main 或把它的临时编译产物加入源码提交。

有限 test SQL 状态核对：

| 路径 | 当前状态 | 纳入边界 |
|---|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/dcc_legacy_name_tables.sql | ??，未 ignored | 是已存在的测试资源路径，不是本轮三份 manifest 新增的生产 DDL。Root 应按此前已审测试资产归属显式纳入；不能因只看 tracked diff 漏掉，也不能仅凭 untracked 宣称其已被三份新 manifest 接收 |
| IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql | M，tracked | 共享测试 schema，范围包含既有整合成果；本报告不拆分或覆盖它，不凭 G33/G34/G35局部检查批准整份 diff |

G33 Kernel 测试继承已存在的 registration fixture；只纳入这10个新源码/测试而遗漏其既有已审依赖会使交付不完整。旧依赖清单仍由 Root 管理，不在此新增泛化 scope 或改写旧 manifest。

## 永久验证脚本：精确 forceadd，草稿排除

当前 backend task `Cleanup Keep` 中这些既有永久 UI 验证源码仍未跟踪，命中根 `.gitignore:103:doc/tasks/**/*.cjs`；普通 git add 会遗漏，未来 Root 纳入时应仅对以下精确路径使用 `git add -f`：

- doc/tasks/20261002-dcc-public-backend-completion/g27-public-lifecycle-ui-helpers.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g29-public-lifecycle-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g29-public-lifecycle-contract-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g30-public-version-relations-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g30-version-relations-contract-test.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g31-distribution-obsolete-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g31-final-lifecycle-contract-test.cjs

共享 g27 helper 是上述 runner 的显式 require 依赖，且在同一 Cleanup Keep 单独登记；它也被同一规则忽略，不能只纳入 runner 而漏 helper。本轮只核 Git 可纳入状态，不重跑或重复宣称这些脚本的真实页面验收。

以下3个 G34 未完成草稿虽然被 Cleanup Keep 保留，**仍须排除最终实现提交、不得 forceadd**。Keep 表示不被清理删除，不表示通过 Review 或批准纳入交付。现有 task 与 Root review 明确 `deferred_for_detail_phase`、未验证、非 runtime-ready：

- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-negative-runner.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-observer.cjs
- doc/tasks/20261002-dcc-public-backend-completion/g34-real-ui-contract-test.cjs

不能使用 `git add -f doc/tasks/...` 或通配全部 `.cjs`，否则会一起纳入延后草稿、raw logs 等未审资产。

## 不应纳入的构建产物和原资产

G33 manifest 的16个 `.class` 只属于编译证据，不是源码或最终 Git 资产。它们位于 target/classes、target/test-classes；`git check-ignore -v` 确认 `IntRuoyiBackend/.gitignore:8:target/` 生效。限定 status 可能只报告被忽略的父目录而不是逐条 class，不可把逐文件未列出解释为未受忽略。无需为 `.class`、Jar、target、dist 或 raw logs forceadd。G35 三个 raw log 均为 ignored，manifest 保留其摘要/SHA即可；G33日志策略同样明确本地保留、Git排除。

`goal-preserved-nontask-assets.json` 的6项当前哈希全部一致，`includeInDccCommit=false`：主目录和整合目录各自的 AGENTS.md、infra FileController.java、infra FileControllerTest.java。它们不是本任务源码，保留原状、不纳入 DCC commit；本报告没有输出或改写这些文件内容。

## 本次结论

没有发现这三批已接收源码的新漂移或未知 ignored 生产文件；实际需要注意的是13个 untracked 源码/测试、既有永久验证脚本的精确 forceadd、延后G34草稿和构建产物排除，以及共享 test SQL 的原有归属核对。以上是最终 Git 收口时的有限操作清单，不是授权现在提交或证明已合入。

新项目首次访问规则方案、实际质量批准、迁移/配置执行和真实页面验证仍由 Root 按用户答复及实际证据推进。本报告没有更改这些门禁，没有恢复延后负向开发，也没有改变整个目标的完成标准。
