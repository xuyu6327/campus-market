#!/bin/bash
# ============================================================
# common.sh - API 测试公共函数库（被其他测试脚本 source，不直接执行）
#
# 用法：在测试脚本开头 source "$(dirname "$0")/common.sh"
# 依赖：后端运行中（默认 http://localhost:8080，可用 BASE 环境变量覆盖）
#        mysql 客户端位于 D:/mysql-8.0.42-winx64/bin/mysql
# ============================================================

BASE=${BASE:-http://localhost:8080}
MYSQL="D:/mysql-8.0.42-winx64/bin/mysql"
MYSQL_ARGS="-uroot -p123456 --default-character-set=utf8mb4"
DB=campus_market

PASS=0
FAIL=0
FAILED_NAMES=()

# ---- 健康检查：后端未启动时直接报错退出 ----
health_check() {
  local code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/" 2>/dev/null)
  if [ "$code" != "200" ]; then
    echo "ERROR: backend not reachable at $BASE (http code: ${code:-none}). Start it first." >&2
    exit 1
  fi
}

# ---- 断言：实际输出包含期望子串 -> PASS，否则 FAIL（记录并继续）----
# 期望值以 ^ 开头时按正则匹配（grep -E），否则按固定子串匹配（grep -F）
# assert <测试名> <期望子串或正则> <实际输出>
assert() {
  local name="$1" expected="$2" actual="$3"
  local ok=1
  case "$expected" in
    ^*) echo "$actual" | grep -qE "$expected" || ok=0 ;;
    *)  echo "$actual" | grep -qF "$expected" || ok=0 ;;
  esac
  if [ "$ok" = "1" ]; then
    PASS=$((PASS + 1))
    echo "PASS  $name"
  else
    FAIL=$((FAIL + 1))
    FAILED_NAMES+=("$name")
    echo "FAIL  $name"
    echo "      expected to contain: $expected"
    echo "      actual:              $(echo "$actual" | head -c 200)"
  fi
}

# ---- 登录：<账号> [密码] -> 输出 token（失败输出空）----
login() {
  local account="$1" pass="${2:-admin123}"
  curl -s -X POST "$BASE/user/login" -H "Content-Type: application/json" \
    --data-binary "{\"account\":\"$account\",\"password\":\"$pass\"}" |
    sed -E 's/.*"token":"([^"]+)".*/\1/'
}

# ---- 登录并校验 token 有效，无效则报错退出 ----
login_or_die() {
  local account="$1" pass="${2:-admin123}"
  local t
  t=$(login "$account" "$pass")
  case "$t" in
    eyJ*) echo "$t" ;;
    *) echo "ERROR: login failed for account '$account'" >&2; exit 1 ;;
  esac
}

# ---- 通用 API 调用：<METHOD> <url> [token] [json-body] -> 输出响应体 ----
# 注意：body 一律写临时文件再 @file 发送——mingw64 的 curl.exe 是 Windows 原生程序，
# 中文参数经 MSYS2 会被转成 GBK（导致 Jackson 解析失败 500），文件方式可保证 UTF-8 字节原样发送
api() {
  local method="$1" url="$2" token="$3" body="$4"
  if [ -n "$body" ]; then
    local tmp
    tmp=$(mktemp)
    printf '%s' "$body" > "$tmp"
    if [ -n "$token" ]; then
      curl -s -X "$method" "$BASE$url" -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" --data-binary "@$tmp"
    else
      curl -s -X "$method" "$BASE$url" -H "Content-Type: application/json" --data-binary "@$tmp"
    fi
    rm -f "$tmp"
  else
    if [ -n "$token" ]; then
      curl -s -X "$method" "$BASE$url" -H "Authorization: Bearer $token"
    else
      curl -s -X "$method" "$BASE$url"
    fi
  fi
}

# ---- 数据库查询（单值/表输出）：<sql> ----
mysqlq() {
  "$MYSQL" $MYSQL_ARGS "$DB" -N -e "$1" 2>/dev/null
}

# ---- 注册临时测试用户 -> 输出新用户学号（登录用；失败输出空）----
# 注意：登录接口只支持 account=学号或手机号，不支持昵称
register_user() {
  local ts="$1"
  # 手机号/学号用随机数生成（时间戳前几位会在一段时间内相同，重复运行会撞号）
  # 纳秒 + RANDOM 共 12 位，取前 8 位，冲突概率可忽略
  local uniq="$(date +%N)${RANDOM}"
  local phone="139${uniq:0:8}"
  local sid="2026${uniq:0:8}"
  local resp
  resp=$(api POST /user/register "" "{\"phone\":\"$phone\",\"password\":\"admin123\",\"nickname\":\"t${ts}_${uniq:0:4}\",\"studentId\":\"$sid\",\"realName\":\"t\"}")
  echo "$resp" | grep -q '"code":0' && echo "$sid" || echo ""
}

# ---- 按学号查用户 id：<学号> -> 输出 id（失败输出空）----
uid_of() {
  mysqlq "SELECT id FROM sys_user WHERE student_id='$1' AND deleted=0;"
}

# ---- 发布测试商品：<seller_token> <ts> -> 输出商品 id ----
publish_goods() {
  local token="$1" ts="$2"
  local resp
  resp=$(api POST /goods "$token" \
    "{\"title\":\"t_${ts}\",\"description\":\"auto test\",\"categoryId\":3,\"price\":9.99,\"goodsCondition\":1,\"images\":[\"/uploads/probe.jpg\"],\"tradeLocation\":\"t\",\"contactMethod\":2,\"contactQq\":\"123456\"}")
  echo "$resp" | sed -E 's/.*"data":([0-9]+).*/\1/'
}

# ---- 清理工具（只删测试产生的数据）----
cleanup_user() {  # 软删测试用户
  local uid="$1"
  mysqlq "UPDATE sys_user SET deleted=1 WHERE id=$uid AND deleted=0;" >/dev/null
  mysqlq "DELETE FROM notification WHERE user_id=$uid;" >/dev/null
}
cleanup_goods() {  # 软删测试商品 + 相关通知（测试商品只被测试用户碰过）
  local gid="$1"
  mysqlq "UPDATE goods_info SET deleted=1 WHERE id=$gid AND deleted=0;" >/dev/null
  mysqlq "DELETE FROM notification WHERE related_id=$gid;" >/dev/null
}
cleanup_order() {  # 物理删除测试订单 + 关联信用分日志
  local oid="$1"
  mysqlq "DELETE FROM credit_log WHERE related_order_id=$oid;" >/dev/null
  mysqlq "DELETE FROM trade_order WHERE id=$oid;" >/dev/null
}
cleanup_credit_logs() {  # 按 reason 清理信用分测试日志
  mysqlq "DELETE FROM credit_log WHERE reason='auto_test';" >/dev/null
}

# ---- 汇总：输出 PASS/FAIL 统计，有失败则退出码 1 ----
summary() {
  echo
  echo "==================== RESULT ===================="
  echo "PASS: $PASS   FAIL: $FAIL"
  if [ "$FAIL" -gt 0 ]; then
    echo "Failed items:"
    printf '  - %s\n' "${FAILED_NAMES[@]}"
    exit 1
  fi
  echo "ALL PASSED"
  exit 0
}
