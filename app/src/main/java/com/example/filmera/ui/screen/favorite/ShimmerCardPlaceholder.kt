package com.example.filmera.ui.screen.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.shimmer
import com.valentinilk.shimmer.rememberShimmer

@Composable
fun ShimmerCardPlaceholder() {
  val shimmerInstance = rememberShimmer(shimmerBounds = ShimmerBounds.View)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp, horizontal = 12.dp)
      .height(100.dp)
      .shimmer(shimmerInstance),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Shimmer poster
    Box(
      modifier = Modifier
        .size(70.dp, 100.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.LightGray)
    )

    Spacer(modifier = Modifier.width(12.dp))

    // Shimmer content
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
      verticalArrangement = Arrangement.SpaceEvenly
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.8f)
          .height(18.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color.LightGray)
      )

      Box(
        modifier = Modifier
          .fillMaxWidth(0.5f)
          .height(14.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color.LightGray)
      )

      Box(
        modifier = Modifier
          .fillMaxWidth(0.3f)
          .height(14.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color.LightGray)
      )
    }
  }
}
