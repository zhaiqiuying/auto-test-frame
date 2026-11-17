#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# 安装 Git hooks — 将 scripts/ 下的 hook 脚本链接到 .git/hooks/
# ──────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GIT_HOOKS_DIR="$(git rev-parse --git-dir)/hooks"

mkdir -p "$GIT_HOOKS_DIR"

install_hook() {
  local name="$1"
  local src="$SCRIPT_DIR/$name"
  local dst="$GIT_HOOKS_DIR/$name"

  if [ ! -f "$src" ]; then
    echo "⚠️  Source not found: $src"
    return 1
  fi

  ln -sf "$src" "$dst"
  chmod +x "$src"
  echo "✅ Installed: $dst → $src"
}

echo "Installing Git hooks..."
install_hook "pre-commit"

echo ""
echo "Done. Hooks will run on every commit."
echo "To skip for a single commit: git commit --no-verify"
