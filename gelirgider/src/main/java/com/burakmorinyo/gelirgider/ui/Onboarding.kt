package com.burakmorinyo.gelirgider.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.burakmorinyo.gelirgider.R

private val heroBrush = Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF5B21B6), Color(0xFF9D174D)))

@Composable
fun OnboardingScreen(onDone: (startDay: Int) -> Unit) {
    var day by remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize().background(heroBrush)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_launcher_fg), null, tint = Color.Unspecified, modifier = Modifier.size(88.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Gelir Gider", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text(
                "Paranı yönet, geleceğini planla.", style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Feature(Icons.Rounded.AutoAwesome, "Akıllı öneriler", "Günlük harcama hakkın, ay sonu tahmini ve olağandışı harcama uyarıları")
            Feature(Icons.Rounded.CreditCard, "Kredi kartı & taksit", "Hesap kesim, son ödeme günü ve taksitler otomatik takipte")
            Feature(Icons.Rounded.NotificationsActive, "Hiçbir ödemeyi kaçırma", "Fatura, kira, abonelik hatırlatmaları ve otomatik kayıt")
            Feature(Icons.Rounded.Savings, "Bütçe, hedef, borç", "Kategori bütçeleri, birikim hedefleri, borç/alacak defteri")
            Feature(Icons.Rounded.Fingerprint, "Sadece senin", "İnternet gerekmez, veriler telefonda kalır; parmak izi kilidi")

            Spacer(Modifier.height(20.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.12f)).padding(18.dp)
            ) {
                Text("Maaşını ayın kaçında alıyorsun?", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Bütçe dönemin bu günden başlar. Takvim ayı için 1'de bırak.",
                    color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = day, onValueChange = { day = it }, valueRange = 1f..28f, steps = 26,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("${day.toInt()}", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onDone(day.toInt()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4C1D95)),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Başlayalım", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(text, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LockScreen(showButton: Boolean, onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(heroBrush), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
            if (showButton) {
                Spacer(Modifier.height(20.dp))
                Text("Gelir Gider kilitli", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onUnlock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4C1D95))
                ) {
                    Icon(Icons.Rounded.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kilidi aç")
                }
            }
        }
    }
}
