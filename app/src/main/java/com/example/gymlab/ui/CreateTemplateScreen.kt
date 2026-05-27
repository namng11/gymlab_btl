/**
 * File: CreateTemplateScreen.kt
 * Project: Gymlab - Ứng dụng hỗ trợ luyện tập tại nhà
 * Module: Giao diện Lên lịch tập luyện (Phần cá nhân)
 * Author: Nguyễn Hải Nam - B22DCCN559
 * Description: Màn hình cho phép người dùng tự tạo mẫu lịch tập cá nhân.
 * Bao gồm việc nhập tên mẫu, chọn bài tập từ hệ thống và gửi request lưu trữ.
 */

package com.example.gymlab.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlab.api.Category
import com.example.gymlab.api.Exercise
import com.example.gymlab.api.RetrofitClient
import com.example.gymlab.api.TemplateExerciseRequest
import com.example.gymlab.api.WorkoutTemplateRequest
import com.example.gymlab.ui.theme.PrimaryPurple
import kotlinx.coroutines.launch

/**
 * Màn hình Tạo mẫu lịch tập mới.
 *
 * @param onBackClick Callback quay lại màn hình trước đó (thường là AddWorkoutScreen).
 * @param onSuccess Callback khi việc tạo mẫu thành công để quay lại và refresh danh sách mẫu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // State lưu trữ thông tin cơ bản của mẫu
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }

    // Danh sách các bài tập người dùng đã chọn để đưa vào mẫu
    // Sử dụng mutableStateListOf để Compose tự động recompose khi thêm/xóa phần tử
    val selectedExercises = remember { mutableStateListOf<TemplateExerciseRequest>() }

    // State quản lý danh mục và danh sách bài tập lấy từ API
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var allExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var filteredExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    // State chặn thao tác khi đang gọi API lưu mẫu
    var isSaving by remember { mutableStateOf(false) }

    // Gọi API lấy dữ liệu hệ thống (nhóm cơ, tất cả bài tập) khi màn hình vừa khởi tạo
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Lấy danh sách danh mục (nhóm cơ)
                val catRes = RetrofitClient.instance.getCategories()
                if (catRes.isSuccessful) {
                    categories = catRes.body() ?: emptyList()
                }

                // Lấy toàn bộ danh sách bài tập (không phân trang, không lọc)
                val exRes = RetrofitClient.instance.getExercises(limit = null, categoryId = null)
                if (exRes.isSuccessful) {
                    allExercises = exRes.body() ?: emptyList()
                    filteredExercises = allExercises // Mặc định hiển thị tất cả
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo mẫu lịch tập", fontWeight = FontWeight.Bold) },
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
        ) {
            // LazyColumn chứa toàn bộ nội dung form, danh sách bài đã chọn và danh sách bài gợi ý
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Chiếm phần không gian còn lại, chừa chỗ cho nút Lưu ở dưới cùng
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Phần 1: Thông tin cơ bản (Tên, Mô tả)
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Tên mẫu (VD: Tập ngực thứ 2)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = templateDescription,
                        onValueChange = { templateDescription = it },
                        label = { Text("Mô tả (Không bắt buộc)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Phần 2: Danh sách các bài tập ĐÃ CHỌN
                item {
                    Text("Bài tập đã chọn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (selectedExercises.isEmpty()) {
                        Text("Chưa có bài tập nào", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                items(selectedExercises) { item ->
                    SelectedExerciseCard(item) {
                        selectedExercises.remove(item)
                    }
                }

                // Phần 3: Danh sách bài tập CÓ SẴN (để chọn)
                item {
                    HorizontalDivider()
                    Text("Danh sách bài tập hệ thống", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bộ lọc theo nhóm cơ
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            CategoryChip(
                                name = "Tất cả",
                                isSelected = selectedCategoryId == null
                            ) {
                                selectedCategoryId = null
                                filteredExercises = allExercises
                            }
                        }

                        items(categories) { cat ->
                            CategoryChip(
                                name = cat.name,
                                isSelected = selectedCategoryId == cat.id
                            ) {
                                selectedCategoryId = cat.id
                                // Lọc offline từ danh sách allExercises đã lấy ban đầu
                                filteredExercises = allExercises.filter { it.categoryId == cat.id }
                            }
                        }
                    }
                }

                // Render danh sách bài tập sau khi lọc
                items(filteredExercises) { exercise ->
                    ExerciseListItem(exercise) {
                        // Tránh thêm trùng bài tập vào mẫu
                        if (selectedExercises.none { it.exerciseId == exercise.id }) {
                            selectedExercises.add(
                                TemplateExerciseRequest(
                                    exerciseId = exercise.id,
                                    exerciseName = exercise.name,
                                    sets = 3, // Mặc định 3 hiệp
                                    reps = 12, // Mặc định 12 lần
                                    orderIndex = selectedExercises.size + 1 // Tự tăng orderIndex
                                )
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Nút Lưu mẫu lịch tập
            Button(
                onClick = {
                    // Validate: Phải nhập tên mẫu, phải chọn ít nhất 1 bài tập và không đang trong tiến trình lưu
                    if (templateName.isBlank() || selectedExercises.isEmpty() || isSaving) return@Button

                    isSaving = true
                    scope.launch {
                        try {
                            val request = WorkoutTemplateRequest(
                                name = templateName,
                                description = templateDescription,
                                exercises = selectedExercises.toList() // Chuyển state list thành List thông thường gửi API
                            )

                            // Gọi API POST /workout-templates
                            val response = RetrofitClient.instance.createWorkoutTemplate(request)
                            if (response.isSuccessful && response.body()?.success == true) {
                                onSuccess()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text(
                    if (isSaving) "ĐANG LƯU..." else "LƯU MẪU LỊCH TẬP",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Card hiển thị bài tập đã được chọn vào mẫu.
 *
 * @param item Thông tin bài tập (số hiệp, số lần).
 * @param onDelete Callback khi người dùng muốn xóa bài tập này khỏi mẫu.
 */
@Composable
fun SelectedExerciseCard(item: TemplateExerciseRequest, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = getExerciseImageRes(item.exerciseName)),
                contentDescription = item.exerciseName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.exerciseName, fontWeight = FontWeight.Bold)
                Text("${item.sets} hiệp x ${item.reps} lần", fontSize = 14.sp, color = Color.Gray)
            }

            // Nút xóa (thùng rác đỏ)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}