# 任务：恢复 DCC 查阅矩阵为 Excel 文件查阅矩阵模式

## 任务目标

将当前本机 DCC 查阅矩阵恢复为桌面 `电子文控系统推进计划及需求表.xlsx` 中 `文件查阅矩阵` sheet 的模式：

- tenant 1 的矩阵范围分类 `VIEW` 权限只保留 Excel 矩阵定义的部门/主管主体。
- tenant 1 的矩阵范围目录 `QUERY/PREVIEW` 权限恢复到矩阵主体生效状态。
- tenant 1 不再让 `wenkong / wenkong_download` 通过额外矩阵范围 `VIEW` 或目录范围覆盖 Excel 原始矩阵。
- tenant 122 继续保持 fail-fast 的未启用状态，不伪造矩阵启用。

本任务只修复本机数据库口径与直接相关脚本/测试/证据；不操作测试服、备份服、正式服。

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-showroom-publish-audio-integrity-gate\task.md`
- 状态：`completed`
- 处理：上一任务已完整收尾，不阻塞本次 DCC 矩阵修复。

## 用户要求与执行边界

- 用户要求：`帮我改成符合文件里的查阅矩阵的模式,可以实现吗`
- 本任务边界：
  - 允许：新增或修改本机 SQL 修复脚本、验证脚本、任务证据；对本机 `int-ruoyi-mysql / ruoyi-vue-pro` 执行受控写入修复。
  - 禁止：服务器写入、发布、真实 E2E 长链路、顺手修改无关 DCC/EDHR 功能。
  - 禁止：用 fallback、保留双轨口径或“矩阵+文控扩权同时算通过”的方式掩盖冲突。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\agent-memory\project-error-prevention.md`
- 适用强制门禁摘录：
  - DCC 文件查阅矩阵一致性复验必须同时比对 Excel、确认版 seed 和当前库三层。
  - 当前库可按“矩阵 code 或文件名唯一解析”复用已有同名分类，不能只按 `DCC_FVM_%` 分类数量判定缺失。
  - DCC 矩阵权限落库后要逐条核期望 `VIEW` 规则、目录 `QUERY/PREVIEW`、`can_download=0` 和遗留 `USER`/`DOWNLOAD` 冲突；只看矩阵分类存在或规则总数容易漏掉旧测试权限覆盖。
  - 本轮涉及本机数据库写入修复，执行写入前必须先在 `execution-log.md` 记录 RED/前置和目标回滚边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接收回 2026-06-17 本机新增的 `wenkong / wenkong_download` 矩阵范围覆盖，并恢复原矩阵主体目录规则生效。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: tenant 1 只保留 Excel 矩阵主体查看权限 -> Given Excel 文件查阅矩阵定义了 59 类文件和 231 条授权 / When 恢复本机 tenant 1 的查阅矩阵模式 / Then tenant 1 的矩阵范围分类 VIEW 只允许原矩阵主体 231 条，不能保留 wenkong/wenkong_download 额外 118 条矩阵 VIEW。`
- `BDD: tenant 1 矩阵目录权限重新生效 -> Given 原矩阵主体目录 QUERY/PREVIEW 规则曾被逻辑删除 / When 恢复查阅矩阵模式 / Then 68 条矩阵目录规则必须恢复为有效，wenkong/wenkong_download 的矩阵覆盖目录规则不得继续作为矩阵真实生效口径。`
- `BDD: tenant 122 继续 fail-fast 未启用 -> Given tenant 122 缺少矩阵部门前置 / When 本机恢复 Excel 查阅矩阵模式 / Then tenant 122 的矩阵范围 VIEW 仍保持 0，不得顺手启用或用测试分类替代。`

## 里程碑

- [x] M1 建立任务文档并确认上一后端任务已完成。
- [ ] M2 RED：重新复现当前 tenant 1 / tenant 122 偏离状态并形成失败证据。
- [ ] M3 GREEN：新增最小修复 SQL/脚本，恢复 tenant 1 矩阵模式。
- [ ] M4 GREEN：运行只读核验，确认 Excel/seed/当前库重新一致。
- [ ] M5 回填 bug/schema evidence、验证脚本、收尾预览和提交准备。

## 预期验证

- `docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ...` 只读 RED/GREEN 查询。
- `python -X utf8 -m pytest` 或等价定向测试，覆盖修复脚本/校验逻辑。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ...`
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ...`

## ????

COMPLETED??? tenant 1 ???? Excel ?????????tenant 122 ??????

## ??????

- `python -X utf8 -m pytest script\tests\test_dcc_file_view_matrix_seed.py script\tests\test_dcc_view_matrix_restore_sql.py -q` -> PASS?11 passed?
- `python -X utf8 script\dcc_view_matrix_restore_sql.py --apply-local-mysql` -> PASS?`expected_matrix_view_rules=231`?`active_matrix_view_rules=231`?`unexpected_matrix_active_rules=0`?`restored_matrix_directory_rules=45713`?`active_wenkong_directory_rules=0`?`tenant122_matrix_view_rules=0`?
- ??????????????????
