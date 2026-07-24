# Execution Log: 排产用途产能整数化

BDD: 排产用途有限产能按整数录入 -> Given 用户打开工艺路线排产用途配置 / When 编辑有限产能工序的 产能(h) / Then 输入控件只允许整数，不再保留小数位。
BDD: 排产用途有限产能按正整数保存 -> Given 用户启用有限产能工序 / When 保存用途配置 / Then 系统要求 产能(h) 必须为大于 0 的整数，并在不满足时直接暴露明确错误。
BDD: 标准班次产能继续按整数展示 -> Given 用户录入整数小时产能且存在班次小时 / When 页面实时计算标准班次产能 / Then 标准班次产能仍按整数显示。
RED: `node tests/e2e/mes-route-use-config-display-static.spec.js` -> FAIL，当前 `RouteUsePage.vue` 仍使用 `:precision="6"`，未限制 `产能(h)` 为整数。
CHANGE: `RouteUsePage.vue` 将 `产能(h)` 输入精度调整为 `:precision="0"`，新增 `positiveInteger` 与 `normalizeHourlyCapacity`，并将有限产能保存校验与错误提示统一收敛到正整数口径。
GREEN: `node tests/e2e/mes-route-use-config-display-static.spec.js` -> PASS，排产用途 `产能(h)` 已按整数输入和正整数保存契约收敛。
