# 20260524 prod frontend only publish g6 g7 execution log

## BDD

- BDD: direct publish must fail closed on env password mismatch -> Given direct publish would rewrite production `.env` / When local and production MySQL passwords differ / Then do not run direct publish and record the production risk.
- BDD: frontend-only publish preserves data -> Given only the frontend JS backend target is wrong / When reviewer approves frontend-only publish / Then keep production `.env`, MySQL, MinIO and backend image unchanged, replace only frontend image and recreate frontend container.
- BDD: G6/G7 after publish -> Given frontend-only publish completes / When Playwright logs into production and fetches the sample file / Then API requests use production backend and sample PDF is returned.

## TDD Evidence

- RED: `cmd /c "D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-prod-status.bat"` -> PASS for current availability, formal runtime shows `IMAGE_TAG=20260524_035800`, backend/frontend HTTP 200.
- RED: compare production `.env` `MYSQL_ROOT_PASSWORD` hash with local `int-ruoyi-mysql` `MYSQL_ROOT_PASSWORD=123456` hash -> FAIL, hashes differ; direct publish script would rewrite production `.env` with local password.
- RED: fetch `http://172.30.30.57:8081/assets/index-b7bUP0rr.js` and scan backend targets -> FAIL, current production frontend still contains `172.30.30.58:48081`.
- RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k frontend_only -q` -> FAIL, frontend-only production publish script and wrapper did not exist.
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k frontend_only -q` -> PASS, 2 passed.
- GREEN: PowerShell Parser on `publish-int-ruoyi-frontend-only-to-prod.ps1` -> PASS, Parse OK.
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 21 passed.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi` -> PASS.

## 过程记录

- 用户输入：`PROD 责任人 jiazeyu ,tangbin`。
- direct publish script risk:
  - `publish-int-ruoyi-to-test.ps1` has `-SkipDatabaseSync` and `-SkipMinioSync`, but still writes `MYSQL_ROOT_PASSWORD=$mySqlRootPassword` to remote `.env`.
  - `mySqlRootPassword` is read from local container `int-ruoyi-mysql`.
  - Current production `.env` password hash differs from local container env hash.
- 结论：direct publish path is blocked. Need explicit approval for frontend-only production publish.
- 已新增：
  - `ruoyi-vue-pro/script/deploy/publish-int-ruoyi-frontend-only-to-prod.ps1`
  - `ruoyi-vue-pro/script/deploy/publish-int-ruoyi-frontend-only-to-prod.bat`
- 新脚本要求 `FRONTEND-ONLY PROD`，读取正式现有 `IMAGE_TAG`，重建同 tag frontend 镜像，只重建 frontend 容器，不写 `.env`，不触发 MySQL、Redis、MinIO 或 backend 发布。
