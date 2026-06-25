package com.psy.ui.auth

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldCheck
import com.psy.ui.app.AppViewModel
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexSans
import com.psy.ui.theme.SpaceGrotesk
import kotlinx.coroutines.launch

/**
 * Welcome / login gate. Google-only sign-in; no dev/email login.
 * On success [AppViewModel.signInGoogle] is called, which flips `isSignedIn` and opens the gate.
 */
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val message by viewModel.uiMessage.collectAsStateWithLifecycle()
    val colors = LocalPsyColors.current

    // The only looping animation on this screen: a soft pulsing teal glow behind the logo.
    val pulse = rememberInfiniteTransition(label = "logoPulse")
    val glowScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowScale",
    )
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo badge with pulsing glow behind it.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                        }
                        .clip(CircleShape)
                        .background(colors.teal.copy(alpha = glowAlpha)),
                )
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF103458), Color(0xFF061A30)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.ShieldCheck,
                        contentDescription = null,
                        tint = colors.teal,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            EyebrowLabel(text = "Smart money tracking", color = colors.blue)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Psy",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                color = colors.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ghi chép chi tiêu dễ thương",
                fontFamily = PlexSans,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = colors.text2,
            )

            Spacer(Modifier.height(48.dp))
            Button(
                onClick = {
                    scope.launch {
                        launchGoogleSignIn(
                            activity = context as Activity,
                            onSuccess = { idToken -> viewModel.signInGoogle(idToken) },
                            onError = { msg -> viewModel.showMessage(msg) },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.blue),
                contentPadding = PaddingValues(vertical = 17.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "G",
                            fontFamily = PlexSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.blue,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Đăng nhập với Google",
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }

            if (message != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message ?: "",
                    fontFamily = PlexSans,
                    fontSize = 14.sp,
                    color = colors.red,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Bottom-pinned trust line.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Lucide.Lock,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Dữ liệu được mã hoá & lưu an toàn",
                fontFamily = PlexSans,
                fontSize = 12.sp,
                color = colors.text3,
            )
        }
    }
}
