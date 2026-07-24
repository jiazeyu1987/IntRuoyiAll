# eDHR 模板内电子签名显示

## 目标

新增电子批记录模板签名位配置入口，并在历史批记录/复盘的模板表格内部显示电子签名。

## 里程碑

1. RED：补充前端静态失败断言，证明只读表格未识别签名位，历史页仍依赖外部签名表。
2. GREEN：实现签名位配置入口、只读模板内签名渲染、历史页传入签名记录。
3. REGRESSION：运行类型检查和页面静态检查。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。前端只消费后端明确 marker，不按“操作人/复核人”文案猜测。
- 是否存在临时补丁或绕过：否。

## 验证

- `node tests/e2e/edhr-batch-history-static.spec.js`
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`
- `pnpm ts:check`

## 当前状态

- completed。

## 完成记录

- 电子批记录模板列表新增“签名位”配置入口，可在弹窗中点击模板单元格标记签名动作。
- 历史批记录/复盘只读模板组件支持 `signatureCellMarkers + signatureRecords`，在模板单元格内显示签名人和签名时间。
- 历史页不再默认展示模板外电子签名明细表作为主视图。
- 验证通过：新增静态测试、历史页静态回归、TypeScript 检查、Playwright 真实页面验证。
