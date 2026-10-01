package com.kizari.dailynotify;

/** Local message bank. No network/API call is needed for notifications. */
public final class LocalMessageBank {
    public static final int SIZE = 10000;
    private LocalMessageBank() {}

    public static String format(int index, String language) {
        if (index < 0) index = 0;
        index %= SIZE;
        int part = index / 1000;
        int offset = index % 1000;
        String my = my(part, offset);
        String en = en(part, offset);
        if ("Burmese only".equals(language)) return my;
        if ("English only".equals(language)) return en;
        return my + "\n\n" + en;
    }

    private static String my(int part, int i) {
        switch (part) {
            case 0: return LocalMessageBankPart01.MY[i];
            case 1: return LocalMessageBankPart02.MY[i];
            case 2: return LocalMessageBankPart03.MY[i];
            case 3: return LocalMessageBankPart04.MY[i];
            case 4: return LocalMessageBankPart05.MY[i];
            case 5: return LocalMessageBankPart06.MY[i];
            case 6: return LocalMessageBankPart07.MY[i];
            case 7: return LocalMessageBankPart08.MY[i];
            case 8: return LocalMessageBankPart09.MY[i];
            default: return LocalMessageBankPart10.MY[i];
        }
    }

    private static String en(int part, int i) {
        switch (part) {
            case 0: return LocalMessageBankPart01.EN[i];
            case 1: return LocalMessageBankPart02.EN[i];
            case 2: return LocalMessageBankPart03.EN[i];
            case 3: return LocalMessageBankPart04.EN[i];
            case 4: return LocalMessageBankPart05.EN[i];
            case 5: return LocalMessageBankPart06.EN[i];
            case 6: return LocalMessageBankPart07.EN[i];
            case 7: return LocalMessageBankPart08.EN[i];
            case 8: return LocalMessageBankPart09.EN[i];
            default: return LocalMessageBankPart10.EN[i];
        }
    }
}
