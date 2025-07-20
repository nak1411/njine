package com.nak.engine.config;

public interface Validatable {
    void validate() throws ValidationException;
}