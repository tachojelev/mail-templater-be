package com.fmi.mailtemplaterbe.controller;

import com.fmi.mailtemplaterbe.domain.resource.RecipientGroupResource;
import com.fmi.mailtemplaterbe.service.RecipientGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
public class RecipientGroupController {

    private final RecipientGroupService recipientGroupService;

    @PostMapping(
            value = "/recipient-groups",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecipientGroupResource> createRecipientGroup(
            @Valid @RequestBody RecipientGroupResource recipientGroupResource) {
        return ResponseEntity.ok(recipientGroupService.createRecipientGroup(recipientGroupResource));
    }

    @GetMapping(
            value = "/recipient-groups",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RecipientGroupResource>> getRecipientGroups() {
        return ResponseEntity.ok(recipientGroupService.getAllRecipientGroups());
    }

    @PatchMapping(
            value = "/recipient-groups/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecipientGroupResource> updateRecipientGroup(
            @PathVariable(value = "id") Long id, @RequestBody RecipientGroupResource recipientGroupResource) {
        return ResponseEntity.ok(recipientGroupService.updateRecipientGroupById(id, recipientGroupResource));
    }

    @DeleteMapping(
            value = "/recipient-groups/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecipientGroupResource> deleteRecipientGroup(@PathVariable(value = "id") Long id) {
        recipientGroupService.deleteRecipientGroupById(id);

        return ResponseEntity.ok().build();
    }
}