# 隐藏工艺路线列表负责人和用途列

## 任务目标

在工艺路线用途列表页隐藏截图蓝框中的 `负责人` 与 `用途` 两列，不引入 fallback、降级或临时绕过。

## 里程碑

- [x] 创建任务目录并记录任务目标
- [x] 定位工艺路线列表页面表格列定义
- [x] 按严格 TDD 补充或调整测试，先观察失败
- [x] 修改前端列定义，隐藏 `负责人` 与 `用途`
- [x] 运行验证并记录结果

## 预期验证

- 相关静态契约覆盖列表列展示行为。
- 页面表格不再显示 `负责人` 与 `用途` 表头及对应数据列。
- 不引入 mock 成功、静默降级、fallback 或临时绕过。

## 当前状态

已完成。`node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js`、`node tests/e2e/mes-route-use-copy-buttons-static.spec.js`、`node tests/e2e/mes-route-use-config-display-static.spec.js` 均通过，前端功能证据校验通过。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，本任务所有中文读写使用 UTF-8 显式处理。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次仅删除冗余列，不改变既有表格视觉体系。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整列定义与测试覆盖。
- 是否存在临时补丁或绕过：否。
