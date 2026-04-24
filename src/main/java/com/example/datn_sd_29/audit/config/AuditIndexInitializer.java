package com.example.datn_sd_29.audit.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup handler — tự quản lý index của collection audit_logs.
 *
 * Quy trình:
 *  1. Drop mọi TTL index cũ trên createdAt (nếu có) — tránh conflict khi đổi schema
 *  2. Tạo lại đầy đủ các index cần thiết: TTL (expiresAt), single (createdAt, severity...),
 *     và compound (userEmail+time, entityType+entityId+time, actionType+time...)
 *  3. idempotent — chạy nhiều lần vẫn OK, MongoDB bỏ qua nếu index đã tồn tại với options giống hệt
 *
 * Đặt Order(0) để chạy trước các runner khác, đảm bảo audit log ready trước khi ghi.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AuditIndexInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    private static final String COLLECTION = "audit_logs";

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Bảo đảm collection tồn tại (tránh lỗi khi listIndexes trên collection chưa có)
            if (!mongoTemplate.collectionExists(COLLECTION)) {
                mongoTemplate.createCollection(COLLECTION);
                log.info("Tạo mới collection {}", COLLECTION);
            }

            MongoCollection<Document> coll = mongoTemplate.getCollection(COLLECTION);

            // Step 1: dọn TTL index cũ xung đột
            cleanupOldTtlIndexes(coll);

            // Step 2: tạo lại các index cần thiết (idempotent — nếu đã tồn tại sẽ được bỏ qua)
            createIndexes(coll);

            log.info("Audit log indexes đã sẵn sàng trên collection '{}'", COLLECTION);
        } catch (Exception e) {
            // Không throw để không chặn startup — audit log vẫn ghi được (chỉ kém hiệu năng query)
            log.error("Lỗi khi init audit log indexes: {}", e.getMessage(), e);
        }
    }

    private void cleanupOldTtlIndexes(MongoCollection<Document> coll) {
        for (Document idx : coll.listIndexes()) {
            String name = idx.getString("name");
            if (name == null || "_id_".equals(name)) continue;

            boolean hasTtl = idx.containsKey("expireAfterSeconds");
            if (!hasTtl) continue;

            Document key = (Document) idx.get("key");
            if (key == null) continue;

            // TTL trên createdAt (cũ) → drop để thay bằng TTL trên expiresAt
            if (key.containsKey("createdAt") && !key.containsKey("expiresAt")) {
                try {
                    coll.dropIndex(name);
                    log.info("Drop TTL index cũ '{}' trên createdAt", name);
                } catch (Exception e) {
                    log.warn("Không drop được index '{}': {}", name, e.getMessage());
                }
            }
        }
    }

    private void createIndexes(MongoCollection<Document> coll) {
        // TTL index — MongoDB xóa document khi expiresAt < now
        safeCreateIndex(coll, Indexes.ascending("expiresAt"),
                new IndexOptions().name("idx_ttl_expiresAt").expireAfter(0L, java.util.concurrent.TimeUnit.SECONDS));

        // Single-field indexes cho filter phổ biến
        safeCreateIndex(coll, Indexes.ascending("userEmail"), new IndexOptions().name("idx_userEmail"));
        safeCreateIndex(coll, Indexes.ascending("userRole"),  new IndexOptions().name("idx_userRole"));
        safeCreateIndex(coll, Indexes.ascending("actionType"), new IndexOptions().name("idx_actionType"));
        safeCreateIndex(coll, Indexes.ascending("entityType"), new IndexOptions().name("idx_entityType"));
        safeCreateIndex(coll, Indexes.ascending("entityId"),   new IndexOptions().name("idx_entityId"));
        safeCreateIndex(coll, Indexes.ascending("severity"),   new IndexOptions().name("idx_severity"));
        safeCreateIndex(coll, Indexes.ascending("createdAt"),  new IndexOptions().name("idx_createdAt"));

        // Compound indexes tối ưu query thường dùng: "hoạt động của user X", "lịch sử entity Y"...
        safeCreateIndex(coll, Indexes.compoundIndex(
                Indexes.ascending("userEmail"),
                Indexes.descending("createdAt")
        ), new IndexOptions().name("idx_user_time"));

        safeCreateIndex(coll, Indexes.compoundIndex(
                Indexes.ascending("userRole"),
                Indexes.descending("createdAt")
        ), new IndexOptions().name("idx_role_time"));

        safeCreateIndex(coll, Indexes.compoundIndex(
                Indexes.ascending("entityType"),
                Indexes.ascending("entityId"),
                Indexes.descending("createdAt")
        ), new IndexOptions().name("idx_entity_time"));

        safeCreateIndex(coll, Indexes.compoundIndex(
                Indexes.ascending("severity"),
                Indexes.descending("createdAt")
        ), new IndexOptions().name("idx_severity_time"));

        safeCreateIndex(coll, Indexes.compoundIndex(
                Indexes.ascending("actionType"),
                Indexes.descending("createdAt")
        ), new IndexOptions().name("idx_action_time"));
    }

    private void safeCreateIndex(MongoCollection<Document> coll, Bson keys, IndexOptions options) {
        try {
            coll.createIndex(keys, options);
        } catch (Exception e) {
            // Bỏ qua nếu index đã tồn tại với options khớp
            log.debug("Index '{}' đã tồn tại hoặc lỗi: {}", options.getName(), e.getMessage());
        }
    }
}
