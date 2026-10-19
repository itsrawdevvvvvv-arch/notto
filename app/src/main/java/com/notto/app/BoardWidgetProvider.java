package com.notto.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

/** Home-screen widget showing the most recent moodboard. */
public class BoardWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_board);
            ChecklistWidgetProvider.bindClick(context, v);

            String json = WidgetData.get(context);
            JSONObject board = null;
            try {
                board = new JSONObject(json).optJSONObject("board");
            } catch (Exception ignored) {
            }

            Bitmap thumb = null;
            if (board != null) {
                String data = board.optString("thumb", "");
                int comma = data.indexOf(',');
                if (data.startsWith("data:") && comma > 0) {
                    try {
                        byte[] bytes = Base64.decode(data.substring(comma + 1), Base64.DEFAULT);
                        Bitmap raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (raw != null) {
                            int max = 300;
                            float k = Math.min(1f, (float) max / Math.max(raw.getWidth(), raw.getHeight()));
                            thumb = Bitmap.createScaledBitmap(raw,
                                    Math.max(1, Math.round(raw.getWidth() * k)),
                                    Math.max(1, Math.round(raw.getHeight() * k)), true);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (board == null || thumb == null) {
                v.setViewVisibility(R.id.w_thumb, View.GONE);
                v.setTextViewText(R.id.w_title,
                        board == null ? "No moodboard yet" : board.optString("title", "Moodboard"));
            } else {
                v.setViewVisibility(R.id.w_thumb, View.VISIBLE);
                v.setImageViewBitmap(R.id.w_thumb, thumb);
                v.setTextViewText(R.id.w_title, board.optString("title", "Moodboard"));
            }
            manager.updateAppWidget(id, v);
        }
    }
}
