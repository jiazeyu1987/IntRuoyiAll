import argparse
import json
from pathlib import Path


MATRIX_WORKBOOK = "../doc/tasks/20260613-dcc-matrix-activation-preflight-v2/outputs/dcc-matrix-activation-preflight.xlsx"
PRODUCT_GROUP_WORKBOOK = "../doc/tasks/20260614-dcc-product-group-confirmation-workbook/dcc-product-group-confirmation.xlsx"


def build_report(gate_result, matrix_workbook, product_group_workbook, output_dir):
    ready = bool(gate_result.get("ready"))
    matrix = gate_result.get("matrix") or {}
    product_group = gate_result.get("productGroup") or {}
    lines = [
        "# DCC 查看权限确认待办报告",
        "",
        f"- 当前状态：{'可生成 SQL 包' if ready else '未就绪，禁止生成或执行 SQL'}",
        f"- 主矩阵确认文件：`{matrix_workbook}`",
        f"- 产品组确认文件：`{product_group_workbook}`",
        "",
        "## 当前确认数量",
        "",
        f"- 主矩阵文件归类确认：{matrix.get('confirmedFiles', 0)}",
        f"- 主矩阵角色成员确认：{matrix.get('confirmedRoles', 0)}",
        f"- 产品组绑定确认：{product_group.get('confirmedProductGroupRows', 0)}",
        "",
    ]

    if ready:
        lines += [
            "## 下一步命令",
            "",
            "确认闸门已通过，可以生成 SQL 包：",
            "",
            "```powershell",
            f"python -X utf8 script\\dcc_view_permission_sql_bundle.py --matrix-workbook {matrix_workbook} --product-group-workbook {product_group_workbook} --output-dir {output_dir}",
            "```",
        ]
        return "\n".join(lines) + "\n"

    lines += [
        "## 阻断原因",
        "",
    ]
    for reason in gate_result.get("reasons") or ["确认闸门未通过"]:
        lines.append(f"- {reason}")

    lines += [
        "",
        "## 业务确认待办",
        "",
        "1. 打开主矩阵确认文件。",
        "2. 在 `文件归类待确认` sheet 中，对确认要启用的文件填写 `manual_confirm_category_code`。",
        "3. 在 `主管角色候选` sheet 中，对确认授权的主管候选填写 `manual_confirm` 为 `确认`。",
        "4. 打开产品组确认文件。",
        "5. 在 `候选明细` sheet 中补齐 `group_code`、`group_name`，并对确认绑定行填写 `manual_confirm` 为 `确认`。",
        "6. 保存工作簿后先运行确认闸门；闸门通过后再运行 SQL 包总入口。",
        "",
        "## 复核命令",
        "",
        "```powershell",
        f"python -X utf8 script\\dcc_view_permission_confirmation_gate.py --matrix-workbook {matrix_workbook} --product-group-workbook {product_group_workbook} --output-json {output_dir}\\confirmation-gate-result.json",
        "```",
    ]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--gate-json", required=True)
    parser.add_argument("--output-md", required=True)
    parser.add_argument("--matrix-workbook", default=MATRIX_WORKBOOK)
    parser.add_argument("--product-group-workbook", default=PRODUCT_GROUP_WORKBOOK)
    parser.add_argument("--bundle-output-dir", default="../doc/tasks/20260614-dcc-view-permission-sql-bundle/bundle-output")
    args = parser.parse_args()

    gate_result = json.loads(Path(args.gate_json).read_text(encoding="utf-8"))
    report = build_report(gate_result, args.matrix_workbook, args.product_group_workbook, args.bundle_output_dir)
    output = Path(args.output_md)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(report, encoding="utf-8")
    print(f"report={output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
