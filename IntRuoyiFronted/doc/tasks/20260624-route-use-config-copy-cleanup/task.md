# 工艺排产路线配置文案调整

## 任务目标

将工艺排产路线配置中的用户可见文案按用户要求调整：

- `当前用途启用` 改为 `启用`
- `有限小时产能` 改为 `有限`
- `无限公式产能` 改为 `无限`
- `小时产能` 列改为 `产能(h)`
- `小时产能` 输入改为整数

## 上一任务检查

- 同仓库上一任务：`doc/tasks/20260623-unified-electronic-signature-tab/`
- 上一任务状态：已完成
- 处理结论：允许开始本任务；本轮只修改当前文案相关文件，不触碰既有脏工作区中的其他改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端表格、表单和操作区域保持 IntPP 生产订单列表风格，维持紧凑、可扫描、任务导向的操作台体验。
  - 本轮为现有表格文案和输入精度调整，不新增页面结构、不引入装饰性 UI。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅调整显示文案和输入精度，不新增 fallback。
- `是否从根因和长期维护角度解决`：是。同步更新静态契约测试，防止旧文案回归。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工艺排产路线配置使用简短产能文案 -> Given 用户打开工艺排产路线配置 / When 查看排产用途配置表格 / Then 启用列显示“启用”，产能模式选项显示“有限/无限”，产能列显示“产能(h)”。`
- `BDD: 工艺排产路线配置产能使用整数 -> Given 用户编辑有限产能工序 / When 输入产能 / Then 产能输入限制为整数，不再允许 2 位小数。`

## 里程碑

- [x] M1：读取任务门禁、定位目标文件并创建任务记录。
- [x] M2：补充 RED 静态契约测试。
- [x] M3：调整前端文案和整数输入。
- [x] M4：运行静态测试、记录验证并收尾。

## 预期验证

- `node tests/e2e/mes-route-use-config-display-static.spec.js`
- 变更后精确检索确认旧文案不再出现在目标配置组件中。

## 当前状态

已完成。

## Current Status

completed

## 验证结果

- `node tests/e2e/mes-route-use-config-display-static.spec.js` -> PASS。
- `rg -n '当前用途启用|有限小时产能|无限公式产能|label="小时产能"|:precision="2"' src\views\mes\pro\route-use src\views\mes\pro\route\RouteUseConfigDialog.vue` -> PASS，目标源码未命中旧文案。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-route-use-config-copy-cleanup --mode preview` -> PASS，未发现可删除临时产物。

## Cleanup Keep

- 无
