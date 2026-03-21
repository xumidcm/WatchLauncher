package com.example.wlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.service.StepCounterManager
import com.example.wlauncher.ui.theme.WatchColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WatchFaceLayer(
    modifier: Modifier = Modifier,
    showSteps: Boolean = true,
    stepGoal: Int = 10000
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    val steps by StepCounterManager.steps.collectAsState()
    val stepAvailable by StepCounterManager.available.collectAsState()

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("M\u6708d\u65e5 EEEE", Locale.CHINESE)
        while (true) {
            val now = Date()
            currentTime = timeFmt.format(now)
            currentDate = dateFmt.format(now)
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1a1a2e), Color.Black),
                    radius = 600f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentTime,
                fontSize = 64.sp,
                fontWeight = FontWeight.W200,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentDate,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = WatchColors.ActiveCyan
            )

            // 步数显示
            if (showSteps && stepAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                StepArcIndicator(
                    current = steps,
                    goal = stepGoal
                )
            }
        }
    }
}

@Composable
private fun StepArcIndicator(
    current: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val arcColor = Color(0xFFFFCC00) // 黄色
    val arcBgColor = Color(0xFF333333)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 鞋子图标
        Icon(
            imageVector = Icons.Filled.DirectionsWalk,
            contentDescription = "步数",
            tint = arcColor,
            modifier = Modifier.size(18.dp)
        )

        // 半圆进度条
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(60.dp, 30.dp)
        ) {
            // 背景弧
            drawArc(
                color = arcBgColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 2)
            )
            // 进度弧
            if (progress > 0) {
                drawArc(
                    color = arcColor,
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height * 2)
                )
            }
        }

        // 步数文字
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = formatStepCount(current),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "/${formatStepCount(goal)}",
                fontSize = 10.sp,
                color = WatchColors.TextTertiary
            )
        }
    }
}

private fun formatStepCount(steps: Int): String {
    return when {
        steps >= 10000 -> String.format("%.1fk", steps / 1000f)
        steps >= 1000 -> String.format("%.1fk", steps / 1000f)
        else -> steps.toString()
    }
}
