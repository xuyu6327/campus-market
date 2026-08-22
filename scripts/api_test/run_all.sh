#!/bin/bash
# ============================================================
# run_all.sh - 一键运行全部 API 回归测试
# 用法：
#   bash scripts/api_test/run_all.sh          # 默认 8080
#   BASE=http://localhost:8081 bash scripts/api_test/run_all.sh   # 指定端口
# 依赖：后端运行中；测试会自动创建并清理临时数据
# ============================================================
cd "$(dirname "$0")"

FAILED_ANY=0
run() {
  echo
  echo "############################################################"
  echo "#  $1"
  echo "############################################################"
  bash "$1" || FAILED_ANY=1
}

run 01_smoke.sh
run 02_regression.sh
run 03_security.sh

echo
echo "============================================================"
if [ "$FAILED_ANY" -eq 0 ]; then
  echo "ALL TEST SUITES PASSED"
else
  echo "SOME TEST SUITE FAILED (see FAIL items above)"
fi
echo "============================================================"
exit $FAILED_ANY
