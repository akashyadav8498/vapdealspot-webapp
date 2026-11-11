package com.vapedealspot.email.contact;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactRepository contactRepository;

    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @GetMapping
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    @GetMapping("/{id}")
    public Contact getContact(@PathVariable String id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
    }

    @PostMapping
    public Contact createContact(@Valid @RequestBody Contact contact) {
        if (contactRepository.existsByEmail(contact.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact with this email already exists");
        }
        return contactRepository.save(contact);
    }

    @PutMapping("/{id}")
    public Contact updateContact(@PathVariable String id, @Valid @RequestBody Contact contactData) {
        Contact existing = contactRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        
        if (!existing.getEmail().equals(contactData.getEmail()) && contactRepository.existsByEmail(contactData.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use by another contact");
        }

        existing.setFirstName(contactData.getFirstName());
        existing.setLastName(contactData.getLastName());
        existing.setEmail(contactData.getEmail());
        existing.setSubscribed(contactData.isSubscribed());
        
        return contactRepository.save(existing);
    }

    @PatchMapping("/{id}/subscription")
    public Contact updateSubscription(@PathVariable String id, @RequestBody Map<String, Boolean> payload) {
        if (!payload.containsKey("isSubscribed")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing isSubscribed field");
        }
        
        Contact existing = contactRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
                
        existing.setSubscribed(payload.get("isSubscribed"));
        return contactRepository.save(existing);
    }
}
