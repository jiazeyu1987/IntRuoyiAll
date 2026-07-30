# 20260730 电子签名普通用户页签权限

## Task Goal

电子签名页面中，普通用户只能看到“我的签名”页签，不能看到无权限的管理/列表页签或触发无权限列表查询。

## Milestones

- [ ] 建立 BDD 场景并定位电子签名页签、路由和权限控制逻辑
- [ ] 写出普通用户页签可见性的 RED 静态回归测试
- [ ] 实现最小正式修复，不引入 fallback、降级或吞异常
- [ ] 运行 GREEN 与相关回归验证并记录证据
- [ ] 完成任务文档、验证报告和收尾状态

## Expected Verification

- `node tests/e2e/electronic-signature-my-tab-only-static.spec.js`
- 必要时补充相关电子签名既有静态合同或 `pnpm ts:check` 阻塞记录

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式权限/页签配置收敛普通用户可见入口。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/experience-index.md` 已存在；本任务命中“只显示/仅展示”类口径，采用正向允许集合建模，禁止只隐藏截图中一个异常页签。
