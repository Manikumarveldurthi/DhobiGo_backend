# DhobiGo Backend (Spring Boot)

Java 17 + Spring Boot 3.3 + Spring Security (JWT) + Spring Data JPA.
Three roles: **CUSTOMER**, **DHOBI**, **ADMIN**.

No Lombok — every class uses plain constructors/getters/setters, so there's
nothing extra to install in Eclipse. Just import and run.

## Importing into Eclipse

1. Eclipse → File → Import → **Maven → Existing Maven Projects**
2. Browse to this `dhobigo-backend` folder → Finish
3. Eclipse will download all dependencies listed in `pom.xml` automatically
   (needs internet the first time — after that they're cached locally)
4. Right-click the project → Run As → **Spring Boot App**
   (or run `DhobigoBackendApplication.java` directly)

No database installation needed to get started — the `dev` profile (active
by default) uses an in-memory H2 database that resets each time you restart
the app, and seeds itself with an admin account, 2 sample dhobis, and the
full item catalog automatically.

The app starts on **http://localhost:8081**.

## Default logins (auto-created on first startup — dev AND prod)

The seeder runs on both profiles. Against MySQL (`prod`), it only inserts
this data the very first time the `dhobigo` schema is empty — every restart
after that, it checks first and does nothing, so it won't duplicate data or
overwrite anything you've since changed.

| Role   | Email                | Password      |
|--------|-----------------------|----------------|
| Admin  | admin@dhobigo.com     | Admin@12345    |
| Dhobi  | ramesh@dhobigo.com    | Dhobi@12345    |
| Dhobi  | sunita@dhobigo.com    | Dhobi@12345    |
| Dhobi (pending approval) | vikram@dhobigo.com | Dhobi@12345 |

Customers aren't seeded — register one via `POST /api/auth/register`.
Vikram is deliberately left unapproved so you can immediately test the
admin approval screen without registering a new dhobi yourself.

**Before a real production deployment** (not just local MySQL testing),
log in as `admin@dhobigo.com` and change the password, or remove the
`seedAdmin()` call in `DataSeeder.java` once you've created your own admin
account by hand — don't leave a publicly known default password active on
a real deployment.

## Exploring the API

- Swagger UI: http://localhost:8081/swagger-ui.html
- H2 console (dev only): http://localhost:8081/h2-console
  (JDBC URL: `jdbc:h2:mem:dhobigo`, user: `sa`, no password)

## Endpoint summary

**Public**
- `POST /api/auth/register` — body: `{ fullName, email, phone, password, role }` (role: CUSTOMER or DHOBI)
- `POST /api/auth/login` — body: `{ email, password }` → returns JWT
- `GET /api/catalog` or `GET /api/catalog?service=WASH` — item prices
- `GET /api/dhobis` or `GET /api/dhobis?lat=..&lng=..` — approved+available dhobis, with badges; sorted nearest-first and includes `distanceKm` when lat/lng given

**Customer** (send `Authorization: Bearer <token>`)
- `POST /api/payments/create-order` — body: `{ items }` → opens a Razorpay order for UPI/Card (amount computed server-side); returns `enabled:false` if Razorpay isn't configured, so the frontend can fall back to demo mode
- `POST /api/orders` — create an order (for UPI/Card, include the `razorpayOrderId`/`razorpayPaymentId`/`razorpaySignature` from Checkout — verified server-side before the order is created)
- `GET /api/orders/{id}` — view one order (also usable by the assigned dhobi or an admin)
- `GET /api/orders/my` — this customer's order history
- `PATCH /api/orders/{id}/reassign` — body: `{ dhobiId }` — pick a replacement dhobi after the assigned one declined (only works while the order is in that state)

**Dhobi**
- `GET /api/dhobi/me` — my own profile (check `approved` before showing the order queue)
- `PATCH /api/dhobi/location` — body: `{ latitude, longitude }`, pushed periodically while dashboard is open
- `PATCH /api/dhobi/availability?available=true` — go online/offline (blocked until approved)
- `GET /api/dhobi/orders` — orders assigned to me
- `PATCH /api/dhobi/orders/{id}/accept` — accept a newly-assigned order
- `PATCH /api/dhobi/orders/{id}/decline` — body (optional): `{ reason }` — decline; unassigns the order and notifies the customer to pick someone else
- `PATCH /api/dhobi/orders/{id}/stage` — advance to the next stage (body: `{ "stage": "COLLECTED" }`) — blocked until the order has been accepted

**Admin**
- `GET /api/admin/orders` — every order in the system
- `GET /api/admin/users/customers`
- `GET /api/admin/users/dhobis`
- `GET /api/admin/dhobis/pending` — new dhobi signups awaiting approval
- `PATCH /api/admin/dhobis/{userId}/approve` — approve a dhobi (goes live immediately, available=true)
- `PATCH /api/admin/dhobis/{userId}/reject` — reject (profile stays, can be reconsidered later)
- `PATCH /api/admin/dhobis/{userId}/availability?available=false` — force a live dhobi offline
- `POST /api/admin/catalog` — add a new item — body: `{ itemKey, name, icon, service, price }`
- `PUT /api/admin/catalog/{dbId}` — edit an item
- `DELETE /api/admin/catalog/{dbId}` — remove an item

## Dhobi accept/decline

Every order lands on the assigned dhobi's queue in `AcceptanceStatus.PENDING`
— dhobi.html shows **Accept**/**Decline** buttons instead of the usual
"Mark as: ..." button until they respond, and `OrderService.updateStage()`
rejects any stage update on an order that hasn't been accepted yet.

- **Accept** → `AcceptanceStatus.ACCEPTED`, order proceeds through the
  normal PLACED → ... → DELIVERED stage flow as before.
- **Decline** → the order is unassigned (`order.dhobi = null`,
  `AcceptanceStatus.DECLINED`), the declining dhobi is remembered
  (`order_declined_dhobis` table) so they can't be routed back to the same
  order, and the customer gets a WhatsApp notification plus a real-time
  push. **This does not auto-reassign** — tracking.html shows a "Your dhobi
  wasn't able to take this order" panel listing available dhobis (same
  picker as the original booking flow, minus anyone who already declined),
  and the customer confirms a replacement via `PATCH /api/orders/{id}/reassign`.
  That puts the order back in `PENDING` for the new dhobi to accept/decline,
  same as any freshly-placed order.

## Dhobi approval workflow

New dhobi signups start with `approved=false, available=false` — they
can log in and see their dashboard, but it shows a "pending approval"
message instead of an order queue, and they can't be selected or
auto-assigned orders. An admin reviews them via
`GET /api/admin/dhobis/pending` and approves via
`PATCH /api/admin/dhobis/{userId}/approve`, at which point `available`
flips to `true` automatically and they go live immediately — mirroring
how delivery-partner apps like Swiggy/Zomato onboard new partners.

## Nearby dhobis + badges

`GET /api/dhobis?lat=..&lng=..` sorts results nearest-first using a
Haversine distance calculation (`GeoUtil.java`) against each dhobi's last
known location (pushed by `dhobi.html` via the browser's geolocation API
while the dashboard is open). Each dhobi response also includes computed
`badges` (e.g. "Top Rated", "500+ Orders", "Online Now") — these aren't
stored, they're derived fresh from `rating`/`completedOrders`/`available`
on every request, so they're always current.

## Real-time (WebSocket) — replaces polling

Connect via SockJS + STOMP at `ws://localhost:8081/ws` (frontend already
does this in `js/realtime.js`). Send the JWT as an `Authorization: Bearer
<token>` STOMP header on CONNECT — same token as REST calls.

Topics (all authorized per-subscriber by `StompAuthInterceptor` — see that
class for why plain topic names aren't a security boundary on their own):
- `/topic/orders/{orderId}` — stage changes, pushed the instant a dhobi advances an order
- `/topic/orders/{orderId}/messages` — new chat messages for that order
- `/topic/dhobis/{dhobiId}/orders` — pushed when a new order is assigned to that dhobi
- `/topic/dhobis/{dhobiId}/location` — live location pushes
- `/topic/admin/orders` — every order create/stage-change, for a live admin table (admin role only)

## In-app chat

Order-scoped chat between the customer and their assigned dhobi (or an
admin, who can see any order's chat).
- `GET /api/orders/{orderId}/messages` — history
- `POST /api/orders/{orderId}/messages` — send (body: `{ "content": "..." }`) — also broadcasts over the WebSocket topic above

## Click-to-call

`OrderResponse` now includes `customerPhone` and `dhobiPhone` — the
frontend renders these as `tel:` links (no backend call needed to place a
call, it just opens the device's phone app).

## WhatsApp order-update notifications

See `WhatsAppNotificationService.java` for the full setup story. Short
version: it's **disabled by default** and just logs what it would send —
nothing breaks if you never touch this. To go live you need your own
Twilio account (wraps Meta's WhatsApp Business API) and to set
`WHATSAPP_ENABLED=true`, `WHATSAPP_ACCOUNT_SID`, `WHATSAPP_AUTH_TOKEN` as
environment variables. Twilio's WhatsApp Sandbox works for testing
immediately; a real production sender requires Meta's business
verification process, which takes them some days to approve — that part
isn't something I can do for you, it's tied to your own business/Meta
account.

## Real payments (UPI/Card) via Razorpay

See `RazorpayProperties.java`/`PaymentService.java` for the full setup
story. Short version: **disabled by default**. With no keys configured,
`payment.html`'s UPI/Card option falls back to demo mode — the order is
placed directly and marked `PENDING`, no real charge happens, nothing
breaks. Cash on delivery never touches this at all, in demo mode or not.

To go live:
1. Sign up at razorpay.com — free, and test mode works immediately with no
   KYC/business verification needed
2. Settings → API Keys → "Generate Test Key" → gives you a Key ID
   (`rzp_test_...`) and a Key Secret
3. Set `RAZORPAY_ENABLED=true`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` as
   environment variables and restart the backend
4. Test with Razorpay's published test card/UPI numbers (see their docs) —
   no real money moves in test mode
5. For actual production charges: complete Razorpay's KYC/activation flow
   (their process, not something pre-configurable here), then swap in your
   `rzp_live_...` key pair via the same env vars

How it works end-to-end: `payment.js` calls `POST /api/payments/create-order`
first — the backend recomputes the total from the cart items server-side
(never trusts an amount from the browser) and opens a Razorpay order.
`payment.js` then opens Razorpay's Checkout modal with that order id. On
success, Razorpay hands back a payment id + signature, which `payment.js`
forwards to the existing `POST /api/orders` call; `OrderService` verifies
that signature against your Key Secret before the DhobiGo order is ever
created, so a tampered/faked "success" in the browser can't create a
`PAID` order. If verification fails, order creation is rejected — nothing
is charged twice and no half-paid order is left behind.

## Scaling this for more traffic

See `SCALABILITY.md` — covers why this architecture supports horizontal
scaling (stateless JWT auth), a working nginx load-balancer config example
for running multiple backend instances, and what to tackle in what order
as load actually increases.

## Reviews

Customers can review an order once it's `DELIVERED` (one review per
order, 1-5 stars + optional comment). Submitting a review recomputes that
dhobi's `rating` as a true average across all their reviews — this feeds
directly into the "Top Rated" badge and nearby-dhobi sorting, so ratings
are always real once reviews start coming in, not just the seeded value.
- `POST /api/orders/{orderId}/review` (customer)
- `GET /api/orders/{orderId}/review` (check if already reviewed)
- `GET /api/dhobi/reviews` (dhobi's own)
- `GET /api/admin/reviews` (admin, all of them)

## Removing a dhobi from the site

`PATCH /api/admin/dhobis/{userId}/ban` — deactivates the dhobi's account
(blocks login entirely) and delists them from customer browsing/
auto-assignment. Doesn't hard-delete the row, since that would break
historical orders and reviews that reference them. `PATCH .../unban`
reverses the login block, but doesn't restore approval — they go back
through the normal approval queue, same as a new signup.

## Dhobi order history + reviews

`GET /api/dhobi/orders/history` (all orders, not just active ones) and
`GET /api/dhobi/reviews` power the "Order history" and "Reviews" tabs on
`dhobi.html`.

## Google Sign-In

Quick customer-only signup/login (mirrors Swiggy/Zomato's social login
pattern — dhobi signup stays through the full form since they need
approval review regardless). Setup (free, no business verification,
unlike WhatsApp):
1. console.cloud.google.com → new project → APIs & Services → Credentials
   → Create Credentials → OAuth client ID → Web application
2. Add your frontend's origin under "Authorized JavaScript origins"
3. Copy the Client ID → set `GOOGLE_CLIENT_ID` on the backend AND replace
   `GOOGLE_CLIENT_ID` in `js/login.js` / `js/signup.js` with the same value
- `POST /api/auth/google` — body: `{ idToken }` (obtained client-side via Google's Identity Services JS)

## Phone-number OTP login

Reuses your existing Twilio account from the WhatsApp setup — just needs
one more free thing:
1. Twilio Console → Verify → Services → Create new Service
2. Copy the Service SID (starts with `VA...`)
3. Set `TWILIO_VERIFY_ENABLED=true` and `TWILIO_VERIFY_SERVICE_SID`
- `POST /api/auth/phone/send-otp` — body: `{ phone }`
- `POST /api/auth/phone/verify-otp` — body: `{ phone, code, fullName }` (fullName only needed on first-time signup)

## Forgot / reset password

Disabled by default (logs the reset link instead of emailing it). Easiest
setup: a Gmail account with an App Password (needs 2-Step Verification
enabled first, then generate one at myaccount.google.com/apppasswords):
```
EMAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=youraddress@gmail.com
SPRING_MAIL_PASSWORD=<16-character app password>
EMAIL_FROM=youraddress@gmail.com
```
- `POST /api/auth/forgot-password` — body: `{ email }` (always returns success, even if the email doesn't exist — prevents email enumeration)
- `POST /api/auth/reset-password` — body: `{ token, newPassword }` (token expires after 30 minutes, single-use)

## Extended profile

`GET /api/users/me` / `PATCH /api/users/me` — works for any logged-in
role. Lets a user update their name, phone, address, and avatar URL after
signing up via Google/phone (which don't collect everything upfront).

## Using your MySQL database (prod profile)

1. Make sure MySQL is running and you know your username/password.
2. You don't need to manually create the `dhobigo` schema — the connection
   URL below includes `createDatabaseIfNotExist=true`, so it's created
   automatically on first connect (as long as your MySQL user has
   `CREATE` privilege).
3. Set these environment variables (or edit the defaults directly in
   `application.yml` under the `prod` profile if you'd rather not use env
   vars locally):
```
DB_URL=jdbc:mysql://localhost:3306/dhobigo?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=<your MySQL password>
JWT_SECRET=<a long random string>
```
4. Run with the `prod` profile active. In Eclipse: Run → Run Configurations
   → your Spring Boot app → Arguments tab → VM arguments →
   `-Dspring.profiles.active=prod` (or set `SPRING_PROFILES_ACTIVE=prod` as
   an environment variable in the same Run Configuration).

`ddl-auto` is `update` in prod (not `create-drop` like dev), so your tables
and data persist across restarts — Hibernate will create the tables on
first run, then only add new columns/tables on top after that. For a real
production rollout later, switch to proper migrations (Flyway/Liquibase)
instead of relying on Hibernate's auto-DDL.

## Connecting the existing frontend

The frontend (VS Code project, `laundry-app/`) currently uses `localStorage`
to move data between `services.html` → `payment.html` → `tracking.html` /
`dhobi.html`. See that project's `PROGRESS.md` for the exact plan to swap
each of those localStorage calls for a `fetch()` to this API — field names
were kept matching on purpose (e.g. `pickupSlot`, `pickupAddress`, `items`)
so it's close to a drop-in replacement, not a data reshape.

CORS is currently open to `http://localhost:*` — tighten
`SecurityConfig.corsConfigurationSource()` once you have a real frontend
domain.
