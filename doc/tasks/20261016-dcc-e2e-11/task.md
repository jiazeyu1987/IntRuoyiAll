# E2E-11 主管先审并签名

## Goal

独立验证E2E-11，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — R4剩余业务4PASS；自然晚到旧响应未观察到；HOLD等待Root review，无Git/cleanup apply授权。

## BDD

- Given U已最终提交
- When U、S及会签人分别查看待办；S分别取消签名、完成签名并通过
- Then 先由S审核；未签名不能推进；S通过并签名后才进入会签，页面记录正确签名人、时间及申请。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 真实页面准备并核验本例账号/fixture，执行 E2E 及任务自有数据收尾。
4. [x] Root review脚本/trace/可见页面断言，逐分支给出实际结论。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C11，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep
- doc/tasks/20261016-dcc-e2e-11/repair-r4-real-report.md
- doc/tasks/20261016-dcc-e2e-11/repair-r4-real/
- doc/tasks/20261016-dcc-e2e-11/artifacts/repair-r4-real/
- doc/tasks/20261016-dcc-e2e-11/r4-real-audit.cjs
- doc/tasks/20261016-dcc-e2e-11/r4-race-probe.cjs
- doc/tasks/20261016-dcc-e2e-11/r4-real-execute.cjs
- doc/tasks/20261016-dcc-e2e-11/r4-real-business.cjs
- doc/tasks/20261016-dcc-e2e-11/r4-login-preflight.cjs
- doc/tasks/20261016-dcc-e2e-11/repair-r4-existing-baseline.txt
- doc/tasks/20261016-dcc-e2e-11/repair-r4-red-baseline.txt
- doc/tasks/20261016-dcc-e2e-11/repair-r4-green.txt
- doc/tasks/20261016-dcc-e2e-11/repair-r4.diff
- doc/tasks/20261016-dcc-e2e-11/repair-r4-root-cause.md
- doc/tasks/20261016-dcc-e2e-11/r3-round2-audit.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-prepare-continuation.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-upload-calibrated.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-readonly-type.cjs

- doc/tasks/20261016-dcc-e2e-11/task.md
- doc/tasks/20261016-dcc-e2e-11/execution-log.md
- doc/tasks/20261016-dcc-e2e-11/verification-report.md
- doc/tasks/20261016-dcc-e2e-11/prepared.json
- doc/tasks/20261016-dcc-e2e-11/e2e-11-supervisor-signature.cjs
- doc/tasks/20261016-dcc-e2e-11/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-11/fixture-ui-console.cjs
- doc/tasks/20261016-dcc-e2e-11/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-11/checkpoint.json

- doc/tasks/20261016-dcc-e2e-11/actor-action.cjs
- doc/tasks/20261016-dcc-e2e-11/add-project-reviewer.cjs
- doc/tasks/20261016-dcc-e2e-11/approve-project.cjs
- doc/tasks/20261016-dcc-e2e-11/assign-approver-role.cjs
- doc/tasks/20261016-dcc-e2e-11/check-post.cjs
- doc/tasks/20261016-dcc-e2e-11/checkpoint-latest.json
- doc/tasks/20261016-dcc-e2e-11/create-category.cjs
- doc/tasks/20261016-dcc-e2e-11/create-project.cjs
- doc/tasks/20261016-dcc-e2e-11/create-scope.cjs
- doc/tasks/20261016-dcc-e2e-11/enable-supervisor-signature.cjs
- doc/tasks/20261016-dcc-e2e-11/execute-e2e11.cjs
- doc/tasks/20261016-dcc-e2e-11/fix-stage-name.cjs
- doc/tasks/20261016-dcc-e2e-11/fresh-login.cjs
- doc/tasks/20261016-dcc-e2e-11/grant-category-access.cjs
- doc/tasks/20261016-dcc-e2e-11/grant-role-menus.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-approval-permissions.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-menu-definitions.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-menu.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-pages.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-project-request.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-project.cjs
- doc/tasks/20261016-dcc-e2e-11/inspect-supervisor-todo.cjs
- doc/tasks/20261016-dcc-e2e-11/post-readonly-evidence.cjs
- doc/tasks/20261016-dcc-e2e-11/recover-submitted-application.cjs
- doc/tasks/20261016-dcc-e2e-11/retry-post.cjs
- doc/tasks/20261016-dcc-e2e-11/run-step.cjs
- doc/tasks/20261016-dcc-e2e-11/setup-org.cjs
- doc/tasks/20261016-dcc-e2e-11/setup-relations.cjs
- doc/tasks/20261016-dcc-e2e-11/setup-roles.cjs
- doc/tasks/20261016-dcc-e2e-11/setup-users.cjs
- doc/tasks/20261016-dcc-e2e-11/upload-application.cjs
- doc/tasks/20261016-dcc-e2e-11/upload-signature-image.cjs
- doc/tasks/20261016-dcc-e2e-11/verification-result.json
- doc/tasks/20261016-dcc-e2e-11/verify-logins.cjs
- doc/tasks/20261016-dcc-e2e-11/verify-post-approval.cjs
- doc/tasks/20261016-dcc-e2e-11/verify-post-task-lists.cjs

- doc/tasks/20261016-dcc-e2e-11/rerun-prepare-20261020.cjs
- doc/tasks/20261016-dcc-e2e-11/rerun-upload-20261020.cjs
- doc/tasks/20261016-dcc-e2e-11/rerun-business-20261020.cjs
- doc/tasks/20261016-dcc-e2e-11/rerun-execute-20261020.cjs
- doc/tasks/20261016-dcc-e2e-11/rerun-audit-20261020.cjs
- doc/tasks/20261016-dcc-e2e-11/rerun-20261020-round2-handoff.json
- doc/tasks/20261016-dcc-e2e-11/rerun-20261020-round2-audit.json
- doc/tasks/20261016-dcc-e2e-11/r2-round2-prepare.cjs
- doc/tasks/20261016-dcc-e2e-11/r2-round2-business.cjs
- doc/tasks/20261016-dcc-e2e-11/r2-round2-execute.cjs
- doc/tasks/20261016-dcc-e2e-11/r2-round2-run.cjs
- doc/tasks/20261016-dcc-e2e-11/r2-round2-recover.cjs
- doc/tasks/20261016-dcc-e2e-11/r2-round2-audit.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-prepare.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-business.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-execute.cjs
- doc/tasks/20261016-dcc-e2e-11/r3-round2-run.cjs
- doc/tasks/20261016-dcc-e2e-11/artifacts/rerun-20261020-r3-round2
- doc/tasks/20261016-dcc-e2e-11/rerun-20261020-r3-round2
- doc/tasks/20261016-dcc-e2e-11/artifacts/rerun-20261020-r2-round2

## Evidence retention

Root已接受业务验收；按最新指令继续保留本目录所有artifacts/fixtures及受保护本地凭据，不再执行业务或清理，不运行cleanup apply。原始失败不覆盖，最终业务结果见verification-result.json。未执行Git写操作。

## Root review

reviewStatus=ROOT_ACCEPTED，时间2026-10-18T05:25:16.916171+08:00；结论来源：[e2e11-root-review.json](../20261016-dcc-24-e2e-coordination/e2e11-root-review.json)。七项业务均已接受；工程仍blocked（无Git授权），不标completed。

## 2026-10-20 第二轮重跑

Given 当前运行27908/20976、原真实U/S/L1/C1及本次独立申请；When 通过页面完成上传、取消、空签名、正式主管签名并查看前后待办；Then 7项分别核验，同一新file/BPM/task/签名可追溯，全部浏览器/context/runner结束。只写本目录；不改产品/公共配置/数据库，不启动下一轮、不cleanup apply。

本轮范围核对：Root明确用户管理中普通用户查到自己不是额外门槛，组织关系按正式上传确认框及本人主管待办核对；保留上述7项全部业务门禁。前3次准备阻塞未创建申请，独立保留，不算产品失败。本次正式申请4308/签名1073，独立结果见rerun-20261020-round2-handoff.json。

## rerun-20261020-r2-round2 正式执行

目标：按原合同全量验证E2E-11七项分支，独立证据，不复制历史PASS。

BDD：Given 新运行41508/20976、原真实U/S/L1/C1及新唯一源文件；When U通过正式上传确认框提交，S取消/空密码/正式签名，L1/C1分别查看前后待办；Then U无本人任务，取消/空密码0审批写且同task，新签名仅一次，S已办通过后L1未指派且审核禁用、C1无任务，页面精确file/version/BPM/签名/意见一致。

里程碑：runtimeReady门禁 → 四账号fresh登录 → 新唯一申请 → 七分支 → trace/截图/首次失败归档 → 全部进程关闭回执 → handoff.json/HOLD。

设计约束：只修改本目录测试资产；原数据只读复用账号/项目/类别；所有业务动作走真实UI，不解析响应JSON作业务依据；不调用API/DB、不改产品/配置、不构建重启、不Git、不子agent、不启动第三轮。首次失败原样留存，必要脚本校准单独记；禁止重复签名或重复提交未知结果申请。

首次准备失败：新申请已提交，S打开详情时默认5秒断言仍见“本次申请身份尚未核验”；原始结果、截图及trace保留在本轮prepare/。只读recovery/重新从S待办打开同一申请4337/BPM ccb4a110-cc47-11f1-ada5-b082e25ec548，60秒内可见编号和主管动作面板；0审批POST，browser/context关闭。只校准真实详情等待时间，不改变业务断言；不重复上传，正式业务仅执行一次。

## R3第二轮 BDD与约束

Given 当前R3 proof/2095源码快照、原真实账号和唯一新文件；When fresh登录、真实UI提交，主管取消/空密码/正式签名、各角色前后查看；Then 原合同七分支逐项判定，同file/version/BPM/签名一致。运行超时保留原始现场，未知提交/签名只做一次只读核验，不重试写操作。只维护本目录测试资产，不改产品/DB/服务/Git/共享配置，不子Agent；历史未知问题保留。本轮模型要求gpt-6.1-sol/low优先于旧high。


## R4 审批中心乱序响应修复

目标：仅 index.vue:getList 和直接必要单元测试；保留现有报错，禁止E2E/构建/重启/DB/Git写入/子Agent。dispatch HOLD，等待Root review。
BDD: Given 两个重叠列表请求且新请求含关键词；When 新旧请求分别按两个顺序成功；Then 只有最新请求能修改list/total/error/loading/徽标/loaded-route。
BDD: Given 新请求成功或仍在等待；When 旧请求失败；Then 不清空新数据、不显示旧错误、不提前结束loading。
BDD: Given 最新请求失败；When 旧请求随后成功；Then 保留正式失败提示和空列表，不恢复旧数据。
里程碑：根因review → 真实getList语义RED → 最小序号门禁GREEN → 已有定向合同 → repair-r4-root-cause.md → HOLD。
设计约束检查：已有PENDING_DELAYED_EFFECTIVE_CONFIRM状态标签为并行改动，保留；无新增缓存/重试/取消框架。测试提取当前SFC实际getList执行，不复制实现。


## R4真实页面定向复验

Given 当前8061/20976、48061/38176及原R3任务自有申请4402（0审批POST，非UNKNOWN）；When fresh真实登录核验主管原任务，再经原审批中心筛选触发重叠请求并等待自然旧请求完成，完成主管签名和L1/C1及签名证据剩余断言；Then 新筛选结果不被旧响应覆盖且同file/BPM/task/签名可追溯。只有自然请求元数据、可见DOM为依据；不拦截/模拟/直接API/DB，不重放UNKNOWN、不构建重启Git写、不子Agent。原前三PASS标明未重跑，R4和R3证据分开。若自然未出现旧响应晚到则明确该覆盖未观察到，不假称PASS；若延迟生效job影响本例则BLOCK不绕过。
