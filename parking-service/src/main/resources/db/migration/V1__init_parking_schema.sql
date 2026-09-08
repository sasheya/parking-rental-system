CREATE TABLE IF NOT EXISTS parking_spaces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    price_per_hour DECIMAL(10, 2) NOT NULL,
    total_slots INT NOT NULL,
    available_slots INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS availability_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parking_space_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    is_booked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_slots_parking_space FOREIGN KEY (parking_space_id) REFERENCES parking_spaces(id) ON DELETE CASCADE
);
