# 定向任务只读前置回执

2026-10-21约14:32–14:35 Asia/Shanghai；分支int_qms。Root授权的独占一次窗口，未创建新文件，未打开任务参数指向的历史文件。

## 运行归属

监听48061 PID32436，java命令指向本仓库`output/runtime/int_qms/backend-e15-isolation-20261021.jar`，profile=dcc-local-development、startup-sync-enabled=false。监听8061 PID20976，Vite路径本仓库IntRuoyiFronted，branch-qms/8061/strictPort。与协调`e15-isolation-runtime-proof.json`进程一致；本轮未重新构建、重启或改参数。

## 正式管理员页面结果

真实登录芋道源码/admin，菜单基础设施→定时任务，处理器字段输入完整`dccControlledFileTargetedActivationJob`后搜索，加载后总计1条，并逐字核对handler确为精确目标。

|字段|已加载页面值|
|---|---|
|任务ID|5629|
|名称|DCC241016Root定向生效|
|handler|dccControlledFileTargetedActivationJob|
|参数|1:2054545668044084228|
|状态|暂停|
|cron|0 * * * * ?|
|重试次数/间隔|0 / 0毫秒|
|监控超时|未开启|

行显示操作：修改、开启、删除、更多；更多菜单显示执行一次、任务详细、调度日志。仅打开任务详细及调度日志，未点击任何写操作。详情的后续执行时间为cron理论值，不作RAM注册证明。

通过5629行菜单进入日志，共15条。默认倒序首条48060，2026-10-20 14:39:24～14:39:25，92毫秒，执行次数1，参数`1:2054545668044084369`，状态成功，加载后详情结果“实际生效版本数：0”。它是历史执行，参数与当前保存参数不同；不能据此证明当前RAM注册，也未据其参数打开任何文件。

## 归属与租约边界

E16 `doc/tasks/20261016-dcc-e2e-16/rerun-20261020-r2-round3/verification-report.md`明确：获Root授权退役5628、同handler新建5629；14:39:27恢复原参数4228和全部字段、暂停，Root继续持有，所有E16进程关闭。当前页面配置与此交接相符，支持“此前Root/E16自有且已暂停”。旧`targeted-activation-lease.json`仍列5627，不能当成当前5629注册回执。

本轮窗口由Root独占授权、其他例离线；本轮无平行操作。正式UI没有实时租约字段，无法独立证明全局无其他租约，交Root协调确认，不能仅靠页面暂停作出该判断。

## 注册结论与交接

**当前RAM job/trigger是否已注册：UNKNOWN。** 页面只有保存任务字段、理论nextTimes和昨日执行记录，无法唯一判断当前进程内注册。本轮只读查询未消除此边界；不得据此开启旧目标或点击执行一次。Root决定精确恢复路径，待其明确放行后才可对5629做任何退役/配置操作；本回执不是删除授权。全局5625及startup-sync保持禁止。

## 证据与关闭

`targeted-job-ui-preflight/02-targeted-handler-loaded-list.json/png`、`03-targeted-row-menu.json/png`、`04-targeted-loaded-detail.json/png`、`05-targeted-loaded-logs.json/png`、`06-log48060-loaded-detail.json/png`。

同目录`trace.zip`：登录完成后才启动录制；脱敏去除network内容、frame snapshots及非图片网络资源，保留37条动作和295帧截图，CRC PASS，networkBytes=0、frameSnapshots=0。见`trace-privacy-check.json`；原始trace.raw.zip已移除。该trace不提供网络请求证据或完整DOM回放，仅支持脱敏后的页面动作/截图回看。

context/browser返回CLOSED；runner随后Ctrl+C退出。无启用、修改、删除、触发、同步任务，无创建文件、DB/API业务写入、服务或Git操作。新文件业务未执行，无新增业务PASS。
