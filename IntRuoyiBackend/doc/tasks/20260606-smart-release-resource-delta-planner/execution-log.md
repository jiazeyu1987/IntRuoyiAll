# Execution Log：Smart Release Resource Delta Planner

BDD: 10000 对象只新增 3 个 -> Given target resource index 已有 10000 个对象且 source reference manifest 只新增 3 个 / When 运行 resource delta planner / Then proof 中 copyObjects=3、verifyOnlyObjects=10000，且没有 full mirror、delete 或 overwrite 操作。

BDD: 同 objectKey 不同 sha256 进入冲突 -> Given source 与 target 有相同 storageProfileId/bucket/objectKey 但 sha256 不同 / When 运行 planner / Then proof 状态为 failed，对象进入 conflictObjects，且不进入 copy 或 verify-only。

BDD: 目标存在但 source 不再引用只产生 tombstone -> Given target index 有对象但 source reference manifest 不再引用 / When 运行 planner / Then 对象进入 tombstoneObjects，proof 不包含物理删除动作。

BDD: 资源引用缺关键字段失败 -> Given source reference 缺 bucket、objectKey、sha256 或 size / When 运行 planner / Then 命令失败并输出明确 errorCode、impact、nextStep。

## Evidence

RED: `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py -q` -> FAIL, expected reason: `run-resource-delta-plan.ps1` does not exist yet.

GREEN: `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py -q` -> PASS, 4 passed.

REGRESSION: `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py script/tests/test_release_manifest_validator.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_release_intake_report_only.py -q` -> PASS, 49 passed.

GREEN: independent verifier subagent `019e9890-a2bc-7b93-9f05-8ea90798194c` -> PASS, confirmed BDD/RED/GREEN/REGRESSION evidence, plan-only local behavior, no remote/delete/mirror/overwrite operations, no hardcoded target IP, and 49-test regression pass.
