package de.mirkosertic.desktopsearch;

import java.nio.file.Path;

public interface DirectoryListener {
    void fileDeleted(FilesystemLocation aFileSystemLocation, Path aFile);

    void fileCreatedOrModified(FilesystemLocation aFileSystemLocation, Path aFile);
}
