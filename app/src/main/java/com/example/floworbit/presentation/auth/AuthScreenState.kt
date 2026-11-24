package com.example.floworbit.presentation.auth

/**
 * Represents the result of a sign-in or sign-up attempt.
 * It holds either the user data on success or an error message on failure.
 */
data class AuthResult(
    val data: UserData? = null,
    val errorMessage: String? = null
)

/**
 * A simple data class to hold the basic information of a logged-in user.
 */
data class UserData(
    val userId: String,
    val username: String?,
    val email: String?
)

/**
 * Represents the entire state of the authentication UI, including
 * login and registration flows.
 */
data class AuthState(
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val loginError: String? = null,
    val isRegistrationSuccess: Boolean = false,
    val registrationError: String? = null
)
