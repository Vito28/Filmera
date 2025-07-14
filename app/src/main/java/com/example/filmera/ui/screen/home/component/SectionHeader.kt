package com.example.filmera.ui.screen.home.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(title: String) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
      Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}
