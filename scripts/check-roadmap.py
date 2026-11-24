#!/usr/bin/env python3
"""Verify the mandatory alpha roadmap's checklist and HANDOFF progress."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
roadmap = (root / "ROADMAP.md").read_text(encoding="utf-8")
handoff = (root / "HANDOFF.md").read_text(encoding="utf-8")
items = re.findall(r"^- \[([ x])\] (R\d{2}\.\d{2}) (.+)$", roadmap, re.M)
if not items:
    sys.exit("No mandatory roadmap items")
ids = [item[1] for item in items]
if len(ids) != len(set(ids)):
    sys.exit("Duplicate roadmap item ID")
groups = {}
for mark, item_id, description in items:
    group = item_id.split(".")[0]
    counts = groups.setdefault(group, [0, 0])
    counts[1] += 1
    if mark == "x":
        if "Доказательство:" not in description or not description.split("Доказательство:", 1)[1].strip():
            sys.exit(f"Missing evidence: {item_id}")
        counts[0] += 1
headings = re.findall(r"^### (R\d{2}) — .+$", roadmap, re.M)
if len(headings) < 6 or len(headings) != len(set(headings)) or set(headings) != set(groups):
    sys.exit("Roadmap headings do not match checklist groups")
rows = re.findall(r"^\| (R\d{2}) \| [^|]+ \| (\d+)/(\d+) \| ([^|]+) \|$", roadmap, re.M)
expected = {group: (a, b, "Готово" if a == b else "В работе" if a else "Запланирован") for group, (a,b) in groups.items()}
actual = {group: (int(a),int(b),status.strip()) for group,a,b,status in rows}
if len(rows) != len(expected) or actual != expected:
    sys.exit(f"Roadmap summary inconsistent; expected {expected}")
done = sum(v[0] for v in groups.values())
total = len(items)
progress = f"{done}/{total} ({done / total * 100:.1f}%)"
closed = sum(a == b for a,b in groups.values())
if f"Обязательные пункты: **{progress}**." not in roadmap:
    sys.exit(f"ROADMAP progress must be {progress}")
if f"Завершённые этапы: **{closed}/{len(groups)}**." not in roadmap:
    sys.exit("ROADMAP closed-stage count mismatch")
if f"Прогресс ROADMAP: **{progress}**." not in handoff:
    sys.exit(f"HANDOFF progress must be {progress}")
if "[ROADMAP.md](ROADMAP.md)" not in handoff:
    sys.exit("HANDOFF missing ROADMAP link")
print(f"ROADMAP verified: {progress}; stages {closed}/{len(groups)}")
