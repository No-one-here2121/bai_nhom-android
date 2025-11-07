    package com.example.demo_thoi_tiet.Model;

    import android.appwidget.AppWidgetManager;
    import android.appwidget.AppWidgetProvider;
    import android.content.Context;
    import android.content.SharedPreferences;
    import android.widget.RemoteViews;

    import com.example.demo_thoi_tiet.R;

    /**
     * Implementation of App Widget functionality.
     */
    public class Widget_thoi_tiet extends AppWidgetProvider {

        private void updateWidgetViews(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                                       String location, double temperature, double rainPercent,double feelsLike, String weatherDescription, String weatherIcon) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_thoi_tiet);

            views.setTextViewText(R.id.Location_widget, location);
            views.setTextViewText(
                    R.id.Location_degree,
                    String.format("%s %.1f°C", weatherIcon, temperature)
            );
            views.setTextViewText(R.id.Loaction_rain_percent, String.format("Mưa: %.0f%%", rainPercent));
            views.setTextViewText(R.id.Location_feel_like,String.format("Cảm giác như %.1f°C", feelsLike));
            views.setTextViewText(R.id.Location_des, weatherDescription);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }


        @Override
        public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            for (int appWidgetId : appWidgetIds) {
                SharedPreferences prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
                String location = prefs.getString("location", "--");
                int temperature = prefs.getInt("temperature", 0);
                int feelsLike = prefs.getInt("feel", 0);
                int pressure = prefs.getInt("pressure", 0);
                float rainPercent = prefs.getFloat("rainPercent", 0);
                String weatherDescription = prefs.getString("weatherDescription", "--");
                String weatherIcon = prefs.getString("weatherIcon", "");

                updateWidgetViews(context, appWidgetManager, appWidgetId,
                        location, temperature, rainPercent, feelsLike, weatherDescription, weatherIcon);
            }
        }

        @Override
        public void onEnabled(Context context) {
            // Enter relevant functionality for when the first widget is created
        }

        @Override
        public void onDisabled(Context context) {
            // Enter relevant functionality for when the last widget is disabled
        }

        private void updateWidgetViews(String location, double temperature, double rainPercent,double feelsLike, String weatherDescription, String weatherIcon) {

        }
    }