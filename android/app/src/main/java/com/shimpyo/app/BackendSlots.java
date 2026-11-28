package com.shimpyo.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 백엔드가 아직 주지 않아서 화면에 임시(데모) 값을 넣어 둔 자리.
 *
 * 화면 코드는 이 객체에서만 값을 읽는다. 백엔드(계좌 연동, 알림 권한 확인 등)가 생기면
 * load() 안에서 받아 온 값으로 필드를 채우면 되고, 화면 코드는 고칠 필요가 없다.
 *
 * 지금 값은 PRD의 데모 시나리오(수입 110만 원 = 용돈 60만 + 알바비 50만)에 맞춘 Mock이다.
 * 예산·기록처럼 이미 Store / core 에서 오는 값은 여기 두지 않는다.
 */
final class BackendSlots {

    /** 연결된 계좌 (S02 "연결된 계좌" 카드). S02에서 은행을 고르고 은행 앱 연결 화면에서 동의하면 채워진다. */
    static final class Account {
        String bank;                      // Banks.Bank.name
        String kind = "입출금 통장";
        String maskedNumber = "****1234";

        Account(String bank, String kind, String maskedNumber) {
            this.bank = bank;
            this.kind = kind;
            this.maskedNumber = maskedNumber;
        }

        String displayName() {
            return bank + " " + kind + " " + maskedNumber;
        }
    }

    /** 정기 수입 한 건 (S02 "정기 수입" 카드의 내역). */
    static final class IncomeSource {
        final String label;
        final long amount;

        IncomeSource(String label, long amount) {
            this.label = label;
            this.amount = amount;
        }
    }

    // ------------------------------------------------------------------ 백엔드가 채울 값

    /** 연결된 계좌. 연결 전이면 null (S02가 은행 고르기 격자를 보여 준다). */
    Account account = null;

    /** 정기 수입 내역. 합계가 설정의 월 수입과 같을 때만 화면에 내역을 보여 준다. */
    List<IncomeSource> incomeSources = new ArrayList<>();

    /** 정기 수입을 찾을 때 보는 기간이자, 같은 날 연속으로 들어온 개월 수 (최근 3개월). */
    int incomeStreakMonths = 3;

    /** 알림 권한 허용 여부 (S01 "알림" 줄의 ✓). 알림 기능이 생기면 실제 값으로. */
    boolean notificationAllowed = false;

    BackendSlots() {
        incomeSources.add(new IncomeSource("용돈", 600_000));
        incomeSources.add(new IncomeSource("알바비", 500_000));
    }

    // ------------------------------------------------------------------ 불러오기

    private static BackendSlots current = new BackendSlots();

    static BackendSlots get() {
        return current;
    }

    /**
     * 백엔드에서 값을 받아 온다. 지금은 아무것도 하지 않고 데모 값을 그대로 쓴다.
     * 연결할 때 예시:
     * <pre>
     *   BackendSlots d = new BackendSlots();
     *   d.account.bank = 응답.bankName;
     *   d.incomeSources.clear();
     *   for (...) d.incomeSources.add(new IncomeSource(이름, 금액));
     *   current = d;
     * </pre>
     * 네트워크 호출이라면 백그라운드 스레드에서 부르고, 끝나면 화면의 refresh()를 다시 불러야 한다.
     */
    static void load(Context c) {
        // TODO 백엔드 연결
    }

    /** 수입 합계 (내역이 월 수입과 맞는지 확인할 때 쓴다). */
    long incomeTotal() {
        long t = 0;
        for (IncomeSource s : incomeSources) t += s.amount;
        return t;
    }
}
