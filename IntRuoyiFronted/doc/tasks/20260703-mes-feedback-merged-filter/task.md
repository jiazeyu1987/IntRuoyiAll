# 20260703 MES 报工列表合并筛选

## 任务目标

将 MES 正式报工列表的多个平铺筛选项合并为一个“筛选类型 + 动态输入控件”的组合：用户先选择筛选字段，再根据字段类型输入文本、选择字典、选择业务对象或选择日期范围。保留现有查询参数、路由参数和搜索/重置行为，不新增 fallback、mock 数据或后端接口变更。

## 里程碑

1. 定位报工页面和当前筛选结构。completed
2. 补充任务文档、BDD 场景和 RED 静态测试。completed
3. 改造正式报工筛选为合并动态控件。completed
4. 运行目标静态测试、类型检查和证据校验。completed
5. 更新任务记录并提交本任务改动。completed

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`；本轮命令显式 UTF-8，不使用 `&&`，中文文件读写使用 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次沿用紧凑运维控制台筛选栏，不做额外视觉重设计。
- 前端交付：已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`；必须记录 BDD、RED/GREEN 和前端证据。
- 本次不涉及真实 E2E、登录后写入、服务器写入、租户数据修改、发布或回滚，不触发高风险运行态门禁。

## BDD 场景

- BDD: 合并正式报工筛选 -> Given 用户打开 MES 正式报工列表 / When 查看筛选栏 / Then 原先报工编号、报工单号、报工类型、生产工单、产品物料、报工人、记录人、状态、报工时间等平铺筛选合并为一个筛选类型下拉和一个动态筛选值控件。
- BDD: 筛选类型决定输入控件 -> Given 用户选择不同筛选类型 / When 筛选值区域渲染 / Then 编号和单号显示文本输入，类型和状态显示字典下拉，生产工单、产品物料、报工人、记录人显示原业务选择器，报工时间显示日期范围。
- BDD: 查询参数保持兼容 -> Given 用户选择某个筛选类型并输入筛选值 / When 点击搜索 / Then 仅对应的旧查询参数被提交，其他旧筛选参数被清空，路由传入 feedbackId/status 仍可设置对应旧查询参数。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；仅改变前端筛选交互，不新增兜底查询、默认成功或吞异常。
- 是否从根因和长期维护角度解决：是；通过字段配置集中管理筛选字段与控件类型，避免多个筛选项平铺扩散。
- 是否存在临时补丁或绕过：否。

## 预期验证

- `node tests/e2e/mes-feedback-merged-filter-static.spec.js`
- `node tests/e2e/mes-feedback-tracking-static.spec.js`
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260703-mes-feedback-merged-filter\frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 当前里程碑：完成
- 已完成：正式报工筛选已合并为 `筛选类型 + 筛选值`；筛选类型集中在 `feedbackFilterFields` 配置；筛选值按文本、字典、生产工单、产品物料、用户选择器、日期范围动态切换；搜索前清理未选中旧参数；路由传入 `feedbackId/status` 仍可激活对应筛选字段。
- 阻塞：暂无。

## Current Status

completed

## 最终验证

- `node tests/e2e/mes-feedback-merged-filter-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-tracking-static.spec.js` -> PASS
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260703-mes-feedback-merged-filter\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260703-mes-feedback-merged-filter --mode preview` -> PASS，预览仅删除临时 `frontend-feature-evidence.md`。
