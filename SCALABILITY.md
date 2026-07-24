# Scaling DhobiGo for more traffic

This is written for when the site has enough real users that one backend
instance isn't enough. You don't need any of this today — but the app was
built in a way that makes it possible without a rewrite.

## Why this architecture can scale horizontally

The backend is **stateless**: every request carries its own JWT, and
nothing about "who's logged in" is stored in server memory (no HTTP
sessions). That's the single most important property for scaling — it
means you can run **multiple copies of the backend** behind a load
balancer, and it doesn't matter which copy handles which request. A user's
next request can hit a completely different instance and still work,
because their identity travels in the token, not in server state.

This is only true because of a few choices already made:
- JWT auth (`JwtService`, `JwtAuthenticationFilter`) instead of session cookies
- No `@SessionScope` beans, no server-side session storage anywhere
- All shared state lives in the database (MySQL), not in memory

## The path to more capacity, roughly in order

**1. Vertical scaling (easiest, do this first)**
Give the MySQL server and backend more CPU/RAM. Buys you time before
anything below is necessary. Tune `HikariCP` pool size in
`application.yml` (`spring.datasource.hikari.maximum-pool-size`) to match
your DB's actual connection limit — the default is usually fine until
you're running multiple backend instances.

**2. Run multiple backend instances behind a load balancer**
Since the backend is stateless, this is a config change, not a code
change. Example `docker-compose.yml` addition — run 3 backend replicas
and put nginx in front as the load balancer:

```yaml
# In docker-compose.yml, instead of one `backend` service, use `deploy.replicas`:
services:
  backend:
    build: .
    deploy:
      replicas: 3
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://db:3306/dhobigo?...
      # ...same as before
    # no `ports:` here anymore — nginx is the only public entry point
```

Then point nginx at all three instances instead of the frontend static
files directly for `/api/*`:

```nginx
# nginx.conf — add this upstream block
upstream dhobigo_backend {
    least_conn;                 # send new requests to whichever instance is least busy
    server backend:8081;        # docker-compose will round-robin across replicas
                                 # on this same hostname automatically
}

server {
    listen 80;

    location /api/ {
        proxy_pass http://dhobigo_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        root /usr/share/nginx/html;   # the static frontend
        try_files $uri $uri.html $uri/ /index.html;
    }
}
```

**3. Database becomes the bottleneck next**
Once you have multiple backend instances, MySQL itself is usually the next
limit. Options, roughly in order of effort:
- Add indexes on frequently-queried columns (`orders.customer_id`,
  `orders.dhobi_id` already get one implicitly from the foreign key —
  check `EXPLAIN` on slow queries as they show up)
- Read replicas for reporting/admin queries (the admin console's
  `GET /api/admin/orders` etc. are good candidates to point at a replica,
  since they don't need to be perfectly real-time)
- Connection pooling tuning (HikariCP `maximum-pool-size` × number of
  backend instances must stay under MySQL's `max_connections`)

**4. Polling → WebSockets**
Right now `tracking.html` and `dhobi.html` poll every few seconds. That's
fine at small scale but multiplies with user count. Spring Boot supports
STOMP-over-WebSocket natively — worth switching to once polling traffic
becomes a meaningful share of total load. This is a backend + frontend
change, not just infra, so budget more time for it than the steps above.

**5. Caching**
The catalog (`GET /api/catalog`) rarely changes — a good candidate for
Redis or even an in-memory cache with a short TTL, once catalog reads
become a hot path.

## What NOT to do yet

Don't add Kubernetes, service mesh, message queues, or microservices
before you actually have the traffic that needs them — each adds
operational complexity that isn't worth paying for at low scale. The
steps above, in order, will comfortably take this from "a few users" to
"a genuinely busy local service" without a rewrite. Revisit this doc when
you're actually seeing load-related slowdowns, not before.
