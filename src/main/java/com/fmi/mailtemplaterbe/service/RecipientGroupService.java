package com.fmi.mailtemplaterbe.service;

import com.fmi.mailtemplaterbe.domain.entity.RecipientGroupEntity;
import com.fmi.mailtemplaterbe.domain.resource.RecipientGroupResource;
import com.fmi.mailtemplaterbe.repository.RecipientGroupRepository;
import com.fmi.mailtemplaterbe.util.ExceptionsUtil;
import com.fmi.mailtemplaterbe.util.RecipientGroupMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipientGroupService {

    private final RecipientGroupRepository recipientGroupRepository;

    /**
     * Create a recipient group.
     *
     * @param recipientGroupResource The recipient group data for creation.
     * @return The created recipient group.
     */
    public RecipientGroupResource createRecipientGroup(RecipientGroupResource recipientGroupResource) {
        RecipientGroupEntity recipientGroupEntity = RecipientGroupMapper.resourceToEntity(recipientGroupResource);
        RecipientGroupEntity savedRecipientGroupEntity = saveRecipientGroupEntity(recipientGroupEntity);

        return RecipientGroupMapper.entityToResource(savedRecipientGroupEntity);
    }


    /**
     * Get all recipient groups.
     *
     * @return {@link List<RecipientGroupResource>}
     */
    public List<RecipientGroupResource> getAllRecipientGroups() {
        return recipientGroupEntitiesToRecipientGroupResource(recipientGroupRepository.findAll());
    }

    /**
     * Update a recipient group by its id.
     *
     * @param id                     The id of the recipient group.
     * @param recipientGroupResource The resource data to use for the update.
     * @return The updated recipient group.
     */
    public RecipientGroupResource updateRecipientGroupById(Long id, RecipientGroupResource recipientGroupResource) {
        RecipientGroupEntity recipientGroupEntity = recipientGroupRepository.findById(id).orElse(null);

        if (recipientGroupEntity == null) {
            throw ExceptionsUtil.getRecipientGroupNotFoundException(id);
        }

        return RecipientGroupMapper.entityToResource(
                updateRecipientGroupEntityIfNecessary(recipientGroupEntity, recipientGroupResource));
    }

    /**
     * Delete a recipient group by its id.
     *
     * @param id The id of the recipient group.
     */
    public void deleteRecipientGroupById(Long id) {
        RecipientGroupEntity recipientGroupEntity = recipientGroupRepository.findById(id).orElse(null);

        if (recipientGroupEntity == null) {
            throw ExceptionsUtil.getRecipientGroupNotFoundException(id);
        }

        recipientGroupRepository.delete(recipientGroupEntity);
    }

    private RecipientGroupEntity saveRecipientGroupEntity(RecipientGroupEntity recipientGroupEntity) {
        RecipientGroupEntity savedRecipientGroupEntity = null;

        try {
            savedRecipientGroupEntity = recipientGroupRepository.save(recipientGroupEntity);
        } catch (DataIntegrityViolationException e) {
            final Throwable dataIntegrityViolationCause = e.getCause();

            if (dataIntegrityViolationCause instanceof ConstraintViolationException) {
                final Throwable constraintViolationCause = dataIntegrityViolationCause.getCause();

                throw ExceptionsUtil.getRecipientGroupConstraintViolationException(
                        constraintViolationCause.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return savedRecipientGroupEntity;
    }

    private RecipientGroupEntity updateRecipientGroupEntityIfNecessary(
            RecipientGroupEntity recipientGroupEntity, RecipientGroupResource recipientGroupResource) {
        final String title = recipientGroupResource.getTitle();
        final String recipientIds = recipientGroupResource.getRecipientIds();

        if (title != null) {
            recipientGroupEntity.setTitle(title);
        }

        if (recipientIds != null) {
            recipientGroupEntity.setRecipientIds(recipientIds);
        }

        return saveRecipientGroupEntity(recipientGroupEntity);
    }

    private List<RecipientGroupResource> recipientGroupEntitiesToRecipientGroupResource(
            List<RecipientGroupEntity> recipientGroupEntities) {
        return recipientGroupEntities.stream()
                .map(RecipientGroupMapper::entityToResource)
                .collect(Collectors.toList());
    }
}
