package com.kizari.dailynotify;

public class FallbackMessages {
    private FallbackMessages() {}

    public static String get(String category) {
        if ("Good Morning".equals(category)) {
            return "မနက်ခင်းကောင်းပါစေ ☀️\n\nGood morning! Make today count.";
        }
        if ("Good Night".equals(category)) {
            return "ညချမ်းသာပါစေ 🌙\n\nGood night! Rest well for tomorrow.";
        }
        if ("Motivational".equals(category)) {
            return "တစ်ဆင့်ချင်း သွားပါ 💪\n\nKeep going, one step at a time.";
        }
        return "ဒီနေ့အတွက် message လေးတစ်ခု 😊\n\nHere is your little daily reminder.";
    }
}
