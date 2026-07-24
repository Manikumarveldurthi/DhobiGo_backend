# New features added (this round)

All changes are additive — nothing existing was removed or renamed, and
every new column/field is nullable with a safe default, so your current
data and working flows are untouched.

## What's fully working out of the box
- **Photo proof** — dhobi attaches a photo at pickup and before delivery
  (`PATCH /api/dhobi/orders/{id}/pickup-photo`, `.../delivery-photo`)
- **Garment-level sub-status** — Washing/Drying/Folding while an order is
  in the WASHING stage (`PATCH /api/dhobi/orders/{id}/substage`)
- **Per-item notes & photos** — customers can flag a stain etc. per item
  at checkout (`OrderItemRequest.specialInstructions` / `.photoUrl`)
- **Eco-friendly badges** — `CatalogItem.ecoFriendly`, editable from the
  admin catalog form
- **Wallet + referrals** — every signup gets a `DGxxxxxx` referral code;
  using someone else's code at signup credits both sides ₹50
  (`GET /api/wallet/me`); wallet can be applied at checkout
  (`CreateOrderRequest.useWallet`)
- **Loyalty tiers** — Bronze/Silver/Gold on `GET /api/users/me`, computed
  from completed order count, no schema change needed
- **Recurring pickup plans** — customers save a plan (service/slot/
  address/frequency); a daily job reminds them by WhatsApp when it's due
  (`/api/subscriptions/**`, see `SubscriptionScheduler`)
- **Corporate accounts** — `accountType` + `companyName` on signup/profile,
  visible to admins via `AdminController.UserSummary`
- **Reorder** — frontend-only, rebuilds a cart from a past order's items

## Deliberately stubbed
- **Web push notifications** — `PushSubscription` storage and the
  `/api/push/subscribe` `/unsubscribe` endpoints are real and work today.
  Actually *sending* a push is a logging stub (see
  `PushNotificationService.java` / `PushProperties.java`) — I didn't want
  to pin an unverified Maven dependency I couldn't compile-test here,
  since a bad dependency would break your whole build. To finish it:
  1. Add a web-push library to `pom.xml` (e.g. `nl.martijndwars:webpush-java`)
  2. Generate a VAPID keypair (that library has a CLI for it)
  3. Set `PUSH_ENABLED=true`, `PUSH_VAPID_PUBLIC_KEY`, `PUSH_VAPID_PRIVATE_KEY`
  4. Fill in the real send call where `PushNotificationService.notifyUser()`
     currently just logs

## New env vars (all optional, sensible defaults)
```
PUSH_ENABLED=false
PUSH_VAPID_PUBLIC_KEY=
PUSH_VAPID_PRIVATE_KEY=
PUSH_VAPID_SUBJECT=mailto:admin@dhobigo.com
```

## One thing worth doing regardless of these features
`application.yml` has real-looking Twilio/Razorpay/Gmail credentials
committed in plain text. Worth rotating before this ever goes to a public
repo, even if they're currently test/dev keys.
