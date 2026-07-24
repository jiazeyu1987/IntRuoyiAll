# Execution Log：补齐安全模块测试依赖以放行已提交版本发布

BDD: 已提交版本构建不得因测试依赖遗漏而在 testCompile 失败 -> Given `SecurityFrameworkServiceImplTest` 已进入 git 提交并被干净 release worktree 编译 / When 执行安全模块定向 Maven 构建 / Then 模块 `pom.xml` 必须显式声明所需的 JUnit / Mockito 测试依赖，且 `testCompile` 能通过。

RED: `mvn -f D:\ProjectPackage\Int\release-worktrees\IntRuoyi-backend-20260629-e4d82d1\pom.xml -pl yudao-framework/yudao-spring-boot-starter-security -DskipTests test -q` -> FAIL，`SecurityFrameworkServiceImplTest` 编译时缺少 `org.junit.jupiter.api`、`org.mockito`、`org.mockito.junit.jupiter` 等测试依赖。

GREEN: `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff -- yudao-framework/yudao-spring-boot-starter-security/pom.xml` -> PASS，确认主工作区已有未提交最小修复：补齐 `spring-boot-starter-test` 与 `mockito-inline` 两个 test scope 依赖。

GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-framework/yudao-spring-boot-starter-security -DskipTests test -q` -> PASS，补齐测试依赖后安全模块 `testCompile` 已通过，不再报 JUnit / Mockito 缺失。
