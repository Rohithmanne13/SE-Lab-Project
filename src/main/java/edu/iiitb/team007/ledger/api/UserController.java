package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.service.UserService;
import edu.iiitb.team007.ledger.service.UserView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /users (FR-1.1, FR-1.2) and GET /users/{userId} (FR-6.1). Role: CLIENT. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;
    private final JsonBodies bodies;

    public UserController(UserService users, JsonBodies bodies) {
        this.users = users;
        this.bodies = bodies;
    }

    @PostMapping
    public ResponseEntity<UserView> create(HttpServletRequest request,
                                           @RequestBody(required = false) byte[] body) {
        bodies.requireJson(request);
        CreateUserRequest r = bodies.read(body, CreateUserRequest.class).value();
        UserView user = users.create(r.name(), r.email());
        return Created.at("/api/v1/users/" + user.userId(), user);
    }

    @GetMapping("/{userId}")
    public UserView get(@PathVariable String userId) {
        UUID id = RequestIds.require("userId", userId);
        return users.get(id);
    }
}
