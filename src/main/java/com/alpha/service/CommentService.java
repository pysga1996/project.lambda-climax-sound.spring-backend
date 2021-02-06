package com.alpha.service;

import com.alpha.model.entity.Comment;

import java.util.Optional;

public interface CommentService {
    Optional<Comment> findById(Long id);

    void save(Comment comment);

    void deleteById(Long id);
}