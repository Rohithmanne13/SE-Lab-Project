package edu.iiitb.team007.ledger.api;

import java.net.URI;
import org.springframework.http.ResponseEntity;

/** 201 Created with a Location header pointing to the new resource (CI-3). */
final class Created {

    private Created() {
    }

    static <T> ResponseEntity<T> at(String location, T body) {
        return ResponseEntity.created(URI.create(location)).body(body);
    }
}
