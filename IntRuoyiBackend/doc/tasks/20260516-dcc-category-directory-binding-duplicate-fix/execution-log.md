# Execution Log

BDD: Rebinding the same category to the same directory stays idempotent -> Given a category is already bound to a directory, When the admin saves the same directory binding again, Then the backend updates the binding state without throwing a duplicate-key database error.

BDD: Rebinding after prior logical deletions can restore the binding -> Given the category binding table already contains a historical row for the same category-directory pair, When the admin binds that category back to the same directory, Then the backend finishes successfully and leaves one active binding for that pair.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, after aligning the DCC test schema with the production unique key and seeding a historical `deleted=1` binding row, `bindDirectory(...)` crashed on `uk_dcc_category_directory_binding` during `DccCategoryDirectoryBindingMapper.insert`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 7 tests passed. Historical deleted bindings no longer block rebinding because the service now physically clears prior rows for the category before inserting the active binding.
