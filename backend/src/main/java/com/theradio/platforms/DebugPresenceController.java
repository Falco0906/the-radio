package com.theradio.platforms;

import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.domain.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug/presence")
public class DebugPresenceController {

    private final ListeningStateRepository listeningStateRepository;
    private final UserRepository userRepository;

    public DebugPresenceController(ListeningStateRepository listeningStateRepository, UserRepository userRepository) {
        this.listeningStateRepository = listeningStateRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getRawPresence(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        ListeningState state = listeningStateRepository.findByUser(user).orElse(null);
        if (state == null) {
            return ResponseEntity.ok("No state in DB for user " + userId);
        }
        
        return ResponseEntity.ok(state);
    }
}
