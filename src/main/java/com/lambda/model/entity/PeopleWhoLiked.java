package com.lambda.model.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "user_favorite_songs")
@Data
public class PeopleWhoLiked {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "user_id")
    private Long userId;

    public PeopleWhoLiked() {};

    public PeopleWhoLiked(Long songId, Long userId) {
        this.songId = songId;
        this.userId = userId;
    }
}