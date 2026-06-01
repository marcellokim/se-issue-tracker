# Swing Full QA Report - 2026-06-01

## Run Metadata

- Date: 2026-06-01 KST
- Last updated: 2026-06-02 KST
- Scope: Swing UI only
- Parent issue: #24
- QA issues: #246, #248
- Execution mode: WSL2 continuation completed for DB-backed Swing smoke, core mutation flows, and component-level cross-cutting interaction flows; OS-level click/focus remains unverified
- Run mode: macOS startup/admin smoke, then WSL2 WSLg/offscreen Swing render and same-JVM interaction capture
- Source snapshot type: `origin/dev` baseline plus Swing QA branch fixes
- Target source: `origin/dev@e82afe057280`; QA branch `test/248-swing-full-qa` with Swing focus fallback, issue-detail title layout, button/table polish, and regenerated QA evidence
- Packaged app target: `IssueTrackerQA` local app image for startup/admin smoke
- Stop gate status: WSL2 offscreen evidence accepted for Swing rendering; OS-level WSLg screenshot automation unavailable in this environment
- Local DB: Oracle Free `FREEPDB1`
- Screenshot policy: generated `WSL-SW-QA-*.png` files are local QA evidence and are intentionally ignored by `.gitignore`; the PR should include code, tests, and this report, not generated WSL screenshot binaries

## Environment Evidence

| Check | Result | Evidence |
| --- | --- | --- |
| `git fetch origin dev --prune` | PASS | Fetched `origin/dev`; `FETCH_HEAD` updated |
| `git status --short --branch` | PASS | QA branch is based on latest `origin/dev` before PR creation; generated screenshot binaries remain ignored |
| `gh pr list` / open Swing PR REST query | PASS | Open PR list returned `[]`; open Swing PR REST query returned no rows |
| `./gradlew check verifySubmissionMetadata --console=plain` | PASS | Baseline gate passed before report creation: `BUILD SUCCESSFUL in 33s`; `oracleIntegrationTest` skipped |
| `./gradlew oracleLocalStatus --console=plain` | BLOCKED then resolved | Initial Docker CLI available, but Colima Docker socket missing; fixed by `colima start` |
| `colima start` | PASS | Docker context switched to `colima`; provisioning completed |
| `./gradlew oracleLocalInitializeDatabase --console=plain` | PASS | Local Oracle running; schema executed 33 blocks; app data reused |
| Missing Oracle env startup | PASS | Startup failure window shown with missing `ITS_DB_URL`, `ITS_DB_USER`, `ITS_DB_PASSWORD` message |
| QA packaged Swing app launch | PASS | `jpackage` app image `IssueTrackerQA` created and launched with Oracle env |
| WSL2 source refresh | PASS | WSL2 run started from `origin/dev@816117349a1f`; PR branch was later refreshed to `origin/dev@e82afe057280` before publication |
| WSLg availability | PASS | `DISPLAY=:0`, `WAYLAND_DISPLAY=wayland-0`, `/mnt/wslg` and `/tmp/.X11-unix/X0` present |
| WSL GUI automation tools | BLOCKED | `xdotool`, `wmctrl`, `scrot`, `import`, `gnome-screenshot`, `xwd`, `xprop`, `xwininfo`, `xdpyinfo` unavailable; `sudo` requires password |
| WSL Docker/Oracle reset | PASS | Docker Desktop started from Windows side; `/tmp/docker-wrapper/docker` bridged Gradle scripts to `docker.exe`; `oracleLocalResetFixedSeed` completed |
| WSL Oracle connection | PASS | `oracleConnectionCheck` connected to Oracle Free `FREEPDB1` as `ITS_USER` |
| WSL OS screenshot capture | BLOCKED | Java Robot and Windows desktop capture produced black PNGs; those files were discarded and are not used as evidence |
| WSL Swing offscreen capture | PASS | Same-JVM Swing `BufferedImage`/`printAll` captures were regenerated after local focus/style fixes and produced valid rendered evidence for login, admin, PL, Dev, Tester screens |
| WSL Swing interaction capture | PASS | Same-JVM DB-backed Swing harness executed account, project, issue workflow, deleted issue, statistics, navigation, double-click duplicate-submit, cancel/window-close, and minimum-size visual flows against Oracle after fixed-seed reset |
| WSL Robot click/focus capture | BLOCKED | Re-run after focus fallback still failed: `java.awt.Robot` coordinate click on `loginIdField` did not give focus; focus owner stayed `<none>`, dispatched mouse event also did not focus, and `requestFocusInWindow()` returned `false` in this WSLg session |
| WSL minimal Robot focus probe | BLOCKED | A minimal standalone `JFrame` + `JTextField` also failed to receive focus after Robot coordinate click with focus owner `<none>`, so this environment cannot prove native Swing click focus |

## Seed Accounts

| Role | Login ID | Password | Result |
| --- | --- | --- | --- |
| Admin | `admin` | `DemoLocalAdmin!` | PASS |
| PL | `pl1` | `DemoLocalPl1!` | PASS |
| Dev | `dev1` | `DemoLocalDev1!` | PASS |
| Tester | `tester1` | `DemoLocalTester1!` | PASS |

## QA Case Results

| ID | Role | Screen | Data State | Risk Type | Result | Evidence | Defect |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SW-QA-ENV-001 | System | Startup | DB unavailable and DB available | environment | PASS | Missing-env failure screen and DB-backed admin dashboard verified |  |
| SW-QA-AUTH-001 | Admin/PL/Dev/Tester | Login | valid and invalid credentials | session/focus | PARTIAL | Login screen, invalid credential message, admin dashboard, PL/Dev/Tester project list captures; focus fallback was added, but OS-level mouse click focus was still not proven in WSLg Robot run | SW-QA-GAP-002 |
| SW-QA-ADMIN-001 | Admin | Dashboard/account/project | normal seed + QA account/project | route/refresh/mutation | PASS | Admin dashboard, account create/duplicate/rename/role/deactivate/activate, project create/rename/description/member add-remove/failure captures |  |
| SW-QA-PL-001 | PL | Issue list/detail/deleted management/statistics | Project A seed + QA issue | workflow/search/register/edit/priority/assignment/comment/dependency/delete/statistics | PASS | PL issue register/detail/edit/priority/assign/comment/dependency, close/reopen, deleted issue restore/purge, statistics full/filter/invalid-range captures; issue id now renders as secondary metadata |  |
| SW-QA-DEV-001 | Dev | Issue detail | QA assigned issue | status/comment/permission | PASS | Dev mark fixed, own comment add/edit/delete, and post-reject mark fixed captures |  |
| SW-QA-TESTER-001 | Tester | Issue detail | QA fixed issue | resolve/reject/permission | PASS | Tester reject-to-assigned and resolve-to-resolved captures |  |
| SW-QA-CROSS-001 | All | Navigation/dialogs | back/deleted navigation/logout visibility | worker/navigation | PASS component-level | Admin/PL/Dev/Tester back and logout paths, role switching, duplicate-submit double-clicks, and dialog cancel/window-close paths executed with WSL offscreen rendered evidence; native mouse/keyboard interaction not covered | SW-QA-GAP-002 |
| SW-QA-VISUAL-001 | All | Main screens | long text/1280x900 and 800x600 windows | visual | PARTIAL | Main tables, errors, disabled states, action groups, dialog forms, and long Korean/English text render; issue id no longer pushes the title out of the header; button/table chrome was improved, but final presentation polish still needs a target-desktop pass | SW-QA-DEF-002 |
| SW-QA-ARCH-001 | Code | Swing package | policy boundary | architecture | PASS | `rg` checks found no production Swing persistence/JDBC dependency; enum display and `availableActions` rendering are UI concerns; `not configured` guards are composition/test guard paths |  |

## Capture And Interaction Log

| ID | App Target | Screen | Action | Observation | Screenshot |
| --- | --- | --- | --- | --- | --- |
| MAC-STARTUP-FAIL | `Main` / `net.java.openjdk.java` | Startup failure | Desktop app inspection | Java process was visible, but native app-state capture could not inspect the Swing window reliably; screenshot fallback used | `docs/qa/artifacts/swing-2026-06-01/SW-QA-ENV-001-startup-failure.png` |
| MAC-PACKAGED-APP | `IssueTrackerQA` / `com.github.marcellokim.issuetracker.qa2` | Login | `jpackage` app image launch | Packaged app fixed display name and bundle id for manual and accessibility-assisted smoke checks |  |
| MAC-AUTH-ADMIN | `IssueTrackerQA` | Admin dashboard | Accessibility-assisted text input and `Return` | `admin` authenticated and dashboard loaded with project/user tables | `docs/qa/artifacts/swing-2026-06-01/SW-QA-AUTH-001-admin-dashboard.png` |
| WSL-GUI-TOOLS | WSLg/X11 | Automation preflight | `command -v` checks | WSLg was available, but common X11 screenshot/input tools were not installed |  |
| WSL-SCREENSHOT | WSLg/Windows desktop | Login | Java Robot and Windows desktop capture attempts | Both routes produced black captures, so they were discarded as invalid evidence |  |
| WSL-OFFSCREEN | Swing JVM | Role smoke | same-JVM `SwingAppFrame` + `BufferedImage` `printAll` | Captured DB-backed rendered states for invalid login, admin, PL, Dev, and Tester | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-001-login-offscreen.png` |
| WSL-MUTATION | Swing JVM | Core workflows | same-JVM Swing component clicks + scripted prompts | Executed account/project/admin mutations and PL/Dev/Tester issue workflow mutations against Oracle | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-025-statistics-invalid-range-offscreen.png` |
| WSL-CROSS | Swing JVM | Cross-cutting workflows | same-JVM Swing component clicks + real dialog captures | Executed back/re-enter/logout, duplicate-submit double-click, cancel/window-close, and 800x600 long-text visual cases | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-045-minimum-long-issue-detail-offscreen.png` |
| WSL-ROBOT-FOCUS | Swing JVM on WSLg | Login | `java.awt.Robot` coordinate click and key typing probe | Login ID field did not receive focus after native coordinate click even after focus fallback; focus owner stayed `<none>` and requestFocusInWindow returned `false`, so WSLg Robot cannot prove manual click behavior | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ROBOT-002-login-id-click-no-focus.png` |

## Test Data Ledger

| Type | Identifier | Created By | Used In | Cleanup |
| --- | --- | --- | --- | --- |
| Account | `qauser1` -> `QA User Renamed`, final role `TESTER`, active `Yes` | Admin | SW-QA-ADMIN-001 | Local QA DB; reset by `oracleLocalResetFixedSeed` |
| Project | `QA Project 2026` -> `QA Project Renamed`, description `QA project updated description` | Admin | SW-QA-ADMIN-001 | Local QA DB; reset by `oracleLocalResetFixedSeed` |
| Project member | `dev1` added to and removed from `QA Project Renamed`; active removal from Project A rejected | Admin | SW-QA-ADMIN-001 | Local QA DB; reset by `oracleLocalResetFixedSeed` |
| Issue | `QA PL issue 2026-06-01` -> `QA PL issue 2026-06-01 updated`, issue id visible as generated `ISSUE-*` | PL | SW-QA-PL-001, SW-QA-DEV-001, SW-QA-TESTER-001 | Preserved in final local QA DB as `REOPENED`; reset by `oracleLocalResetFixedSeed` |
| Comment/history | `QA PL comment`, `QA fixed`, `QA reject`, `QA fixed after reject`, `QA resolved`, `QA close`, `QA reopen` | PL/Dev/Tester | Workflow status and comment evidence | Local QA DB; reset by `oracleLocalResetFixedSeed` |
| Comment | `QA dev own comment` -> `QA dev own comment edited` -> deleted | Dev | SW-QA-DEV-001 | Local QA DB; reset by `oracleLocalResetFixedSeed` |
| Dependency | Blocking issue id `3` added to and removed from the QA issue | PL | SW-QA-PL-001 | Removed during QA run |
| Deleted issue | Seed issue `Statistics chart label overflow` restored, deleted again, then purged | PL | SW-QA-PL-001 | Purged during QA run |
| Cross-click issue | `QA double action 2026-06-01` -> `QA double action updated once 2026-06-01` | PL/Dev | SW-QA-CROSS-001 | Local QA DB; reset by `oracleLocalResetFixedSeed` |
| Long visual issue | Korean/English long title and comment data | PL | SW-QA-VISUAL-001 | Local QA DB; reset by `oracleLocalResetFixedSeed` |

## Workflow State Coverage

| State | Required UI Check | Result | Evidence |
| --- | --- | --- | --- |
| NEW | PL can edit, change priority, assign, comment, and manage dependency when allowed | PASS | `WSL-SW-QA-PL-011-new-detail-offscreen.png`, `WSL-SW-QA-PL-012-issue-edited-offscreen.png`, `WSL-SW-QA-PL-013-priority-changed-offscreen.png`, `WSL-SW-QA-PL-014-assigned-offscreen.png` |
| ASSIGNED | Dev can mark fixed; PL can see assignment actions | PASS | `WSL-SW-QA-PL-014-assigned-offscreen.png`, `WSL-SW-QA-DEV-010-mark-fixed-offscreen.png` |
| FIXED | Tester can reject and resolve | PASS | `WSL-SW-QA-TESTER-011-rejected-offscreen.png`, `WSL-SW-QA-TESTER-010-resolved-offscreen.png` |
| RESOLVED | PL can close where allowed | PASS | `WSL-SW-QA-PL-018-closed-offscreen.png` |
| CLOSED | PL can reopen/delete where allowed | PASS | `WSL-SW-QA-PL-018-closed-offscreen.png`, `WSL-SW-QA-PL-020-soft-deleted-offscreen.png` |
| REOPENED | PL can see assignment/edit actions again | PASS | `WSL-SW-QA-PL-019-reopened-offscreen.png` |
| DELETED | PL can restore and purge from deleted management | PASS | `WSL-SW-QA-PL-020-soft-deleted-offscreen.png`, `WSL-SW-QA-PL-021-restored-offscreen.png`, `WSL-SW-QA-PL-022-purged-offscreen.png` |

## Phase Gates

| Phase | Status | Decision |
| --- | --- | --- |
| Phase 1 - preflight/startup | PASS | Continue with role smoke on DB-backed packaged Swing app |
| Phase 2 - role smoke | PASS component-level / PARTIAL OS-level | Admin, PL, Dev, Tester valid login and invalid login failure verified by component/AX automation; native mouse click focus remains unproven after focus fallback |
| Phase 3 - workflow depth | PASS | Admin account/project mutation and PL/Dev/Tester issue mutation flows executed against Oracle |
| Phase 4 - cross-cutting and visual | PASS component-level / PARTIAL OS-level | Navigation, role switching, duplicate-submit double-click handling, dialog cancel/window-close, disabled states, and 800x600 long-text render verified through component-level/offscreen evidence; issue id/title layout and button/table chrome improved; native mouse/keyboard and final visual polish still need target desktop pass |
| Phase 5 - architecture and routing | PASS | Swing package boundary checks completed by `rg` spot checks and production path review |

## Visual QA Notes

| Area | Finding | Evidence | Follow-up |
| --- | --- | --- | --- |
| Login mouse focus | Focus fallback was added for mouse press/release, window open, and window activation, but the current WSLg run still cannot prove native click-to-focus; `Robot` coordinate click left focus owner as `<none>`. A minimal Swing `JTextField` probe failed the same way, so this is a target-desktop retest gate rather than a completed pass. | `WSL-SW-QA-ROBOT-002-login-id-click-no-focus.png` | Re-run on the presentation target OS with manual mouse capture or a working OS automation tool |
| Issue detail title | Fixed in this QA pass: human-readable title renders first, and generated issue id moved to a secondary metadata line. Very long titles still ellipsize at 800px, which is acceptable if the title starts visible. | `WSL-SW-QA-PL-011-new-detail-offscreen.png`, `WSL-SW-QA-PL-012-issue-edited-offscreen.png`, `WSL-SW-QA-CROSS-045-minimum-long-issue-detail-offscreen.png` | No further blocker; optional tooltip/manual copy affordance can be added later |
| Native Swing polish | Button gradients and table headers were improved in this branch; screens are now cleaner but still presentation-utilitarian, with dense tables and large blank panels on some views. | `WSL-SW-QA-AUTH-003-admin-dashboard-offscreen.png`, `WSL-SW-QA-PL-001-issue-list-offscreen.png`, `WSL-SW-QA-PL-025-statistics-invalid-range-offscreen.png` | Target-desktop pass should confirm spacing hierarchy, selected/focus states, and disabled-action affordance |
| Dialogs | Dialogs are usable and cancel/window-close paths work, but forms inherit native default styling and are visually detached from the main screen language. | `WSL-SW-QA-CROSS-020-account-create-cancel-dialog.png`, `WSL-SW-QA-CROSS-035-edit-cancel-dialog.png` | Accept for functional submission or wrap key dialogs in project-styled panels if time allows |

## Defects

| ID | Severity | Title | Linked PR/Issue | Status |
| --- | --- | --- | --- | --- |
| SW-QA-DEF-001 | Medium | Issue detail header displayed long issue hash first, causing the human-readable title to be truncated at 1280x900 |  | Fixed in local QA branch |
| SW-QA-DEF-002 | Low | Swing screens still need final presentation polish review for dense tables, disabled-action rows, and large empty areas |  | Improved in local QA branch; residual polish open |
| SW-QA-GAP-001 | Low | WSLg OS-level screenshot automation unavailable without additional X11 capture tools or manual desktop capture support |  | Documented |
| SW-QA-GAP-002 | Medium | Native mouse click and keyboard typing cannot be claimed complete from current WSLg run; Robot click on login ID field did not focus the window/component after local focus fallback |  | Documented; target desktop retest required |

## Target Desktop Retest Gate

This QA run does not close native mouse/keyboard behavior because WSLg `Robot` also fails on a minimal standalone Swing `JTextField`. Before release or demo, run the following on the actual presentation desktop.

| ID | Required Check | Expected Result | Evidence |
| --- | --- | --- | --- |
| TDT-FOCUS-001 | Open Swing app, click directly inside `ID`, type `admin` | First click places caret in `ID`; typed text appears in `ID` without using Tab | Screen recording or screenshot sequence |
| TDT-FOCUS-002 | Click directly inside `Password`, type `DemoLocalAdmin!` | First click places caret in `Password`; password field receives input | Screen recording or screenshot sequence |
| TDT-FOCUS-003 | Click `Sign in` with mouse | Admin dashboard opens without pressing Enter | Dashboard screenshot |
| TDT-FOCUS-004 | Repeat invalid login with mouse-only field selection and button click | Error message is visible and password clears | Login error screenshot |
| TDT-VISUAL-001 | Inspect login, admin dashboard, PL issue list/detail, statistics at default window size | No overlap, clipped primary text, unreachable button, or unreadable disabled state | Screenshots |
| TDT-VISUAL-002 | Resize to the documented minimum `800x600` and inspect long title/detail case | Primary title remains visible; scrolling works where content exceeds height | Screenshot |

Gate decision: any failed `TDT-FOCUS-*` item is release-blocking for Swing. `TDT-VISUAL-*` failures are blocking only when text is unreadable, controls are clipped, or the normal workflow becomes unreachable.

## Command Log

```text
$ git status --short --branch
## test/248-swing-full-qa...origin/dev

$ git rev-parse --short=12 origin/dev
e82afe057280

$ gh pr list --repo marcellokim/se-issue-tracker --state open --limit 100 --json number,title,isDraft,headRefName,baseRefName,labels,updatedAt
[]

$ ./gradlew check verifySubmissionMetadata --console=plain
BUILD SUCCESSFUL in 33s
oracleIntegrationTest SKIPPED

$ git fetch origin dev --prune && git rev-parse --short=12 origin/dev
e82afe057280

$ gh api --method GET repos/marcellokim/se-issue-tracker/pulls --paginate -f state=open -f per_page=100 --jq '.[] | select(([.labels[].name] | index("area:ui")) or (.title | test("Swing"))) | {number,title,draft,head:.head.ref,base:.base.ref,sha:.head.sha}'
<no rows>

$ rg -n "admin|pl1|dev1|tester1|DemoLocal" docs/oracleDB-seed-password.md
16:| `admin` | `DemoLocalAdmin!` |
17:| `pl1` | `DemoLocalPl1!` |
19:| `dev1` ~ `dev10` | `DemoLocalDev1!` ~ `DemoLocalDev10!` |
20:| `tester1` ~ `tester5` | `DemoLocalTester1!` ~ `DemoLocalTester5!` |

$ ./gradlew oracleLocalStatus --console=plain
BUILD SUCCESSFUL in 541ms
Docker CLI: available
Docker Compose: available
JDBC URL: jdbc:oracle:thin:@//localhost:1521/FREEPDB1
failed to connect to the docker API at unix:///Users/ydmac/.colima/default/docker.sock
container state: not created

$ colima start
Current context is now "colima"
done

$ ./gradlew oracleLocalInitializeDatabase --console=plain
Executed 33 blocks from db/oracle/schema-oracle.sql
Oracle application data reused without seed reset.
Oracle application database check completed.
BUILD SUCCESSFUL in 1m 3s

$ jpackage --type app-image --name IssueTrackerQA ...
<app image created at /Users/ydmac/Applications/IssueTrackerQA.app>

$ accessibility-assisted login as admin
windows=1
titles=Admin dashboard
Admin (ADMIN)

$ git fetch origin dev test/248-swing-full-qa --prune && git rev-parse --short=12 origin/dev && git rev-parse --short=12 HEAD && git status --short --branch
816117349a1f
cf6cf5e3059e
## test/248-swing-full-qa...origin/test/248-swing-full-qa

$ printf 'DISPLAY=%s\nWAYLAND_DISPLAY=%s\n' "$DISPLAY" "$WAYLAND_DISPLAY"; ls -ld /mnt/wslg /tmp/.X11-unix/X0
DISPLAY=:0
WAYLAND_DISPLAY=wayland-0
/mnt/wslg present
/tmp/.X11-unix/X0 present

$ for c in xdotool wmctrl scrot import gnome-screenshot xwd xprop xwininfo xdpyinfo; do command -v "$c" || echo "$c=missing"; done
xdotool=missing
wmctrl=missing
scrot=missing
import=missing
gnome-screenshot=missing
xwd=missing
xprop=missing
xwininfo=missing
xdpyinfo=missing

$ PATH="/tmp/docker-wrapper:$PATH" ./gradlew oracleLocalResetFixedSeed --console=plain
Oracle schema reset and fixed seed completed.
BUILD SUCCESSFUL in 3m 8s

$ ITS_DB_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1' ITS_DB_USER='ITS_USER' ITS_DB_PASSWORD='ItsLocalDev2026!' PATH="/tmp/docker-wrapper:$PATH" ./gradlew oracleConnectionCheck --console=plain
Oracle Database connection succeeded.
ITS Oracle connection OK.
BUILD SUCCESSFUL in 1s

$ java -cp "/tmp/swing-qa-tools/classes:<runtime-classpath>" com.github.marcellokim.issuetracker.ui.swing.SwingQaCapture docs/qa/artifacts/swing-2026-06-01
PASS invalid-login message=Invalid ID or password.
PASS admin-dashboard projects=2
PASS admin-project-management projects=2
PASS admin-account-management users=18
PASS pl-project-list projects=1
PASS pl-issue-list issues=14 message=14 issues
PASS pl-deleted-issues rows=0
PASS pl-issue-detail title=[7b12464fb13eefd341275d986489cc68be1551da56ad3b5f8afbe72879e35efc] Assignment notification not shown
PASS dev-project-list projects=1
PASS dev-issue-list issues=1 message=1 issues
PASS dev-issue-detail title=[2fea689c8d29ec9bf36c8eb73a69064a045aac1bb54e3eaa7ced9cd7d11d9c25] Dependency resolution flow blocked
PASS tester-project-list projects=1
PASS tester-issue-list issues=6 message=6 issues
PASS tester-issue-detail title=[26f1ad37d100a4f6215564b8a7ae2d2fe328e2186fd95b5c664a0904650f27b5] Session timeout warning absent

$ ./gradlew test --tests 'com.github.marcellokim.issuetracker.ui.swing.*' --console=plain
BUILD SUCCESSFUL in 18s

$ ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1" ITS_DB_USER="ITS_USER" ITS_DB_PASSWORD="ItsLocalDev2026!" ./gradlew oracleLocalResetFixedSeed --console=plain
Oracle schema reset and fixed seed completed.
BUILD SUCCESSFUL in 1m 10s

$ java -cp "/tmp/swing-qa-tools/classes:<runtime-classpath>" com.github.marcellokim.issuetracker.ui.swing.SwingQaCapture docs/qa/artifacts/swing-2026-06-01
PASS invalid-login message=Invalid ID or password.
PASS admin-dashboard projects=2
PASS admin-project-management projects=2
PASS admin-account-management users=18
PASS pl-project-list projects=1
PASS pl-issue-list issues=14 message=14 issues
PASS pl-deleted-issues rows=0
PASS pl-issue-detail title=Assignment notification not shown
PASS dev-project-list projects=1
PASS dev-issue-list issues=1 message=1 issues
PASS dev-issue-detail title=Dependency resolution flow blocked
PASS tester-project-list projects=1
PASS tester-issue-list issues=6 message=6 issues

$ ITS_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1" ITS_DB_USER="ITS_USER" ITS_DB_PASSWORD="ItsLocalDev2026!" ./gradlew oracleLocalResetFixedSeed --console=plain
Oracle schema reset and fixed seed completed.
BUILD SUCCESSFUL in 1m 10s

$ java -cp "/tmp/swing-qa-tools/classes:<runtime-classpath>" com.github.marcellokim.issuetracker.ui.swing.SwingQaInteractionCapture docs/qa/artifacts/swing-2026-06-01
PASS admin-account-mutations qauser1 create/duplicate/rename/role/deactivate/activate
PASS admin-project-mutations project create/rename/description/member add-remove/failure
PASS pl-statistics full/filter/invalid-range
PASS issue-workflow-mutations qaIssueId=31
PASS cross-navigation back/re-enter/logout/role-switch
PASS cross-duplicate-submit login/search/register/action buttons
PASS cross-dialog-cancel-window-close
PASS cross-minimum-visual long Korean/English issue rendered at 800x600
PASS cross-cutting navigation/double-click/cancel/minimum-visual
PASS architecture-spot-checks deferred-to-command-log

$ rg -n "repository|persistence|jdbc|DataSource|Connection|PreparedStatement" src/main/java/com/github/marcellokim/issuetracker/ui/swing src/test/java/com/github/marcellokim/issuetracker/ui/swing
Production Swing package: no matches. Test package: repository imports only for fixture/controller construction.

$ rg -n "Role\\.|IssueStatus\\.|Priority\\.|can[A-Z]|availableActions|user\\.role\\(\\)" src/main/java/com/github/marcellokim/issuetracker/ui/swing
Matches are display labels, enum selectors, presenter/controller-derived `canRegisterIssue`, comment action state, and `availableActions`-driven button rendering.

$ rg -n "PlaceholderPanel|showPlaceholder|Issue action|not configured|UnsupportedOperationException" src/main/java/com/github/marcellokim/issuetracker/ui/swing src/test/java/com/github/marcellokim/issuetracker/ui/swing
No placeholder routes. `not configured` matches are constructor/composition guard paths for assignment, statistics, deleted issue, and issue state controllers.

$ ./gradlew test --tests 'com.github.marcellokim.issuetracker.ui.swing.*' --console=plain
BUILD SUCCESSFUL in 14s

$ ./gradlew check verifySubmissionMetadata --console=plain
BUILD SUCCESSFUL in 24s

$ git diff --check
git diff --check produced no output.

$ java -cp "/tmp/swing-qa-tools/classes:<runtime-classpath>" com.github.marcellokim.issuetracker.ui.swing.SwingQaRobotFocusCapture docs/qa/artifacts/swing-2026-06-01
FAIL robot-click-focus loginId focusOwner=<none>
FAIL dispatched-click-focus loginId focusOwner=<none>
INFO requestFocusInWindow loginId=false focusOwner=<none>
Exception in thread "main" java.lang.IllegalStateException: Timed out waiting for Swing state

$ javac /tmp/swing-focus-probe/MinimalFocusProbe.java && java -cp /tmp/swing-focus-probe MinimalFocusProbe
FAIL minimal JTextField focus owner=<none>
```

## Local Screenshot Index

The `WSL-SW-QA-*.png` entries below document locally generated evidence from this QA run. They are not intended to be committed; reproduce them from the command log when fresh visual evidence is needed.

| Screenshot | Scenario | Path |
| --- | --- | --- |
| SW-QA-ENV-001-startup-failure.png | Missing Oracle env startup failure | `docs/qa/artifacts/swing-2026-06-01/SW-QA-ENV-001-startup-failure.png` |
| SW-QA-AUTH-001-admin-dashboard.png | Admin valid login dashboard | `docs/qa/artifacts/swing-2026-06-01/SW-QA-AUTH-001-admin-dashboard.png` |
| WSL-SW-QA-AUTH-001-login-offscreen.png | WSL2 offscreen login form | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-001-login-offscreen.png` |
| WSL-SW-QA-AUTH-002-invalid-login-offscreen.png | WSL2 invalid credential message | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-002-invalid-login-offscreen.png` |
| WSL-SW-QA-AUTH-003-admin-dashboard-offscreen.png | WSL2 admin dashboard | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-003-admin-dashboard-offscreen.png` |
| WSL-SW-QA-ADMIN-001-project-management-offscreen.png | WSL2 admin project management | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-001-project-management-offscreen.png` |
| WSL-SW-QA-ADMIN-002-account-management-offscreen.png | WSL2 admin account management | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-002-account-management-offscreen.png` |
| WSL-SW-QA-AUTH-004-pl-project-list-offscreen.png | WSL2 PL project list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-004-pl-project-list-offscreen.png` |
| WSL-SW-QA-PL-001-issue-list-offscreen.png | WSL2 PL issue list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-001-issue-list-offscreen.png` |
| WSL-SW-QA-PL-002-deleted-issues-offscreen.png | WSL2 PL deleted issue management | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-002-deleted-issues-offscreen.png` |
| WSL-SW-QA-PL-003-issue-detail-offscreen.png | WSL2 PL issue detail | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-003-issue-detail-offscreen.png` |
| WSL-SW-QA-AUTH-005-dev-project-list-offscreen.png | WSL2 Dev project list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-005-dev-project-list-offscreen.png` |
| WSL-SW-QA-DEV-001-issue-list-offscreen.png | WSL2 Dev issue list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-001-issue-list-offscreen.png` |
| WSL-SW-QA-DEV-002-issue-detail-offscreen.png | WSL2 Dev issue detail | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-002-issue-detail-offscreen.png` |
| WSL-SW-QA-AUTH-006-tester-project-list-offscreen.png | WSL2 Tester project list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-AUTH-006-tester-project-list-offscreen.png` |
| WSL-SW-QA-TESTER-001-issue-list-offscreen.png | WSL2 Tester issue list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-TESTER-001-issue-list-offscreen.png` |
| WSL-SW-QA-TESTER-002-issue-detail-offscreen.png | WSL2 Tester issue detail | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-TESTER-002-issue-detail-offscreen.png` |
| WSL-SW-QA-ADMIN-010-account-create-blank-offscreen.png | Admin blank account creation rejected | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-010-account-create-blank-offscreen.png` |
| WSL-SW-QA-ADMIN-011-account-created-offscreen.png | Admin account created | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-011-account-created-offscreen.png` |
| WSL-SW-QA-ADMIN-012-account-mutated-offscreen.png | Admin account renamed, role changed, activated | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-012-account-mutated-offscreen.png` |
| WSL-SW-QA-ADMIN-013-project-create-blank-offscreen.png | Admin blank project creation rejected | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-013-project-create-blank-offscreen.png` |
| WSL-SW-QA-ADMIN-014-project-mutated-offscreen.png | Admin project created, renamed, description changed | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-014-project-mutated-offscreen.png` |
| WSL-SW-QA-ADMIN-015-project-member-add-remove-offscreen.png | Admin project member add/remove | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-015-project-member-add-remove-offscreen.png` |
| WSL-SW-QA-ADMIN-016-project-member-active-remove-failure-offscreen.png | Admin active assignee removal rejected | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ADMIN-016-project-member-active-remove-failure-offscreen.png` |
| WSL-SW-QA-PL-010-register-refresh-offscreen.png | PL registered QA issue appears in list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-010-register-refresh-offscreen.png` |
| WSL-SW-QA-PL-011-new-detail-offscreen.png | PL QA issue detail in NEW state | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-011-new-detail-offscreen.png` |
| WSL-SW-QA-PL-012-issue-edited-offscreen.png | PL edited QA issue title/description | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-012-issue-edited-offscreen.png` |
| WSL-SW-QA-PL-013-priority-changed-offscreen.png | PL changed priority | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-013-priority-changed-offscreen.png` |
| WSL-SW-QA-PL-014-assigned-offscreen.png | PL assigned dev/verifier | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-014-assigned-offscreen.png` |
| WSL-SW-QA-PL-015-comment-added-offscreen.png | PL comment added | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-015-comment-added-offscreen.png` |
| WSL-SW-QA-PL-016-dependency-added-offscreen.png | PL dependency added | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-016-dependency-added-offscreen.png` |
| WSL-SW-QA-PL-017-dependency-removed-offscreen.png | PL dependency removed | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-017-dependency-removed-offscreen.png` |
| WSL-SW-QA-DEV-010-mark-fixed-offscreen.png | Dev marked QA issue fixed | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-010-mark-fixed-offscreen.png` |
| WSL-SW-QA-DEV-011-comment-added-offscreen.png | Dev comment added | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-011-comment-added-offscreen.png` |
| WSL-SW-QA-DEV-012-comment-edited-offscreen.png | Dev own comment edited | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-012-comment-edited-offscreen.png` |
| WSL-SW-QA-DEV-013-comment-deleted-offscreen.png | Dev own comment deleted | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-013-comment-deleted-offscreen.png` |
| WSL-SW-QA-TESTER-011-rejected-offscreen.png | Tester rejected fixed issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-TESTER-011-rejected-offscreen.png` |
| WSL-SW-QA-DEV-014-mark-fixed-after-reject-offscreen.png | Dev fixed after tester rejection | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-DEV-014-mark-fixed-after-reject-offscreen.png` |
| WSL-SW-QA-TESTER-010-resolved-offscreen.png | Tester resolved fixed issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-TESTER-010-resolved-offscreen.png` |
| WSL-SW-QA-PL-018-closed-offscreen.png | PL closed resolved issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-018-closed-offscreen.png` |
| WSL-SW-QA-PL-019-reopened-offscreen.png | PL reopened closed issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-019-reopened-offscreen.png` |
| WSL-SW-QA-PL-020-soft-deleted-offscreen.png | PL soft-deleted seed closed issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-020-soft-deleted-offscreen.png` |
| WSL-SW-QA-PL-021-restored-offscreen.png | PL restored deleted issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-021-restored-offscreen.png` |
| WSL-SW-QA-PL-022-purged-offscreen.png | PL purged deleted issue | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-022-purged-offscreen.png` |
| WSL-SW-QA-PL-023-statistics-full-offscreen.png | PL statistics all-time | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-023-statistics-full-offscreen.png` |
| WSL-SW-QA-PL-024-statistics-filtered-offscreen.png | PL statistics filtered | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-024-statistics-filtered-offscreen.png` |
| WSL-SW-QA-PL-025-statistics-invalid-range-offscreen.png | PL statistics invalid range rejected | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-PL-025-statistics-invalid-range-offscreen.png` |
| WSL-SW-QA-CROSS-001-admin-navigation-back-offscreen.png | Admin account/project back navigation and logout controls | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-001-admin-navigation-back-offscreen.png` |
| WSL-SW-QA-CROSS-002-pl-back-reenter-offscreen.png | PL project back and re-enter issue list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-002-pl-back-reenter-offscreen.png` |
| WSL-SW-QA-CROSS-003-dev-after-pl-logout-offscreen.png | Dev login after PL logout | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-003-dev-after-pl-logout-offscreen.png` |
| WSL-SW-QA-CROSS-004-tester-after-dev-logout-offscreen.png | Tester login after Dev logout | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-004-tester-after-dev-logout-offscreen.png` |
| WSL-SW-QA-CROSS-010-double-register-offscreen.png | Duplicate register double-click kept first request only | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-010-double-register-offscreen.png` |
| WSL-SW-QA-CROSS-011-double-search-offscreen.png | Duplicate search double-click kept list stable | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-011-double-search-offscreen.png` |
| WSL-SW-QA-CROSS-012-double-edit-offscreen.png | Duplicate edit double-click kept first request only | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-012-double-edit-offscreen.png` |
| WSL-SW-QA-CROSS-013-double-assignment-offscreen.png | Duplicate assignment double-click kept first request only | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-013-double-assignment-offscreen.png` |
| WSL-SW-QA-CROSS-014-double-comment-dependency-offscreen.png | Duplicate comment/dependency double-click kept first request only | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-014-double-comment-dependency-offscreen.png` |
| WSL-SW-QA-CROSS-015-double-status-offscreen.png | Duplicate status double-click kept first request only | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-015-double-status-offscreen.png` |
| WSL-SW-QA-CROSS-020-account-create-cancel-dialog.png | Account create cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-020-account-create-cancel-dialog.png` |
| WSL-SW-QA-CROSS-021-account-create-window-close-dialog.png | Account create window-close dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-021-account-create-window-close-dialog.png` |
| WSL-SW-QA-CROSS-022-account-rename-cancel-dialog.png | Account rename cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-022-account-rename-cancel-dialog.png` |
| WSL-SW-QA-CROSS-023-account-role-cancel-dialog.png | Account role cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-023-account-role-cancel-dialog.png` |
| WSL-SW-QA-CROSS-024-account-deactivate-cancel-dialog.png | Account deactivate cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-024-account-deactivate-cancel-dialog.png` |
| WSL-SW-QA-CROSS-025-account-after-cancel-offscreen.png | Account management state after cancel/window-close actions | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-025-account-after-cancel-offscreen.png` |
| WSL-SW-QA-CROSS-026-project-create-cancel-dialog.png | Project create cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-026-project-create-cancel-dialog.png` |
| WSL-SW-QA-CROSS-027-project-create-window-close-dialog.png | Project create window-close dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-027-project-create-window-close-dialog.png` |
| WSL-SW-QA-CROSS-028-project-rename-cancel-dialog.png | Project rename cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-028-project-rename-cancel-dialog.png` |
| WSL-SW-QA-CROSS-029-project-description-cancel-dialog.png | Project description cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-029-project-description-cancel-dialog.png` |
| WSL-SW-QA-CROSS-030-project-delete-cancel-dialog.png | Project delete cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-030-project-delete-cancel-dialog.png` |
| WSL-SW-QA-CROSS-031-project-after-cancel-offscreen.png | Project management state after cancel/window-close actions | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-031-project-after-cancel-offscreen.png` |
| WSL-SW-QA-CROSS-032-register-cancel-dialog.png | Issue register cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-032-register-cancel-dialog.png` |
| WSL-SW-QA-CROSS-033-register-window-close-dialog.png | Issue register window-close dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-033-register-window-close-dialog.png` |
| WSL-SW-QA-CROSS-034-assignment-cancel-dialog.png | Issue assignment cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-034-assignment-cancel-dialog.png` |
| WSL-SW-QA-CROSS-035-edit-cancel-dialog.png | Issue edit cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-035-edit-cancel-dialog.png` |
| WSL-SW-QA-CROSS-036-edit-window-close-dialog.png | Issue edit window-close dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-036-edit-window-close-dialog.png` |
| WSL-SW-QA-CROSS-037-priority-cancel-dialog.png | Issue priority cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-037-priority-cancel-dialog.png` |
| WSL-SW-QA-CROSS-038-comment-cancel-dialog.png | Issue comment cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-038-comment-cancel-dialog.png` |
| WSL-SW-QA-CROSS-039-dependency-cancel-dialog.png | Issue dependency cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-039-dependency-cancel-dialog.png` |
| WSL-SW-QA-CROSS-040-delete-cancel-dialog.png | Issue delete cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-040-delete-cancel-dialog.png` |
| WSL-SW-QA-CROSS-041-issue-detail-after-cancel-offscreen.png | Issue detail state after cancel/window-close actions | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-041-issue-detail-after-cancel-offscreen.png` |
| WSL-SW-QA-CROSS-042-status-cancel-dialog.png | Dev status cancel dialog | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-042-status-cancel-dialog.png` |
| WSL-SW-QA-CROSS-043-status-after-cancel-offscreen.png | Dev detail state after status cancel | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-043-status-after-cancel-offscreen.png` |
| WSL-SW-QA-CROSS-044-minimum-long-issue-list-offscreen.png | 800x600 long Korean/English issue list | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-044-minimum-long-issue-list-offscreen.png` |
| WSL-SW-QA-CROSS-045-minimum-long-issue-detail-offscreen.png | 800x600 long Korean/English issue detail | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-CROSS-045-minimum-long-issue-detail-offscreen.png` |
| WSL-SW-QA-ROBOT-001-login-initial.png | Robot focus probe initial login screen | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ROBOT-001-login-initial.png` |
| WSL-SW-QA-ROBOT-002-login-id-click-no-focus.png | Robot coordinate click on login ID did not focus field | `docs/qa/artifacts/swing-2026-06-01/WSL-SW-QA-ROBOT-002-login-id-click-no-focus.png` |
