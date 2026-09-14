# 表单解析生产批记录 JSON 明细展示

## Task Goal

在表单解析页的生产批记录解析结果中，基于已下载的批记录总识别 JSON 展示完整业务明细：输入物料、输出物料、工序设备、设备参数参考范围、默认值、最小/最大值、步长、单位、控件、选项、实际值占位和选择模式，并保留完整 JSON 可核对入口。

## Milestones

- [x] M1: 分析 `批记录总对应.json` 与当前页面展示缺口
- [x] M2: 补充前端静态合同 RED
- [x] M3: 实现表单解析页明细展示
- [x] M4: 运行静态合同、类型检查和 diff 检查
- [x] M5: 记录验证证据并进入收尾状态

## Expected Verification

- `node tests\e2e\form-parser-json-download-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check`
- 前端 evidence validator 已在 cleanup 前执行并归档到 `verification-report.md`；临时 evidence 文件已按 cleanup 规则删除。

## Current Status

completed

已实现表单解析页的生产批记录 JSON 展开明细：输入物料、输出物料、输出物料-设备-参数对应、设备组、参数范围、默认值、实际值、完整 JSON 与工序 JSON 均可核对；cleanup preview/apply 已通过。实现提交：`dc9ee471c9481e3c78cbe4a353b6d95b4b82ae22`。

## 设计约束检查

- 只扩展前端展示，不改后端 JSON 合同。
- 不把工序级设备伪造成输出物料的一对一设备绑定；页面明确标注 JSON 未提供单个输出物料与设备的一对一字段，并按“同工序设备组”展示对应关系。
- 保持 `.json` 下载逻辑、parse-only API 和权限不变。
- 不执行真实 E2E，除非用户当轮明确要求。
