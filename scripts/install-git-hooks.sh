#!/usr/bin/env bash
# 把仓库内的 git hook 安装到 .git/hooks（不改 git config）
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
HOOK_SRC="$ROOT/scripts/git-hooks/pre-commit"
HOOK_DST="$ROOT/.git/hooks/pre-commit"

if [ ! -f "$HOOK_SRC" ]; then
  echo "找不到 $HOOK_SRC"
  exit 1
fi

if [ ! -d "$ROOT/.git/hooks" ]; then
  echo "找不到 .git/hooks，请在仓库根目录执行"
  exit 1
fi

cp "$HOOK_SRC" "$HOOK_DST"
chmod +x "$HOOK_SRC" "$HOOK_DST" "$ROOT/scripts/stylelint-guard.sh" "$ROOT/scripts/semantic-guard.sh"

echo "已安装 pre-commit：$HOOK_DST"
echo "提交含 Vue/CSS 的改动时会先跑 stylelint，失败则阻止提交。"
