# 쉼표 UI 컴포넌트·토큰 목록

모든 값과 부품은 `android/app/src/main/java/com/shimpyo/app/Ui.java` 한 곳에 있다.
화면 코드(`MainActivity`, `OverlayController`, `DemoCheckoutActivity`)에는 숫자 dp·sp·색을 쓰지 않고 아래 이름만 쓴다.
값을 바꾸면 그 이름을 쓰는 모든 화면이 함께 바뀐다.

앱의 설정(홈 오른쪽 위 톱니바퀴) › 테스트 도구 › **UI 갤러리 열기**에서 아래 토큰과 부품을 실제 모양으로 볼 수 있다 (`UiGalleryActivity.java`).
토큰 목록은 `Ui.java`에서 자동으로 읽으므로 새 토큰은 갤러리에 바로 나온다. 새 부품을 만들면 갤러리의 `components()`에 예시를 하나 추가한다.

## 토큰

### 색 (Figma Color 변수)

밝은 화면 · 어두운 화면 두 벌이 있다 (`Ui.applyPalette` 안의 표). 그래서 색은 `final`이 아니다.
이름은 밝은 화면 기준이라, 어두운 화면에서는 `WHITE`(카드 바탕)가 짙은 회색, `BRAND`(가장 진한 강조)가 밝은 색이 된다.

| 이름 | 밝게 | 어둡게 | Figma |
|---|---|---|---|
| `BRAND` | #292D2B | #DCE4DE | brand/blue · brand/navy (먹색) |
| `BRAND_LIGHT` | #EAF1EC | #2A3C32 | brand/blue-light · 서비스 색 연한 바탕 (개입 시트, 다음 달엔 상자) |
| `TEXT` | #292D2B | #E6EBE7 | text/primary |
| `MUTED` | #737A75 | #A3ACA6 | text/muted |
| `SUBTLE` | #879A8A | #8FA396 | text/subtle |
| `LINE` | #DFE3DF | #414A44 | line · chip/stroke |
| `BG` | #F3F5F2 | #121513 | bg (화면 바탕) |
| `WHITE` | #FFFFFF | #262C28 | surface (카드 · 시트) |
| `FILL` | #F3F5F2 | #333B36 | 카드 안 회색 칸 · 막대 바탕 · AI 말풍선. 밝은 화면은 BG와 같고, 어두운 화면은 카드보다 살짝 밝게 (BG를 쓰면 카드 안에 까만 구멍처럼 보여서 따로 둠) |
| `RED` / `RED_BG` | #A14D46 / #F7EAE8 | #E8968C / #4A2D29 | status/red |
| `AMBER` / `AMBER_BG` | #8C6C23 / #F7F0DE | #DDB96B / #463A20 | status/amber |
| `GREEN` / `GREEN_BG` | #37624C / #E8EFE9 | #88C8A3 / #26392D | status/green · **서비스 색(쉼표 초록)** |
| `DIM` | #17201A 55% | 검정 67% | dim |
| `ON_BRAND` / `ON_BRAND_SUB` | #FFFFFF / #DFE3DF | #151916 / #55605A | 먹색 바탕 위 글자 |
| `HERO_BG` / `ON_HERO` / `ON_HERO_SUB` | #37624C / #FFFFFF / #CFE0D5 | #2F4A3A / #E6EBE7 / #A6B6AB | 강조 카드 (heroCard · heroSentence). 어두운 화면은 눈부시지 않게 짙은 초록 |
| `ON_HERO_BOX` | 흰색 14% | 흰색 14% | 강조 카드 안 상자 |
| `GUARD_FILL` | status/green 16% | #88C8A3 19% | 결제 버튼 가드 표시 |

**화면 모드 (다크 모드)**
- 홈 윗줄의 해/달 토글(`Ui.themeToggle`)로 바꾼다. 밝은 화면이면 해, 어두운 화면이면 달 모양이고 누르면 반대로 바뀐다. 보던 화면은 그대로 다시 그린다.
- 고른 값은 `SharedPreferences("ui")`의 `theme`(light · dark)에 저장한다 (`Ui.saveTheme`). 한 번도 안 골랐으면 휴대폰 다크 모드 설정을 따른다 (`Ui.wantsDark`).
- 화면(Activity · 개입 오버레이 · 토스트)을 만들기 전에 `Ui.applyTheme(context)`를 부른다. `MainActivity`는 돌아왔을 때 모드가 바뀌었으면 다시 만든다.
- 데모 결제 화면(`DemoCheckoutActivity`)은 배민 · 쿠팡 · 무신사 흉내라 다크 모드여도 항상 밝게 (`Ui.applyPalette(false)`).
- 리포트 카테고리 색(`ReportViews.Category.color()`)도 어두운 화면용 단계가 따로 있다.
- 어두운 화면은 바탕(BG) < 카드(WHITE) < 카드 안 칸(FILL) 순으로 밝아진다. 카드 안 칸에 BG를 쓰지 않는다.

### `Ui.Tone`: 상태 색 묶음 (바탕 + 글자)

| 이름 | 바탕 / 글자 | 쓰는 곳 |
|---|---|---|
| `SUCCESS` | GREEN_BG / GREEN | 멈춤, 여유, 필요 판정, 식비 예산 |
| `WARNING` | AMBER_BG / AMBER | 주의, 숙고 후 결제, 행동 트리거 |
| `DANGER` | RED_BG / RED | 초과, 재무 트리거 |
| `BRAND` | BRAND_LIGHT / BRAND | 강도 태그, 회고 태그 |
| `ACCENT` | BRAND_LIGHT / GREEN | 필수 권한, 이유 선택 결제 |
| `NEUTRAL` | FILL / MUTED | 선택 권한, 여윳돈, 기타 결과 |
| `INVERSE` | BRAND / WHITE | 강한 개입 강도 |

### `Ui.Font`: 글자 크기 (sp / 줄 높이)

| 이름 | 크기 | Figma 스타일 | 쓰는 곳 |
|---|---|---|---|
| `DISPLAY` | 30/38 | Display | 큰 숫자 |
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
| `accentChip(c, label, selected, click)` · `setAccentChipSelected` | 서비스 색 칩: 고르면 초록 바탕 + 흰 글자 (리포트 회고 답) |
| `chipFlow(c, chips)` · `chipGrid(c, chips, perRow)` | 칩 줄바꿈 배치 · 같은 폭 배치 |
| `tag(c, label, Tone)` | Tag (radius 8, 12 Medium) |
| `smallTag(c, label, Tone)` | 작은 태그 (radius 6, 12 Bold) |
| `tagFlow(c)` | 태그 줄바꿈 배치 (gap 6) |

### 정보 표시

| 함수 | 모양 | 화면 |
|---|---|---|
| `kv(c, label, value, total)` | 왼쪽 라벨 · 오른쪽 값 | S03, 리포트, 설정 |
| `listRow(c, [dotColor,] title, sub, amount, tag)` | (결과 색 점) 제목·부가 / 금액·태그 | 홈 최근 기록, 기록 화면 |
| `tintRow(c, row, bg)` | 줄 전체 연한 바탕 | 기록 줄 결과 색 |
| `statTile(c, label, value, sub, outlined[, ratio, barColor, barTrack])` | 요약 타일 (+ 얇은 진행 막대) | 홈(outlined, 막대) · 리포트 |
| `tilePair(c, left, right, gap)` | 타일 두 개 나란히 | 홈 · 리포트 |
| `heroCard(c, label, value, sub, large)` | 강조 카드 (서비스 색) | S03 여윳돈 |
| `alertBox(c, title, desc, action, click)` | 빨간 경고 상자 | 홈 결제 감지 꺼짐 |
| `highlightBox(c, label, value[, click])` | 연초록 성과 상자 (click이 있으면 오른쪽 → 화살표, 누르면 이동) | 홈 이번 달 쉼표 → 리포트 |
| `tintNote(c, s, Font, fg, Radius, padH, padV)` | 연초록 안내 한 줄 | S01, 리포트, 개입 재시도 |
| `factBox(c, s)` | 사실 한 줄 (왼쪽 초록 선) | 개입 오버레이 |
| `bar(c, ratio, fill, track[, heightDp])` | 진행 막대 | 홈, 리포트 |
| `splitBar(c, values[], colors[])` | 나눠진 막대 (값 비율대로 색 이어 붙임) | 리포트 가치별 만족/후회 |
| `legendRow(c, color, label, value)` | 색 점 · 이름 · 값 | 리포트 |
| `cardHeader(c, title, desc, link, click)` | 카드 머리말: 굵은 제목 17 · 한 줄 설명 12 · 오른쪽 위 초록 링크(없으면 null). 카드가 무엇을 보여 주고 어디로 갈 수 있는지 알려 준다 | 홈 최근 기록 · 데모, 리포트 카드 전부 |
| `entryBar(c, label, summary, action, click)` | 얇은 흰 테두리 이동 막대: 왼쪽 라벨(초록) + 한 줄 요약, 오른쪽 초록 알약 버튼 "○○ 보기 →". 바탕은 조용히, 버튼만 진하게 (홈 주인공보다 덜 눈에 띄게). 막대 전체도 눌림 | 홈 › 리포트 입구 |
| `heroSentence(c, label, sentence, sub, rows...)` | 강조 카드 + 큰 한 문장 + 아래 줄들(반투명 상자) | 리포트 결제 vs 후회 |
| `heroRow(c, tag, Tone, name, value)` | 강조 카드 안 한 줄: 태그 · 이름 · 금액 | 리포트 결제 vs 후회 |
| `quote(c, said, sub)` | 내가 한 말 인용 (왼쪽 초록 선) | 리포트 그때 내가 한 말 |
| `divider(c)` | 1dp 선 | 기록 화면 |

### 대화 · 내비게이션 · 입력

| 함수 | 모양 |
|---|---|
| `bubbleAi(c, question, faded)` · `bubbleUser(c, answer)` | Bubble/AI(회색, 이름표) · Bubble/User |
| `screenHeader(c, title, back)` | ← 뒤로 + 화면 제목 (기록 · 리포트 · 설정) |
| `screenHeader(c, title, back, right)` | 위와 같고 오른쪽 끝에 부품 하나 (전체 기록의 보기 필터) |
| `filterButton(c, label, click)` | 테두리 알약 "전체 ▾": 지금 고른 값 + ▾, 누르면 메뉴 (전체 기록 보기 필터) |
| `sectionHeader(c, title, link, click)` | 섹션 머리말 + 오른쪽 글자 링크 (홈 "최근 기록 · 더보기 ›") |
| `settingsButton(c, click)` | 톱니바퀴 아이콘 (코드로 그림, `GearDrawable`) |
| `themeToggle(c, click)` | 화면 모드 토글: 밝은 화면이면 해, 어두운 화면이면 달 (코드로 그림, `SunMoonDrawable`) |
| `avatar(c)` | 쉼표 얼굴 (초록 동그라미 "쉼", 개입 채팅 머리줄) |
| `bubbleAi(c, question, faded, bg)` · `bubbleUser(c, answer, bg, fg)` | 얼굴 + 말풍선 · 색 지정 내 답 (개입 채팅: 연초록 시트 위 흰 말풍선) |
| `bottomNav(c, labels, selected, onSelect)` | BottomNav (지금 앱에서는 안 씀) |
| `steps(c, current, total, doneFilled)` | 온보딩 단계 점 |
| `checkRow(c, label, checked, click)` | 체크 상자 + 문구 |
| `input(c, hint, value, number)` | Input (테두리) |
| `plainInput(c, hint, value)` | 테두리 없는 입력칸 |
| `labeledField(c, label, input)` | 라벨 + 고정 폭 입력칸 |

## 홈(S04) 색 규칙

| 자리 | 색 | 정하는 곳 (`MainActivity`) |
|---|---|---|
| 오늘 쓸 수 있는 돈 | 신호 색 바탕: 여유 연초록 · 주의 연노랑 · 초과 연빨강 | `todayBudgetCard()` |
| 여유 / 주의 / 초과 태그 | 초록 / 노랑 / 빨강 | `signalTone()` |
| 남은 여윳돈 · 식비 예산 막대 | 남으면 초록, 다 쓰면 빨강(바탕 연빨강) | `remainColor()` · `remainTrack()` |
| 기록 태그 · 결과 색 | 줄 바탕(TINT), 태그와 같은 연한 색: 멈춤·식비 예산 연초록, 이유 선택 결제 연한 먹색, 숙고 후 결제 연노랑, 나머지 회색 | `outcomeTone()` · `recordRowView()` |

## 개입 채팅 (`OverlayController`)

서비스 색이 보이게: 시트 바탕 연초록(`BRAND_LIGHT`), 쉼표 말풍선 · 입력칸 흰색, 내 답 먹색.
머리줄은 쉼표 얼굴 · "쉼표" · "배민 · 23,000원 결제 전에 잠깐", 진행은 점, X (`headerRow`).
트리거 · 강도("재정위험 전환점", "보통 ●●○") 같은 시스템 용어는 보이지 않는다.
UI 갤러리의 "개입 화면 미리보기"에서 바로 눌러 볼 수 있다.

## 리포트 (`ReportViews.render`)

집계는 `ReportViews.Data`가 화면에서 직접 센다 (core `Stats`는 그대로).
설정 › 테스트 도구 › **① 시나리오 10일째로 맞추기** → **리포트 예시 기록 16건 추가**로 데이터를 넣고 보면 된다 (주별 그래프에 두 주 이상 나온다).

1. **이번 달 결제 vs 후회** (서비스 색 강조 카드): 결제 합계 중 다음 날 '후회돼요'라고 답한 금액 · 비율 (막대 + 후회 / 그 밖의 결제 두 줄).
   아래 **무엇에 썼나요** 카드에 결제 · 후회 원그래프 두 개(`ReportViews.PieChart`, 도넛 모양, 큰 순 5조각 + 나머지는 "그 밖").
   조각을 누르면 그 조각이 커진 채로 남고(나머지는 흐려짐) 가운데에 이름 · 금액 · 비율이 나온다. 같은 조각이나 가운데를 다시 누르면 풀린다 (마우스가 있으면 올리기만 해도 보인다). 그래프를 눌러도 화면은 넘어가지 않고, 그래프 아래 [결제 자세히 보기] [후회 자세히 보기] 버튼으로 **자세히 화면**에 간다 (`MainActivity.spendScreen` → `ReportViews.spendPage`): 합계 → 카테고리별 지출 막대(누르면 그 카테고리만) → 결제 목록.
   카테고리(`ReportViews.Category`)는 앱과 품목 판정으로 나눈다: 배민 → 배달 음식, 식비 예산에서 나간 것 → 식료품, 무신사 → 옷·잡화, 쿠팡 필요 판정 → 생활용품, 나머지 쿠팡 → 그 밖의 쇼핑. 판정 값이 없는 기록은 상품명 키워드(`KeywordJudge.looksNeed`)로 본다.
   계좌 내역은 가맹점 이름으로 외식 · 카페 · 편의점으로 나누고(`Category.ofMerchant`), 모르는 가맹점은 기타 (맨 뒤). 색은 카테고리마다 고정(dataviz 기본 범주 팔레트 순서)이라 원그래프 · 막대 · 범례가 같은 색을 쓴다.
2. **다음 달엔 어떤 결제를 줄이고 싶어요?** (연초록 상자): 이번 달 후회가 남은 가치를 앞에 두고, 고르면 쉼표가 한마디 한다.
3. **쉼표로 아낀 돈**: 멈춘 결제 합계 · "말을 건 N번 중 M번 멈췄어요" · 급여일 기준 주별 막대 그래프(`ReportViews.WeekBars`, 한 가지 색, 막대 위에 값, 이번 주는 굵게, 아직 안 온 주는 흐리게).
4. **돌아본 기록**: 가치 회고 한 문장 + 건수. 제목 줄 오른쪽 위 [더보기 ›]를 누르면 **돌아본 기록 화면**(`MainActivity.historyScreen` → `ReportViews.historyPage`)으로 넘어간다.
   - **무엇 때문에 썼고, 다음 날 어땠나요**: 고른 이유를 가치로 묶어(`ReportViews.Value`: 사람 때문에 · 기분으로 · 대화 끝에) 가치마다 한 칸. 칸 바탕과 태그가 판정 색이다 (만족했어요 연초록 · 후회했어요 연빨강 · 반반이에요 / 아직 몰라요 회색). 만족/후회 막대 · 횟수와 금액 · 그 안의 이유들.
   - **그때 내가 한 말**: 대화에서 직접 적은 가장 긴 답을 인용 (`Record.qa`)
   - **멈춘 결제, 지금은 어때요?** [아직 사고 싶어요] [안 사길 잘했어요]
5. **미리 떼어 둔 돈, 평균과 비교** (맨 아래): 고정지출 5항목 · 마트 장보기 · 쿠팡 식료품마다 평균(= 떼어 둔 금액)과의 차이를 가운데가 평균인 막대로 (`ReportViews.DiffBar`, 더 나가면 오른쪽 빨강 · 덜 나가면 왼쪽 초록, 글자에 +/− 부호).
   식비는 한 달 내내 나가서 "오늘까지 평균 속도"(평균 × 지난 날 비율)와 비교한다. 고정지출의 이번 달 금액은 `BackendSlots.fixedThisMonth`(지금은 데모 값, 계좌 연동 후 채움).

- **필요해서 한 결제는 회고에서 뺀다**: 생필품 · 끼니 이유 칩, AI가 필요(`Judgment.NEED`)로 판정한 결제. 이미 판단 기준이 정해진 것이라 가치판단이 아니다 (차민상).
- 4 · 5는 화면만 있고 저장하지 않는다. 답을 어디에 둘지(Record 구조, `core/Stats` 후회율)는 차민상과 합의 후 연결.
- 이유 → 가치 묶음은 초안이다 (`Value.of`). 기획 확인 필요.

## 화면 메서드 (`MainActivity`)
## 화면 메서드 (`MainActivity`)

| 화면 | 메서드 | 안의 카드 |
|---|---|---|
| S01 권한 | `onboardingPermissionScreen()` | `permissionCard`, `permissionRow` |
| S02 정기 수입 | `onboardingIncomeScreen()` | `linkedAccountCard`, `incomeCard`, `consentCard` |
| S03 예산 | `onboardingBudgetScreen()` | `amountListCard`, heroCard |
| S04 홈 | `homeScreen()` | `homeHeader`(해/달 토글 · 톱니바퀴), `todayBudgetCard`, 리포트 입구(`entryBar` → 리포트), 최근 기록(`cardHeader`, 전체 기록 › → 기록), `recentRecordsCard`, `demoCard` |
| S09 회고 | `showReview()` | `reviewSheetContent` |
| 전체 기록 (홈 › 전체 기록 ›) | `recordsScreen()` | 오른쪽 위 보기 필터(`RecordFilter`: 전체 · 멈춘 결제 · 대화 후 결제 · 후회한 결제 · 말 안 건 결제 · 식비 예산 · 계좌 내역, 메뉴에 건수), `recordRow` |
| S10 리포트 (홈 › 이번 달 쉼표) | `reportScreen()` → `ReportViews.render` | `regretHero`, `piesCard`, `nextMonthCard`, `savedCard`, `historyCard`, `setAsideCard` |
| 리포트 › 결제 · 후회 자세히 | `spendScreen()` → `ReportViews.spendPage` | 합계, `categoryCard`, 결제 목록(`recordRow`) |
| 리포트 › 돌아본 기록 | `historyScreen()` → `ReportViews.historyPage` | `valuesCard`, `saidCard`, `stoppedCard` |
| 설정 (홈 › 톱니바퀴) | `settingsScreen()` | `statusCard`, `numberField`, `testTools`, `logCard`, `screenTextCard` |

하단 탭은 없다. 홈이 기본 화면이고 기록 · 리포트 · 설정은 홈에서 들어가는 하위 화면이다 (`open(PAGE_...)`, ← 뒤로나 기기 뒤로가기로 홈). 리포트의 결제 · 후회 자세히(`PAGE_SPEND`)와 돌아본 기록(`PAGE_HISTORY`)은 뒤로 가면 리포트로 돌아간다.

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
