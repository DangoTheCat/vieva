#!/usr/bin/env bash
# End-to-end API checks for student / lecturer / admin flows. Usage: bash e2e.sh [--expiry]
B=http://localhost:8080/api/v1
PASS=0; FAIL=0
ok(){ PASS=$((PASS+1)); echo "  PASS  $1"; }
ko(){ FAIL=$((FAIL+1)); echo "  FAIL  $1 -> $2"; }
# req METHOD TOKEN PATH [json]  -> sets $CODE and $BODY
req(){ local auth=(); [ -n "$2" ] && auth=(-H "Authorization: Bearer $2")
  local out; out=$(curl -s -w $'\n%{http_code}' -X "$1" "${auth[@]}" -H "Content-Type: application/json" "$B$3" ${4:+-d "$4"})
  CODE=${out##*$'\n'}; BODY=${out%$'\n'*}; }
expect(){ [ "$CODE" = "$1" ] && ok "$2" || ko "$2" "HTTP $CODE ${BODY:0:160}"; }
field(){ echo "$BODY" | grep -oE "\"$1\":\"[^\"]+\"" | head -1 | cut -d'"' -f4; }
login(){ req POST "" /auth/login "{\"email\":\"$1\",\"password\":\"${2:-password123}\"}"; field accessToken; }

echo "== AUTH"
A=$(login admin@aives.edu.vn); [ -n "$A" ] && ok "admin login" || ko "admin login" "$BODY"
L=$(login vancee@fpt.edu.vn); [ -n "$L" ] && ok "lecturer login" || ko "lecturer login" "$BODY"
S=$(login student@fpt.edu.vn); [ -n "$S" ] && ok "student login" || ko "student login" "$BODY"
req POST "" /auth/login '{"email":"student@fpt.edu.vn","password":"wrong-pass"}'; expect 401 "wrong password rejected"
req POST "" /auth/login '{"email":"SE170001","password":"password123"}'; expect 400 "non-email login rejected"

echo "== RBAC"
req GET "$S" /lecturer/subjects; expect 403 "student blocked from lecturer API"
req GET "$S" /admin/users; expect 403 "student blocked from admin API"
req GET "$L" /admin/users; expect 403 "lecturer blocked from admin API"
req GET "$L" /student/exams; expect 403 "lecturer blocked from student API"
req GET "$L" /lecturer/exams/; expect 404 "trailing slash -> 404 (not 500)"

echo "== ADMIN"
req GET "$A" /admin/stats; expect 200 "admin stats"
req GET "$A" /admin/subjects; expect 200 "admin subjects"
req GET "$A" /admin/speech; expect 200 "admin speech settings"
req GET "$A" /lecturer/subjects; expect 200 "admin lecturer view: subjects"
n=$(echo "$BODY" | grep -o '"subjectId"' | wc -l); [ "$n" -ge 1 ] && ok "admin sees $n subjects in lecturer view" || ko "admin sees subjects" "0"
EMAIL="qa.$(date +%s)@fpt.edu.vn"
req POST "$A" /admin/users "{\"email\":\"$EMAIL\",\"fullName\":\"QA Auto\",\"roleCode\":\"ROLE_STUDENT\"}"; expect 201 "admin create user"
U=$(field userId)
req POST "$A" /admin/users/$U/reset-password '{"newPassword":"Tmp9xKq27mPa"}'; expect 200 "admin reset password"
req POST "" /auth/login "{\"email\":\"$EMAIL\",\"password\":\"Tmp9xKq27mPa\"}"; expect 200 "new user logs in with temp password"
echo "$BODY" | grep -q '"mustChangePassword":true' && ok "mustChangePassword=true" || ko "mustChangePassword" "$BODY"
req DELETE "$A" /admin/users/$U; expect 200 "admin delete user"
req POST "" /auth/login "{\"email\":\"$EMAIL\",\"password\":\"Tmp9xKq27mPa\"}"; expect 401 "deleted user cannot log in"

echo "== LECTURER"
SUB=d0000000-0000-0000-0000-000000000001
req GET "$L" /lecturer/subjects; expect 200 "lecturer subjects"
req GET "$L" /lecturer/subjects/$SUB/topics; expect 200 "topics"
req GET "$L" /lecturer/subjects/$SUB/documents; expect 200 "documents"
req GET "$L" "/lecturer/question-versions?subjectId=$SUB&status=UNDER_REVIEW"; expect 200 "review queue UNDER_REVIEW filter"
req GET "$L" /lecturer/grading/courses; expect 200 "grading courses"
req GET "$L" /lecturer/scores; expect 200 "scores"
req GET "$L" "/knowledge?subjectId=$SUB&q=dependency&limit=2"; expect 200 "knowledge search"
# Submit-for-review on a fresh manual draft
# Submit-for-review → approve on the first existing DRAFT (e.g. an AI-generated one)
req GET "$L" "/lecturer/question-versions?subjectId=$SUB&status=DRAFT&size=1"
QID=$(field questionId); VID=$(field versionId); LV=$(echo "$BODY" | grep -oE '"lockVersion":[0-9]+' | head -1 | cut -d: -f2)
if [ -n "$VID" ]; then
  BL=$(field bloomLevel)
  req POST "$L" /lecturer/question-versions/$VID/confirm-bloom "{\"bloomLevel\":\"${BL:-UNDERSTAND}\",\"expectedVersion\":${LV:-0}}"; expect 200 "confirm Bloom level"
  echo "  (lockVersion before confirm: ${LV:-0}, after: $(echo "$BODY" | grep -oE '"lockVersion":[0-9]+' | head -1 | cut -d: -f2))"
  LV=$(echo "$BODY" | grep -oE '"lockVersion":[0-9]+' | head -1 | cut -d: -f2)
  req POST "$L" /lecturer/studio/questions/$QID/submit-for-review "{\"versionId\":\"$VID\",\"expectedVersion\":${LV:-0}}"
  [ "$CODE" = 200 ] && echo "$BODY" | grep -q UNDER_REVIEW && ok "submit for review -> UNDER_REVIEW" || ko "submit for review" "HTTP $CODE ${BODY:0:200}"
  LV=$(echo "$BODY" | grep -oE '"lockVersion":[0-9]+' | head -1 | cut -d: -f2)
  req POST "$L" /lecturer/question-versions/$VID/approve "{\"expectedVersion\":${LV:-0}}"
  [ "$CODE" = 200 ] && echo "$BODY" | grep -q APPROVED && ok "approve UNDER_REVIEW -> APPROVED" || ko "approve under review" "HTTP $CODE ${BODY:0:200}"
else echo "  SKIP  no DRAFT question to submit for review"; fi

echo "== STUDENT"
req GET "$S" /student/exams; expect 200 "student open exams"
req GET "$S" /student/scores; expect 200 "student scores"
req GET "$S" /student/speech; expect 200 "student speech settings"

if [ "$1" = "--expiry" ]; then
  echo "== EXAM EXPIRY (server clock)"
  req POST "$L" /lecturer/exams "{\"subjectId\":\"$SUB\",\"title\":\"QA expiry\",\"durationSeconds\":60}"; expect 201 "create 60s exam"
  E=$(field id)
  req PUT "$L" /lecturer/exams/$E/questions '{"questions":[{"questionId":"10000000-0000-0000-0000-000000000001","questionVersionId":"11000000-0000-0000-0000-000000000001","orderIndex":0}]}'; expect 200 "set exam questions"
  req PATCH "$L" /lecturer/exams/$E/status '{"status":"IN_PROGRESS"}'; expect 200 "open exam"
  req POST "$S" /student/exams/$E/enter; expect 200 "student enters exam"; AT=$(field attemptId)
  [ -z "$AT" ] && AT=$(field id)
  req POST "$S" /student/attempts/$AT/start '{"deviceReady":true}'; expect 200 "start attempt"
  sleep 5; req GET "$S" /student/attempts/$AT
  R=$(echo "$BODY" | grep -oE '"remainingSeconds":[0-9]+' | cut -d: -f2); [ "${R:-99}" -le 56 ] && ok "remainingSeconds counts down ($R)" || ko "countdown" "$R"
  echo "  ...waiting 100s for time-out + scheduler"; sleep 100
  req GET "$S" /student/attempts/$AT; ST=$(field status)
  case "$ST" in COMPLETED|FINALIZING|INTERRUPTED) ok "expired attempt finalized ($ST)";; *) ko "expired attempt finalized" "$ST";; esac
  req PATCH "$L" /lecturer/exams/$E/status '{"status":"COMPLETED"}'; expect 200 "close exam"
fi

echo; echo "RESULT: $PASS passed, $FAIL failed"
