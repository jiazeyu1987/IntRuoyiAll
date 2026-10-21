# E2E-14 第二次完整重跑第三轮：Root review 独立对照

## 目标、依据与状态

本记录只整理既有证据的验收归属，不新增一次运行，也不覆盖原始档。依据为用户本轮转达的 Root review（来源线程 01a13ad1-10c0-72a3-b271-f192b6058114）：原合同 14-C 生效事实、14-D 日常正文已由正式授权 D 的 FINAL 真实页面核验通过；额外 U 无 VIEW，其禁用属于权限边界观察，不能覆盖原合同结果。正式用例见 doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md 的 E2E-14。

总体结论：**原合同六分支 PASS，仍有未解决的 D 签名弹窗及阅读计时 UI 异常 OPEN**。这不是无异常全绿，也不表示缺陷关闭或工程任务 completed。继续 HOLD，不重跑、不启浏览器、不改权限或产品。

文档整理的 Given/When/Then：Given D 的原合同证据与追加 U 观察复用了结果槽位，When 按真实执行账号和正式合同分别归档，Then 原合同六分支按 D 最终证据记录 PASS，U 单列为权限边界观察，原始 state 的 4 PASS/2 FAIL 保持原样，D 真实 UI 异常继续 OPEN。

里程碑：已核对合同、原档及 Root 指示 → 建立原档 SHA256 基线 → 新增独立对照和修订交接 → 结构、引用及原档不变检查。验证范围仅文档，不重跑历史 evidence checker，不重写 trace，不执行 E2E、构建、服务、Git 或 cleanup。

## 本轮文件身份

- runId：rerun-20261020-r2-round3；文件：2054545668044084364；编号：DCC241020C14R2R31792476051458；版本：A/1；项目：284。
- BPM：a1fd2026-cc4b-11f1-ada5-b082e25ec548；D 任务：2760ec1d-cc4c-11f1-ada5-b082e25ec548。
- 本轮原始目录：[rerun-20261020-r2-round3](rerun-20261020-r2-round3/)。本文不计入历史 4215、4317 文件的通过结论。

## 原合同结果：六分支 PASS

| 分支 | Root review 归属 | 本轮真实证据 |
|---|---|---|
|14-A 取消签名后重开|PASS|D 同一任务取消后尚未生效、尚未受控；D-A-cancel-not-effective.png、D-trace-1.zip；原 state 的 14-A。|
|14-N1 空密码|PASS|D 必填提示，负例正式审批请求 0；D-negative-empty-password.png；原 state 的 14-N1。|
|14-N2 空意见|PASS|D 必填提示，负例正式审批请求 0；D-negative-empty-reason.png；原 state 的 14-N2。|
|14-B 重试并正式签名|PASS，保留 UI 异常 OPEN|D 正式 approve-task 仅 1 次；fresh D 原待办消失，文控节点 #1198 · VALID。D-result.json 保留首次界面失败；FINAL-result.json、FINAL-dom/14-C-doc-control-signature.png 证明实际签名终态。|
|14-C 生效事实|PASS|正式授权 D 的 FINAL exit0：签名与实际生效均 2026-10-20 14:05:43，受控及日常启用均 14:05:49；FINAL-trace-1.zip 中 call@151/155/159/163；FINAL-dom/14-C-effective-detail.png。|
|14-D 日常正文|PASS|正式授权 D 的 FINAL exit0：同一文件 A/1、受控（已生效），正文非空；FINAL-dom/14-D-active-row.png、14-D-nonempty-pdf-canvas.png。两次 canvas 均 685×969，41486 采样点中非白 1128。|

以上时间为页面 Asia/Shanghai 原始值，精度到秒；签名/生效时间对应用户 America/New_York 2026-10-20 02:05:43。仅证明页面显示精度一致，不声称服务端微秒相等。14-B 的业务 PASS 来自有效签名与最终页面证据，不把未知 HTTP 响应补成成功。

原合同计数：主分支 4 PASS、空值负例 2 PASS、FAIL 0、BLOCKED 0。该计数只描述原合同六个业务断言；总体仍带未解决 UI 异常。

## 追加 U：权限边界观察，独立于六分支

UREAD 真实执行保留：U 登录成功，列表可见同一 A/1 生效行，浏览按钮 disabled，两次等待超时，exit2、业务写请求 0。证据为 UREAD-result.json、UREAD-trace-1.zip、UREAD-C-failure.png、UREAD-D-failure.png，以及 UREAD-dom/14-D-active-row.png。

Root 已确认该场景现有 VIEW 矩阵仅授权 D。U 禁用归为 **OBSERVED_PERMISSION_BOUNDARY**：不计为原合同 14-C/14-D FAIL，也不直接认定为产品缺陷。本轮未扩权，不声称完成全部权限入口验收。

原 state.json 的 14-C/14-D 被追加 U 复用槽位写为 FAIL，形成原始 4 PASS/2 FAIL；原 result.json、handoff、report 与 evidence-validation.json 同样保留该聚合历史。**这些原档不改写**。本次修订通过独立对照解释归属：D 原合同 PASS 与 U 只读超时同时真实存在，不能用 U 槽位覆盖 D 的业务证据。

## 未解决异常及保留边界

**E14-R2R3-01：OPEN。** D 首次正式签名后，弹窗 65 秒未关闭，弹窗及阅读计时区域出现 `Current user cannot access this controlled file`。原 D-result.json exit2、D-failure.png 与相应 trace 全部保留。后续 FINAL 生效与正文通过不能消除该 UI 异常，也不能把它解释为 U 的权限观察。

定位身份为上述 fileId/BPM/taskId；原 D 运行窗口为上海时间 2026-10-20 14:05:24—14:06:48，对应用户纽约时间 02:05:24—02:06:48。approve-task POST 只发 1 次，完成响应未捕获，HTTP 状态仍 UNKNOWN；原 reading-session/start 3 条、stop 2 条自然完成响应为 HTTP 200。未解析响应 JSON 判断根因，不能确定错误来自哪个接口。

原 E14-R2R3-02 在旧交接中标为 OPEN_REQUIRES_CONTRACT_REVIEW；本次 Root review 将其验收归属明确为上述 U 权限边界观察，仅在新交接表达，不修改旧档。

原 EXACTD 任务 URL 回读头部超时与项目区域 30000ms timeout、CHECK 的旧待办数量预期错误继续作为历史诊断保留。它们不推翻 FINAL 终态，不被新记为修复或重跑通过；本次没有确定它们的根因。

## 证据与修订交接

原 15 份 trace、48 张截图及关闭回执保留；原校验 PASS 与浏览器/context/runner 全部关闭为既有证据，本次未重新执行验证动作。

本次仅新增三个独立文件：本对照、[修订交接 JSON](rerun-20261020-r2-round3-root-review-handoff.json)、[原档不变及结构校验](rerun-20261020-r2-round3-root-review-validation.json)。后者逐项保存 87 份原文件的 SHA256，并在写入后比对。原 task.md、execution-log.md、verification-report.md、原 plan/report/handoff、脚本及完整本轮证据目录均保持原样。

工程状态沿用 blocked；Git/cleanup 未授权，未标 completed。下一步仍为 HOLD，D UI 异常 OPEN 留 Root 后续处理。本文及新 JSON 是原交接的验收归属补充，阅读时应同时保留原档上下文。
