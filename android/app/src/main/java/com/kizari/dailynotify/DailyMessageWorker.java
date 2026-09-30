package com.kizari.dailynotify;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DailyMessageWorker extends Worker {
    private static final String PREFS="daily_ai_prefs";
    public DailyMessageWorker(@NonNull Context appContext,@NonNull WorkerParameters workerParams){super(appContext,workerParams);}
    @NonNull @Override public Result doWork(){
        SharedPreferences p=getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(p.getBoolean("enabled",false))DailyMessageReceiver.ensureNextMessage(getApplicationContext());
        return Result.success();
    }
}
