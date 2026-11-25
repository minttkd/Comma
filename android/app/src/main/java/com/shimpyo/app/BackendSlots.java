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
 * 지금 값은 PRD의 데모 시나리오(수입 100만 원 = 용돈 60만 + 과외비 40만)에 맞춘 Mock이다.
 * 예산·기록처럼 이미 Store / core 에서 오는 값은 여기 두지 않는다.
 */
final class BackendSlots {

    /** 연결된 계좌 (S02 "연결된 계좌" 카드). */
    static final class Account {
        String bank = "쉼표은행";
        String kind = "입출금";
        String maskedNumber = "****1234";
        boolean demo = true;              // true면 이름 뒤에 "(데모)"를 붙인다

        String displayName() {
            return bank + " " + kind + " " + maskedNumber + (demo ? " (데모)" : "");
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

    /** 연결된 계좌. 연결 전이면 null. */
    Account account = new Account();

    /** 정기 수입 내역. 합계가 설정의 월 수입과 같을 때만 화면에 내역을 보여 준다. */
    List<IncomeSource> incomeSources = new ArrayList<>();

    /** 정기 수입이 몇 개월 연속 같은 날 들어왔는지. */
    int incomeStreakMonths = 6;

    /** 알림 권한 허용 여부 (S01 "알림" 줄의 ✓). 알림 기능이 생기면 실제 값으로. */
    boolean notificationAllowed = false;

    /**
     * 이번 달 계좌에서 실제로 나간 고정지출 (리포트 "미리 떼어 둔 돈" 그래프).
     * 순서는 월세·관리비 · 공과금 · 통신비 · 교통비 · 구독 (Settings 고정지출 5항목과 같다).
     * 아직 청구되지 않은 항목은 -1. 계좌 동기화가 생기면 가맹점 분류 결과로 채운다.
     */
    long[] fixedThisMonth = {350_000, 62_000, 45_000, 33_000, 29_000};
    /** true면 fixedThisMonth가 데모 값이라 화면에 "(데모)"를 붙인다. */
    boolean fixedDemo = true;

    BackendSlots() {
        incomeSources.add(new IncomeSource("용돈", 600_000));
        incomeSources.add(new IncomeSource("과외비", 400_000));
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
