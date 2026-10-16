package com.project.solaria_mobile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize().background(SolariaWhite), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SolariaSpace.xl)) {
            Box(
                modifier = Modifier.size(148.dp).clip(RoundedCornerShape(38.dp)).background(SolariaOrange),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.lucide_ic_handshake),
                    contentDescription = "Solaria",
                    tint = SolariaWhite,
                    modifier = Modifier.size(82.dp),
                )
            }
            LinearProgressIndicator(
                progress = { 0.36f },
                modifier = Modifier.fillMaxWidth(0.58f).height(6.dp).clip(RoundedCornerShape(SolariaSpace.pill)),
                color = SolariaOrange,
                trackColor = SolariaOrange.copy(alpha = 0.08f),
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}
