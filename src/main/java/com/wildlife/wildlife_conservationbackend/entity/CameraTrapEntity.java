package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("camera_traps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraTrapEntity {
    @MongoId
    private String id;

    @Indexed
    private String parkId;

    private String areaId;

    private String name;

    private Location location;
}
