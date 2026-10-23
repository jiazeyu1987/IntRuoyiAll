# E2E-22 第四轮独立验收结果

## Current Status

blocked — 业务验收 22-A、22-B、22-C 全部 PASS；本轮禁止 Git 提交推送和证据清理，待 Root 复核，不标 completed。历史报告保持原状。本轮先进入 ready_for_closeout，再核验 trace、截图及进程关闭。

真实执行时间：2026-10-20 11:02:35–11:27:33（北京时间）。使用 int_qms 的 8061/48061，前端 PID20976、后端 PID27908。运行 Jar 为 backend-e2e24-spreadsheet-20261020-030326.jar，SHA256 ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab；启动浏览器前独立核对 PID、Jar 路径、哈希和健康状态。权威来源：doc/tasks/20261020-qms-e2e24-runtime-update/runtime-proof.json。

| 分支 | 本轮结果 | 真实页面证据 |
| --- | --- | --- |
| 22-A | PASS | Reader 逐版核对字段、四项属性、提交/受控/生效/下发时间、BPM 与修订来源，A2→A1→A2。U 本轮检出 A2，逐项核对检出人/时间/原因；A1 三项未检出；返回 A2 保留同一锁。UI 撤销后两版均未检出。 |
| 22-B | PASS | 两版固定 Rel1 A1，A1 另有后补 Rel2 A1；全部三处正文 canvas 非空，旧标识搜索命中、新标识搜索无匹配；原部门、签名、来源及仅 A2 的整改人/期限隔离。另重新打开 Rel1 当前 B1，未借用历史存在性结论。 |
| 22-C | PASS | A1 引用去重为 2 项目、3 文件夹，A2 为 1 项目、1 文件夹；四条引用正文全部实测 canvas、版本、正反搜索。 |

本轮 Reader 新参数断言 7 项、U 新检出事实断言 6 项，均从本轮 startedAt 过滤，未把 manifest 中继承的旧数组计入本轮。原样本均采用文控标准化签名后生效，预设/延迟日期不适用；没有遗漏适用分支。

## 对象与证据

- Main A1：2054545668044084249，BPM 1842903f-cb00-11f1-ad52-b082e25ec548。
- Main A2：2054545668044084260，BPM 1db3db90-cb73-11f1-9356-b082e25ec548。
- 固定关联：Rel1 A1 2054545668044084243；当前 Rel1 B1 2054545668044084258；Rel2 A1 2054545668044084248。
- 完整独立交接：doc/tasks/20261016-dcc-e2e-22/rerun-20261020-round4-handoff.json。
- Reader 本轮：doc/tasks/20261016-dcc-e2e-22/e2e-artifacts/fixture-1792465355294/，含 round4-reader-complete-manifest.json、六类参数 JSON、trace-1.zip、16 张 PNG。
- U 本轮：doc/tasks/20261016-dcc-e2e-22/e2e-artifacts/fixture-1792466510259/，含 round4-author-complete-manifest.json、trace-1.zip、7 张 PNG 和进程关闭回执。

## 首次异常及实际边界

Reader 最后一条负向搜索在 5 秒断言上限内用时 23.837ms 成功，随后关闭正文弹窗。此前误把交互 runner 等待 stdin 当作搜索仍在执行，造成约 12 分钟多余等待，未发现业务 hang。准确 before/after、返回文本及计时见 Reader 的 round4-negative-search-proof.json。

U 检出后页面停在存储目录，脚本漏切“项目文件与引用”，排队的 8 次只读定位/URL 断言失败；原始错误全部保留。通过正式页签切换后重新执行完整检出隔离及撤销回读通过，失败步骤未触发额外业务写入。另首次报告生成命令语法错误已修正，非业务失败。

## 恢复、关闭与验证

本轮只检出一次、撤销一次，均由 Playwright 页面完成，自然请求元数据各为 HTTP200；撤销时间 11:26:37。没有新申请/版本、没有权限或配置变更，原夹具及历史证据保留；页面阅读自然遥测仍保留。

两个 runner（15824、24920）均 finish 正常退出 0，finish 在 context.close/browser.close 成功后返回。操作系统核对原 runner 以及 U 自有 Chrome 全部进程均不存在，未停止其他任务浏览器。完整 PID 列表见 U 的 round4-owned-processes-before-close.json / round4-owned-processes-after-close.json。

2 份脱敏 trace 的 ZIP CRC 通过，network 和响应资源已剔除，保留真实 UI 快照与截图；23 张 PNG 结构通过，并目视核对 A2 检出正例、A1 未检出负例。独立 JSON 已校验新断言数量、对象身份、撤销事实、负向搜索结果及证据路径。

本轮没有生产代码修改、构建、重启、数据库业务操作、Git 提交推送或子 Agent。cleanup preview/apply 未运行：本轮限验收归档，要求保留本轮及历史证据；工程状态如实 blocked，未声明清理或提交完成。
