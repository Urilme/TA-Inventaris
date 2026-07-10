package com.ta.konstruksi.presentation.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ta.konstruksi.R
import com.ta.konstruksi.presentation.ui.component.NetworkBadge
import com.ta.konstruksi.presentation.ui.theme.AmberBg
import com.ta.konstruksi.presentation.ui.theme.AmberBorder
import com.ta.konstruksi.presentation.ui.theme.AmberText
import com.ta.konstruksi.presentation.ui.theme.AppBackground
import com.ta.konstruksi.presentation.ui.theme.BorderInput
import com.ta.konstruksi.presentation.ui.theme.Navy
import com.ta.konstruksi.presentation.ui.theme.RedBg
import com.ta.konstruksi.presentation.ui.theme.RedBorder
import com.ta.konstruksi.presentation.ui.theme.RedDark
import com.ta.konstruksi.presentation.ui.theme.RedMain
import com.ta.konstruksi.presentation.ui.theme.TextPrimary
import com.ta.konstruksi.presentation.ui.theme.TextSecondary
import com.ta.konstruksi.presentation.ui.theme.TextSubtle
import com.ta.konstruksi.presentation.viewmodel.AuthUiState
import com.ta.konstruksi.presentation.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    isOnline: Boolean = false,
    viewModel: AuthViewModel = hiltViewModel()
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf("") } // dipertahankan saat kartu menutup (animasi keluar)
    val shake = remember { Animatable(0f) }
    val statusBarTopPx = WindowInsets.statusBars.getTop(LocalDensity.current)

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Error -> {
                errorMsg = s.message
                errorText = s.message
                // Getar horizontal — umpan balik "kredensial salah" yang khas
                shake.snapTo(0f)
                shake.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 480
                        0f at 0; 12f at 60; -12f at 120; 9f at 180
                        (-9f) at 240; 5f at 300; (-5f) at 360; 0f at 440
                    }
                )
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Navy hero band — identitas app senada dengan header Dashboard/Profil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Navy)
            ) {
                // Cincin monitoring — memancar dari logo, metafora pelacakan aset
                Canvas(modifier = Modifier.matchParentSize()) {
                    val pusat = Offset(size.width / 2f, statusBarTopPx + 90.dp.toPx())
                    val cincin = listOf(
                        34.dp to 0.09f, 62.dp to 0.07f, 92.dp to 0.05f,
                        128.dp to 0.038f, 168.dp to 0.028f
                    )
                    cincin.forEach { (jari, alpha) ->
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = jari.toPx(),
                            center = pusat,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo SKN dalam chip putih agar tetap terbaca di atas Navy
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_skn),
                            contentDescription = "Logo SKN",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        "Sistem Monitoring\nAset & Inventaris",
                        fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White,
                        lineHeight = 28.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "PT Sahabat Konstruksi Nusantara",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xB3FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Username field
            Text("Username", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(x = shake.value.dp).height(58.dp),
                shape = RoundedCornerShape(12.dp),
                isError = errorMsg != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    errorBorderColor = RedMain, errorContainerColor = Color.White, errorLeadingIconColor = RedMain
                ),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(22.dp)) },
                placeholder = { Text("username", color = TextSubtle, fontSize = 15.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Password field
            Text("Kata Sandi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(x = shake.value.dp).height(58.dp),
                shape = RoundedCornerShape(12.dp),
                isError = errorMsg != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy, unfocusedBorderColor = BorderInput,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    errorBorderColor = RedMain, errorContainerColor = Color.White, errorLeadingIconColor = RedMain
                ),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(22.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null, tint = TextSubtle, modifier = Modifier.size(22.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true
            )

            // Peringatan interaktif — muncul & getar saat kredensial salah, hilang saat diedit
            AnimatedVisibility(
                visible = errorMsg != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 18.dp)
                        .offset(x = shake.value.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RedBg)
                        .border(1.5.dp, RedBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RedMain, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        errorText,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RedDark, lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MASUK button
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp, pressedElevation = 0.dp, disabledElevation = 0.dp
                ),
                enabled = uiState !is AuthUiState.Loading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("MASUK", fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Network status card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.5.dp, BorderInput, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Status Jaringan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    NetworkBadge(isOnline = isOnline, onDarkBg = false)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (isOnline) "Terhubung ke jaringan. Kredensial diverifikasi ke server."
                    else "Mode offline — login pertama memerlukan internet. Bila sudah pernah masuk, sesi tersimpan dapat digunakan tanpa sinyal.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = TextSubtle
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
