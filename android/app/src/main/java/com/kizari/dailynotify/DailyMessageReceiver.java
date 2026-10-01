package com.kizari.dailynotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class DailyMessageReceiver extends BroadcastReceiver {
    private static final String PREFS="daily_ai_prefs";
    private static final String ALARM_ACTION="com.kizari.dailynotify.DAILY_ALARM";
    private static final int ALARM_REQUEST_CODE=1001;

    @Override public void onReceive(Context context, Intent intent){
        if(context==null)return;
        Context app=context.getApplicationContext();
        SharedPreferences p=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean("enabled",false))return;

        int index=ensureNextIndex(app);
        String language=p.getString("language","Burmese + English");
        String message=formatIndex(app,index,language);
        NotificationHelper.show(app,"Daily AI Notification",message);

        p.edit().remove("next_index").apply();
        ensureNextMessage(app);
        scheduleNext(app);
    }

    public static void ensureNextMessage(Context context){
        if(context==null)return;
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(p.getBoolean("enabled",false))ensureNextIndex(context.getApplicationContext());
    }

    public static String peekNextText(Context context,String language){
        if(context==null)return null;
        int index=ensureNextIndex(context.getApplicationContext());
        return formatIndex(context.getApplicationContext(),index,language);
    }

    private static String formatIndex(Context context,int index,String language){
        if(index<LocalMessageBank.SIZE){
            return LocalMessageBank.format(index,language);
        }
        List<String> custom=CustomMessageStore.getAll(context);
        int customIndex=index-LocalMessageBank.SIZE;
        if(customIndex>=0 && customIndex<custom.size())return custom.get(customIndex);
        return LocalMessageBank.format(0,language);
    }

    private static int poolSize(Context context){
        return LocalMessageBank.SIZE + CustomMessageStore.getCount(context);
    }

    private static int ensureNextIndex(Context context){
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        int size=poolSize(context);

        String next=p.getString("next_index","");
        if(next!=null&&!next.trim().isEmpty()){
            try{
                int i=Integer.parseInt(next.trim());
                if(i>=0&&i<size)return i;
            }catch(NumberFormatException ignored){}
        }

        String order=p.getString("remaining_order","");
        int cursor=p.getInt("remaining_cursor",0);
        int[] values;

        if(order==null||order.trim().isEmpty()||cursor>=size){
            values=newShuffledOrder(size);
            order=join(values);
            cursor=0;
        }else{
            values=parse(order,size);
            if(values.length!=size||cursor<0||cursor>=values.length){
                values=newShuffledOrder(size);
                order=join(values);
                cursor=0;
            }
        }

        int index=values[cursor];
        p.edit()
                .putString("remaining_order",order)
                .putInt("remaining_cursor",cursor+1)
                .putString("next_index",String.valueOf(index))
                .apply();
        return index;
    }

    private static int[] newShuffledOrder(int size){
        ArrayList<Integer> values=new ArrayList<>(size);
        for(int i=0;i<size;i++)values.add(i);
        Collections.shuffle(values,new SecureRandom());
        int[] result=new int[size];
        for(int i=0;i<size;i++)result[i]=values.get(i);
        return result;
    }

    private static String join(int[] values){
        StringBuilder b=new StringBuilder(values.length*5);
        for(int i=0;i<values.length;i++){
            if(i>0)b.append(',');
            b.append(values[i]);
        }
        return b.toString();
    }

    private static int[] parse(String value,int size){
        String[] parts=value.split(",");
        if(parts.length!=size)return new int[0];
        int[] r=new int[parts.length];
        boolean[] seen=new boolean[size];
        try{
            for(int i=0;i<parts.length;i++){
                r[i]=Integer.parseInt(parts[i]);
                if(r[i]<0||r[i]>=size||seen[r[i]])return new int[0];
                seen[r[i]]=true;
            }
            return r;
        }catch(NumberFormatException e){
            return new int[0];
        }
    }

    public static void scheduleNext(Context context){
        if(context==null)return;
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean("enabled",false))return;

        int hour=p.getInt("hour",8), minute=p.getInt("minute",0);
        Calendar c=Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,hour);
        c.set(Calendar.MINUTE,minute);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);

        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        PendingIntent pi=getPendingIntent(context);
        long at=c.getTimeInMillis();
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                if(am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            }else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }catch(SecurityException e){
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }
    }

    private static PendingIntent getPendingIntent(Context context){
        Intent i=new Intent(context,DailyMessageReceiver.class);
        i.setAction(ALARM_ACTION);
        return PendingIntent.getBroadcast(context,ALARM_REQUEST_CODE,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancel(Context context){
        if(context==null)return;
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am!=null)am.cancel(getPendingIntent(context));
    }
}
