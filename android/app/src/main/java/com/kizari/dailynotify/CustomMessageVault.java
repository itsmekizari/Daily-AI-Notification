package com.kizari.dailynotify;

import java.util.Random;

/**
 * Messages in this file are intentionally not shown anywhere in the app UI.
 * Add or edit entries in MESSAGES only.
 * Each entry is: { Burmese, English }.
 */
public final class CustomMessageVault {

    private CustomMessageVault() {}

    private static final String[][] MESSAGES = new String[][] {
        {"Online  ဖြစ်နေတာတွေ့ပြီး message မပို့ဘဲ detective mode ဝင်နေပြန်ပြီ 😏", "They are online and you still switched to detective mode instead of sending a message. 😏"},
        {"Story ကြည့်ပြီး meaning ရှာနေတာက CSI episode ထက်တောင် serious ဖြစ်နေပြီ 😂", "Reading meaning into a story is getting more serious than a CSI episode. 😂"},
        {"Profile ကို တစ်ခေါက်ကြည့်မယ်ဆိုပြီး ဝင်တာနဲ့ old post တွေအထိရောက်သွားပြီ။ Mission ပျက်ပြီ 😌", "You opened the profile for one quick look and somehow reached the old posts. Mission failed. 😌"},
        {"Reply မလာသေးတာကို universe က test လုပ်နေတယ်လို့ မသတ်မှတ်နဲ့ဦး 😂", "Don't declare it a test from the universe just because there is no reply yet. 😂"},
        {"Typing... မပေါ်ဘဲ imagination ကတော့ full speed နဲ့ run နေတယ် 😏", "There is no typing indicator, but the imagination is running at full speed. 😏"},
        {"Like တစ်ချက်ရတာကို breaking news လုပ်မယ့်အဆင့် ရောက်နေပြီ 🤭", "One like and you're already treating it like breaking news. 🤭"},
        {"Last seen ကို စောင့်ကြည့်တာက notification setting မဟုတ်ဘူးနော် 😂", "Watching the last-seen status is not a notification setting, you know. 😂"},
        {"'မစိတ်ဝင်စားဘူး' လို့ပြောပြီး profile ကို ထပ်ကြည့်တာက logic ရဲ့ final exam ပဲ 😌", "Saying 'I don't care' and then checking the profile again is logic's final exam. 😌"},
        {"Message ရိုက်ပြီး ဖျက်လိုက်တာတွေက draft folder ထဲမှာ အလုပ်များနေပြီ 🤣", "All those typed-then-deleted messages are keeping the draft folder very busy. 🤣"},
        {"Emoji တစ်လုံးကို ၅ မိနစ်ရွေးနေတာက diplomatic summit လောက် serious ဖြစ်နေပြီ 😂", "Spending five minutes choosing one emoji is getting as serious as a diplomatic summit. 😂"},
        {"Story တင်ပြီး views ကြည့်နေတာကို research လို့ခေါ်လို့ရမလား 🤭", "Can checking who viewed a story be called research? 🤭"},
        {"Reply တစ်ကြောင်းအတွက် scenario သုံးဆယ်လောက်ရေးပြီးသားပဲ 😏", "You've already written about thirty scenarios for one possible reply. 😏"},
        {"'ဘာမှမဖြစ်ဘူး' ဆိုပြီး phone ကို တစ်မိနစ်တစ်ခါ စစ်နေတာက ထူးဆန်းတဲ့ coincidence ပါ 😂", "'Nothing is happening' while checking the phone every minute is a fascinating coincidence. 😂"},
        {"Online status တစ်ချက်နဲ့ mood တစ်နေ့လုံး update ဖြစ်သွားတယ်ဆိုရင် software update ထက်မြန်တယ် 😌", "If one online status updates your whole mood, that's faster than a software update. 😌"},
        {"Seen ဖြစ်သွားတာနဲ့ FBI level analysis မလုပ်နဲ့ဦး 🤣", "Getting a seen receipt does not require an FBI-level analysis. 🤣"},
        {"'မစောင့်ဘူး' လို့ပြောပြီး lock screen ကို စစ်တာက classic move ပဲ 😏", "Saying 'I'm not waiting' and checking the lock screen is a classic move. 😏"},
        {"Message notification မဟုတ်ဘဲ Wi-Fi reconnect ဖြစ်တာကိုတောင် အဓိပ္ပါယ်ဖော်နေပြီ 😂", "You're already finding meaning in a Wi-Fi reconnect notification. 😂"},
        {"Friend က 'မကြည့်နဲ့' လို့ပြောတဲ့အရာကို ပိုကြည့်ချင်လာတာ လူသားတို့ရဲ့ feature တစ်ခုပါ 🤭", "Wanting to look more after a friend says 'don't check it' is apparently a human feature. 🤭"},
        {"Reply ရမလားလို့မေးပြီး ကိုယ့်ဘာသာ answer ထုတ်ထားတာက efficiency ကောင်းတယ် 😌", "Asking whether you'll get a reply and then answering yourself is impressive efficiency. 😌"},
        {"Photo တစ်ပုံကို ၁၀ ခေါက်ကြည့်ပြီး 'အဲဒါပုံပဲလေ' လို့ပြောနေတာ suspicious တယ် 😂", "Looking at one photo ten times and saying 'it's just a photo' is suspicious. 😂"},
        {"Chat box ဖွင့်လိုက်၊ ပိတ်လိုက်နဲ့ thumb workout လုပ်နေပြီ 🤣", "Opening and closing the chat box has officially become a thumb workout. 🤣"},
        {"Notification sound မကြားရလို့ စိတ်မချမ်းသာတာက phone မဟုတ်ဘူး expectation ပဲ 😏", "The problem isn't the phone. It's the expectation waiting for a notification. 😏"},
        {"'ဘာမှမမျှော်လင့်ဘူး' ဆိုတဲ့ sentence က usually အများဆုံးမျှော်လင့်နေတဲ့အချိန်ပဲ 😂", "'I expect nothing' is usually said at the exact moment expectations are highest. 😂"},
        {"Old chat ကိုပြန်ဖတ်တာကို archive research လို့ နာမည်ပြောင်းလိုက်ပြီ 🤭", "Re-reading an old chat has officially been renamed archival research. 🤭"},
        {"Profile picture ပြောင်းတာကို press conference လိုမျိုး analyze မလုပ်နဲ့ဦး 😌", "Don't analyze a profile-picture change like it's a press conference. 😌"},
        {"One-word reply ကို paragraph အဓိပ္ပါယ်မထည့်နဲ့ 😂", "Don't turn a one-word reply into a whole paragraph of meaning. 😂"},
        {"Message ပို့ဖို့ courage ရှာနေရင်း battery ကတော့ courage မစောင့်ဘူး 🔋😂", "While you're searching for courage to send the message, the battery is not waiting for it. 🔋😂"},
        {"Typing bubble တစ်စက္ကန့်ပေါ်ပြီး ပျောက်တာကို season finale မလုပ်နဲ့ 🤣", "A typing bubble that appears for one second is not a season finale. 🤣"},
        {"Social media algorithm ကမရပ်ဘူး၊ overthinking algorithm ကလည်း မရပ်ဘူး 😏", "The social-media algorithm never stops, and neither does the overthinking algorithm. 😏"},
        {"Story တစ်ခုနဲ့ အတွေးတစ်ရာထွက်လာတာက productivity ရဲ့ အမိုက်စား version မဟုတ်ဘူး 😂", "Getting a hundred thoughts from one story is not exactly the deluxe version of productivity. 😂"}
    };

    public static int size() {
        return MESSAGES.length;
    }

    public static int randomIndex(Random random) {
        return random.nextInt(MESSAGES.length);
    }

    public static String format(int index, String language) {
        String my = MESSAGES[index][0];
        String en = MESSAGES[index][1];
        if ("Burmese only".equals(language)) return my;
        if ("English only".equals(language)) return en;
        return my + "\n\n" + en;
    }
}
