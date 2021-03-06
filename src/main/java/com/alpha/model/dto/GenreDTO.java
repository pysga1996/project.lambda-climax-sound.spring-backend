package com.alpha.model.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(value = {"songs", "albums"}, allowGetters = true, ignoreUnknown = true)
public class GenreDTO {

    private Integer id;

    @NotBlank
    private String name;

    @JsonBackReference("song-genre")
    private Collection<SongDTO> songs;

    @JsonBackReference("album-genre")
    private Collection<AlbumDTO> albums;
}
