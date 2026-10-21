# E2E-14 第二次完整重跑第三轮交接

runId：rerun-20261020-r2-round3。结论：REVIEW_REQUIRED_OPEN_ANOMALIES，执行结束并 HOLD，不启动第四轮。

本轮从新文件完成 U 单次提交、S 审核、L 签名指派 C、C 会签、A 批准并确认 U 为文件负责人，以及 D 取消、空密码、空意见和一次正式签名。D 新上下文已确认签名与受控生效终态、非空正文，但首次签名界面错误仍 OPEN；追加 U 只读核验无法打开正文。因此不能声明本轮全程无异常 PASS。

## 身份与运行态

- 分支 int_qms；前端 8061/PID 20976；后端 48061/PID 41508。执行前门禁和结束复核一致，未重启服务。
- 统一 proof：doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r2-runtime-proof.json，仅作只读来源。
- Jar：output/runtime/int_qms/backend-rerun-r2-20261020-1301.jar；SHA256：709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147。
- 租户芋道源码，原本例 U/S/L/C/A/D 独立账号；项目 284，类别 119。未更改权限或共享配置。
- 文件 2054545668044084364，编号 DCC241020C14R2R31792476051458，版本 A/1。
- BPM a1fd2026-cc4b-11f1-ada5-b082e25ec548；原 D 任务 2760ec1d-cc4c-11f1-ada5-b082e25ec548。
- 来源为任务已有合法 PDF 的独立命名副本；历史 4215、4317 文件结果不计入本轮。

## 本轮真实页面结果

| 项目 | D 账号证据 | 追加 U 只读核验 |
|---|---|---|
|14-A 取消后重开|PASS：同一任务，尚未生效、尚未受控|不重复业务动作|
|14-N1 空密码|PASS：必填提示，正式审批请求 0|不重复业务动作|
|14-N2 空意见|PASS：必填提示，正式审批请求 0|不重复业务动作|
|14-B 正式签名|一次请求；fresh D 原待办 0，#1198 · VALID；首次界面完成异常 OPEN|未重签|
|14-C 生效事实|FINAL exit0：签名及实际生效均 2026-10-20 14:05:43，受控及日常启用均 14:05:49|FAIL：浏览按钮禁用，详情与签名不可读|
|14-D 日常受控正文|FINAL exit0：同一文件 A/1 受控（已生效），正文 canvas 非空|FAIL：列表可见生效行，浏览按钮禁用，正文不可读|

表中业务时间为页面 Asia/Shanghai 时间，精度到秒；14:05:43 对应 UTC 06:05:43、用户 America/New_York 02:05:43。只证明页面显示精度一致，不证明服务器微秒相等。FINAL trace 中 call@151/155/159/163 保留上述四个时间的原始 DOM 返回值。

D PDF canvas：685×969；每次采样 41486 点，其中非白 1128 点；两次正文预览均完成。签名截图、正文截图已目视复核，正文有文字及受控章。本例不扩展声明盖章细则。

## 保留的异常及归因边界

1. **D 首次正式签名完成异常 OPEN**。D-result.json 实际 exit2；等待签名弹窗关闭 65 秒超时，弹窗仍在。页面出现 `Current user cannot access this controlled file`，阅读统计区域同时显示 `阅读计时或统计失败：Current user cannot access this controlled file`。D 在上海时间 14:05:24—14:06:48 运行；正式签名落库可见时间 14:05:43，生效受控可见时间 14:05:49。业务已经生效的证据不消除页面异常，弹窗未关闭也不能推断签名未持久化。
2. **请求分开记录**。D 自然发出 approve-task POST 仅 1 次，没有捕获完成响应，HTTP 状态 UNKNOWN，不写 200/成功。reading-session/start 共 3 条、stop 共 2 条完成响应均 HTTP 200；只记录状态，不解析业务 JSON，不能据此把界面错误确定归因到签名接口或阅读接口。
3. **fresh D 原任务 URL 回读未完成**。EXACTD 访问同一 fileId/taskId/processInstanceId 后可见受控浏览页，未得到预期详情页头，45 秒超时 exit2；另见项目区域 `timeout of 30000ms exceeded`。保留 requestedTaskUrl、可见文本、截图和 trace；未保存跳转后 URL，因此不编造具体跳转地址或认定该超时为签名失败。
4. **U 追加只读核验失败**。UREAD 本人登录成功，当前生效受控版本唯一行可见，A/1、受控（已生效）；“浏览”按钮 disabled，详情与日常正文两次等待均 22 秒超时，exit2，业务写请求 0。本例历史查看矩阵仅授权 D，未在本轮扩权；目前只能确认 U 无法浏览，是否符合文件负责人权限合同交 Root 判断，不能直接认定后端权限根因。
5. **CHECK 的旧待办预期失效**。首次只读诊断仍预期待办 1 条，实际 0 条，exit2。该诊断错误保留；随后 FINAL 新上下文从真实生效终态完成确认，没有重复签名。

UREAD 复用了同一结果槽位，把 state.json 的 14-C/14-D 更新为本次 U FAIL；没有改回 PASS。D 的 FINAL exit0、截图、trace、两次 canvas 采样独立保留。当前 state 为 4 PASS/2 FAIL，描述的是最后 U 核验覆盖后的结果；不能用它覆盖 D 已确认的终态，也不能用 D 成功覆盖 U 失败。

## 证据和关闭

- 正式执行与只读核验：U/S/L/C/A/FINAL exit0，D/CHECK/EXACTD/UREAD exit2；10 份结果均保留，非零运行不统一称作脚本校准。
- evidence checker exit0：15 份 trace CRC、空 network payload、已知凭据与 token 检查 PASS；48 张截图，已知凭据违规 0。该 PASS 只表示证据检查通过，业务接收仍需 review。
- 登录、密码输入和正式签名确认在 trace 关闭区间，未声明逐点击完整录制。trace 删除 console/pageError 事件仅为证据隐私处理；运行器 pageErrors 数组另行保留。
- runtime-closeout.json：所有 browser/context 关闭，remainingContexts=0；10 个 runner PID 与本轮脚本进程查询均无存活。本机原服务继续运行。
- 本轮无产品改动、Maven/构建、服务重启、API/DB 业务、权限调整、Git 写入、子 Agent、共享协调文件写入。
- 文件、签名、账号及本轮证据保留。Git 和 cleanup 未授权且当前范围禁止 cleanup，因此不运行 preview/apply，不标 completed；工程状态 blocked，后续动作 HOLD。

交接入口：rerun-20261020-r2-round3-handoff.json；独立证据目录：rerun-20261020-r2-round3/。Root 可按上述 fileId、BPM、taskId 及上海时间窗口关联日志。根因未确定，不申请或执行新的业务尝试。
