# Development

## Build & Run Commands

```bash
./mvnw                            # Run in dev mode (default goal: spring-boot:run)
./mvnw clean package              # Production build (JAR in target/)
./mvnw test                       # Run all tests
./mvnw test -Dtest=ClassName      # Run a single test class
```

The app runs on port 8080 (configurable via `PORT` env var).

On first startup, the application creates `admin@retailstore.com` with a random, unusable
bootstrap password and sends a one-time password-reset link to that address. Configure
`MAIL_HOST` and `MAIL_PORT` to deliver mail to the administrator before the first startup.
The administrator must set a new password through the link before signing in. If mail
delivery fails, restore mail service and use **Forgot password** to request another link.
Later startups leave the account and its credentials unchanged.

## Docker

To build a Docker image, run:

```bash
docker build -t my-application:latest .
```

If you use commercial components, pass the license key as a build secret:

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```
