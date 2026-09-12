import copy
import unittest

from db_diff import diagram, render


class SchemaReportTest(unittest.TestCase):
    def test_nullable_fk_and_unique_changes(self):
        before = {"accounts": {
            "options": ["InnoDB", "utf8mb4_0900_ai_ci", ""],
            "columns": [["id", "binary", "binary(16)", "NO", None],
                        ["email", "varchar", "varchar(255)", "NO", None]],
            "indexes": [["PRIMARY", 0, 1, "id", None, "BTREE", None, "YES"]],
            "foreign_keys": [], "checks": [],
        }}
        after = copy.deepcopy(before)
        after["accounts"]["columns"][1][3] = "YES"
        after["sessions"] = {
            "options": [], "columns": [["account_id", "binary", "binary(16)", "NO", None]],
            "indexes": [], "foreign_keys": [["fk_session", "account_id", "accounts", "id", "RESTRICT", "CASCADE"]],
            "checks": [],
        }
        report = render(before, after)
        self.assertIn("변경: `accounts`", report)
        self.assertIn("추가: `sessions`", report)
        self.assertIn("T0 ||..o{ T1", report)
        self.assertIn("varchar(255)_ NOT NULL", report)
        self.assertIn("varchar(255)_ NULL", report)
        after["sessions"]["indexes"] = [["unique_account", 0, 1, "account_id", None, "BTREE", None, "YES"]]
        self.assertIn("T0 ||..o| T1", diagram(after, set(after)))
        self.assertIn("삭제: `sessions`", render(after, before))

    def test_unchanged_and_unsafe_label(self):
        self.assertIn("최종 스키마 차이는 없습니다", render({}, {}))
        malicious = {"x\"```\n": {"options": [], "columns": [], "indexes": [], "foreign_keys": [], "checks": []}}
        self.assertEqual(diagram(malicious, set(malicious)).count("```"), 2)


if __name__ == "__main__":
    unittest.main()
