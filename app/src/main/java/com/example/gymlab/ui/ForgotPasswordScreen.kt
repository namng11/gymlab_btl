package com.example.gymlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlab.ui.theme.PrimaryPurple
import com.example.gymlab.ui.theme.TextGray

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onSendCodeClick: (String) -> Unit,
    onLoginNowClick: () -> Unit
) {
    var emailOrPhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF5F5F5), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Quên mật khẩu? 🔒",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Đừng lo lắng! Hãy nhập email bạn đã đăng ký tài khoản. Chúng tôi sẽ gửi mã xác minh 4 số để giúp bạn đặt lại mật khẩu mới.",
            fontSize = 14.sp,
            color = TextGray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Địa chỉ Email hoặc Số điện thoại",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            placeholder = { Text("Ví dụ: duc.hoang@email.com", color = Color.LightGray) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8F9FA),
                focusedContainerColor = Color(0xFFF8F9FA),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = PrimaryPurple,
                cursorColor = PrimaryPurple
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { onSendCodeClick(emailOrPhone) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text(
                text = "Gửi mã xác nhận",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onLoginNowClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Bạn đã nhớ mật khẩu? ")
                    withStyle(style = SpanStyle(color = PrimaryPurple, fontWeight = FontWeight.Bold)) {
                        append("Đăng nhập ngay")
                    }
                },
                fontSize = 14.sp,
                color = TextGray
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
