# 执行日志：20260616-showroom-company-honor-hall

BDD: 当前奖项归入公司荣誉展柜 -> Given 测试租户已有已发布奖项 / When 应用公司荣誉展柜规则 / Then 展柜管理出现 `公司荣誉展柜`，且所有奖项都以 `AWARD` 展项归入该展柜并具备完整画布布局。

BDD: 后续奖项自动归入公司荣誉展柜 -> Given 用户新增或导入发布一个奖项 / When 奖项发布成功 / Then 系统自动确保 `公司荣誉展柜` 存在，并将该奖项加入该展柜。

BDD: 奖项唯一归属公司荣誉展柜 -> Given 管理员维护非公司荣誉展柜 / When 保存包含 `AWARD` 的展项映射 / Then 后端明确失败，不能把奖项放入其他展柜。

INFO: 经验门禁 -> 已读取 `docs/experience-index.md`、`docs/login-access.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本任务只允许本机验证，不访问测试服或正式服。

INFO: 只读数据库现状 -> 本机 `tenant_id=122` 有 46 条奖项；不存在 `company_honor` 或 `公司荣誉展柜`；`showroom_hall_item` 当前无 `AWARD` 映射；46 条奖项都有当前发布修订版和封面，只有 1 个奖项有公开中英文语音。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomPersistentContentServiceTest#awardPublishShouldEnsureCompanyHonorHallAndBindAwardOnlyThere+nonHonorHallShouldRejectAwardMappings test` -> FAIL，预期原因：发布奖项不会自动创建公司荣誉展柜，且非荣誉展柜仍可保存 `AWARD` 映射。

GREEN: experience-preflight -> PASS；本次数据库写入仅作用本机 Docker MySQL `int-ruoyi-mysql` 的测试租户 `tenant_id=122`，不访问测试服/正式服，不修改芋道源码租户数据。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomPersistentContentServiceTest#awardPublishShouldEnsureCompanyHonorHallAndBindAwardOnlyThere+nonHonorHallShouldRejectAwardMappings test` -> PASS。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHallMixedItemContentTest,ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldExposeAwardDocumentFieldsRequiredByWebsiteRuntime" test` -> PASS。

GREEN: `SET @showroom_company_honor_target_tenant_id := 122; source sql/showroom/20260616_showroom_company_honor_hall.sql` -> PASS；本机 `tenant_id=122` 创建 `company_honor/公司荣誉展柜`。

GREEN: 数据库只读核验 -> PASS；`award_count=46`、`honor_award_mapping_count=46`、`non_honor_award_mapping_count=0`、`incomplete_layout_count=0`、`layout_area=1.000000`。

RED: `python -X utf8 -m pytest script\tests\test_showroom_company_honor_hall_sql.py -q` -> FAIL，预期原因：公司荣誉展柜数据修复 SQL 契约测试尚不存在。

GREEN: `python -X utf8 -m pytest script\tests\test_showroom_company_honor_hall_sql.py -q` -> PASS，5 passed。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260616-showroom-company-honor-hall\database-schema-evidence.md` -> PASS。

BLOCKER: Playwright 页面只读验证 -> 登录页实际提交 `tenantName=测试租户`、`username=aoteman`、`password=admin123`，后端 `/admin-api/system/auth/login` 返回 `code=500`、`message=登录失败，账号密码不正确`；影响：无法通过真实浏览器进入 `/showroom/hall` 复核页面文案和公司荣誉展柜可见性，未切换账号或环境替代。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-showroom-company-honor-hall --mode preview` -> PASS；`delete=<none>`、`blocked=<none>`、`warnings=<none>`。
