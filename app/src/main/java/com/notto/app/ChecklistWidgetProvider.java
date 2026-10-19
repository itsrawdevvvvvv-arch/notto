package com.notto.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/** Home-screen widget showing the most recent checklist. */
public class ChecklistWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_checklist);
            bindClick(context, v);

            String json = WidgetData.get(context);
            JSONObject todo = null;
            try {
                JSONObject o = new JSONObject(json);
                todo = o.optJSONObject("todo");
            } catch (Exception ignored) {
            }

            int[] rowIds = {R.id.w_r1, R.id.w_r2, R.id.w_r3, R.id.w_r4, R.id.w_r5};
            for (int rid : rowIds) {
                v.setViewVisibility(rid, View.GONE);
            }

            if (todo == null) {
                v.setTextViewText(R.id.w_title, "No checklist yet");
                v.setViewVisibility(R.id.w_empty, View.VISIBLE);
            } else {
                v.setViewVisibility(R.id.w_empty, View.GONE);
                v.setTextViewText(R.id.w_title, todo.optString("title", "Checklist"));
                JSONArray items = todo.optJSONArray("items");
                for (int i = 0; i < rowIds.length && items != null && i < items.length(); i++) {
                    JSONObject it = items.optJSONObject(i);
                    if (it == null) {
                        continue;
                    }
                    boolean done = it.optInt("d", 0) == 1;
                    String text = (done ? "\u2713  " : "\u25CB  ") + it.optString("t", "");
                    v.setTextViewText(rowIds[i], text);
                    v.setViewVisibility(rowIds[i], View.VISIBLE);
                }
            }
            manager.updateAppWidget(id, v);
        }
    }

    static void bindClick(Context context, RemoteViews v) {
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        v.setOnClickPendingIntent(R.id.widget_root,
                PendingIntent.getActivity(context, 0, open, flags));
    }
}
