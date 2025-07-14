//package com.example.filmera.ui.screen.home
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalFocusManager
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController
//import com.example.filmera.model.Movie
//import com.example.filmera.viewModel.MovieViewModel
//import com.example.filmera.viewModel.SearchViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ExploreScreen(
//  navController: NavController,
//  movieViewModel: MovieViewModel = hiltViewModel(),
//  searchViewModel: SearchViewModel = hiltViewModel()
//) {
//  val focusManager = LocalFocusManager.current
//  var query by remember { mutableStateOf("") }
//  var hasSearched by remember { mutableStateOf(false) }
//
//  val allPopularMovies = movieViewModel.popularMovies.collectAsState().value
//  val searchResults = searchViewModel.searchResults.collectAsState().value
//  val isSearching = searchViewModel.isSearching.collectAsState().value
//
//  // Live suggestions while typing
//  val liveSuggestions = if (query.isNotEmpty() && !hasSearched) {
//    allPopularMovies.filter {
//      it.title.contains(query, ignoreCase = true)
//    }.take(5)
//  } else emptyList()
//
//  Column(
//    modifier = Modifier
//      .fillMaxSize()
//      .padding(16.dp)
//  ) {
//    // Search Bar
//    Row(
//      verticalAlignment = Alignment.CenterVertically,
//      modifier = Modifier.fillMaxWidth()
//    ) {
//      Box(
//        modifier = Modifier
//          .weight(1f)
//          .background(Color.DarkGray, shape = RoundedCornerShape(25.dp))
//          .padding(horizontal = 16.dp, vertical = 12.dp)
//      ) {
//        Row(
//          verticalAlignment = Alignment.CenterVertically
//        ) {
//          Icon(
//            imageVector = Icons.Default.Search,
//            contentDescription = "Search",
//            tint = Color.Gray,
//            modifier = Modifier.size(20.dp)
//          )
//          Spacer(modifier = Modifier.width(8.dp))
//          BasicTextField(
//            value = query,
//            onValueChange = {
//              query = it
//              hasSearched = false
//              // Clear previous search results when typing
//              if (it.isEmpty()) {
//                searchViewModel.clearSearch()
//              }
//            },
//            singleLine = true,
//            textStyle = TextStyle(
//              color = Color.White,
//              fontSize = 16.sp
//            ),
//            keyboardOptions = KeyboardOptions(
//              imeAction = ImeAction.Search
//            ),
//            keyboardActions = KeyboardActions(
//              onSearch = {
//                if (query.isNotEmpty()) {
//                  hasSearched = true
//                  searchViewModel.searchMovies(query)
//                  focusManager.clearFocus()
//                }
//              }
//            ),
//            decorationBox = { innerTextField ->
//              if (query.isEmpty()) {
//                Text(
//                  text = "Input content or names you want to search",
//                  color = Color.Gray,
//                  fontSize = 16.sp
//                )
//              }
//              innerTextField()
//            },
//            modifier = Modifier.weight(1f)
//          )
//          if (query.isNotEmpty()) {
//            IconButton(
//              onClick = {
//                query = ""
//                hasSearched = false
//                searchViewModel.clearSearch()
//                focusManager.clearFocus()
//              }
//            ) {
//              Icon(
//                imageVector = Icons.Default.Close,
//                contentDescription = "Clear",
//                tint = Color.Gray,
//                modifier = Modifier.size(20.dp)
//              )
//            }
//          }
//        }
//      }
//
//      if (query.isNotEmpty()) {
//        TextButton(
//          onClick = {
//            query = ""
//            hasSearched = false
//            searchViewModel.clearSearch()
//            focusManager.clearFocus()
//          }
//        ) {
//          Text("Cancel", color = Color.White)
//        }
//      }
//    }
//
//    Spacer(modifier = Modifier.height(16.dp))
//
//    // Content based on state
//    when {
//      // Show live suggestions while typing (before search)
//      query.isNotEmpty() && !hasSearched && liveSuggestions.isNotEmpty() -> {
//        LazyColumn {
//          items(liveSuggestions) { movie ->
//            SearchSuggestionItem(
//              movie = movie,
//              query = query,
//              onClick = {
//                query = movie.title
//                hasSearched = true
//                searchViewModel.searchMovies(movie.title)
//                focusManager.clearFocus()
//              }
//            )
//          }
//        }
//      }
//
//      // Show search results after searching
//      hasSearched && query.isNotEmpty() -> {
//        if (isSearching) {
//          Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//          ) {
//            CircularProgressIndicator(color = Color.White)
//          }
//        } else {
//          if (searchResults.isNotEmpty()) {
//            LazyColumn {
//              items(searchResults) { movie: Movie ->
//                SearchResultItem(
//                  movie = movie,
//                  onClick = {
//                    // Navigate to detail screen
//                    // navController.navigate("detail/${movie.id}")
//                  }
//                )
//              }
//            }
//          } else {
//            Box(
//              modifier = Modifier.fillMaxSize(),
//              contentAlignment = Alignment.Center
//            ) {
//              Text(
//                text = "No results found for '$query'",
//                color = Color.Gray,
//                fontSize = 16.sp
//              )
//            }
//          }
//        }
//      }
//
//      // Show top searches when not searching
//      else -> {
//        Text(
//          text = "Top searches",
//          style = MaterialTheme.typography.titleMedium,
//          color = Color.White,
//          modifier = Modifier.padding(vertical = 8.dp)
//        )
//        LazyColumn {
//          items(allPopularMovies.take(10)) { movie ->
//            TopSearchItem(
//              movie = movie,
//              rank = allPopularMovies.indexOf(movie) + 1,
//              onClick = {
//                // Navigate to detail screen
//                // navController.navigate("detail/${movie.id}")
//              }
//            )
//          }
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun SearchSuggestionItem(
//  movie: Movie,
//  query: String,
//  onClick: () -> Unit
//) {
//  Text(
//    text = movie.title,
//    style = MaterialTheme.typography.bodyLarge,
//    color = Color.White,
//    modifier = Modifier
//      .fillMaxWidth()
//      .clickable { onClick() }
//      .padding(vertical = 12.dp, horizontal = 8.dp)
//  )
//}
//
//@Composable
//fun SearchResultItem(
//  movie: Movie,
//  onClick: () -> Unit
//) {
//  Row(
//    modifier = Modifier
//      .fillMaxWidth()
//      .clickable { onClick() }
//      .padding(vertical = 8.dp),
//    verticalAlignment = Alignment.CenterVertically
//  ) {
//    // Movie poster placeholder
//    Box(
//      modifier = Modifier
//        .size(80.dp, 120.dp)
//        .background(Color.Gray, RoundedCornerShape(8.dp))
//    ) {
//      // Replace with your image loading composable
//      // AsyncImage(model = movie.posterUrl, ...)
//    }
//
//    Spacer(modifier = Modifier.width(12.dp))
//
//    Column {
//      Text(
//        text = movie.title,
//        style = MaterialTheme.typography.bodyLarge,
//        color = Color.White,
//        fontWeight = FontWeight.Medium
//      )
//      // Add more movie details as needed
//      Text(
//        text = movie.overview?.take(100) + "..." ?: "",
//        style = MaterialTheme.typography.bodySmall,
//        color = Color.Gray,
//        maxLines = 2
//      )
//    }
//  }
//}
//
//@Composable
//fun TopSearchItem(
//  movie: Movie,
//  rank: Int,
//  onClick: () -> Unit
//) {
//  Row(
//    modifier = Modifier
//      .fillMaxWidth()
//      .clickable { onClick() }
//      .padding(vertical = 8.dp),
//    verticalAlignment = Alignment.CenterVertically
//  ) {
//    // Rank indicator
//    Box(
//      modifier = Modifier
//        .size(24.dp)
//        .background(Color.Red, CircleShape),
//      contentAlignment = Alignment.Center
//    ) {
//      Text(
//        text = rank.toString(),
//        color = Color.White,
//        fontSize = 12.sp,
//        fontWeight = FontWeight.Bold
//      )
//    }
//
//    Spacer(modifier = Modifier.width(12.dp))
//
//    // Movie poster
//    Box(
//      modifier = Modifier
//        .size(60.dp, 80.dp)
//        .background(Color.Gray, RoundedCornerShape(4.dp))
//    ) {
//      // Replace with your image loading composable
//      // AsyncImage(model = movie.posterUrl, ...)
//    }
//
//    Spacer(modifier = Modifier.width(12.dp))
//
//    Column {
//      Text(
//        text = movie.title,
//        style = MaterialTheme.typography.bodyLarge,
//        color = Color.White,
//        fontWeight = FontWeight.Medium
//      )
//      // Add genre or other info
//      Text(
//        text = "Movie", // or movie.genre
//        style = MaterialTheme.typography.bodySmall,
//        color = Color.Gray
//      )
//    }
//  }
//}