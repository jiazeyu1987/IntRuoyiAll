# Execution Log: DCC 分发规则重复保存唯一键修复

BDD: repeated save replaces the same department rule -> Given a category already
has a distribution rule for one department, When the administrator saves the
same department again through the replace API, Then the backend must replace the
rule instead of hitting the unique key.

BDD: replace semantics remain exact -> Given the replace API owns the whole rule
set for one category, When a new save arrives, Then only the submitted rows
remain active after the save.

- M1: Completed. Created the backend bugfix task package before code edits.
- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccCategoryDistributionRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  -> FAIL, after mirroring the unique constraint into the H2 test schema, the
  second save of the same `category + department` reproduced the duplicate-key
  violation.
- M2: Completed. Recorded the duplicate-save failure as the RED baseline.
- M3: Completed. Added a hard-delete mapper path for
  `dcc_file_category_distribution_rule` and switched replace logic to use it.
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccCategoryDistributionRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  -> PASS, repeated save now keeps one row and the updated
  `distributionMedium`.
- M4: Completed. Targeted backend verification is green.
