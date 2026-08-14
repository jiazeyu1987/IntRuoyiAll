# Frontend Feature Evidence

## Feature Goal

把生产一线报工原型进一步简化：去掉收到数量，保留完成数量，并让员工按当前工序配置的不良类型分别填写数量；损耗数量自动汇总。

## Non-goals

- 不修改 PQC 原型。
- 不接入真实接口、权限、工单或持久化。
- 不增加统计、说明、趋势或审核功能。

## Acceptance

- `FL-DEFECT-01`: 有设备和无设备模板都不显示收到数量。
- `FL-DEFECT-02`: 两个模板都显示完成数量和只读损耗总数。
- `FL-DEFECT-03`: 页面支持 7 种不良，每种不良可独立填写数量。
- `FL-DEFECT-04`: 直接输入或加减不良数量后自动汇总损耗总数。
- `FL-DEFECT-05`: 1920×1080 下无横向溢出、无遮挡。
- `FL-DEFECT-06`: 不良数量不使用弹框，7 种不良在主页面直接填写。

## Owned Files

- `output/frontline-production-operator-1920.html`
- `output/frontline-production-operator-1920-no-device.html`
- `doc/tasks/20260730-frontline-ui-prototypes/frontline-defect-quantity.static.cjs`

## BDD

- BDD: Given 生产员工打开报工页，When 页面加载，Then 不显示收到数量。
- BDD: Given 当前工序有 7 种不良，When 直接输入或加减任一不良数量，Then 该类数量和损耗总数同步更新。
- BDD: Given 多种不良已有数量，When 修改其中一类，Then 其他类别保持不变。
- BDD: Given 页面显示 7 种不良，When 员工填写数量，Then 每种不良都能在主页面直接输入或加减，不出现不良编辑弹框。

## RED / GREEN

- RED: `node doc/tasks/20260730-frontline-ui-prototypes/frontline-defect-quantity.static.cjs` -> FAIL，旧页面仍包含收到数量。
- GREEN: 同一命令 -> PASS；两个模板均通过静态结构和 DOM 交互断言。
- RED: 同一命令 -> FAIL，旧页面仍包含 `defectEditor` 不良数量弹框。
- GREEN: 同一命令 -> PASS；两个模板均删除不良弹框，7 种不良在主页面直接填写。

## Responsive And Accessibility

- 目标画布固定为 1920×1080。
- 不良按钮使用真实 `button`，数量编辑器提供明确名称、返回和完成按钮。
- 可点击区域保持大尺寸，不使用密集表格。
- 不良区域固定为两列四行，容纳 7 个不良类型。

## Data States

- 初始态：有设备模板损耗 5 件，无设备模板损耗 2 件。
- 零值态：数量为 0 的不良按钮保持普通样式。
- 已填态：数量大于 0 的不良按钮使用绿色强调并显示 `x件`。
- 修改态：只修改当前输入的不良类型，其他不良数量不变。
- 重填态：恢复模板初始完成数量和各不良数量。

## API Contract

- 静态原型无 API；所有数据只存在于当前页面内存。

## Loading Empty Error Permission Checks

- Loading：不适用，静态原型无请求。
- Empty：7 种不良均为 0 时损耗数量显示 0。
- Error：数量输入按非负整数归一化，不吞接口错误，因为不存在接口。
- Permission：不适用；正式系统仍需沿用账号绑定工序和员工权限。

## Verification Path

- 运行任务专用 Node 静态合同。
- 合同执行页面内联 JavaScript，模拟直接输入不良数量、完成数量加减和重填。
- 浏览器控制因 `file://` URL 安全策略被拒绝，本轮未生成新截图。

## Blockers

- 无实现阻塞。
- 视觉截图证据受本地 `file://` 浏览器控制安全策略限制。
