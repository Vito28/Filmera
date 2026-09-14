package com.example.filmera.feature.auth.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {
  @Test
  fun `email validation rejects malformed values`() {
    assertEquals(AuthValidationError.REQUIRED, validateEmail(" "))
    assertEquals(AuthValidationError.INVALID_EMAIL, validateEmail("viewer@filmera"))
    assertNull(validateEmail("viewer@filmera.app"))
  }

  @Test
  fun `username validation matches backend constraint`() {
    assertEquals(AuthValidationError.USERNAME_FORMAT, validateUsername("Movie Fan"))
    assertEquals(AuthValidationError.USERNAME_FORMAT, validateUsername("ab"))
    assertNull(validateUsername("movie_fan26"))
  }

  @Test
  fun `password confirmation and strength are derived without storing secrets`() {
    assertEquals(
      AuthValidationError.PASSWORD_MISMATCH,
      validateConfirmPassword("Cinema!2026", "Cinema!2025"),
    )
    assertEquals(PasswordStrength.STRONG, passwordStrength("Cinema!2026"))
  }
}
