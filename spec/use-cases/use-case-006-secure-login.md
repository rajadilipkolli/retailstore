# UC-006: Secure Login

**As an** application user, **I want to** sign in securely **so that** I can access inventory features according to my role.

**Status:** Implemented
**Date:** 2026-09-22

> A use case cannot be marked as **Implemented** unless all criteria in the `/implement-use-case` skill are fulfilled.

## Actors

- **Primary actors:** Inventory Manager, Warehouse Staff, and Purchasing Manager
- **Secondary actors:** Spring Security authentication provider and email delivery service for password recovery

## Preconditions

- The user may register, but an administrator must onboard or approve the account before authentication succeeds.
- The user does not need an existing authenticated session to begin the flow.

## Trigger

- An unauthenticated user opens a protected inventory route and is redirected to `/login`.
- An authenticated user opens the application and proceeds to `/home`.

## Main Flow

1. User opens a protected inventory route.
2. System redirects the unauthenticated user to `/login`.
3. System displays a responsive Vaadin `LoginForm` with email, password, Remember me, Login, and Forgot password controls.
4. User enters their email and password.
5. User optionally selects Remember me.
6. User selects Login.
7. Spring Security authenticates the credentials through the configured authentication provider.
8. System creates the authenticated session according to the Remember me selection.
9. System redirects the user to `/home`.

## Alternative Flows

### Invalid credentials

- **Branches from:** Main Flow step 7
- **Condition:** The email or password is invalid, or the account is not eligible for authentication.
- **Flow:** System displays the standard authentication error and keeps the user on `/login` without creating an authenticated session.
- **Outcome:** The user may correct the credentials and retry.

### Request password reset

- **Branches from:** Main Flow step 3
- **Condition:** The user cannot remember their password.
- **Flow:**
  1. User selects Forgot password.
  2. System navigates to `/forgot-password`.
  3. User enters their email.
  4. System checks the email and returns a neutral response that does not reveal whether an account exists.
  5. If the email belongs to an account, the system sends a single-use, time-limited reset link.
- **Outcome:** The user can follow the reset link when it is received.

### Complete password reset

- **Branches from:** Request password reset step 5
- **Condition:** The user opens a valid reset link.
- **Flow:**
  1. System navigates to `/reset-password`.
  2. User enters and confirms a new password.
  3. System validates the password policy and confirmation value.
  4. System changes the password and invalidates the reset link.
  5. System returns the user to `/login`.
- **Outcome:** The user can sign in with the new password.

### Invalid or expired reset link

- **Branches from:** Complete password reset step 1
- **Condition:** The reset link is invalid, expired, or has already been used.
- **Flow:** System displays an error and requires the user to request a new reset link.
- **Outcome:** The existing password remains unchanged.

### Invalid new password

- **Branches from:** Complete password reset step 3
- **Condition:** The new password fails the standard password policy or the confirmation does not match.
- **Flow:** System displays standard validation errors and keeps the user on the reset form.
- **Outcome:** The existing password remains unchanged and the user may retry.

## Postconditions

### Success

- The user has an authenticated session and is redirected to `/home`.
- Session persistence follows the Remember me selection.
- After a successful password reset, the new password is active and the reset link cannot be reused.

### Failure

- Failed login does not create an authenticated session.
- Failed password recovery or reset does not change the existing password.

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Email is the unique account identity and login value. |
| BR-02 | Only administrator-onboarded accounts may authenticate. |
| BR-03 | Remember me controls session persistence. |
| BR-04 | Password reset links are single-use and time-limited. |
| BR-05 | New passwords must satisfy the standard password policy, and confirmation must match. |
| BR-06 | Password recovery responses must not reveal whether an email belongs to an account. |

## Acceptance Criteria

- [ ] Authenticated users can access `/home` and unauthenticated users are redirected to `/login` from protected routes.
- [ ] The responsive Vaadin `LoginForm` accepts email, password, Remember me, Login, and Forgot password interactions.
- [ ] Valid credentials authenticate through Spring Security and redirect the user to `/home`.
- [ ] Invalid credentials show a standard authentication error and do not create a session.
- [ ] Password recovery uses a neutral response for both existing and unknown email addresses.
- [ ] Valid reset links allow a compliant password change and cannot be reused.
- [ ] Invalid or expired reset links show an error and require a new reset request.
- [ ] Invalid or mismatched new passwords show validation errors without changing the existing password.

## Tests

> Write UI tests that verify the acceptance criteria above. See `architecture.md` § Testing for conventions.

- [ ] `AuthenticationTest`
- [ ] Verify authenticated and unauthenticated routing, valid login, invalid credentials, and Remember me session behavior.
- [ ] Verify the responsive login form controls and Forgot password navigation.
- [ ] Verify neutral responses for known and unknown email addresses.
- [ ] Verify successful password reset, single-use reset links, invalid or expired links, and standard password validation errors.
- [ ] Verify each business rule BR-01 through BR-06.

## UI / Routes

Use the standard responsive Vaadin `LoginForm`. The login view must provide email, password, Remember me, Login, and Forgot password controls. Password recovery and reset views are public. The home view is authenticated for all three primary roles. User registration and administrator onboarding are prerequisites and are outside this use case; any administrator onboarding view is restricted to `ADMIN`.

| Route | Access | Notes |
|-------|--------|-------|
| `/login` | public | Vaadin `LoginForm`; entry point for unauthenticated users. |
| `/forgot-password` | public | Accepts an email and returns a neutral recovery response. |
| `/reset-password` | public | Accepts a valid reset token and a new password. |
| `/home` | authenticated | Destination after successful authentication for all three roles. |