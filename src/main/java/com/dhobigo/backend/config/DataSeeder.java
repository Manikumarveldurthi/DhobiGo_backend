package com.dhobigo.backend.config;

import com.dhobigo.backend.model.*;
import com.dhobigo.backend.repository.CatalogItemRepository;
import com.dhobigo.backend.repository.DhobiProfileRepository;
import com.dhobigo.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds an admin account, 2 sample dhobis, and the full catalog on
 * startup — in BOTH the "dev" (H2) and "prod" (MySQL) profiles.
 *
 * Safe to run against a real database: every seed method checks whether
 * the data already exists first, so on every restart after the first one
 * it does nothing at all — it won't create duplicates or overwrite
 * anything you've changed by hand.
 *
 * Default admin login (DEV/first-run only — change the password after
 * logging in once, or delete this seeder once you have real accounts):
 *   email:    admin@dhobigo.com
 *   password: Admin@12345
 */
@Component
@Profile({"dev", "prod"})
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DhobiProfileRepository dhobiProfileRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                       DhobiProfileRepository dhobiProfileRepository,
                       CatalogItemRepository catalogItemRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.dhobiProfileRepository = dhobiProfileRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDhobis();
        seedCatalog();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@dhobigo.com")) return;

        User admin = User.builder()
                .fullName("DhobiGo Admin")
                .email("admin@dhobigo.com")
                .phone("9999999999")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
    }

    private void seedDhobis() {
        if (!userRepository.findByRole(Role.DHOBI).isEmpty()) return;

        createDhobi("Ramesh Kumar", "ramesh@dhobigo.com", "9812345001", 4.9, 610, true);
        createDhobi("Sunita Patil", "sunita@dhobigo.com", "9812345002", 4.8, 430, true);
        // Left pending on purpose — gives the admin approval queue
        // (GET /api/admin/dhobis/pending) something to show immediately.
        createDhobi("Vikram Singh", "vikram@dhobigo.com", "9812345003", 5.0, 0, false);
    }

    private void createDhobi(String name, String email, String phone, double rating, int completed, boolean approved) {
        User dhobiUser = User.builder()
                .fullName(name)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode("Dhobi@12345"))
                .role(Role.DHOBI)
                .enabled(true)
                .build();
        dhobiUser = userRepository.save(dhobiUser);

        DhobiProfile.Builder profileBuilder = DhobiProfile.builder()
                .user(dhobiUser)
                .rating(rating)
                .completedOrders(completed)
                .available(approved)
                .approved(approved);
        if (approved) {
            profileBuilder.approvedAt(java.time.Instant.now());
        }
        dhobiProfileRepository.save(profileBuilder.build());
    }

    private void seedCatalog() {
        if (catalogItemRepository.count() > 0) return;

        // Mirrors CATALOG in services-data.js exactly — keep both in sync
        // manually until the frontend is switched over to GET /api/catalog.
        save("shirt", "Shirt", "👕", ServiceType.WASH, 20);
        save("tshirt", "T-Shirt", "👕", ServiceType.WASH, 18);
        save("jeans", "Jeans", "👖", ServiceType.WASH, 30);
        save("trousers", "Trousers", "👖", ServiceType.WASH, 25);
        save("saree", "Saree", "🥻", ServiceType.WASH, 45);
        save("bedsheet", "Bedsheet (double)", "🛏️", ServiceType.WASH, 55);
        save("towel", "Towel", "🧺", ServiceType.WASH, 15);
        save("kurta", "Kurta", "👘", ServiceType.WASH, 28);

        save("shirt", "Shirt", "👕", ServiceType.IRON, 15);
        save("tshirt", "T-Shirt", "👕", ServiceType.IRON, 12);
        save("trousers", "Trousers", "👖", ServiceType.IRON, 15);
        save("saree", "Saree", "🥻", ServiceType.IRON, 35);
        save("kurta", "Kurta", "👘", ServiceType.IRON, 20);
        save("bedsheet", "Bedsheet", "🛏️", ServiceType.IRON, 30);

        save("blazer", "Blazer", "🧥", ServiceType.DRYCLEAN, 180);
        save("suit", "2-pc Suit", "🕴️", ServiceType.DRYCLEAN, 320);
        save("saree", "Saree (silk)", "🥻", ServiceType.DRYCLEAN, 220);
        save("coat", "Winter Coat", "🧥", ServiceType.DRYCLEAN, 250);
        save("curtain", "Curtain (pair)", "🪟", ServiceType.DRYCLEAN, 150);
        save("sherwani", "Sherwani", "👘", ServiceType.DRYCLEAN, 300);

        save("shirt", "Shirt", "👕", ServiceType.EXPRESS, 35);
        save("tshirt", "T-Shirt", "👕", ServiceType.EXPRESS, 30);
        save("jeans", "Jeans", "👖", ServiceType.EXPRESS, 50);
        save("trousers", "Trousers", "👖", ServiceType.EXPRESS, 45);
        save("kurta", "Kurta", "👘", ServiceType.EXPRESS, 48);
    }

    private void save(String key, String name, String icon, ServiceType service, int price) {
        catalogItemRepository.save(CatalogItem.builder()
                .itemKey(key).name(name).icon(icon).service(service).price(price).build());
    }
}
