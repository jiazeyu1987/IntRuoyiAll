# E2E-14 文控后立即生效

## Goal

独立验证E2E-14，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — Root 接受错误展示最小修复及 7+52 单测后，已完成本次授权的 R/O 两份新单据串行定向 UI：R 明确拒绝业务表现 PASS（原 runner 文案断言失败单列解释），O 一次正常签名成功且同文件 A/1 受控已生效。未确认/阅读拒绝均 NOT_EXERCISED，原 #1198 现场网络根因仍 OPEN，不用本次样本替代。23 trace/69 截图证据与全部浏览器/自有进程关闭检查 PASS；HOLD，不重签、不追加 E2E。交接见 r2-signature-ui-recheck/handoff.json。Git/cleanup 未授权，不标 completed。

历史第三轮：原文件 4364/A1、#1198 VALID 和六分支合同沿 Root 独立 review；原 state 的 4 PASS/2 FAIL、U 浏览权限观察和首次 D 异常证据保持不变，详见 rerun-20261020-r2-round3-root-review.md。新修复/定向 UI 结论单独记录，不改写历史原档。

## 2026-10-19 r4 遗留导出菜单合同

- 目标：只核对并修正旧工具栏测试与正式导出入口约定的冲突；生产菜单和r2身份修复不变。
- 正式依据：20260908-unified-csv-approval-evidence-entry任务Goal/Design Constraints及验证报告明确用户清单和通用账号导出统一到CSV质量包；当前CsvPackageGovernanceListPane保留system:user:export及正式UserApi.exportUser调用。
- BDD：Given 用户清单导出已迁到CSV质量包，When 验证用户管理高级菜单，Then 新增、导入、钉钉导入、批量删除、删除组织及其原流程继续存在，旧导出菜单和分派分支不得恢复；CSV账号证据入口继续保留权限和正式导出API。
- 里程碑：正式合同溯源 → 旧测试实际FAIL留证 → 最小测试修正 → 新合同GREEN/负向变异RED及CSV相邻合同 → r4交接。
- 预期验证：只运行本轮更改合同、其负向变异及直接相关CSV静态合同，保留实际退出码和JUnit XML。保留原立即生效ROOT_ACCEPTED、r2的14+6及页面PASS、r3全量类型PASS，不重跑这些已通过项；不运行Maven/E2E/服务/数据写入/Git写入或Agent。

## 2026-10-19 r3 工程验证

- 目标/里程碑：核对派工及进程 → 执行正式pnpm ts:check → 归因实际诊断 → 仅本轮错误最小修复及必要复验 → repair-20261019-r3-handoff.json。
- BDD：Given r2前端身份修复已完成且没有其他vue-tsc，When 以package.json既定8192MB堆执行全量类型检查，Then 保留退出码/原始诊断；只修本轮错误，其他模块与旧导出菜单合同分开交接。
- 验证/约束：不修改tsconfig、堆大小、正式菜单或关闭检查；无新生产改动则不重复14定向/6相邻/页面PASS。只使用本任务证据目录，不执行Maven、服务重启、业务数据写入、Git写入或启用子Agent。

## 2026-10-19 r2 定向修复

- 目标：用任务自有用户证实列表响应、稳定行身份和编辑目标是否可能串行，按实际RED做最小修复。
- BDD：Given 用户列表先后查询本例D与U，When 较早分页或角色补充响应晚于新查询完成，Then 列表、总数和loading只能属于最新查询，编辑按钮以稳定用户ID定位。
- BDD：Given D编辑详情仍在加载，When 关闭并打开U或新增表单，Then 旧详情不能覆盖新表单；详情或辅助选项加载失败不得允许提交。
- 里程碑：规则/调用点核对 → 实际组件异步RED → 最小GREEN与相邻回归 → 真实页面自有用户定向验证 → repair-20261019-r2-handoff.json。
- 验证：独立轻量前端测试可运行；全量类型检查交Root串行排期；不运行Maven、不切包/重启，不改共享Query。真实E2E只使用Playwright页面、独立context和唯一trace目录，不用接口/DB/拦截响应替代。
- 原始910170事故仅作历史线索，不宣称缺少row-key就是事故根因；不改该账号、不重放组织准备。优先复用本例D/U，只读打开/取消；如须写入验证，仅限本例资产并明确恢复。

## BDD

- Given 本次选择立即生效，已完成前置审批
- When D取消签名后重试，最后签名审核通过；回到文件详情及日常目录查看
- Then 未完成文控审核及签名不生效；完成后记录真实实际生效时间，文件按已收到的规则进入相应受控/生效状态；不得用预填日期代替实际时间。盖章细节待第8项原文补充。
- 2026-10-18 恢复门禁：Given 原现场未确认是否已有正式申请，When U/D 新上下文读取工作台、待办和原文件身份，Then 先归档实际现场；已有申请则继续原任务，不重复创建。已在全部版本中按本例前缀确认 0 条；Root 进一步明确复用项目 284（D 正式 OWNER / U 正式 EDIT）及既有前置，不为变更负责人创建新项目，当前未提交任何新项目。使用 APPLICATION_CUSTOM 申请，PUBLIC 类型不在本例验收声明内。
- 负向分支：Given 同一版本仍待文控签名，When 取消签名或未填必填签名信息，Then 不生成文控签名、不推进任务、不记录生效时间；再次由 D 本人签名后核对同一版本的真实终态与时间。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root放行后真实前端执行；任务自有数据保留待Root review，清理状态如实记录。
4. [x] Root独立review脚本/trace/可见页面断言并接受4/4及负例。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 原准备阶段只改本目录资产；后续 Root 明确授权四个生产文件的部门清空最小修复及专属测试，并在真实审计 RED 后授权遗漏部门审计修复。当前源码已冻结，Maven、构建、部署与迁移由 Root 执行；本线程不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C14，不改其它用例对象或公共矩阵；复用正式项目审核职责仅绑定本例专用账号，不修改职责定义或其它成员。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。
- 组织准备误改账号 910170 已按原值恢复部门和岗位，精确证据见 incident-report.md 与 department-recovery-result.json；不再对其写入。后续准备只处理本例资产。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-14/task.md
- doc/tasks/20261016-dcc-e2e-14/execution-log.md
- doc/tasks/20261016-dcc-e2e-14/verification-report.md
- doc/tasks/20261016-dcc-e2e-14/prepared.json
- doc/tasks/20261016-dcc-e2e-14/e2e-14-immediate-effective.cjs

- doc/tasks/20261016-dcc-e2e-14/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-14/incident-report.md
- doc/tasks/20261016-dcc-e2e-14/real-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-14/ui-extensions.cjs
- doc/tasks/20261016-dcc-e2e-14/credential-store.cjs
- doc/tasks/20261016-dcc-e2e-14/sanitize-evidence.py
- doc/tasks/20261016-dcc-e2e-14/evidence-sanitization.json
- doc/tasks/20261016-dcc-e2e-14/.gitignore
- doc/tasks/20261016-dcc-e2e-14/live-evidence
- doc/tasks/20261016-dcc-e2e-14/credential-evidence-check.json
- doc/tasks/20261016-dcc-e2e-14/department-recovery-plan.md
- doc/tasks/20261016-dcc-e2e-14/root-test-handoff.json
- doc/tasks/20261016-dcc-e2e-14/department-clear-front.test.cjs
- doc/tasks/20261016-dcc-e2e-14/recover-department-910170.cjs
- doc/tasks/20261016-dcc-e2e-14/department-recovery-result.json
- doc/tasks/20261016-dcc-e2e-14/department-recovery
- doc/tasks/20261016-dcc-e2e-14/prepare-controlled-file.cjs
- doc/tasks/20261016-dcc-e2e-14/upload-preparation-result.json
- doc/tasks/20261016-dcc-e2e-14/upload-preparation
- doc/tasks/20261016-dcc-e2e-14/route-preview-blocker.md
- doc/tasks/20261016-dcc-e2e-14/route-preview-runtime-error.txt
- doc/tasks/20261016-dcc-e2e-14/root-precondition-handoff.json
- doc/tasks/20261016-dcc-e2e-14/fixture-assets
- doc/tasks/20261016-dcc-e2e-14/verify-own-evidence.cjs
- doc/tasks/20261016-dcc-e2e-14/main-trace-sanitization.json
- doc/tasks/20261016-dcc-e2e-14/advance-original-flow.cjs
- doc/tasks/20261016-dcc-e2e-14/verify-final-state-readonly.cjs
- doc/tasks/20261016-dcc-e2e-14/resume-20261018
- doc/tasks/20261016-dcc-e2e-14/resume-20261018-checkpoint.json
- doc/tasks/20261016-dcc-e2e-14/final-readonly-result.json
- doc/tasks/20261016-dcc-e2e-14/screenshots
- doc/tasks/20261016-dcc-e2e-14/e2e-14-failure-trace.zip
- doc/tasks/20261016-dcc-e2e-14/e2e-14-pre-signature-trace.zip
- doc/tasks/20261016-dcc-e2e-14/e2e-14-before-negative-secret-trace.zip
- doc/tasks/20261016-dcc-e2e-14/acceptance-result.json
- doc/tasks/20261016-dcc-e2e-14/user-async-identity.test.cjs
- doc/tasks/20261016-dcc-e2e-14/verify-user-identity-r2.cjs
- doc/tasks/20261016-dcc-e2e-14/check-user-r2-evidence.cjs
- doc/tasks/20261016-dcc-e2e-14/repair-20261019-r2
- doc/tasks/20261016-dcc-e2e-14/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-14/repair-20261019-r3
- doc/tasks/20261016-dcc-e2e-14/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-14/verify-toolbar-contract-r4.cjs
- doc/tasks/20261016-dcc-e2e-14/repair-20261019-r4
- doc/tasks/20261016-dcc-e2e-14/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-plan.md
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020.cjs
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-evidence.cjs
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-round3-report.md
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-round3-handoff.json
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-round3-1792467600140
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-r2-round3-plan.md
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-r2-round3.cjs
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-r2-round3-evidence.cjs
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-r2-round3-report.md
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-r2-round3-handoff.json
- doc/tasks/20261016-dcc-e2e-14/rerun-20261020-r2-round3

- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/R-evidence-validation.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/closure-validation.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/evidence-check.cjs
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/evidence-validation.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/execute-gate.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/execution-log.md
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/execution-log.md.before.txt
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/final-document-validation.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/handoff.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/page-plan.md
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/pre-execution.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/raw-run-artifact-hashes.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-signed-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/A-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-signed-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/C-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-normal-success-dialog-closed.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-normal-success-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/D-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/DCC241020E14O1792488015305-immediate.pdf
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-assignment-complete.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-exact-C-selected.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/L-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/READBACK-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/READBACK-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/READBACK-same-record-browser-readonly.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/READBACK-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-signed-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/S-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-new-source-uploaded.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-single-submit-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-submitted-browser.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/U-unique-number-no-existing-file.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-O/state.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-signed-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/A-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-signed-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/C-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-dialog-outcome.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-failure.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-signature-outcome-and-reading.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/D-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/DCC241020E14R1792487781321-immediate.pdf
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-assignment-complete.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-exact-C-selected.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/L-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/PREFLIGHT-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/READBACK-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/READBACK-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/READBACK-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/READBACK-same-pending-record-readonly.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/READBACK-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/READBACK-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-exact-todo.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-signature-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-signed-todo-gone.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-task-identity-not-effective.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/S-trace-2.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-login-identity.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-new-source-uploaded.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-result.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-single-submit-confirmation.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-submitted-browser.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-trace-1.zip
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/U-unique-number-no-existing-file.png
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/interpretation.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/run-R/state.json
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/task.md
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/task.md.before.txt
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/ui-recheck.cjs
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/verification-report.md
- doc/tasks/20261016-dcc-e2e-14/r2-signature-ui-recheck/verification-report.md.before.txt
