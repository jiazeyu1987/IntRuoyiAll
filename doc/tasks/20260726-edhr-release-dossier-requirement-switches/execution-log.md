# eDHR 放行资料限制开关 Execution Log

## User Intent

实现 eDHR 放行资料限制开关计划：个人中心配置页签新增来料检报告、灭菌报告、成品检报告、成品检记录限制 4 个金手指开关；默认关闭，开启后放行必须校验对应特殊节点已完成且有已保存 `ADD` 附件；配置缺失、非法、预检后变更、证据不完整均 fail fast。

## Workspace Baseline

- `git status --short --branch`：`int_main...origin/int_main [ahead 1]`，已有大量本任务开始前的未提交 tracked/untracked 改动。
- 本任务将避免修改无关文件；提交阶段按项目 dirty-worktree baseline 规则处理。

## BDD Scenarios

- `BDD: 金手指配置可见性 -> Given 金手指用户 / When 打开个人中心配置页签 / Then 可看到 4 个资料限制开关；普通用户不可见配置页签。`
- `BDD: 默认关闭保持现状 -> Given 四个开关默认关闭 / When 特殊节点未完成且无附件 / Then 放行预检不因这些资料阻塞。`
- `BDD: 打开后阻止无资料放行 -> Given 某资料限制打开 / When 对应特殊节点未完成或无已保存附件 / Then 放行预检生成 BLOCKER 且提交放行失败。`
- `BDD: 完成并上传后允许放行 -> Given 某资料限制打开 / When 对应特殊节点已完成且存在已保存 ADD 附件 / Then 该检查项 PASS。`
- `BDD: 配置变更后必须重跑预检 -> Given 预检后开关状态发生变化 / When 提交放行 / Then 后端拒绝提交并提示重新预检。`

## Milestone Log

- 2026-07-26：创建任务文档，记录用户计划、BDD 场景和当前脏工作区基线。
- 2026-07-26：`GREEN: experience-preflight -> PASS`，已读取 `docs/experience-index.md`、前端/后端/数据库/E2E/登录/编码/任务收尾规则；命中前端静态契约隔离、静态合同同步、全局开关 E2E 恢复、Maven reactor 兄弟模块和 PowerShell/Git 门禁。

## RED/GREEN Evidence

- `RED: node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js -> FAIL, src/api/mes/pro/edhr/releaseDossierRequirementSetting.ts 缺失`（来自本任务 RED 交接记录）。
- `RED: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" test -> FAIL, upstream reactor 模块无指定测试；按 PowerShell/Maven 门禁改用 "-Dsurefire.failIfNoSpecifiedTests=false"`（来自本任务 RED 交接记录）。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-static.spec.js -> PASS`，前端 API wrapper、Profile 配置页签、4 个 switch 文案、金手指权限、确认保存、失败回滚和放行检查展示映射通过静态合同。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile -> PASS`，2026-07-26 13:15:50 完成后端生产代码 reactor 编译。
- `GREEN: pnpm ts:check -> PASS`，`vue-tsc --noEmit -p tsconfig.relaxed.json` 通过。
- `GREEN: node tests\e2e\edhr-release-check-result-chinese-static.spec.js -> PASS`，放行检查结果中文映射保持通过。
- `GREEN: node tests\e2e\edhr-release-dialog-copy-cleanup-static.spec.js -> PASS`，放行弹窗文案相邻静态合同保持通过。
- `GREEN: git diff --check -> PASS`，仅输出 CRLF 工作区警告，无 whitespace error。
- `BLOCKER: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，第一次失败于并行/无关 `MesProRouteBatchRecordAttachmentOwnerServiceTest` 缺少 route attachment owner VO；复跑时该阻塞变化为并行/无关 `yudao-module-system` 编译错误 `CodexTestRunnerServiceImpl` 未实现 `getRunnerStatus()`，导致 `yudao-module-mes` 被 reactor 跳过。
- `BLOCKER: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" test -> FAIL`，辅助 MES-only 复验被并行/无关 route/BPM 改动阻塞：`MesProRouteFlowConfigServiceImpl` 未实现 `saveBatchRecordAttachmentOwners(...)`，以及 `BusinessApprovalPolicyDOBuilder` 缺少 `formPolicyType(String)`。

## Blockers

- 当前仓库存在本任务开始前/并行任务产生的大量未提交与未跟踪改动；不会改变本任务实现范围，但阻塞最终提交/推送边界，需按项目 dirty-worktree baseline 和选择性暂存规则处理。
- 后端正式目标 JUnit 不能完成：`-am` reactor 当前先失败在并行 system 模块缺实现方法；MES-only 辅助复验当前失败在并行 route/BPM 编译漂移。需先由对应任务修复或隔离这些无关编译阻塞后复跑本任务后端目标测试。
