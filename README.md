# 쉼표 (Comma)

> 결제 버튼을 누르는 순간, 한 번 더 생각하게 하는 AI 질문

배달의민족 · 쿠팡 · 무신사에서 **오늘 한도를 넘는 소비에만** 말을 거는 Android 앱입니다. 결제를 막지 않고, 결제 버튼을 누른 순간 강도별 AI 질문(가벼움 · 보통 · 강함)으로 스스로 다시 생각하게 합니다.

숭실대학교 글로벌미디어학부 창의프로젝트 · 19조 차민상 · 안강원

## 바로 보기

| 자료 | 위치 |
|---|---|
| 중간점검 발표자료 (12장) | [`presentation/쉼표_중간점검_발표.pdf`](presentation/쉼표_중간점검_발표.pdf) · 원본 [`.pptx`](presentation/쉼표_중간점검_발표.pptx) |
| Figma 하이파이 프로토타입 (21화면) | [figma.com/design/NCFIvnfUcQ8BBIgFgkBOfE](https://www.figma.com/design/NCFIvnfUcQ8BBIgFgkBOfE?node-id=31-73) — Hi-Fi 프로토타입 › 쉼표 와이어프레임 › ▶ Present |
| FigJam User Flow | [figma.com/board/urLBlIngX8aXahBFmfaj4W](https://www.figma.com/board/urLBlIngX8aXahBFmfaj4W) |
| Android 프로토타입 APK (v0.31, 주의 상태 게이지 신호등 황색) | [`apk/shimpyo-v0.31.apk`](apk/shimpyo-v0.31.apk) · 이전 버전 [`v0.30`](apk/shimpyo-v0.30.apk) · [`v0.29`](apk/shimpyo-v0.29.apk) · [`v0.28`](apk/shimpyo-v0.28.apk) · [`v0.27`](apk/shimpyo-v0.27.apk) · [`v0.26`](apk/shimpyo-v0.26.apk) · [`v0.25`](apk/shimpyo-v0.25.apk) · [`v0.24`](apk/shimpyo-v0.24.apk) · [`v0.23`](apk/shimpyo-v0.23.apk) · [`v0.22`](apk/shimpyo-v0.22.apk) · [`v0.21`](apk/shimpyo-v0.21.apk) · [`v0.20`](apk/shimpyo-v0.20.apk) · [`v0.19`](apk/shimpyo-v0.19.apk) · [`v0.18`](apk/shimpyo-v0.18.apk) · [`v0.17`](apk/shimpyo-v0.17.apk) · [`v0.16`](apk/shimpyo-v0.16.apk) · [`v0.15`](apk/shimpyo-v0.15.apk) · [`v0.14`](apk/shimpyo-v0.14.apk) · [`v0.13`](apk/shimpyo-v0.13.apk) · [`v0.12`](apk/shimpyo-v0.12.apk) · [`v0.11`](apk/shimpyo-v0.11.apk) · [`v0.10`](apk/shimpyo-v0.10.apk) · [`v0.9`](apk/shimpyo-v0.9.apk) · [`v0.8`](apk/shimpyo-v0.8.apk) · [`v0.7`](apk/shimpyo-v0.7.apk) · [`v0.6`](apk/shimpyo-v0.6.apk) · [`v0.5`](apk/shimpyo-v0.5.apk) · [`v0.4`](apk/shimpyo-v0.4.apk) · [`v0.3`](apk/shimpyo-v0.3.apk) |
| 실행 · 테스트 가이드 | [`android/README_실행·테스트.md`](android/README_실행·테스트.md) |

## 폴더 구조

```
comma/
├── android/        Android 프로토타입 소스 (Java · 접근성 서비스 · 결제 버튼 가드 · 오버레이 질문)
│   └── app/src/test/…/EngineTest.java   예산 · 트리거 엔진 단위 테스트 31개
├── apk/            빌드된 APK (shimpyo-v0.31 최신, v0.30, v0.29, v0.28, v0.27, v0.26, v0.25, v0.24, v0.23, v0.22, v0.21, v0.20, v0.19, v0.18, v0.17, v0.16, v0.15, v0.14, v0.13, v0.12, v0.11, v0.10, v0.9, v0.8, v0.7, v0.6, v0.5, v0.4, v0.3)
├── docs/           기획 · 설계 문서
│   ├── 쉼표_PRD.md
│   ├── 쉼표_6주차_시나리오명세.pdf
│   ├── 쉼표_7주차_UserFlow_화면구조.pdf
│   ├── 쉼표_6-7주차_설계.md
│   └── 쉼표_중간점검_설계.md        아키텍처 · API · 데이터 흐름 · ERD · 개발 계획 · 리스크
├── presentation/   중간점검 발표자료 (.pptx, .pdf)
└── archive/        이전 단계 자료
    ├── 배달브레이크/   쉼표의 이전 이름일 때의 기획 · PRD · 디자인 레퍼런스 · 발표 대본
    └── 초기_기획/      주제 선정 · 서비스 계획 수립 · 핵심 기술 테스트(ImpulseGuard) 자료
```

## 핵심 아이디어

- **결제 버튼 가드** — 결제 화면에 들어온 것만으로는 개입하지 않고, 결제 버튼 위 투명 가드가 터치를 받은 순간의 최종 금액으로 한 번만 판단
- **예산 엔진** — 여윳돈 = 수입 − 고정지출 − 식비, 하루 한도 = min(남은 돈 ÷ 남은 날, 2 × 기준액)
- **트리거 4종** — 재정위험 전환점 · 고액 단건(돈) / 몰아치기 · 야식 반복(행동 패턴)
- **강도별 질문** — 가벼움 이유 칩 1개 · 보통 질문 2개 · 강함 질문 3개(1개 직접 입력). [그만둘게요]는 크게, [결제 계속]은 없음
- **핵심 지표** — 마음 돌림 비율 = 방지 ÷ 개입

## 빌드

Android Studio에서 `android/` 폴더를 열고 ▶ Run. 자세한 에뮬레이터 설정과 시나리오 테스트 방법은 [`android/README_실행·테스트.md`](android/README_실행·테스트.md)를 보세요.

Claude API 키는 저장소에 들어 있지 않습니다. 앱 설정 화면에서 직접 입력하고, 비워 두면 기본 질문 · 키워드 판정으로 동작합니다.
