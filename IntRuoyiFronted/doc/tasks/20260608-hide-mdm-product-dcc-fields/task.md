# 任务：隐藏产品主数据 DCC 显示字段

## 任务目标

按用户要求，产品主数据页面不再显示 `DCC产品编号` 表格列，也不再显示 `DCC编号` 查询过滤项。后端接口、编辑表单、导入预览和引用统计不在本任务范围内变更。

## Previous Task Check

- 上一个产品主数据前端任务：`doc/tasks/20260607-mdm-product-update-date-format/task.md`
- 状态：已完成并提交。
- 当前仓库存在其他无关未提交改动和未跟踪任务目录，本任务不修改、不提交这些文件。

## BDD 场景

- BDD: 产品主数据列表不显示 DCC 产品编号列 -> Given 管理员进入产品主数据页面 / When 查看列表表头 / Then 列表中不显示 `DCC产品编号` 列。
- BDD: 产品主数据查询区不显示 DCC 编号过滤项 -> Given 管理员进入产品主数据页面 / When 查看查询过滤区 / Then 查询区不显示 `DCC编号` 过滤项，也不会从查询参数中提交 `dccProductCode`。
- BDD: 其他主数据字段保留 -> Given 管理员进入产品主数据页面 / When 查看查询区和列表 / Then `产品编码`、`中文名称`、`英文名称`、`分类`、`状态`、`更新时间` 等主数据字段仍正常显示。

## Milestones

- [x] M1：建立任务文档、执行日志和 BDD。
- [x] M2：补静态失败测试覆盖 DCC 筛选项和列表列不可见。
- [x] M3：移除产品主数据页面查询区和列表中的 DCC 显示字段。
- [x] M4：运行静态测试、类型检查、证据校验和收尾预览。
- [x] M5：提交本任务相关改动。

## Expected Verification

- `node tests/mdm-product-hide-dcc-fields-static.spec.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260608-hide-mdm-product-dcc-fields/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260608-hide-mdm-product-dcc-fields --mode preview`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅调整前端显示，不引入兜底分支。
- `是否从根因和长期维护角度解决`：是。直接移除页面查询项、查询参数和列表列，避免用户继续看到不需要的 DCC 字段。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed: 已移除产品主数据查询区的 `DCC编号` 过滤项、主列表的 `DCC产品编号` 列和分页查询参数中的 `dccProductCode`。编辑弹窗、导入预览、引用统计和后端接口合同未变更。

## 最终验证结果

- `node tests/mdm-product-hide-dcc-fields-static.spec.mjs` -> PASS。
- `node tests/mdm-product-update-date-format-static.spec.mjs` -> PASS。
- `node tests/mdm-product-code-editable-static.spec.mjs` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260608-hide-mdm-product-dcc-fields/frontend-feature-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260608-hide-mdm-product-dcc-fields --mode preview` -> PASS，delete=<none>，blocked=<none>。

## Cleanup Keep

- doc/tasks/20260608-hide-mdm-product-dcc-fields/frontend-feature-evidence.md
