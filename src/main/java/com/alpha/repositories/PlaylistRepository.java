package com.alpha.repositories;

import com.alpha.model.entity.Playlist;
import com.alpha.model.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    @Query("SELECT p FROM Playlist p LEFT JOIN FETCH p.songs WHERE p.id=:id")
    @NonNull
    Optional<Playlist> findById(@NonNull @Param("id") Long id);

    Iterable<Playlist> findAllByUsernameAndSongsNotContains(String username, Song song);

    Page<Playlist> findAllByUsername(String username, Pageable pageable);

    Page<Playlist> findAllByTitleContaining(String title, Pageable pageable);
}
