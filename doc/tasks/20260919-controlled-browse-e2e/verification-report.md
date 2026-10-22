# Verification Report

## Current Status

ready_for_closeout

## Scope

真实前端受控浏览主流程：登录测试租户、进入受控浏览、打开受控文件详情、在线预览。

## Evidence

- 登录：真实页面登录成功，租户“芋道源码”，账号标签 `admin`，登录响应 HTTP 200。
- 入口：从登录重定向进入 `/dcc/controlled-file/browser`，页面显示“文控中心 / 受控浏览”。
- 目录：通过页面点击“质量管理”，自然列表请求 HTTP 200，参数包含 `directoryId=908991`、`includeDescendantDirectories=false`。
- 文件：页面可见 1 条当前有效文件 `CODEX-DCC-MAJOR-20260918234457.docx`，版本 `C/1`，目录“质量管理”，发布文件和盖章文件均显示“已生成”。
- 详情与预览：通过文件行可见“预览”操作打开详情页；页面显示“当前有效版 / ACTIVE / C/1”、受控预览、`禁止截图/外传`、水印 `WM-20260919-5351E7773766 | admin | <时间>`、`第 1 / 1 页`，并显示文件类别、目录、编号、流程实例、提交人、发布时间等元数据。
- 页面质量：本次 Playwright 运行 `consoleErrors=0`、`pageErrors=0`。业务动作全部由真实前端完成，未使用 API/DB 代替点击或导航。
- 业务动作边界复核：登录、目录选择、文件预览、详情导航和在线预览全部由真实页面完成；没有使用 `fetch`、`apiGet`、直接 API 或数据库执行任何业务动作。网络请求仅被动监听状态，不作为页面业务结果的替代。
- 证据文件：[real-evidence.json](/C:/IntRuoyiAll-int_main/doc/tasks/20260919-controlled-browse-e2e/real-evidence.json)、[controlled-browse-final.png](/C:/IntRuoyiAll-int_main/doc/tasks/20260919-controlled-browse-e2e/controlled-browse-final.png)、[controlled-file-detail.png](/C:/IntRuoyiAll-int_main/doc/tasks/20260919-controlled-browse-e2e/controlled-file-detail.png)。

## Blockers

- 运行态风险：当前 Git 分支为 `int_qms`，但仓库矩阵规定 `int_qms` 基准端口为 `8061/48061`；本次实际页面使用 `8081/48081`。前后端进程命令行均指向当前仓库，且后端 Jar 为 `output/runtime/int_main/backend-runtime-control-20260919-dcc-upload.jar`，因此本报告只证明当前本机运行态的真实页面流程，不把结果表述为 `int_qms` 专属端口运行态证明。
- 未生成 Playwright `trace.zip`；本轮用户未要求逐步截图验收，保留入口/列表/详情截图作为证据。

## Conclusion

真实受控浏览流程在当前运行页面上通过：登录 -> 受控浏览 -> 目录选择 -> 当前有效文件 -> 详情 -> 在线受控预览。分支端口归属风险单独保留，不影响已完成页面动作的事实记录。
