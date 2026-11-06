---
name: tmdb-api-v3
version: 1.0.0
description: >-
  Complete operational guide for integrating The Movie Database (TMDB) API v3
  into FILMERA or another movie/TV application. Use for authentication, search,
  discovery, movie, TV series, season, episode, people, account, favorites,
  watchlist, ratings, images, videos, providers, and JSON DTO design.
last_verified: 2026-07-27
primary_language: Indonesian
---

# TMDB API v3 Skill

## 1. Tujuan skill

Gunakan skill ini ketika tugas berkaitan dengan:

- membuat atau memperbaiki `TmdbApiService`;
- menentukan endpoint TMDB yang tepat;
- membuat DTO Kotlin/Kotlinx Serialization/Gson;
- mengambil Movie, TV Series, Season, Episode, Person, Credits, Images, Video, dan Watch Provider;
- membuat fitur akun TMDB seperti favorite, watchlist, rating, dan custom list;
- membuat halaman Home, Search, Detail, Artist, Season, Episode, Favorites, Watchlist, atau Discover;
- memahami bentuk JSON TMDB dan field yang dapat `null`;
- menggabungkan beberapa sub-endpoint dengan `append_to_response`.

Skill ini berfokus pada **TMDB API v3** dengan base URL:

```text
https://api.themoviedb.org/3/
```

Dokumentasi resmi:

```text
https://developer.themoviedb.org/reference
https://developer.themoviedb.org/openapi/tmdb-api.json
```

---

## 2. Aturan wajib untuk agent

1. Gunakan dokumentasi resmi TMDB sebagai sumber utama.
2. Jangan pernah menulis token asli ke source code, Git, log, screenshot, atau jawaban.
3. Gunakan `Authorization: Bearer <TMDB_READ_ACCESS_TOKEN>` untuk application authentication.
4. Gunakan `session_id` hanya untuk operasi akun pengguna seperti favorite, watchlist, rating, dan list.
5. Anggap field path gambar, tanggal, homepage, biography, external ID, dan episode berikutnya dapat `null`.
6. Jangan menganggap TMDB menyediakan video film penuh. Endpoint `videos` biasanya berisi metadata trailer/teaser, sering kali berupa `key` YouTube.
7. Watch provider hanya memberi informasi ketersediaan streaming/rent/buy per negara. Tampilkan atribusi **JustWatch** ketika memakai data provider.
8. Untuk pencarian lintas tipe, baca `media_type` sebelum mapping JSON.
9. Gunakan pagination; jangan mengasumsikan semua hasil berada pada halaman pertama.
10. Gunakan `append_to_response` untuk halaman detail agar request lebih efisien, maksimal 20 sub-request per detail request.
11. Jangan membuat semua field non-null hanya karena contoh JSON berisi nilai. API produksi dapat mengembalikan `null`, string kosong, array kosong, atau field tambahan.
12. Jangan memakai `person/latest` atau `movie/latest` sebagai daftar konten terbaru. Endpoint tersebut hanya mengembalikan ID terbaru yang dibuat di database.
13. Untuk tanggal rilis bioskop per negara, prioritaskan `release_dates`, bukan hanya `release_date` dari Movie Details.
14. Untuk credit TV lintas seluruh episode, prioritaskan `aggregate_credits`; endpoint `credits` TV hanya merepresentasikan credit season terbaru.
15. Untuk custom list campuran Movie + TV dan fitur list modern, pertimbangkan API v4. List v3 tetap bekerja tetapi lebih terbatas.

---

## 3. Authentication

### 3.1 Application authentication

Metode yang disarankan:

```http
Authorization: Bearer TMDB_READ_ACCESS_TOKEN
Accept: application/json
```

Contoh cURL:

```bash
curl --request GET \
  --url 'https://api.themoviedb.org/3/movie/550?language=id-ID' \
  --header 'Authorization: Bearer TMDB_READ_ACCESS_TOKEN' \
  --header 'accept: application/json'
```

Alternatif lama:

```text
?api_key=TMDB_V3_API_KEY
```

Jangan mengirim bearer token dan API key sekaligus tanpa alasan.

### 3.2 User authentication dan `session_id`

Alur normal:

1. `GET /authentication/token/new`
2. Arahkan pengguna ke `https://www.themoviedb.org/authenticate/{REQUEST_TOKEN}`
3. Setelah disetujui, panggil `POST /authentication/session/new`
4. Simpan `session_id` secara aman
5. Ambil detail account untuk memperoleh `account_id`

Request membuat session:

```json
{
  "request_token": "approved_request_token"
}
```

Response:

```json
{
  "success": true,
  "session_id": "tmdb_user_session_id"
}
```

`session_id` bersifat rahasia seperti password. Jangan menyimpannya di repository atau log.

### 3.3 Guest session

Guest session terutama untuk rating terbatas:

```http
GET /authentication/guest_session/new
```

Response:

```json
{
  "success": true,
  "guest_session_id": "guest_session_token",
  "expires_at": "2026-07-28 12:00:00 UTC"
}
```

Gunakan full user session untuk favorite, watchlist, dan sinkronisasi account yang stabil.

---

## 4. Parameter umum

| Parameter | Contoh | Kegunaan |
|---|---:|---|
| `language` | `id-ID` | Bahasa title, overview, dan metadata terjemahan |
| `region` | `ID` | Filter atau penyesuaian hasil berdasarkan wilayah |
| `watch_region` | `ID` | Negara untuk filter watch provider |
| `page` | `1` | Pagination, dimulai dari 1 |
| `include_adult` | `false` | Menyertakan konten dewasa pada search/discover |
| `append_to_response` | `credits,videos` | Menggabungkan sub-resource ke detail response |
| `include_image_language` | `id,en,null` | Fallback bahasa gambar |
| `include_video_language` | `id,en` | Filter bahasa video |
| `session_id` | rahasia | Operasi akun pengguna |
| `guest_session_id` | rahasia | Operasi rating guest |

Rekomendasi untuk aplikasi Indonesia:

```text
language=id-ID
region=ID
watch_region=ID
include_image_language=id,en,null
include_video_language=id,en
```

TMDB tidak selalu memiliki terjemahan Indonesia. UI harus fallback ke `original_title`, `original_name`, atau data bahasa Inggris.

---

## 5. Image URL

TMDB mengembalikan path seperti:

```json
{
  "poster_path": "/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg",
  "backdrop_path": "/2w4xG178RpB4MDAIfTkqAuSJzec.jpg"
}
```

Path tersebut bukan URL lengkap.

Format URL:

```text
https://image.tmdb.org/t/p/{size}{file_path}
```

Contoh:

```text
https://image.tmdb.org/t/p/w500/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg
https://image.tmdb.org/t/p/original/2w4xG178RpB4MDAIfTkqAuSJzec.jpg
```

Ukuran sebaiknya dibaca dari `GET /configuration`, bukan di-hardcode selamanya.

Helper Kotlin:

```kotlin
fun tmdbImageUrl(path: String?, size: String = "w500"): String? {
  if (path.isNullOrBlank()) return null
  return "https://image.tmdb.org/t/p/$size$path"
}
```

---

# 6. Endpoint catalog

## 6.1 Authentication

| Method | Endpoint | Kegunaan | Body/Query | Return |
|---|---|---|---|---|
| GET | `/authentication` | Validasi bearer/API key | - | `ValidationResponse` |
| GET | `/authentication/token/new` | Membuat temporary request token | - | `RequestTokenResponse` |
| POST | `/authentication/session/new` | Membuat user session dari approved request token | body `request_token` | `SessionResponse` |
| POST | `/authentication/token/validate_with_login` | Validasi token memakai username/password; bukan pilihan utama | body credentials + token | `RequestTokenResponse` |
| POST | `/authentication/session/convert/4` | Membuat v3 session dari v4 access token | body `access_token` | `SessionResponse` |
| GET | `/authentication/guest_session/new` | Membuat guest session | - | `GuestSessionResponse` |
| DELETE | `/authentication/session` | Logout/menghapus session | body `session_id` | `SuccessResponse` |

## 6.2 Account

Semua endpoint account membutuhkan `account_id`. Operasi user membutuhkan `session_id`.

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/account/{account_id}` | Detail profil account TMDB | `AccountDetails` |
| POST | `/account/{account_id}/favorite` | Tambah/hapus Movie atau TV dari favorite | `ActionResponse` |
| POST | `/account/{account_id}/watchlist` | Tambah/hapus Movie atau TV dari watchlist | `ActionResponse` |
| GET | `/account/{account_id}/favorite/movies` | Daftar favorite Movie | `Page<MovieSummary>` |
| GET | `/account/{account_id}/favorite/tv` | Daftar favorite TV | `Page<TvSummary>` |
| GET | `/account/{account_id}/watchlist/movies` | Watchlist Movie | `Page<MovieSummary>` |
| GET | `/account/{account_id}/watchlist/tv` | Watchlist TV | `Page<TvSummary>` |
| GET | `/account/{account_id}/rated/movies` | Movie yang diberi rating | `Page<RatedMovie>` |
| GET | `/account/{account_id}/rated/tv` | TV yang diberi rating | `Page<RatedTv>` |
| GET | `/account/{account_id}/rated/tv/episodes` | Episode yang diberi rating | `Page<RatedEpisode>` |
| GET | `/account/{account_id}/lists` | Custom list milik user | `Page<ListSummary>` |

Body favorite:

```json
{
  "media_type": "movie",
  "media_id": 550,
  "favorite": true
}
```

`media_type` hanya `movie` atau `tv`.

Body watchlist:

```json
{
  "media_type": "tv",
  "media_id": 1399,
  "watchlist": true
}
```

Gunakan `false` untuk menghapus item.

## 6.3 Movie

### Movie detail dan sub-resource

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/movie/{movie_id}` | Detail utama Movie | `MovieDetails` |
| GET | `/movie/{movie_id}/account_states` | Status favorite, watchlist, dan rating user | `AccountStates` |
| GET | `/movie/{movie_id}/alternative_titles` | Judul alternatif per negara | `AlternativeTitlesResponse` |
| GET | `/movie/{movie_id}/changes` | Perubahan data Movie, maksimal rentang 14 hari | `ChangesResponse` |
| GET | `/movie/{movie_id}/credits` | Cast dan crew | `CreditsResponse` |
| GET | `/movie/{movie_id}/external_ids` | IMDb, Wikidata, Facebook, Instagram, X/Twitter | `ExternalIds` |
| GET | `/movie/{movie_id}/images` | Poster, backdrop, logo | `ImagesResponse` |
| GET | `/movie/{movie_id}/keywords` | Keyword/topik Movie | `MovieKeywordsResponse` |
| GET | `/movie/{movie_id}/lists` | Public lists yang memuat Movie | `Page<ListSummary>` |
| GET | `/movie/{movie_id}/recommendations` | Rekomendasi berdasarkan data TMDB | `Page<MovieSummary>` |
| GET | `/movie/{movie_id}/release_dates` | Tanggal rilis dan sertifikasi per negara | `ReleaseDatesResponse` |
| GET | `/movie/{movie_id}/reviews` | Review user TMDB | `Page<ReviewSummary>` |
| GET | `/movie/{movie_id}/similar` | Movie serupa | `Page<MovieSummary>` |
| GET | `/movie/{movie_id}/translations` | Terjemahan metadata | `TranslationsResponse` |
| GET | `/movie/{movie_id}/videos` | Trailer, teaser, clip, featurette | `VideosResponse` |
| GET | `/movie/{movie_id}/watch/providers` | Streaming/rent/buy per negara | `WatchProvidersResponse` |
| POST | `/movie/{movie_id}/rating` | Menambahkan rating | `ActionResponse` |
| DELETE | `/movie/{movie_id}/rating` | Menghapus rating | `ActionResponse` |

Rating body:

```json
{
  "value": 8.5
}
```

### Movie list

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/movie/now_playing` | Sedang tayang di bioskop berdasarkan region | `DatedPage<MovieSummary>` |
| GET | `/movie/popular` | Movie populer | `Page<MovieSummary>` |
| GET | `/movie/top_rated` | Movie rating tertinggi | `Page<MovieSummary>` |
| GET | `/movie/upcoming` | Movie mendatang berdasarkan region | `DatedPage<MovieSummary>` |
| GET | `/movie/latest` | Record Movie paling baru dibuat, bukan daftar upcoming | `MovieDetails` |

### Recommended Movie detail request

```http
GET /movie/{movie_id}
  ?language=id-ID
  &append_to_response=credits,videos,images,keywords,recommendations,similar,release_dates,watch/providers,external_ids,reviews,translations,account_states
  &include_image_language=id,en,null
  &include_video_language=id,en
  &session_id={SESSION_ID}
```

Hapus `account_states` dan `session_id` bila user belum login.

## 6.4 TV Series

### TV Series detail dan sub-resource

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/tv/{series_id}` | Detail utama TV Series | `TvSeriesDetails` |
| GET | `/tv/{series_id}/account_states` | Status account dan rating | `AccountStates` |
| GET | `/tv/{series_id}/aggregate_credits` | Gabungan cast/crew seluruh episode | `AggregateCreditsResponse` |
| GET | `/tv/{series_id}/alternative_titles` | Judul alternatif | `AlternativeTitlesResponse` |
| GET | `/tv/{series_id}/changes` | Perubahan TV Series, maksimal 14 hari | `ChangesResponse` |
| GET | `/tv/{series_id}/content_ratings` | Rating usia per negara | `ContentRatingsResponse` |
| GET | `/tv/{series_id}/credits` | Credit season terbaru | `CreditsResponse` |
| GET | `/tv/{series_id}/episode_groups` | Pengelompokan episode alternatif | `EpisodeGroupsResponse` |
| GET | `/tv/{series_id}/external_ids` | IMDb, TVDB, Wikidata, social IDs | `ExternalIds` |
| GET | `/tv/{series_id}/images` | Poster, backdrop, logo | `ImagesResponse` |
| GET | `/tv/{series_id}/keywords` | Keyword TV | `TvKeywordsResponse` |
| GET | `/tv/{series_id}/lists` | Public lists yang memuat TV Series | `Page<ListSummary>` |
| GET | `/tv/{series_id}/recommendations` | Rekomendasi TV | `Page<TvSummary>` |
| GET | `/tv/{series_id}/reviews` | Review user | `Page<ReviewSummary>` |
| GET | `/tv/{series_id}/screened_theatrically` | Episode yang pernah ditayangkan secara teatrikal | `ScreenedTheatricallyResponse` |
| GET | `/tv/{series_id}/similar` | TV serupa | `Page<TvSummary>` |
| GET | `/tv/{series_id}/translations` | Terjemahan metadata | `TranslationsResponse` |
| GET | `/tv/{series_id}/videos` | Trailer, teaser, clip | `VideosResponse` |
| GET | `/tv/{series_id}/watch/providers` | Streaming/rent/buy per negara | `WatchProvidersResponse` |
| POST | `/tv/{series_id}/rating` | Tambah rating TV | `ActionResponse` |
| DELETE | `/tv/{series_id}/rating` | Hapus rating TV | `ActionResponse` |

### TV list

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/tv/airing_today` | TV yang tayang hari ini berdasarkan timezone | `Page<TvSummary>` |
| GET | `/tv/on_the_air` | TV yang memiliki episode tayang dalam sekitar 7 hari | `Page<TvSummary>` |
| GET | `/tv/popular` | TV populer | `Page<TvSummary>` |
| GET | `/tv/top_rated` | TV rating tertinggi | `Page<TvSummary>` |
| GET | `/tv/latest` | Record TV paling baru dibuat | `TvSeriesDetails` |

### Recommended TV detail request

```http
GET /tv/{series_id}
  ?language=id-ID
  &append_to_response=aggregate_credits,credits,videos,images,keywords,recommendations,similar,content_ratings,watch/providers,external_ids,reviews,translations,episode_groups,account_states
  &include_image_language=id,en,null
  &include_video_language=id,en
  &session_id={SESSION_ID}
```

## 6.5 TV Season

Perhatikan perbedaan ID:

- detail menggunakan `series_id` + `season_number`;
- endpoint changes menggunakan internal `season_id`.

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/tv/{series_id}/season/{season_number}` | Detail season dan daftar episode | `TvSeasonDetails` |
| GET | `/tv/{series_id}/season/{season_number}/account_states` | Status account per episode season | `SeasonAccountStatesResponse` |
| GET | `/tv/{series_id}/season/{season_number}/aggregate_credits` | Credit gabungan season | `AggregateCreditsResponse` |
| GET | `/tv/{series_id}/season/{season_number}/credits` | Credit season | `CreditsResponse` |
| GET | `/tv/{series_id}/season/{season_number}/external_ids` | External IDs season | `ExternalIds` |
| GET | `/tv/{series_id}/season/{season_number}/images` | Poster season | `SeasonImagesResponse` |
| GET | `/tv/{series_id}/season/{season_number}/translations` | Terjemahan season | `TranslationsResponse` |
| GET | `/tv/{series_id}/season/{season_number}/videos` | Video season | `VideosResponse` |
| GET | `/tv/{series_id}/season/{season_number}/watch/providers` | Provider season | `WatchProvidersResponse` |
| GET | `/tv/season/{season_id}/changes` | Perubahan season berdasarkan internal ID | `ChangesResponse` |

Recommended request:

```http
GET /tv/{series_id}/season/{season_number}
  ?language=id-ID
  &append_to_response=aggregate_credits,credits,external_ids,images,translations,videos,watch/providers,account_states
  &include_image_language=id,en,null
  &include_video_language=id,en
```

## 6.6 TV Episode

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}` | Detail episode | `TvEpisodeDetails` |
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/account_states` | Rating/favorite/watchlist state | `AccountStates` |
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/credits` | Cast, crew, guest stars | `EpisodeCreditsResponse` |
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/external_ids` | External IDs episode | `ExternalIds` |
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/images` | Still images episode | `EpisodeImagesResponse` |
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/translations` | Terjemahan episode | `TranslationsResponse` |
| GET | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/videos` | Video episode | `VideosResponse` |
| POST | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/rating` | Tambah rating episode | `ActionResponse` |
| DELETE | `/tv/{series_id}/season/{season_number}/episode/{episode_number}/rating` | Hapus rating episode | `ActionResponse` |
| GET | `/tv/episode/{episode_id}/changes` | Perubahan episode berdasarkan internal ID | `ChangesResponse` |

Recommended request:

```http
GET /tv/{series_id}/season/{season_number}/episode/{episode_number}
  ?language=id-ID
  &append_to_response=credits,external_ids,images,translations,videos,account_states
  &include_image_language=id,en,null
  &include_video_language=id,en
```

## 6.7 TV Episode Groups

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/tv/episode_group/{tv_episode_group_id}` | Detail grouping episode seperti DVD, absolute, story arc | `EpisodeGroupDetails` |

Tipe group:

| Nilai | Jenis |
|---:|---|
| 1 | Original air date |
| 2 | Absolute |
| 3 | DVD |
| 4 | Digital |
| 5 | Story arc |
| 6 | Production |
| 7 | TV |

## 6.8 People

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/person/{person_id}` | Biodata artist/crew | `PersonDetails` |
| GET | `/person/{person_id}/changes` | Perubahan data person | `ChangesResponse` |
| GET | `/person/{person_id}/combined_credits` | Movie + TV credits | `CombinedCreditsResponse` |
| GET | `/person/{person_id}/external_ids` | IMDb dan social IDs | `ExternalIds` |
| GET | `/person/{person_id}/images` | Foto profile | `PersonImagesResponse` |
| GET | `/person/{person_id}/movie_credits` | Filmografi Movie | `PersonMovieCreditsResponse` |
| GET | `/person/{person_id}/tagged_images` | Gambar yang menandai person | `Page<TaggedImage>` |
| GET | `/person/{person_id}/translations` | Terjemahan biography | `TranslationsResponse` |
| GET | `/person/{person_id}/tv_credits` | Filmografi TV | `PersonTvCreditsResponse` |
| GET | `/person/popular` | Person populer | `Page<PersonSummary>` |
| GET | `/person/latest` | Record person terbaru dibuat | `PersonDetails` |

Recommended request:

```http
GET /person/{person_id}
  ?language=id-ID
  &append_to_response=combined_credits,movie_credits,tv_credits,images,external_ids,translations,tagged_images
```

## 6.9 Search

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/search/movie` | Cari Movie berdasarkan judul asli/terjemahan/alternatif | `Page<MovieSummary>` |
| GET | `/search/tv` | Cari TV Series | `Page<TvSummary>` |
| GET | `/search/person` | Cari actor/crew | `Page<PersonSummary>` |
| GET | `/search/multi` | Cari Movie, TV, dan Person sekaligus | `Page<MultiSearchResult>` |
| GET | `/search/collection` | Cari franchise/collection | `Page<CollectionSummary>` |
| GET | `/search/company` | Cari production company | `Page<CompanySummary>` |
| GET | `/search/keyword` | Cari keyword | `Page<Keyword>` |

Contoh:

```http
GET /search/multi?query=avatar&language=id-ID&page=1&include_adult=false
```

Mapping wajib:

```text
media_type == "movie"  -> MovieSummary
media_type == "tv"     -> TvSummary
media_type == "person" -> PersonSummary
```

## 6.10 Discover

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/discover/movie` | Browse Movie dengan filter kompleks | `Page<MovieSummary>` |
| GET | `/discover/tv` | Browse TV dengan filter kompleks | `Page<TvSummary>` |

Filter penting Movie:

```text
sort_by
primary_release_date.gte
primary_release_date.lte
release_date.gte
release_date.lte
with_release_type
with_genres
without_genres
with_keywords
without_keywords
with_cast
with_crew
with_people
with_companies
with_original_language
with_runtime.gte
with_runtime.lte
vote_average.gte
vote_average.lte
vote_count.gte
vote_count.lte
certification_country
certification
certification.gte
certification.lte
watch_region
with_watch_providers
with_watch_monetization_types
region
```

Filter penting TV:

```text
sort_by
air_date.gte
air_date.lte
first_air_date.gte
first_air_date.lte
first_air_date_year
with_genres
without_genres
with_keywords
without_keywords
with_networks
with_companies
with_origin_country
with_original_language
with_runtime.gte
with_runtime.lte
vote_average.gte
vote_average.lte
vote_count.gte
vote_count.lte
include_null_first_air_dates
screened_theatrically
timezone
watch_region
with_watch_providers
with_watch_monetization_types
with_status
with_type
```

Pemisah filter:

```text
comma: AND
pipe: OR
```

Contoh:

```text
with_genres=28,878       -> Action AND Science Fiction
with_genres=28|878       -> Action OR Science Fiction
```

## 6.11 Trending

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/trending/all/{time_window}` | Trending semua tipe | `Page<MultiSearchResult>` |
| GET | `/trending/movie/{time_window}` | Trending Movie | `Page<MovieSummary>` |
| GET | `/trending/tv/{time_window}` | Trending TV | `Page<TvSummary>` |
| GET | `/trending/person/{time_window}` | Trending Person | `Page<PersonSummary>` |

`time_window`:

```text
day
week
```

## 6.12 Collections

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/collection/{collection_id}` | Detail franchise/collection dan daftar part | `CollectionDetails` |
| GET | `/collection/{collection_id}/images` | Poster/backdrop collection | `ImagesResponse` |
| GET | `/collection/{collection_id}/translations` | Terjemahan collection | `TranslationsResponse` |

## 6.13 Companies dan Networks

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/company/{company_id}` | Detail production company | `CompanyDetails` |
| GET | `/company/{company_id}/alternative_names` | Nama alternatif company | `AlternativeNamesResponse` |
| GET | `/company/{company_id}/images` | Logo company | `CompanyImagesResponse` |
| GET | `/network/{network_id}` | Detail TV network | `NetworkDetails` |
| GET | `/network/{network_id}/alternative_names` | Nama alternatif network | `AlternativeNamesResponse` |
| GET | `/network/{network_id}/images` | Logo network | `CompanyImagesResponse` |

## 6.14 Keywords, Genres, Certifications

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/keyword/{keyword_id}` | Detail keyword | `Keyword` |
| GET | `/keyword/{keyword_id}/movies` | Movie dengan keyword; endpoint lama | `Page<MovieSummary>` |
| GET | `/genre/movie/list` | Genre Movie | `GenresResponse` |
| GET | `/genre/tv/list` | Genre TV | `GenresResponse` |
| GET | `/certification/movie/list` | Sertifikasi usia Movie per negara | `CertificationsResponse` |
| GET | `/certification/tv/list` | Sertifikasi usia TV per negara | `CertificationsResponse` |

## 6.15 Configuration

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/configuration` | Base URL dan ukuran image | `ConfigurationDetails` |
| GET | `/configuration/countries` | Negara yang didukung | `Country[]` |
| GET | `/configuration/jobs` | Department dan job crew | `DepartmentJobs[]` |
| GET | `/configuration/languages` | Bahasa yang didukung | `Language[]` |
| GET | `/configuration/primary_translations` | Kode primary translations | `String[]` |
| GET | `/configuration/timezones` | Timezone per negara | `TimezoneGroup[]` |

Configuration bersifat relatif statis. Cache lokal diperbolehkan dengan strategi refresh berkala.

## 6.16 Changes

### Daftar ID yang berubah

| Method | Endpoint | Return |
|---|---|---|
| GET | `/movie/changes` | `Page<ChangedId>` |
| GET | `/tv/changes` | `Page<ChangedId>` |
| GET | `/person/changes` | `Page<ChangedId>` |

### Detail perubahan item

```text
GET /movie/{movie_id}/changes
GET /tv/{series_id}/changes
GET /person/{person_id}/changes
GET /tv/season/{season_id}/changes
GET /tv/episode/{episode_id}/changes
```

Default perubahan biasanya 24 jam terakhir. `start_date` dan `end_date` mendukung maksimal rentang 14 hari per request.

## 6.17 Find by external ID

```http
GET /find/{external_id}?external_source=imdb_id
```

`external_source` yang umum:

```text
imdb_id
facebook_id
instagram_id
tvdb_id
tiktok_id
twitter_id
wikidata_id
youtube_id
```

Return:

```json
{
  "movie_results": [],
  "person_results": [],
  "tv_results": [],
  "tv_episode_results": [],
  "tv_season_results": []
}
```

## 6.18 Reviews dan Credits global

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/review/{review_id}` | Detail satu review | `ReviewDetails` |
| GET | `/credit/{credit_id}` | Detail satu credit Movie/TV | `CreditDetails` |

## 6.19 Watch Provider configuration

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/watch/providers/regions` | Negara provider yang tersedia | `RegionsResponse` |
| GET | `/watch/providers/movie` | Daftar provider Movie | `WatchProviderListResponse` |
| GET | `/watch/providers/tv` | Daftar provider TV | `WatchProviderListResponse` |

Data availability per item tetap di:

```text
/movie/{movie_id}/watch/providers
/tv/{series_id}/watch/providers
/tv/{series_id}/season/{season_number}/watch/providers
```

## 6.20 Lists v3

| Method | Endpoint | Kegunaan | Return |
|---|---|---|---|
| GET | `/list/{list_id}` | Detail list dan Movie di dalamnya | `ListDetails` |
| POST | `/list` | Membuat list | `CreateListResponse` |
| POST | `/list/{list_id}/add_item` | Menambah Movie | `ActionResponse` |
| POST | `/list/{list_id}/remove_item` | Menghapus Movie | `ActionResponse` |
| POST | `/list/{list_id}/clear` | Mengosongkan list | `ActionResponse` |
| DELETE | `/list/{list_id}` | Menghapus list | `ActionResponse` |
| GET | `/list/{list_id}/item_status?movie_id={id}` | Cek Movie berada di list | `ItemStatusResponse` |

List v3 berorientasi Movie. Gunakan API v4 bila perlu list campuran Movie + TV, private list, komentar item, dan fitur list modern.

## 6.21 Guest session result lists

| Method | Endpoint | Return |
|---|---|---|
| GET | `/guest_session/{guest_session_id}/rated/movies` | `Page<RatedMovie>` |
| GET | `/guest_session/{guest_session_id}/rated/tv` | `Page<RatedTv>` |
| GET | `/guest_session/{guest_session_id}/rated/tv/episodes` | `Page<RatedEpisode>` |

---

# 7. JSON response contracts

Bagian ini adalah kontrak praktis untuk membuat DTO. Contoh tidak menjamin setiap field selalu terisi.

## 7.1 Generic success dan error

### ActionResponse

```json
{
  "status_code": 1,
  "status_message": "Success."
}
```

Status lain yang umum ketika resource dibuat/dihapus tetap memakai struktur serupa.

### ErrorResponse

```json
{
  "success": false,
  "status_code": 7,
  "status_message": "Invalid API key: You must be granted a valid key."
}
```

DTO:

```kotlin
@Serializable
data class TmdbErrorDto(
  val success: Boolean? = null,
  @SerialName("status_code") val statusCode: Int? = null,
  @SerialName("status_message") val statusMessage: String? = null,
)
```

## 7.2 Pagination

### Page<T>

```json
{
  "page": 1,
  "results": [],
  "total_pages": 100,
  "total_results": 2000
}
```

### DatedPage<T>

`now_playing` dan `upcoming` dapat menambahkan rentang tanggal:

```json
{
  "dates": {
    "maximum": "2026-08-20",
    "minimum": "2026-07-01"
  },
  "page": 1,
  "results": [],
  "total_pages": 50,
  "total_results": 1000
}
```

## 7.3 MovieSummary

Digunakan oleh list, search, discover, recommendation, dan similar.

```json
{
  "adult": false,
  "backdrop_path": "/backdrop.jpg",
  "genre_ids": [28, 878],
  "id": 550,
  "original_language": "en",
  "original_title": "Original Movie Title",
  "overview": "Overview dapat kosong.",
  "popularity": 120.25,
  "poster_path": "/poster.jpg",
  "release_date": "1999-10-15",
  "title": "Localized Movie Title",
  "video": false,
  "vote_average": 8.4,
  "vote_count": 30000,
  "media_type": "movie"
}
```

`media_type` biasanya ada pada multi-search/trending, tetapi tidak selalu ada pada endpoint khusus Movie.

## 7.4 TvSummary

```json
{
  "adult": false,
  "backdrop_path": "/backdrop.jpg",
  "genre_ids": [18, 10765],
  "id": 1399,
  "origin_country": ["US"],
  "original_language": "en",
  "original_name": "Original Series Name",
  "overview": "Series overview.",
  "popularity": 300.5,
  "poster_path": "/poster.jpg",
  "first_air_date": "2011-04-17",
  "name": "Localized Series Name",
  "vote_average": 8.4,
  "vote_count": 20000,
  "media_type": "tv"
}
```

## 7.5 MovieDetails

```json
{
  "adult": false,
  "backdrop_path": "/backdrop.jpg",
  "belongs_to_collection": {
    "id": 10,
    "name": "Collection Name",
    "poster_path": "/collection-poster.jpg",
    "backdrop_path": "/collection-backdrop.jpg"
  },
  "budget": 11000000,
  "genres": [
    { "id": 12, "name": "Adventure" }
  ],
  "homepage": "https://official.example",
  "id": 11,
  "imdb_id": "tt0076759",
  "origin_country": ["US"],
  "original_language": "en",
  "original_title": "Original Title",
  "overview": "Movie overview.",
  "popularity": 20.69,
  "poster_path": "/poster.jpg",
  "production_companies": [
    {
      "id": 1,
      "logo_path": "/logo.png",
      "name": "Production Company",
      "origin_country": "US"
    }
  ],
  "production_countries": [
    { "iso_3166_1": "US", "name": "United States of America" }
  ],
  "release_date": "1977-05-25",
  "revenue": 775398007,
  "runtime": 121,
  "spoken_languages": [
    { "english_name": "English", "iso_639_1": "en", "name": "English" }
  ],
  "status": "Released",
  "tagline": "A tagline.",
  "title": "Localized Title",
  "video": false,
  "vote_average": 8.2,
  "vote_count": 22061
}
```

Nullable yang harus diperhatikan:

```text
backdrop_path
belongs_to_collection
homepage
imdb_id
poster_path
release_date dapat string kosong
production_companies[].logo_path
```

## 7.6 TvSeriesDetails

```json
{
  "adult": false,
  "backdrop_path": "/backdrop.jpg",
  "created_by": [
    {
      "id": 9813,
      "credit_id": "credit-id",
      "name": "Creator Name",
      "gender": 2,
      "profile_path": "/profile.jpg"
    }
  ],
  "episode_run_time": [60],
  "first_air_date": "2011-04-17",
  "genres": [
    { "id": 10765, "name": "Sci-Fi & Fantasy" }
  ],
  "homepage": "https://official.example",
  "id": 1399,
  "in_production": false,
  "languages": ["en"],
  "last_air_date": "2019-05-19",
  "last_episode_to_air": {
    "id": 1551830,
    "name": "Episode Name",
    "overview": "Episode overview.",
    "vote_average": 8.0,
    "vote_count": 100,
    "air_date": "2019-05-19",
    "episode_number": 6,
    "episode_type": "finale",
    "production_code": "806",
    "runtime": 80,
    "season_number": 8,
    "show_id": 1399,
    "still_path": "/still.jpg"
  },
  "name": "Localized Series Name",
  "next_episode_to_air": null,
  "networks": [
    {
      "id": 49,
      "logo_path": "/network.png",
      "name": "Network Name",
      "origin_country": "US"
    }
  ],
  "number_of_episodes": 73,
  "number_of_seasons": 8,
  "origin_country": ["US"],
  "original_language": "en",
  "original_name": "Original Series Name",
  "overview": "Series overview.",
  "popularity": 346.0,
  "poster_path": "/poster.jpg",
  "production_companies": [
    {
      "id": 76043,
      "logo_path": "/company.png",
      "name": "Production Company",
      "origin_country": "US"
    }
  ],
  "production_countries": [
    { "iso_3166_1": "GB", "name": "United Kingdom" }
  ],
  "seasons": [
    {
      "air_date": "2011-04-17",
      "episode_count": 10,
      "id": 3624,
      "name": "Season 1",
      "overview": "Season overview.",
      "poster_path": "/season.jpg",
      "season_number": 1,
      "vote_average": 8.4
    }
  ],
  "spoken_languages": [
    { "english_name": "English", "iso_639_1": "en", "name": "English" }
  ],
  "status": "Ended",
  "tagline": "Series tagline.",
  "type": "Scripted",
  "vote_average": 8.4,
  "vote_count": 21390
}
```

`next_episode_to_air`, `last_episode_to_air`, `homepage`, image paths, dan tanggal dapat `null`.

## 7.7 TvSeasonDetails

```json
{
  "_id": "internal-string-id",
  "air_date": "2011-04-17",
  "episodes": [
    {
      "air_date": "2011-04-17",
      "episode_number": 1,
      "episode_type": "standard",
      "id": 63056,
      "name": "Episode Name",
      "overview": "Episode overview.",
      "production_code": "101",
      "runtime": 62,
      "season_number": 1,
      "show_id": 1399,
      "still_path": "/still.jpg",
      "vote_average": 8.1,
      "vote_count": 396,
      "crew": [
        {
          "department": "Directing",
          "job": "Director",
          "credit_id": "credit-id",
          "adult": false,
          "gender": 2,
          "id": 44797,
          "known_for_department": "Directing",
          "name": "Crew Name",
          "original_name": "Crew Original Name",
          "popularity": 1.0,
          "profile_path": "/profile.jpg"
        }
      ],
      "guest_stars": [
        {
          "character": "Character",
          "credit_id": "credit-id",
          "order": 10,
          "adult": false,
          "gender": 2,
          "id": 119783,
          "known_for_department": "Acting",
          "name": "Guest Name",
          "original_name": "Guest Original Name",
          "popularity": 1.0,
          "profile_path": "/profile.jpg"
        }
      ]
    }
  ],
  "name": "Season 1",
  "networks": [
    {
      "id": 49,
      "logo_path": "/network.png",
      "name": "Network",
      "origin_country": "US"
    }
  ],
  "overview": "Season overview.",
  "id": 3624,
  "poster_path": "/season.jpg",
  "season_number": 1,
  "vote_average": 8.4
}
```

## 7.8 TvEpisodeDetails

```json
{
  "air_date": "2011-04-17",
  "crew": [
    {
      "department": "Directing",
      "job": "Director",
      "credit_id": "credit-id",
      "adult": false,
      "gender": 2,
      "id": 44797,
      "known_for_department": "Directing",
      "name": "Crew Name",
      "original_name": "Crew Original Name",
      "popularity": 7.7,
      "profile_path": "/profile.jpg"
    }
  ],
  "episode_number": 1,
  "guest_stars": [
    {
      "character": "Character",
      "credit_id": "credit-id",
      "order": 62,
      "adult": false,
      "gender": 2,
      "id": 119783,
      "known_for_department": "Acting",
      "name": "Guest Name",
      "original_name": "Guest Original Name",
      "popularity": 6.7,
      "profile_path": "/profile.jpg"
    }
  ],
  "name": "Episode Name",
  "overview": "Episode overview.",
  "id": 63056,
  "production_code": "101",
  "runtime": 62,
  "season_number": 1,
  "still_path": "/still.jpg",
  "vote_average": 7.8,
  "vote_count": 286
}
```

## 7.9 PersonDetails

```json
{
  "adult": false,
  "also_known_as": ["Alternative Name"],
  "biography": "Biography text.",
  "birthday": "1956-07-09",
  "deathday": null,
  "gender": 2,
  "homepage": null,
  "id": 31,
  "imdb_id": "nm0000158",
  "known_for_department": "Acting",
  "name": "Person Name",
  "place_of_birth": "City, Country",
  "popularity": 82.9,
  "profile_path": "/profile.jpg"
}
```

Gender enum:

| Nilai | Arti |
|---:|---|
| 0 | Not set / not specified |
| 1 | Female |
| 2 | Male |
| 3 | Non-binary |

## 7.10 CreditsResponse

```json
{
  "id": 550,
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 819,
      "known_for_department": "Acting",
      "name": "Actor Name",
      "original_name": "Actor Original Name",
      "popularity": 50.0,
      "profile_path": "/profile.jpg",
      "cast_id": 4,
      "character": "Character Name",
      "credit_id": "credit-id",
      "order": 0
    }
  ],
  "crew": [
    {
      "adult": false,
      "gender": 2,
      "id": 7467,
      "known_for_department": "Directing",
      "name": "Crew Name",
      "original_name": "Crew Original Name",
      "popularity": 20.0,
      "profile_path": "/profile.jpg",
      "credit_id": "credit-id",
      "department": "Directing",
      "job": "Director"
    }
  ]
}
```

TV Episode Credits dapat menambahkan:

```json
{
  "guest_stars": []
}
```

## 7.11 AggregateCreditsResponse

Cast TV aggregate tidak hanya memiliki satu `character`; satu person dapat memiliki banyak role.

```json
{
  "id": 1399,
  "cast": [
    {
      "adult": false,
      "gender": 2,
      "id": 123,
      "known_for_department": "Acting",
      "name": "Actor Name",
      "original_name": "Actor Name",
      "popularity": 10.0,
      "profile_path": "/profile.jpg",
      "roles": [
        {
          "credit_id": "credit-id",
          "character": "Character",
          "episode_count": 70
        }
      ],
      "total_episode_count": 70,
      "order": 0
    }
  ],
  "crew": [
    {
      "adult": false,
      "gender": 2,
      "id": 456,
      "known_for_department": "Production",
      "name": "Crew Name",
      "original_name": "Crew Name",
      "popularity": 5.0,
      "profile_path": "/profile.jpg",
      "jobs": [
        {
          "credit_id": "credit-id",
          "job": "Executive Producer",
          "episode_count": 50
        }
      ],
      "department": "Production",
      "total_episode_count": 50
    }
  ]
}
```

## 7.12 CombinedCreditsResponse

```json
{
  "id": 31,
  "cast": [
    {
      "id": 100,
      "media_type": "movie",
      "title": "Movie Title",
      "original_title": "Original Movie Title",
      "release_date": "2020-01-01",
      "character": "Character",
      "credit_id": "credit-id",
      "order": 0,
      "poster_path": "/poster.jpg",
      "backdrop_path": "/backdrop.jpg",
      "overview": "Overview",
      "genre_ids": [18],
      "original_language": "en",
      "popularity": 10.0,
      "vote_average": 7.5,
      "vote_count": 1000
    },
    {
      "id": 200,
      "media_type": "tv",
      "name": "TV Name",
      "original_name": "Original TV Name",
      "first_air_date": "2021-01-01",
      "character": "Character",
      "credit_id": "credit-id",
      "episode_count": 10,
      "poster_path": "/poster.jpg"
    }
  ],
  "crew": []
}
```

Mapping berdasarkan `media_type`.

## 7.13 ImagesResponse

```json
{
  "id": 550,
  "backdrops": [
    {
      "aspect_ratio": 1.778,
      "height": 1080,
      "iso_639_1": null,
      "file_path": "/backdrop.jpg",
      "vote_average": 5.6,
      "vote_count": 20,
      "width": 1920
    }
  ],
  "logos": [
    {
      "aspect_ratio": 5.2,
      "height": 300,
      "iso_639_1": "en",
      "file_path": "/logo.png",
      "vote_average": 5.3,
      "vote_count": 1,
      "width": 1500
    }
  ],
  "posters": [
    {
      "aspect_ratio": 0.667,
      "height": 1500,
      "iso_639_1": "id",
      "file_path": "/poster.jpg",
      "vote_average": 5.2,
      "vote_count": 6,
      "width": 1000
    }
  ]
}
```

Variasi:

- Person Images: `id`, `profiles`;
- Episode Images: `id`, `stills`;
- Season Images: `id`, `posters`;
- Company/Network Images: `id`, `logos`.

## 7.14 VideosResponse

```json
{
  "id": 550,
  "results": [
    {
      "iso_639_1": "en",
      "iso_3166_1": "US",
      "name": "Official Trailer",
      "key": "O-b2VfmmbyA",
      "site": "YouTube",
      "size": 1080,
      "type": "Trailer",
      "official": true,
      "published_at": "2026-01-01T10:00:00.000Z",
      "id": "video-record-id"
    }
  ]
}
```

YouTube URL bila `site == "YouTube"`:

```text
https://www.youtube.com/watch?v={key}
https://www.youtube.com/embed/{key}
```

Jangan membangun URL YouTube untuk provider site lain.

Pemilihan trailer:

1. `official == true`
2. `type == "Trailer"`
3. bahasa user, lalu English
4. ukuran terbesar
5. tanggal terbaru

## 7.15 Keywords

Movie:

```json
{
  "id": 550,
  "keywords": [
    { "id": 825, "name": "support group" }
  ]
}
```

TV:

```json
{
  "id": 1399,
  "results": [
    { "id": 123, "name": "fantasy world" }
  ]
}
```

Perbedaan nama field `keywords` dan `results` harus ditangani DTO terpisah atau custom mapping.

## 7.16 ExternalIds

Superset praktis:

```json
{
  "id": 1399,
  "imdb_id": "tt0944947",
  "freebase_mid": null,
  "freebase_id": null,
  "tvdb_id": 121361,
  "tvrage_id": null,
  "wikidata_id": "Q23572",
  "facebook_id": "GameOfThrones",
  "instagram_id": "gameofthrones",
  "twitter_id": "GameOfThrones"
}
```

Tidak semua resource memiliki semua field.

## 7.17 AlternativeTitlesResponse

Movie biasanya memakai `titles`, TV dapat memakai `results` tergantung endpoint/schema.

```json
{
  "id": 550,
  "titles": [
    {
      "iso_3166_1": "ID",
      "title": "Judul Alternatif",
      "type": ""
    }
  ]
}
```

Gunakan DTO terpisah bila nama array berbeda.

## 7.18 TranslationsResponse

```json
{
  "id": 550,
  "translations": [
    {
      "iso_3166_1": "ID",
      "iso_639_1": "id",
      "name": "Bahasa Indonesia",
      "english_name": "Indonesian",
      "data": {
        "homepage": "",
        "overview": "Ringkasan terjemahan.",
        "runtime": 120,
        "status": "Released",
        "tagline": "Tagline",
        "title": "Judul Indonesia",
        "name": "Nama TV/Season/Episode",
        "biography": "Biography person",
        "translation": "Optional translated text"
      }
    }
  ]
}
```

Isi `data` berbeda berdasarkan resource. Jadikan field nullable atau gunakan DTO per resource.

## 7.19 Reviews

### ReviewSummary

```json
{
  "author": "username",
  "author_details": {
    "name": "Display Name",
    "username": "username",
    "avatar_path": "/avatar.jpg",
    "rating": 8.0
  },
  "content": "Review text.",
  "created_at": "2026-01-01T10:00:00.000Z",
  "id": "review-id",
  "updated_at": "2026-01-02T10:00:00.000Z",
  "url": "https://www.themoviedb.org/review/review-id"
}
```

### ReviewDetails

```json
{
  "author": "username",
  "author_details": {},
  "content": "Review text.",
  "created_at": "2026-01-01T10:00:00.000Z",
  "id": "review-id",
  "iso_639_1": "en",
  "media_id": 550,
  "media_title": "Movie Title",
  "media_type": "movie",
  "updated_at": "2026-01-02T10:00:00.000Z",
  "url": "https://www.themoviedb.org/review/review-id"
}
```

## 7.20 WatchProvidersResponse

`results` adalah object dengan key dinamis ISO-3166-1, bukan array.

```json
{
  "id": 550,
  "results": {
    "ID": {
      "link": "https://www.themoviedb.org/movie/550/watch?locale=ID",
      "flatrate": [
        {
          "logo_path": "/provider.png",
          "provider_id": 8,
          "provider_name": "Provider Name",
          "display_priority": 0
        }
      ],
      "free": [],
      "ads": [],
      "rent": [],
      "buy": []
    }
  }
}
```

Kotlin:

```kotlin
@Serializable
data class WatchProvidersResponseDto(
  val id: Int,
  val results: Map<String, WatchProviderRegionDto> = emptyMap(),
)

@Serializable
data class WatchProviderRegionDto(
  val link: String? = null,
  val flatrate: List<WatchProviderDto>? = null,
  val free: List<WatchProviderDto>? = null,
  val ads: List<WatchProviderDto>? = null,
  val rent: List<WatchProviderDto>? = null,
  val buy: List<WatchProviderDto>? = null,
)
```

Jangan membuat property statis `ID`, `US`, `JP`, dan seterusnya.

## 7.21 ReleaseDatesResponse

```json
{
  "id": 550,
  "results": [
    {
      "iso_3166_1": "ID",
      "release_dates": [
        {
          "certification": "17+",
          "descriptors": [],
          "iso_639_1": "id",
          "note": "",
          "release_date": "2026-08-01T00:00:00.000Z",
          "type": 3
        }
      ]
    }
  ]
}
```

Release type:

| Nilai | Jenis |
|---:|---|
| 1 | Premiere |
| 2 | Theatrical limited |
| 3 | Theatrical |
| 4 | Digital |
| 5 | Physical |
| 6 | TV |

Untuk jadwal bioskop Indonesia, cari entry `iso_3166_1 == "ID"` dan prioritaskan type 3, kemudian type 2.

## 7.22 ContentRatingsResponse

```json
{
  "id": 1399,
  "results": [
    {
      "descriptors": [],
      "iso_3166_1": "US",
      "rating": "TV-MA"
    }
  ]
}
```

## 7.23 CertificationsResponse

```json
{
  "certifications": {
    "US": [
      {
        "certification": "PG-13",
        "meaning": "Some material may be inappropriate...",
        "order": 3
      }
    ],
    "ID": []
  }
}
```

Gunakan `Map<String, List<Certification>>`.

## 7.24 ChangesResponse

```json
{
  "changes": [
    {
      "key": "overview",
      "items": [
        {
          "id": "change-id",
          "action": "updated",
          "time": "2026-07-27 00:00:00 UTC",
          "iso_639_1": "id",
          "iso_3166_1": "ID",
          "value": "New value",
          "original_value": "Old value"
        }
      ]
    }
  ]
}
```

`value` dan `original_value` bersifat polymorphic: string, number, boolean, object, array, atau null. Gunakan `JsonElement?`.

## 7.25 AccountDetails

```json
{
  "avatar": {
    "gravatar": {
      "hash": "gravatar-hash"
    },
    "tmdb": {
      "avatar_path": "/avatar.png"
    }
  },
  "id": 548,
  "iso_639_1": "en",
  "iso_3166_1": "ID",
  "name": "Display Name",
  "include_adult": false,
  "username": "username"
}
```

## 7.26 AccountStates

```json
{
  "id": 550,
  "favorite": true,
  "rated": {
    "value": 8.5
  },
  "watchlist": false
}
```

`rated` dapat berupa:

```json
false
```

atau object:

```json
{ "value": 8.5 }
```

Karena tipe berubah, gunakan custom serializer, `JsonElement`, atau model adapter.

## 7.27 Rated result

Rated Movie/TV menambahkan:

```json
{
  "rating": 8.5
}
```

Rated Episode:

```json
{
  "air_date": "2026-01-01",
  "episode_number": 1,
  "episode_type": "standard",
  "id": 123,
  "name": "Episode",
  "overview": "Overview",
  "production_code": "101",
  "runtime": 45,
  "season_number": 1,
  "show_id": 999,
  "still_path": "/still.jpg",
  "vote_average": 8.0,
  "vote_count": 100,
  "rating": 9.0
}
```

## 7.28 CollectionDetails

```json
{
  "id": 10,
  "name": "Collection Name",
  "overview": "Collection overview.",
  "poster_path": "/poster.jpg",
  "backdrop_path": "/backdrop.jpg",
  "parts": [
    {
      "adult": false,
      "backdrop_path": "/backdrop.jpg",
      "genre_ids": [12],
      "id": 11,
      "original_language": "en",
      "original_title": "Original Title",
      "overview": "Overview",
      "popularity": 20.0,
      "poster_path": "/poster.jpg",
      "release_date": "1977-05-25",
      "title": "Title",
      "video": false,
      "vote_average": 8.2,
      "vote_count": 22000
    }
  ]
}
```

## 7.29 CompanyDetails dan NetworkDetails

```json
{
  "description": "Company description.",
  "headquarters": "City, Country",
  "homepage": "https://company.example",
  "id": 1,
  "logo_path": "/logo.png",
  "name": "Company Name",
  "origin_country": "US",
  "parent_company": null
}
```

Network biasanya subset:

```json
{
  "headquarters": "City, Country",
  "homepage": "https://network.example",
  "id": 49,
  "logo_path": "/logo.png",
  "name": "Network Name",
  "origin_country": "US"
}
```

## 7.30 ConfigurationDetails

```json
{
  "images": {
    "base_url": "http://image.tmdb.org/t/p/",
    "secure_base_url": "https://image.tmdb.org/t/p/",
    "backdrop_sizes": ["w300", "w780", "w1280", "original"],
    "logo_sizes": ["w45", "w92", "w154", "w185", "w300", "w500", "original"],
    "poster_sizes": ["w92", "w154", "w185", "w342", "w500", "w780", "original"],
    "profile_sizes": ["w45", "w185", "h632", "original"],
    "still_sizes": ["w92", "w185", "w300", "original"]
  },
  "change_keys": ["adult", "air_date", "also_known_as"]
}
```

## 7.31 Configuration arrays

Country:

```json
{
  "iso_3166_1": "ID",
  "english_name": "Indonesia",
  "native_name": "Indonesia"
}
```

Language:

```json
{
  "iso_639_1": "id",
  "english_name": "Indonesian",
  "name": "Bahasa Indonesia"
}
```

Timezone group:

```json
{
  "iso_3166_1": "ID",
  "zones": ["Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura"]
}
```

Department jobs:

```json
{
  "department": "Directing",
  "jobs": ["Director", "Assistant Director"]
}
```

## 7.32 List models

List summary:

```json
{
  "description": "List description",
  "favorite_count": 10,
  "id": 123,
  "item_count": 20,
  "iso_639_1": "en",
  "list_type": "movie",
  "name": "List Name",
  "poster_path": "/poster.jpg"
}
```

Create list request:

```json
{
  "name": "My List",
  "description": "List description",
  "language": "id"
}
```

Create response:

```json
{
  "status_code": 1,
  "status_message": "Success.",
  "success": true,
  "list_id": 123
}
```

Add/remove item body:

```json
{
  "media_id": 550
}
```

Item status:

```json
{
  "id": "123",
  "item_present": true
}
```

---

# 8. Kotlin DTO strategy

## 8.1 Gunakan nullable secara realistis

Contoh:

```kotlin
@Serializable
data class MovieDetailsDto(
  val id: Int,
  val adult: Boolean = false,
  @SerialName("backdrop_path") val backdropPath: String? = null,
  @SerialName("belongs_to_collection")
  val belongsToCollection: CollectionSummaryDto? = null,
  val budget: Long = 0,
  val genres: List<GenreDto> = emptyList(),
  val homepage: String? = null,
  @SerialName("imdb_id") val imdbId: String? = null,
  @SerialName("origin_country") val originCountry: List<String> = emptyList(),
  @SerialName("original_language") val originalLanguage: String = "",
  @SerialName("original_title") val originalTitle: String = "",
  val overview: String? = null,
  val popularity: Double = 0.0,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("production_companies")
  val productionCompanies: List<ProductionCompanyDto> = emptyList(),
  @SerialName("production_countries")
  val productionCountries: List<ProductionCountryDto> = emptyList(),
  @SerialName("release_date") val releaseDate: String? = null,
  val revenue: Long = 0,
  val runtime: Int? = null,
  @SerialName("spoken_languages")
  val spokenLanguages: List<SpokenLanguageDto> = emptyList(),
  val status: String? = null,
  val tagline: String? = null,
  val title: String = "",
  val video: Boolean = false,
  @SerialName("vote_average") val voteAverage: Double = 0.0,
  @SerialName("vote_count") val voteCount: Int = 0,
)
```

Gunakan `Long` untuk `budget` dan `revenue`.

## 8.2 Generic page

```kotlin
@Serializable
data class PageDto<T>(
  val page: Int = 1,
  val results: List<T> = emptyList(),
  @SerialName("total_pages") val totalPages: Int = 0,
  @SerialName("total_results") val totalResults: Int = 0,
)
```

Pastikan converter Retrofit mendukung generic serializer yang digunakan.

## 8.3 Multi search

Pilihan aman:

```kotlin
@Serializable
data class MultiSearchRawDto(
  @SerialName("media_type") val mediaType: String,
  val id: Int,
  val title: String? = null,
  val name: String? = null,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("profile_path") val profilePath: String? = null,
  val knownFor: JsonElement? = null,
)
```

Pilihan lebih bersih: custom serializer menjadi sealed class:

```kotlin
sealed interface MultiSearchItemDto {
  val id: Int

  data class Movie(/* ... */) : MultiSearchItemDto
  data class Tv(/* ... */) : MultiSearchItemDto
  data class Person(/* ... */) : MultiSearchItemDto
}
```

Jangan memilih class hanya berdasarkan adanya `title` atau `name`; gunakan `media_type`.

## 8.4 Account rated polymorphism

Karena `rated` dapat `false` atau object, simpan sebagai `JsonElement` pada network DTO:

```kotlin
@Serializable
data class AccountStatesDto(
  val id: Int,
  val favorite: Boolean = false,
  val rated: JsonElement? = null,
  val watchlist: Boolean = false,
)

fun AccountStatesDto.ratingValue(): Double? {
  val obj = rated as? JsonObject ?: return null
  return obj["value"]?.jsonPrimitive?.doubleOrNull
}
```

## 8.5 Satu file service dengan grouping

Sesuai gaya FILMERA, endpoint dapat tetap berada dalam satu file selama diberi section jelas:

```kotlin
interface TmdbApiService {
  // region Authentication
  // endregion

  // region Account
  // endregion

  // region Movies
  // endregion

  // region TV Series
  // endregion

  // region TV Seasons
  // endregion

  // region TV Episodes
  // endregion

  // region People
  // endregion

  // region Search & Discover
  // endregion

  // region Configuration
  // endregion
}
```

Jangan memecah file hanya berdasarkan setiap endpoint. Pisahkan DTO/domain mapper bila file service mulai sulit dinavigasi.

## 8.6 Retrofit examples

```kotlin
interface TmdbApiService {
  @GET("movie/{movie_id}")
  suspend fun getMovieDetails(
    @Path("movie_id") movieId: Int,
    @Query("language") language: String = "id-ID",
    @Query("append_to_response") appendToResponse: String? = null,
    @Query("include_image_language") imageLanguages: String? = "id,en,null",
    @Query("include_video_language") videoLanguages: String? = "id,en",
    @Query("session_id") sessionId: String? = null,
  ): MovieDetailsDto

  @GET("tv/{series_id}")
  suspend fun getTvSeriesDetails(
    @Path("series_id") seriesId: Int,
    @Query("language") language: String = "id-ID",
    @Query("append_to_response") appendToResponse: String? = null,
    @Query("include_image_language") imageLanguages: String? = "id,en,null",
    @Query("include_video_language") videoLanguages: String? = "id,en",
    @Query("session_id") sessionId: String? = null,
  ): TvSeriesDetailsDto

  @GET("tv/{series_id}/season/{season_number}")
  suspend fun getSeasonDetails(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Query("language") language: String = "id-ID",
    @Query("append_to_response") appendToResponse: String? = null,
  ): TvSeasonDetailsDto

  @GET("tv/{series_id}/season/{season_number}/episode/{episode_number}")
  suspend fun getEpisodeDetails(
    @Path("series_id") seriesId: Int,
    @Path("season_number") seasonNumber: Int,
    @Path("episode_number") episodeNumber: Int,
    @Query("language") language: String = "id-ID",
    @Query("append_to_response") appendToResponse: String? = null,
  ): TvEpisodeDetailsDto

  @GET("search/multi")
  suspend fun searchMulti(
    @Query("query") query: String,
    @Query("language") language: String = "id-ID",
    @Query("page") page: Int = 1,
    @Query("include_adult") includeAdult: Boolean = false,
  ): PageDto<MultiSearchRawDto>

  @POST("account/{account_id}/favorite")
  suspend fun setFavorite(
    @Path("account_id") accountId: Int,
    @Query("session_id") sessionId: String,
    @Body request: FavoriteRequestDto,
  ): ActionResponseDto
}
```

Authorization interceptor:

```kotlin
class TmdbAuthInterceptor(
  private val tokenProvider: () -> String,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val token = tokenProvider().trim()
    require(token.isNotBlank()) { "TMDB bearer token is missing" }

    val request = chain.request()
      .newBuilder()
      .header("Authorization", "Bearer $token")
      .header("Accept", "application/json")
      .build()

    return chain.proceed(request)
  }
}
```

---

# 9. Feature-to-endpoint mapping untuk FILMERA

## Home

```text
Hero carousel       -> trending/movie/day atau trending/all/day
Now Playing         -> movie/now_playing?region=ID
Popular Movies      -> movie/popular
Upcoming            -> movie/upcoming?region=ID
Top Rated           -> movie/top_rated
Airing Today        -> tv/airing_today?timezone=Asia/Jakarta
On The Air          -> tv/on_the_air?timezone=Asia/Jakarta
Popular TV          -> tv/popular
Country sections    -> discover/movie atau discover/tv + with_origin_country / region
Genre sections      -> discover + with_genres
```

## Search

```text
Initial discovery   -> trending/all/day + genres + popular people
Live query          -> search/multi
Movie-only filter   -> search/movie
TV-only filter      -> search/tv
Person-only filter  -> search/person
Keyword suggestions -> search/keyword
```

Gunakan debounce sekitar 300-500 ms pada query search dan batalkan request lama ketika input berubah.

## Movie detail

```text
Core information    -> movie/{id}
Cast/crew           -> credits
Trailer             -> videos
Gallery             -> images
Themes              -> keywords
More like this      -> recommendations + similar
Cinema dates        -> release_dates
Streaming           -> watch/providers
Reviews             -> reviews
User state          -> account_states
```

## TV detail

```text
Core information    -> tv/{id}
All cast/crew        -> aggregate_credits
Latest credit       -> credits
Season list         -> included in TV details
Trailer/gallery     -> videos + images
Age rating          -> content_ratings
Streaming           -> watch/providers
Episode grouping    -> episode_groups
Recommendations     -> recommendations + similar
```

## Person detail

```text
Biography            -> person/{id}
All filmography       -> combined_credits
Movie filmography     -> movie_credits
TV filmography        -> tv_credits
Photos                -> images
Social/IMDb links     -> external_ids
Tagged media images   -> tagged_images
```

## User library

```text
Favorites Movie     -> account/{id}/favorite/movies
Favorites TV        -> account/{id}/favorite/tv
Watchlist Movie     -> account/{id}/watchlist/movies
Watchlist TV        -> account/{id}/watchlist/tv
Rated Movie         -> account/{id}/rated/movies
Rated TV            -> account/{id}/rated/tv
Rated Episode       -> account/{id}/rated/tv/episodes
```

---

# 10. Caching dan error handling

## Cache suggestions

| Data | Cache praktis |
|---|---|
| Configuration, genre, countries, languages | lama; refresh periodik |
| Movie/TV details | beberapa jam atau stale-while-revalidate |
| Popular/trending | lebih singkat |
| Images/videos | beberapa jam |
| Account states/favorites/watchlist | jangan terlalu lama; update optimistic lalu revalidate |
| Watch providers | beberapa jam/hari, tetap sediakan refresh |

Cache policy harus mematuhi terms TMDB yang berlaku. Jangan menganggap cache dapat disimpan permanen tanpa pengecekan terms.

## HTTP handling

```text
200 -> success
201 -> resource created pada beberapa mutation
401 -> invalid token/session atau tidak terautentikasi
404 -> resource tidak ditemukan
422 -> parameter/body tidak valid
429 -> rate limited; retry dengan exponential backoff
5xx -> server error; tampilkan retry state
```

Jangan melakukan retry otomatis tanpa batas pada 401, 404, atau 422.

Untuk 429/5xx:

```text
attempt 1: 500 ms
attempt 2: 1 s
attempt 3: 2 s
maksimal retry terbatas + jitter
```

---

# 11. Common mistakes

1. Menambahkan `/` dua kali pada base URL dan path.
2. Menganggap `/poster.jpg` adalah URL lengkap.
3. Memakai `title` untuk TV; TV memakai `name`.
4. Memakai `release_date` untuk TV; TV memakai `first_air_date`.
5. Memakai Movie DTO untuk `search/multi`.
6. Membuat `poster_path: String` non-null.
7. Menganggap `rated` selalu object.
8. Menganggap semua watch provider berada pada array; sebenarnya map berdasarkan kode negara.
9. Menggunakan `credits` TV untuk seluruh season; gunakan `aggregate_credits`.
10. Menganggap `now_playing` menyimpan histori kapan film diturunkan dari bioskop. TMDB memberi snapshot/list dan release metadata, bukan histori jadwal setiap bioskop lokal.
11. Menganggap watch providers memberikan URL stream langsung. Provider response memberi metadata dan TMDB watch link.
12. Tidak menampilkan atribusi JustWatch.
13. Menaruh `session_id` atau bearer token ke `BuildConfig` yang ikut terdistribusi tanpa memahami risikonya. Mobile client tetap dapat dibongkar; untuk operasi sensitif pertimbangkan backend proxy.
14. Memakai `person/latest` sebagai “artist trending terbaru”. Gunakan `person/popular` atau trending.
15. Mengabaikan `region`, `watch_region`, dan timezone sehingga hasil Indonesia tidak tepat.

---

# 12. Completion checklist untuk agent

Sebelum menyelesaikan implementasi TMDB:

- [ ] Base URL berakhir dengan `/3/` atau Retrofit base URL yang benar.
- [ ] Bearer token dikirim melalui header.
- [ ] Token tidak tercatat di repository/log.
- [ ] `language=id-ID` dan fallback diterapkan.
- [ ] Image URL dibangun dari configuration atau secure image base URL.
- [ ] Semua image path nullable.
- [ ] Pagination didukung.
- [ ] Multi search membaca `media_type`.
- [ ] TV detail memakai `aggregate_credits` bila butuh seluruh cast/crew.
- [ ] Season dan Episode memiliki model sendiri.
- [ ] Watch provider memakai `Map<String, Region>`.
- [ ] JustWatch attribution tampil bila provider digunakan.
- [ ] Account mutation memakai `session_id`.
- [ ] Favorite/watchlist body memakai `media_type` yang benar.
- [ ] Rating mendukung guest atau user session sesuai kebutuhan.
- [ ] 401, 404, 422, 429, dan 5xx ditangani.
- [ ] Empty state, loading, retry, dan null image placeholder tersedia.
- [ ] `append_to_response` tidak melebihi 20 item.
- [ ] JSON field baru tidak membuat parsing gagal (`ignoreUnknownKeys = true`).

Kotlinx JSON recommendation:

```kotlin
val json = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
  coerceInputValues = true
  isLenient = false
}
```

---

# 13. Source of truth

Periksa ulang endpoint atau field yang berubah melalui:

```text
https://developer.themoviedb.org/reference
https://developer.themoviedb.org/openapi/tmdb-api.json
https://developer.themoviedb.org/docs/append-to-response
https://developer.themoviedb.org/docs/image-basics
https://developer.themoviedb.org/docs/image-languages
https://developer.themoviedb.org/docs/authentication-application
https://developer.themoviedb.org/reference/authentication-how-do-i-generate-a-session-id
```

Skill ini mendokumentasikan kontrak praktis, bukan pengganti OpenAPI machine-readable resmi. Bila dokumentasi halaman dan OpenAPI berbeda, verifikasi dengan response API aktual tanpa mengekspos credential.
