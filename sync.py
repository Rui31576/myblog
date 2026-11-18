#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从飞书多维表格拉数据 → 写进 assets/index.html → 重新出包。

用法：python3 sync.py [--no-build]
依赖：lark-cli 已登录（user 身份），build.sh 可用。
"""
import json
import os
import re
import subprocess
import sys

BASE = "LKHTbzXoZaQ0DPspOpDcvN4UnCf"
TABLE = "tbl6OxkseTXKl8yR"
HERE = os.path.dirname(os.path.abspath(__file__))


def fetch():
    cmd = ["lark-cli", "base", "+record-list", "--as", "user",
           "--base-token", BASE, "--table-id", TABLE,
           "--format", "json", "--limit", "200"]
    raw = subprocess.run(cmd, capture_output=True, text=True, timeout=120).stdout
    d = json.loads(raw)
    if not d.get("ok"):
        raise SystemExit("拉取失败: " + json.dumps(d.get("error"), ensure_ascii=False))
    data = d["data"]
    cols, rows = data["fields"], data["data"]
    out = []
    for r in rows:
        rec = {}
        for c, v in zip(cols, r):
            rec[c] = v[0] if isinstance(v, list) and v else v
        out.append(rec)
    return out


def inject(records):
    p = os.path.join(HERE, "assets", "index.html")
    s = open(p, encoding="utf-8").read()
    payload = json.dumps(records, ensure_ascii=False)
    pat = re.compile(r"const SNAPSHOT = .*?;\n\nconst ORDER", re.S)
    if not pat.search(s):
        raise SystemExit("注入失败：没找到 SNAPSHOT 标记")
    s2 = pat.sub(lambda m: "const SNAPSHOT = " + payload + ";\n\nconst ORDER", s, count=1)
    open(p, "w", encoding="utf-8").write(s2)
    return len(records)


def build():
    return subprocess.run(["bash", os.path.join(HERE, "build.sh")],
                          cwd=HERE, capture_output=True, text=True)


if __name__ == "__main__":
    recs = fetch()
    n = inject(recs)
    print("已注入 %d 条" % n)
    if "--no-build" not in sys.argv:
        r = build()
        print(r.stdout[-500:] if r.stdout else "", r.stderr[-500:] if r.stderr else "")
        print("退出码", r.returncode)
