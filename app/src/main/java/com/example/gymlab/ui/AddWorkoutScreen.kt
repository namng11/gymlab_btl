/**
 * File: AddWorkoutScreen.kt
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà
 * Module: Giao diện Lên lịch tập luyện (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Màn hình cho phép người dùng thêm bài tập vào lịch tập của một ngày cụ thể.
 * Hỗ trợ hai chế độ: Áp dụng theo mẫu lịch tập (Template) hoặc Thêm bài tập lẻ (Exercise).
 */

package com.example.gymlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlab.api.AddExerciseRequest
import com.example.gymlab.api.ApplyTemplateRequest
import com.example.gymlab.api.Category
import com.example.gymlab.api.Exercise
import com.example.gymlab.api.RetrofitClient
import com.example.gymlab.api.WorkoutTemplate
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Màn hình Thêm bài tập (Master-Detail flow từ WorkoutScheduleScreen).
 *
 * @param userId ID của người dùng.
 * @param targetDate Ngày đang được chọn để thêm bài tập (Định dạng yyyy-MM-dd).
 * @param onBackClick Callback quay lại màn hình trước.
 * @param onSuccess Callback khi thêm bài tập/mẫu thành công (thường để reload lại lịch tập).
 * @param onCreateTemplateClick Callback điều hướng sang màn hình CreateTemplateScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutScreen(
    userId: Int,
    targetDate: String,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    onCreateTemplateClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Quản lý state cho danh sách dữ liệu từ API
    var templates by remember { mutableStateOf<List<WorkoutTemplate>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    // State lưu category đang được chọn để lọc bài tập lẻ
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    // State quản lý hiệu ứng loading chung toàn màn hình
    var isLoading by remember { mutableStateOf(false) }

    /**
     * Lấy danh sách bài tập. Nếu [catId] != null, sẽ gọi API GET /exercises?category_id=catId.
     */
    fun fetchExercises(catId: Int? = null) {
        scope.launch {
            try {
                val res = RetrofitClient.instance.getExercises(limit = null, categoryId = catId)
                if (res.isSuccessful) {
                    exercises = res.body() ?: emptyList()
                    android.util.Log.d("AddWorkoutScreen", "Exercises loaded: ${exercises.size}")
                } else {
                    android.util.Log.e("AddWorkoutScreen", "getExercises failed: ${res.code()} - ${res.errorBody()?.string()}")
                    exercises = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("AddWorkoutScreen", "getExercises exception", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Lấy danh sách mẫu lịch tập (GET /templates).
     */
    fun fetchTemplates() {
        scope.launch {
            try {
                val tRes = RetrofitClient.instance.getTemplates(limit = 20)
                if (tRes.isSuccessful) {
                    templates = tRes.body() ?: emptyList()
                    android.util.Log.d("AddWorkoutScreen", "Templates loaded: ${templates.size}")
                } else {
                    android.util.Log.e(
                        "AddWorkoutScreen",
                        "getTemplates failed: ${tRes.code()} - ${tRes.errorBody()?.string()}"
                    )
                    templates = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("AddWorkoutScreen", "getTemplates exception", e)
                e.printStackTrace()
                templates = emptyList()
            }
        }
    }

    // Effect khởi tạo: Gọi đồng thời API lấy templates, categories và exercises
    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            try {
                fetchTemplates()

                val cRes = RetrofitClient.instance.getCategories()
                if (cRes.isSuccessful) {
                    categories = cRes.body() ?: emptyList()
                    android.util.Log.d("AddWorkoutScreen", "Categories loaded: ${categories.size}")
                } else {
                    android.util.Log.e("AddWorkoutScreen", "getCategories failed: ${cRes.code()} - ${cRes.errorBody()?.string()}")
                }

                fetchExercises()
            } catch (e: Exception) {
                android.util.Log.e("AddWorkoutScreen", "LaunchedEffect exception", e)
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Lắng nghe vòng đời ON_RESUME để tự động tải lại danh sách Template
    // Đảm bảo sau khi người dùng tạo mẫu mới ở CreateTemplateScreen và quay lại đây,
    // mẫu mới sẽ xuất hiện ngay lập tức mà không cần gọi API thủ công.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchTemplates()
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
                title = { Text("Thêm vào: $targetDate", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Nhánh 1: Danh sách mẫu lịch tập (Template)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mẫu lịch tập", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                        // Nút điều hướng sang nhánh độc lập CreateTemplateScreen
                        TextButton(onClick = onCreateTemplateClick) {
                            Text("Tạo mẫu mới")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(templates) { template ->
                            TemplateCard(template) {
                                // Gọi API POST /apply-template để thêm toàn bộ bài tập trong mẫu vào ngày
                                scope.launch {
                                    try {
                                        val res = RetrofitClient.instance.applyTemplateToDate(
                                            ApplyTemplateRequest(
                                                userId = userId,
                                                targetDate = targetDate,
                                                templateId = template.id
                                            )
                                        )
                                        if (res.isSuccessful && res.body()?.success == true) {
                                            onSuccess()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Nhánh 2: Danh sách bài tập lẻ có lọc theo nhóm cơ
                item {
                    Text("Bài tập lẻ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Thanh cuộn ngang chứa các Filter Chip (nhóm cơ)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            CategoryChip(
                                name = "Tất cả",
                                isSelected = selectedCategoryId == null
                            ) {
                                selectedCategoryId = null
                                fetchExercises(null) // Lấy toàn bộ bài tập
                            }
                        }

                        items(categories) { cat ->
                            CategoryChip(
                                name = cat.name,
                                isSelected = selectedCategoryId == cat.id
                            ) {
                                selectedCategoryId = cat.id
                                fetchExercises(cat.id) // Lấy bài tập theo category_id
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Render danh sách bài tập lẻ từ state exercises
                items(exercises) { exercise ->
                    ExerciseListItem(exercise) {
                        // Gọi API POST /add-exercise để thêm 1 bài tập cụ thể vào ngày
                        scope.launch {
                            try {
                                val res = RetrofitClient.instance.addExerciseToSchedule(
                                    AddExerciseRequest(
                                        userId = userId,
                                        targetDate = targetDate,
                                        exerciseId = exercise.id
                                    )
                                )
                                if (res.isSuccessful && res.body()?.success == true) {
                                    onSuccess()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Card hiển thị tóm tắt một mẫu lịch tập.
 * Nằm trong LazyRow "Mẫu lịch tập".
 *
 * @param template Dữ liệu mẫu tập luyện.
 * @param onClick Callback khi người dùng chọn áp dụng mẫu này.
 */
@Composable
fun TemplateCard(template: WorkoutTemplate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(template.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(template.description ?: "", fontSize = 12.sp, color = Color.Gray, maxLines = 2)
        }
    }
}