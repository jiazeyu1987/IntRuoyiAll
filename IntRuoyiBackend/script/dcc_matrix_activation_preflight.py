import csv
import json
import re
import subprocess
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
TASK = ROOT / "doc/tasks/20260613-dcc-matrix-activation-preflight-v2"
SEED = ROOT / "ruoyi-vue-pro/sql/mysql/20260613_dcc_file_view_matrix_seed.sql"


def qjson(full_sql):
    cmd = [
        "docker", "exec", "int-ruoyi-mysql", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-p123456", "--batch", "--raw", "--skip-column-names", "ruoyi-vue-pro",
        "-e", full_sql,
    ]
    cp = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    if cp.returncode != 0:
        raise RuntimeError(cp.stderr.decode("utf-8", errors="replace"))
    return [json.loads(line) for line in cp.stdout.decode("utf-8", errors="replace").splitlines() if line]


def insert_block(seed_text, table, next_marker):
    start = seed_text.find(f"INSERT INTO {table}")
    end = seed_text.find(next_marker, start)
    if start < 0 or end < 0:
        raise RuntimeError(f"Cannot locate insert block for {table}")
    return seed_text[start:end].split("VALUES", 1)[1].rsplit(";", 1)[0]


def norm(text):
    return re.sub(r"[\s　_\-—/\\（）()、，,.:：;；\[\]【】]+", "", str(text or "").lower())


def pattern_prefix(pattern):
    pattern = (pattern or "").strip()
    if not pattern or pattern == "/":
        return ""
    for token in ["项目代码", "设备编号", "模具编号"]:
        if token in pattern:
            return pattern.split(token)[0].strip("-")
    return re.split(r"[-/]", pattern)[0]


def write_csv(path, rows, fields):
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def descendants(children, dept_id):
    result = []
    stack = [dept_id]
    while stack:
        current = stack.pop()
        for child in children.get(current, []):
            result.append(child)
            stack.append(child["id"])
    return result


def main():
    TASK.mkdir(parents=True, exist_ok=True)
    seed = SEED.read_text(encoding="utf-8")
    categories = [
        {
            "matrix_group": m.group(1),
            "matrix_sort": int(m.group(2)),
            "file_number_pattern": m.group(3),
            "matrix_file_name": m.group(4),
            "category_code": m.group(5),
        }
        for m in re.finditer(
            r"\('([^']*)',\s*(\d+),\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'\)",
            insert_block(seed, "tmp_dcc_file_view_matrix_category",
                         "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_department"),
        )
    ]
    roles = [
        {
            "matrix_department": m.group(1),
            "role_name": m.group(2),
            "role_code": m.group(3),
            "role_remark": m.group(4),
        }
        for m in re.finditer(
            r"\('([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'\)",
            insert_block(seed, "tmp_dcc_file_view_matrix_role",
                         "DROP TEMPORARY TABLE IF EXISTS tmp_dcc_file_view_matrix_grant"),
        )
    ]
    if len(categories) != 59:
        raise RuntimeError(f"Expected 59 matrix categories, got {len(categories)}")

    files = qjson("""
SELECT JSON_OBJECT('file_id', f.id, 'tenant_id', f.tenant_id, 'deleted', f.deleted,
'title', f.title, 'file_name', IFNULL(f.file_name,''), 'file_number', IFNULL(f.file_number,''),
'product_code', IFNULL(f.product_code,''), 'product_name', IFNULL(f.product_name,''),
'current_category_id', f.category_id, 'current_category_name', IFNULL(c.name,''),
'current_category_code', IFNULL(c.code,''), 'file_status', f.status,
'version_no', f.version_no, 'published_time', IFNULL(CAST(f.published_time AS CHAR),''),
'create_time', CAST(f.create_time AS CHAR))
FROM dcc_controlled_file f
LEFT JOIN dcc_file_category c ON c.id=f.category_id
WHERE f.tenant_id=1
ORDER BY f.id
""")

    def best_match(file_row):
        haystack = norm(" ".join([
            file_row.get("title", ""),
            file_row.get("file_name", ""),
            file_row.get("file_number", ""),
            file_row.get("current_category_name", ""),
        ]))
        file_no = (file_row.get("file_number") or "").upper()
        scored = []
        for category in categories:
            score = 0
            reasons = []
            category_name = norm(category["matrix_file_name"])
            if category_name and category_name in haystack:
                score += 80
                reasons.append("名称包含矩阵文件名")
            parts = [norm(x) for x in re.split(r"[、/]", category["matrix_file_name"]) if norm(x)]
            hits = sum(1 for part in parts if len(part) >= 2 and part in haystack)
            if hits and score < 80:
                score += min(50, hits * 20)
                reasons.append(f"命中复合名称片段{hits}个")
            prefix = pattern_prefix(category["file_number_pattern"]).upper()
            if prefix and file_no.startswith(prefix):
                score += 30
                reasons.append(f"文件编号前缀匹配{prefix}")
            if category["category_code"] == file_row.get("current_category_code"):
                score += 100
                reasons.append("当前分类编码已是矩阵编码")
            if score:
                scored.append((score, category, ";".join(reasons)))
        if not scored:
            return None, 0, "", "无法匹配"
        scored.sort(key=lambda item: (-item[0], item[1]["matrix_group"], item[1]["matrix_sort"]))
        score, category, reason = scored[0]
        if score >= 100:
            status = "高置信"
        elif score >= 80:
            status = "候选"
        else:
            status = "建议确认"
        return category, score, reason, status

    classification = []
    summary = {"total": len(files), "高置信": 0, "候选": 0, "建议确认": 0, "无法匹配": 0}
    for file_row in files:
        category, score, reason, status = best_match(file_row)
        summary[status] += 1
        row = dict(file_row)
        row.update({
            "candidate_category_code": category["category_code"] if category else "",
            "candidate_group": category["matrix_group"] if category else "",
            "candidate_sort": category["matrix_sort"] if category else "",
            "candidate_file_name": category["matrix_file_name"] if category else "",
            "candidate_file_number_pattern": category["file_number_pattern"] if category else "",
            "match_score": score,
            "match_reason": reason,
            "confirmation_status": status,
            "manual_confirm_category_code": "",
            "manual_confirm_note": "",
        })
        classification.append(row)

    active_counts = qjson("""
SELECT JSON_OBJECT('tenant_id',tenant_id,'deleted',deleted,'cnt',COUNT(*))
FROM dcc_controlled_file
GROUP BY tenant_id,deleted
ORDER BY tenant_id,deleted
""")
    depts = qjson("""
SELECT JSON_OBJECT('id',id,'parent_id',parent_id,'name',name)
FROM system_dept
WHERE tenant_id=1 AND deleted=b'0'
""")
    children = {}
    for dept in depts:
        children.setdefault(dept["parent_id"], []).append(dept)

    dept_map = {"新品开发部": 136, "包装设计": 344, "生产": 125, "生产采购": 359, "QC": 222}
    role_rows = []
    for role in roles:
        dept_id = dept_map.get(role["matrix_department"])
        if not dept_id:
            role_rows.append({
                **role,
                "source_dept_id": "",
                "candidate_user_id": "",
                "username": "",
                "nickname": "",
                "candidate_source": "无显式部门映射",
                "confirmation_status": "需人工确认",
                "manual_confirm": "",
                "note": "seed 角色需人工指定成员",
            })
            continue
        ids = [dept_id] + [dept["id"] for dept in descendants(children, dept_id)]
        id_csv = ",".join(str(x) for x in ids)
        leaders = qjson(f"""
SELECT JSON_OBJECT('user_id',u.id,'username',u.username,'nickname',u.nickname,
'dept_id',d.id,'dept_name',d.name)
FROM system_dept d
JOIN system_users u ON u.id=d.leader_user_id AND u.deleted=b'0' AND u.status=0
WHERE d.tenant_id=1 AND d.deleted=b'0' AND d.id IN ({id_csv})
ORDER BY d.id,u.id
""")
        posts = []
        if role["matrix_department"] == "生产":
            posts = qjson(f"""
SELECT JSON_OBJECT('user_id',u.id,'username',u.username,'nickname',u.nickname,
'dept_id',u.dept_id,'dept_name',IFNULL(d.name,''))
FROM system_users u
JOIN system_user_post up ON up.user_id=u.id AND up.deleted=b'0' AND up.tenant_id=u.tenant_id
JOIN system_post p ON p.id=up.post_id AND p.deleted=b'0' AND p.tenant_id=u.tenant_id
LEFT JOIN system_dept d ON d.id=u.dept_id
WHERE u.tenant_id=1 AND u.deleted=b'0' AND u.status=0
  AND u.dept_id IN ({id_csv})
  AND (p.name LIKE '%主任%' OR p.name LIKE '%主管%' OR p.code='WORKSHOP_DIRECTOR')
ORDER BY u.id
""")
        seen = set()
        candidates = []
        for row in leaders:
            if row["user_id"] not in seen:
                seen.add(row["user_id"])
                candidates.append((row, "部门负责人"))
        for row in posts:
            if row["user_id"] not in seen:
                seen.add(row["user_id"])
                candidates.append((row, "岗位含主管/主任"))
        if not candidates:
            role_rows.append({
                **role,
                "source_dept_id": dept_id,
                "candidate_user_id": "",
                "username": "",
                "nickname": "",
                "candidate_source": "未找到候选",
                "confirmation_status": "需人工确认",
                "manual_confirm": "",
                "note": "没有可自动建议的负责人/主管岗位用户",
            })
        for row, source in candidates:
            role_rows.append({
                **role,
                "source_dept_id": dept_id,
                "candidate_user_id": row["user_id"],
                "username": row["username"],
                "nickname": row["nickname"],
                "candidate_source": f"{source}: {row['dept_name']}({row['dept_id']})",
                "confirmation_status": "待确认",
                "manual_confirm": "",
                "note": "",
            })

    class_path = TASK / "dcc-tenant1-deleted-file-classification-review.csv"
    role_path = TASK / "dcc-matrix-role-member-candidates.csv"
    matrix_path = TASK / "dcc-matrix-category-reference.csv"
    summary_path = TASK / "activation-preflight-summary.md"
    write_csv(matrix_path, categories, ["matrix_group", "matrix_sort", "file_number_pattern",
                                        "matrix_file_name", "category_code"])
    write_csv(class_path, classification, list(classification[0].keys()))
    write_csv(role_path, role_rows, ["matrix_department", "role_name", "role_code", "role_remark",
                                     "source_dept_id", "candidate_user_id", "username", "nickname",
                                     "candidate_source", "confirmation_status", "manual_confirm", "note"])

    lines = [
        "# DCC 文件查阅矩阵真实启用前置摘要",
        "",
        f"- Generated at: {datetime.now().isoformat(timespec='seconds')}",
        f"- Tenant 1 DCC files queried: {summary['total']}（当前均为 deleted=1，不能直接按当前启用文件重分类）",
        f"- Matrix categories: {len(categories)}",
        f"- Role definitions requiring membership confirmation: {len(roles)}",
        "",
        "## DCC 文件状态分布",
        "",
    ]
    for item in active_counts:
        lines.append(f"- tenant_id={item['tenant_id']}, deleted={item['deleted']}: {item['cnt']}")
    lines.extend(["", "## tenant_id=1 历史文件归类匹配统计", ""])
    for key in ["高置信", "候选", "建议确认", "无法匹配"]:
        lines.append(f"- {key}: {summary.get(key, 0)}")
    lines.extend([
        "",
        "## 主管角色候选统计",
        "",
        f"- Candidate/member rows: {len(role_rows)}",
        "",
        "## 关键结论",
        "",
        "- 本清单只读生成，未写入 DCC 分类、权限或角色成员数据。",
        "- 当前正式租户文件均为 `deleted=1`，执行真实重分类前必须先确认哪个租户/哪些文件才是当前有效 DCC 文件。",
        "- `▲` 角色成员清单来自部门负责人和生产主管/主任岗位候选，不自动授权；必须在 `manual_confirm` 列确认。",
        "- 新品开发部普通成员仅看组内产品仍缺正式产品组-成员-产品模型，本清单不将其视为完成。",
        "",
        "## 输出文件",
        "",
        f"- `{class_path.name}`",
        f"- `{role_path.name}`",
        f"- `{matrix_path.name}`",
    ])
    summary_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(summary)
    print(f"role_rows={len(role_rows)}")


if __name__ == "__main__":
    main()
