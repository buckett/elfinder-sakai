package org.sakaiproject.elfinder.sakai.site;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.elfinder.sakai.FsVolumeFactory;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.SiteService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the top-level volume. It contains the volumes for the site.
 * At the moment it will just contain the Resources Volume.
 * We'll have pre-hashed paths of /siteId/content/ /siteId/forums/ /siteId/
 *
 */
public class SiteFsVolume implements FsVolume {
    public String getSiteId() {
        return siteId;
    }

    private String siteId;
    private SiteService siteService;
    private SakaiFsService service;

    // These are the volumes and the prefixes they are mounted under.
    // They already know the siteId.
    private Map<String, FsVolume> subVolumes;

    public SiteFsVolume(String siteId, SakaiFsService service) {
        this.siteId = siteId;
        this.service = service;
        // TODO Injection
        siteService = (SiteService) ComponentManager.get(SiteService.class);
    }

    @Override
    public void createFile(FsItem fsi) throws IOException {
        throw new UnsupportedOperationException("Can't create files here.");
    }

    @Override
    public void createFolder(FsItem fsi) throws IOException {
        throw new UnsupportedOperationException("Can't create folders here.");
    }

    @Override
    public void deleteFile(FsItem fsi) throws IOException {
        throw new UnsupportedOperationException("Can't delete files here.");
    }

    @Override
    public void deleteFolder(FsItem fsi) throws IOException {
        throw new UnsupportedOperationException("Can't delete files here.");
    }

    @Override
    public boolean exists(FsItem newFile) {
        return false;
    }

    @Override
    public FsItem fromPath(String relativePath) {
        return null;
    }

    @Override
    public String getDimensions(FsItem fsi) {
        return null;
    }

    @Override
    public long getLastModified(FsItem fsi) {
        return 0;
    }

    @Override
    public String getMimeType(FsItem fsi) {
        return "directory";
    }

    @Override
    public String getName() {
        // We may want to cache this in the volumne
        String title = "";
        try {
            title = siteService.getSite(siteId).getTitle();
        } catch (IdUnusedException e) {
            // Ignore
        }
        return title;
    }

    @Override
    public String getName(FsItem fsi) {
        return null;
    }

    @Override
    public FsItem getParent(FsItem fsi) {
        return null;
    }

    @Override
    public String getPath(FsItem fsi) throws IOException {
        return null;
    }

    @Override
    public FsItem getRoot() {
        return new SiteFsItem(this, siteId);
    }

    @Override
    public long getSize(FsItem fsi) {
        return 0;
    }

    @Override
    public String getThumbnailFileName(FsItem fsi) {
        return null;
    }

    @Override
    public boolean hasChildFolder(FsItem fsi) {
        return true;
    }

    @Override
    public boolean isFolder(FsItem fsi) {
        // All items here are directories.
        return true;
    }

    @Override
    public boolean isRoot(FsItem fsi) {
        return true;
    }

    @Override
    public FsItem[] listChildren(FsItem fsi) {
        List<FsItem> children = new ArrayList<>();
        String siteId = ((SiteFsItem)fsi).getId();
        for (Map.Entry<String, FsVolumeFactory> factory : service.getToolVolume().entrySet()) {
            children.add(factory.getValue().getVolume(siteId).getRoot());
        }
        return children.toArray(new FsItem[0]);
    }

    @Override
    public InputStream openInputStream(FsItem fsi) throws IOException {
        return null;
    }

    @Override
    public OutputStream openOutputStream(FsItem fsi) throws IOException {
        return null;
    }

    @Override
    public void rename(FsItem src, FsItem dst) throws IOException {
        throw new UnsupportedOperationException("Can't rename here.");
    }
}
