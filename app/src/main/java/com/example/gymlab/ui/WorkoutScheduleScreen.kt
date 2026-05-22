//package com.example.gymlab.ui
//
//import android.app.DatePickerDialog
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.CalendarMonth
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.gymlab.api.DailyScheduleResponse
//import com.example.gymlab.api.RetrofitClient
//import com.example.gymlab.api.WorkoutExercise
//import com.example.gymlab.ui.theme.PrimaryPurple
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.*
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleEventObserver
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import coil.compose.AsyncImage
//import androidx.compose.ui.layout.ContentScale
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun WorkoutScheduleScreen(
//    userId: Int, // 1. Thêm tham số này
//    onBackClick: () -> Unit,
//    onAddWorkoutClick: (String) -> Unit,
//    onExerciseClick: (WorkoutExercise) -> Unit
//) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
//    var scheduleData by remember { mutableStateOf(DailyScheduleResponse()) }
//    var isLoading by remember { mutableStateOf(false) }
//
//    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//
//    fun fetchSchedule(date: Calendar) {
//        isLoading = true
//        scope.launch {
//            try {
//                // 3. Sử dụng userId từ tham số truyền vào
//                val response = RetrofitClient.instance.getDailySchedule(
//                    userId = userId,
//                    date = apiFormat.format(date.time)
//                )
//
//                scheduleData = if (response.isSuccessful) {
//                    response.body() ?: DailyScheduleResponse()
//                } else {
//                    DailyScheduleResponse()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                scheduleData = DailyScheduleResponse()
//            } finally {
//                isLoading = false
//            }
//        }
//    }
//
//    LaunchedEffect(selectedDate.timeInMillis) {
//        fetchSchedule(selectedDate)
//    }
//    DisposableEffect(lifecycleOwner, selectedDate.timeInMillis) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_RESUME) {
//                fetchSchedule(selectedDate)
//            }
//        }
//
//        lifecycleOwner.lifecycle.addObserver(observer)
//
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//        }
//    }
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Lịch tập luyện", fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = {
//                        val calendar = Calendar.getInstance()
//                        DatePickerDialog(
//                            context,
//                            { _, year, month, day ->
//                                selectedDate = Calendar.getInstance().apply {
//                                    set(year, month, day)
//                                }
//                            },
//                            calendar.get(Calendar.YEAR),
//                            calendar.get(Calendar.MONTH),
//                            calendar.get(Calendar.DAY_OF_MONTH)
//                        ).show()
//                    }) {
//                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { onAddWorkoutClick(apiFormat.format(selectedDate.time)) },
//                containerColor = PrimaryPurple,
//                contentColor = Color.White
//            ) {
//                Icon(Icons.Default.Add, contentDescription = "Add Workout")
//            }
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(Color(0xFFF8F9FA))
//        ) {
//            DateStrip(selectedDate = selectedDate) { date ->
//                selectedDate = date
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            SummaryCard(scheduleData)
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            if (isLoading) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator(color = PrimaryPurple)
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier.fillMaxSize(),
//                    contentPadding = PaddingValues(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    items(scheduleData.exercises) { exercise ->
//                        ScheduleExerciseItem(exercise) {
//                            onExerciseClick(exercise)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun DateStrip(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        val calendar = selectedDate.clone() as Calendar
//        calendar.add(Calendar.DAY_OF_YEAR, -2)
//
//        repeat(5) {
//            val date = calendar.clone() as Calendar
//            val isSelected = isSameDay(date, selectedDate)
//
//            DateItem(
//                dayOfWeek = SimpleDateFormat("EE", Locale("vi", "VN")).format(date.time),
//                dayOfMonth = SimpleDateFormat("dd", Locale.getDefault()).format(date.time),
//                isSelected = isSelected,
//                onClick = { onDateSelected(date) }
//            )
//            calendar.add(Calendar.DAY_OF_YEAR, 1)
//        }
//    }
//}
//
//@Composable
//fun DateItem(
//    dayOfWeek: String,
//    dayOfMonth: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .width(60.dp)
//            .height(80.dp)
//            .clip(RoundedCornerShape(12.dp))
//            .background(if (isSelected) PrimaryPurple else Color.Transparent)
//            .clickable { onClick() },
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = dayOfWeek,
//            fontSize = 12.sp,
//            color = if (isSelected) Color.White else Color.Gray
//        )
//        Text(
//            text = dayOfMonth,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold,
//            color = if (isSelected) Color.White else Color.Black
//        )
//    }
//}
//
//@Composable
//fun SummaryCard(data: DailyScheduleResponse) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    "🔥 ${data.totalCalories} kcal",
//                    fontWeight = FontWeight.Bold,
//                    color = PrimaryPurple
//                )
//                Text(
//                    "✅ ${data.completedCount}/${data.totalCount} bài tập",
//                    fontSize = 14.sp,
//                    color = Color.Gray
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            LinearProgressIndicator(
//                progress = {
//                    if (data.totalCount > 0) {
//                        data.completedCount.toFloat() / data.totalCount
//                    } else {
//                        0f
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(8.dp)
//                    .clip(CircleShape),
//                color = PrimaryPurple,
//                trackColor = Color(0xFFF0F0F0)
//            )
//        }
//    }
//}
//
////@Composable
////fun ScheduleExerciseItem(exercise: WorkoutExercise, onClick: () -> Unit) {
////    Card(
////        onClick = onClick,
////        modifier = Modifier.fillMaxWidth(),
////        shape = RoundedCornerShape(12.dp),
////        colors = CardDefaults.cardColors(containerColor = Color.White)
////    ) {
////        Row(
////            modifier = Modifier
////                .padding(16.dp)
////                .fillMaxWidth(),
////            verticalAlignment = Alignment.CenterVertically
////        ) {
////            Checkbox(
////                checked = exercise.isCompleted,
////                onCheckedChange = null,
////                colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
////            )
////
////            Spacer(modifier = Modifier.width(12.dp))
////
////            Column(modifier = Modifier.weight(1f)) {
////                Text(exercise.name, fontWeight = FontWeight.Bold)
////                Text(
////                    "${exercise.duration} • ${exercise.calories} kcal",
////                    fontSize = 14.sp,
////                    color = Color.Gray
////                )
////            }
////        }
////    }
////}
//
//@Composable
//fun ScheduleExerciseItem(
//    exercise: WorkoutExercise,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        onClick = onClick,
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            if (!exercise.imageUrl.isNullOrBlank()) {
//                AsyncImage(
//                    model = exercise.imageUrl,
//                    contentDescription = exercise.name,
//                    modifier = Modifier
//                        .size(64.dp)
//                        .clip(RoundedCornerShape(12.dp)),
//                    contentScale = ContentScale.Crop
//                )
//            } else {
//                Box(
//                    modifier = Modifier
//                        .size(64.dp)
//                        .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("IMG", color = Color.Gray, fontSize = 12.sp)
//                }
//            }
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = exercise.name,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp
//                )
//                Text(
//                    text = "${exercise.duration} • ${exercise.calories} kcal",
//                    fontSize = 14.sp,
//                    color = Color.Gray
//                )
//            }
//
//            Checkbox(
//                checked = exercise.isCompleted,
//                onCheckedChange = null
//            )
//        }
//    }
//}
//
//fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
//    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
//            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
//}






















//package com.example.gymlab.ui
//
//import android.app.DatePickerDialog
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.CalendarMonth
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleEventObserver
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import coil.compose.SubcomposeAsyncImage
//import com.example.gymlab.api.DailyScheduleResponse
//import com.example.gymlab.api.RetrofitClient
//import com.example.gymlab.api.WorkoutExercise
//import com.example.gymlab.ui.theme.PrimaryPurple
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun WorkoutScheduleScreen(
//    userId: Int,
//    onBackClick: () -> Unit,
//    onAddWorkoutClick: (String) -> Unit,
//    onExerciseClick: (WorkoutExercise) -> Unit
//) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
//    var scheduleData by remember { mutableStateOf(DailyScheduleResponse()) }
//    var isLoading by remember { mutableStateOf(false) }
//
//    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//
//    fun fetchSchedule(date: Calendar) {
//        isLoading = true
//        scope.launch {
//            try {
//                val response = RetrofitClient.instance.getDailySchedule(
//                    userId = userId,
//                    date = apiFormat.format(date.time)
//                )
//
//                scheduleData = if (response.isSuccessful) {
//                    response.body() ?: DailyScheduleResponse()
//                } else {
//                    DailyScheduleResponse()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                scheduleData = DailyScheduleResponse()
//            } finally {
//                isLoading = false
//            }
//        }
//    }
//
//    LaunchedEffect(selectedDate.timeInMillis) {
//        fetchSchedule(selectedDate)
//    }
//
//    DisposableEffect(lifecycleOwner, selectedDate.timeInMillis) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_RESUME) {
//                fetchSchedule(selectedDate)
//            }
//        }
//
//        lifecycleOwner.lifecycle.addObserver(observer)
//
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Lịch tập luyện", fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = {
//                        val calendar = Calendar.getInstance()
//                        DatePickerDialog(
//                            context,
//                            { _, year, month, day ->
//                                selectedDate = Calendar.getInstance().apply {
//                                    set(year, month, day)
//                                }
//                            },
//                            calendar.get(Calendar.YEAR),
//                            calendar.get(Calendar.MONTH),
//                            calendar.get(Calendar.DAY_OF_MONTH)
//                        ).show()
//                    }) {
//                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { onAddWorkoutClick(apiFormat.format(selectedDate.time)) },
//                containerColor = PrimaryPurple,
//                contentColor = Color.White
//            ) {
//                Icon(Icons.Default.Add, contentDescription = "Add Workout")
//            }
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(Color(0xFFF8F9FA))
//        ) {
//            DateStrip(selectedDate = selectedDate) { date ->
//                selectedDate = date
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            SummaryCard(scheduleData)
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            if (isLoading) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator(color = PrimaryPurple)
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier.fillMaxSize(),
//                    contentPadding = PaddingValues(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    items(scheduleData.exercises) { exercise ->
//                        ScheduleExerciseItem(
//                            exercise = exercise,
//                            onClick = {
//                                if (exercise.isCompleted) {
//                                    Toast.makeText(
//                                        context,
//                                        "Bài tập này đã hoàn thành",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                } else {
//                                    onExerciseClick(exercise)
//                                }
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun DateStrip(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        val calendar = selectedDate.clone() as Calendar
//        calendar.add(Calendar.DAY_OF_YEAR, -2)
//
//        repeat(5) {
//            val date = calendar.clone() as Calendar
//            val isSelected = isSameDay(date, selectedDate)
//
//            DateItem(
//                dayOfWeek = SimpleDateFormat("EE", Locale("vi", "VN")).format(date.time),
//                dayOfMonth = SimpleDateFormat("dd", Locale.getDefault()).format(date.time),
//                isSelected = isSelected,
//                onClick = { onDateSelected(date) }
//            )
//            calendar.add(Calendar.DAY_OF_YEAR, 1)
//        }
//    }
//}
//
//@Composable
//fun DateItem(
//    dayOfWeek: String,
//    dayOfMonth: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .width(60.dp)
//            .height(80.dp)
//            .clip(RoundedCornerShape(12.dp))
//            .background(if (isSelected) PrimaryPurple else Color.Transparent)
//            .clickable { onClick() },
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = dayOfWeek,
//            fontSize = 12.sp,
//            color = if (isSelected) Color.White else Color.Gray
//        )
//        Text(
//            text = dayOfMonth,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold,
//            color = if (isSelected) Color.White else Color.Black
//        )
//    }
//}
//
//@Composable
//fun SummaryCard(data: DailyScheduleResponse) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    "🔥 ${data.totalCalories} kcal",
//                    fontWeight = FontWeight.Bold,
//                    color = PrimaryPurple
//                )
//                Text(
//                    "✅ ${data.completedCount}/${data.totalCount} bài tập",
//                    fontSize = 14.sp,
//                    color = Color.Gray
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            LinearProgressIndicator(
//                progress = {
//                    if (data.totalCount > 0) {
//                        data.completedCount.toFloat() / data.totalCount
//                    } else {
//                        0f
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(8.dp)
//                    .clip(CircleShape),
//                color = PrimaryPurple,
//                trackColor = Color(0xFFF0F0F0)
//            )
//        }
//    }
//}
//
//@Composable
//fun ScheduleExerciseItem(
//    exercise: WorkoutExercise,
//    onClick: () -> Unit
//) {
//    val enabled = !exercise.isCompleted
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .alpha(if (enabled) 1f else 0.65f),
//        onClick = {
//            if (enabled) onClick()
//        },
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            ExerciseThumbnail(
//                imageUrl = exercise.imageUrl,
//                exerciseName = exercise.name
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = exercise.name,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp
//                )
//
//                Text(
//                    text = "${exercise.duration} • ${exercise.calories} kcal",
//                    fontSize = 14.sp,
//                    color = Color.Gray
//                )
//
//                if (exercise.isCompleted) {
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = "Đã hoàn thành",
//                        fontSize = 12.sp,
//                        color = PrimaryPurple,
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//            }
//
//            Checkbox(
//                checked = exercise.isCompleted,
//                onCheckedChange = null,
//                colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
//            )
//        }
//    }
//}
//
//@Composable
//fun ExerciseThumbnail(
//    imageUrl: String?,
//    exerciseName: String
//) {
//    if (!imageUrl.isNullOrBlank()) {
//        SubcomposeAsyncImage(
//            model = imageUrl,
//            contentDescription = exerciseName,
//            modifier = Modifier
//                .size(64.dp)
//                .clip(RoundedCornerShape(12.dp)),
//            contentScale = ContentScale.Crop,
//            loading = {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color(0xFFF0F0F0)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator(
//                        modifier = Modifier.size(20.dp),
//                        strokeWidth = 2.dp,
//                        color = PrimaryPurple
//                    )
//                }
//            },
//            error = {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color(0xFFF0F0F0)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("IMG", color = Color.Gray, fontSize = 12.sp)
//                }
//            }
//        )
//    } else {
//        Box(
//            modifier = Modifier
//                .size(64.dp)
//                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
//            contentAlignment = Alignment.Center
//        ) {
//            Text("IMG", color = Color.Gray, fontSize = 12.sp)
//        }
//    }
//}
//
//fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
//    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
//            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
//}




















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

    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var scheduleData by remember { mutableStateOf(DailyScheduleResponse()) }
    var isLoading by remember { mutableStateOf(false) }

    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

    LaunchedEffect(selectedDate.timeInMillis) {
        fetchSchedule(selectedDate)
    }

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
            DateStrip(selectedDate = selectedDate) { date ->
                selectedDate = date
            }

            Spacer(modifier = Modifier.height(16.dp))

            SummaryCard(scheduleData)

            Spacer(modifier = Modifier.height(16.dp))

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

@Composable
fun ExerciseThumbnail(exerciseName: String) {
    Image(
        painter = painterResource(id = getExerciseImageRes(exerciseName)),
        contentDescription = exerciseName,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

//fun getExerciseImageRes(name: String): Int {
//    val normalized = name.trim().lowercase()
//
//    return when {
//        normalized == "jumping jacks" -> R.drawable.jumping_jacks
//        normalized == "burpees" -> R.drawable.burpees
//        normalized == "push up" || normalized == "push-up" -> R.drawable.push_up
//        normalized == "incline push up" || normalized == "incline push-up" -> R.drawable.incline_push_up
//        normalized == "crunches" -> R.drawable.crunches
//        normalized == "plank" -> R.drawable.plank
//        normalized == "bodyweight squat" -> R.drawable.bodyweight_squat
//        normalized == "lunges" -> R.drawable.lunges
//        else -> R.drawable.exercise_placeholder
//    }
//}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}