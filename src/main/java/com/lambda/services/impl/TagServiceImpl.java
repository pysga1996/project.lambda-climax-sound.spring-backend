package com.lambda.services.impl;

import com.lambda.models.entities.Tag;
import com.lambda.repositories.TagRepository;
import com.lambda.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TagServiceImpl implements TagService {
    @Autowired
    TagRepository tagRepository;

    @Override
    public Optional<Tag> findById(Long id) {
        return tagRepository.findById(id);
    }

    @Override
    public Tag findByName(String name) {
        return tagRepository.findByName(name);
    }

    @Override
    public Page<Tag> findAll(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }

    @Override
    public Page<Tag> findAllByNameContaining(String name, Pageable pageable) {
         return tagRepository.findAllByNameContaining(name, pageable);
    }

    @Override
    public void save(Tag tag) {
        tagRepository.save(tag);
    }

    @Override
    public void deleteById(Long id) {
        tagRepository.deleteById(id);
    }
}