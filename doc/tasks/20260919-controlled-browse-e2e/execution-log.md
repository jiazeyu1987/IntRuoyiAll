# Execution Log

## 2026-09-19

- 已读取 `docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/task-closeout-rules.md` 及 Computer Use 规则。
- 当前 Git 分支：`int_qms`；HEAD：`a9bcb6d36`。
- 工作区存在大量既有未提交修改；本任务不改动这些文件。
- 当前监听：前端 `8081`，后端 `48081`；health 返回 `UP`。
- 当前阻断风险：分支矩阵要求 `int_qms` 使用 `8061/48061`，现有运行态为 `8081/48081`，仍需确认运行归属后才能进行真实验收。
- 已使用真实 Playwright + Chrome 完成登录：租户“芋道源码”，账号 `admin`；登录 POST HTTP 200，进入 `/dcc/controlled-file/browser`。
- 受控浏览页面真实选择目录“质量管理”；目录列表 GET HTTP 200，实际请求包含 `directoryId=908991` 和 `includeDescendantDirectories=false`。
- 页面看到 1 条当前有效文件：`CODEX-DCC-MAJOR-20260918234457.docx`，版本 `C/1`，目录“质量管理”，发布文件和盖章文件均显示“已生成”。
- 通过页面可见“预览”操作打开详情新页：`/dcc/controlled-file/detail/2054545668044083987?viewer=1&from=browser...`；页面显示“当前有效版 / ACTIVE / C/1”、受控预览、水印、`第 1 / 1 页` 和文件元数据。
- Playwright 证据文件：`doc/tasks/20260919-controlled-browse-e2e/real-evidence.json`；截图：`controlled-browser-blocked.png`、`controlled-browse-final.png`、`controlled-file-detail.png`。
- 真实页面 `console` error/warning 为 0，`pageErrors` 为 0；没有执行业务写入、API 写入或数据库写入。
- Cleanup preview/apply 未执行成功：仓库脚本需要 Python，但本机 `python` 和 `py` 均不可用；未将该阻断伪报为 PASS。
- 手动清理调试截图也被当前命令执行策略阻断；`fresh.png` 作为待清理临时文件保留并单独记录。
- 用户补充验收边界：E2E 全部业务动作必须通过真实页面操作完成。复核本次流程：登录、目录选择、文件行预览、详情页和在线预览均由 Playwright 页面操作完成；没有调用业务 API/数据库承担动作，网络监听仅作被动状态记录。
