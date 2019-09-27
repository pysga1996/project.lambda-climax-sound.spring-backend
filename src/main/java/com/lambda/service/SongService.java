package com.lambda.service;

import com.lambda.model.entity.Song;
import javafx.print.Collation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public interface SongService {
    Optional<Song> findById(Long id);
    Song findByName(String name);
    Page<Song> findAll(Pageable pageable);
    Page<Song> findAllByNameContaining(String name, Pageable pageable);
    Page<Song> findAllByAlbum_Id(Long id, Pageable pageable);
    Iterable<Song> findAllByAlbum_Id(Long id);
    Page<Song> findAllByTags_Name(String name, Pageable pageable);
    void save(Song song);
    void deleteAll(Collection<Song> songs);
}
