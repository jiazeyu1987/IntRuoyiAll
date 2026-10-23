# E2E-08 项目/类型切换保护

## Goal

独立验证E2E-08，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — R3第二轮本例17分支 17 PASS/0 FAIL/0 BLOCKED，分类专项PASS；原错误/一次只读校准保留。正式交接rerun-20261020-r3-round2/handoff.json。全部资源已关闭，HOLD待Root；工程closeout无Git授权独立blocked。

## BDD

- Given 已经手改属性、会签、下发及关联
- When 分别尝试切项目、切已维护类型、切“其他”；各执行取消和确认
- Then 有相关手改时先提示；取消保留原项目/类型及全部选择；确认切项目重载属性/文件夹/适用配置并重新选关联；切已维护类型重载矩阵/默认下发；“其他”由作者配置并核对下发。
- Root review：Given 分别仅属性手改、仅会签手改、仅下发手改；When 切项目取消，属性另覆盖第二层替换取消；Then 只有指定字段相对默认值改变，取消后逐字段保持，确认切换时单独断言关联清空。上传前登记资产，退出前通过页面核实清理或保留残留标识；trace 在登录后开启。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 自行完成真实页面 fixture 核验、逐分支 E2E 及临时源件清理。
4. [x] Root已复核脚本、可见截图、逐分支断言及trace完整性，原范围业务证据ROOT_ACCEPTED。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 用户已授权创建本例账号/角色及相应菜单授权；仅用测试管理员配置，本例业务角色独立 context 登录，不授管理员、不改共享角色或既有账号密码。凭据只在运行时环境或受保护临时配置处理，不进入 trace/截图/日志。
- 自有数据前缀DCC241016C08，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。读取统一授权和运行身份，使用本例真实页面 manifest 核验，持续完成授权工作；共享 roleFixtureVerified=false 不阻断本例，也不擅自改为 true。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/evidence-verification.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/exit-code.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/finalize.cjs
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/gate.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/handoff.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/initial-category-load/first-selection.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/initial-category-load/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/own-profile.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/owned-processes.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/plan.md
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/process-closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/result.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/active-baselines/active-baselines.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/active-baselines/blocked.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/active-baselines/P1-active-row.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/active-baselines/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/evidence-verification.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/exit-code.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/gate.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/initial-category-load/first-selection.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/initial-category-load/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/own-profile.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/owned-processes.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/plan.md
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/process-closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/result.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-CANCEL-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-CANCEL-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-CANCEL-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-CONFIRM-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-CONFIRM-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-CONFIRM-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-INNER-CANCEL-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-INNER-CANCEL-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/ATTRIBUTE-ONLY-INNER-CANCEL-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-attribute-branches/attribute-results.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/DISTRIBUTION-ONLY-CANCEL-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/DISTRIBUTION-ONLY-CANCEL-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/DISTRIBUTION-ONLY-CANCEL-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/list-type-results.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/OTHER-CANCEL-NO-RELATION-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/OTHER-CANCEL-NO-RELATION-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/OTHER-CANCEL-NO-RELATION-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/OTHER-CONFIRM-NO-RELATION-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/OTHER-CONFIRM-NO-RELATION-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/OTHER-CONFIRM-NO-RELATION-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/PROJECT-CONFIRM-NO-RELATION-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/PROJECT-CONFIRM-NO-RELATION-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/PROJECT-CONFIRM-NO-RELATION-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/SIGNOFF-ONLY-CANCEL-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/SIGNOFF-ONLY-CANCEL-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/SIGNOFF-ONLY-CANCEL-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/TYPE-CANCEL-NO-RELATION-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/TYPE-CANCEL-NO-RELATION-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/TYPE-CANCEL-NO-RELATION-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/TYPE-CONFIRM-NO-RELATION-after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/TYPE-CONFIRM-NO-RELATION-before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract/verify-list-type-branches/TYPE-CONFIRM-NO-RELATION-trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/active-baselines.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/P1-active-body.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/P1-active-preview.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/P1-active-row.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/P2-active-body.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/P2-active-preview.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/P2-active-row.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/active-baselines/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/evidence-verification.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/exit-code.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/gate.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/OTHER-CANCEL/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/OTHER-CANCEL/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/OTHER-CANCEL/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/OTHER-CONFIRM/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/OTHER-CONFIRM/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/OTHER-CONFIRM/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/own-profile.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/owned-processes.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/plan.md
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/process-closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-CANCEL/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-CANCEL/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-CANCEL/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-CONFIRM/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-CONFIRM/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-CONFIRM/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-INNER-CANCEL/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-INNER-CANCEL/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/PROJECT-INNER-CANCEL/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/result.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/TYPE-CANCEL/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/TYPE-CANCEL/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/TYPE-CANCEL/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/TYPE-CONFIRM/after.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/TYPE-CONFIRM/before.png
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked/TYPE-CONFIRM/trace.zip
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/report.md
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/evidence-index.json

- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/root-acceptance-structure-check.json

- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/ATTRIBUTE-ONLY-INNER-CANCEL-after.png
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/ATTRIBUTE-ONLY-INNER-CANCEL-before.png
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/ATTRIBUTE-ONLY-INNER-CANCEL-trace.zip
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/attribute-results.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/calibrated-verify-attribute-branches.cjs
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/calibrated-verify-list-type-branches.cjs
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/calibration-attribute-results.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/calibration-distribution-results.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/closure.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/DISTRIBUTION-ONLY-CANCEL-after.png
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/DISTRIBUTION-ONLY-CANCEL-before.png
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/DISTRIBUTION-ONLY-CANCEL-trace.zip
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/distribution-results.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/E2E-08-ATTRIBUTE-ONLY-INNER-CANCEL-observation-5s.png
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/E2E-08-DISTRIBUTION-ONLY-CANCEL-observation-5s.png
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/evidence-verification.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/exit-code.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/handoff.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/owned-processes.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/plan.md
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/process-closure.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/report.md
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/result.json
- doc/tasks/20261016-dcc-e2e-08/project-label-ui-20261020/run.cjs

- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/plan.md
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/report.md
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/project-label-cancel.test.cjs
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/verify-delivery.py
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/upload-before.vue
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/authorized.patch
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/source-verification.json
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/handoff.json
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/closure.json
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/diagnosis-red.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/red.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/red.json
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/green.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/green.json
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/green-harness-missing-confirm.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/green-harness-missing-confirm.json
- doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/red-before-harness-calibration.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/plan.md
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/report.md
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/project-label-cancel.test.cjs
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/prepare-delivery.py
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/red.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/red-observations.json
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/harness-initial-parser.txt
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/trace-timeline.json
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/historical-evidence.json
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/proposed-not-applied.patch
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/handoff.json
- doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-r2-round2.cjs
- doc/tasks/20261016-dcc-e2e-08/r2-round2-verify-active-baselines.cjs
- doc/tasks/20261016-dcc-e2e-08/r2-round2-verify-attribute-branches.cjs
- doc/tasks/20261016-dcc-e2e-08/r2-round2-verify-list-type-branches.cjs
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/plan.md
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/calibrate.py
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/finalize.py
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/gate.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/calibration.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/result.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/handoff.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/report.md
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/owned-processes.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/process-closure.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/runner-exit.json
- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2/evidence-verification.json
- doc/tasks/20261016-dcc-e2e-08/repair-20261020-fix-handoff.json
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/report.md
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/category-readiness.test.cjs
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/verify-delivery.cjs
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/upload-before.vue
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/authorized.patch
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/source-verification.json
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/closure.json
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/red.json
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/red.txt
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/green.json
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/green.txt
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/green-harness-missing-global.json
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/green-harness-missing-global.txt
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/adjacent-regression.txt
- doc/tasks/20261016-dcc-e2e-08/classification-race-fix-20261020/category-binding-static.txt
- doc/tasks/20261016-dcc-e2e-08/classification-race-diagnosis-20261020/category-load-race.test.cjs
- doc/tasks/20261016-dcc-e2e-08/classification-race-diagnosis-20261020/diagnosis-report.md
- doc/tasks/20261016-dcc-e2e-08/repair-20261020-handoff.json

- doc/tasks/20261016-dcc-e2e-08/task.md
- doc/tasks/20261016-dcc-e2e-08/execution-log.md
- doc/tasks/20261016-dcc-e2e-08/verification-report.md
- doc/tasks/20261016-dcc-e2e-08/prepared.json
- doc/tasks/20261016-dcc-e2e-08/project-type-switch-protection.e2e.cjs
- doc/tasks/20261016-dcc-e2e-08/structure-check.cjs
- doc/tasks/20261016-dcc-e2e-08/fixture-template.json
- doc/tasks/20261016-dcc-e2e-08/static-review-check.json
- doc/tasks/20261016-dcc-e2e-08/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-08/runtime-interruption-20261017-2205.json
- doc/tasks/20261016-dcc-e2e-08/runtime-pause-20261017.json
- doc/tasks/20261016-dcc-e2e-08/runtime-pause-20261017-before-205424.json
- doc/tasks/20261016-dcc-e2e-08/attribute-branches-20261017.json
- doc/tasks/20261016-dcc-e2e-08/list-type-branches-20261017.json
- doc/tasks/20261016-dcc-e2e-08/verify-attribute-branches.cjs
- doc/tasks/20261016-dcc-e2e-08/verify-list-type-branches.cjs
- doc/tasks/20261016-dcc-e2e-08/setup-role-user.cjs
- doc/tasks/20261016-dcc-e2e-08/create-project.cjs
- doc/tasks/20261016-dcc-e2e-08/review-projects.cjs
- doc/tasks/20261016-dcc-e2e-08/configure-organization.cjs
- doc/tasks/20261016-dcc-e2e-08/configure-company.cjs
- doc/tasks/20261016-dcc-e2e-08/create-types.cjs
- doc/tasks/20261016-dcc-e2e-08/create-categories.cjs
- doc/tasks/20261016-dcc-e2e-08/create-public-matrices.cjs
- doc/tasks/20261016-dcc-e2e-08/grant-category-permissions.cjs
- doc/tasks/20261016-dcc-e2e-08/authorize-signatures.cjs
- doc/tasks/20261016-dcc-e2e-08/prepare-own-signature.cjs
- doc/tasks/20261016-dcc-e2e-08/ui-live.cjs
- doc/tasks/20261016-dcc-e2e-08/check-evidence-privacy.cjs
- doc/tasks/20261016-dcc-e2e-08/generate-fixture-files.py

- doc/tasks/20261016-dcc-e2e-08/runtime-pause-20261017-before-2223.json
- doc/tasks/20261016-dcc-e2e-08/candidate-doc-control-blocker.json
- doc/tasks/20261016-dcc-e2e-08/read-recovered-state.cjs
- doc/tasks/20261016-dcc-e2e-08/upload-candidate.cjs
- doc/tasks/20261016-dcc-e2e-08/candidate-task.cjs
- doc/tasks/20261016-dcc-e2e-08/fixture-real-ui.json
- doc/tasks/20261016-dcc-e2e-08/verify-candidate-relations.cjs
- doc/tasks/20261016-dcc-e2e-08/run-linked.cjs

- doc/tasks/20261016-dcc-e2e-08/baseline-stage.cjs
- doc/tasks/20261016-dcc-e2e-08/prepare-base-author-access.cjs
- doc/tasks/20261016-dcc-e2e-08/verify-active-baselines.cjs
- doc/tasks/20261016-dcc-e2e-08/acceptance-result-20261018.json
- doc/tasks/20261016-dcc-e2e-08/historical-cleanup-review.json
- doc/tasks/20261016-dcc-e2e-08/evidence-privacy-20261017.json
- doc/tasks/20261016-dcc-e2e-08/cleanup-preview-20261018.txt
- doc/tasks/20261016-dcc-e2e-08/closeout-status-20261018.json

## 第二轮新重跑 rerun-20261020-round2-1792462063637

Given 当前int_qms及运行证明 runtime-proof.json；When 使用原本人账号在8061真实页面重新执行7完整关联、3属性和7名单/类型分支；Then 逐分支以当前DOM断言，独立记录PASS/FAIL/BLOCKED、身份、trace、截图和关闭回执。使用本轮唯一源文件名；不提交旧终态、不改共享配置或产品。仅验收及问题收集，不做Git、重启、子Agent或cleanup apply。本轮状态in_progress，历史ROOT_ACCEPTED只适用于历史结果。


## 分类首次加载间歇错误诊断（2026-10-20）

状态in_progress。本轮限定只读生产源码及本例诊断测试；Upload页和共享帮助函数由02独占，不修改。历史FAIL/BLOCKED保持不变。
BDD: Given 首次类别目录未就绪而项目/文件类型已可选择；When 正式类型分类解析先完成、基础类别目录后完成；Then 正式有效分类最终应可用，不能留下错误且需要用户重选。用真实生产函数的竞态行为测试证明当前行为与正常顺序差异，独立记录RED；未经Root串行放行不改页面，不以诊断对照GREEN声称产品已修复。
预期验证：生产函数/调用顺序与精确源码指纹、类别先完成和后完成两顺序、缺失/多绑定/接口失败/过时请求边界；必要时仅最小真实UI复现，不全量重跑。无Maven/package/重启、API/DB业务动作、Git、子Agent。


## 分类诊断里程碑与保留

- [x] 当前生产调用顺序、历史HTTP事件次序与源码声明指纹。
- [x] 真实生产函数竞态行为证明；业务RED、正常顺序和负向边界。
- [x] 本轮根因/最小建议交接；生产GREEN及真实页面修复验收尚未执行。
- [ ] Root串行放行后由指定负责人修复Upload并GREEN/定向UI。

## 分类诊断证据索引

- doc/tasks/20261016-dcc-e2e-08/classification-race-diagnosis-20261020/category-load-race.test.cjs
- doc/tasks/20261016-dcc-e2e-08/classification-race-diagnosis-20261020/diagnosis-report.md
- doc/tasks/20261016-dcc-e2e-08/repair-20261020-handoff.json


## 串行放行分类竞态修复（2026-10-20）

Root已明确释放02独占窗口，本轮只改Upload首次目录就绪门禁及专属测试。BDD: Given 类型候选先完成且首次类别目录仍加载；When 作者准备选择类型；Then 控件保持加载/禁用，目录成功后首次选择立即得到唯一正式分类；目录失败明确报错并继续禁用，缺失/多绑定/权限/过时请求校验不变。先运行新专属业务RED，再最小修复、GREEN及相邻定向回归。历史诊断和FAIL不改写。完成后hold等待Round2，不开浏览器、不E2E、不构建重启、不Git。


## Root 接受定向修复复验（2026-10-20）

Root 已 review 并接受 ATTRIBUTE-ONLY-INNER-CANCEL、DISTRIBUTION-ONLY-CANCEL 两项当前修复真实页面复验。旧失败现存，新独立复验2PASS；旧 rerun-20261020-r2-round2 仍为15 PASS/2 FAIL，原 result/handoff/截图/trace 八文件 SHA 全部保留。新 project-label-ui-20261020 为独立2 PASS，非新跑完整17项；其余15项未复跑。模块自然加载源码指纹、5秒采样/截图、trace CRC/隐私、PID closure 已获Root接受。最新交接：[handoff](project-label-ui-20261020/handoff.json)。

本轮仅文档/交接及协调台账同步；按本轮明确授权，仅为本例向协调台账08项追加 repairRevalidation，覆盖原“Root独占协调目录”的本次必要例外，其余项及旧计数保持。结构与八文件SHA检查后HOLD；不操作浏览器/业务数据，不重跑其余15项。工程closeout保持blocked。

- doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2/structure-check.json
