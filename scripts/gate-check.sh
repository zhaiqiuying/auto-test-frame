#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# Gate 证据检查脚本 — 确定性验证 OpenSpec change 的完成证据
#
# 功能：
#   1. 检查当前活跃 change 的 tasks.md 是否有未完成任务
#   2. 检查已标 [x] 的任务对应的构建验证步骤是否真的运行过
#   3. 检查 mvn -B clean test 是否通过
#   4. 检查禁止模式是否残留（printStackTrace / System.out / EMPTY_LIST 等）
#   5. 输出 Gate 报告
#
# 用法：
#   bash scripts/gate-check.sh [change-name]
#   不传 change-name 时自动检测 openspec/changes/ 下第一个非 archive 的 change
# ──────────────────────────────────────────────────────────────
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

fail()   { echo -e "${RED}❌ GATE FAILED: $1${NC}"; }
pass()   { echo -e "${GREEN}✅ $1${NC}"; }
warn()   { echo -e "${YELLOW}⚠️  $1${NC}"; }

GATE_FAIL=0

# ─── 确定 change ──────────────────────────────────────────────
CHANGE_NAME="${1:-}"
if [ -z "$CHANGE_NAME" ]; then
  for dir in openspec/changes/*/; do
    if [ -d "$dir" ] && [ "$(basename "$dir")" != "archive" ]; then
      CHANGE_NAME="$(basename "$dir")"
      break
    fi
  done
fi

if [ -z "$CHANGE_NAME" ]; then
  echo "No active change found in openspec/changes/"
  echo "Gate check only applies to active changes."
  exit 0
fi

TASKS_FILE="openspec/changes/${CHANGE_NAME}/tasks.md"
if [ ! -f "$TASKS_FILE" ]; then
  fail "tasks.md not found: $TASKS_FILE"
  exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  Gate Check — Change: ${CHANGE_NAME}"
echo "═══════════════════════════════════════════════════════════"
echo ""

# ─── 1. 检查 tasks.md 完成状态 ────────────────────────────────
echo "─── 1/5 Task Completion Status ───"
TOTAL=$(grep -cE '^\s*-\s*\[' "$TASKS_FILE" || true)
COMPLETED=$(grep -cE '^\s*-\s*\[x\]' "$TASKS_FILE" || true)
INCOMPLETE=$(grep -cE '^\s*-\s*\[\s*\]' "$TASKS_FILE" || true)

echo "  Total tasks:    $TOTAL"
echo "  Completed [x]:  $COMPLETED"
echo "  Incomplete [ ]: $INCOMPLETE"

if [ "$INCOMPLETE" -gt 0 ]; then
  fail "$INCOMPLETE tasks still incomplete"
  GATE_FAIL=1
else
  pass "All tasks completed"
fi
echo ""

# ─── 2. 构建验证步骤检查 ──────────────────────────────────────
echo "─── 2/5 Build Verification Tasks ───"
BUILD_TASKS=$(grep -iE 'mvn.*clean.*test|mvn.*compile|构建验证|build.*verify' "$TASKS_FILE" || true)
if [ -n "$BUILD_TASKS" ]; then
  echo "  Found build verification tasks in tasks.md"
  pass "Build verification tasks exist"
else
  warn "No explicit build verification task found in tasks.md"
fi
echo ""

# ─── 3. 实际构建验证 ──────────────────────────────────────────
echo "─── 3/5 Build & Test ───"
if mvn -B clean test 2>&1 | tail -20; then
  pass "mvn clean test passed"
else
  fail "mvn clean test FAILED"
  GATE_FAIL=1
fi
echo ""

# ─── 4. 禁止模式残留检查 ──────────────────────────────────────
echo "─── 4/5 Forbidden Pattern Residual Check ───"
FORBIDDEN_FOUND=0

check_pattern() {
  local pattern="$1"
  local description="$2"
  local result
  result=$(grep -rnE "$pattern" --include="*.java" \
    --exclude-dir=target --exclude-dir=test \
    btest/src/main itest/src/main 2>/dev/null || true)
  if [ -n "$result" ]; then
    fail "$description"
    echo "$result" | head -5
    FORBIDDEN_FOUND=1
  fi
}

check_pattern 'e\.printStackTrace\(\)' 'printStackTrace() residual'
check_pattern 'System\.out\.println' 'System.out.println residual'
check_pattern 'System\.err\.println' 'System.err.println residual'
check_pattern 'Collections\.EMPTY_LIST' 'Collections.EMPTY_LIST residual'
check_pattern 'Collections\.EMPTY_MAP' 'Collections.EMPTY_MAP residual'
check_pattern '@SuppressWarnings\("unchecked"\)' '@SuppressWarnings("unchecked") residual'

if [ "$FORBIDDEN_FOUND" -eq 0 ]; then
  pass "No forbidden patterns in main source"
else
  GATE_FAIL=1
fi
echo ""

# ─── 5. CI Profile 静态检查 ───────────────────────────────────
echo "─── 5/5 Static Analysis (CI Profile) ───"

echo "  Running SpotBugs (High)..."
if mvn -B -Pci -q spotbugs:check 2>&1; then
  pass "SpotBugs: no High severity issues"
else
  fail "SpotBugs: High severity issues found"
  GATE_FAIL=1
fi

echo "  Running PMD..."
if mvn -B -Pci -q pmd:check 2>&1; then
  pass "PMD: no violations"
else
  fail "PMD: violations found"
  GATE_FAIL=1
fi

echo "  Running Checkstyle..."
if mvn -B -Pci -q checkstyle:check 2>&1; then
  pass "Checkstyle: no violations"
else
  fail "Checkstyle: violations found"
  GATE_FAIL=1
fi
echo ""

# ─── Gate 结论 ────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════"
if [ "$GATE_FAIL" -eq 0 ]; then
  pass "GATE PASSED — Change '${CHANGE_NAME}' is ready to archive"
  echo "═══════════════════════════════════════════════════════════"
  exit 0
else
  fail "GATE FAILED — Fix issues above before archiving"
  echo "═══════════════════════════════════════════════════════════"
  exit 1
fi
