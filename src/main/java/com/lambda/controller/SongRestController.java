package com.lambda.controller;

import com.lambda.model.entity.Album;
import com.lambda.model.entity.Song;
import com.lambda.model.form.AudioUploadForm;
import com.lambda.model.util.UploadResponse;
import com.lambda.service.AlbumService;
import com.lambda.service.SongService;
import com.lambda.service.impl.AudioStorageService;
import com.lambda.service.impl.DownloadService;
import com.lambda.service.impl.FormConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/song")
public class SongRestController {
    @Autowired
    SongService songService;

    @Autowired
    AlbumService albumService;

    @Autowired
    private AudioStorageService audioStorageService;

    @Autowired
    private FormConvertService formConvertService;

    @Autowired
    private DownloadService downloadService;

    @PostMapping("/create")
    public ResponseEntity<Long> createSong(@RequestBody AudioUploadForm audioUploadForm, @RequestParam(value = "albumId", required = false) String albumId) {
        Song song = formConvertService.convertToSong(audioUploadForm);
        if (song == null) return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        if (albumId != null) {
            Optional<Album> album = albumService.findById(Long.parseLong(albumId));
            if (album.isPresent()) {
                Collection<Album> albums = new HashSet<>();
                albums.add(album.get());
                song.setAlbums(albums);
            }
        }
        Long id = songService.save(song).getId();
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<Object> uploadAudio(@RequestPart("audio") MultipartFile file, @RequestPart("songId") String id, @RequestPart(value = "albumId", required = false) String albumId) {
        Optional<Song> song = songService.findById(Long.parseLong(id));
        if (!song.isPresent()) return new ResponseEntity<>("Song metadata was not found in database!", HttpStatus.NOT_FOUND);
        String fileName = audioStorageService.storeFile(file, song.get());
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/song/download/")
                .path(fileName)
                .toUriString();
        song.get().setUrl(fileDownloadUri);
        if (albumId != null) {
            Optional<Album> album = albumService.findById(Long.parseLong(albumId));
            if (album.isPresent()) {
                Collection<Album> albums;
                if (song.get().getAlbums() == null) {
                    albums = new HashSet<>();
                    albums.add(album.get());
                } else {
                    albums = song.get().getAlbums();
                }
                song.get().setAlbums(albums);
            }
        }
        songService.save(song.get());
        return new ResponseEntity<>(new UploadResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize()), HttpStatus.OK);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadAudio(@PathVariable String fileName, HttpServletRequest request) {
        return downloadService.generateUrl(fileName, request, audioStorageService);
    }

    @GetMapping("/list")
    public ResponseEntity<Page<Song>> songList(Pageable pageable) {
        Page<Song> songList = songService.findAll(pageable);
        if (songList.getTotalElements() == 0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else return new ResponseEntity<>(songList, HttpStatus.OK);
    }

    @GetMapping(value = "/detail", params = {"id"})
    public ResponseEntity<Song> songDetail(@RequestParam("id") Long id) {
        Optional<Song> song = songService.findById(id);
        return song.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @GetMapping(value = "/search", params = "tag")
    public ResponseEntity<Page<Song>> songListByTag(@RequestParam("tag") String tag, Pageable pageable) {
        Page<Song> songList = songService.findAllByTags_Name(tag, pageable);
        if (songList.getTotalElements()==0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(songList, HttpStatus.OK);
    }

    @PutMapping(value = "/edit", params = "id")
    public ResponseEntity<String> editSong(@RequestBody AudioUploadForm audioUploadForm, @RequestParam("id") Long id) {
        Song song = formConvertService.convertToSong(audioUploadForm);
        song.setId(id);
        songService.save(song);
        return new ResponseEntity<>("Song updated successfully!", HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete", params = "id")
    public ResponseEntity<String> deleteSong(@RequestParam("id") Long id) {
        Boolean result = songService.deleteById(id);
        if (result) {
            return new ResponseEntity<>("Song removed successfully", HttpStatus.OK);
        } else return new ResponseEntity<>("Song removed but media file was not found on server", HttpStatus.NOT_FOUND);
    }


}
