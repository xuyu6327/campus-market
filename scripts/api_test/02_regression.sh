#!/bin/bash
# ============================================================
# 02_regression.sh - 历史高价值逻辑回归（会自动创建/清理测试数据）
# 覆盖：
#   A. 信用分联动：扣分冻结 / 加分解冻 / 冻结时启用被拒（CLAUDE.md 高价值修复）
#   B. 订单状态机：预订 -> 商品锁定 -> 买家取消 -> 商品恢复
#   C. 强制下架审核链路：下架 -> relist拦截 -> 编辑提交 -> 审核通过
# ============================================================
source "$(dirname "$0")/common.sh"
health_check
echo "==== 02 REGRESSION TEST ===="

TS=$(date +%s)
ATOKEN=$(login_or_die "admin")

# ================== A. 信用分冻结/解冻联动 ==================
echo "-- A. credit score freeze/unfreeze --"
ASID=$(register_user "1${TS}")   # 卖家 + 信用分测试用户（学号）
assert "A1 register temp user" '^[0-9]+$' "$ASID"
[ -n "$ASID" ] || { echo "ABORT: cannot register user"; exit 1; }
AUID=$(uid_of "$ASID")
STOKEN=$(login_or_die "$ASID")

assert "A2 credit -95 -> frozen" '"code":0' \
  "$(api PUT "/admin/user/$AUID/credit" "$ATOKEN" '{"changeValue":-95,"reason":"auto_test"}')"
A_STATUS=$(mysqlq "SELECT status FROM sys_user WHERE id=$AUID;")
assert "A3 user status=0 after freeze" "0" "$A_STATUS"

assert "A4 enable blocked when credit<10" '"code":400' \
  "$(api PUT "/admin/user/$AUID/enable" "$ATOKEN")"

assert "A5 credit +95 -> unfreeze" '"code":0' \
  "$(api PUT "/admin/user/$AUID/credit" "$ATOKEN" '{"changeValue":95,"reason":"auto_test"}')"
A_STATUS=$(mysqlq "SELECT status FROM sys_user WHERE id=$AUID;")
assert "A6 user status=1 after unfreeze" "1" "$A_STATUS"

# ================== B. 订单状态机 ==================
echo "-- B. order state machine --"
BSID=$(register_user "2${TS}")   # 买家
assert "B0 register buyer" '^[0-9]+$' "$BSID"
[ -n "$BSID" ] || { echo "ABORT: cannot register buyer"; exit 1; }
BUID=$(uid_of "$BSID")
BTOKEN=$(login_or_die "$BSID")

BGID=$(publish_goods "$STOKEN" "${TS}")
assert "B1 publish test goods" '^[0-9]+$' "$BGID"

BPHONE="138$(date +%N)${RANDOM}"
BPHONE="${BPHONE:0:11}"
BRESP=$(api POST /order "$BTOKEN" "{\"goodsId\":$BGID,\"buyerPhone\":\"$BPHONE\"}")
BOID=$(echo "$BRESP" | sed -E 's/.*"data":([0-9]+).*/\1/')
assert "B2 book order" '^[0-9]+$' "$BOID"

O_STATUS=$(mysqlq "SELECT status FROM trade_order WHERE id=$BOID;")
assert "B3 order status=0 pending" "0" "$O_STATUS"
G_STATUS=$(mysqlq "SELECT status FROM goods_info WHERE id=$BGID;")
assert "B4 goods status=2 reserved" "2" "$G_STATUS"

assert "B5 buyer cancel" '"code":0' "$(api PUT "/order/$BOID/buyer-cancel" "$BTOKEN")"
G_STATUS=$(mysqlq "SELECT status FROM goods_info WHERE id=$BGID;")
assert "B6 goods back to on-sale" "1" "$G_STATUS"
O_STATUS=$(mysqlq "SELECT status FROM trade_order WHERE id=$BOID;")
assert "B7 order status=2 buyer-cancelled" "2" "$O_STATUS"

# ================== C. 强制下架审核链路 ==================
echo "-- C. force takedown review flow --"
CGID=$(publish_goods "$STOKEN" "2${TS}")
assert "C1 publish goods for review" '^[0-9]+$' "$CGID"

assert "C2 admin force takedown" '"code":0' \
  "$(api PUT "/admin/goods/$CGID/takedown" "$ATOKEN" '{"reason":"auto test"}')"
G_STATUS=$(mysqlq "SELECT takedown_by FROM goods_info WHERE id=$CGID;")
assert "C3 takedown_by=1 marked" "1" "$G_STATUS"

assert "C4 relist blocked" '商品已被管理员下架' \
  "$(api PUT "/goods/$CGID/relist" "$STOKEN")"

assert "C5 edit + apply review" '"code":0' \
  "$(api PUT "/goods/$CGID" "$STOKEN" \
    "{\"title\":\"t_${TS}_v2\",\"description\":\"e\",\"categoryId\":3,\"price\":8.88,\"goodsCondition\":1,\"images\":[\"/uploads/probe.jpg\"],\"tradeLocation\":\"t\",\"contactMethod\":2,\"contactQq\":\"1\"}")"
G_STATUS=$(mysqlq "SELECT status FROM goods_info WHERE id=$CGID;")
assert "C6 status=4 pending review" "4" "$G_STATUS"

assert "C7 admin approve" '"code":0' \
  "$(api PUT "/admin/goods/$CGID/review" "$ATOKEN" '{"approve":true,"reason":"ok"}')"
G_STATUS=$(mysqlq "SELECT CONCAT(status,',',takedown_by) FROM goods_info WHERE id=$CGID;")
assert "C8 approved: status=1 takedown_by=0" "1,0" "$G_STATUS"

# ================== 清理 ==================
echo "-- cleanup --"
cleanup_order "$BOID"
cleanup_goods "$BGID"
cleanup_goods "$CGID"
cleanup_user "$AUID"
cleanup_user "$BUID"
cleanup_credit_logs
echo "cleanup done"

summary
