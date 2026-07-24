# 任务：修复 DCC Office 受控预览空白

## 任务目标

修复 DCC 受控浏览中 `.xlsx/.docx` 等 Office 文件在线浏览区域只显示受控水印、正文空白且没有错误提示的问题。预览必须在 OnlyOffice 成功挂载时显示可见编辑器 iframe；若 OnlyOffice 脚本、挂载或文档加载失败，页面必须直接显示错误提示，不能静默留白。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260602-dcc-nas-transfer-confirm-layer/task.md`
- 状态：`blocked`
- 影响：该任务已明确阻塞并隔离 NAS 转移闭环范围；本任务只处理 DCC Office 受控预览空白，不接管或回滚 NAS 转移改动。

## BDD 场景

- BDD: Office 预览成功必须有可见承载 -> Given 用户打开 DCC 受控文件详情 `viewer=1` 且预览类型为 `OFFICE` / When OnlyOffice 脚本加载并创建 DocEditor / Then 页面必须出现可见 iframe 或挂载状态，不能只显示水印背景和空白正文。
- BDD: OnlyOffice 运行时错误必须显式暴露 -> Given OnlyOffice 脚本加载失败、DocEditor 构造失败或文档加载事件返回错误 / When 用户打开 Office 受控预览 / Then 页面必须显示错误提示，不能静默留白。

## Milestones

- [x] M1: 建立任务记录，确认上一前端任务已阻塞隔离。
- [x] M2: 复现 `JL-INT/GL/JY-010-01` xlsx 受控预览空白，并记录 RED 证据。
- [x] M3: 增加失败优先回归断言，覆盖 OnlyOffice 挂载失败必须显示错误。
- [x] M4: 最小化修复 OnlyOffice viewer 的挂载、事件错误和超时显式提示。
- [x] M5: 运行静态回归、真实浏览器预览验证、evidence 校验和 closeout 预览。

## Expected Verification

- RED：`node scripts/dcc-onlyoffice-readonly-config.test.mjs` 新增断言先失败，指出 OnlyOffice viewer 没有捕获 DocEditor 挂载错误和事件错误。
- GREEN：`node scripts/dcc-onlyoffice-readonly-config.test.mjs` 通过。
- E2E：Playwright 打开本机 `http://localhost:8081/dcc/controlled-file/detail/2054545668044047034?viewer=1&from=detail`，Office 预览不再静默空白；若文档服务失败则可见 `.el-alert--error`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。失败时显式报错，不切换下载或伪装为成功。
- `是否从根因和长期维护角度解决`：是，修复受控 Office viewer 对 OnlyOffice 运行时失败的静默留白问题，并用回归断言固化。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## 已完成工作

- 前端 `OnlyOfficeReadOnlyViewer` 已捕获 OnlyOffice 脚本加载失败、`DocEditor` 构造失败和 OnlyOffice `onError` 文档事件，并显示 `.el-alert--error`，不再静默留白。
- 后端本地/开发配置修复见 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260603-dcc-office-preview-runtime-config`：`onlyofficeBaseUrl` 从错误的 `8082` 纠正为本机实际 OnlyOffice `8080`。
- 真实浏览器复跑同一文件 `2054545668044047034`，预览元数据返回 `onlyofficeBaseUrl=http://127.0.0.1:8080`，页面出现 OnlyOffice spreadsheet editor iframe。

## 最终验证结果

- 前端回归：`node scripts/dcc-onlyoffice-readonly-config.test.mjs` -> PASS，4 tests passed。
- 后端配置回归：`python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> PASS，2 tests passed。
- 类型检查：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- 真实 E2E：`node doc\tasks\20260603-dcc-office-preview-blank\scripts\reproduce-dcc-office-preview-blank.mjs` -> PASS，同一 xlsx 出现 OnlyOffice iframe。
- bug evidence：`validate_bug_regression.py --evidence doc\tasks\20260603-dcc-office-preview-blank\bug-regression-evidence.md` -> PASS。
- 收尾清理：`task_closeout.py --task-id 20260603-dcc-office-preview-blank --mode apply` -> PASS，仅删除任务目录中的一次性复现脚本、截图、JSON 和临时 bug evidence，blocked `<none>`、warnings `<none>`。
