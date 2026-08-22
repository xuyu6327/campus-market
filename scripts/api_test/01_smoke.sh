#!/bin/bash
# ============================================================
# 01_smoke.sh - 全接口冒烟测试（只读，无副作用）
# 覆盖：匿名接口 / 用户端需登录接口 / 管理端全部只读接口
# ============================================================
source "$(dirname "$0")/common.sh"
health_check
echo "==== 01 SMOKE TEST ===="

# ---------- 匿名接口 ----------
echo "-- anonymous --"
assert "category list"   '"code":0' "$(api GET "/goods/category")"
assert "goods list"      '"records"' "$(api GET "/goods/list?pageNum=1&pageSize=2")"
assert "goods search"    '"code":0' "$(api GET "/goods/search?keyword=t")"
GID=$(api GET "/goods/list?pageNum=1&pageSize=1" | sed -E 's/.*"id":([0-9]+).*/\1/')
[ -n "$GID" ] && assert "goods detail (id=$GID)" '"code":0' "$(api GET "/goods/detail/$GID")"
assert "user public profile" '"code":0' "$(api GET "/user/1/profile")"

# ---------- 登录 ----------
ATOKEN=$(login_or_die "admin")
echo "-- admin authed --"
assert "login role=1" '"role":1' "$(api GET "/user/info" "$ATOKEN")"

# ---------- 用户端需登录接口 ----------
assert "user info"       '"nickname"' "$(api GET "/user/info" "$ATOKEN")"
assert "my goods"        '"records"' "$(api GET "/goods/my?pageNum=1&pageSize=2" "$ATOKEN")"
assert "my favorites"    '"code":0' "$(api GET "/goods/favorites?pageNum=1&pageSize=2" "$ATOKEN")"
assert "browse history"  '"code":0' "$(api GET "/goods/history?pageNum=1&pageSize=2" "$ATOKEN")"
assert "order buy"       '"records"' "$(api GET "/order/buy?pageNum=1&pageSize=2" "$ATOKEN")"
assert "order sell"      '"records"' "$(api GET "/order/sell?pageNum=1&pageSize=2" "$ATOKEN")"
assert "credit score"    '"code":0' "$(api GET "/user/credit" "$ATOKEN")"
assert "credit logs"     '"code":0' "$(api GET "/user/credit/logs" "$ATOKEN")"
assert "notifications"   '"records"' "$(api GET "/notification/list?pageNum=1&pageSize=2" "$ATOKEN")"
assert "unread count"    '"code":0' "$(api GET "/notification/unread-count" "$ATOKEN")"
assert "chat convs"      '"code":0' "$(api GET "/chat/conversations?pageNum=1&pageSize=2" "$ATOKEN")"
assert "chat unread"     '"code":0' "$(api GET "/chat/unread-count" "$ATOKEN")"

# ---------- 管理端只读接口 ----------
echo "-- admin module --"
assert "dashboard"              '"totalUsers"' "$(api GET "/admin/dashboard" "$ATOKEN")"
assert "user list"              '"records"' "$(api GET "/admin/user/list?pageNum=1&pageSize=2" "$ATOKEN")"
assert "user detail"            '"nickname"' "$(api GET "/admin/user/1" "$ATOKEN")"
assert "goods list"             '"records"' "$(api GET "/admin/goods/list?pageNum=1&pageSize=2" "$ATOKEN")"
assert "goods detail"           '"code":0' "$(api GET "/admin/goods/$GID" "$ATOKEN")"
assert "order list"             '"records"' "$(api GET "/admin/order/list?pageNum=1&pageSize=2" "$ATOKEN")"
assert "report list"            '"records"' "$(api GET "/admin/report/list?pageNum=1&pageSize=2" "$ATOKEN")"
assert "category list"          '"records"' "$(api GET "/admin/category/list?pageNum=1&pageSize=50" "$ATOKEN")"
assert "user list filter"       '"records"' "$(api GET "/admin/user/list?role=1&status=1" "$ATOKEN")"
assert "goods list filter"      '"records"' "$(api GET "/admin/goods/list?status=1" "$ATOKEN")"
assert "order list filter"      '"records"' "$(api GET "/admin/order/list?status=1" "$ATOKEN")"
assert "report list filter"     '"records"' "$(api GET "/admin/report/list?status=0" "$ATOKEN")"

summary
