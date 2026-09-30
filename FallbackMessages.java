package com.kizari.dailynotify;

import android.content.*;
import java.security.*;
import java.util.*;

public class FallbackMessages {
    private static final String PREF = "fallback_used";

    private static final String[][] DATA = {
        {"Teasing","ဒီနေ့လည်း notification တောင် ငါက အရင်လာရတယ်နော် 🤭","Even your notification gets here before you do 🤭"},
        {"Funny","ဒီ notification ကို ignore လုပ်ရင် မနက်ဖြန် ထပ်လာမယ်နော် 😂","Ignore this and tomorrow's one will still show up 😂"},
        {"Cute","ဒီနေ့အတွက် cute reminder လေးရောက်လာပြီ 🌸","Your little cute reminder has arrived 🌸"},
        {"Motivational","နည်းနည်းချင်းစီလုပ်ရင်လည်း ရောက်တဲ့နေရာရောက်တယ် 💪","Small progress still counts. Keep going 💪"},
        {"Good Morning","မနက်ခင်းကောင်းပါစေ ☀️ ဒီနေ့ကို အေးအေးဆေးဆေးစလိုက်။","Good morning ☀️ Start today calmly."},
        {"Good Night","ညကောင်းကောင်းအိပ်ပါ 🌙 မနက်ဖြန်အတွက် energy ချန်ထား။","Good night 🌙 Save some energy for tomorrow."},
        {"Study Reminder","စာနည်းနည်းလောက်လုပ်လိုက်ဦး 📚 မနက်ဖြန်ကိုယ်တိုင်ကို ကျေးဇူးတင်လိမ့်မယ်။","Study a little 📚 Tomorrow-you will thank you."},
        {"Random","ဒီနေ့အတွက် random reminder တစ်ခု ရောက်လာပြီ 😌","A random reminder for today just arrived 😌"}
    };

    public static String[] next(Context context, String category) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < DATA.length; i++) {
            if (DATA[i][0].equals(category) || "Random".equals(category)) {
                candidates.add(i);
            }
        }

        if (candidates.isEmpty()) candidates.add(0);

        android.content.SharedPreferences p =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        List<Integer> unused = new ArrayList<>();
        for (int i : candidates) {
            if (!p.getBoolean("u_" + hash(DATA[i][1]), false)) unused.add(i);
        }
        if (unused.isEmpty()) {
            p.edit().clear().apply();
            unused = candidates;
        }

        int pick = unused.get(new Random().nextInt(unused.size()));
        p.edit().putBoolean("u_" + hash(DATA[pick][1]), true).apply();

        return new String[]{DATA[pick][1], DATA[pick][2]};
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder x = new StringBuilder();
            for (byte v : b) x.append(String.format("%02x", v));
            return x.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
