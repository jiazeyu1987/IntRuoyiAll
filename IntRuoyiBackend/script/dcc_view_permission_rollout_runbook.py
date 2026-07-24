import argparse
from pathlib import Path


DEFAULT_MATRIX_WORKBOOK = "../doc/tasks/20260613-dcc-matrix-activation-preflight-v2/outputs/dcc-matrix-activation-preflight.xlsx"
DEFAULT_PRODUCT_GROUP_WORKBOOK = "../doc/tasks/20260614-dcc-product-group-confirmation-workbook/dcc-product-group-confirmation.xlsx"
DEFAULT_BUNDLE_DIR = "../doc/tasks/20260614-dcc-view-permission-sql-bundle/bundle-output"
DEFAULT_GATE_JSON = "../doc/tasks/20260614-dcc-view-permission-confirmation-gate/confirmation-gate-result.json"


def build_runbook(matrix_workbook, product_group_workbook, bundle_dir, gate_json, mysql_command):
    return f"""# DCC 查看权限矩阵落地 Runbook

## 适用范围

- 仅用于测试租户先行验证 DCC 文件查阅矩阵查看权限。
- 不得跳过业务确认工作簿、确认闸门、SQL 包静态校验或执行后只读校验。
- 未经明确授权，不得在正式环境执行 SQL。

## 输入文件

- 主矩阵确认工作簿：`{matrix_workbook}`
- 产品组确认工作簿：`{product_group_workbook}`
- SQL 包输出目录：`{bundle_dir}`
- 确认闸门结果：`{gate_json}`

## 执行步骤

### 1. 业务填写确认工作簿

- 主矩阵 `文件归类待确认.manual_confirm_category_code`
- 主矩阵 `主管角色候选.manual_confirm`
- 产品组 `候选明细.group_code`
- 产品组 `候选明细.group_name`
- 产品组 `候选明细.manual_confirm`

### 2. 运行确认闸门

```powershell
python -X utf8 script\\dcc_view_permission_confirmation_gate.py --matrix-workbook {matrix_workbook} --product-group-workbook {product_group_workbook} --output-json {gate_json}
```

### 3. 生成 SQL 包

```powershell
python -X utf8 script\\dcc_view_permission_sql_bundle.py --matrix-workbook {matrix_workbook} --product-group-workbook {product_group_workbook} --output-dir {bundle_dir}
```

### 4. 执行前静态校验

```powershell
python -X utf8 script\\dcc_view_permission_sql_bundle_verify.py --bundle-dir {bundle_dir}
```

### 5. 测试租户执行 SQL

按 `manifest.json` 的 `executionOrder` 顺序，在测试租户数据库执行：

1. `01-dcc-matrix-confirmed.sql`
2. `02-dcc-product-group-confirmed.sql`

示例命令占位：

```powershell
{mysql_command} < {bundle_dir}\\01-dcc-matrix-confirmed.sql
{mysql_command} < {bundle_dir}\\02-dcc-product-group-confirmed.sql
```

### 6. 执行后只读校验

```powershell
python -X utf8 script\\dcc_view_permission_apply_verify.py --bundle-dir {bundle_dir} --mysql-command {mysql_command}
```

### 7. 真实用户路径验证

- 使用测试租户账号登录前端。
- 验证有权限用户能查询、进入详情、预览对应 DCC 文件。
- 验证无权限用户不能看到或打开超出矩阵范围的 DCC 文件。
- 记录浏览器截图、接口响应和执行后只读校验输出。

## 禁止事项

- 禁止跳过确认闸门直接生成或执行 SQL。
- 禁止未做静态校验就执行 SQL。
- 禁止在未完成测试租户验证前操作正式环境。
- 禁止手工修改 SQL 包里的确认 CSV、manifest 或 SQL 执行顺序。
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-md", required=True)
    parser.add_argument("--matrix-workbook", default=DEFAULT_MATRIX_WORKBOOK)
    parser.add_argument("--product-group-workbook", default=DEFAULT_PRODUCT_GROUP_WORKBOOK)
    parser.add_argument("--bundle-dir", default=DEFAULT_BUNDLE_DIR)
    parser.add_argument("--gate-json", default=DEFAULT_GATE_JSON)
    parser.add_argument("--mysql-command", default="mysql --defaults-extra-file=TEST_TENANT_CLIENT.cnf TEST_DATABASE")
    args = parser.parse_args()

    output = Path(args.output_md)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        build_runbook(args.matrix_workbook, args.product_group_workbook, args.bundle_dir, args.gate_json, args.mysql_command),
        encoding="utf-8",
    )
    print(f"runbook={output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
