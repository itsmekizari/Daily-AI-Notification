package com.kizari.dailynotify;

public final class FallbackMessages {
    private FallbackMessages() {}

    public static String[] get(String category) {
        if ("Good Morning".equals(category)) {
            return new String[]{"မင်္ဂလာမနက်ခင်းပါ 🌞 ဒီနေ့လည်း အေးအေးဆေးဆေး စလိုက်နော်!", "Good morning 🌞 Start today calmly and confidently!"};
        }
        if ("Good Night".equals(category)) {
            return new String[]{"ညချမ်းကောင်းပါစေ 🌙 မနက်ဖြန်အတွက် အားပြန်ဖြည့်လိုက်နော်!", "Good night 🌙 Rest up for tomorrow!"};
        }
        if ("Study Reminder".equals(category)) {
            return new String[]{"စာနည်းနည်းလေး လုပ်လိုက်ဦး 📚 ၁၅ မိနစ်တောင် ကောင်းပါတယ်!", "A little study time 📚 even 15 minutes counts!"};
        }
        if ("Motivational".equals(category)) {
            return new String[]{"တစ်ဆင့်ချင်းသွားရင်လည်း ရောက်ပါတယ် 💪", "One step at a time still gets you there 💪"};
        }
        if ("Cute".equals(category)) {
            return new String[]{"ဒီနေ့လည်း ပြုံးဖို့ အကြောင်းလေးတစ်ခု ရှာပါ 😊", "Find one tiny reason to smile today 😊"};
        }
        if ("Funny".equals(category)) {
            return new String[]{"ဒီ notification ကိုဖတ်ပြီး အလုပ်လုပ်သလိုမျိုး လုပ်ထားလိုက် 😌", "Read this notification and pretend you were productive 😌"};
        }
        if ("Teasing".equals(category)) {
            return new String[]{"Notification တောင် ရောက်လာပြီ၊ မင်းကတော့ အခုထိမလှုပ်သေးဘူးနော် 😏", "Even the notification arrived. You still haven't moved 😏"};
        }
        return new String[]{"ဒီနေ့အတွက် message လေးတစ်စောင် ရောက်လာပြီ ✨", "A little message for today ✨"};
    }
}
