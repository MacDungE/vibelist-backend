# Implementation Plan

- [x] 1. Update TokenResponse DTO to match new response format
  - Modify TokenResponse class to only include tokenType and accessToken fields
  - Remove unnecessary fields (expiresIn, userId, username, role) that shouldn't be exposed to clients
  - Ensure tokenType is always set to "Bearer"
  - Add proper validation and documentation
  - _Requirements: 1.1, 1.3, 1.4_

- [x] 2. Enhance JwtAuthenticationFilter to prioritize Authorization header
  - Modify getTokenFromRequest method to check Authorization header first
  - Implement fallback to cookie-based tokens for backward compatibility
  - Add logging to track which authentication method is being used
  - Update token validation logic to handle both authentication methods
  - _Requirements: 3.1, 3.2, 3.3, 7.1, 7.2_

- [x] 3. Update AuthController login endpoints to return tokens in response body
  - Modify login endpoint to return access token in response body format
  - Ensure refresh token is still set as HTTP-only cookie
  - Remove access token cookie setting from login response
  - Update error handling to return appropriate authentication errors
  - _Requirements: 1.1, 1.2, 2.1, 2.4, 6.1_

- [x] 4. Update AuthController refresh token endpoint
  - Modify refresh endpoint to return new access token in response body
  - Ensure refresh token is read from HTTP-only cookie
  - Remove access token cookie setting from refresh response
  - Update error handling for invalid refresh tokens
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.2_

- [x] 5. Update AuthService to support new token response format
  - Modify createTokenResponse method to return simplified TokenResponse
  - Remove access token cookie setting from token creation methods
  - Ensure refresh token cookie setting remains intact
  - Update token refresh logic to work with new response format
  - _Requirements: 1.1, 2.1, 2.4, 5.2_

- [x] 6. Update CookieUtil to remove access token cookie operations
  - Remove setAccessTokenCookie method from public interface
  - Remove removeAccessTokenCookie method from public interface
  - Update removeAllAuthCookies to only clear refresh token cookies
  - Maintain all refresh token cookie operations
  - _Requirements: 1.2, 2.1, 2.2, 2.3_

- [x] 7. Update OAuth2 login success handler to follow new token pattern
  - Modify OAuth2LoginSuccessHandler to return access token in response body
  - Ensure refresh token is set as HTTP-only cookie for OAuth2 logins
  - Update redirect logic to handle new token format
  - Maintain consistency with regular login endpoints
  - _Requirements: 6.3, 6.4_

- [x] 8. Add comprehensive unit tests for TokenResponse changes
  - Write tests for new TokenResponse structure
  - Test JSON serialization format matches expected client format
  - Test builder pattern functionality with new fields
  - Verify tokenType is always "Bearer"
  - _Requirements: 1.1, 1.3, 1.4_

- [x] 9. Add unit tests for enhanced JwtAuthenticationFilter
  - Test Authorization header token extraction priority
  - Test cookie fallback mechanism for backward compatibility
  - Test authentication setting with both token methods
  - Test error handling for invalid tokens from both sources
  - _Requirements: 3.1, 3.2, 3.3, 4.4, 7.1, 7.2_

- [x] 10. Add integration tests for updated authentication flow
  - Test complete login flow with new response format
  - Test token refresh flow with cookie-based refresh tokens
  - Test protected endpoint access using Authorization header
  - Test backward compatibility with existing cookie-based clients
  - _Requirements: 5.1, 5.2, 6.1, 6.2, 7.1, 7.2, 7.3_

- [x] 11. Add security tests for token handling
  - Test that refresh tokens are not exposed in response body
  - Test that access tokens are not set as cookies
  - Test proper cookie security attributes (HttpOnly, Secure)
  - Test authentication priority system works correctly
  - _Requirements: 2.1, 2.2, 2.4, 3.2, 4.1_

- [x] 12. Update logout functionality to handle new authentication method
  - Modify logout endpoint to clear refresh token cookies only
  - Ensure logout works for both Authorization header and cookie-based clients
  - Update logout response to indicate successful token clearing
  - Test logout functionality with both authentication methods
  - _Requirements: 7.4_