# Notification Process

This document explains how notifications work in the MainFrankenIT application, especially the WhatsApp notification flow used when new events are imported.

## Overview

The notification system connects four main parts of the application:

1. Users register or are created through the chat flow.
2. Users opt in to WhatsApp notifications by giving explicit consent and a phone number.
3. Events are imported from crawled sources or from `manual-events.csv`.
4. The backend creates notification records and, when the user has opted in, sends the message through WhatsApp via Twilio.

The backend always stores notifications in the database. WhatsApp is an additional delivery channel on top of that.

## Technologies Used

- **Quarkus**: Backend application framework.
- **Hibernate ORM / Panache**: Database entity persistence.
- **PostgreSQL**: Stores users, events, notifications, and delivery records.
- **Quarkus Scheduler**: Runs periodic event import and manual CSV reload jobs.
- **Java HTTP Client**: Sends requests to the Twilio REST API.
- **Twilio WhatsApp API**: Sends real WhatsApp messages when enabled.
- **React / Vite**: Frontend application.
- **Nginx**: Serves the frontend and proxies `/api/` requests to the backend in Docker.
- **Docker Compose**: Runs the database, backend, and frontend together.
- **Cloudflare Tunnel or public domain**: Provides a public HTTPS URL for clickable WhatsApp links.

## Important Files

- `src/main/java/de/mainfrankenit/notifications/application/NotificationService.java`
  - Creates notifications.
  - Chooses the delivery channel.
  - Builds event links.
  - Sends WhatsApp messages through `WhatsAppPort`.

- `src/main/java/de/mainfrankenit/notifications/adapter/out/whatsapp/MockWhatsAppAdapter.java`
  - Implements the WhatsApp sending port.
  - Can run in `mock` mode or `twilio` mode.

- `src/main/java/de/mainfrankenit/events/application/EventImportService.java`
  - Creates, updates, or cancels events.
  - Calls `NotificationService` when important event changes happen.

- `src/main/java/de/mainfrankenit/events/application/ManualEventFileLoader.java`
  - Reads `manual-events.csv`.
  - Imports manual event rows.
  - Can trigger notifications when `EVENT_MANUAL_NOTIFY=true`.

- `src/main/java/de/mainfrankenit/assistant/application/ChatService.java`
  - Handles the chatbot flow.
  - Asks the user whether they want WhatsApp notifications.
  - Stores the phone number and sends an initial recommendation message.

- `src/main/java/de/mainfrankenit/identity/application/UserService.java`
  - Handles WhatsApp opt-in.
  - Stores the phone number and consent flag on the user.

- `src/main/resources/db/migration/V1__initial_schema.sql`
  - Creates `notifications` and `notification_deliveries`.

## User Opt-In Flow

Users only receive WhatsApp messages after opt-in.

The opt-in stores:

- `phoneNumber`
- `whatsappOptIn=true`
- user preferences such as city, event types, attendance mode, and interests

The phone number must use E.164 format:

```text
+<country-code><number>
```

Example:

```text
+491234567890
```

In the chat flow, this happens when:

1. The chatbot asks whether the user wants WhatsApp notifications.
2. The user answers yes.
3. The chatbot asks for a phone number.
4. The backend validates the phone number.
5. `UserService.optIn(...)` stores the consent and phone number.
6. `NotificationService.create(...)` sends the initial recommendation message.

There is also a REST endpoint for opt-in:

```http
POST /api/users/{id}/whatsapp-opt-in
```

The request requires explicit consent.

## Event Import Flow

Events can enter the system from two paths:

1. **Crawler/import sources**
   - Sources are configured in `event-sources.csv`.
   - The backend fetches source pages.
   - Parsers extract event data.
   - `EventImportService.upsert(...)` inserts or updates events.

2. **Manual CSV**
   - Events are listed in `manual-events.csv`.
   - `ManualEventFileLoader` reads the file on startup.
   - It also reloads the file periodically when it changes.
   - Each active row is converted into an `EventDraft`.
   - The draft is passed to `EventImportService.upsert(...)`.

For manual events, notifications are controlled by:

```env
EVENT_MANUAL_NOTIFY=true
```

If this is `false`, manual events are imported but do not send WhatsApp notifications.

## When Notifications Are Created

Notifications are created by `EventImportService` in these cases:

### New Event

When an event does not exist yet, the backend creates it and calls:

```java
notifications.eventCreated(event);
```

This creates a `NEW_EVENT` notification for every enabled user.

### Updated Event

When an existing event changes, the backend checks whether the changed fields are meaningful.

Meaningful fields include:

- `title`
- `eventType`
- `startAt`
- `endAt`
- `locationName`
- `city`
- `address`
- `attendanceMode`
- `status`

If one of those fields changed, the backend creates an `EVENT_UPDATED` notification.

### Cancelled Event

When an imported event disappears from its source and is still in the future, the backend marks it as cancelled and creates an `EVENT_CANCELLED` notification.

## Notification Storage

Every notification is stored in the `notifications` table.

Important fields:

- `user_id`
- `event_id`
- `type`
- `title`
- `body`
- `dedupe_key`

The `dedupe_key` prevents duplicate notifications for the same event and user.

Examples:

```text
NEW_EVENT:<user-id>:<event-id>
EVENT_UPDATED:<user-id>:<event-id>:<changed-fields>
EVENT_CANCELLED:<user-id>:<event-id>
```

Every delivery attempt is stored in `notification_deliveries`.

Important fields:

- `notification_id`
- `channel`
- `status`
- `provider_reference`
- `error_message`
- `delivered_at`

Possible delivery channels:

- `WEB`
- `WHATSAPP`

Possible delivery statuses:

- `PENDING`
- `SENT`
- `FAILED`

## Delivery Channel Decision

When `NotificationService.create(...)` creates a notification, it checks the user:

```java
d.channel = user.whatsappOptIn ? DeliveryChannel.WHATSAPP : DeliveryChannel.WEB;
```

If the user has not opted in, the notification remains a web notification.

If the user has opted in, the backend immediately attempts WhatsApp delivery.

## WhatsApp Sending

WhatsApp sending goes through the `WhatsAppPort` interface.

Current implementation:

```text
NotificationService
  -> WhatsAppPort
    -> MockWhatsAppAdapter
      -> mock mode or Twilio mode
```

The adapter has two modes.

### Mock Mode

Configured with:

```env
WHATSAPP_PROVIDER=mock
```

In mock mode, no real WhatsApp message is sent. The adapter only validates the phone number and returns a fake provider reference.

This is useful for local tests and development.

### Twilio Mode

Configured with:

```env
WHATSAPP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_WHATSAPP_FROM=whatsapp:+...
```

In Twilio mode, the backend sends an HTTP POST request to:

```text
https://api.twilio.com/2010-04-01/Accounts/<account-sid>/Messages.json
```

The request body contains:

- `From`
- `To`
- `Body`

The recipient is formatted as:

```text
whatsapp:<user-phone-number>
```

For example:

```text
whatsapp:+491234567890
```

If Twilio returns a 2xx response, the delivery is marked as `SENT`.

If Twilio returns an error, the delivery is marked as `FAILED` and the error is stored in `notification_deliveries.error_message`.

## Link Generation

The notification message includes a link to the event page inside our app.

The link is built in `NotificationService`:

```java
publicUrl + "/events/" + event.id
```

The base URL comes from:

```env
APP_PUBLIC_URL=...
```

For local development this might be:

```env
APP_PUBLIC_URL=http://localhost:3000
```

For real WhatsApp messages, this should be a public HTTPS URL:

```env
APP_PUBLIC_URL=https://app.example.com
```

WhatsApp often does not make `localhost`, private IP addresses, or non-public HTTP links clickable. A stable HTTPS domain is the recommended solution.

## Frontend and Original Event Links

The WhatsApp message links to our app first:

```text
https://app.example.com/events/<event-id>
```

The event page in our app can then show the event details and provide a second button/link to the original event source URL.

This is intentional:

1. The user lands in our app.
2. We can show normalized event data.
3. The user can then click through to the original event page.

## Public URL Setup

For testing, a Cloudflare quick tunnel can expose the local Docker frontend:

```text
https://random-name.trycloudflare.com
```

This works for demos, but it is temporary and can change after restart.

For a stable setup, use one of these:

- Named Cloudflare Tunnel with a custom domain.
- VPS/server with Nginx and HTTPS.
- Managed hosting such as Render, Railway, or Fly.io.

Recommended stable production-style value:

```env
APP_PUBLIC_URL=https://app.mainfrankenit.de
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,https://app.mainfrankenit.de
VITE_API_URL=
```

With the current Docker/Nginx setup, the frontend can proxy API requests through the same public origin:

```text
/api/... -> backend:8080/api/...
```

That avoids mixed-origin problems when the app is opened from a public domain.

## Important Environment Variables

```env
APP_PUBLIC_URL=https://app.example.com
WHATSAPP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_WHATSAPP_FROM=whatsapp:+...
EVENT_MANUAL_ENABLED=true
EVENT_MANUAL_FILE=manual-events.csv
EVENT_MANUAL_NOTIFY=true
EVENT_SOURCES_RELOAD_INTERVAL=15s
VITE_API_URL=
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,https://app.example.com
```

Notes:

- `APP_PUBLIC_URL` controls the links inside WhatsApp messages.
- `WHATSAPP_PROVIDER=twilio` enables real WhatsApp sending.
- `WHATSAPP_PROVIDER=mock` disables real sending.
- `EVENT_MANUAL_NOTIFY=true` allows manual CSV events to trigger notifications.
- `VITE_API_URL=` lets the frontend use same-origin `/api` requests through Nginx.

## Testing the Flow

### 1. Start the stack

```powershell
docker compose up -d --build
```

### 2. Confirm backend configuration

```powershell
docker compose exec backend printenv APP_PUBLIC_URL WHATSAPP_PROVIDER EVENT_MANUAL_NOTIFY
```

### 3. Add a fresh manual event

Add a new unique row to `manual-events.csv`.

Important:

- Use a unique `externalEventId`.
- Use a unique `sourceUrl`.
- Set `active` to `true`.
- Use event data that matches an opted-in user's preferences.

Example:

```csv
manual-2026-whatsapp-test;WhatsApp Test Java Meetup;2026-07-05T18:00:00+02:00;2026-07-05T19:00:00+02:00;Wuerzburg;Online;Online;MEETUP;ONLINE;MainFrankenIT;Manual Events;https://mainfrankenit.example/events/whatsapp-test-2026;java,ai,meetup;Manual test event for WhatsApp notifications.;true
```

### 4. Wait for reload

The manual loader checks the file periodically according to:

```env
EVENT_SOURCES_RELOAD_INTERVAL=15s
```

### 5. Check backend logs

```powershell
docker compose logs --tail=100 backend
```

Look for:

```text
Loaded manual events from manual-events.csv: imported=... created=1 ...
```

### 6. Check latest delivery records

```powershell
docker compose exec db psql -U mainfrankenit -d mainfrankenit -c "select n.title, n.body, au.phone_number, nd.channel, nd.status, nd.provider_reference, nd.error_message, nd.created_at from notification_deliveries nd join notifications n on n.id=nd.notification_id join app_users au on au.id=n.user_id where nd.channel='WHATSAPP' order by nd.created_at desc limit 5;"
```

Expected result:

- `channel` is `WHATSAPP`
- `status` is `SENT`
- `error_message` is empty
- `body` contains the public `APP_PUBLIC_URL`

## Common Problems

### Message is not sent

Check:

- Is `WHATSAPP_PROVIDER=twilio`?
- Are Twilio credentials present?
- Is `TWILIO_WHATSAPP_FROM` configured with the `whatsapp:` prefix?
- Is the user opted in?
- Is the phone number E.164 formatted?
- Does Twilio allow sending to that number?

### Message is stored but not sent by WhatsApp

Check `notification_deliveries`:

- `status`
- `error_message`
- `provider_reference`

If the status is `FAILED`, the error message usually explains whether it is a Twilio config problem, a phone number problem, or an API response problem.

### Link says localhost

`APP_PUBLIC_URL` is still set to a local value.

Change it to a public URL:

```env
APP_PUBLIC_URL=https://app.example.com
```

Then restart the backend:

```powershell
docker compose up -d --force-recreate backend
```

### Link is not clickable in WhatsApp

This usually happens when the URL is:

- `localhost`
- a private LAN IP address
- plain HTTP
- a temporary or unusual URL WhatsApp does not recognize well

Use a stable HTTPS domain.

### Manual CSV event does not notify

Check:

- `EVENT_MANUAL_NOTIFY=true`
- the row is active
- the row has a unique `sourceUrl`
- the event is actually new or meaningfully updated
- there is at least one enabled user with `whatsappOptIn=true`

## Current Limitations

- The Twilio adapter currently stores an internal generated `twilio-...` reference instead of the actual Twilio message SID.
- Notifications are sent to all enabled users, not only users whose preferences match the event.
- There is no retry queue for failed WhatsApp deliveries.
- The quick Cloudflare tunnel URL is temporary unless replaced with a named tunnel or stable deployment.

## Recommended Improvements

1. Store the real Twilio message SID from the Twilio API response.
2. Send new-event notifications only to users whose preferences match the event.
3. Add retry handling for temporary Twilio failures.
4. Add an admin/debug page for notification delivery status.
5. Replace quick tunnel URLs with a stable production domain.
6. Add unsubscribe/opt-out handling for WhatsApp messages.

