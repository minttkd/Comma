package com.shimpyo.app;

import com.shimpyo.app.core.Settings;

/**
 * 데모 페르소나: S02에서 고른 은행으로 누구의 계좌인지 정한다.
 *
 * - IBK기업 → 사회초년생 지수 (월급 210만, 고정 95만, 식비 25만 → 여윳돈 90만)
 * - 그 밖의 은행 → 대학생 수민 (용돈 60만 + 알바비 50만, 고정 62.5만, 식비 10만 → 여윳돈 37.5만, Settings 기본값)
 *
 * 실제 오픈뱅킹 연동 전이라 계좌 목록 · 정기 수입 · 예산을 여기서 채운다.
 * 이번 주기 지출 같은 기록은 온보딩을 마칠 때 MainActivity.seedPersona()가 넣는다.
 */
final class Personas {
    private Personas() {}

    enum P { SUMIN, JISU }

    static P forBank(Banks.Bank b) {
        return b != null && "IBK기업".equals(b.name) ? P.JISU : P.SUMIN;
    }

    static P forAccount(BackendSlots.Account a) {
        return a == null ? P.SUMIN : forBank(Banks.byName(a.bank));
    }

    /** 은행 앱 연결 화면의 계좌 목록: {종류, 번호, 설명}. 첫 줄이 기본 선택. */
    static String[][] accounts(P p) {
        if (p == P.JISU) return new String[][]{
                {"급여 통장", "****7788", "매달 25일 월급이 들어와요"},
                {"청년 적금", "****2040", "매달 자동이체"},
        };
        return new String[][]{
                {"입출금 통장", "****1234", "매달 용돈·알바비가 들어와요"},
                {"생활비 통장", "****5678", "자동이체 · 카드값"},
        };
    }

    /** 정기 수입 내역만 (앱을 다시 켰을 때 저장된 계좌로 되살릴 때). */
    static void applyIncome(P p, BackendSlots d) {
        d.incomeSources.clear();
        if (p == P.JISU) {
            d.incomeSources.add(new BackendSlots.IncomeSource("월급", 2_100_000));
        } else {
            d.incomeSources.add(new BackendSlots.IncomeSource("용돈", 600_000));
            d.incomeSources.add(new BackendSlots.IncomeSource("알바비", 500_000));
        }
    }

    /** 계좌를 연결하면: 정기 수입 내역(BackendSlots)과 예산(Settings)을 그 사람 것으로. */
    static void apply(P p, BackendSlots d, Settings s) {
        applyIncome(p, d);
        Settings def = new Settings();
        s.payday = 25;
        if (p == P.JISU) {
            s.income = 2_100_000;
            s.rent = 650_000;
            s.utilities = 100_000;
            s.phone = 70_000;
            s.transport = 80_000;
            s.subscriptions = 50_000;
            s.martBudget = 150_000;
            s.coupangFoodBudget = 100_000;
        } else {
            s.income = def.income;
            s.rent = def.rent;
            s.utilities = def.utilities;
            s.phone = def.phone;
            s.transport = def.transport;
            s.subscriptions = def.subscriptions;
            s.martBudget = def.martBudget;
            s.coupangFoodBudget = def.coupangFoodBudget;
        }
    }
}
