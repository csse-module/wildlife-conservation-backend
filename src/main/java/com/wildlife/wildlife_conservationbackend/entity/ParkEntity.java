package com.wildlife.wildlife_conservationbackend.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("parks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkEntity {
    @MongoId
    private String id;
    private String name;
    private String timezone;
    private List<ParkArea> areas;
}
