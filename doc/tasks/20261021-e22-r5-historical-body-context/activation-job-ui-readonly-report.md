# 5625 新包后只读页面核对

2026-10-21 11:49–11:53 Asia/Shanghai；分支 int_qms。仅 Playwright 正式页面查询，无接口调用、执行一次、配置修改、业务写入、构建或服务操作。当前浏览器 context/browser 已关闭，runner 返回 CLOSED。

## 已取得证据

- `activation-job-ui-readonly/09-job5625-after1140-loaded.json` 及同名 PNG：从任务5625行进入调度日志，尝试输入开始2026-10-21 11:40:00、结束2026-10-21 23:59:59，点击搜索并等待加载结束，结果“暂无数据”。更正：控件源码为type=date，截图只显示2026-10-21，runner未保存自然请求URL，也未记录提交后的input值，因此实际beginTime/endTime及精确11:40过滤均为UNKNOWN，不能将本次空结果写成精确时间窗无日志。未再次查询或修改产品。无过滤默认倒序列表首行48167/02:47，仅支持“本轮未见新记录”。
- `10-job5625-detail-loaded.json/png`：等待任务编号5625显示后另行截图。状态正常，handler=dccControlledFileActivationJob，参数空，cron=0 * * * * ?，重试次数0、间隔0毫秒、超时30000毫秒。此前05为加载前截图，不用于字段结论。
- `07-job5625-logs-loaded.json/png` 与 `08-log48167-loaded.json/png`：最新历史日志48167，2026-10-21 02:47:00，显示成功；完整结果为 `{"1":"IllegalStateException: EXECUTION_CONTRACT_UNREGISTERED_DEFINITION","162":"实际生效版本数：0","121":"实际生效版本数：0","122":"实际生效版本数：0"}`。这是启动前旧包记录，不证明新包成功或失败，不推断实际失败文件。

## 原4216当前终态未取得

根据原任务证据精确对应文件2054545668044084216、编号DCC241016C15DOC、A/1、项目DCC241016C15延迟项目。在当前admin页面，受控浏览全局范围按该编号查询未返回记录；项目名称/编码查询DCC241016C15同样无记录，见 `11-original4216-list-loaded.json/png`、`12-original4216-project-query-no-results.json/png`。不能据此认定文件不存在、状态变化或权限原因；当前终态记 UNKNOWN。未打开正文（避免新增阅读登记），未补签或修改文件。原任务旧ACTIVE证据仅为历史，不重报PASS。

## 结论与下一步影响范围

新包运行分支尚未取得真实执行证据，保持未验证。Root确认 startup-sync 关闭且 RAM jobstore；任务列表正常不证明scheduler已注册，SchedulerManager.triggerJob也不负责注册不存在job，因此不建议简单点击执行一次。

若后续恢复注册或安排一次全局5625运行，须先明确只注册该任务的实现/运行范围，并冻结全部启用租户及各租户完整due清单、任务归属和当前状态；批准范围应覆盖每个成功对象独立提交、当前版本指针、旧版作废、保留期、审计、生命周期事件及下发。重试次数0仅排除该配置的自动重试，不缩小全局业务范围。不启用全局sync，不擅自触发。原4216可由具备既有对象可见性的账号进行正式页面只读核对；本轮不继续扩展。
