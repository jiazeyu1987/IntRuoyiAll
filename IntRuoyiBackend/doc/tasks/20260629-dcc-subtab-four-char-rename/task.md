# 任务：DCC 文控中心子页签改为四字名称

- Task ID: `20260629-dcc-subtab-four-char-rename`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

统一 DCC 菜单种子中的文控中心子页签名称为无 `DCC` 前缀的 4 字名称，保持 permission、path、component 与菜单 ID 不变。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-smart-scheduling-role-scope-check\task.md`
- 状态：`COMPLETED`
- 处理说明：上一后端任务已完成，本次只处理 DCC 菜单文案契约。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文 SQL / 文档检索与日志维护必须显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接统一基础菜单种子、补丁 SQL 与契约测试，避免新环境初始化仍出现旧菜单名。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 新环境初始化后 DCC 文控中心子页签名称符合四字契约 -> Given 系统执行 DCC 菜单基础种子与补丁 SQL / When 菜单写入 system_menu / Then 目标页签名称应为无 DCC 前缀且互不重名的 4 字名称。`

## Milestones

1. M1：建立后端任务文档与执行日志。`completed`
2. M2：先补契约断言并执行 RED。`completed`
3. M3：修改 SQL、运行库迁移与测试并执行 GREEN。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_subtab_four_char_menu_sql.py -q`

## Current Blockers

- 无。

## Final Verification Result

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_subtab_four_char_menu_sql.py -q` -> PASS
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -N -e "SELECT path, name FROM system_menu WHERE parent_id = 6800 AND deleted = 0 ORDER BY sort;" ruoyi-vue-pro` -> PASS

## Completion Result

- DCC 基础菜单种子与补丁 SQL 已统一四字新名称。
- 本机运行库已执行正式重命名 SQL，对应目标菜单名称已回查通过。
