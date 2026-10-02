package com.example.vieva.application.usecases.catalog;

import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.Topic;

import java.util.List;
import java.util.UUID;

/**
 * Read side of the catalog for lecturers: assigned subjects and their topics. Topics can be created
 * minimally when needed (no full CRUD — assumption of group 1).
 */
public interface LecturerCatalogService {
    List<Subject> listAssignedSubjects(UUID lecturerId);

    List<Topic> listTopics(UUID subjectId);

    Topic createTopic(UUID subjectId, String name, String description, UUID actorId);
}
