# 任务：DCC 浏览/查阅权限按审阅矩阵限制的后端改造

## 任务目标

- 将 DCC 浏览列表、浏览详情和已发布文件预览的授权真源切换为当前类别生效审阅矩阵解析参与人。
- 保留申请人自查、目录管理权限和进行中流程 `route snapshot` 预览。
- 移除查询链路对旧 `VIEW + 产品可见性 + 目录 QUERY/PREVIEW` 真源的依赖。

## 当前状态

`completed`

## Current Status

completed

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-tab\task.md`
- 状态：`completed`
- 处理：上一任务已完成“审阅矩阵页签 + snapshot 权限”改造，本任务继续在其基础上调整浏览/查阅真源。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 本任务适用强制门禁：
  - 不为缺失的岗位分配、缺失的上传者上下文或旧权限规则做 fallback。
  - 浏览/查阅真源切换后，待审原件预览仍需保证旧 snapshot 不回溯失效。
  - 真实 E2E 前先完成本机定向单测并记录 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是。通过明确的审阅矩阵参与人解析服务取代旧 VIEW/QUERY 混合链路。
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 旧 VIEW/QUERY 不再放行浏览 -> Given 用户仍命中旧 VIEW/目录 QUERY 权限但不在当前类别审阅矩阵解析参与人内 When 查询 DCC 浏览列表或打开文件详情 Then 该文件不可见且不可查阅。`
- `BDD: 审阅矩阵参与人可直接查阅 -> Given 用户命中当前类别审阅矩阵解析参与人 When 打开浏览列表、详情和已发布文件预览 Then 即使旧 VIEW/目录 QUERY/PREVIEW 权限未命中也允许查阅。`
- `BDD: 老流程待审预览不回溯 -> Given 文件已在旧 route snapshot 中流转且当前用户命中旧 snapshot 参与人但不在当前矩阵参与人内 When 预览该待审文件原件 Then 仍按 snapshot 放行。`

## 里程碑

1. 建立后端任务文档与执行日志。`DONE`
2. RED：补查询服务/矩阵参与人解析失败测试。`DONE`
3. GREEN：实现查询权限切换与必要 mapper/service 调整。`DONE`
4. GREEN：执行定向 Maven 回归并回填证据。`DONE`

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileReviewMatrixAccessServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 预期交付物

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-browse-permission-change\execution-log.md`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-browse-permission-change\backend-api-evidence.md`

## 最终验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFilePreviewProtectionTest,DccOnlyOfficeControlledPreviewTest -Dsurefire.failIfNoSpecifiedTests=false test` -> `PASS`，`65` tests passed。
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileReviewMatrixAccessServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> `PASS`，`51` tests passed。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-browse-permission-change\backend-api-evidence.md` -> `PASS`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260622-dcc-review-matrix-browse-permission-change --mode preview` -> `PASS`，预览仅识别 `backend-api-evidence.md` 为可清理任务附属产物。

## 完成结论

- 浏览列表、浏览详情和已发布文件预览的放行真源已切换为“申请人 / 目录管理 / 当前类别审阅矩阵参与人”，不再依赖旧 `VIEW + 目录 QUERY/PREVIEW` 混合规则。
- 待审原件预览仍保留 `route snapshot` 参与人放行，不回溯破坏历史流程。
- 为适配新真源，相关查询、预览与 OnlyOffice 保护测试已同步更新，并新增 `DccControlledFileReviewMatrixAccessServiceTest` 覆盖矩阵参与人解析。
