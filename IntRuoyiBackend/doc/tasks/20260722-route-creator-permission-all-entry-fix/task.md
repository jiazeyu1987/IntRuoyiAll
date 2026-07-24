# 工艺路线创建入口权限一致性修复

## Task Goal

修复 Sheet1 Excel 导入、IntGY Markdown 导入、eDHR Word 批记录自动生成路线三个直接插入入口未绑定创建者对象级权限的问题，并将普通创建、复制和导入生成入口统一到同一个路线所有者权限服务。

## Milestones

- [x] M1：记录 BDD 场景和严格 TDD 证据。
- [x] M2：补充失败测试覆盖三个入口。
- [x] M3：提取共享路线创建者权限服务并接入所有入口。
- [x] M4：运行目标回归测试。
- [ ] M5：提交实现并完成根目录任务收尾。

## Expected Verification

- 路线普通创建、复制、Sheet1 导入、IntGY 导入和 Word 批记录路线生成均绑定当前登录创建者。
- 权限命令包含 `ROUTE` 对象、目标 routeId、当前 actor、`VIEW/ROUTE_EDIT/PERMISSION_ADMIN`、`USER/ALLOW/ENABLED`。
- 不引入默认用户、fallback、吞异常或历史数据自动补权。

## 经验门禁

- 新增或修改 `routeMapper.insert` 后必须统一调用路线创建者权限绑定能力。
- 无效 creator 不得自动映射到 `admin`、`aoteman` 或超级管理员。
- 测试必须断言权限保存命令，不得用静态角色或菜单权限替代对象级权限。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout
