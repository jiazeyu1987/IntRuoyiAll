# E2E24 表格转换修复共享运行包更新

## Current Status
blocked — 运行交付PASS；当轮禁止Git提交/推送和删除证据，工程收尾未completed。已先进入ready_for_closeout，cleanup preview PASS（delete为空、warnings none），未执行apply。

## 目标
只更新本机 int_qms 48061 的共享运行包，保留既有修复与原始失败样本；构建、关键 class 核验、health 与真实页面登录后关闭验证浏览器并交回 Root。

## 里程碑
- 独立核对 E2E24 冻结源码/编译产物、15 项 GREEN 证据、浏览器和 Maven 窗口。
- 全量 server package，保留 testCompile；既有 74 class 加新增转换 class 核验后复制不可变 Jar。
- 配置预检后仅替换旧 PID30476，验证 health、真实登录、浏览器关闭并回传证据。

## Expected Verification
BDD: Given 冻结交接和独占窗口，When 完整构建并替换明确归属的 QMS 旧进程，Then 新运行 Jar 哈希与核验产物一致、关键类完整、health UP、真实用户可登录，且其他运行态不变。
本任务无生产代码改动；复用 E2E24 RED/GREEN 证据，不重跑 E2E10 或转换业务。

## 设计约束检查
只复用现有配置与签名密钥，内存传递，不输出凭据；不改变端口、数据源或转换策略。无 Git 提交/推送、数据库/业务 API 写入、远程操作、子 Agent 或其他任务派发授权。只允许向 Root 回传证据。原 4270/4271 失败产物保留。
工程收尾遵守当轮禁 Git/删除证据约束；运行交付完成后如实记录工程 blocked，不宣称 completed。

## Cleanup Keep
- doc/tasks/20261020-qms-e2e24-runtime-update/cleanup-preview.txt
- doc/tasks/20261020-qms-e2e24-runtime-update/execution-log.md
- doc/tasks/20261020-qms-e2e24-runtime-update/input-verification.json
- doc/tasks/20261020-qms-e2e24-runtime-update/login-home-ready.png
- doc/tasks/20261020-qms-e2e24-runtime-update/login-home.png
- doc/tasks/20261020-qms-e2e24-runtime-update/login-only.cjs
- doc/tasks/20261020-qms-e2e24-runtime-update/login-proof-initial.json
- doc/tasks/20261020-qms-e2e24-runtime-update/login-proof-loading-locator-failure.json
- doc/tasks/20261020-qms-e2e24-runtime-update/login-proof.json
- doc/tasks/20261020-qms-e2e24-runtime-update/package-process.json
- doc/tasks/20261020-qms-e2e24-runtime-update/package.log
- doc/tasks/20261020-qms-e2e24-runtime-update/replace-runtime.ps1
- doc/tasks/20261020-qms-e2e24-runtime-update/runtime-copy.json
- doc/tasks/20261020-qms-e2e24-runtime-update/runtime-proof.json
- doc/tasks/20261020-qms-e2e24-runtime-update/runtime-start-r7sheet.json
- doc/tasks/20261020-qms-e2e24-runtime-update/runtime-stop-proof.json
- doc/tasks/20261020-qms-e2e24-runtime-update/start-runtime.py
- doc/tasks/20261020-qms-e2e24-runtime-update/task.md
- doc/tasks/20261020-qms-e2e24-runtime-update/verification-report.md
- doc/tasks/20261020-qms-e2e24-runtime-update/verify-and-package.py
- doc/tasks/20261020-qms-e2e24-runtime-update/verify-runtime.ps1
