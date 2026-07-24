BDD: 岗位分配页显示固定本地岗位 -> Given local MySQL already contains active fixed岗位 `900333 / 900334` / When the frontend requests `GET /dcc/approval-positions` / Then the response includes these two岗位 with user-facing names `部门负责人` and `部门授权代表`.

BDD: IntAuth 迁移岗位仍保持可见 -> Given imported IntAuth岗位 already exist locally / When the岗位列表接口返回可见岗位 / Then the imported `INTAUTH:*` rows remain visible and keep their current behavior.

BDD: 其他本地测试种子仍被排除 -> Given local test seed rows such as `source='E2E'` may exist / When the岗位列表接口返回可见岗位 / Then unrelated local seed rows remain hidden and are not accidentally exposed.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` before the service change -> FAIL in the new expectation because `getPositionList()` only returned active `INTAUTH:*` rows and excluded local fixed roles `LOCAL-ROLE-APPROVER-DEPT / LOCAL-ROLE-AUTH-REP`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, targeted DCC position tests green after allowing the fixed local岗位 codes through the list filter.

GREEN: live local MySQL update -> PASS, `dcc_approval_position.id=900333` now stores `部门负责人`, and `id=900334` now stores `部门授权代表`.

GREEN: live API check -> PASS, `GET http://127.0.0.1:48081/admin-api/dcc/approval-positions` returned `totalCount=33` and included ids `900333 / 900334` with names `部门负责人 / 部门授权代表`.
