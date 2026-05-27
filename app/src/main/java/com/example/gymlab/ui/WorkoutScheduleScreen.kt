/**
 * File: WorkoutScheduleScreen.kt
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà
 * Module: Giao diện Lên lịch tập luyện (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Màn hình chính hiển thị lịch tập luyện theo ngày của người dùng,
 * bao gồm tổng quan (calories, tiến độ) và danh sách chi tiết các bài tập.
 */

package com.example.gymlab.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gymlab.R
import com.example.gymlab.api.DailyScheduleResponse
import com.example.gymlab.api.RetrofitClient
import com.example.gymlab.api.WorkoutExercise
import com.example.gymlab.ui.theme.PrimaryPurple
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình chính quản lý lịch tập luyện.
 *
 * @param userId ID của người dùng hiện tại (lấy từ Session/SharedPreferences).
 * @param onBackClick Callback khi nhấn nút Back trên TopAppBar.
 * @param onAddWorkoutClick Callback điều hướng sang AddWorkoutScreen, truyền theo ngày đang chọn.
 * @param onExerciseClick Callback khi chọn vào một bài tập chưa hoàn thành để bắt đầu tập.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScheduleScreen(
    userId: Int,
    onBackClick: () -> Unit,
    onAddWorkoutClick: (String) -> Unit,
    onExerciseClick: (WorkoutExercise) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Quản lý trạng thái ngày đang chọn, mặc định là hôm nay
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Quản lý dữ liệu lịch tập trả về từ API
    var scheduleData by remember { mutableStateOf(DailyScheduleResponse()) }

    // Trạng thái loading khi gọi API
    var isLoading by remember { mutableStateOf(false) }

    // Định dạng ngày chuẩn để gửi lên API (yyyy-MM-dd)
    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Hàm nội bộ để gọi API GET /daily-schedule.
     * Cập nhật [scheduleData] khi có kết quả trả về.
     */
    fun fetchSchedule(date: Calendar) {
        isLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.getDailySchedule(
                    userId = userId,
                    date = apiFormat.format(date.time)
                )

                scheduleData = if (response.isSuccessful) {
                    response.body() ?: DailyScheduleResponse()
                } else {
                    DailyScheduleResponse()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scheduleData = DailyScheduleResponse()
            } finally {
                isLoading = false
            }
        }
    }

    // Tự động gọi API khi selectedDate thay đổi
    LaunchedEffect(selectedDate.timeInMillis) {
        fetchSchedule(selectedDate)
    }

    // Lắng nghe vòng đời của Compose (LifecycleEventObserver).
    // Tự động refresh dữ liệu (fetchSchedule) mỗi khi màn hình quay trở lại trạng thái ON_RESUME
    // (ví dụ: sau khi thêm bài tập từ AddWorkoutScreen xong và quay lại).
    DisposableEffect(lifecycleOwner, selectedDate.timeInMillis) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchSchedule(selectedDate)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch tập luyện", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Nút mở DatePickerDialog để chọn ngày bất kỳ
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedDate = Calendar.getInstance().apply {
                                    set(year, month, day)
                                }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                }
            )
        },
        floatingActionButton = {
            // Nút FAB (+) thêm bài tập mới
            FloatingActionButton(
                onClick = { onAddWorkoutClick(apiFormat.format(selectedDate.time)) },
                containerColor = PrimaryPurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Workout")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            // Thanh chọn ngày nhanh (DateStrip)
            DateStrip(selectedDate = selectedDate) { date ->
                selectedDate = date
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thẻ tóm tắt tổng số calo và tiến độ hoàn thành
            SummaryCard(scheduleData)

            Spacer(modifier = Modifier.height(16.dp))

            // Hiển thị danh sách bài tập hoặc loading
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scheduleData.exercises) { exercise ->
                        ScheduleExerciseItem(
                            exercise = exercise,
                            onClick = {
                                if (exercise.isCompleted) {
                                    Toast.makeText(
                                        context,
                                        "Bài tập này đã hoàn thành",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onExerciseClick(exercise)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component hiển thị thanh 5 ngày liên tiếp (2 ngày trước, ngày hiện tại, 2 ngày sau).
 * @param selectedDate Ngày đang được chọn hiện tại.
 * @param onDateSelected Callback khi người dùng bấm chọn một ngày khác trên thanh.
 */
@Composable
fun DateStrip(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val calendar = selectedDate.clone() as Calendar
        calendar.add(Calendar.DAY_OF_YEAR, -2)

        repeat(5) {
            val date = calendar.clone() as Calendar
            val isSelected = isSameDay(date, selectedDate)

            DateItem(
                dayOfWeek = SimpleDateFormat("EE", Locale("vi", "VN")).format(date.time),
                dayOfMonth = SimpleDateFormat("dd", Locale.getDefault()).format(date.time),
                isSelected = isSelected,
                onClick = { onDateSelected(date) }
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}

/**
 * Item đại diện cho 1 ngày trong DateStrip.
 * Nếu [isSelected] là true, item sẽ được highlight bằng màu PrimaryPurple.
 */
@Composable
fun DateItem(
    dayOfWeek: String,
    dayOfMonth: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryPurple else Color.Transparent)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfWeek,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Gray
        )
        Text(
            text = dayOfMonth,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

/**
 * Thẻ hiển thị tóm tắt buổi tập (Tổng Calories dự kiến & Tiến độ hoàn thành).
 */
@Composable
fun SummaryCard(data: DailyScheduleResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "🔥 ${data.totalCalories} kcal",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    "✅ ${data.completedCount}/${data.totalCount} bài tập",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thanh tiến độ bài tập
            LinearProgressIndicator(
                progress = {
                    if (data.totalCount > 0) {
                        data.completedCount.toFloat() / data.totalCount
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = PrimaryPurple,
                trackColor = Color(0xFFF0F0F0)
            )
        }
    }
}

/**
 * Card hiển thị chi tiết 1 bài tập trong danh sách lịch tập.
 * Nếu [exercise.isCompleted] là true, card sẽ bị mờ đi (alpha 0.65) và vô hiệu hóa click.
 */
@Composable
fun ScheduleExerciseItem(
    exercise: WorkoutExercise,
    onClick: () -> Unit
) {
    val enabled = !exercise.isCompleted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.65f),
        onClick = {
            if (enabled) onClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseThumbnail(exerciseName = exercise.name)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${exercise.duration} • ${exercise.calories} kcal",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                if (exercise.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đã hoàn thành",
                        fontSize = 12.sp,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Checkbox(
                checked = exercise.isCompleted,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
            )
        }
    }
}

/**
 * Helper component hiển thị thumbnail của bài tập.
 * Tạm thời load từ resource cục bộ dựa theo tên.
 */
@Composable
fun ExerciseThumbnail(exerciseName: String) {
    Image(
        painter = painterResource(id = getExerciseImageRes(exerciseName)), // Hàm này cần được định nghĩa ở file tiện ích
        contentDescription = exerciseName,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

/**
 * Hàm tiện ích kiểm tra hai đối tượng [Calendar] có trỏ đến cùng một ngày hay không.
 */
fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}