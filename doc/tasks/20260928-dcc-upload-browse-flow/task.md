# DCC 上传与浏览流程图

## Goal

静态分析 DCC 受控文件上传、浏览器选版、在线预览的前后端调用链，形成可维护的 Mermaid 流程图。

## Milestones

1. 阅读仓库指令、closeout 及 DCC 上传相关规则。
2. 定位前端页面/API、后端 Controller/Service 的上传和预览路径。
3. 编写流程图、源码锚点与行为边界说明。
4. 校验文档结构、流程图关键分支及路径锚点。

## Expected Verification

- 文档包含 Mermaid `flowchart` 并闭合代码围栏。
- 覆盖上传临时票据、正式提交、浏览选版、metadata 授权、二进制读取与失败路径。
- 生成 SVG 流程图图片，校验其 XML/SVG 结构并保留 Mermaid 源图。
- 表格引用的源码文件均存在，引用的方法/端点可在当前工作区检索。
- 只做只读静态验证，不运行 E2E、不操作服务或数据库。

## Design Constraints

- 区分临时上传与正式提交，区分在线预览与下载。
- 不推断源文件可回退为浏览文件；失败路径按源码事实呈现。
- 保留工作区其他未提交改动；本任务只新增本目录任务记录与流程图文档。

## Current Status

blocked

### Blockers

- Repository closeout requires commit and push, while current-turn authorization does not permit Git writes. Current checked-out branch is `int_qms`, not the documented `int_main`; no branch switch or commit/push was attempted.

## Cleanup Keep

- doc/tasks/20260928-dcc-upload-browse-flow/task.md
- doc/tasks/20260928-dcc-upload-browse-flow/execution-log.md
- doc/tasks/20260928-dcc-upload-browse-flow/verification-report.md
- doc/tasks/20260928-dcc-upload-browse-flow/artifacts/dcc-upload-browse-flow.svg
