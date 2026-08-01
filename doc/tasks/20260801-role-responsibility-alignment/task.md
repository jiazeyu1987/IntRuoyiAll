# 20260801-role-responsibility-alignment

## Task Goal

统一 `C:\Users\BJB110\Desktop\文档\职责\` 下各岗位职责文档中的订单池、报工、PQC 任务来源、同步方式、异常边界和检验规则口径。

## Milestones

- [x] 读取职责文档并识别需要统一的冲突口径。
- [x] 按用户确认口径更新相关 Markdown 文档。
- [x] 执行文本一致性校验，确认旧冲突表达已消除。
- [x] 记录验证结果和收尾状态。

## Expected Verification

- 使用 UTF-8 方式读取目标目录职责文档。
- 使用 `rg --encoding utf-8` 检查关键旧口径和新口径。
- 本任务为文档口径调整，不涉及生产代码、构建、数据库或 E2E。

## Current Status

ready_for_closeout

实现和验证已完成。当前仓库进入任务前已存在 unrelated dirty changes 且分支 `int_main` ahead `origin/int_main` 5 commits；本任务未处理、未提交、未推送这些既有改动，因此按项目提交/推送门禁不能标记为 `completed`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接统一职责文档中的正式业务口径。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 中文 Markdown 读写必须使用 UTF-8；写入优先使用 `apply_patch`。
- 本任务只修改用户指定职责文档和当前任务记录，不处理既有无关工作区改动。
- 当前仓库进入任务前已存在未提交改动且 `int_main` ahead `origin/int_main`，本任务不得混入或回滚这些改动。
