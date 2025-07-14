package com.example.filmera.ui.screen.home.component.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filmera.model.credit.Cast
import com.example.filmera.model.credit.Crew

@Composable
fun CreditSection(
  castList: List<Cast>,
  crewList: List<Crew>
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    //Main Cast
    if (castList.isNotEmpty()) {
      Text(
        text = "Main Cast",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))

      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(castList.take(8)) { cast ->
          CastItem(cast)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      //Supporting Cast
      if (castList.size > 8) {
        Text(
          text = "Supporting Cast",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(castList.drop(8)) { cast ->
            CastItem(cast)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    //Top Crew
    val topJobs = listOf("Director", "Writer", "Screenplay", "Music", "Producer")
    val topCrew = crewList.filter { it.job in topJobs }.distinctBy { it.name }

    if (topCrew.isNotEmpty()) {
      Text(
        text = "Top Crew",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))

      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(topCrew) { crew ->
          CrewItem(crew)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    //Full Crew List
    if (crewList.isNotEmpty()) {
      Text(
        text = "Full Crew",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 250.dp)
      ) {
        items(crewList) { crew ->
          Text(text = "${crew.job}: ${crew.name}", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}