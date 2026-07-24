# 任务：SRM NAS定位 黑名单按钮缺失排查（后端 / 运行库）

- Task ID: `20260701-srm-nas-locator-blacklist-button-missing`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

核对 NAS定位 `黑名单` 按钮对应的权限菜单、租户套餐和当前账号角色菜单绑定，确认当前本机运行环境里按钮为什么被隐藏。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-wildcard-search-error\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，不阻塞本轮权限可见性排查。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL / 日志 / 文档统一按 UTF-8 回读；数据库只做只读查询。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本轮只读核对菜单、套餐和账号绑定，不通过跳过权限检查来“显示按钮”。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 页面按钮受 config 权限控制 -> Given NAS定位 页面源码存在黑名单按钮 / When 用户未拥有 `srm:nas-locator:config` / Then 按钮应被前端指令隐藏。`
- `BDD: 环境未落菜单 SQL 时缺口必须可读 -> Given system_menu 中缺少 991104 / When 查询菜单与套餐 / Then 能明确指出菜单未安装或套餐未扩展。`
- `BDD: 当前账号缺权限时能定位到角色菜单绑定 -> Given 菜单 991104 已存在 / When 查询当前用户角色与 role_menu / Then 能判断按钮是否因角色未绑定而隐藏。`

## Milestones

1. M1：建立后端排查台账，确认查询范围。`completed`
2. M2：核对菜单 SQL 与前端权限点。`completed`
3. M3：查询本机运行库菜单、套餐、角色和当前用户绑定。`completed`
4. M4：补证据与收尾。`completed`

## Expected Verification

- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "<readonly sql>"`

## Current Blockers

- 暂无。

## Final Verification Result

- 前端显示条件核对：
  - `index.vue` 中黑名单按钮存在，但需要 `srm:nas-locator:config`。
- 运行库菜单核对：
  - `system_menu.id=991104` 当前为 `文控管理员 / dcc:controlled-file:category:manage`
  - 当前库里不存在 `permission='srm:nas-locator:config'`
- 当前账号绑定核对：
  - `aoteman`（tenant `122`）已绑定 `super_admin` 与 `srm_admin`
  - 这些角色目前只持有 `991100~991103`，没有黑名单配置菜单
- 结论：
  - 按钮缺失不是前端漏做，而是菜单 SQL ID 冲突导致黑名单权限菜单未成功落地。

## Current Status

completed
