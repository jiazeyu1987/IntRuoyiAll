# 菜单/权限开发约定

菜单迁移在开发时随代码交付，发布只负责检查并按原有流程执行，不能在打包时猜测菜单。

运行 `python scripts/preflight/check-menu-migrations.py`。失败会打印缺少的 `source/kind/value`，原样填写到 `config/menu-migrations.json` 的 `changes` 数组：

```json
{
  "source": "IntRuoyiFronted/src/views/mes/example/index.vue",
  "kind": "page",
  "value": "mes/example/index",
  "migrations": ["IntRuoyiBackend/sql/mysql/20261021_mes_example_menu.sql"]
}
```

一个修改可能产生多个检查项（页面、权限、路由），各项可以关联同一份 SQL。SQL 必须使用现有 `release-migration` 元数据，支持 test/prod/backup，依赖存在且可执行；菜单、目标角色和租户套餐的更新必须放在实际 SQL 中，注释不能充当更新。授权对象按业务需求选择，禁止为了通过检查直接全角色、全租户授权。开发者还须验证真实页面可见及权限有效；静态检查不能证明 SQL 的业务含义或数据库执行结果。

详情页、内部子页面、复用已有权限时，使用 `reason` 替代 `migrations`，写清复用的菜单/权限或无需菜单的理由：

```json
{
  "source": "IntRuoyiFronted/src/views/mes/example/detail/index.vue",
  "kind": "page",
  "value": "mes/example/detail/index",
  "reason": "订单详情通过现有订单列表打开，沿用 mes:example:query，无独立菜单"
}
```

提交时将代码、关联记录和 SQL 一起暂存。pre-commit/pre-merge 检查 Git 暂存区，未暂存的 SQL 不算交付；pre-push 逐一检查实际推送的提交，CI 检查 HEAD，不能靠工作区未提交的 SQL 蒙混过关。路由项用文件指纹标识，每次路由编辑需重新作迁移决定。新克隆仓库后运行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/git/install-branch-runtime-hooks.ps1` 安装本地 hook。

检查累计比较引入门禁时的提交 `32ff79dec7bee877df7a65d896c6143ae6d53523`，不会只检查最后一个提交。该提交之前的菜单缺失需另行补齐，不会因本门禁通过而自动修复。保留历史关联项，不随发布重置基线。

发布入口额外传入实际入包 SQL 清单，关联迁移及其依赖漏打包会直接失败。检查脚本缺失同样失败；旧源码须先合入本门禁再构建新包。前端单独发包遇到需要新增菜单的变更，须改走包含 SQL 的完整发布。

当前静态规则覆盖字面量权限、新建约定页面 `views/**/index.vue` 和静态路由变更；动态拼接权限、非 index.vue 页面或纯数据库新增菜单仍须开发者按本约定登记并由代码审查确认。不要把静态通过当成全部菜单的可见性验收。

Git hook 为本机检查，不能防止人为禁用。GitHub workflow 提交后才会在远端运行；仓库管理员将 `menu-migrations` 设置为分支保护必需检查后，才能阻止未通过检查的 PR 合并。
