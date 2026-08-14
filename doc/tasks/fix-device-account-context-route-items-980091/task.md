# 修复一线 PQC 切换按压式扩张压力泵上下文报错

## Task Goal

修复一线 PQC 切换到“按压式扩张压力泵”时出现的设备账号上下文异常：`设备账号上下文不完整或不一致：routeProjectItems routeId=980091，missingItemIds=[14]`。

## Milestones

- [ ] 建立缺陷复现与 BDD/TDD 证据
- [ ] 定位 routeProjectItems 与设备账号上下文缺失的根因
- [ ] 增加失败回归测试
- [ ] 实施最小正式修复
- [ ] 运行定向回归验证并记录结果
- [ ] 更新收尾证据与经验沉淀检查

## Expected Verification

- 后端定向测试覆盖 routeId=980091 / missingItemIds=[14] 同类上下文完整性场景。
- 相关 PQC / 一线设备账号上下文构建逻辑不再误报缺失。
- 不引入 fallback、默认成功、吞异常或 mock 数据。

## Current Status

in_progress

已创建任务证据，待完成复现、RED/GREEN 与回归验证。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：待确认根因后更新
- 是否存在临时补丁或绕过：否

## Applicable Experience Gates

- 待读取并摘录 `docs/backend-development.md#mes-一线设备账号权限门禁`。
