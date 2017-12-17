package hu.csecsey.gergely.lepkehalo;

/**
 * Created by gcsecsey on 12/17/2017.
 */

public class Book {
    private String title;
    private String author;
    private String thumbnailUrl;
    private String id;

    public Book() {
    }

    public Book(String id, String title, String author, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }


}
