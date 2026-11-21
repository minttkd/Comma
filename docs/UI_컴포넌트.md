# 쉼표 UI 컴포넌트·토큰 목록

모든 값과 부품은 `android/app/src/main/java/com/shimpyo/app/Ui.java` 한 곳에 있다.
화면 코드(`MainActivity`, `OverlayController`, `DemoCheckoutActivity`)에는 숫자 dp·sp·색을 쓰지 않고 아래 이름만 쓴다.
값을 바꾸면 그 이름을 쓰는 모든 화면이 함께 바뀐다.

앱의 설정(홈 오른쪽 위 톱니바퀴) › 테스트 도구 › **UI 갤러리 열기**에서 아래 토큰과 부품을 실제 모양으로 볼 수 있다 (`UiGalleryActivity.java`).
토큰 목록은 `Ui.java`에서 자동으로 읽으므로 새 토큰은 갤러리에 바로 나온다. 새 부품을 만들면 갤러리의 `components()`에 예시를 하나 추가한다.

## 토큰

### 색 (Figma Color 변수)

| 이름 | 값 | Figma |
|---|---|---|
| `BRAND` | #292D2B | brand/blue · brand/navy (먹색) |
| `BRAND_LIGHT` | #EAF1EC | brand/blue-light |
| `TEXT` | #292D2B | text/primary |
| `MUTED` | #737A75 | text/muted |
| `SUBTLE` | #879A8A | text/subtle |
| `LINE` | #DFE3DF | line · chip/stroke |
| `BG` | #F3F5F2 | bg |
| `WHITE` | #FFFFFF | surface |
| `RED` / `RED_BG` | #A14D46 / #F7EAE8 | status/red |
| `AMBER` / `AMBER_BG` | #8C6C23 / #F7F0DE | status/amber |
| `GREEN` / `GREEN_BG` | #37624C / #E8EFE9 | status/green |
| `DIM` | #17201A 55% | dim |
| `ON_BRAND` / `ON_BRAND_SUB` | WHITE / LINE | 먹색 바탕 위 글자 |
| `HERO_BG` / `ON_HERO` / `ON_HERO_SUB` / `ON_HERO_TRACK` | BRAND / WHITE / LINE / 흰색 20% | 먹색 강조 카드 (heroCard · 홈 오늘 카드 먹색). 어두운 화면에서는 짙은 초록 바탕 |
| `GUARD_FILL` | status/green 16% | 결제 버튼 가드 표시 |

### `Ui.Tone`: 상태 색 묶음 (바탕 + 글자)

| 이름 | 바탕 / 글자 | 쓰는 곳 |
|---|---|---|
| `SUCCESS` | GREEN_BG / GREEN | 멈춤, 여유, 필요 판정, 식비 예산 |
| `WARNING` | AMBER_BG / AMBER | 주의, 숙고 후 결제, 행동 트리거 |
| `DANGER` | RED_BG / RED | 초과, 재무 트리거 |
| `BRAND` | BRAND_LIGHT / BRAND | 강도 태그, 회고 태그 |
| `ACCENT` | BRAND_LIGHT / GREEN | 필수 권한, 이유 선택 결제 |
| `NEUTRAL` | BG / MUTED | 선택 권한, 여윳돈, 기타 결과 |
| `INVERSE` | BRAND / WHITE | 강한 개입 강도 |

### `Ui.Font`: 글자 크기 (sp / 줄 높이)

| 이름 | 크기 | Figma 스타일 | 쓰는 곳 |
|---|---|---|---|
| `DISPLAY` | 30/38 | Display | 리포트 마음 돌림 비율 |
| `AMOUNT` | 28/40 | — | 홈 오늘 쓸 수 있는 돈 |
| `TITLE` | 22/30 | Title | 화면 제목 |
| `STAT` | 18/26 | — | 홈 요약 타일 값 |
| `HEADING` | 17/25 | Heading | 시트·개입 제목, 합계 |
| `BUTTON` | 16/22 | Button | 버튼 |
| `BODY` | 15/22 | Body | 본문, 칩, 줄 라벨 |
| `LABEL` | 14/20 | — | 목록 제목 |
| `SMALL` | 13/19 | — | 짧은 안내, 글자 링크 |
| `CAPTION` | 12/17 | Caption | 머리말, 태그, 부가 정보 |
| `MICRO` | 11/16 | — | 아주 작은 설명, 로그 |

굵기: `REGULAR` 400 · `MEDIUM` 500 · `BOLD` 700

### 간격과 여백을 나눈 이유

- `Ui.Space`는 **요소와 요소 사이**(바깥) 간격만 담는다.
- `Ui.Pad`는 **부품 안쪽** 여백을 담고, 부품·자리마다 이름이 따로 있다.

그래서 `Space.BLOCK`을 바꾸면 카드 사이만 넓어지고 카드 크기는 그대로다.
`Pad.BUTTON_V`를 바꾸면 버튼 높이만 바뀐다. 값이 같아도 이름이 다르면 서로 영향을 주지 않는다.

### `Ui.Space`: 사이 간격 (dp)

| 이름 | 값 | 쓰는 자리 |
|---|---|---|
| `TEXT` | 2 | 제목 ↔ 바로 아래 설명 |
| `LINE` | 4 | 가까운 줄 사이 |
| `TIGHT` | 6 | 단계 점·태그 사이, 내 답 줄 사이 |
| `ITEM` | 8 | 나란히 놓인 요소, 칩 간격, 설정 입력 줄 사이 |
| `ROW` | 10 | 카드 안 줄 사이 |
| `BLOCK` | 12 | 카드 ↔ 카드 (홈·리포트·기록·S03) |
| `STACK` | 14 | 개입 시트·S01 세로 간격 |
| `WIDE` | 16 | S02 블록 사이 |
| `SECTION` | 18 | 섹션 제목 위 (설정) |
| `LARGE` | 24 | 큰 섹션 위 (설정 테스트 도구) |

### `Ui.Pad`: 안쪽 여백 (dp, `_H` 좌우 · `_V` 위아래)

| 묶음 | 이름 (값) |
|---|---|
| 화면 틀 | `SCREEN_H` 20 · `SCREEN_TOP` 12 · `SCREEN_BOTTOM` 24 · `FOOTER_TOP` 8 · `STEPS_TOP` 14 · `STEPS_BOTTOM` 10 |
| 시트 | `SHEET_H` 20 · `SHEET_TOP` 18 · `REVIEW_SHEET_TOP` 22 · `SHEET_BOTTOM` 24 |
| 카드·상자 | `CARD` 16 · `TODAY_CARD` 18 · `TILE_H` 16 · `TILE_V` 14 · `REPORT_TILE` 14 · `HERO` 16 · `HERO_LARGE` 20 · `BOX_H` 16 · `BOX_V` 14 · `ANSWER_BOX` 14 · `TOAST_H` 18 · `TOAST_V` 14 |
| 안내 상자 | `FACT_H` 12 · `FACT_V` 8 · `PRIVACY_NOTE` 12 · `RETRY_NOTE_H` 12 · `RETRY_NOTE_V` 10 · `FOOD_NOTE_H` 14 · `FOOD_NOTE_V` 10 |
| 버튼·칩·태그 | `BUTTON_H` 20 · `BUTTON_V` 16 · `LINK` 8 · `ICON` 6 · `CHIP_H` 16 · `CHIP_V` 10 · `TAG_H` 10 · `TAG_V` 4 · `SMALL_TAG_H` 8 · `SMALL_TAG_V` 3 |
| 대화·입력 | `BUBBLE_H` 14 · `BUBBLE_AI_V` 12 · `BUBBLE_USER_V` 10 · `INPUT_H` 14 · `INPUT_V` 12 · `SEND_LEFT` 10 |
| 목록 | `LIST_ROW_V` 10 · `QA_INDENT` 10 · `TINT_ROW_H` 12 · `TINT_ROW_V` 10 |
| 데모 결제 | `DEMO_SIDE` 16 · `DEMO_APPBAR_V` 12 · `DEMO_SECTION_V` 14 · `DEMO_PAYBAR_TOP` 10 · `DEMO_PAYBAR_BOTTOM` 18 · `DEMO_PAY_V` 15 · `DEMO_CONTENT_TOP` 8 · `DEMO_CONTENT_BOTTOM` 16 |

### `Ui.Radius`: 모서리 (dp)

`BAR` 4 · `CHECK` 6 · `TAG` 8 · `NOTE` 10 · `INPUT` 12 · `BOX` 14 · `CARD` 16 · `CHIP` 20 · `SHEET` 24

### `Ui.Size`: 고정 크기 (dp)

`NAV_HEIGHT` 63 · `NAV_INSET` 28 · `NAV_DOT` 6 · `ICON_BUTTON` 24 · `CHECKBOX` 20 · `STEP_DOT` 8 · `STEP_ACTIVE` 24 · `BAR` 8 · `BAR_THIN` 6 · `LIST_DOT` 8 · `LIST_BAR` 4 · `FIELD_WIDTH` 130 · `ROW_LABEL` 48 · `STROKE` 1 · `STROKE_STRONG` 1.5 · `GUARD_STROKE` 3 · `ELEVATION` 6 · `TOAST_OFFSET` 100 · (갤러리 전용) `SWATCH` 32 · `RADIUS_SAMPLE` 48 · `GALLERY_LABEL` 150

## 컴포넌트

### 글자

| 함수 | 모양 |
|---|---|
| `title(c, s)` | 22 Bold, 화면 제목 |
| `heading(c, s)` | 17 Bold, 시트 제목 |
| `sectionTitle(c, s)` | 15 Bold, 섹션 머리말 |
| `body(c, s)` | 15 Regular muted |
| `bodySmall(c, s)` | 13 Regular muted |
| `caption(c, s)` | 12 Medium muted, 카드 머리말 |
| `meta(c, s)` | 12 Regular muted, 시각·부가 정보 |
| `micro(c, s, color)` | 11 Regular |
| `text(c, s, Font, color, weight)` | 위에 없는 조합 |

### 배치

| 함수 | 하는 일 |
|---|---|
| `stack(parent, child, gap)` | 세로로 붙이고 위 간격을 둔다 |
| `inline(row, child, gap)` | 가로로 붙이고(내용 크기) 왼쪽 간격을 둔다 |
| `inlineFill(row, child, gap)` | 가로로 붙이고 남은 폭을 나눠 갖는다 |
| `centered(c, child)` | 가운데 정렬 한 줄 |
| `pad(v, c, all)` · `pad(v, c, h, v)` · `pad(v, c, l, t, r, b)` | 안쪽 여백 |

### 컨테이너

| 함수 | 모양 | 화면 |
|---|---|---|
| `card(c)` | 흰 바탕, radius 16, padding 16 | 공통 |
| `tintCard(c, color)` | 색 바탕 card | S02 정기 수입 |
| `outlineCard(c, padH, padV)` | 흰 바탕 + 1dp 테두리, radius 14 | S04 홈 |
| `tintBox(c, color, padH, padV)` | 색 바탕, radius 14 | 토스트, 내 답 |
| `bottomSheet(c, padTop)` | 위 모서리 24 흰 시트 | S09 회고, 개입 오버레이 |

### 버튼

| 함수 | 모양 |
|---|---|
| `button(c, label, primary, click)` | Button/Primary · Secondary |
| `buttonPair(c, left, right)` | 같은 폭 버튼 두 개 |
| `linkButton(c, label, click)` | Button/Text (12 Medium muted) |
| `textLink(c, label, color, click)` | 13 Bold 글자 링크 ("… 열기 ›") |
| `backButton(c, click)` · `closeButton(c, click)` | ← · ✕ |
| `setEnabled(button, on)` | 비활성 표시 |

### 칩 · 태그

| 함수 | 모양 |
|---|---|
| `chip(c, label, selected, click)` | Chip · Chip/Selected |
| `chipFlow(c, chips)` · `chipGrid(c, chips, perRow)` | 칩 줄바꿈 배치 · 같은 폭 배치 |
| `tag(c, label, Tone)` | Tag (radius 8, 12 Medium) |
| `smallTag(c, label, Tone)` | 작은 태그 (radius 6, 12 Bold) |
| `tagFlow(c)` | 태그 줄바꿈 배치 (gap 6) |

### 정보 표시

| 함수 | 모양 | 화면 |
|---|---|---|
| `kv(c, label, value, total)` | 왼쪽 라벨 · 오른쪽 값 | S03, 리포트, 설정 |
| `listRow(c, [dotColor,] title, sub, amount, tag)` | (결과 색 점) 제목·부가 / 금액·태그 | 홈 최근 기록, 기록 화면 |
| `listRowBar(c, barColor, title, sub, amount, tag)` | 왼쪽 세로 색 막대 + 목록 한 줄 | 기록 색 ② |
| `tintRow(c, row, bg)` | 줄 전체 연한 바탕 | 기록 색 ③ |
| `statTile(c, label, value, sub, outlined[, ratio, barColor, barTrack])` | 요약 타일 (+ 얇은 진행 막대) | 홈(outlined, 막대) · 리포트 |
| `tilePair(c, left, right, gap)` | 타일 두 개 나란히 | 홈 · 리포트 |
| `heroCard(c, label, value, sub, large)` | 먹색 강조 카드 | S03 여윳돈 · S10 마음 돌림 비율(large) |
| `alertBox(c, title, desc, action, click)` | 빨간 경고 상자 | 홈 결제 감지 꺼짐 |
| `highlightBox(c, label, value[, click])` | 연초록 성과 상자 (click이 있으면 오른쪽 → 화살표, 누르면 이동) | 홈 이번 달 쉼표 → 리포트 |
| `tintNote(c, s, Font, fg, Radius, padH, padV)` | 연초록 안내 한 줄 | S01, 리포트, 개입 재시도 |
| `factBox(c, s)` | 사실 한 줄 (왼쪽 초록 선) | 개입 오버레이 |
| `bar(c, ratio, fill, track[, heightDp])` | 진행 막대 | 홈, 리포트 |
| `splitBar(c, values[], colors[])` | 나눠진 막대 (값 비율대로 색 이어 붙임) | 리포트 ② 가치 회고 · ③ 만족·후회 |
| `legendRow(c, color, label, value)` | 색 점 · 이름 · 값 | 리포트 |
| `levelDots(c, level, total)` | 깊이 표시 ●●○ | 리포트 ④ 대화 깊이 |
| `dotFlow(c, colors[])` | 점 여러 개 줄바꿈 배치 (대화 하나 = 점 하나) | 리포트 ④ 대화 깊이 |
| `heroSentence(c, label, sentence, sub)` | 강조 카드 + 한 문장 | 리포트 ② 가치 회고 |
| `quote(c, said, sub)` | 내가 한 말 인용 (왼쪽 초록 선) | 리포트 ② 가치 회고 |
| `divider(c)` | 1dp 선 | 기록 화면 |

### 대화 · 내비게이션 · 입력

| 함수 | 모양 |
|---|---|
| `bubbleAi(c, question, faded)` · `bubbleUser(c, answer)` | Bubble/AI · Bubble/User |
| `screenHeader(c, title, back)` | ← 뒤로 + 화면 제목 (기록 · 리포트 · 설정) |
| `sectionHeader(c, title, link, click)` | 섹션 머리말 + 오른쪽 글자 링크 (홈 "최근 기록 · 더보기 ›") |
| `settingsButton(c, click)` | 톱니바퀴 아이콘 (코드로 그림, `GearDrawable`) |
| `avatar(c)` | 쉼표 얼굴 (초록 동그라미 "쉼", 개입 채팅 ②·③) |
| `bubbleAi(c, question, faded, bg)` · `bubbleUser(c, answer, bg, fg)` | 얼굴 + 말풍선 · 색 지정 내 답 (개입 채팅 ②·③) |
| `bottomNav(c, labels, selected, onSelect)` | BottomNav (지금 앱에서는 안 씀) |
| `steps(c, current, total, doneFilled)` | 온보딩 단계 점 |
| `checkRow(c, label, checked, click)` | 체크 상자 + 문구 |
| `input(c, hint, value, number)` | Input (테두리) |
| `plainInput(c, hint, value)` | 테두리 없는 입력칸 |
| `labeledField(c, label, input)` | 라벨 + 고정 폭 입력칸 |

## 홈(S04) 색 규칙

| 자리 | 색 | 정하는 곳 (`MainActivity`) |
|---|---|---|
| 오늘 쓸 수 있는 돈 | 신호 색 바탕 (SIGNAL): 여유 연초록 · 주의 연노랑 · 초과 연빨강 | `todayBudgetCard()` 위 주석에 다른 방식 |
| 여유 / 주의 / 초과 태그 | 초록 / 노랑 / 빨강 | `signalTone()` |
| 남은 여윳돈 · 식비 예산 막대 | 남으면 초록, 다 쓰면 빨강(바탕 연빨강) | `remainColor()` · `remainTrack()` |
| 기록 태그 · 결과 색 | 줄 바탕(TINT), 태그와 같은 연한 색: 멈춤·식비 예산 연초록, 이유 선택 결제 연한 먹색, 숙고 후 결제 연노랑, 나머지 회색 | `outcomeTone()` · `outcomeDotColor()` · `recordRowView()` |

## 다크 모드

색은 밝은 화면 · 어두운 화면 두 벌이다 (`Ui.applyPalette()` 안의 표: 왼쪽 밝게 · 오른쪽 어둡게).
화면 모드는 `UiPrefs.theme`: **① 자동**(기본, 휴대폰이 다크 모드면 어둡게) · ② 밝게 · ③ 어둡게. 설정 화면 맨 위 "화면 모드" 카드나 UI 갤러리에서 바꾼다.

- 색 이름은 밝은 화면 기준이다. `WHITE`는 "카드 바탕(surface)", `BRAND`는 "가장 진한 강조 색"이라 어두운 화면에서는 `WHITE`가 짙은 회색, `BRAND`(Primary 버튼)가 밝은 색, `ON_BRAND`가 어두운 글자가 된다. 먹색 강조 카드는 밝은 덩어리가 눈부셔서 `HERO_BG`(짙은 초록)로 따로 둔다.
- 화면을 만들기 전에 `Ui.applyTheme(context)`를 부른다: `MainActivity` · `UiGalleryActivity` onCreate, 개입 오버레이 `show()` · `toast()`. 다크 모드 색을 고치려면 `applyPalette()` 오른쪽 값만 바꾸면 된다.
- 데모 결제 화면은 다른 앱 흉내라 항상 밝은 색이다 (`Ui.applyPalette(false)`).
- 색을 새로 쓸 때 `Color.WHITE` 같은 고정 색을 쓰면 다크 모드에서 안 바뀐다. 꼭 `Ui.` 색 이름을 쓴다.

## 앱 안에서 UI 바꿔 보기 (`UiPrefs`)

설정(톱니바퀴) › 테스트 도구 › **UI 갤러리 열기** › 맨 위 **앱 UI 설정**에서 번호 칩으로 고른다. 고르면 바로 저장되고 홈으로 돌아가면 반영된다.
값은 프론트 전용 저장소(`ui_prefs`)에 있어 `Store`와 섞이지 않는다. 각 설정의 **①이 팀이 정한 기본**이고 나머지는 비교용 후보다. **기본값으로** 버튼으로 언제든 ①로 돌아간다.

| 설정 | 고를 수 있는 값 (굵게 = 기본) | 그리는 곳 |
|---|---|---|
| 화면 모드 (다크 모드) | **① 자동** · ② 밝게 · ③ 어둡게 | `Ui.applyTheme()` |
| 오늘 쓸 수 있는 돈 카드 | **① 상태 색** · ② 먹색 · ③ 흰색 | `todayBudgetCard()` |
| 남은 여윳돈 · 식비 예산 막대 | **켜기** · 끄기 | `tileBar()` |
| 카드 회색 테두리 | **켜기** · 끄기 | `Ui.outlineCard()` |
| 기록 결과 색 표시 | **① 줄 바탕** · ② 점 · ③ 왼쪽 막대 · ④ 없음 | `recordRowView()` |
| 개입 채팅 (고르는 중) | **① 지금** · ② 부드럽게 · ③ 쉼표 색 | `OverlayController` (`aiBubble`, `userBubble`, `softHeaderRow`) |
| 리포트 내용 (고르는 중) | **① 지금** · ② 가치 회고 · ③ 만족·후회 · ④ 대화 깊이 · ⑤ 숫자 세 개 | `reportScreen()` → `ReportViews` |

개입 채팅 · 리포트는 아직 팀이 고르는 중이라 ①이 지금 화면이다. 정해지면 그 값을 ①(기본)로 옮긴다.

- **개입 채팅 후보**: ①은 트리거·강도 태그("재정위험 전환점", "보통 ●●○")와 회색 말풍선. ②는 태그 대신 쉼표 얼굴 · "배민 · 23,000원 결제 전에 잠깐", 진행은 점, 쉼표 말풍선 연초록 · 내 답 초록. ③은 ②의 머리줄에 시트 바탕을 연초록으로, 말풍선은 흰색. UI 갤러리의 "개입 화면 미리보기"에서 바로 눌러 볼 수 있다.
- **리포트 ② 가치 회고** (`ReportViews.reflect`): 사용자의 가치판단을 돌아보게 하는 후보.
  1. **이번 달 내 기준** 한 문장: 고른 이유를 가치로 묶어(`ReportViews.Value`: 필요해서 · 사람 때문에 · 기분으로 · 대화 끝에) 다음 날 만족/후회와 잇는다. 예: "필요해서·사람 때문에 쓴 돈엔 만족했고, 기분으로 쓴 돈은 후회했어요"
  2. **무엇 때문에 썼고, 다음 날 어땠나요**: 가치별 횟수 · 금액 · 만족/후회 막대 · 그 안의 이유들
  3. **그때 내가 한 말**: 대화에서 마지막으로 한 답을 인용 (`Record.qa`)
  4. **멈춘 결제, 지금은 어때요?** [아직 사고 싶어요] [안 사길 잘했어요] · 5. **다음 달엔** 줄이고 싶은 결제 — 4·5는 화면만 있고 저장하지 않는다. 답을 어디에 둘지(Record 구조, `core/Stats` 후회율)는 차민상과 합의 후 연결.
  - 이유 → 가치 묶음은 초안이다 (`Value.of`). 기획 확인 필요.
- **리포트 후보**: 시스템 용어(가벼움·보통·강함, 결과 분류) 대신 사용자가 직접 한 판단을 보여 준다. 결제할 때 고른 이유(가벼움 이유 칩), 다음 날 돌아본 마음(S09 만족해요 / 후회돼요), 대화하다 멈춘 결제. ④ 대화 깊이는 강도 세 단계를 그대로 보여 주되 이름을 바꾼다: 가벼움 → "살짝 물어봤을 때", 보통 → "이야기를 나눴을 때", 강함 → "깊게 이야기했을 때" (`ReportViews.depthName`). 대화 하나가 점 하나(초록 = 멈춤, 회색 = 결제)이고, 깊이별 멈춘 비율을 한 문장으로 요약한다 (`depthInsight`). 집계는 `ReportViews.Data`가 화면에서 직접 센다 (core `Stats`는 그대로). 설정 › 테스트 도구 › **리포트 예시 기록 12건 추가**로 데이터를 넣고 보면 된다.

기록 색을 볼 때는 테스트 도구의 **결과별 예시 기록 4건 추가**로 멈춤·이유 선택·숙고 후·식비 기록을 넣고 보면 잘 보인다.

새 방식을 추가하려면: `UiPrefs`의 enum에 값 추가 → 그리는 코드에서 분기 → `UiGalleryActivity.uiSettings()`에 칩 추가.

## 화면 메서드 (`MainActivity`)

| 화면 | 메서드 | 안의 카드 |
|---|---|---|
| S01 권한 | `onboardingPermissionScreen()` | `permissionCard`, `permissionRow` |
| S02 정기 수입 | `onboardingIncomeScreen()` | `linkedAccountCard`, `incomeCard`, `consentCard` |
| S03 예산 | `onboardingBudgetScreen()` | `amountListCard`, heroCard |
| S04 홈 | `homeScreen()` | `homeHeader`(톱니바퀴), `todayBudgetCard`, 이번 달 쉼표(→ 리포트), 최근 기록(더보기 → 기록), `recentRecordsCard`, `demoCard` |
| S09 회고 | `showReview()` | `reviewSheetContent` |
| 기록 (홈 › 더보기) | `recordsScreen()` | `recordRow` |
| S10 리포트 (홈 › 이번 달 쉼표) | `reportScreen()` (후보 ②~④는 `ReportViews.values/story/simple`) | `intensityCard`, `outcomeCard` |
| 설정 (홈 › 톱니바퀴) | `settingsScreen()` | `statusCard`, `numberField`, `testTools`, `logCard`, `screenTextCard` |

하단 탭은 없다. 홈이 기본 화면이고 기록 · 리포트 · 설정은 홈에서 들어가는 하위 화면이다 (`open(PAGE_...)`, ← 뒤로나 기기 뒤로가기로 홈).

각 화면 메서드는 맨 위에서 데이터(`Store`, `BudgetEngine`, `Stats`, `BackendSlots`)를 한 번 읽고 카드 메서드에 값으로 넘긴다.

## 데이터가 오는 곳

| 출처 | 값 | 상태 |
|---|---|---|
| `Store` · `core/` (차민상) | 설정, 결제 기록, 예산 계산, 통계 | 연결됨 |
| `BackendSlots.java` (프론트) | 연결된 계좌, 정기 수입 내역, 수입 연속 개월 수, 알림 허용 여부 | **데모 값**. 백엔드가 생기면 채운다 |
| `PackageManager` | 앱 버전 (설정) | `build.gradle.kts`의 `versionName`에서 자동으로 |

### `BackendSlots` 연결하는 법

1. `BackendSlots.load(Context)` 안에서 백엔드 값을 받아 새 `BackendSlots`를 채우고 `current`에 넣는다 (파일 안 주석에 예시가 있다).
2. 네트워크 호출이면 백그라운드에서 부르고, 끝나면 화면의 `refresh()`를 다시 부른다.
3. 화면 코드는 고치지 않아도 된다. 화면은 `BackendSlots.get()`으로만 읽는다.

| 필드 | 쓰는 화면 | 비었을 때 |
|---|---|---|
| `account` (`bank`, `kind`, `maskedNumber`, `demo`) | S02 연결된 계좌 | `null`이면 "연결된 계좌가 없어요" |
| `incomeSources` (`label`, `amount`) | S02 정기 수입 내역 | 합계가 월 수입과 다르면 "최근 N개월 연속 같은 날 들어왔어요" |
| `incomeStreakMonths` | S02 설명·내역 | — |
| `notificationAllowed` | S01 알림 줄 ✓ | `false` |
