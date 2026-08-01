# 修复测试服排程用料清单子项映射

## Task Goal

修复测试服务器 `172.30.30.58` 中用户 `zhaojie` 在租户 `芋道源码` 打开排程日历时出现的错误：`排程工单生产用料清单子项未映射本地物料: 881MO093613 / A003.017.01.004.2008`。

## Milestones

- [x] 建立任务范围、门禁和审计记录。
- [x] 只读复现并定位目标工单、用料清单行和物料主数据缺口。
- [x] 备份测试服相关表。
- [x] 执行最小数据修复。
- [x] 使用用户授权账号完成真实页面/API 抽样验证。
- [ ] 完成验证报告和收尾状态。

## Expected Verification

- 测试服后端健康检查正常。
- 目标租户、用户、目标工单 `881MO093613`、子项物料 `A003.017.01.004.2008` 均通过真实库/API 确认。
- 写入前完成测试服相关表备份并记录 hash/gzip 校验。
- 修复后生产用料清单对应行有同租户有效 `child_material_id`，且不产生跨租户或孤儿引用。
- 使用 `zhaojie` / `芋道源码` 真实前端路径复验排程日历不再出现该映射错误；如登录验证码或权限阻塞，记录真实 blocker，不用 API-only 冒充页面通过。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；根因是测试服目标工单三条生产用料清单子项 `child_material_id` 为空，但同租户正式物料唯一可解析，将仅补齐同租户正式引用，不做跨租户引用或默认成功。
- `是否存在临时补丁或绕过`：否。

## Scope

- 目标环境：测试服务器 `172.30.30.58`。
- 目标租户：`芋道源码`。
- 目标用户标签：`zhaojie`。
- 目标业务对象：排程日历加载、排程工单 `881MO093613`、生产用料清单子项 `A003.017.01.004.2008`。
- 禁止范围：不操作正式服/备用服，不发布代码，不关闭校验，不跨租户引用物料主数据，不删除业务数据。

## Applied Experience Gates

- 生产用料清单跨环境白名单 upsert 门禁：已按测试服 tenant `1`、显式目标表、同租户物料唯一解析、备份先行和页面/API 抽样验证执行。
- 服务器访问门禁：仅访问用户授权测试服 `172.30.30.58`，未操作正式服或备用服。
- E2E 登录门禁：真实登录页被滑块验证码阻塞；已记录 blocker，后续页面渲染验证明确标记为 token-bootstrap，不冒充真实登录页通过。

## Closeout Blockers

- Git closeout blocked: current branch `int_main` is already ahead of `origin/int_main` by `10` commits and contains many unrelated dirty changes from other task scopes. This task did not stage, commit, push, revert, or baseline unrelated files.
- Real login-page E2E blocked: deployed test frontend requires slider captcha; token-bootstrap page rendering passed and is recorded separately.
