# 测试服数据库 SQL 快应用入口

## 任务目标

实现一个运行控制台入口，用于将本机明确指定的 SQL 文件快速应用到测试服务器 `172.30.30.58` 的 `intruoyi-mysql` / `ruoyi-vue-pro` 数据库。该入口用于开发联调阶段的必要 schema、菜单、字典、权限或测试租户数据修正，不做整库 dump/import，不同步 MinIO，不替代正式发布包。

## 经验门禁

- 目标环境固定为测试服；不得访问、探测或写入正式服。
- 执行前必须确认目标主机、远端运行目录、目标容器和目标库。
- SQL 文件必须显式传入且存在；不得生成默认成功或空操作成功。
- 执行前必须检查测试库基础表 `system_tenant` 存在；缺失时 fail fast。
- 不执行整库重置、dump/import、MinIO 同步、NAS 清理或服务发布。
- 高风险真实执行前必须在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。
- 前端入口遵循 IntPP 运维控制台风格，保持紧凑、可扫描，不做营销式页面。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少 SQL 文件、目标主机、SSH、基础表或 MySQL 执行失败时直接失败。
- `是否从根因和长期维护角度解决`：是；将“整库同步”与“必要 SQL 快应用”拆成独立受控动作，减少测试服发布等待时间。
- `是否存在临时补丁或绕过`：否；该入口是测试服开发辅助通道，最终验收仍需正式发布包。

## 里程碑

1. M1 文档与门禁：创建任务文档、执行日志和数据库证据。
2. M2 RED：新增运行控制台动作、脚本和前端入口的失败契约测试。
3. M3 GREEN：实现测试服 SQL 快应用 PowerShell 脚本、后端动作参数、前端入口。
4. M4 验证：运行脚本静态测试、后端 RuntimeControl 测试、前端静态/类型检查。
5. M5 收尾：更新证据、执行 cleanup 预览并提交本任务改动。

## 预期验证

- RED：后端动作测试最初失败，因为缺少 `apply-test-db-sql` 动作和 `-SqlPath` 参数。
- RED：脚本静态测试最初失败，因为缺少快应用脚本或安全门禁。
- RED：前端静态测试最初失败，因为运行控制台页面没有数据库快应用入口。
- GREEN：目标测试通过，证明入口只指向测试服、传入明确 SQL、不走 dump/import/MinIO/正式服。

## 当前状态

已完成：测试服数据库 SQL 快应用入口已实现，任务证据、验证和 cleanup 预览已完成。

## 里程碑状态

- M1 文档与门禁：完成，已创建任务文档、执行日志和数据库证据。
- M2 RED：完成，后端动作、脚本和前端入口测试均先失败。
- M3 GREEN：完成，已实现测试服 SQL 快应用脚本、后端动作参数和前端入口。
- M4 验证：完成，脚本静态、后端 RuntimeControl 单测、前端静态和证据 validator 已通过。
- M5 收尾：完成，cleanup 预览无删除项、无阻塞、无警告。

## 验证结果

- `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q`：PASS，6 passed。
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`：PASS，65 tests。
- `node tests\e2e\runtime-control-test-db-quick-apply-static.spec.js`：PASS。
- `node tests\e2e\runtime-control-static.spec.js`：PASS。
- `node tests\e2e\runtime-control-release-package-static.spec.js`：PASS。
- `pnpm ts:check`：FAIL 于无关既有文件 `src/views/mes/pro/route-use/RouteUsePage.vue`，本任务未修改该文件。
- `task-closeout-cleanup --mode preview`：PASS，delete/blocked/warnings 均为 `<none>`。

## Cleanup Keep

- `doc/tasks/20260615-test-db-quick-apply/database-schema-evidence.md`
