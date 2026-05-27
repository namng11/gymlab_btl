/**
 * File: NotificationSettingsScreen.kt
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà
 * Module: Giao diện Cài đặt thông báo (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Màn hình thiết lập tính năng nhắc nhở tập luyện.
 * Xử lý trực tiếp các quyền (Permissions) trên Android 13+ và lên lịch
 * bằng AlarmManager (hỗ trợ Android 12+).
 */

package com.example.gymlab.ui

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gymlab.notification.NotificationReceiver
import com.example.gymlab.ui.theme.PrimaryPurple
import java.util.*

/**
 * Màn hình Cài đặt thông báo nhắc nhở on-device.
 *
 * @param userId ID người dùng hiện tại.
 * @param onBackClick Callback quay lại màn hình trước.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    userId: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // Khởi tạo Launcher xin quyền hiển thị thông báo (Dành cho Android 13 / API 33 trở lên)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                android.widget.Toast.makeText(context, "Cần cấp quyền để nhận thông báo!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Chạy kiểm tra và xin quyền tự động khi vừa mở màn hình
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // State quản lý Bật/Tắt thông báo
    var isEnabled by remember { mutableStateOf(true) }

    // State lưu giờ nhắc nhở (Định dạng HH:mm)
    var reminderTime by remember { mutableStateOf("08:00") }

    // State cho tùy chọn tần suất lặp lại
    val repeatOptions = listOf("Một lần", "Hàng ngày", "Hàng tuần")
    var expanded by remember { mutableStateOf(false) } // State mở dropdown menu
    var selectedRepeat by remember { mutableStateOf(repeatOptions[1]) } // Mặc định: Hàng ngày

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt nhắc nhở", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .padding(24.dp)
        ) {
            // 1. Module: Switch Bật/Tắt thông báo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Nhắc nhở tập luyện", modifier = Modifier.weight(1f), fontSize = 16.sp)
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }
            }

            // Chỉ hiển thị phần cài đặt thời gian nếu đã bật Switch
            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Module: Chọn Thời gian (Sử dụng TimePickerDialog native của Android)
                Card(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(context, { _, h, m ->
                            reminderTime = String.format("%02d:%02d", h, m)
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Thời gian", fontSize = 12.sp, color = Color.Gray)
                            Text(reminderTime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Module: Chọn Lặp lại (Sử dụng ExposedDropdownMenu)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lặp lại", fontSize = 12.sp, color = Color.Gray)
                                Text(selectedRepeat, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }

                    // Danh sách Dropdown xổ xuống
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        repeatOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedRepeat = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Nút Lưu cài đặt (Áp dụng hoặc Hủy lịch báo thức vào hệ thống)
            Button(
                onClick = {
                    if (isEnabled) {
                        scheduleNotification(context, reminderTime, selectedRepeat)
                        android.widget.Toast.makeText(context, "Đã lưu nhắc nhở $selectedRepeat lúc $reminderTime", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        cancelNotification(context)
                    }
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("LƯU CÀI ĐẶT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =====================================================================
// --- LOGIC LẬP LỊCH ALARM MANAGER NÂNG CAO ---
// =====================================================================

/**
 * Đăng ký một lịch hẹn giờ với AlarmManager của Android.
 *
 * @param context Context của ứng dụng.
 * @param timeString Giờ nhắc nhở định dạng "HH:mm".
 * @param repeatOption Tùy chọn lặp lại, được truyền qua Broadcast Receiver qua Intent Extra.
 */
private fun scheduleNotification(context: Context, timeString: String, repeatOption: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Bắt buộc kiểm tra quyền báo thức chính xác trên Android 12+ (API 31)
    // Nếu chưa cấp, điều hướng người dùng mở màn hình Cài đặt hệ thống.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
        return
    }

    // Đóng gói Intent trỏ tới BroadcastReceiver
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("REPEAT_OPTION", repeatOption)
    }

    // Sử dụng FLAG_UPDATE_CURRENT để cập nhật PendingIntent nếu đã tồn tại lịch trước đó
    val pendingIntent = PendingIntent.getBroadcast(
        context, 101, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Xử lý chuỗi thời gian và khởi tạo Calendar
    val parts = timeString.split(":")
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        set(Calendar.MINUTE, parts[1].toInt())
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0) // Quan trọng: Reset mili-giây để tính toán chuẩn xác

        // Nếu giờ đặt đã trôi qua so với hiện tại, tự động cộng thêm 1 ngày
        if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
    }

    when (repeatOption) {
        "Một lần", "Hàng ngày", "Hàng tuần" -> {
            // Lưu ý kiến trúc:
            // KHÔNG dùng setRepeating() vì từ Android 6+ (Doze Mode), hệ thống sẽ gộp báo thức làm chậm trễ.
            // Giải pháp: LUÔN dùng setExactAndAllowWhileIdle() để xuyên thủng Doze Mode.
            // Logic lặp lại sẽ được xử lý đệ quy bên trong NotificationReceiver.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
}

/**
 * Hủy bỏ lịch nhắc nhở đã được đăng ký trước đó.
 */
private fun cancelNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)

    // Sử dụng FLAG_NO_CREATE để kiểm tra xem PendingIntent có đang tồn tại không (tránh tạo mới tốn tài nguyên)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 101, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
    )

    // Nếu tồn tại, hủy lịch trong AlarmManager
    if (pendingIntent != null) alarmManager.cancel(pendingIntent)
}