package com.alpha.controller;

import com.alpha.model.dto.SongUploadForm;
import com.alpha.model.dto.UserDTO;
import com.alpha.model.entity.Comment;
import com.alpha.model.entity.Song;
import com.alpha.service.*;
import com.alpha.service.impl.AudioStorageService;
import com.alpha.service.impl.FormConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@CrossOrigin(origins = {"https://alpha-sound.netlify.com", "http://localhost:4200"}, allowedHeaders = "*")
@RestController
@RequestMapping("/api/song")
public class SongRestController {

    private final SongService songService;
    private final AlbumService albumService;
    private final UserService userService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final FormConvertService formConvertService;
    private final AudioStorageService audioStorageService;

    @Autowired
    public SongRestController(SongService songService, AlbumService albumService, UserService userService,
                              LikeService likeService, CommentService commentService,
                              FormConvertService formConvertService, AudioStorageService audioStorageService) {
        this.songService = songService;
        this.albumService = albumService;
        this.userService = userService;
        this.likeService = likeService;
        this.commentService = commentService;
        this.formConvertService = formConvertService;
        this.audioStorageService = audioStorageService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<Void> uploadSong(@Valid @RequestPart("song") SongUploadForm songUploadForm, @RequestPart("audio") MultipartFile file, @RequestParam(value = "album-albumId", required = false) Long albumId) {
        Song song = formConvertService.convertSongUploadFormToSong(songUploadForm);
        try {
            Song songToSave = songService.save(song);
            String fileDownloadUri = audioStorageService.saveToFirebaseStorage(songToSave, file);
            songToSave.setUrl(fileDownloadUri);
            songToSave.setUploader(userService.getCurrentUser());
            albumService.pushToAlbum(song, albumId);
            songService.save(songToSave);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            if (song.getId() != null) {
                songService.deleteById(song.getId());
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @GetMapping("/download/{fileName:.+}")
//    public ResponseEntity<Resource> downloadAudio(@PathVariable String fileName, HttpServletRequest request) {
//        return downloadService.generateUrl(fileName, request, audioStorageService);
//    }

    @GetMapping(value = "/list")
    public ResponseEntity<Page<Song>> songList(@PageableDefault() Pageable pageable, @RequestParam(value = "sort", required = false) String sort) {
        Page<Song> songList = songService.findAll(pageable, sort);
        HttpCookie httpCookie = ResponseCookie.from("pysga-alpha-sound", "shit").maxAge(300).path("/").build();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.SET_COOKIE, httpCookie.toString());
        httpHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Set-Cookie");
        httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        if (songList.getTotalElements() == 0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            songService.setLike(songList);
            return new ResponseEntity<>(songList, httpHeaders, HttpStatus.OK);
        }
    }

    @GetMapping(value = "/list-top")
    public ResponseEntity<Iterable<Song>> topSongList(@RequestParam(value = "sort", required = false) String sort) {
        Iterable<Song> songList;
        if (sort != null) {
            songList = songService.findTop10By(sort);
        } else {
            songList = songService.findAll();
        }
        int size = 0;
        if (songList instanceof Collection) {
            size = ((Collection<?>) songList).size();
        }
        if (size == 0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            songService.setLike(songList);
            return new ResponseEntity<>(songList, HttpStatus.OK);
        }
    }

    @GetMapping(value = "/detail", params = "id")
    public ResponseEntity<Song> songDetail(@RequestParam("id") Long id) {
        Optional<Song> song = songService.findById(id);
        song.ifPresent(songService::setLike);
        return song.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/search", params = "name")
    public ResponseEntity<Iterable<Song>> songListByName(@RequestParam("name") String name) {
        Iterable<Song> songList = songService.findAllByTitleContaining(name);
        int listSize = 0;
        if (songList instanceof Collection) {
            listSize = ((Collection<?>) songList).size();
        }
        if (listSize == 0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(songList, HttpStatus.OK);
    }

    @GetMapping(value = "/search", params = "tag")
    public ResponseEntity<Page<Song>> songListByTag(@RequestParam("tag") String tag, Pageable pageable) {
        Page<Song> songList = songService.findAllByTag_Name(tag, pageable);
        if (songList.getTotalElements() == 0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(songList, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/edit", params = "id")
    public ResponseEntity<Void> editSong(@RequestPart("song") Song song, @RequestParam("id") Long id, @RequestPart(value = "audio", required = false) MultipartFile multipartFile) {
        Optional<Song> oldSong = songService.findById(id);
        if (oldSong.isPresent()) {
            if (multipartFile != null) {
                String fileDownloadUri = audioStorageService.saveToFirebaseStorage(oldSong.get(), multipartFile);
                song.setUrl(fileDownloadUri);
            }
            songService.setFields(oldSong.get(), song);
            songService.save(oldSong.get());
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/delete", params = "id")
    public ResponseEntity<Void> deleteSong(@RequestParam("id") Long id) {
        songService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-song")
    public ResponseEntity<Page<Song>> mySongList(Pageable pageable) {
        Page<Song> mySongList = songService.findAllByUsersContains(userService.getCurrentUser(), pageable);
        if (mySongList.getTotalElements() > 0) {
            return new ResponseEntity<>(mySongList, HttpStatus.OK);
        } else return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(params = {"like", "song-id"})
    public ResponseEntity<Void> likeSong(@RequestParam("song-id") Long id) {
        likeService.like(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(params = {"unlike", "song-id"})
    public ResponseEntity<Void> dislikeSong(@RequestParam("song-id") Long id) {
        likeService.unlike(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/uploaded/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Song>> userSongList(Pageable pageable) {
        UserDTO currentUser = userService.getCurrentUser();
        Page<Song> userSongList = songService.findAllByUploader_Id(currentUser.getId(), pageable);
        boolean isEmpty = userSongList.getTotalElements() == 0;
        if (isEmpty) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else return new ResponseEntity<>(userSongList, HttpStatus.OK);
    }

    @PostMapping(params = {"listen", "song-id"})
    public ResponseEntity<Void> listenToSong(@RequestParam("song-id") Long id) {
        Optional<Song> song = songService.findById(id);
        if (song.isPresent()) {
            long currentListeningFrequency = song.get().getListeningFrequency();
            song.get().setListeningFrequency(++currentListeningFrequency);
            songService.save(song.get());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(params = {"comment", "song-id"})
    public ResponseEntity<Void> commentOnSong(@Valid @RequestBody Comment comment, @RequestParam("song-id") Long id) {
        Optional<Song> song = songService.findById(id);
        if (song.isPresent()) {
            LocalDateTime localDateTime = LocalDateTime.now();
            UserDTO currentUser = userService.getCurrentUser();
            comment.setLocalDateTime(localDateTime);
            comment.setSong(song.get());
            comment.setUser(currentUser);
            commentService.save(comment);
            return new ResponseEntity<>(HttpStatus.OK);
        } else return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(params = {"comment", "comment-id"})
    public ResponseEntity<Void> deleteCommentOnSong(@RequestParam("comment-id") Long id) {
        Optional<Comment> comment = commentService.findById(id);
        if (comment.isPresent() && comment.get().getUser().getId().equals(userService.getCurrentUser().getId())) {
            commentService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } else return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}