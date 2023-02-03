package top.dreamlike.helper;

import java.util.Optional;

public class FileEvent {
    public final int mask;
    public final Optional<String> name;

    public final int wfd;

    @Override
    public String toString() {
        return "FileEvent{" +
                "mask=" + mask +
                ", name=" + name +
                ", wfd=" + wfd +
                '}';
    }

    public FileEvent(int mask, String name, int wfd) {
        this.mask = mask;
        this.name = name.isBlank() ? Optional.empty() : Optional.of(name);
        this.wfd = wfd;

    }
}
