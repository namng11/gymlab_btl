/**
 * File: NotificationReceiver.kt
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà
 * Module: Giao diện Cài đặt thông báo (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Lớp BroadcastReceiver lắng nghe và xử lý báo thức từ AlarmManager.
 * Đảm nhiệm việc hiển thị thông báo ra màn hình và tự động lên lịch đệ quy
 * cho lần tiếp theo nhằm vượt qua giới hạn của Doze Mode.
 */

package com.example.gymlab.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

/**
 * Receiver nhận sự kiện khi đến giờ hẹn báo thức.
 * Phải được đăng ký trong thẻ <receiver> của AndroidManifest.xml.
 */
class NotificationReceiver : BroadcastReceiver() {

    /**
     * Hàm được gọi tự động bởi hệ điều hành khi AlarmManager kích hoạt PendingIntent.
     *
     * @param context Context của ứng dụng.
     * @param intent Intent mang theo dữ liệu (như REPEAT_OPTION) được cấu hình từ lúc đặt báo thức.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gymlab_reminder"

        // 1. Tạo Notification Channel
        // Từ Android 8.0 (API 26) trở lên, thông báo bắt buộc phải có Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở tập luyện",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // 2. Build thông báo (Notification Builder)
        // Thiết lập PRIORITY_HIGH để thông báo có thể pop-up (heads-up) trên màn hình.
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Gymlab nhắc bạn!")
            .setContentText("Đến giờ tập rồi Nam ơi! Cháy hết mình hôm nay nhé 🔥")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // TODO: Cập nhật icon của app (VD: R.mipmap.ic_launcher) khi release
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Tự động ẩn thông báo sau khi người dùng bấm vào
            .build()

        // 3. Kích hoạt hiển thị thông báo
        // Sử dụng notificationId = 1 cố định để các thông báo sau tự ghi đè lên thông báo cũ (nếu chưa xem)
        manager.notify(1, notification)

        // ==========================================
        // 4. LOGIC TỰ ĐỘNG LẶP LẠI (XUYÊN DOZE MODE)
        // ==========================================
        // Lấy tùy chọn lặp lại do NotificationSettingsScreen truyền sang
        val repeatOption = intent.getStringExtra("REPEAT_OPTION") ?: "Một lần"

        // Nếu không phải là "Một lần", tiến hành lên lịch tiếp theo
        if (repeatOption != "Một lần") {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Tạo lại Intent và nhét lại dữ liệu lặp để duy trì vòng lặp đệ quy
            val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("REPEAT_OPTION", repeatOption)
            }

            // Dùng FLAG_UPDATE_CURRENT để ghi đè pending intent hiện tại
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Tính toán mốc thời gian tiếp theo dựa trên chu kỳ đã chọn
            val nextCalendar = Calendar.getInstance().apply {
                if (repeatOption == "Hàng ngày") {
                    add(Calendar.DATE, 1)
                } else if (repeatOption == "Hàng tuần") {
                    add(Calendar.DATE, 7)
                }
            }

            // Tiếp tục đặt báo thức cho lần kế tiếp
            // Tiếp tục sử dụng setExactAndAllowWhileIdle để đảm bảo không bị hệ điều hành đóng băng
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextCalendar.timeInMillis,
                nextPendingIntent
            )
        }
    }
}