BDD: DCC 岗位列表只读本地岗位主数据 -> Given live 后端 `GET /dcc/approval-positions` 已经按设计只读取本地 `dcc_approval_position` / When live 本地库已经导入 IntAuth 当前岗位主数据 / Then 列表接口返回本地 `INTAUTH:*` 岗位而不是运行时调用 IntAuth。

BDD: 管理员一次性导入 IntAuth 当前岗位到本地 -> Given live 后端已加载 `POST /dcc/approval-positions/import-intauth` 实现且 IntAuth 内部岗位契约可用 / When 管理员触发导入 / Then 本地库新增或复用当前 IntAuth 31 条岗位，并保留本地 `INTAUTH:<id>` 来源语义。

BDD: live 本地测试脏数据被清理 -> Given live 本地库存在 `source='E2E'` 的测试岗位和对应分配 / When 本次迁移执行完成 / Then 该测试岗位及其 `dcc_position_assignment` 数据被删除，不再干扰本地岗位一致性口径。

BDD: 本次只迁移岗位主数据 -> Given IntAuth 岗位成员主键是 UUID 字符串而本地 `dcc_position_assignment.user_id` 是 `bigint` / When 执行本次迁移 / Then 只迁移岗位主数据并在证据中明确不迁移成员分配。

RED: `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\inspect_position_alignment.py` -> FAIL, live local MySQL only had 1 `E2E` test position row, `localIntAuthActiveCount=0`, `missingInLocal` listed all 31 IntAuth position names, and `dcc_position_assignment` still contained the matching E2E seed assignment.

RED: `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\migrate_positions_via_live_api.py` before backend restart -> FAIL, live `POST /dcc/approval-positions/import-intauth` returned business error `IntAuth position sync config is missing`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar` with the current DCC local-only/import implementation.

GREEN: `$env:INTERNAL_SERVICE_TOKEN='intkb-local-internal-token'; cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, restarted live backend/frontend runtime with the IntAuth internal token injected for the DCC import path.

GREEN: `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\migrate_positions_via_live_api.py` -> PASS, live import created 31 positions and deleted `E2E` position id `900301`.

GREEN: `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\inspect_position_alignment.py` after migration -> PASS, `intauthCount=31`, `localIntAuthActiveCount=31`, `missingInLocal=[]`, `extraInLocal=[]`, `e2eRows=[]`, and `localAssignmentCount=0`.
