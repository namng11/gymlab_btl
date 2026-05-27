/**
 * File: AchievementsScreen.kt
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà
 * Module: Giao diện Hệ thống thành tựu (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Màn hình hiển thị danh sách các huy hiệu (badges) và trạng thái
 * hoàn thành của người dùng dựa trên điểm (points) và chuỗi ngày tập (streak).
 */

package com.example.gymlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlab.api.Badge
import com.example.gymlab.api.RetrofitClient
import com.example.gymlab.ui.theme.PrimaryPurple
import com.example.gymlab.ui.theme.TextGray
import kotlinx.coroutines.launch

/**
 * Màn hình chính quản lý và hiển thị danh sách Thành tích/Huy hiệu.
 *
 * @param userId ID của người dùng hiện tại, nhận từ Navigation/MainActivity.
 * @param onBackClick Callback xử lý khi nhấn nút quay lại.
 * @param onActivityClick Callback điều hướng BottomBar.
 * @param onDietClick Callback điều hướng BottomBar.
 * @param onScheduleClick Callback điều hướng BottomBar.
 * @param onNotificationClick Callback mở màn hình NotificationSettingsScreen (thuộc phần cá nhân).
 */
@Composable
fun AchievementsScreen(
    userId: Int, // Đã đồng bộ nhận userId từ MainActivity
    onBackClick: () -> Unit,
    onActivityClick: () -> Unit,
    onDietClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // State quản lý danh sách huy hiệu lấy từ API
    var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State quản lý huy hiệu đang được chọn để xem chi tiết (hiển thị Dialog nếu != null)
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    // Gọi API lấy dữ liệu thành tựu khi màn hình khởi tạo hoặc khi userId thay đổi
    LaunchedEffect(userId) {
        scope.launch {
            try {
                // Gọi GET /achievements/:userId
                // Thuộc tính is_earned đã được backend (server.js) tính toán động sẵn.
                val response = RetrofitClient.instance.getAchievements(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    badges = response.body()?.badges ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                onProfileClick = {},
                onActivityClick = onActivityClick,
                onDietClick = onDietClick,
                onScheduleClick = onScheduleClick
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Header chứa nút Back và nút cài đặt Thông báo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                        Text("Quay lại", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Danh sách thành tích", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(24.dp))

                // Hiển thị loading hoặc danh sách
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(badges) { badge ->
                            AchievementCard(
                                title = badge.title,
                                description = badge.description,
                                icon = getBadgeIcon(badge.title),
                                isEarned = badge.isEarned,
                                onClick = { selectedBadge = badge } // Gán badge được chọn để mở popup
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }

            // --- POPUP CHI TIẾT THÀNH TÍCH ---
            // Chỉ render Dialog khi có một badge được chọn (selectedBadge != null)
            selectedBadge?.let { badge ->
                BadgeDetailDialog(
                    badge = badge,
                    onDismiss = { selectedBadge = null }
                )
            }
        }
    }
}

/**
 * Dialog (Popup) hiển thị thông tin chi tiết của một huy hiệu.
 * Bao gồm mô tả, yêu cầu cần đạt (điểm, streak) và trạng thái hiện tại.
 *
 * @param badge Thông tin chi tiết của huy hiệu được chọn.
 * @param onDismiss Callback kích hoạt khi người dùng muốn đóng popup.
 */
@Composable
fun BadgeDetailDialog(badge: Badge, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Biểu tượng huy hiệu: Đổi màu nền và alpha dựa theo isEarned
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (badge.isEarned) PrimaryPurple.copy(alpha = 0.15f) else Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBadgeIcon(badge.title),
                        contentDescription = null,
                        tint = if (badge.isEarned) PrimaryPurple else Color.Gray,
                        modifier = Modifier.size(40.dp).graphicsLayer(alpha = if (badge.isEarned) 1f else 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = badge.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = badge.description,
                    fontSize = 15.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                // Hiển thị điều kiện cần thiết để đạt huy hiệu
                Text("YÊU CẦU ĐẠT ĐƯỢC:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                if (badge.requiredPoints > 0) {
                    Text("• Tích lũy đủ: ${badge.requiredPoints} pts", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (badge.requiredStreak > 0) {
                    Text("• Chuỗi tập luyện: ${badge.requiredStreak} ngày", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hiển thị trạng thái hoàn thành
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TRẠNG THÁI: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    Text(
                        text = if (badge.isEarned) "Đã đạt được" else "Chưa hoàn thành",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isEarned) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ĐÓNG", fontWeight = FontWeight.Bold, color = PrimaryPurple)
            }
        }
    )
}

/**
 * Card UI hiển thị một item huy hiệu trong danh sách của màn hình chính.
 * Mọi UI component bên trong đều thay đổi dựa theo cờ [isEarned].
 *
 * @param title Tên huy hiệu.
 * @param description Mô tả ngắn.
 * @param icon Biểu tượng của huy hiệu.
 * @param isEarned Nếu true (đã đạt), Card có nền trắng, đổ bóng và icon màu tím.
 * Nếu false (chưa đạt), nền xám bẹt và icon xám mờ.
 * @param onClick Callback mở popup chi tiết khi nhấn vào.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEarned: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isEarned) Color.White else Color(0xFFF1F1F1)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEarned) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vòng tròn chứa Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isEarned) PrimaryPurple.copy(alpha = 0.1f) else Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEarned) PrimaryPurple else Color.Gray,
                    modifier = Modifier.size(28.dp).graphicsLayer(alpha = if (isEarned) 1f else 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isEarned) Color.Black else Color.Gray
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                    maxLines = 1 // Ở danh sách chỉ hiện 1 dòng cho gọn
                )
            }
            // Hiện tick xanh nếu đã hoàn thành
            if (isEarned) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Hàm tiện ích ánh xạ tên huy hiệu (dựa vào từ khóa trả về từ API)
 * sang biểu tượng (ImageVector) của Material Design.
 */
private fun getBadgeIcon(title: String): ImageVector {
    return when (title.lowercase()) {
        "tập sự" -> Icons.Default.FitnessCenter
        "duy trì" -> Icons.Default.Timeline
        "nâng cao" -> Icons.AutoMirrored.Filled.TrendingUp
        "chuỗi" -> Icons.Default.FlashOn
        "thử sức" -> Icons.Default.EmojiEvents
        "khỏe mạnh" -> Icons.Default.Favorite
        "bền bỉ" -> Icons.AutoMirrored.Filled.DirectionsRun
        "nâng tạ" -> Icons.Default.Sports
        else -> Icons.Default.Star
    }
}