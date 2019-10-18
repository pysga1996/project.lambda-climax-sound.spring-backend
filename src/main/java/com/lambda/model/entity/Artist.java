package com.lambda.model.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@JsonIgnoreProperties(value = {"albums", "songs", "avatarBlobString", "avatarUrl"}, allowGetters = true, ignoreUnknown = true)
//@JsonIdentityInfo(
//        generator = ObjectIdGenerators.PropertyGenerator.class,
//        property = "id")
public class Artist {
    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

    @DateTimeFormat(pattern = "MM-dd-yyyy")
    private Date birthDate;

    private String avatarUrl;

    private String avatarBlobId;

//    @Column(columnDefinition = "LONGTEXT")
    private String biography;

    @JsonBackReference(value = "song-artist")
    @ManyToMany(mappedBy = "artists", fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    private Collection<Song> songs;

    @JsonBackReference(value = "album-artist")
    @ManyToMany(mappedBy = "artists", fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    private Collection<Album> albums;

    public Artist(String name) {
        this.name = name;
    }

    public Artist(String name, Date birthDate, String avatarUrl, String biography) {
        this.name = name;
        this.birthDate = birthDate;
        this.avatarUrl = avatarUrl;
        this.biography = biography;
    }
}
