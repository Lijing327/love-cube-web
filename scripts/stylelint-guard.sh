#!/usr/bin/env bash
# 提交前 Stylelint：只检查暂存/改动的 Vue 与平台 CSS
# 用法：
#   bash scripts/stylelint-guard.sh --git-hook

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT" || exit 1

collect_files() {
  git diff --cached --name-only --diff-filter=ACMR
  git diff --name-only --diff-filter=ACMR
}

FILES=$(collect_files | grep -E '^frontend/(src/.+\.vue|src/assets/styles/.+\.css)$' | sort -u || true)

if [ -z "$FILES" ]; then
  exit 0
fi

if [ ! -d "$ROOT/frontend/node_modules/stylelint" ]; then
  echo ""
  echo "❌ Stylelint 未安装 — 提交已阻止"
  echo "请先在 frontend 目录执行：npm install"
  echo ""
  exit 1
fi

REL_FILES=()
while IFS= read -r f; do
  [ -z "$f" ] && continue
  [ -f "$f" ] || continue
  REL_FILES+=("${f#frontend/}")
done <<< "$FILES"

if [ "${#REL_FILES[@]}" -eq 0 ]; then
  exit 0
fi

echo "stylelint: ${REL_FILES[*]}"
if (cd "$ROOT/frontend" && npx --no-install stylelint "${REL_FILES[@]}"); then
  exit 0
fi

echo ""
echo "❌ Stylelint 失败 — 提交已阻止"
echo "常见原因：同一个 @media 里重复写了同一个选择器（no-duplicate-selectors）"
echo "处理：把属性合并进已有规则，不要再开一个同名块"
echo "全量检查：cd frontend && npm run lint:style"
echo ""
exit 1
