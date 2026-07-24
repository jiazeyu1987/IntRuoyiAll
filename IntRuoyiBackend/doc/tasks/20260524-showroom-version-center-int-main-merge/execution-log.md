# 执行日志：展厅版本中心融合到 int_main

## 2026-05-24

- BDD: 后端融合准备 -> Given `task/20260523-showroom-version-center-impl` 已完成且 `int_main` 有新提交且主工作区脏, When 在隔离 worktree 分支吸收 `int_main` 已提交历史, Then 不覆盖主工作区未提交改动且产出可验证的融合结果
- RED: `git merge --ff-only int_main` -> FAIL, `task/20260523-showroom-version-center-impl` 与 `int_main` 已分叉，非快进
- INFO: `git status --short` @ `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` -> 主工作区存在与版本中心改动重叠的未提交文件，不能直接在主工作区安全 merge
- GREEN: `git merge --no-commit int_main` -> PASS（在后端 worktree 吸收 `int_main` 已提交历史，冲突已解）
- RED: `mvn -pl yudao-module-showroom -DskipTests compile` -> FAIL, `ShowroomNarrationVersionMapper` 合并后残留重复语句导致语法错误
- GREEN: `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS
- RED: `javac @yudao-module-showroom/target/javac-version-center.args && java @yudao-module-showroom/target/java-version-center.args` -> FAIL, `AbstractShowroomReleaseDbTest` 新基座未为产品发布 preview asset，`insertProductBundle` 前置条件失效；另有公司 cover_image 断言仍停留在旧 URL
- GREEN: `javac @yudao-module-showroom/target/javac-version-center.args && java @yudao-module-showroom/target/java-version-center.args` -> PASS（30 tests）
- GREEN: `python -m pytest script/tests/test_showroom_version_center_sql.py -q` -> PASS（2 tests）
- REGRESSION: `javac @yudao-module-showroom/target/javac-version-center.args && java @yudao-module-showroom/target/java-version-center.args` -> PASS
- GREEN: `git merge --no-commit int_main` -> PASS（继续吸收最新 `int_main` 提交，解决 `ShowroomSchemaMapperContractTest` 冲突）
- REGRESSION: `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS
- REGRESSION: `javac @yudao-module-showroom/target/javac-version-center.args && java @yudao-module-showroom/target/java-version-center.args` -> PASS（30 tests）
- BLOCKED: 后端主工作区 `int_main` 仍有与本任务重叠的未提交改动；当前不能安全执行真正的分支前推
