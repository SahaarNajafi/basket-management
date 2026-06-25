package com.example.basketservice.common.exception;

/** Thrown when a requested resource (product, basket, item) does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(
            String resource,
            Object id
    ) {
        super("%s with id %s was not found"
                .formatted(resource, id));
    }
}
