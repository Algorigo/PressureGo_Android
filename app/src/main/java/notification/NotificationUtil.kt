package notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.algorigo.pressuregoapp.R
import service.CSVRecordService

object NotificationUtil {

    enum class NotificationType(
        @StringRes val channelId: Int,
        @StringRes val channelName: Int,
        val importance: Int,
    ) {
        CSVRecordChannel(R.string.csv_record_channel_id, R.string.csv_record_channel_name, NotificationManagerCompat.IMPORTANCE_LOW)
    }

    @JvmStatic
    fun createCSVRecordChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService<NotificationManager>()

            NotificationType.CSVRecordChannel.let {
                NotificationChannel(context.getString(it.channelId), context.getString(it.channelName), it.importance)
            }.let {
                manager?.createNotificationChannel(it)
            }
        }
    }

    @JvmStatic
    fun getNotificationChannel(context: Context, type: NotificationType, @StringRes titleRes: Int, @StringRes contentRes: Int, @DrawableRes iconRes: Int, pendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context,
            context.getString(type.channelId))
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(contentRes))
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .build()
    }

    @JvmStatic
    fun sendNotification(context: Context) {

    }
}