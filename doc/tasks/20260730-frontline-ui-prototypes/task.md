# 一线报工与 PQC 极简原型任务

## Task Goal

在 `output/` 下继续完善一线生产报工与 PQC 检验 1920×1080 静态原型，保持页面足够简单：顶部只放必要上下文卡片，工序选择从顶部“工序”卡片进入，不新增管理统计、说明文案或复杂控件。

## Milestones

- [x] 创建任务文档并记录设计约束。
- [x] 统一生产与 PQC 页面的工序选择交互。
- [x] 使用 1920×1080 浏览器截图和 Playwright 交互检查验证。
- [x] 记录验证结果和剩余限制。
- [x] 生产页去掉“收到数量”，增加按不良类型分别填写数量。
- [x] 验证有设备和无设备模板的不良数量录入及损耗汇总。
- [x] 删除不良数量弹框，改为 7 种不良在主页面直接填写。

## Expected Verification

- 生产页点击左上角“工序”卡片后，只显示可选工序大按钮；选择后顶部工序值更新。
- PQC 页顶部按用户截图调整为 `生产订单 / 工序 / 员工 / 主页`，点击“工序”卡片后选择工序并更新顶部工序值。
- PQC 页左侧原生产订单卡片改为“检验内容”，长度、压力等数值可输入，外观、密封等判断项可选择合格/不合格。
- PQC 页右侧不显示巡检卡片下方小字说明，也不显示“检验方法”输入行。
- PQC 页右侧不显示底部“结果 / 合格 / 不合格”整行。
- 两个页面在 1920×1080 下无横向溢出，底部提交栏不遮挡主表单。
- 生产有设备页和无设备页均不显示“收到数量”。
- 生产员工只填写完成数量；损耗数量由各不良类型数量自动汇总显示。
- 每个工序可显示 7 个不良类型，每种不良直接提供减号、数量输入框和加号。
- 不良数量输入或加减后，损耗数量立即更新。
- 不良数量不得使用弹框；7 种不良的名称、数量输入和加减按钮必须同时显示在主页面。

## Current Status

ready_for_closeout

## Verification Summary

- 生产有设备页：点击左上角“工序”卡片后弹出 4 个大按钮，选择“装配”后顶部工序更新，无横向溢出。
- 生产无设备页：顶部结构补齐为“工序 / 员工 / 主页”，点击“工序”卡片后可用同样弹层选择工序。
- PQC 页：顶部调整为“生产订单 / 工序 / 员工 / 主页”，左侧改为可填写“检验内容”，长度和压力可输入，外观和密封可选择合格/不合格；点击“工序”卡片后使用同样弹层选择工序，选择“装配”后顶部工序更新。
- PQC 页：已隐藏右侧黄色框区域，即巡检卡片下方 `30件 / 外观+压力 / 损耗1` 类说明和“检验方法”整行。
- PQC 页：已隐藏右侧底部“结果 / 合格 / 不合格”整行。
- 1920×1080 检查：三个页面 `bodyScrollWidth=1920`，主表单底部均未压到提交栏。
- 生产有设备和无设备页均已删除“收到数量”，只保留完成数量和只读损耗数量。
- 两个生产模板均把 7 种不良直接铺在主页面，每种不良同屏显示名称、减号、数量输入、加号和单位。
- 不良数量不再使用弹框，也没有返回或二次完成按钮。
- 直接输入不良数量后自动汇总损耗数量；任务专用 DOM 测试已验证修改、独立保存、完成数量加减和重填恢复。
- 本轮浏览器控制因 `file://` URL 安全策略被拒绝，未生成新的浏览器截图；固定 1920×1080 画布、两列四行不良按钮布局和无遮挡空间由静态合同与尺寸计算验证。

## Output Files

- `output/frontline-production-operator-1920.html`
- `output/frontline-production-operator-1920-no-device.html`
- `output/frontline-pqc-operator-1920.html`
- `output/playwright/frontline-production-operator-1920-process-picker-open.png`
- `output/playwright/frontline-production-operator-1920-no-device-process-picker-open.png`
- `output/playwright/frontline-pqc-operator-1920-process-picker-open.png`
- `output/playwright/frontline-pqc-operator-1920-order-process-employee-v2.png`
- `output/playwright/frontline-pqc-operator-1920-editable-content.png`
- `output/playwright/frontline-pqc-operator-1920-yellow-hidden.png`
- `output/playwright/frontline-pqc-operator-1920-result-hidden.png`
- `doc/tasks/20260730-frontline-ui-prototypes/frontline-defect-quantity.static.cjs`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否，本任务只调整静态原型交互。
- `是否从根因和长期维护角度解决`：是，统一生产与 PQC 的工序入口位置和选择方式。
- `是否存在临时补丁或绕过`：否。
