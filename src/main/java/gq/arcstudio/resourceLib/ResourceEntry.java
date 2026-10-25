package gq.arcstudio.resourceLib;

import java.io.File;

public class ResourceEntry {
    public final String virtualPath;
    public final File file;
    public final boolean force;

    public ResourceEntry(String virtualPath, File file, boolean force) {
        this.virtualPath = virtualPath;
        this.file = file;
        this.force = force;
    }
}
