"""Compare disposable MySQL schemas and emit a PR-ready Mermaid report (stdlib only)."""

import difflib
import json
import os
from pathlib import Path
import re
import subprocess
import sys


def query(schema, sql):
    result = subprocess.run(
        ["mysql", "--protocol=TCP", "-h", os.environ.get("MYSQL_HOST", "127.0.0.1"),
         "-P", os.environ.get("MYSQL_TCP_PORT", "3306"), "-u", os.environ.get("MYSQL_USER", "root"),
         "--batch", "--raw", "--skip-column-names", schema, "-e", sql],
        check=True, stdout=subprocess.PIPE, text=True,
    )
    return [json.loads(line) for line in result.stdout.splitlines()]


def snapshot(schema):
    # Read metadata from DATABASE(), never interpolate PR-provided SQL identifiers.
    tables = query(schema, """
        SELECT JSON_ARRAY(TABLE_NAME, ENGINE, TABLE_COLLATION, TABLE_COMMENT)
        FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME <> 'flyway_schema_history' ORDER BY TABLE_NAME
    """)
    result = {name: {"options": options, "columns": [], "indexes": [], "foreign_keys": [], "checks": []}
              for name, *options in tables}
    queries = {
        "columns": """SELECT JSON_ARRAY(TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE,
            IS_NULLABLE, COLUMN_DEFAULT, EXTRA, GENERATION_EXPRESSION, COLLATION_NAME, COLUMN_COMMENT)
            FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
            ORDER BY TABLE_NAME, ORDINAL_POSITION""",
        "indexes": """SELECT JSON_ARRAY(TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX,
            COLUMN_NAME, SUB_PART, INDEX_TYPE, EXPRESSION, IS_VISIBLE)
            FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
            ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX""",
        "foreign_keys": """SELECT JSON_ARRAY(k.TABLE_NAME, k.CONSTRAINT_NAME, k.COLUMN_NAME,
            k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME, r.UPDATE_RULE, r.DELETE_RULE)
            FROM information_schema.KEY_COLUMN_USAGE k
            JOIN information_schema.REFERENTIAL_CONSTRAINTS r
            ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA AND r.TABLE_NAME = k.TABLE_NAME
            AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
            WHERE k.TABLE_SCHEMA = DATABASE() AND k.REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION""",
        "checks": """SELECT JSON_ARRAY(t.TABLE_NAME, t.CONSTRAINT_NAME, c.CHECK_CLAUSE, t.ENFORCED)
            FROM information_schema.TABLE_CONSTRAINTS t
            JOIN information_schema.CHECK_CONSTRAINTS c
            ON t.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA AND t.CONSTRAINT_NAME = c.CONSTRAINT_NAME
            WHERE t.TABLE_SCHEMA = DATABASE() AND t.CONSTRAINT_TYPE = 'CHECK'
            ORDER BY t.TABLE_NAME, t.CONSTRAINT_NAME""",
    }
    for kind, sql in queries.items():
        for name, *row in query(schema, sql):
            if name in result:
                result[name][kind].append(row)
    return result


def label(value):
    # Mermaid labels cannot contain raw quotes, newlines or Markdown fences.
    return re.sub(r'[^\w .(),:=+/-]', '_', str(value), flags=re.ASCII)


def diagram(tables, selected):
    present = sorted(set(tables) & selected)
    if not present:
        return "해당 테이블 없음.\n"
    ids = {name: f"T{i}" for i, name in enumerate(present)}
    lines = ["```mermaid", "erDiagram"]
    for name in present:
        table = tables[name]
        lines.append(f'    {ids[name]}["{label(name)}"] {{')
        for column, dtype, full_type, nullable, default, *_ in table["columns"]:
            keys = []
            if any(index[0] == "PRIMARY" and index[3] == column for index in table["indexes"]):
                keys.append("PK")
            if any(fk[1] == column for fk in table["foreign_keys"]):
                keys.append("FK")
            if any(index[0] != "PRIMARY" and index[1] == 0 and index[3] == column for index in table["indexes"]):
                keys.append("UK")
            details = f'{full_type}; {"NULL" if nullable == "YES" else "NOT NULL"}'
            if default is not None:
                details += f"; default={default}"
            safe_column = re.sub(r"\W", "_", column, flags=re.ASCII)
            lines.append(f'        {label(dtype)} {safe_column} {",".join(keys)} "{label(details)}"')
        lines.append("    }")
        grouped = {}
        for constraint, column, parent, parent_column, *_ in table["foreign_keys"]:
            grouped.setdefault((constraint, parent), []).append((column, parent_column))
        for (constraint, parent), pairs in grouped.items():
            if parent not in ids:
                continue
            child_columns = {pair[0] for pair in pairs}
            nullable = any(c[0] in child_columns and c[3] == "YES" for c in table["columns"])
            unique_indexes = {}
            for index, non_unique, _, column, prefix, *_ in table["indexes"]:
                if non_unique == 0:
                    unique_indexes.setdefault(index, set()).add(column)
            unique = any(cols and None not in cols and cols <= child_columns for cols in unique_indexes.values())
            left = "|o" if nullable else "||"
            right = "o|" if unique else "o{"
            lines.append(f'    {ids[parent]} {left}..{right} {ids[name]} : "{label(constraint)}"')
    return "\n".join(lines + ["```", ""])


def render(before, after):
    changed = sorted(name for name in before.keys() | after.keys() if before.get(name) != after.get(name))
    if not changed:
        return "마이그레이션 파일은 변경됐지만 최종 스키마 차이는 없습니다. 데이터 변경 여부는 SQL diff를 확인하세요.\n"
    selected = set(changed)
    for tables in (before, after):
        for name, table in tables.items():
            for fk in table["foreign_keys"]:
                if name in changed or fk[2] in changed:
                    selected.update((name, fk[2]))
    lines = ["변경된 테이블과 직접 연결된 테이블을 표시합니다. 데이터 변경·운영 데이터 호환성은 검증하지 않습니다.",
             "UK는 복합 UNIQUE의 구성 컬럼일 수 있습니다. 정확한 제약은 아래 diff를 확인하세요.", ""]
    for name in changed:
        status = "추가" if name not in before else "삭제" if name not in after else "변경"
        lines.append(f"- {status}: `{label(name)}`")
    lines.extend(["", "### 변경 전", "", diagram(before, selected), "### 변경 후", "", diagram(after, selected),
                  "<details><summary>스키마 상세 diff (컬럼·인덱스·FK·CHECK·테이블 옵션)</summary>", "", "```diff"])
    old = json.dumps({n: before[n] for n in changed if n in before}, ensure_ascii=True, indent=2).splitlines()
    new = json.dumps({n: after[n] for n in changed if n in after}, ensure_ascii=True, indent=2).splitlines()
    lines.extend(line.replace("`", "\\u0060") for line in difflib.unified_diff(old, new, fromfile="before", tofile="after", lineterm=""))
    lines.extend(["```", "", "</details>", ""])
    return "\n".join(lines)


if __name__ == "__main__":
    before, after, output = sys.argv[1:]
    Path(output).write_text(render(snapshot(before), snapshot(after)), encoding="utf-8")
